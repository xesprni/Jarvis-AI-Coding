package com.qifu.ui.smartconversation.settings.models

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.qihoo.finance.lowcode.smartconversation.service.FeatureType
import com.qihoo.finance.lowcode.smartconversation.service.ServiceType
import com.qihoo.finance.lowcode.common.util.Icons
import javax.swing.Icon

data class ModelSelection(
    val provider: ServiceType,
    val model: String,
    val displayName: String,
    val icon: Icon? = null
) {
    val fullDisplayName: String = if (provider == ServiceType.LLAMA_CPP) {
        displayName
    } else {
        "$provider • $displayName"
    }
}

data class ModelCapability(
    val provider: ServiceType,
    val supportedFeatures: Set<FeatureType>
)

@Service
class ModelRegistry {

    private val logger = thisLogger()

    private val providerCapabilities = mapOf(
        ServiceType.PROXYAI to ModelCapability(
            ServiceType.PROXYAI,
            setOf(
                FeatureType.CHAT,
                FeatureType.CODE_COMPLETION,
                FeatureType.AUTO_APPLY,
                FeatureType.COMMIT_MESSAGE,
                FeatureType.EDIT_CODE,
                FeatureType.NEXT_EDIT,
                FeatureType.LOOKUP
            )
        ),
        ServiceType.OPENAI to ModelCapability(
            ServiceType.OPENAI,
            setOf(
                FeatureType.CHAT, FeatureType.CODE_COMPLETION, FeatureType.AUTO_APPLY,
                FeatureType.COMMIT_MESSAGE, FeatureType.EDIT_CODE, FeatureType.LOOKUP
            )
        ),
        ServiceType.ANTHROPIC to ModelCapability(
            ServiceType.ANTHROPIC,
            setOf(
                FeatureType.CHAT, FeatureType.AUTO_APPLY, FeatureType.COMMIT_MESSAGE,
                FeatureType.EDIT_CODE, FeatureType.LOOKUP
            )
        ),
        ServiceType.GOOGLE to ModelCapability(
            ServiceType.GOOGLE,
            setOf(
                FeatureType.CHAT, FeatureType.AUTO_APPLY, FeatureType.COMMIT_MESSAGE,
                FeatureType.EDIT_CODE, FeatureType.LOOKUP
            )
        ),
        ServiceType.MISTRAL to ModelCapability(
            ServiceType.MISTRAL,
            setOf(
                FeatureType.CHAT, FeatureType.CODE_COMPLETION, FeatureType.AUTO_APPLY,
                FeatureType.COMMIT_MESSAGE, FeatureType.EDIT_CODE, FeatureType.LOOKUP
            )
        ),
        ServiceType.OLLAMA to ModelCapability(
            ServiceType.OLLAMA,
            setOf(
                FeatureType.CHAT, FeatureType.CODE_COMPLETION, FeatureType.AUTO_APPLY,
                FeatureType.COMMIT_MESSAGE, FeatureType.EDIT_CODE, FeatureType.LOOKUP
            )
        ),
        ServiceType.LLAMA_CPP to ModelCapability(
            ServiceType.LLAMA_CPP,
            setOf(
                FeatureType.CHAT, FeatureType.CODE_COMPLETION, FeatureType.AUTO_APPLY,
                FeatureType.COMMIT_MESSAGE, FeatureType.EDIT_CODE, FeatureType.LOOKUP
            )
        ),
        ServiceType.CUSTOM_OPENAI to ModelCapability(
            ServiceType.CUSTOM_OPENAI,
            setOf(
                FeatureType.CHAT, FeatureType.CODE_COMPLETION, FeatureType.AUTO_APPLY,
                FeatureType.COMMIT_MESSAGE, FeatureType.EDIT_CODE, FeatureType.LOOKUP
            )
        )
    )

    private val fallbackDefaults = mapOf(
        FeatureType.CHAT to ModelSelection(
            ServiceType.PROXYAI,
            GEMINI_FLASH_2_5,
            "Gemini Flash 2.5"
        ),
        FeatureType.AUTO_APPLY to ModelSelection(
            ServiceType.PROXYAI,
            GEMINI_FLASH_2_5,
            "Gemini Flash 2.5"
        ),
        FeatureType.COMMIT_MESSAGE to ModelSelection(
            ServiceType.PROXYAI,
            GPT_5_MINI,
            "GPT-5 Mini"
        ),
        FeatureType.EDIT_CODE to ModelSelection(ServiceType.PROXYAI, GPT_5_MINI, "GPT-5 Mini"),
        FeatureType.LOOKUP to ModelSelection(ServiceType.PROXYAI, GPT_5_MINI, "GPT-5 Mini"),
        FeatureType.CODE_COMPLETION to ModelSelection(
            ServiceType.PROXYAI,
            QWEN_2_5_32B_CODE,
            "Qwen 2.5 32B Code"
        ),
        FeatureType.NEXT_EDIT to ModelSelection(ServiceType.PROXYAI, ZETA, "Zeta")
    )

    fun getAllModelsForFeature(featureType: FeatureType): List<ModelSelection> {
        return when (featureType) {
            FeatureType.CHAT, FeatureType.AUTO_APPLY, FeatureType.COMMIT_MESSAGE,
            FeatureType.EDIT_CODE, FeatureType.LOOKUP -> getAllChatModels()

            FeatureType.CODE_COMPLETION -> getAllCodeModels()
            FeatureType.NEXT_EDIT -> getNextEditModels()
        }
    }


    fun getDefaultModelForFeature(
        featureType: FeatureType
    ): ModelSelection {
        val planBasedDefaults = getAllModels()
        return fallbackDefaults[featureType]!!
    }


    fun getProvidersForFeature(featureType: FeatureType): List<ServiceType> {
        return providerCapabilities.values
            .filter { it.supportedFeatures.contains(featureType) }
            .map { it.provider }
    }

    fun isFeatureSupportedByProvider(featureType: FeatureType, provider: ServiceType): Boolean {
        return providerCapabilities[provider]?.supportedFeatures?.contains(featureType) == true
    }

    fun findModel(provider: ServiceType, modelCode: String): ModelSelection? {
        return getAllModels()
            .filter { it.provider == provider }
            .find { it.model == modelCode }
    }

    fun getModelDisplayName(provider: ServiceType, modelCode: String): String {
        return findModel(provider, modelCode)?.displayName ?: modelCode
    }

    fun getAllModels(): List<ModelSelection> {
        return buildList {
            addAll(getAllChatModels())
            addAll(getAllCodeModels())
            addAll(getNextEditModels())
        }.distinctBy { "${it.provider}:${it.model}" }
    }

    private fun getAllChatModels(): List<ModelSelection> {
        return buildList {
            addAll(getProxyAIChatModels())
            addAll(getOpenAIChatModels())
            addAll(getAnthropicModels())
            addAll(getMistralModels())
        }
    }

    private fun getAllCodeModels(): List<ModelSelection> {
        return buildList {
            addAll(getProxyAICodeModels())
            add(getOpenAICodeModel())
            addAll(getMistralCodeModels())
        }
    }


    private fun getNextEditModels(): List<ModelSelection> {
        return listOf(ModelSelection(ServiceType.PROXYAI, ZETA, "Zeta"))
    }

    fun getProxyAIChatModels(): List<ModelSelection> {
        return listOf(
            ModelSelection(
                ServiceType.PROXYAI,
                GPT_5,
                "GPT-5",
                Icons.OpenAI
            ),
            ModelSelection(
                ServiceType.PROXYAI,
                GPT_5_MINI,
                "GPT-5 Mini",
                Icons.OpenAI
            ),
            ModelSelection(
                ServiceType.PROXYAI,
                O4_MINI,
                "o4-mini",
                Icons.OpenAI
            )
        )
    }

    private fun getProxyAICodeModels(): List<ModelSelection> {
        return listOf(
            ModelSelection(
                ServiceType.PROXYAI,
                QWEN_2_5_32B_CODE,
                "Qwen 2.5 Coder",
                Icons.Qwen
            ),
            ModelSelection(
                ServiceType.PROXYAI,
                GPT_3_5_TURBO_INSTRUCT,
                "GPT-3.5 Turbo Instruct",
                Icons.OpenAI
            )
        )
    }

    private fun getOpenAIChatModels(): List<ModelSelection> {
        return listOf(
            ModelSelection(ServiceType.OPENAI, "gpt-5", "GPT-5"),
            ModelSelection(ServiceType.OPENAI, "gpt-5-mini", "GPT-5 Mini"),
            ModelSelection(ServiceType.OPENAI, "o4-mini", "O4 Mini"),
            ModelSelection(ServiceType.OPENAI, "o3-pro", "O3 Pro"),
            ModelSelection(ServiceType.OPENAI, "o3", "O3"),
            ModelSelection(ServiceType.OPENAI, "o3-mini", "O3 Mini"),
            ModelSelection(ServiceType.OPENAI, "gpt-4.1", "GPT-4.1"),
            ModelSelection(ServiceType.OPENAI, "gpt-4.1-mini", "GPT-4.1 Mini"),
            ModelSelection(ServiceType.OPENAI, "gpt-4.1-nano", "GPT-4.1 Nano"),
            ModelSelection(ServiceType.OPENAI, "o1-preview", "O1 Preview"),
            ModelSelection(ServiceType.OPENAI, "o1-mini", "O1 Mini"),
            ModelSelection(ServiceType.OPENAI, "gpt-4o", "GPT-4o"),
            ModelSelection(ServiceType.OPENAI, "gpt-4-0125-preview", "GPT-4 0125 Preview"),
            ModelSelection(ServiceType.OPENAI, "gpt-3.5-turbo-instruct", "GPT-3.5 Turbo Instruct"),
            ModelSelection(ServiceType.OPENAI, "gpt-4-vision-preview", "GPT-4 Vision Preview")
        )
    }

    private fun getOpenAICodeModel(): ModelSelection {
        return ModelSelection(ServiceType.OPENAI, GPT_3_5_TURBO_INSTRUCT, "GPT-3.5 Turbo Instruct")
    }

    private fun getAnthropicModels(): List<ModelSelection> {
        return listOf(
            ModelSelection(ServiceType.ANTHROPIC, CLAUDE_OPUS_4_20250514, "Claude Opus 4"),
            ModelSelection(ServiceType.ANTHROPIC, CLAUDE_SONNET_4_20250514, "Claude Sonnet 4")
        )
    }

    private fun getMistralModels(): List<ModelSelection> {
        return listOf(
            ModelSelection(ServiceType.MISTRAL, DEVSTRAL_MEDIUM_2507, "Devstral Medium"),
            ModelSelection(ServiceType.MISTRAL, MISTRAL_LARGE_2411, "Mistral Large"),
            ModelSelection(ServiceType.MISTRAL, CODESTRAL_LATEST, "Codestral"),
        )
    }

    private fun getMistralCodeModels(): List<ModelSelection> {
        return listOf(ModelSelection(ServiceType.MISTRAL, CODESTRAL_LATEST, "Codestral"))
    }

    companion object {
        // ProxyAI Models
        const val GEMINI_PRO_2_5 = "gemini-pro-2.5"
        const val GEMINI_FLASH_2_5 = "gemini-flash-2.5"
        const val CLAUDE_4_SONNET = "claude-4-sonnet"
        const val CLAUDE_4_SONNET_THINKING = "claude-4-sonnet-thinking"
        const val DEEPSEEK_R1 = "deepseek-r1"
        const val DEEPSEEK_V3 = "deepseek-v3"
        const val QWEN_2_5_32B_CODE = "qwen-2.5-32b-code"
        const val ZETA = "zeta"
        const val QWEN3_CODER = "qwen3-coder"

        // OpenAI Models
        const val GPT_3_5_TURBO_INSTRUCT = "gpt-3.5-turbo-instruct"
        const val O4_MINI = "o4-mini"
        const val O3_PRO = "o3-pro"
        const val O3 = "o3"
        const val O3_MINI = "o3-mini"
        const val O1_PREVIEW = "o1-preview"
        const val O1_MINI = "o1-mini"
        const val GPT_4_1 = "gpt-4.1"
        const val GPT_4_1_MINI = "gpt-4.1-mini"
        const val GPT_4_1_NANO = "gpt-4.1-nano"
        const val GPT_4O = "gpt-4o"
        const val GPT_4O_MINI = "gpt-4o-mini"
        const val GPT_4_0125_PREVIEW = "gpt-4-0125-preview"
        const val GPT_4_VISION_PREVIEW = "gpt-4-vision-preview"
        const val GPT_5 = "gpt-5"
        const val GPT_5_MINI = "gpt-5-mini"

        // Anthropic Models
        const val CLAUDE_OPUS_4_20250514 = "claude-opus-4-20250514"
        const val CLAUDE_SONNET_4_20250514 = "claude-sonnet-4-20250514"

        // Google Models
        const val GEMINI_2_0_FLASH = "gemini-2.0-flash"

        // Mistral Models
        const val MISTRAL_LARGE_2411 = "mistral-large-2411"
        const val DEVSTRAL_MEDIUM_2507 = "devstral-medium-2507"
        const val CODESTRAL_LATEST = "codestral-latest"

        // Ollama default models
        const val LLAMA_3_2 = "llama3.2"

        // Llama.cpp default models
        const val LLAMA_3_2_3B_INSTRUCT = "llama-3.2-3b-instruct"

        @JvmStatic
        fun getInstance(): ModelRegistry {
            return ApplicationManager.getApplication().getService(ModelRegistry::class.java)
        }
    }
}