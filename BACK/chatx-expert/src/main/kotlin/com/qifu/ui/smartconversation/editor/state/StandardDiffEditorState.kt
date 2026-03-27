package com.qifu.ui.smartconversation.editor.state

import com.intellij.diff.tools.fragmented.UnifiedDiffViewer
import com.intellij.diff.util.Side
import com.intellij.ide.actions.OpenFileAction
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.application
import com.qifu.agent.parser.Code
import com.qifu.agent.parser.ReplaceWaiting
import com.qifu.agent.parser.SearchReplace
import com.qifu.agent.parser.Segment
import com.qifu.ui.smartconversation.editor.action.DiffHeaderPanel
import com.qifu.ui.smartconversation.panels.ResponseEditorPanel
import com.qifu.ui.smartconversation.sse.DiffEditorManager
import okhttp3.sse.EventSource

class StandardDiffEditorState(
    editor: EditorEx,
    segment: Segment,
    project: Project,
    diffViewer: UnifiedDiffViewer?,
    virtualFile: VirtualFile?,
    private val diffEditorManager: DiffEditorManager,
    eventSource: EventSource? = null,
    private val originalSuggestion: String? = null
) : DiffEditorState(editor, segment, project, diffViewer, virtualFile, eventSource) {

    override fun applyAllChanges() {
        val before = diffViewer?.getDocument(Side.LEFT)?.text ?: return
        val after = diffViewer.getDocument(Side.RIGHT).text
        val changes = diffEditorManager.applyAllChanges()
        if (changes.isNotEmpty()) {
            (editor.permanentHeaderComponent as? DiffHeaderPanel)
                ?.handleChangesApplied(before, after, changes)
            virtualFile?.let { OpenFileAction.openFile(it, project) }
        }
    }

    override fun updateContent(segment: Segment) {
        if (editor.editorKind == EditorKind.DIFF) {
            val (search, replace) = if (segment is SearchReplace) {
                segment.search to segment.replace
            } else if (segment is ReplaceWaiting) {
                segment.search to segment.replace
            } else {
                return
            }

            diffEditorManager.updateDiffContent(search, replace)
            (editor.permanentHeaderComponent as? DiffHeaderPanel)
                ?.updateDiffStats(diffViewer?.diffChanges ?: emptyList())
        }
    }

    fun refresh() {
        application.executeOnPooledThread {
            runInEdt {
                diffViewer?.rediff(true)
            }
        }
    }

    override fun handleClose() {
        runInEdt {
            val responsePanel = editor.component.parent as? ResponseEditorPanel ?: return@runInEdt
            val contentToKeep = originalSuggestion ?: when (segment) {
                is SearchReplace -> segment.replace
                is ReplaceWaiting -> segment.replace
                else -> diffViewer?.getDocument(Side.RIGHT)?.text ?: ""
            }
            responsePanel.replaceEditorWithSegment(
                Code(contentToKeep, segment.language, segment.filePath)
            )
        }
    }
}
