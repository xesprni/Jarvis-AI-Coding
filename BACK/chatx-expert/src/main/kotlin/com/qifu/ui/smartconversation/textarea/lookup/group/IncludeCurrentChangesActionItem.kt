package com.qifu.ui.smartconversation.textarea.lookup.group

import com.intellij.openapi.project.Project
import com.qifu.ui.smartconversation.panels.UserInputPanel
import com.qifu.ui.smartconversation.textarea.header.CurrentGitChangesTagDetails
import com.qifu.ui.smartconversation.textarea.lookup.action.AbstractLookupActionItem
import javax.swing.Icon

class IncludeCurrentChangesActionItem : AbstractLookupActionItem() {

    override val displayName: String =
        "Include current changes"
    override val icon: Icon? = null

    override fun execute(project: Project, userInputPanel: UserInputPanel) {
        userInputPanel.addTag(CurrentGitChangesTagDetails())
    }
}