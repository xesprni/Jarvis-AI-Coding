package com.qifu.ui.smartconversation.textarea.lookup.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.qihoo.finance.lowcode.smartconversation.settings.SmartToolWindowContentManager

class AddSelectionToContextAction : BaseEditorAction(AllIcons.General.Add) {

    override fun actionPerformed(project: Project, editor: Editor, selectedText: String) {
        val chatToolWindowContentManager = project.service<SmartToolWindowContentManager>()
        val chatTabPanel = chatToolWindowContentManager
            .tryFindActiveChatTabPanel()
            .orElseThrow()

        val toolwindow = chatToolWindowContentManager.toolWindow
        if (!toolwindow.isActive) {
            toolwindow.show()
        }

        chatTabPanel.addSelection(editor.virtualFile, editor.selectionModel)
    }
}
