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

class ChatComposerField(
    private val project: Project,
    private val onSubmit: () -> Unit,
) : EditorTextField(EditorFactory.getInstance().createDocument(""), project, FileTypes.PLAIN_TEXT), com.intellij.openapi.Disposable {

    private data class EditorPlaceholder(
        val highlighter: RangeHighlighter,
        val label: String,
        var content: String,
    )

    private enum class SlashInsertMode {
        REPLACE_ALL_INPUT,
        REPLACE_TOKEN_AT_CARET,
    }

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val searchManager = ChatComposerSearchManager(project)
    private val placeholders = mutableListOf<EditorPlaceholder>()
    private val slashHighlighters = mutableListOf<RangeHighlighter>()
    private val slashArgumentHighlighters = mutableListOf<RangeHighlighter>()
    private val dispatcherId: UUID = UUID.randomUUID()

    private var lookup: LookupImpl? = null
    private var showSuggestionsJob: Job? = null

    init {
        isOneLineMode = false
        minimumSize = Dimension(0, JBUI.scale(112))
        preferredSize = Dimension(0, JBUI.scale(112))
        maximumSize = Dimension(Int.MAX_VALUE, JBUI.scale(180))
        setPlaceholder("Ask anything... Use '/' for commands, '@' for references")
    }

    override fun onEditorAdded(editor: Editor) {
        IdeEventQueue.getInstance().addDispatcher(ChatComposerEventDispatcher(dispatcherId), this)
    }

    override fun createEditor(): EditorEx {
        val editorEx = super.createEditor()
        editorEx.settings.isUseSoftWraps = true
        editorEx.backgroundColor = INPUT_BACKGROUND
        setupDocumentListener(editorEx)
        return editorEx
    }

    override fun updateBorder(editor: EditorEx) {
        editor.setBorder(JBUI.Borders.empty(5, 8))
    }

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

    fun clearComposer() {
        hideLookupIfShown()
        text = ""
        clearPlaceholders()
        clearSlashHighlights()
        clearSlashArgumentHighlights()
    }

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

    fun requestComposerFocus() {
        ApplicationManager.getApplication().invokeLater {
            requestFocusInWindow()
            editor?.caretModel?.moveToOffset(document.textLength)
        }
    }

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

    internal fun isLookupVisible(): Boolean {
        return lookup?.let { it.isShown && !it.isLookupDisposed } == true
    }

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
                        showSlashLookup(resolveSlashLookupScope(text), "/")
                    }
                    else -> handleTextChange(text, caretOffset)
                }
            }
        }, this)
    }

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
                    showSlashLookup(resolveSlashLookupScope(text), slashSearchText)
                }
            }
            else -> {
                showSuggestionsJob?.cancel()
                hideLookupIfShown()
            }
        }
    }

    private fun resolveSlashLookupScope(text: String): SlashCommandScope {
        return if (text.trim() == "/") {
            SlashCommandScope.ALL
        } else {
            SlashCommandScope.SKILLS_ONLY
        }
    }

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
                        val invocationText = ChatComposerSupport.buildSlashInvocationText(item.command)
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

    private fun showLookup(newLookup: LookupImpl) {
        hideLookupIfShown()
        lookup = newLookup
        newLookup.refreshUi(false, true)
        newLookup.showLookup()
    }

    private fun hideLookupIfShown() {
        lookup?.let { existingLookup ->
            if (!existingLookup.isLookupDisposed && existingLookup.isShown) {
                existingLookup.hide()
            }
        }
        lookup = null
    }

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

    private fun findPlaceholderAtOffset(offset: Int): EditorPlaceholder? {
        return placeholders.firstOrNull { placeholder ->
            placeholder.highlighter.isValid &&
                offset >= placeholder.highlighter.startOffset &&
                offset < placeholder.highlighter.endOffset
        }
    }

    private fun findPlaceholdersIntersecting(start: Int, end: Int): List<EditorPlaceholder> {
        return placeholders.filter { placeholder ->
            placeholder.highlighter.isValid &&
                placeholder.highlighter.startOffset < end &&
                placeholder.highlighter.endOffset > start
        }
    }

    private fun clearPlaceholders() {
        val editor = editor as? EditorEx ?: return
        if (editor.isDisposed) return
        placeholders.forEach { placeholder ->
            runCatching { editor.markupModel.removeHighlighter(placeholder.highlighter) }
        }
        placeholders.clear()
    }

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

    private fun clearSlashArgumentHighlights() {
        val editorEx = editor as? EditorEx ?: return
        clearSlashArgumentHighlights(editorEx)
    }

    private fun clearSlashArgumentHighlights(editorEx: EditorEx) {
        if (editorEx.isDisposed) return
        slashArgumentHighlighters.forEach { highlighter ->
            runCatching { editorEx.markupModel.removeHighlighter(highlighter) }
        }
        slashArgumentHighlighters.clear()
    }

    private fun maybeAppendSpaceAfterMatchedSlash(event: DocumentEvent, text: String) {
        if (event.oldLength != 0 || event.newLength == 0) return
        val insertedText = event.newFragment.toString()
        if (insertedText.any { it.isWhitespace() }) return

        val editorEx = editor as? EditorEx ?: return
        if (editorEx.isDisposed) return
        val insertedAtEnd = event.offset + event.newLength == text.length
        if (!insertedAtEnd) return

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

    override fun dispose() {
        hideLookupIfShown()
        clearPlaceholders()
        clearSlashHighlights()
        clearSlashArgumentHighlights()
        showSuggestionsJob?.cancel()
        coroutineScope.cancel()
    }

    private inner class ChatComposerEventDispatcher(
        private val targetDispatcherId: UUID,
    ) : IdeEventQueue.EventDispatcher {

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

        private fun findFocusedComposer(): Component? {
            return findParentByCondition(KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner) { component ->
                component is ChatComposerField && component.dispatcherId == targetDispatcherId
            }
        }

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
