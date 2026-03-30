package com.miracle.ui.core.composer

import com.intellij.codeInsight.lookup.LookupEvent
import com.intellij.codeInsight.lookup.LookupListener
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupArranger
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.ui.ComponentUtil.findParentByCondition
import com.intellij.ui.EditorTextField
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.miracle.ui.core.ChatComposerInsertion
import com.miracle.ui.core.ChatTheme.INPUT_BACKGROUND
import com.miracle.utils.UiUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.AWTEvent
import java.awt.Component
import java.awt.Dimension
import java.awt.KeyboardFocusManager
import java.awt.datatransfer.DataFlavor
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.util.UUID
import javax.swing.KeyStroke

/**
 * 聊天输入框组件，支持斜杠命令自动补全、@引用文件/文件夹、
 * 占位符高亮以及代码引用插入等功能。
 *
 * @param project 当前 IntelliJ 项目实例
 * @param onSubmit 用户按下 Enter 提交输入时的回调
 */
class ChatComposerField(
    private val project: Project,
    private val onSubmit: () -> Unit,
) : EditorTextField(EditorFactory.getInstance().createDocument(""), project, FileTypes.PLAIN_TEXT), com.intellij.openapi.Disposable {

    /**
     * 编辑器中占位符的内部数据结构，用于表示文件引用、代码引用等内联标签。
     *
     * @property highlighter 占位符对应的文本高亮器
     * @property label 占位符在编辑器中显示的文本标签
     * @property content 占位符展开后的实际内容
     */
    private data class EditorPlaceholder(
        val highlighter: RangeHighlighter,
        val label: String,
        var content: String,
    )

    /**
     * 斜杠命令插入模式，用于确定选中命令后替换文本的范围。
     */
    private enum class SlashInsertMode {
        /** 替换整个输入框内容 */
        REPLACE_ALL_INPUT,
        /** 仅替换光标处的斜杠命令词元 */
        REPLACE_TOKEN_AT_CARET,
    }

    /** 协程作用域，用于管理异步任务 */
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    /** 搜索管理器，用于查找文件和文件夹引用 */
    private val searchManager = ChatComposerSearchManager(project)
    /** 当前所有活跃的占位符列表 */
    private val placeholders = mutableListOf<EditorPlaceholder>()
    /** 斜杠命令高亮器列表 */
    private val slashHighlighters = mutableListOf<RangeHighlighter>()
    /** 斜杠命令参数占位符高亮器列表 */
    private val slashArgumentHighlighters = mutableListOf<RangeHighlighter>()
    /** 事件分发器唯一标识，用于匹配当前输入框实例 */
    private val dispatcherId: UUID = UUID.randomUUID()

    /** 当前显示的自动补全弹窗实例 */
    private var lookup: LookupImpl? = null
    /** 延迟显示建议的协程任务 */
    private var showSuggestionsJob: Job? = null

    init {
        isOneLineMode = false
        minimumSize = Dimension(0, JBUI.scale(72))
        preferredSize = Dimension(0, JBUI.scale(72))
        maximumSize = Dimension(Int.MAX_VALUE, JBUI.scale(152))
        setPlaceholder("Ask anything... Use '/' for commands, '@' for references")
    }

    override fun onEditorAdded(editor: Editor) {
        IdeEventQueue.getInstance().addDispatcher(ChatComposerEventDispatcher(dispatcherId), this)
    }

    /**
     * 创建编辑器实例，配置软换行和背景色。
     *
     * @return 配置完成的编辑器实例
     */
    override fun createEditor(): EditorEx {
        val editorEx = super.createEditor()
        editorEx.settings.isUseSoftWraps = true
        editorEx.backgroundColor = INPUT_BACKGROUND
        setupDocumentListener(editorEx)
        return editorEx
    }

    override fun updateBorder(editor: EditorEx) {
        editor.setBorder(JBUI.Borders.empty(2, 8))
    }

    /**
     * 获取输入框中展开后的完整文本，将所有占位符替换为其对应的实际内容。
     *
     * @return 展开后的文本内容
     */
    fun expandedText(): String {
        val snapshots = placeholders.filter { it.highlighter.isValid }
            .map { placeholder ->
                PlaceholderSnapshot(
                    startOffset = placeholder.highlighter.startOffset,
                    endOffset = placeholder.highlighter.endOffset,
                    label = placeholder.label,
                    content = placeholder.content,
                )
            }
        return ChatComposerSupport.expandText(text, snapshots)
    }

    /**
     * 清空输入框内容，包括文本、所有占位符和斜杠命令高亮。
     */
    fun clearComposer() {
        hideLookupIfShown()
        text = ""
        clearPlaceholders()
        clearSlashHighlights()
        clearSlashArgumentHighlights()
    }

    /**
     * 根据插入类型将内容插入到输入框中。
     *
     * @param insertion 插入内容，可以是纯文本、代码引用或路径引用
     */
    fun applyInsertion(insertion: ChatComposerInsertion) {
        when (insertion) {
            is ChatComposerInsertion.PlainText -> appendPlainText(insertion.text)
            is ChatComposerInsertion.CodeReference -> {
                insertCodeReference(
                    fileName = java.io.File(insertion.filePath).name,
                    filePath = insertion.filePath,
                    startLine = insertion.startLine,
                    endLine = insertion.endLine,
                    selectedCode = insertion.code,
                )
            }
            is ChatComposerInsertion.PathReference -> {
                val displayName = java.io.File(insertion.path).name.ifBlank { insertion.path }
                if (insertion.directory) {
                    insertFolderReference(displayName, insertion.path)
                } else {
                    insertFileReference(displayName, insertion.path)
                }
            }
        }
    }

    /**
     * 在输入框末尾追加纯文本内容，自动在已有文本后添加换行分隔。
     *
     * @param value 要追加的文本
     */
    fun appendPlainText(value: String) {
        val normalized = value.trim()
        if (normalized.isBlank()) return
        val editor = editor as? EditorEx
        if (editor == null) {
            text = if (text.isBlank()) normalized else text.trimEnd() + "\n\n" + normalized
            return
        }
        runUndoTransparentWriteAction {
            val existingText = text.trim()
            val prefix = if (existingText.isBlank()) "" else "\n\n"
            val insertValue = if (editor.document.text.isBlank()) normalized else prefix + normalized
            replaceSelectionOrInsert(editor, insertValue)
        }
        requestComposerFocus()
    }

    /**
     * 在输入框中插入代码引用占位符，显示为高亮标签。
     *
     * @param fileName 文件名
     * @param filePath 文件完整路径
     * @param startLine 代码起始行号
     * @param endLine 代码结束行号
     * @param selectedCode 选中的代码内容
     */
    fun insertCodeReference(
        fileName: String,
        filePath: String,
        startLine: Int,
        endLine: Int,
        selectedCode: String,
    ) {
        val editor = editor as? EditorEx
        val label = " $fileName ($startLine-$endLine) "
        val content = ChatComposerSupport.formatCodeReference(filePath, selectedCode, startLine, endLine)
        if (editor == null) {
            appendPlainText(content.trim())
            return
        }
        runUndoTransparentWriteAction {
            val (start, end) = replaceSelectionOrInsert(editor, label)
            addPlaceholder(editor, start, end, label, content)
        }
        requestComposerFocus()
    }

    /**
     * 在输入框中插入文件引用占位符，显示为高亮标签。
     *
     * @param fileName 文件显示名称
     * @param filePath 文件完整路径
     */
    fun insertFileReference(fileName: String, filePath: String) {
        val editor = editor as? EditorEx
        val basePath = project.basePath ?: return
        val label = " $fileName "
        val content = ChatComposerSupport.formatPathReference(filePath, basePath)
        if (editor == null) {
            appendPlainText(content)
            return
        }
        runUndoTransparentWriteAction {
            val (start, end) = replaceSelectionOrInsert(editor, label)
            addPlaceholder(editor, start, end, label, content)
        }
        requestComposerFocus()
    }

    /**
     * 在输入框中插入文件夹引用占位符，显示为高亮标签。
     *
     * @param folderName 文件夹显示名称
     * @param folderPath 文件夹完整路径
     */
    fun insertFolderReference(folderName: String, folderPath: String) {
        val editor = editor as? EditorEx
        val basePath = project.basePath ?: return
        val label = " $folderName "
        val content = ChatComposerSupport.formatPathReference(folderPath, basePath)
        if (editor == null) {
            appendPlainText(content)
            return
        }
        runUndoTransparentWriteAction {
            val (start, end) = replaceSelectionOrInsert(editor, label)
            addPlaceholder(editor, start, end, label, content)
        }
        requestComposerFocus()
    }

    /**
     * 请求将焦点设置到输入框并将光标移动到文本末尾。
     */
    fun requestComposerFocus() {
        ApplicationManager.getApplication().invokeLater {
            requestFocusInWindow()
            editor?.caretModel?.moveToOffset(document.textLength)
        }
    }

    /**
     * 处理占位符的删除操作，支持退格键和删除键，以及选区删除。
     *
     * @param isBackspace 是否为退格键删除（true 为退格，false 为 Delete 键）
     * @return 是否成功处理了占位符删除
     */
    internal fun handlePlaceholderDelete(isBackspace: Boolean): Boolean {
        val editor = editor as? EditorEx ?: return false
        val document = editor.document
        val caret = editor.caretModel
        val selectionModel = editor.selectionModel

        if (selectionModel.hasSelection()) {
            val start = selectionModel.selectionStart
            val end = selectionModel.selectionEnd
            val intersecting = findPlaceholdersIntersecting(start, end)
            if (intersecting.isEmpty()) {
                return false
            }
            val deleteStart = minOf(start, intersecting.minOf { it.highlighter.startOffset })
            val deleteEnd = maxOf(end, intersecting.maxOf { it.highlighter.endOffset })
            runUndoTransparentWriteAction {
                document.deleteString(deleteStart, deleteEnd)
                caret.moveToOffset(deleteStart)
            }
            intersecting.forEach { editor.markupModel.removeHighlighter(it.highlighter) }
            placeholders.removeAll(intersecting.toSet())
            selectionModel.removeSelection()
            return true
        }

        val offset = caret.offset
        val target = if (isBackspace) {
            if (offset == 0) return false
            offset - 1
        } else {
            if (offset >= document.textLength) return false
            offset
        }
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

    /**
     * 检查自动补全弹窗是否当前可见。
     *
     * @return 弹窗是否可见且未被销毁
     */
    internal fun isLookupVisible(): Boolean {
        return lookup?.let { it.isShown && !it.isLookupDisposed } == true
    }

    /**
     * 为编辑器设置文档变更监听器，处理 @引用、/命令的自动补全触发以及占位符维护。
     *
     * @param editor 目标编辑器实例
     */
    private fun setupDocumentListener(editor: EditorEx) {
        editor.document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                prunePlaceholders(event)
                val text = event.document.text
                refreshSlashHighlights()
                maybeAppendSpaceAfterMatchedSlash(event, text)
                val caretOffset = (event.offset + event.newLength).coerceIn(0, text.length)
                when {
                    event.oldLength == 0 && event.newFragment.toString() == "@" -> showReferenceLookup("")
                    event.oldLength == 0 && event.newFragment.toString() == "/" -> {
                        showSlashLookup(resolveSlashLookupScope(text, caretOffset), "/")
                    }
                    else -> handleTextChange(text, caretOffset)
                }
            }
        }, this)
    }

    /**
     * 处理文本变更事件，根据当前输入内容决定显示引用补全还是斜杠命令补全。
     *
     * @param text 当前文档完整文本
     * @param caretOffset 当前光标位置
     */
    private fun handleTextChange(text: String, caretOffset: Int) {
        val searchText = ChatComposerSupport.findSearchTextAfterAt(text, caretOffset)
        val slashSearchText = ChatComposerSupport.findSlashSearchText(text, caretOffset)
        when {
            searchText != null -> {
                showSuggestionsJob?.cancel()
                showSuggestionsJob = coroutineScope.launch {
                    if (searchText.isNotEmpty()) {
                        delay(200)
                    }
                    showReferenceLookup(searchText)
                }
            }
            slashSearchText != null -> {
                if (isLookupVisible()) {
                    return
                }
                showSuggestionsJob?.cancel()
                showSuggestionsJob = coroutineScope.launch {
                    showSlashLookup(resolveSlashLookupScope(text, caretOffset), slashSearchText)
                }
            }
            else -> {
                showSuggestionsJob?.cancel()
                hideLookupIfShown()
            }
        }
    }

    /**
     * 根据当前文本和光标位置解析斜杠命令查找的范围。
     *
     * @param text 当前文档文本
     * @param caretOffset 光标位置
     * @return 斜杠命令查找范围
     */
    private fun resolveSlashLookupScope(text: String, caretOffset: Int): SlashCommandScope {
        return ChatComposerSupport.resolveSlashLookupScope(text, caretOffset)
    }

    /**
     * 显示文件/文件夹引用的自动补全弹窗，搜索匹配的文件和文件夹。
     *
     * @param searchText 搜索关键词，为空时显示默认推荐项
     */
    private fun showReferenceLookup(searchText: String) {
        showSuggestionsJob?.cancel()
        showSuggestionsJob = coroutineScope.launch {
            val results = searchManager.searchReferences(searchText)
            val lookupElements = results.map { it.createLookupElement() }.toTypedArray()
            ApplicationManager.getApplication().invokeLater {
                val editor = editor ?: return@invokeLater
                if (lookupElements.isEmpty()) {
                    hideLookupIfShown()
                    return@invokeLater
                }
                val lookup = createLookup(editor, lookupElements, "")
                UiUtil.enableSingleClickSelection(lookup)
                lookup.addLookupListener(object : LookupListener {
                    override fun itemSelected(event: LookupEvent) {
                        val item = event.item?.getUserData(ChatComposerLookupItem.KEY) as? ChatComposerLookupActionItem ?: return
                        val tokenRange = ChatComposerSupport.findAtTokenRange(editor.document.text, editor.caretModel.offset)
                        if (tokenRange != null) {
                            runUndoTransparentWriteAction {
                                editor.document.deleteString(tokenRange.startOffset, tokenRange.endOffset)
                                editor.caretModel.moveToOffset(tokenRange.startOffset)
                            }
                        }
                        item.execute(this@ChatComposerField)
                    }
                })
                showLookup(lookup)
            }
        }
    }

    /**
     * 显示斜杠命令的自动补全弹窗，列出匹配的命令列表。
     *
     * @scope 斜杠命令查找范围
     * @param prefix 当前已输入的斜杠命令前缀
     */
    private fun showSlashLookup(scope: SlashCommandScope, prefix: String) {
        showSuggestionsJob?.cancel()
        showSuggestionsJob = coroutineScope.launch {
            val commands = SlashCommandRegistry.getCommands(project, scope)
            val lookupItems = commands.map { SlashCommandLookupItem(it) }
            val lookupElements = lookupItems.map { it.createLookupElement() }.toTypedArray()
            val insertMode = when (scope) {
                SlashCommandScope.ALL -> SlashInsertMode.REPLACE_ALL_INPUT
                SlashCommandScope.SKILLS_ONLY -> SlashInsertMode.REPLACE_TOKEN_AT_CARET
            }
            ApplicationManager.getApplication().invokeLater {
                val editor = editor ?: return@invokeLater
                if (lookupElements.isEmpty()) {
                    hideLookupIfShown()
                    return@invokeLater
                }
                val lookup = createLookup(editor, lookupElements, prefix)
                UiUtil.enableSingleClickSelection(lookup)
                lookup.addLookupListener(object : LookupListener {
                    override fun itemSelected(event: LookupEvent) {
                        val item = event.item?.getUserData(ChatComposerLookupItem.KEY) as? SlashCommandLookupItem ?: return
                        val invocationText = ChatComposerSupport.buildSlashInvocationText(item.command, appendTrailingSpace = true)
                        runUndoTransparentWriteAction {
                            val selectionRange = when (insertMode) {
                                SlashInsertMode.REPLACE_ALL_INPUT -> {
                                    editor.document.setText(invocationText)
                                    editor.caretModel.moveToOffset(invocationText.length)
                                    ChatComposerSupport.firstPlaceholderSelectionRange(0, invocationText)
                                }
                                SlashInsertMode.REPLACE_TOKEN_AT_CARET -> {
                                    val (updatedText, insertStart) = ChatComposerSupport.replaceSlashToken(
                                        editor.document.text,
                                        editor.caretModel.offset,
                                        invocationText,
                                    )
                                    editor.document.setText(updatedText)
                                    editor.caretModel.moveToOffset((insertStart + invocationText.length).coerceAtMost(updatedText.length))
                                    ChatComposerSupport.firstPlaceholderSelectionRange(insertStart, invocationText)
                                }
                            }
                            if (selectionRange != null) {
                                editor.selectionModel.setSelection(selectionRange.startOffset, selectionRange.endOffset)
                                editor.caretModel.moveToOffset(selectionRange.endOffset)
                            } else {
                                editor.selectionModel.removeSelection()
                            }
                        }
                    }
                })
                showLookup(lookup)
            }
        }
    }

    /**
     * 创建自动补全弹窗实例。
     *
     * @param editor 关联的编辑器
     * @param lookupElements 补全候选元素数组
     * @param prefix 已输入的前缀文本
     * @return 创建的 LookupImpl 实例
     */
    private fun createLookup(
        editor: Editor,
        lookupElements: Array<LookupElement>,
        prefix: String,
    ): LookupImpl {
        return LookupManager.getInstance(project).createLookup(
            editor,
            lookupElements,
            prefix,
            LookupArranger.DefaultArranger(),
        ) as LookupImpl
    }

    /**
     * 显示自动补全弹窗，隐藏已有的弹窗。
     *
     * @param newLookup 要显示的新弹窗实例
     */
    private fun showLookup(newLookup: LookupImpl) {
        hideLookupIfShown()
        lookup = newLookup
        newLookup.refreshUi(false, true)
        newLookup.showLookup()
    }

    /**
     * 隐藏当前显示的自动补全弹窗。
     */
    private fun hideLookupIfShown() {
        lookup?.let { existingLookup ->
            if (!existingLookup.isLookupDisposed && existingLookup.isShown) {
                existingLookup.hide()
            }
        }
        lookup = null
    }

    /**
     * 替换当前选中文本或在光标位置插入新文本。
     *
     * @param editor 编辑器实例
     * @param value 要插入的文本
     * @return 插入文本的起始和结束位置对
     */
    private fun replaceSelectionOrInsert(editor: EditorEx, value: String): Pair<Int, Int> {
        val document = editor.document
        val caret = editor.caretModel
        val selectionModel = editor.selectionModel
        val start = if (selectionModel.hasSelection()) {
            val selectionStart = selectionModel.selectionStart
            document.replaceString(selectionStart, selectionModel.selectionEnd, value)
            selectionModel.removeSelection()
            selectionStart
        } else {
            val offset = caret.offset
            document.insertString(offset, value)
            offset
        }
        val end = start + value.length
        caret.moveToOffset(end)
        return start to end
    }

    /**
     * 在编辑器中添加占位符高亮，用于显示文件引用、代码引用等内联标签。
     *
     * @param editor 编辑器实例
     * @param start 占位符起始偏移量
     * @param end 占位符结束偏移量
     * @param label 占位符显示文本
     * @param content 占位符展开后的实际内容
     */
    private fun addPlaceholder(
        editor: EditorEx,
        start: Int,
        end: Int,
        label: String,
        content: String,
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
            HighlighterTargetArea.EXACT_RANGE,
        )
        highlighter.isGreedyToLeft = false
        highlighter.isGreedyToRight = false
        placeholders.add(EditorPlaceholder(highlighter, label, content))
    }

    /**
     * 查找指定偏移量处的占位符。
     *
     * @param offset 目标偏移量
     * @return 匹配的占位符，未找到时返回 null
     */
    private fun findPlaceholderAtOffset(offset: Int): EditorPlaceholder? {
        return placeholders.firstOrNull { placeholder ->
            placeholder.highlighter.isValid &&
                offset >= placeholder.highlighter.startOffset &&
                offset < placeholder.highlighter.endOffset
        }
    }

    /**
     * 查找与指定范围相交的所有占位符。
     *
     * @param start 范围起始偏移量
     * @param end 范围结束偏移量
     * @return 相交的占位符列表
     */
    private fun findPlaceholdersIntersecting(start: Int, end: Int): List<EditorPlaceholder> {
        return placeholders.filter { placeholder ->
            placeholder.highlighter.isValid &&
                placeholder.highlighter.startOffset < end &&
                placeholder.highlighter.endOffset > start
        }
    }

    /**
     * 清除所有占位符及其高亮。
     */
    private fun clearPlaceholders() {
        val editor = editor as? EditorEx ?: return
        if (editor.isDisposed) return
        placeholders.forEach { placeholder ->
            runCatching { editor.markupModel.removeHighlighter(placeholder.highlighter) }
        }
        placeholders.clear()
    }

    /**
     * 刷新文档中的斜杠命令高亮，清除旧高亮并根据当前文本重新标记。
     */
    private fun refreshSlashHighlights() {
        val editorEx = editor as? EditorEx ?: return
        if (editorEx.isDisposed) return

        clearSlashHighlights(editorEx)
        clearSlashArgumentHighlights(editorEx)
        val knownCommands = SlashCommandRegistry.getCommands(project, SlashCommandScope.ALL)
            .map { it.command.lowercase() }
            .toSet()
        val slashRanges = ChatComposerSupport.findMatchedSlashCommandRanges(document.text, knownCommands)
        if (slashRanges.isEmpty()) return

        val attrs = TextAttributes().apply {
            backgroundColor = JBColor(0xEAF3FF, 0x23354D)
            effectType = EffectType.ROUNDED_BOX
            effectColor = JBColor(0xA9C4EB, 0x4D709A)
            foregroundColor = JBColor(0x0D47A1, 0xB8D8FF)
        }
        slashRanges.forEach { range ->
            val highlighter = editorEx.markupModel.addRangeHighlighter(
                range.startOffset,
                range.endOffset,
                HighlighterLayer.ADDITIONAL_SYNTAX + 1,
                attrs,
                HighlighterTargetArea.EXACT_RANGE,
            )
            highlighter.isGreedyToLeft = false
            highlighter.isGreedyToRight = false
            slashHighlighters += highlighter
        }

        val argumentRanges = ChatComposerSupport.findSlashArgumentPlaceholderRanges(document.text, slashRanges)
        if (argumentRanges.isEmpty()) return

        val argumentAttrs = TextAttributes().apply {
            backgroundColor = JBColor(0xFFF8E1, 0x4A3D1D)
            effectType = EffectType.ROUNDED_BOX
            effectColor = JBColor(0xF0C36D, 0xB0893E)
            foregroundColor = JBColor(0x8A5A00, 0xFFD180)
        }
        argumentRanges.forEach { range ->
            val highlighter = editorEx.markupModel.addRangeHighlighter(
                range.startOffset,
                range.endOffset,
                HighlighterLayer.ADDITIONAL_SYNTAX,
                argumentAttrs,
                HighlighterTargetArea.EXACT_RANGE,
            )
            highlighter.isGreedyToLeft = false
            highlighter.isGreedyToRight = false
            slashArgumentHighlighters += highlighter
        }
    }

    /**
     * 清除所有斜杠命令高亮（无参版本，自动获取编辑器实例）。
     */
    private fun clearSlashHighlights() {
        val editorEx = editor as? EditorEx ?: return
        clearSlashHighlights(editorEx)
    }

    /**
     * 清除指定编辑器上的所有斜杠命令高亮。
     *
     * @param editorEx 目标编辑器
     */
    private fun clearSlashHighlights(editorEx: EditorEx) {
        if (editorEx.isDisposed) return
        slashHighlighters.forEach { highlighter ->
            runCatching { editorEx.markupModel.removeHighlighter(highlighter) }
        }
        slashHighlighters.clear()
    }

    /**
     * 清除所有斜杠命令参数占位符高亮（无参版本，自动获取编辑器实例）。
     */
    private fun clearSlashArgumentHighlights() {
        val editorEx = editor as? EditorEx ?: return
        clearSlashArgumentHighlights(editorEx)
    }

    /**
     * 清除指定编辑器上的所有斜杠命令参数占位符高亮。
     *
     * @param editorEx 目标编辑器
     */
    private fun clearSlashArgumentHighlights(editorEx: EditorEx) {
        if (editorEx.isDisposed) return
        slashArgumentHighlighters.forEach { highlighter ->
            runCatching { editorEx.markupModel.removeHighlighter(highlighter) }
        }
        slashArgumentHighlighters.clear()
    }

    /**
     * 在匹配到斜杠命令后自动追加空格，提升输入体验。
     *
     * @param event 文档变更事件
     * @param text 当前文档文本
     */
    private fun maybeAppendSpaceAfterMatchedSlash(event: DocumentEvent, text: String) {
        if (event.oldLength != 0 || event.newLength == 0) return
        val insertedText = event.newFragment.toString()
        if (insertedText.any { it.isWhitespace() }) return

        val editorEx = editor as? EditorEx ?: return
        if (editorEx.isDisposed) return

        val knownCommands = SlashCommandRegistry.getCommands(project, SlashCommandScope.ALL)
            .map { it.command.lowercase() }
            .sortedByDescending { it.length }

        val hasTailMatchedSlash = slashHighlighters.any { highlighter ->
            highlighter.isValid && highlighter.endOffset == text.length
        }
        val insertOffset = if (hasTailMatchedSlash) {
            text.length
        } else {
            val prefixBeforeInserted = text.substring(0, event.offset)
            ChatComposerSupport.findMatchedSlashCommandStart(
                prefixBeforeInserted,
                prefixBeforeInserted.length,
                knownCommands,
            )?.let { event.offset }
        } ?: return

        ApplicationManager.getApplication().invokeLater {
            if (editorEx.isDisposed) return@invokeLater
            runUndoTransparentWriteAction {
                val latestText = editorEx.document.text
                if (insertOffset < 0 || insertOffset > latestText.length) return@runUndoTransparentWriteAction
                if (insertOffset < latestText.length && latestText[insertOffset].isWhitespace()) return@runUndoTransparentWriteAction
                val latestPrefix = latestText.substring(0, insertOffset)
                if (
                    ChatComposerSupport.findMatchedSlashCommandStart(
                        latestPrefix,
                        latestPrefix.length,
                        knownCommands,
                    ) == null
                ) return@runUndoTransparentWriteAction

                val latestCaret = editorEx.caretModel.offset
                editorEx.document.insertString(insertOffset, " ")
                if (latestCaret >= insertOffset) {
                    editorEx.caretModel.moveToOffset((latestCaret + 1).coerceAtMost(editorEx.document.textLength))
                }
            }
        }
    }

    /**
     * 查找指定偏移量处的斜杠命令高亮器。
     *
     * @param offset 目标偏移量
     * @return 匹配的高亮器，未找到时返回 null
     */
    private fun findSlashHighlighterAtOffset(offset: Int): RangeHighlighter? {
        return slashHighlighters.firstOrNull { highlighter ->
            highlighter.isValid && offset >= highlighter.startOffset && offset < highlighter.endOffset
        }
    }

    /**
     * 查找与指定范围相交的所有斜杠命令高亮器。
     *
     * @param start 范围起始偏移量
     * @param end 范围结束偏移量
     * @return 相交的高亮器列表
     */
    private fun findSlashHighlightersIntersecting(start: Int, end: Int): List<RangeHighlighter> {
        return slashHighlighters.filter { highlighter ->
            highlighter.isValid && highlighter.startOffset < end && highlighter.endOffset > start
        }
    }

    /**
     * 处理斜杠命令高亮词元的删除操作，删除时整个命令词元会被整体移除。
     *
     * @param isBackspace 是否为退格键删除
     * @return 是否成功处理了删除操作
     */
    internal fun handleSlashTokenDelete(isBackspace: Boolean): Boolean {
        val editorEx = editor as? EditorEx ?: return false
        if (slashHighlighters.isEmpty()) return false

        val document = editorEx.document
        val caret = editorEx.caretModel
        val selectionModel = editorEx.selectionModel

        if (selectionModel.hasSelection()) {
            val start = selectionModel.selectionStart
            val end = selectionModel.selectionEnd
            val intersecting = findSlashHighlightersIntersecting(start, end)
            if (intersecting.isEmpty()) return false

            val deleteStart = minOf(start, intersecting.minOf { it.startOffset })
            val deleteEnd = maxOf(end, intersecting.maxOf { it.endOffset })
            runUndoTransparentWriteAction {
                document.deleteString(deleteStart, deleteEnd)
                caret.moveToOffset(deleteStart)
            }
            intersecting.forEach { highlighter ->
                runCatching { editorEx.markupModel.removeHighlighter(highlighter) }
            }
            slashHighlighters.removeAll(intersecting.toSet())
            selectionModel.removeSelection()
            return true
        }

        val caretOffset = caret.offset
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
            caret.moveToOffset(start)
        }
        runCatching { editorEx.markupModel.removeHighlighter(targetHighlighter) }
        slashHighlighters.remove(targetHighlighter)
        return true
    }

    /**
     * 清理失效或文本不匹配的占位符，保持占位符列表与文档内容一致。
     *
     * @param event 文档变更事件
     */
    private fun prunePlaceholders(event: DocumentEvent) {
        if (placeholders.isEmpty()) return
        val editor = editor as? EditorEx ?: return
        val document = event.document
        val textLength = document.textLength
        val toRemove = mutableListOf<EditorPlaceholder>()
        for (placeholder in placeholders) {
            val highlighter = placeholder.highlighter
            if (!highlighter.isValid) {
                toRemove.add(placeholder)
                continue
            }
            val start = highlighter.startOffset
            val end = highlighter.endOffset
            if (start < 0 || end > textLength || start >= end) {
                toRemove.add(placeholder)
                continue
            }
            val span = runCatching { document.charsSequence.subSequence(start, end).toString() }.getOrNull()
            if (span != placeholder.label) {
                toRemove.add(placeholder)
            }
        }
        if (toRemove.isEmpty()) return
        toRemove.forEach { placeholder ->
            runCatching { editor.markupModel.removeHighlighter(placeholder.highlighter) }
        }
        placeholders.removeAll(toRemove.toSet())
    }

    /**
     * 执行粘贴操作，当剪贴板内容与当前编辑器选区匹配时自动插入为代码引用。
     */
    private fun insertSelectionAwarePaste() {
        val clipText = try {
            CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor) as? String
        } catch (_: Exception) {
            null
        } ?: return

        val selectedEditor = FileEditorManager.getInstance(project).selectedTextEditor
        val virtualFile = selectedEditor?.document?.let { FileDocumentManager.getInstance().getFile(it) }
        val selectionModel = selectedEditor?.selectionModel
        if (
            selectedEditor != null &&
            virtualFile != null &&
            selectionModel != null &&
            selectionModel.hasSelection() &&
            selectionModel.selectedText?.trim() == clipText.trim()
        ) {
            val document = selectedEditor.document
            val startLine = document.getLineNumber(selectionModel.selectionStart)
            val endLine = document.getLineNumber(selectionModel.selectionEnd)
            val startOffset = document.getLineStartOffset(startLine)
            val endOffset = document.getLineEndOffset(endLine)
            val fullLineText = document.getText(TextRange(startOffset, endOffset))
            insertCodeReference(
                fileName = virtualFile.name,
                filePath = virtualFile.path,
                startLine = startLine + 1,
                endLine = endLine + 1,
                selectedCode = fullLineText,
            )
            return
        }

        val editor = editor as? EditorEx ?: return
        runUndoTransparentWriteAction {
            replaceSelectionOrInsert(editor, clipText)
        }
    }

    /**
     * 释放资源，隐藏弹窗、清除高亮并取消协程任务。
     */
    override fun dispose() {
        hideLookupIfShown()
        clearPlaceholders()
        clearSlashHighlights()
        clearSlashArgumentHighlights()
        showSuggestionsJob?.cancel()
        coroutineScope.cancel()
    }

    /**
     * 聊天输入框的事件分发器，负责拦截键盘和鼠标事件，
     * 处理占位符删除、斜杠命令词元删除、粘贴和提交等操作。
     *
     * @param targetDispatcherId 目标输入框的唯一标识，用于匹配事件
     */
    private inner class ChatComposerEventDispatcher(
        private val targetDispatcherId: UUID,
    ) : IdeEventQueue.EventDispatcher {

        /**
         * 分发并处理 AWT 事件，拦截特定按键以实现自定义行为。
         *
         * @param event AWT 事件
         * @return 是否已消费该事件
         */
        override fun dispatch(event: AWTEvent): Boolean {
            if ((event is KeyEvent || event is MouseEvent) && findFocusedComposer() != null) {
                if (event is KeyEvent) {
                    if (event.id == KeyEvent.KEY_TYPED) {
                        if (handleLookupInterception(event)) {
                            return true
                        }
                    } else if (event.id == KeyEvent.KEY_PRESSED) {
                        when (event.keyCode) {
                            KeyEvent.VK_BACK_SPACE -> {
                                if (handleSlashTokenDelete(isBackspace = true)) {
                                    event.consume()
                                    return true
                                }
                                if (handlePlaceholderDelete(isBackspace = true)) {
                                    event.consume()
                                    return true
                                }
                            }
                            KeyEvent.VK_DELETE -> {
                                if (handleSlashTokenDelete(isBackspace = false)) {
                                    event.consume()
                                    return true
                                }
                                if (handlePlaceholderDelete(isBackspace = false)) {
                                    event.consume()
                                    return true
                                }
                            }
                            KeyEvent.VK_V -> {
                                if (event.isControlDown || event.isMetaDown) {
                                    insertSelectionAwarePaste()
                                    event.consume()
                                    return true
                                }
                            }
                            KeyEvent.VK_ENTER -> {
                                if (isLookupVisible()) {
                                    return false
                                }
                                if (!event.isControlDown && !event.isAltDown && !event.isShiftDown) {
                                    onSubmit()
                                    event.consume()
                                    return true
                                }
                            }
                        }
                    }
                }
            }
            return false
        }

        /**
         * 查找当前获得焦点的 ChatComposerField 实例。
         *
         * @return 匹配的组件实例，未找到时返回 null
         */
        private fun findFocusedComposer(): Component? {
            return findParentByCondition(KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner) { component ->
                component is ChatComposerField && component.dispatcherId == targetDispatcherId
            }
        }

        /**
         * 处理自动补全弹窗中的字符拦截（句点和空格），自动插入字符并保持输入连贯性。
         *
         * @param event 键盘事件
         * @return 是否已处理该事件
         */
        private fun handleLookupInterception(event: KeyEvent): Boolean {
            if (!isLookupVisible()) {
                return false
            }
            val char = event.keyChar
            if (char != '.' && char != ' ') {
                return false
            }
            val editor = editor as? EditorEx ?: return false
            runUndoTransparentWriteAction {
                val offset = editor.caretModel.offset
                editor.document.insertString(offset, char.toString())
                editor.caretModel.moveToOffset(offset + 1)
            }
            event.consume()
            return true
        }
    }
}
