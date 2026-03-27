package com.qifu.ui.smartconversation.settings.configuration

import com.intellij.openapi.components.*
import com.qihoo.finance.lowcode.smartconversation.utils.EditorActionsUtil
import kotlin.math.max
import kotlin.math.min

@Service
@State(
    name = "Jarvis_ConfigurationSettings_210",
    storages = [Storage("Jarvis_ConfigurationSettings_210.xml")]
)
class ConfigurationSettings :
    SimplePersistentStateComponent<ConfigurationSettingsState>(ConfigurationSettingsState()) {
    companion object {
        @JvmStatic
        fun getState(): ConfigurationSettingsState {
            return service<ConfigurationSettings>().state
        }
    }
}

class ConfigurationSettingsState : BaseState() {
    var commitMessagePrompt by string("test prompt")
    var maxTokens by property(8192)
    var temperature by property(0.1f) { max(0f, min(1f, it)) }
    var checkForPluginUpdates by property(true)
    var checkForNewScreenshots by property(true)
    var ignoreGitCommitTokenLimit by property(false)
    var methodNameGenerationEnabled by property(true)
    var captureCompileErrors by property(true)
    var autoFormattingEnabled by property(true)
    var tableData by map<String, String>()
    var chatCompletionSettings by property(ChatCompletionSettingsState())
    var codeCompletionSettings by property(CodeCompletionSettingsState())

    init {
        tableData.putAll(EditorActionsUtil.DEFAULT_ACTIONS)
    }
}

class ChatCompletionSettingsState : BaseState() {
    var editorContextTagEnabled by property(true)
    var psiStructureEnabled by property(true)
    var psiStructureAnalyzeDepth by property(3)
}

class CodeCompletionSettingsState : BaseState() {
    var treeSitterProcessingEnabled by property(true)
    var gitDiffEnabled by property(true)
    var collectDependencyStructure by property(true)
    var contextAwareEnabled by property(false)
    var psiStructureAnalyzeDepth by property(2)
}