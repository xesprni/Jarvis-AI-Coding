package com.qifu.ui.smartconversation.textarea

import com.intellij.codeInsight.lookup.*
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.codeInsight.lookup.impl.PrefixChangeListener
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.qifu.ui.smartconversation.SlashCommand
import com.qifu.ui.smartconversation.textarea.lookup.DynamicLookupGroupItem
import com.qifu.ui.smartconversation.textarea.lookup.LookupActionItem
import com.qifu.ui.smartconversation.textarea.lookup.LookupGroupItem
import com.qifu.ui.smartconversation.textarea.lookup.LookupItem
import com.qifu.ui.smartconversation.textarea.lookup.action.AgentActionItem
import com.qifu.ui.smartconversation.textarea.lookup.action.CodeAnalyzeActionItem
import com.qifu.ui.smartconversation.textarea.lookup.action.SlashCommandActionItem
import com.qifu.ui.smartconversation.textarea.lookup.action.WebActionItem
import com.qifu.ui.smartconversation.textarea.lookup.files.FileActionItem
import com.qifu.ui.smartconversation.textarea.lookup.files.FolderActionItem
import com.qifu.ui.smartconversation.textarea.lookup.files.GitCommitActionItem
import com.qifu.utils.UiUtil

enum class SlashInsertMode {
    REPLACE_ALL_INPUT,
    REPLACE_TOKEN_AT_CARET,
}

class PromptTextFieldLookupManager(
    private val project: Project,
    private val promptTextField: PromptTextField,
    private val onLookupAdded: (LookupActionItem) -> Unit
) {

    fun createLookup(
        editor: Editor,
        lookupElements: Array<LookupElement>,
        searchText: String
    ): LookupImpl = runReadAction {
        LookupManager.getInstance(project).createLookup(
            editor,
            lookupElements,
            searchText,
            LookupArranger.DefaultArranger()
        ) as LookupImpl
    }

    fun showGroupLookup(
        editor: Editor,
        lookupElements: Array<LookupElement>,
        onGroupSelected: (group: LookupGroupItem, searchText: String) -> Unit,
        onWebActionSelected: (WebActionItem) -> Unit,
        onCodeAnalyzeSelected: (CodeAnalyzeActionItem) -> Unit,
        searchText: String = ""
    ): LookupImpl {
        val lookup = createLookup(editor, lookupElements, "")
        UiUtil.enableSingleClickSelection(lookup)

        lookup.addLookupListener(object : LookupListener {
            override fun itemSelected(event: LookupEvent) {
                val suggestion = event.item?.getUserData(LookupItem.KEY) ?: return

                replaceAtSymbol(editor, suggestion)

                when (suggestion) {
                    is WebActionItem -> onWebActionSelected(suggestion)
                    is CodeAnalyzeActionItem -> onCodeAnalyzeSelected(suggestion)
                    is LookupGroupItem -> onGroupSelected(suggestion, searchText)
                    is LookupActionItem -> onLookupAdded(suggestion)
                }
            }
        })

        lookup.refreshUi(false, true)
        lookup.showLookup()
        return lookup
    }

    fun showSearchResultsLookup(
        editor: Editor,
        results: List<LookupItem>,
        searchText: String
    ): LookupImpl {
        val lookupElements = results.map { it.createLookupElement() }.toTypedArray()
        val lookup = createLookup(editor, lookupElements, "")
        
        // 启用单击选择
        UiUtil.enableSingleClickSelection(lookup)

        lookup.addLookupListener(object : LookupListener {
            override fun itemSelected(event: LookupEvent) {
                val lookupString = event.item?.lookupString ?: return
                val suggestion = event.item?.getUserData(LookupItem.KEY) ?: return

                // 处理不同类型的 LookupItem
                when (suggestion) {
                    is LookupGroupItem -> {
                        // 如果是分组菜单,导航到该分组
                        // 需要先删除被自动插入的 lookupString（如 "group_files"）
                        val offset = editor.caretModel.offset
                        val start = offset - lookupString.length
                        if (start >= 0) {
                            runUndoTransparentWriteAction {
                                editor.document.deleteString(start, offset)
                            }
                        }
                        // 然后交给 handleGroupSelected 处理
                        promptTextField.handleGroupSelected(suggestion, searchText)
                    }
                    is LookupActionItem -> {
                        // 如果是可执行项(文件/文件夹),执行操作
                        val offset = editor.caretModel.offset
                        val start = offset - lookupString.length
                        if (start >= 0) {
                            runUndoTransparentWriteAction {
                                editor.document.deleteString(start, offset)
                            }
                        }

                        replaceAtSymbolWithSearch(editor, suggestion, searchText)
                        onLookupAdded(suggestion)
                    }
                }
            }
        })

        runReadAction {
            lookup.refreshUi(false, true)
        }
        lookup.showLookup()
        return lookup
    }

    fun showSuggestionLookup(
        editor: Editor,
        lookupElements: Array<LookupElement>,
        parentGroup: LookupGroupItem,
        onDynamicUpdate: (String) -> Unit,
        filterText: String = ""
    ): LookupImpl {
        val lookup = createLookup(editor, lookupElements, filterText)
        
        lookup.addLookupListener(object : LookupListener {
            override fun itemSelected(event: LookupEvent) {
                val lookupString = event.item?.lookupString ?: return
                val suggestion =
                    event.item?.getUserData(LookupItem.KEY) as? LookupActionItem ?: return

                val offset = editor.caretModel.offset
                val start = offset - lookupString.length
                if (start >= 0) {
                    runUndoTransparentWriteAction {
                        editor.document.deleteString(start, offset)
                    }
                }

                replaceAtSymbolWithSearch(editor, suggestion, filterText)
                onLookupAdded(suggestion)
            }
        })

        if (parentGroup is DynamicLookupGroupItem) {
            setupDynamicLookupListener(lookup, onDynamicUpdate)
        }

        lookup.refreshUi(false, true)
        lookup.showLookup()
        return lookup
    }

    fun showSlashCommandLookup(
        editor: Editor,
        commands: Array<SlashCommandActionItem>,
        insertMode: SlashInsertMode = SlashInsertMode.REPLACE_ALL_INPUT,
    ): LookupImpl {
        val lookupElements = commands.map { it.createLookupElement() }.toTypedArray()
        val lookup = createLookup(editor, lookupElements, PromptTextFieldConstants.SLASH_SYMBOL)
        UiUtil.enableSingleClickSelection(lookup)

        lookup.addLookupListener(object : LookupListener {
            override fun itemSelected(event: LookupEvent) {
                val lookupString = event.item?.lookupString ?: return
                val suggestion = event.item?.getUserData(LookupItem.KEY) as? SlashCommandActionItem ?: return
                val invocationText = buildSlashInvocationText(suggestion.command)
                runUndoTransparentWriteAction {
                    val offset = editor.caretModel.offset
                    val start = offset - lookupString.length
                    if (start >= 0) {
                        val selectedText = editor.document.charsSequence.subSequence(start, offset).toString()
                        if (selectedText == lookupString) {
                            editor.document.deleteString(start, offset)
                            editor.caretModel.moveToOffset(start)
                        }
                    }

                    when (insertMode) {
                        SlashInsertMode.REPLACE_ALL_INPUT -> {
                            editor.document.setText(invocationText)
                            selectFirstPlaceholderOrMoveCaret(editor, 0, invocationText)
                        }

                        SlashInsertMode.REPLACE_TOKEN_AT_CARET -> {
                            val insertStart = replaceSlashTokenAtCaret(editor, invocationText)
                            val selectedPlaceholder = selectFirstPlaceholderOrMoveCaret(editor, insertStart, invocationText)
                            if (!selectedPlaceholder) {
                                ensureTrailingSpaceAtEnd(editor)
                            }
                        }
                    }
                }
            }
        })

        lookup.refreshUi(false, true)
        lookup.showLookup()
        return lookup
    }

    private fun setupDynamicLookupListener(
        lookup: LookupImpl,
        onDynamicUpdate: (String) -> Unit
    ) {
        lookup.addPrefixChangeListener(object : PrefixChangeListener {
            override fun afterAppend(c: Char) {
                val searchText = getSearchTextFromLookup(lookup)
                if (searchText.length >= PromptTextFieldConstants.MIN_DYNAMIC_SEARCH_LENGTH) {
                    onDynamicUpdate(searchText)
                }
            }

            override fun afterTruncate() {
                val searchText = getSearchTextFromLookup(lookup)
                if (searchText.isEmpty()) {
                    onDynamicUpdate("")
                }
            }
        }, lookup)
    }

    private fun getSearchTextFromLookup(lookup: LookupImpl): String {
        val editor = lookup.editor
        val text = editor.document.text
        val atIndex = text.lastIndexOf(PromptTextFieldConstants.AT_SYMBOL)
        return if (atIndex >= 0) text.substring(atIndex + 1) else ""
    }

    private fun getSearchTextFromEditor(editor: Editor): String {
        val text = editor.document.text
        val caretOffset = editor.caretModel.offset
        val atIndex = text.lastIndexOf(PromptTextFieldConstants.AT_SYMBOL)
        return if (atIndex >= 0 && atIndex < caretOffset) {
            text.substring(atIndex + 1, caretOffset)
        } else {
            ""
        }
    }

    private fun replaceAtSymbolWithSearch(
        editor: Editor,
        lookupItem: LookupItem,
        searchText: String
    ) {
        val atPos = findAtSymbolPosition(editor)
        if (atPos >= 0) {
            runUndoTransparentWriteAction {
                val actualSearchText = getSearchTextFromEditor(editor)
                val endPos = atPos + 1 + actualSearchText.length
                editor.document.deleteString(atPos, endPos)

                if (shouldInsertDisplayName(lookupItem)) {
                    insertWithHighlight(editor, atPos, lookupItem.displayName)
                }
            }
        }
    }

    private fun replaceAtSymbol(editor: Editor, lookupItem: LookupItem) {
        val offset = editor.caretModel.offset
        val start = findAtSymbolPosition(editor)
        if (start >= 0) {
            runUndoTransparentWriteAction {
                val shouldInsert = shouldInsertDisplayName(lookupItem)
                if (shouldInsert) {
                    editor.document.deleteString(start, offset)
                    insertWithHighlight(editor, start, lookupItem.displayName)
                } else {
                    editor.document.deleteString(start + 1, offset)
                }
            }
        }
    }

    private fun shouldInsertDisplayName(lookupItem: LookupItem): Boolean {
        // FileActionItem 和 FolderActionItem 现在使用 placeholder，不需要插入高亮文字
        return lookupItem is GitCommitActionItem
                || lookupItem is AgentActionItem
    }

    private fun insertWithHighlight(editor: Editor, position: Int, text: String) {
        editor.document.insertString(position, text)
        editor.caretModel.moveToOffset(position + text.length)
        editor.markupModel.addRangeHighlighter(
            position,
            position + text.length,
            HighlighterLayer.SELECTION,
            TextAttributes().apply {
                foregroundColor = JBColor(
                    PromptTextFieldConstants.LIGHT_THEME_COLOR,
                    PromptTextFieldConstants.DARK_THEME_COLOR
                )
            },
            HighlighterTargetArea.EXACT_RANGE
        )
    }

    private fun findAtSymbolPosition(editor: Editor): Int {
        val atPos = editor.document.text.lastIndexOf(PromptTextFieldConstants.AT_SYMBOL)
        return if (atPos >= 0) atPos else -1
    }

    private fun replaceSlashTokenAtCaret(editor: Editor, replacement: String): Int {
        val slashRange = findSlashTokenRange(editor)
        if (slashRange == null) {
            val offset = editor.caretModel.offset
            editor.document.insertString(offset, replacement)
            editor.caretModel.moveToOffset(offset + replacement.length)
            return offset
        }

        val (start, end) = slashRange
        editor.document.replaceString(start, end, replacement)
        editor.caretModel.moveToOffset(start + replacement.length)
        return start
    }

    private fun findSlashTokenRange(editor: Editor): Pair<Int, Int>? {
        val text = editor.document.charsSequence
        if (text.isEmpty()) {
            return null
        }

        val caretOffset = editor.caretModel.offset.coerceIn(0, text.length)
        val slashStart = findSlashStart(text, caretOffset) ?: return null

        var tokenEnd = slashStart + 1
        while (tokenEnd < text.length && isSlashTokenChar(text[tokenEnd])) {
            tokenEnd++
        }

        if (caretOffset < slashStart || caretOffset > tokenEnd) {
            return null
        }
        return slashStart to tokenEnd
    }

    private fun findSlashStart(text: CharSequence, caretOffset: Int): Int? {
        var cursor = (caretOffset - 1).coerceAtLeast(0)
        while (cursor >= 0) {
            val current = text[cursor]
            if (current == PromptTextFieldConstants.SLASH_SYMBOL[0]) {
                return cursor
            }
            if (current.isWhitespace()) {
                return null
            }
            cursor--
        }
        return null
    }

    private fun isSlashTokenChar(ch: Char): Boolean {
        return ch.isLetterOrDigit() || ch == '-' || ch == '_' || ch == ':' || ch == '.'
    }

    private fun buildSlashInvocationText(command: SlashCommand): String {
        if (command.argumentTemplates.isEmpty()) {
            return command.command
        }
        return buildString {
            append(command.command)
            append(' ')
            append(command.argumentTemplates.joinToString(" "))
        }
    }

    private fun selectFirstPlaceholderOrMoveCaret(editor: Editor, startOffset: Int, invocationText: String): Boolean {
        val placeholderRegex = Regex("""<([^>]+)>""")
        val firstPlaceholder = placeholderRegex.find(invocationText)
        if (firstPlaceholder == null) {
            editor.caretModel.moveToOffset(startOffset + invocationText.length)
            editor.selectionModel.removeSelection()
            return false
        }

        val rangeStart = startOffset + firstPlaceholder.range.first + 1
        val rangeEnd = startOffset + firstPlaceholder.range.last
        if (rangeStart >= rangeEnd) {
            editor.caretModel.moveToOffset(startOffset + invocationText.length)
            editor.selectionModel.removeSelection()
            return false
        }
        editor.selectionModel.setSelection(rangeStart, rangeEnd)
        editor.caretModel.moveToOffset(rangeEnd)
        return true
    }

    private fun ensureTrailingSpaceAtEnd(editor: Editor) {
        val caretOffset = editor.caretModel.offset
        if (caretOffset != editor.document.textLength) {
            return
        }
        editor.document.insertString(caretOffset, " ")
        editor.caretModel.moveToOffset(caretOffset + 1)
    }
}
