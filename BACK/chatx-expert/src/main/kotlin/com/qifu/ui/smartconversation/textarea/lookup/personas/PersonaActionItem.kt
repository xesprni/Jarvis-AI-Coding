package com.qifu.ui.smartconversation.textarea.lookup.personas

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.qifu.ui.smartconversation.panels.UserInputPanel
import com.qifu.ui.smartconversation.settings.prompts.PersonaDetails
import com.qifu.ui.smartconversation.textarea.header.PersonaTagDetails
import com.qifu.ui.smartconversation.textarea.lookup.action.AbstractLookupActionItem

class PersonaActionItem(
    private val personaDetails: PersonaDetails
) : AbstractLookupActionItem() {

    override val displayName = personaDetails.name
    override val icon = AllIcons.General.User

    override fun execute(project: Project, userInputPanel: UserInputPanel) {
        userInputPanel.addTag(PersonaTagDetails(personaDetails))
    }
}
