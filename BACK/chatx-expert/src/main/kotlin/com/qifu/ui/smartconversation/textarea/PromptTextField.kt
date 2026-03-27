package com.qifu.ui.smartconversation.textarea

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupEvent
import com.intellij.codeInsight.lookup.LookupListener
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.EditorTextField
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.qifu.ui.smartconversation.textarea.header.EditorSelectionTagDetails
import com.qifu.ui.smartconversation.textarea.header.TagManager
import com.qifu.ui.smartconversation.textarea.lookup.DynamicLookupGroupItem
import com.qifu.ui.smartconversation.textarea.lookup.LookupActionItem
import com.qifu.ui.smartconversation.textarea.lookup.LookupGroupItem
import com.qifu.ui.smartconversation.textarea.lookup.LookupItem
import com.qifu.ui.smartconversation.textarea.lookup.action.SlashCommandActionItem
import com.qifu.ui.utils.CompletionRequestUtil
import com.qifu.utils.UiUtil
import com.qifu.ui.smartconversation.SlashCommandRegistry
import com.qifu.ui.smartconversation.SlashCommandScope
import com.qihoo.finance.lowcode.smartconversation.configuration.JarvisKeys.IS_PROMPT_TEXT_FIELD_DOCUMENT
import com.qihoo.finance.lowcode.smartconversation.configuration.JarvisKeys.PROMPT_FIELD_KEY
import kotlinx.coroutines.*
import java.awt.Dimension
import java.awt.datatransfer.DataFlavor
import java.awt.event.MouseAdapter
import java.awt.event.MouseMotionAdapter
import java.util.*

class PromptTextField(
    private val project: Project,
    private val tagManager: TagManager,
    supportImage: Boolean = false,
    taskId: String,
    private val onTextChanged: (String) -> Unit,
    private val onBackSpace: () -> Unit,
    private val onLookupAdded: (LookupActionItem) -> Unit,
    private val onSubmit: (String) -> Unit,
    private val onImagePasted: ((java.awt.Image) -> Unit)? = null,
) : EditorTextField(project, FileTypes.PLAIN_TEXT), Disposable {

    /** 协程作用域，随组件销毁自动取消 */
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val lookupManager = PromptTextFieldLookupManager(project, this, onLookupAdded)
    private val searchManager = SearchManager(project, tagManager,taskId)

    private var mouseClickListener: MouseAdapter? = null
    private var mouseMotionListener: MouseMotionAdapter? = null

    private var showSuggestionsJob: Job? = null
    private var searchState = SearchState()
    private var lastSearchResults: List<LookupItem>? = null

    /** 用于事件分发 */
    val dispatcherId: UUID = UUID.randomUUID()
    var lookup: LookupImpl? = null
    val LOG = thisLogger()


    init {
        isOneLineMode = false
        IS_PROMPT_TEXT_FIELD_DOCUMENT.set(document, true)
        document.putUserData(PROMPT_FIELD_KEY, this)
        setPlaceholder("Ask anything... Use '@' to include additional context")
        document.putUserData(PROMPT_FIELD_KEY, this)
        installPasteHandler()
        searchManager.setSupportImages(supportImage)
    }

    // -------------------------------
    // 生命周期钩子
    // -------------------------------

    override fun onEditorAdded(editor: Editor) {
        // 注册键盘事件监听（带 Disposable）
        IdeEventQueue.getInstance().addDispatcher(
            PromptTextFieldEventDispatcher(dispatcherId, onBackSpace) { event ->
                val shown = lookup?.let { it.isShown && !it.isLookupDisposed } == true
                if (shown) {
                    return@PromptTextFieldEventDispatcher
                }

                onSubmit(getExpandedText())
                event.consume()
            },
            this
        )
    }

    fun clear() {
        runInEdt {
            text = ""
            clearPlaceholders()
            clearSlashHighlights()
            (editor as? EditorEx)?.let { clearSlashArgumentHighlights(it) }
        }
    }

    fun setTextAndFocus(text: String) {
        runInEdt {
            this.text = text
            requestFocusInWindow()
        }
    }

    /**
     * 插入代码选择占位符
     * @param fileName 文件名
     * @param startLine 开始行号
     * @param endLine 结束行号
     * @param selectedCode 选中的代码内容
     */
    fun insertCodeSelectionPlaceholder(fileName: String, startLine: Int, endLine: Int, selectedCode: String) {
        val editor = editor as? EditorEx ?: return
        
        runUndoTransparentWriteAction {
            // 创建显示的 label 文本，格式：文件名 (行号-行号)
            val label = " $fileName ($startLine-$endLine) "
            
            // 插入 label 到当前光标位置
            val (start, end) = replaceSelectionOrInsert(editor, label)
            
            // 添加占位符，关联实际的代码内容
            addEditorPlaceholder(editor, start, end, label, selectedCode)
            
            // 滚动到光标位置
            invokeLater {
                if (!editor.isDisposed) {
                    editor.scrollingModel.scrollToCaret(com.intellij.openapi.editor.ScrollType.MAKE_VISIBLE)
                }
            }
        }
    }

    /**
     * 插入文件路径占位符
     * @param fileName 文件名（显示用）
     * @param filePath 文件绝对路径
     */
    fun insertFilePathPlaceholder(fileName: String, filePath: String) {
        val editor = editor as? EditorEx ?: return
        val basePath = project.basePath ?: ""
        
        runUndoTransparentWriteAction {
            // 显示的 label 文本，格式：文件名
            val label = " $fileName "
            
            // 插入 label 到当前光标位置
            val (start, end) = replaceSelectionOrInsert(editor, label)
            
            // 生成实际的 markdown 代码块格式的相对路径
            val relativePath = com.qifu.utils.toRelativePath(filePath, basePath)
            val content = "`$relativePath`"
            
            // 添加占位符，关联实际的相对路径
            addEditorPlaceholder(editor, start, end, label, content)
            
            // 滚动到光标位置
            invokeLater {
                if (!editor.isDisposed) {
                    editor.scrollingModel.scrollToCaret(com.intellij.openapi.editor.ScrollType.MAKE_VISIBLE)
                }
            }
        }
    }

    /**
     * 插入文件夹路径占位符
     * @param folderName 文件夹名（显示用）
     * @param folderPath 文件夹绝对路径
     */
    fun insertFolderPathPlaceholder(folderName: String, folderPath: String) {
        val editor = editor as? EditorEx ?: return
        val basePath = project.basePath ?: ""
        
        runUndoTransparentWriteAction {
            // 显示的 label 文本，格式：文件夹名
            val label = " $folderName "
            
            // 插入 label 到当前光标位置
            val (start, end) = replaceSelectionOrInsert(editor, label)
            
            // 生成实际的 markdown 代码块格式的相对路径
            val relativePath = com.qifu.utils.toRelativePath(folderPath, basePath)
            val content = "`$relativePath`"
            
            // 添加占位符，关联实际的相对路径
            addEditorPlaceholder(editor, start, end, label, content)
            
            // 滚动到光标位置
            invokeLater {
                if (!editor.isDisposed) {
                    editor.scrollingModel.scrollToCaret(com.intellij.openapi.editor.ScrollType.MAKE_VISIBLE)
                }
            }
        }
    }

    /**
     * 移除 EditorSelectionTagDetails,但保持编辑器中的选中状态
     */
    private fun removeEditorSelectionTag() {
        // 查找所有 EditorSelectionTagDetails
        val editorSelectionTags = tagManager.getTags().filterIsInstance<EditorSelectionTagDetails>()
        // 移除所有找到的 EditorSelectionTagDetails
        editorSelectionTags.forEach { tag ->
            tagManager.remove(tag)
        }
    }

    suspend fun showGroupLookup() {
        val lookupItems = searchManager.getDefaultGroups()
            .map { it.createLookupElement() }
            .toTypedArray()

        withContext(Dispatchers.Main) {
            editor?.let { editor ->
                lookup = lookupManager.showGroupLookup(
                    editor = editor,
                    lookupElements = lookupItems,
                    onGroupSelected = { group, selectedText ->
                        handleGroupSelected(group, selectedText)
                    },
                    onWebActionSelected = { webAction ->
                        onLookupAdded(webAction)
                    },
                    onCodeAnalyzeSelected = { codeAnalyzeAction ->
                        onLookupAdded(codeAnalyzeAction)
                    }
                )
            }
        }
    }

    private fun showGlobalSearchResults(
        results: List<LookupItem>,
        searchText: String
    ) {
        editor?.let { editor ->
            try {
                hideLookupIfShown()
                lookup = lookupManager.showSearchResultsLookup(editor, results, searchText)
            } catch (e: Exception) {
                logger.error("Error showing lookup: $e", e)
            }
        }
    }

    internal fun handleGroupSelected(group: LookupGroupItem, searchText: String) {
        showSuggestionsJob?.cancel()
        showSuggestionsJob = coroutineScope.launch {
            showGroupSuggestions(group, searchText)
        }
    }

    private suspend fun showGroupSuggestions(group: LookupGroupItem, filterText: String = "") {
        val suggestions = group.getLookupItems()
        if (suggestions.isEmpty()) {
            return
        }

        val lookupElements = suggestions.map { it.createLookupElement() }.toTypedArray()

        withContext(Dispatchers.Main) {
            showSuggestionLookup(lookupElements, group, filterText)
        }
    }

    private fun showSuggestionLookup(
        lookupElements: Array<LookupElement>,
        parentGroup: LookupGroupItem,
        filterText: String = "",
    ) {
        editor?.let { editor ->
            searchState = searchState.copy(isInGroupLookupContext = true)

            lookup = lookupManager.showSuggestionLookup(
                editor = editor,
                lookupElements = lookupElements,
                parentGroup = parentGroup,
                onDynamicUpdate = { searchText ->
                    handleDynamicUpdate(parentGroup, lookupElements, searchText, filterText)
                },
                filterText = filterText
            )
            lookup?.let { UiUtil.enableSingleClickSelection(it) }

            lookup?.addLookupListener(object : LookupListener {
                override fun lookupCanceled(event: LookupEvent) {
                    searchState = searchState.copy(isInGroupLookupContext = false)
                }
            })
        }
    }

    private fun handleDynamicUpdate(
        parentGroup: LookupGroupItem,
        lookupElements: Array<LookupElement>,
        searchText: String,
        filterText: String
    ) {
        showSuggestionsJob?.cancel()
        showSuggestionsJob = coroutineScope.launch {
            if (parentGroup is DynamicLookupGroupItem) {
                if (searchText.length >= PromptTextFieldConstants.MIN_DYNAMIC_SEARCH_LENGTH) {
                    parentGroup.updateLookupList(lookup!!, searchText)
                } else if (searchText.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        showSuggestionLookup(lookupElements, parentGroup, filterText)
                    }
                }
            }
        }
    }

    override fun createEditor(): EditorEx {
        val editorEx = super.createEditor()
        editorEx.settings.isUseSoftWraps = true
        editorEx.backgroundColor = service<EditorColorsManager>().globalScheme.defaultBackground
        setupDocumentListener(editorEx)
        return editorEx
    }

    override fun updateBorder(editor: EditorEx) {
        editor.setBorder(
            JBUI.Borders.empty(
                PromptTextFieldConstants.BORDER_PADDING,
                PromptTextFieldConstants.BORDER_SIDE_PADDING
            )
        )
    }


    /**
     * IntelliJ 在 dispose(this) 时会自动清理 documentListener，
     * 因为注册时传入了 parentDisposable = this
     */
    override fun dispose() {
        showSuggestionsJob?.cancel()
        lastSearchResults = null
        clearPlaceholders()
        clearSlashHighlights()
        (editor as? EditorEx)?.let { clearSlashArgumentHighlights(it) }
        val ed = this.editor
        mouseClickListener?.let { l -> ed?.contentComponent?.removeMouseListener(l) }
        mouseMotionListener?.let { l -> ed?.contentComponent?.removeMouseMotionListener(l) }
    }

    fun insertPlaceholderFor(pastedText: String) {
        val editor = editor as? EditorEx ?: return
        if (pastedText.isEmpty()) return

        // 检查是否是当前激活Editor的选中内容
        val activeEditor = com.qifu.ui.utils.EditorUtil.getSelectedEditor(project)
        var needRemoveEditorSelection = false
        val isActiveEditorSelection = activeEditor?.let { activeEditorEx ->
            val selectedText = activeEditorEx.selectionModel.selectedText
            if (selectedText?.trim() == pastedText.trim()) {
                // 如果选中的只有一行，且当前行还有非选中的可见字符，则直接插入原文
                val document = activeEditorEx.document
                val startLine = document.getLineNumber(activeEditorEx.selectionModel.selectionStart)
                val endLine = document.getLineNumber(activeEditorEx.selectionModel.selectionEnd)
                if (startLine == endLine) {
                    val lineStart = document.getLineStartOffset(startLine)
                    val lineEnd = document.getLineEndOffset(startLine)
                    val lineText = document.getText(TextRange(lineStart, lineEnd))
                    if (selectedText.trim() != lineText.trim()) {
                        needRemoveEditorSelection = true
                        return@let false
                    }
                }
                true
            } else {
                false
            }
        } ?: false

        if (isActiveEditorSelection) {
            val virtualFile = activeEditor.virtualFile
            val fileName = virtualFile?.name ?: "Unknown"

            // 获取选中的行号范围
            val document = activeEditor.document
            val selectionModel = activeEditor.selectionModel
            val startLine = document.getLineNumber(selectionModel.selectionStart) + 1  // 行号从1开始
            val endLine = document.getLineNumber(selectionModel.selectionEnd) + 1
            val lineStartPos = document.getLineStartOffset(startLine - 1)
            val lineEndPos = document.getLineEndOffset(endLine - 1)
            val selectedCode = document.getText(TextRange(lineStartPos, lineEndPos))
            val codeBlock = CompletionRequestUtil.formatCodeBlock(
                virtualFile.path,
                project.basePath?:"",
                selectedCode,
                startLine,
                endLine
            )

            insertCodeSelectionPlaceholder(fileName, startLine, endLine, codeBlock)
            // 移除 EditorSelectionTagDetails,但保持编辑器中的选中状态
            removeEditorSelectionTag()
        } else {
            // 直接插入原文,不使用占位符
            runUndoTransparentWriteAction { 
                replaceSelectionOrInsert(editor, pastedText)
            }
            if (needRemoveEditorSelection) removeEditorSelectionTag()
        }
        
        // 粘贴后滚动到光标位置
        invokeLater {
            if (!editor.isDisposed) {
                editor.scrollingModel.scrollToCaret(com.intellij.openapi.editor.ScrollType.MAKE_VISIBLE)
            }
        }
    }

    private fun replaceSelectionOrInsert(editor: EditorEx, text: String): Pair<Int, Int> {
        val document = editor.document
        val caret = editor.caretModel
        val selectionModel = editor.selectionModel
        val start: Int
        if (selectionModel.hasSelection()) {
            val selectionStart = selectionModel.selectionStart
            val selectionEnd = selectionModel.selectionEnd
            document.replaceString(selectionStart, selectionEnd, text)
            selectionModel.removeSelection()
            start = selectionStart
        } else {
            start = caret.offset
            document.insertString(start, text)
        }
        val end = start + text.length
        caret.moveToOffset(end)
        return start to end
    }

    private fun addEditorPlaceholder(
        editor: EditorEx,
        start: Int,
        end: Int,
        label: String,
        content: String
    ) {
        val attrs = TextAttributes().apply {
            backgroundColor = JBColor(0xE3F2FD, 0x26374D)
            effectType = EffectType.ROUNDED_BOX
            effectColor = JBColor(0xC4C9D0, 0x44484F)
            foregroundColor = JBColor(0x26374D, 0xD5D6D8)
        }
        val highlighter = editor.markupModel.addRangeHighlighter(
            start,
            end,
            HighlighterLayer.ADDITIONAL_SYNTAX,
            attrs,
            HighlighterTargetArea.EXACT_RANGE
        )
        highlighter.isGreedyToLeft = false
        highlighter.isGreedyToRight = false
        placeholders.add(EditorPlaceholder(highlighter, label, content))
    }

    internal data class EditorPlaceholder(
        val highlighter: RangeHighlighter,
        val label: String,
        var content: String,
    )

    private val placeholders: MutableList<EditorPlaceholder> = mutableListOf()
    private val slashHighlighters: MutableList<RangeHighlighter> = mutableListOf()
    private val slashArgumentHighlighters: MutableList<RangeHighlighter> = mutableListOf()

    fun getExpandedText(): String {
        val text = document.text
        if (placeholders.isEmpty()) return text
        val validPlaceholders =
            placeholders.filter { it.highlighter.isValid }.sortedBy { it.highlighter.startOffset }
        if (validPlaceholders.isEmpty()) return text
        val result = StringBuilder()
        var cursor = 0
        for (placeholder in validPlaceholders) {
            val start = placeholder.highlighter.startOffset
            val end = placeholder.highlighter.endOffset
            if (start < cursor || start > text.length || end > text.length) continue
            if (cursor < start) result.append(text, cursor, start)
            val span = text.substring(start, end)
            if (span == placeholder.label) result.append(placeholder.content) else result.append(
                span
            )
            cursor = end
        }
        if (cursor < text.length) result.append(text.substring(cursor))
        return result.toString()
    }

    internal fun findPlaceholderAtOffset(offset: Int): EditorPlaceholder? {
        return placeholders.firstOrNull { ph ->
            ph.highlighter.isValid && offset >= ph.highlighter.startOffset && offset < ph.highlighter.endOffset
        }
    }

    private fun findPlaceholdersIntersecting(start: Int, end: Int): List<EditorPlaceholder> {
        return placeholders.filter { ph ->
            ph.highlighter.isValid && ph.highlighter.startOffset < end && ph.highlighter.endOffset > start
        }
    }

    private fun clearPlaceholders() {
        val ed = this.editor as? EditorEx ?: return
        if (ed.isDisposed) return
        placeholders.forEach { ph -> ed.markupModel.removeHighlighter(ph.highlighter) }
        placeholders.clear()
    }

    private fun refreshSlashHighlights() {
        val editorEx = editor as? EditorEx ?: return
        if (editorEx.isDisposed) return

        clearSlashHighlights(editorEx)
        clearSlashArgumentHighlights(editorEx)
        val slashRanges = findSlashCommandRanges(document.text)
        if (slashRanges.isEmpty()) return

        val attrs = TextAttributes().apply {
            backgroundColor = JBColor(0xEAF3FF, 0x23354D)
            effectType = EffectType.ROUNDED_BOX
            effectColor = JBColor(0xA9C4EB, 0x4D709A)
            foregroundColor = JBColor(0x0D47A1, 0xB8D8FF)
        }

        slashRanges.forEach { (start, end) ->
            val highlighter = editorEx.markupModel.addRangeHighlighter(
                start,
                end,
                HighlighterLayer.ADDITIONAL_SYNTAX + 1,
                attrs,
                HighlighterTargetArea.EXACT_RANGE
            )
            highlighter.isGreedyToLeft = false
            highlighter.isGreedyToRight = false
            slashHighlighters.add(highlighter)
        }

        val argumentRanges = findSlashArgumentPlaceholderRanges(document.text, slashRanges)
        if (argumentRanges.isEmpty()) return

        val argumentAttrs = TextAttributes().apply {
            backgroundColor = JBColor(0xFFF8E1, 0x4A3D1D)
            effectType = EffectType.ROUNDED_BOX
            effectColor = JBColor(0xF0C36D, 0xB0893E)
            foregroundColor = JBColor(0x8A5A00, 0xFFD180)
        }
        argumentRanges.forEach { (start, end) ->
            val highlighter = editorEx.markupModel.addRangeHighlighter(
                start,
                end,
                HighlighterLayer.ADDITIONAL_SYNTAX,
                argumentAttrs,
                HighlighterTargetArea.EXACT_RANGE
            )
            highlighter.isGreedyToLeft = false
            highlighter.isGreedyToRight = false
            slashArgumentHighlighters.add(highlighter)
        }
    }

    private fun clearSlashHighlights() {
        val editorEx = editor as? EditorEx ?: return
        clearSlashHighlights(editorEx)
    }

    private fun clearSlashHighlights(editorEx: EditorEx) {
        if (editorEx.isDisposed) return
        slashHighlighters.forEach { highlighter ->
            runCatching { editorEx.markupModel.removeHighlighter(highlighter) }
        }
        slashHighlighters.clear()
    }

    private fun clearSlashArgumentHighlights(editorEx: EditorEx) {
        if (editorEx.isDisposed) return
        slashArgumentHighlighters.forEach { highlighter ->
            runCatching { editorEx.markupModel.removeHighlighter(highlighter) }
        }
        slashArgumentHighlighters.clear()
    }

    private fun findSlashCommandRanges(text: String): List<Pair<Int, Int>> {
        if (text.isEmpty() || !text.contains(PromptTextFieldConstants.SLASH_SYMBOL)) return emptyList()

        val knownCommands = SlashCommandRegistry
            .getCommands(project, SlashCommandScope.ALL)
            .asSequence()
            .map { it.command.lowercase() }
            .toSet()
        if (knownCommands.isEmpty()) return emptyList()

        val ranges = mutableListOf<Pair<Int, Int>>()
        var cursor = 0
        while (cursor < text.length) {
            if (text[cursor] == PromptTextFieldConstants.SLASH_SYMBOL[0] && (cursor == 0 || text[cursor - 1] != '/')) {
                var end = cursor + 1
                while (end < text.length && isSlashTokenChar(text[end])) {
                    end++
                }

                if (end > cursor + 1) {
                    val token = text.substring(cursor, end).lowercase()
                    if (token in knownCommands) {
                        ranges.add(cursor to end)
                    }
                }
                cursor = end
                continue
            }
            cursor++
        }
        return ranges
    }

    private fun isSlashTokenChar(ch: Char): Boolean {
        return ch.isLetterOrDigit() || ch == '-' || ch == '_' || ch == ':' || ch == '.'
    }

    private fun findSlashArgumentPlaceholderRanges(
        text: String,
        slashRanges: List<Pair<Int, Int>>,
    ): List<Pair<Int, Int>> {
        val angleRegex = Regex("""<([^>\n\r]+)>""")
        val ranges = mutableListOf<Pair<Int, Int>>()
        slashRanges.forEach { (_, commandEnd) ->
            val lineEnd = findLineEndOffset(text, commandEnd)
            if (commandEnd >= lineEnd) {
                return@forEach
            }

            val lineSegment = text.substring(commandEnd, lineEnd)
            angleRegex.findAll(lineSegment).forEach { match ->
                val start = commandEnd + match.range.first + 1
                val end = commandEnd + match.range.last
                if (start < end) {
                    ranges.add(start to end)
                }
            }
        }
        return ranges.distinct()
    }

    private fun findLineEndOffset(text: String, startOffset: Int): Int {
        var cursor = startOffset.coerceAtLeast(0)
        while (cursor < text.length) {
            if (text[cursor] == '\n' || text[cursor] == '\r') {
                break
            }
            cursor++
        }
        return cursor
    }

    private fun findSlashHighlighterAtOffset(offset: Int): RangeHighlighter? {
        return slashHighlighters.firstOrNull { highlighter ->
            highlighter.isValid && offset >= highlighter.startOffset && offset < highlighter.endOffset
        }
    }

    private fun findSlashHighlightersIntersecting(start: Int, end: Int): List<RangeHighlighter> {
        return slashHighlighters.filter { highlighter ->
            highlighter.isValid && highlighter.startOffset < end && highlighter.endOffset > start
        }
    }

    fun handleSlashTokenDelete(isBackspace: Boolean): Boolean {
        val editorEx = editor as? EditorEx ?: return false
        if (slashHighlighters.isEmpty()) return false

        val document = editorEx.document
        val caretModel = editorEx.caretModel
        val selectionModel = editorEx.selectionModel

        if (selectionModel.hasSelection()) {
            val selectionStart = selectionModel.selectionStart
            val selectionEnd = selectionModel.selectionEnd
            val intersecting = findSlashHighlightersIntersecting(selectionStart, selectionEnd)
            if (intersecting.isNotEmpty()) {
                val deleteStart = minOf(selectionStart, intersecting.minOf { it.startOffset })
                val deleteEnd = maxOf(selectionEnd, intersecting.maxOf { it.endOffset })
                runUndoTransparentWriteAction {
                    document.deleteString(deleteStart, deleteEnd)
                    caretModel.moveToOffset(deleteStart)
                }
                intersecting.forEach { highlighter ->
                    runCatching { editorEx.markupModel.removeHighlighter(highlighter) }
                }
                slashHighlighters.removeAll(intersecting.toSet())
                selectionModel.removeSelection()
                return true
            }
            return false
        }

        val caretOffset = caretModel.offset
        val targetOffset = if (isBackspace) {
            if (caretOffset == 0) return false
            caretOffset - 1
        } else {
            if (caretOffset >= document.textLength) return false
            caretOffset
        }

        val targetHighlighter = findSlashHighlighterAtOffset(targetOffset) ?: return false
        val start = targetHighlighter.startOffset
        val end = targetHighlighter.endOffset

        runUndoTransparentWriteAction {
            document.deleteString(start, end)
            caretModel.moveToOffset(start)
        }
        runCatching { editorEx.markupModel.removeHighlighter(targetHighlighter) }
        slashHighlighters.remove(targetHighlighter)
        return true
    }

    fun handlePlaceholderDelete(isBackspace: Boolean): Boolean {
        val editor = editor as? EditorEx ?: return false
        val document = editor.document
        val caret = editor.caretModel
        val selectionModel = editor.selectionModel

        if (selectionModel.hasSelection()) {
            val selStart = selectionModel.selectionStart
            val selEnd = selectionModel.selectionEnd
            val intersecting = findPlaceholdersIntersecting(selStart, selEnd)
            if (intersecting.isNotEmpty()) {
                val newStart = minOf(selStart, intersecting.minOf { it.highlighter.startOffset })
                val newEnd = maxOf(selEnd, intersecting.maxOf { it.highlighter.endOffset })
                runUndoTransparentWriteAction {
                    document.deleteString(newStart, newEnd)
                    caret.moveToOffset(newStart)
                }
                intersecting.forEach { ph -> editor.markupModel.removeHighlighter(ph.highlighter) }
                placeholders.removeAll(intersecting.toSet())
                selectionModel.removeSelection()
                return true
            }
            return false
        }

        val offset = caret.offset
        val target = if (isBackspace) (if (offset > 0) offset - 1 else offset) else offset
        val placeholder = findPlaceholderAtOffset(target) ?: return false
        val start = placeholder.highlighter.startOffset
        val end = placeholder.highlighter.endOffset
        runUndoTransparentWriteAction {
            document.deleteString(start, end)
            caret.moveToOffset(start)
        }
        editor.markupModel.removeHighlighter(placeholder.highlighter)
        placeholders.remove(placeholder)
        return true
    }

    fun updateModelSupportImages(supportImage: Boolean){
        searchManager.setSupportImages(supportImage)
    }





    // -------------------------------
    // 文本监听逻辑
    // -------------------------------

    private fun setupDocumentListener(editor: EditorEx) {
        editor.document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val currentText = event.document.text
                
                // 检查字符数限制（20k字符）
                if (currentText.length > PromptTextFieldConstants.MAX_TEXT_LENGTH) {
                    // 使用 invokeLater 延迟执行文档修改,避免在 DocumentListener 中直接修改
                    invokeLater {
                        if (!editor.isDisposed) {
                            runUndoTransparentWriteAction {
                                val truncatedText = currentText.substring(0, PromptTextFieldConstants.MAX_TEXT_LENGTH)
                                editor.document.setText(truncatedText)
                                editor.caretModel.moveToOffset(truncatedText.length)
                            }
                            com.qihoo.finance.lowcode.common.util.NotifyUtils.notify(
                                "输入内容已达到最大限制 (${PromptTextFieldConstants.MAX_TEXT_LENGTH} 字符)",
                                com.intellij.notification.NotificationType.WARNING
                            )
                        }
                    }
                    return
                }
                val document = editor.document
                if (document.isInBulkUpdate) {
                    invokeLater {
                        if (!editor.isDisposed && !document.isInBulkUpdate) {
                            adjustHeight(editor)
                        }
                    }
                } else {
                    adjustHeight(editor)
                }

                onTextChanged(currentText)
                handleDocumentChange(event)
            }
        }, this)
    }

    private fun handleDocumentChange(event: DocumentEvent) {
        prunePlaceholders(event)
        refreshSlashHighlights()
        val text = event.document.text
        maybeAppendSpaceAfterMatchedSlash(event, text)
        val caretOffset = event.offset + event.newLength

        when {
            isAtSymbolTyped(event) -> handleAtSymbolTyped()
            isSlashSymbolTyped(event) -> handleSlashSymbolTyped(text)
            else -> handleTextChange(text, caretOffset)
        }
    }

    private fun maybeAppendSpaceAfterMatchedSlash(event: DocumentEvent, text: String) {
        if (event.oldLength != 0 || event.newLength == 0) {
            return
        }
        val insertedText = event.newFragment.toString()
        if (insertedText.any { it.isWhitespace() }) {
            return
        }

        val editorEx = editor as? EditorEx ?: return
        if (editorEx.isDisposed) return
        val insertedAtEnd = event.offset + event.newLength == text.length
        if (!insertedAtEnd) {
            return
        }

        val hasTailMatchedSlash = slashHighlighters.any { highlighter ->
            highlighter.isValid && highlighter.endOffset == text.length
        }

        val insertOffset = if (hasTailMatchedSlash) {
            text.length
        } else {
            val prefixBeforeInserted = text.substring(0, event.offset)
            if (findMatchedSlashCommandStart(prefixBeforeInserted, prefixBeforeInserted.length) != null) {
                event.offset
            } else {
                null
            }
        } ?: return

        invokeLater {
            if (editorEx.isDisposed) {
                return@invokeLater
            }
            runUndoTransparentWriteAction {
                val latestText = editorEx.document.text
                if (insertOffset < 0 || insertOffset > latestText.length) {
                    return@runUndoTransparentWriteAction
                }
                if (insertOffset < latestText.length && latestText[insertOffset].isWhitespace()) {
                    return@runUndoTransparentWriteAction
                }

                val latestPrefix = latestText.substring(0, insertOffset)
                if (findMatchedSlashCommandStart(latestPrefix, latestPrefix.length) == null) {
                    return@runUndoTransparentWriteAction
                }

                val latestCaret = editorEx.caretModel.offset
                editorEx.document.insertString(insertOffset, " ")
                if (latestCaret >= insertOffset) {
                    editorEx.caretModel.moveToOffset((latestCaret + 1).coerceAtMost(editorEx.document.textLength))
                }
            }
        }
    }

    private fun findMatchedSlashCommandStart(text: String, endOffset: Int): Int? {
        if (endOffset <= 0) return null

        val knownCommands = SlashCommandRegistry
            .getCommands(project, SlashCommandScope.ALL)
            .asSequence()
            .map { it.command.lowercase() }
            .sortedByDescending { it.length }
            .toList()
        if (knownCommands.isEmpty()) return null

        val lowerText = text.lowercase()
        knownCommands.forEach { command ->
            if (endOffset < command.length) {
                return@forEach
            }
            val start = endOffset - command.length
            if (lowerText.substring(start, endOffset) != command) {
                return@forEach
            }
            if (text[start] != PromptTextFieldConstants.SLASH_SYMBOL[0]) {
                return@forEach
            }
            if (start > 0 && text[start - 1] == PromptTextFieldConstants.SLASH_SYMBOL[0]) {
                return@forEach
            }
            return start
        }
        return null
    }

    private fun prunePlaceholders(event: DocumentEvent) {
        if (placeholders.isEmpty()) return

        val editor = editor as? EditorEx ?: return
        val document = event.document
        val textLength = document.textLength
        val placeholdersToRemove = mutableListOf<EditorPlaceholder>()
        for (placeholder in placeholders) {
            val highlighter = placeholder.highlighter
            if (!highlighter.isValid) {
                placeholdersToRemove.add(placeholder)
                continue
            }
            val start = highlighter.startOffset
            val end = highlighter.endOffset
            if (start < 0 || end > textLength || start >= end) {
                placeholdersToRemove.add(placeholder)
                continue
            }
            val span = try {
                document.charsSequence.subSequence(start, end).toString()
            } catch (_: Exception) {
                null
            }
            if (span == null || span != placeholder.label) {
                placeholdersToRemove.add(placeholder)
            }
        }
        if (placeholdersToRemove.isNotEmpty()) {
            try {
                placeholdersToRemove.forEach { placeholder ->
                    editor.markupModel.removeHighlighter(placeholder.highlighter)
                }
                placeholders.removeAll(placeholdersToRemove.toSet())
            } catch (e : Exception) {
                LOG.warn("Error removing placeholder", e)
            }
        }
    }

    private fun isAtSymbolTyped(event: DocumentEvent): Boolean {
        return PromptTextFieldConstants.AT_SYMBOL == event.newFragment.toString()
    }

    private fun isSlashSymbolTyped(event: DocumentEvent): Boolean {
        return PromptTextFieldConstants.SLASH_SYMBOL == event.newFragment.toString()
            && event.oldLength == 0
    }

    private fun handleAtSymbolTyped() {
        searchState = searchState.copy(
            isInSearchContext = true,
            lastSearchText = ""
        )

        showSuggestionsJob?.cancel()
        showSuggestionsJob = coroutineScope.launch {
            // 输入 @ 时,显示最近文件 + 分组菜单
            updateLookupWithSearchResults("")
        }
    }

    private fun handleSlashSymbolTyped(text: String) {
        val slashScope = resolveSlashLookupScope(text)
        val insertMode = when (slashScope) {
            SlashCommandScope.ALL -> SlashInsertMode.REPLACE_ALL_INPUT
            SlashCommandScope.SKILLS_ONLY -> SlashInsertMode.REPLACE_TOKEN_AT_CARET
        }

        showSuggestionsJob?.cancel()
        showSuggestionsJob = coroutineScope.launch {
            showSlashCommandLookup(slashScope, insertMode)
        }
    }

    private fun resolveSlashLookupScope(text: String): SlashCommandScope {
        return if (text.trim() == PromptTextFieldConstants.SLASH_SYMBOL) {
            SlashCommandScope.ALL
        } else {
            SlashCommandScope.SKILLS_ONLY
        }
    }

    private fun handleTextChange(text: String, caretOffset: Int) {
        val searchText = searchManager.getSearchTextAfterAt(text, caretOffset)

        when {
            searchText != null && searchText.isEmpty() -> handleEmptySearch()
            !searchText.isNullOrEmpty() -> handleNonEmptySearch(searchText)
            searchText == null -> handleNoSearch()
        }
    }

    private fun handleEmptySearch() {
        if (!searchState.isInSearchContext || searchState.lastSearchText != "") {
            searchState = searchState.copy(
                isInSearchContext = true,
                lastSearchText = "",
                isInGroupLookupContext = false
            )

            showSuggestionsJob?.cancel()
            showSuggestionsJob = coroutineScope.launch {
                // 空搜索时,显示最近文件 + 分组菜单
                updateLookupWithSearchResults("")
            }
        }
    }

    private fun handleNonEmptySearch(searchText: String) {
        if (!searchState.isInGroupLookupContext) {
//            if (!searchManager.matchesAnyDefaultGroup(searchText)) {
                if (!searchState.isInSearchContext || searchState.lastSearchText != searchText) {
                    searchState = searchState.copy(
                        isInSearchContext = true,
                        lastSearchText = searchText
                    )

                    showSuggestionsJob?.cancel()
                    showSuggestionsJob = coroutineScope.launch {
                        delay(PromptTextFieldConstants.SEARCH_DELAY_MS)
                        updateLookupWithSearchResults(searchText)
                    }
                }
//            }
        }
    }

    private fun handleNoSearch() {
        if (searchState.isInSearchContext) {
            searchState = SearchState()
            showSuggestionsJob?.cancel()
            hideLookupIfShown()
        }
    }

    private fun hideLookupIfShown() {
        lookup?.let { existingLookup ->
            if (!existingLookup.isLookupDisposed && existingLookup.isShown) {
                runInEdt { existingLookup.hide() }
            }
        }
    }




    private suspend fun updateLookupWithGroups() {
        val lookupItems = searchManager.getDefaultGroups()
            .map { it.createLookupElement() }
            .toTypedArray()

        withContext(Dispatchers.Main) {
            editor?.let { editor ->
                lookup?.let { existingLookup ->
                    if (existingLookup.isShown && !existingLookup.isLookupDisposed) {
                        existingLookup.hide()
                    }
                }

                lookup = lookupManager.showGroupLookup(
                    editor = editor,
                    lookupElements = lookupItems,
                    onGroupSelected = { group, currentSearchText ->
                        handleGroupSelected(
                            group,
                            currentSearchText
                        )
                    },
                    onWebActionSelected = { webAction -> onLookupAdded(webAction) },
                    onCodeAnalyzeSelected = { codeAnalyzeAction -> onLookupAdded(codeAnalyzeAction) },
                    searchText = ""
                )
            }
        }
    }

    private suspend fun showSlashCommandLookup(
        scope: SlashCommandScope,
        insertMode: SlashInsertMode,
    ) {
        val commands = withContext(Dispatchers.Default) {
            SlashCommandRegistry.getCommands(project, scope)
        }
        if (commands.isEmpty()) return

        val lookupItems = commands.map { SlashCommandActionItem(it) }.toTypedArray()
        withContext(Dispatchers.Main) {
            editor?.let { editor ->
                lookup?.let { existingLookup ->
                    if (existingLookup.isShown && !existingLookup.isLookupDisposed) {
                        existingLookup.hide()
                    }
                }
                lookup = lookupManager.showSlashCommandLookup(editor, lookupItems, insertMode)
            }
        }
    }

    private suspend fun updateLookupWithSearchResults(searchText: String) {
        val matchedResults = searchManager.performGlobalSearch(searchText)

        if (lastSearchResults != matchedResults) {
            lastSearchResults = matchedResults
            withContext(Dispatchers.Main) {
                showGlobalSearchResults(matchedResults, searchText)
            }
        }
    }


    // -------------------------------
    // UI 调整逻辑
    // -------------------------------

    private fun adjustHeight(editor: EditorEx) {
        val contentHeight =
            editor.contentComponent.preferredSize.height + PromptTextFieldConstants.HEIGHT_PADDING

        val toolWindow = project.service<ToolWindowManager>().getToolWindow("Jarvis")
        val maxHeight = if (toolWindow == null || !toolWindow.component.isAncestorOf(this)) {
            JBUI.scale(600)
        } else {
            JBUI.scale(getToolWindowHeight(toolWindow) / 2)
        }
        val newHeight = minOf(contentHeight, maxHeight)

        runInEdt {
            preferredSize = Dimension(width, newHeight)
            editor.setVerticalScrollbarVisible(contentHeight > maxHeight)
            parent?.revalidate()
        }
    }

    private fun getToolWindowHeight(toolWindow: ToolWindow): Int {
        return toolWindow.component.visibleRect?.height
            ?: PromptTextFieldConstants.DEFAULT_TOOL_WINDOW_HEIGHT
    }

    companion object {
        private val logger = thisLogger()
        private var pasteHandlerInstalled = false
        private var originalPasteHandler: EditorActionHandler? = null

        private fun installPasteHandler() {
            if (pasteHandlerInstalled) return
            synchronized(PromptTextField::class.java) {
                if (pasteHandlerInstalled) return
                val manager = EditorActionManager.getInstance()
                val existing = manager.getActionHandler(IdeActions.ACTION_EDITOR_PASTE)
                originalPasteHandler = existing
                manager.setActionHandler(
                    IdeActions.ACTION_EDITOR_PASTE,
                    object : EditorActionHandler() {
                        override fun doExecute(
                            editor: Editor,
                            caret: Caret?,
                            dataContext: DataContext
                        ) {
                            val field = editor.document.getUserData(PROMPT_FIELD_KEY)
                            if (field != null) {
                                val clipboard = CopyPasteManager.getInstance()
                                
                                val image = try {
                                    clipboard.getContents(DataFlavor.imageFlavor) as? java.awt.Image
                                } catch (_: Exception) {
                                    null
                                }
                                
                                if (image != null && field.onImagePasted != null) {
                                    field.onImagePasted.invoke(image)
                                    return
                                }
                                
                                val pasted = try {
                                    clipboard.getContents(DataFlavor.stringFlavor) as? String
                                } catch (_: Exception) {
                                    null
                                }
                                if (!pasted.isNullOrEmpty()) {
                                    field.insertPlaceholderFor(pasted)
                                    return
                                }
                            }
                            originalPasteHandler?.let { handler ->
                                val project = editor.project
                                CommandProcessor.getInstance().executeCommand(project, {
                                    handler.execute(editor, caret, dataContext)
                                }, "Paste", null)
                            }
                        }
                    })
                pasteHandlerInstalled = true
            }
        }
    }
}
