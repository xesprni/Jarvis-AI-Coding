package com.qifu.ui.smartconversation.panels

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.diff.tools.fragmented.UnifiedDiffChange
import com.intellij.diff.util.DiffUserDataKeys
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.writeText
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.InlineBanner
import com.intellij.ui.components.ActionLink
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import com.qifu.ui.smartconversation.editor.DiffStatsComponent
import com.qifu.ui.smartconversation.editor.state.DiffEditorState
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel

class DiffAcceptedPanel(
    project: Project,
    virtualFile: VirtualFile,
    before: String,
    after: String,
    changes: List<UnifiedDiffChange>,
) : InlineBanner() {

    init {
        isOpaque = false
        border = JBUI.Borders.empty(8)

        val fileLink = createFileLink(project, virtualFile.path, virtualFile.name)
        val statsPanel = DiffStatsComponent.createStatsPanel(changes)

        val contentPanel = BorderLayoutPanel().andTransparent()
            .addToLeft(createLeftPanel(fileLink, statsPanel))
            .addToRight(createRightPanel(project, before, after, virtualFile))

        add(contentPanel)
        status = EditorNotificationPanel.Status.Success
        showCloseButton(false)
    }

    private fun createFileLink(project: Project, filePath: String, name: String): ActionLink =
        ActionLink(name) {
            LocalFileSystem.getInstance().findFileByPath(filePath)?.let { vFile ->
                FileEditorManager.getInstance(project).openFile(vFile, true)
            }
        }

    private fun createLeftPanel(fileLink: ActionLink, statsPanel: JPanel): JPanel =
        JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            add(fileLink)
            add(statsPanel)
        }

    private fun createRightPanel(
        project: Project,
        before: String,
        after: String,
        virtualFile: VirtualFile
    ): JPanel {
        val revertChangesLink = ActionLink("Revert Changes") {
            val contentFactory = DiffContentFactory.getInstance()
            val left = contentFactory.create(project, virtualFile)
            val right = contentFactory.create(project, before, virtualFile.fileType)

            val diffRequest = SimpleDiffRequest(
                "Revert Changes",
                left, right,
                "After",
                "Before"
            ).apply {
                val revertAllButton =
                    DiffEditorState.createContextActionButton("Revert All", AllIcons.Actions.Redo) {
                        runWriteAction {
                            virtualFile.writeText(StringUtil.convertLineSeparators(before))
                        }
                    }

                putUserData(DiffUserDataKeys.CONTEXT_ACTIONS, listOf(revertAllButton))
            }

            DiffManager.getInstance().showDiff(project, diffRequest)
        }
        val viewDetailsLink = ActionLink("View Details") {
            val contentFactory = DiffContentFactory.getInstance()
            val left = contentFactory.create(project, before, virtualFile.fileType)
            val right = contentFactory.create(project, after, virtualFile.fileType)

            val diffRequest = SimpleDiffRequest(
                "View Details",
                left, right,
                "Before",
                "After"
            )

            DiffManager.getInstance().showDiff(project, diffRequest)
        }

        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            add(revertChangesLink)
            add(Box.createHorizontalStrut(6))
            add(viewDetailsLink)
        }
    }
}