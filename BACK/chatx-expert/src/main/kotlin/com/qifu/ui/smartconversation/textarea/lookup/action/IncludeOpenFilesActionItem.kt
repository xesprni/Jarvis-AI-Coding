package com.qifu.ui.smartconversation.textarea.lookup.action

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.qifu.ui.smartconversation.panels.UserInputPanel
import com.qifu.ui.smartconversation.textarea.header.FileTagDetails
import com.qihoo.finance.lowcode.common.util.Icons
import javax.swing.Icon

class IncludeOpenFilesActionItem : AbstractLookupActionItem() {
    override val displayName: String =
        "Include open files"
    override val icon: Icon = Icons.ListFiles

    override fun execute(project: Project, userInputPanel: UserInputPanel) {
        val fileTags = userInputPanel.getSelectedTags().filterIsInstance<FileTagDetails>()
        project.service<FileEditorManager>().openFiles
            .filter { openFile ->
                fileTags.none { it.virtualFile == openFile }
            }
            .forEach {
                userInputPanel.addTag(FileTagDetails(it))
            }
    }
}