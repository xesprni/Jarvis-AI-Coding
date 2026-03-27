package com.qifu.ui.smartconversation.textarea.lookup.personas

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.qifu.ui.smartconversation.panels.UserInputPanel
import com.qifu.ui.smartconversation.textarea.lookup.action.AbstractLookupActionItem
import kotlin.jvm.java

class AddPersonaActionItem : AbstractLookupActionItem() {

    override val displayName: String =
       "Add new persona"
    override val icon = AllIcons.General.Add

    override fun execute(project: Project, userInputPanel: UserInputPanel) {
//        service<ShowSettingsUtil>().showSettingsDialog(
//            project,
//            PromptsConfigurable::class.java
//        )
    }
}