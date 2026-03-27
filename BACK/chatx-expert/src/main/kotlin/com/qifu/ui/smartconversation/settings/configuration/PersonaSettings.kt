package com.qifu.ui.smartconversation.settings.configuration

import com.intellij.openapi.components.*
import com.qifu.ui.smartconversation.settings.prompts.PersonasState

@Deprecated("Use PromptsSettings instead")
@Service
@State(
    name = "Jarvis_PersonaSettings",
    storages = [Storage("Jarvis_PersonaSettings.xml")]
)
class PersonaSettings :
    SimplePersistentStateComponent<PersonaSettingsState>(PersonaSettingsState())

class PersonaSettingsState : BaseState() {
    var userCreatedPersonas by list<PersonaDetailsState>()
}

class PersonaDetailsState : BaseState() {
    var id by property(PersonasState.DEFAULT_PERSONA.id)
    var name by string(PersonasState.DEFAULT_PERSONA.name)
    var instructions by string(PersonasState.DEFAULT_PERSONA.instructions)
}
