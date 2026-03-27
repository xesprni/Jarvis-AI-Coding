package com.miracle.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.miracle.ui.smartconversation.settings.configuration.ChatMode
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

@State(
    name = "chatx-settings",
    storages = [Storage("chatx-settings.xml")]
)
@Service(Service.Level.APP)
class JarvisCoreSettings : PersistentStateComponent<JarvisCoreSettings.State> {

    data class State(
        var chatModelId: String? = null,
        var modelSupportImage: Boolean = false,
        var skillTemplate: String = "",
        var ruleTemplate: String = "",
        var chatMode: String = ChatMode.AGENT.displayName,
    )

    private var state = State()

    override fun getState(): State {
        ensureLocalDefaults()
        return state
    }

    override fun loadState(state: State) {
        this.state = state
        ensureLocalDefaults()
    }

    var selectedChatModelId: String?
        get() {
            ensureLocalDefaults()
            return state.chatModelId?.takeIf { it.isNotBlank() }
        }
        set(value) {
            state.chatModelId = value?.takeIf { it.isNotBlank() }
        }

    var modelSupportsImages: Boolean
        get() = state.modelSupportImage
        set(value) {
            state.modelSupportImage = value
        }

    var skillTemplate: String
        get() {
            ensureLocalDefaults()
            return state.skillTemplate
        }
        set(value) {
            state.skillTemplate = value.ifBlank { loadResource("/templates/default-skill.md") }
        }

    var ruleTemplate: String
        get() {
            ensureLocalDefaults()
            return state.ruleTemplate
        }
        set(value) {
            state.ruleTemplate = value.ifBlank { loadResource("/templates/default-rules.md") }
        }

    var chatMode: ChatMode
        get() = ChatMode.entries.firstOrNull { it.displayName == state.chatMode } ?: ChatMode.AGENT
        set(value) {
            state.chatMode = value.displayName
        }

    fun ensureValidSelectedModel(validModelIds: Set<String>) {
        val current = selectedChatModelId ?: return
        if (current !in validModelIds) {
            selectedChatModelId = null
            modelSupportsImages = false
        }
    }

    private fun ensureLocalDefaults() {
        if (state.skillTemplate.isBlank()) {
            state.skillTemplate = loadResource("/templates/default-skill.md")
        }
        if (state.ruleTemplate.isBlank()) {
            state.ruleTemplate = loadResource("/templates/default-rules.md")
        }
        if (state.chatMode.isBlank()) {
            state.chatMode = ChatMode.AGENT.displayName
        }
    }

    private fun loadResource(path: String): String {
        val stream = JarvisCoreSettings::class.java.getResourceAsStream(path)
            ?: return ""
        return BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }
    }

    companion object {
        fun getInstance(): JarvisCoreSettings {
            return ApplicationManager.getApplication().getService(JarvisCoreSettings::class.java)
        }
    }
}
