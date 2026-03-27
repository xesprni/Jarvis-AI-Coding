package com.qifu.ui.smartconversation.textarea.lookup.group

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.qifu.ui.smartconversation.settings.prompts.PersonaDetails
import com.qifu.ui.smartconversation.settings.prompts.PromptsSettings
import com.qifu.ui.smartconversation.textarea.header.PersonaTagDetails
import com.qifu.ui.smartconversation.textarea.header.TagManager
import com.qifu.ui.smartconversation.textarea.lookup.LookupActionItem
import com.qifu.ui.smartconversation.textarea.lookup.personas.AddPersonaActionItem
import com.qifu.ui.smartconversation.textarea.lookup.personas.PersonaActionItem

class PersonasGroupItem(private val tagManager: TagManager) :
    AbstractLookupGroupItem() {

    override val displayName: String = "Personas"
    override val icon = AllIcons.General.User
    override val enabled: Boolean
        get() = tagManager.getTags().none { it is PersonaTagDetails }

    override suspend fun getLookupItems(searchText: String): List<LookupActionItem> {
        return listOf(AddPersonaActionItem()) + service<PromptsSettings>().state.personas.prompts
            .map {
                PersonaDetails(it.id, it.name ?: "Unknown", it.instructions ?: "Unknown")
            }
            .filter {
                searchText.isEmpty() || it.name.contains(searchText, true)
            }
            .map { PersonaActionItem(it) }
            .take(10)
    }
}