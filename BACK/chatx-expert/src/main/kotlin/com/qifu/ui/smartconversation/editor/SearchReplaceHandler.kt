package com.qifu.ui.smartconversation.editor

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.readText
import com.qifu.agent.parser.Code
import com.qifu.agent.parser.ReplaceWaiting
import com.qifu.agent.parser.SearchReplace
import com.qifu.agent.parser.Segment
import com.qifu.ui.smartconversation.editor.action.DiffHeaderPanel
import com.qifu.ui.smartconversation.panels.ResponseEditorPanel.Companion.RESPONSE_EDITOR_DIFF_VIEWER_VALUE_PAIR_KEY
import com.qifu.ui.smartconversation.sse.EditorStateManager


class SearchReplaceHandler(
    private val stateManager: EditorStateManager,
    private val onEditorReplaced: (EditorEx, EditorEx) -> Unit
) {

    companion object {
        private val logger = thisLogger()
    }

    private var searchFailed = false

    fun handleSearchReplace(item: SearchReplace) {
        handleReplace(item, item.codeFilePath, item.search, item.replace)

        val editor = stateManager.getCurrentState()?.editor ?: return
        (editor.permanentHeaderComponent as? DiffHeaderPanel)?.handleDone()
        RESPONSE_EDITOR_DIFF_VIEWER_VALUE_PAIR_KEY.set(editor, Pair(item.search, item.replace))
    }

    fun handleReplace(item: ReplaceWaiting) {
        val editor = stateManager.getCurrentState()?.editor ?: return
        (editor.permanentHeaderComponent as? DiffHeaderPanel)?.editing()

        handleReplace(item, item.filePath, item.search, item.replace)
    }

    private fun handleReplace(
        item: Segment,
        filePath: String?,
        searchContent: String,
        replaceContent: String
    ) {
        val editor = stateManager.getCurrentState()?.editor ?: return

        if (filePath == null && editor.editorKind != EditorKind.DIFF) return

        val virtualFile = Key.create<ToolWindowEditorFileDetails?>("jarvis.toolwindowEditorFileDetails").get(editor)?.virtualFile
        if (virtualFile == null) {
            if (searchFailed && editor.editorKind == EditorKind.UNTYPED && replaceContent.isNotEmpty()) {
                stateManager.getCurrentState()?.updateContent(item)
            } else {
                handleNonExistentFile(replaceContent)
            }
            return
        }

        val currentText = virtualFile.readText()
        val containsText = currentText.contains(searchContent.trim())

        if (searchContent.isNotEmpty() && editor.editorKind == EditorKind.DIFF && !containsText && !searchFailed) {
            handleFailedDiffSearch(searchContent, replaceContent)
            return
        }

        stateManager.getCurrentState()?.updateContent(item)
    }

    private fun handleNonExistentFile(replaceContent: String) {
        logger.debug("Could not find file to replace in, falling back to untyped editor")

        val state = stateManager.getCurrentState() ?: return
        val oldEditor = state.editor
        val segment = Code(replaceContent, state.segment.language, state.segment.filePath)

        val newState = stateManager.createFromSegment(segment)
        onEditorReplaced(oldEditor, newState.editor)

        searchFailed = true
    }

    private fun handleFailedDiffSearch(searchContent: String, replaceContent: String) {
        logger.debug("Could not map diff search to file, falling back to untyped editor")

        val oldEditor = stateManager.getCurrentState()?.editor ?: return
        stateManager.transitionToFailedDiffState(searchContent, replaceContent)?.let {
            onEditorReplaced(oldEditor, it.editor)
        }

        searchFailed = true
    }
}