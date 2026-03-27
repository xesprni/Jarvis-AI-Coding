package com.miracle.services

import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.Logger
import com.miracle.config.JarvisCoreSettings
import com.miracle.agent.tool.Tool
import com.miracle.agent.tool.ToolRegistry
import com.miracle.agent.tool.ToolUseContext
import com.miracle.utils.Debug
import com.miracle.utils.getUserConfigDirectory
import com.miracle.utils.getCurrentRequest
import com.miracle.utils.markPhase
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.SystemMessage.systemMessage
import dev.langchain4j.kotlin.model.chat.request.chatRequest
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.chat.response.ChatResponse
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiResponsesStreamingChatModel
import dev.langchain4j.model.openai.OpenAiStreamingChatModel
import kotlinx.coroutines.future.await
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Duration
import java.util.concurrent.CompletableFuture
import javax.swing.Icon

private var LOG = Logger.getInstance("com.miracle.services.model")
@OptIn(ExperimentalSerializationApi::class)
private val json = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
}

enum class ModelProvider(val code: String, val desc: String) {
    OPENAI_COMPATIBLE("OPENAI_COMPATIBLE", "自定义模型");

    companion object {
        fun fromCode(code: String): ModelProvider? {
            return entries.find { it.code == code }
        }
    }
}

@Serializable
enum class ModelApiStyle(val desc: String) {
    CHAT_COMPLETIONS("Chat Completions"),
    RESPONSES("Responses"),
    CODEX_CLI("Codex CLI");

    override fun toString(): String = desc
}

fun ModelApiStyle.requiresApiCredentials(): Boolean = this != ModelApiStyle.CODEX_CLI

private val REASONING_EFFORTS = setOf("low", "medium", "high", "xhigh")

@Serializable
data class ModelConfig(
    @kotlinx.serialization.Transient
    val provider: ModelProvider = ModelProvider.OPENAI_COMPATIBLE,   // 模型提供商
    val model: String,      // 模型提供商的名称
    @SerialName("alias")
    val _alias: String? = null,   // JSON 里的 alias（可选）
    val endpoint: String,   // 模型提供商的接口地址
    val apiKey: String?,    // 模型提供商的token
    val contextTokens: Int, // 上下文最大token数
    val maxOutputTokens: Int? = null,  // 最大输出token
    val apiStyle: ModelApiStyle = ModelApiStyle.CHAT_COMPLETIONS,  // API 协议类型
    val reasoningEffort: String? = null,  // reasoning effort，主要给 Codex/Responses 模型使用
    val supportsImages: Boolean = false,  // 是否支持图片
    @kotlinx.serialization.Transient
    var icon: Icon = AllIcons.Nodes.Plugin
) {
    val id: String
        get() = "${provider}_${model}"
    val alias: String
        get() = _alias ?: model
    val isCodexModel: Boolean
        get() = model.contains("codex", ignoreCase = true) || alias.contains("codex", ignoreCase = true)
    val resolvedApiStyle: ModelApiStyle
        get() = when {
            apiStyle == ModelApiStyle.CODEX_CLI -> ModelApiStyle.CODEX_CLI
            apiStyle == ModelApiStyle.CHAT_COMPLETIONS && isCodexModel -> ModelApiStyle.RESPONSES
            else -> apiStyle
        }
    val resolvedReasoningEffort: String?
        get() = normalizeReasoningEffort(reasoningEffort) ?: if (isCodexModel) "medium" else null

    companion object {
        fun from(
            provider: ModelProvider,
            model: String,
            contextTokens: Int,
            endpoint: String,
            apiKey: String? = null,
            alias: String? = null,
            apiStyle: ModelApiStyle = ModelApiStyle.CHAT_COMPLETIONS,
            reasoningEffort: String? = null,
            supportsImages: Boolean = false,
            icon: Icon = AllIcons.Nodes.Plugin,
            maxOutputTokens: Int? = null,
        ): ModelConfig {
            return ModelConfig(
                provider = provider,
                model = model,
                _alias = alias,
                endpoint = endpoint,
                apiKey = apiKey,
                contextTokens = contextTokens,
                apiStyle = apiStyle,
                reasoningEffort = reasoningEffort,
                supportsImages = supportsImages,
                icon = icon,
                maxOutputTokens = maxOutputTokens
            )
        }
    }
}

data class QueryLLMOptions(
    val model: String,
    val streaming: Boolean = false,
    val toolUseContext: ToolUseContext? = null // 可空类型对应可选属性
)

suspend fun chatCompletion(
    messages: List<ChatMessage>,
    systemPrompt: List<String> = emptyList(),
    tools: List<Tool<*>> = emptyList(),
    options: QueryLLMOptions,
): ChatResponse {
    Debug.api(
        "LLM_REQUEST_START", mapOf(
            "messageCount" to messages.size,
            "systemPrompt" to systemPrompt.joinToString(" ").length,
            "toolCount" to (tools.size),
            "model" to options.model,
            "requestId" to getCurrentRequest()?.id
        )
    )
    markPhase("LLM_CALL")
    val systemMessages: List<ChatMessage> = systemPrompt.filter { it.isNotBlank() }.map { systemMessage(it) }
    val allMessages = systemMessages + messages
    return executeChatRequest(
        modelId = options.model,
        request = chatRequest {
            this.messages += allMessages
            parameters {
                this.toolSpecifications = ToolRegistry.getToolSpecifications(tools)
            }
        }
    )
}

suspend fun getStreamChatModel(modelId: String): StreamingChatModel {
    val modelConfig = getModelConfig(modelId)
    if (modelConfig.resolvedApiStyle == ModelApiStyle.CODEX_CLI) {
        throw IllegalStateException("Codex CLI 模型不走内部 StreamingChatModel 链路")
    }
    return buildStreamingChatModel(modelConfig)
}

suspend fun getChatModel(modelId: String): ChatModel? {
    val modelConfig = getModelConfig(modelId)
    return if (modelConfig.resolvedApiStyle != ModelApiStyle.CHAT_COMPLETIONS) null else buildChatModel(modelConfig)
}

/**
 * 获取模型列表
 */
private var CUSTOM_MODELS: List<ModelConfig> = listOf()
suspend fun loadModelConfigs(forceUpdate: Boolean = false): Map<String, ModelConfig> {
    if (CUSTOM_MODELS.isEmpty() || forceUpdate) {
        CUSTOM_MODELS = loadCustomModelConfigs()
    }
    val modelConfigs = CUSTOM_MODELS
    JarvisCoreSettings.getInstance().ensureValidSelectedModel(modelConfigs.map { it.id }.toSet())
    return modelConfigs.associateBy { it.id }
}

private fun loadCustomModelConfigs(): List<ModelConfig> {
    return runCatching {
        val modelFile = File(getUserConfigDirectory(), "models.json")
        if (!modelFile.exists()) {
            return emptyList()
        }
        val jsonText = modelFile.readText()
        val models = Json.decodeFromString<List<ModelConfig>>(jsonText)
        return models.map { normalizeModelConfig(it) }
    }.getOrElse { emptyList() }
}

/**
 * 检查自定义模型是否已存在
 */
fun isCustomModelExists(model: String): Boolean {
    val modelConfigs = loadCustomModelConfigs()
    return modelConfigs.any { it.model == model }
}

/**
 * 获取所有自定义模型列表
 */
fun getCustomModels(): List<ModelConfig> {
    return loadCustomModelConfigs()
}

fun addCustomModel(
    model: String,
    endpoint: String,
    apiKey: String?,
    contextTokens: Int,
    alias: String? = null,
    apiStyle: ModelApiStyle = ModelApiStyle.CHAT_COMPLETIONS,
    reasoningEffort: String? = null,
    supportsImages: Boolean = false,
) {
    val modelConfigs = loadCustomModelConfigs()

    // 检查模型是否已存在
    val existingModel = modelConfigs.find { it.model == model }
    if (existingModel != null) {
        throw IllegalArgumentException("模型 '$model' 已存在，请使用其他模型名称")
    }

    val modelConfig = ModelConfig(
        provider = ModelProvider.OPENAI_COMPATIBLE,
        model = model,
        _alias = alias,
        endpoint = endpoint.trim(),
        apiKey = apiKey?.trim()?.takeIf { it.isNotBlank() },
        contextTokens = contextTokens,
        apiStyle = apiStyle,
        reasoningEffort = normalizeReasoningEffort(reasoningEffort),
        supportsImages = supportsImages
    )
    CUSTOM_MODELS = modelConfigs + modelConfig
    val jsonText = json.encodeToString(CUSTOM_MODELS)
    val modelFile = File(getUserConfigDirectory(), "models.json")
    modelFile.writeText(jsonText)
}

/**
 * 删除自定义模型
 */
fun deleteCustomModel(model: String) {
    val modelConfigs = loadCustomModelConfigs()
    val updatedConfigs = modelConfigs.filter { it.model != model }
    
    if (modelConfigs.size == updatedConfigs.size) {
        throw IllegalArgumentException("模型 '$model' 不存在")
    }

    CUSTOM_MODELS = updatedConfigs
    val jsonText = json.encodeToString(CUSTOM_MODELS)
    val modelFile = File(getUserConfigDirectory(), "models.json")
    modelFile.writeText(jsonText)
}

fun getSelectedModelId(): String? {
    return JarvisCoreSettings.getInstance().selectedChatModelId
}

fun setSelectedModel(modelId: String?) {
    val settings = JarvisCoreSettings.getInstance()
    settings.selectedChatModelId = modelId
    val selectedModel = getCustomModels().firstOrNull { it.id == modelId }
    settings.modelSupportsImages = selectedModel?.supportsImages ?: false
}

fun getSelectedModel(): ModelConfig? {
    val modelId = JarvisCoreSettings.getInstance().selectedChatModelId ?: return null
    return getCustomModels().firstOrNull { it.id == modelId }
}

/**
 * 更新自定义模型
 */
fun updateCustomModel(
    oldModel: String,
    newModel: String,
    endpoint: String,
    apiKey: String?,
    contextTokens: Int,
    alias: String? = null,
    apiStyle: ModelApiStyle = ModelApiStyle.CHAT_COMPLETIONS,
    reasoningEffort: String? = null,
    supportsImages: Boolean = false,
) {
    val modelConfigs = loadCustomModelConfigs()

    // 检查旧模型是否存在
    val existingIndex = modelConfigs.indexOfFirst { it.model == oldModel }
    if (existingIndex == -1) {
        throw IllegalArgumentException("模型 '$oldModel' 不存在")
    }

    // 如果模型名称改变了，检查新名称是否已存在
    if (oldModel != newModel) {
        val newModelExists = modelConfigs.any { it.model == newModel }
        if (newModelExists) {
            throw IllegalArgumentException("模型 '$newModel' 已存在")
        }
    }

    val existingModel = modelConfigs[existingIndex]
    val resolvedApiKey = apiKey?.trim()?.takeIf { it.isNotBlank() } ?: existingModel.apiKey

    val updatedModel = ModelConfig(
        provider = ModelProvider.OPENAI_COMPATIBLE,
        model = newModel,
        _alias = alias,
        endpoint = endpoint.trim(),
        apiKey = resolvedApiKey,
        contextTokens = contextTokens,
        apiStyle = apiStyle,
        reasoningEffort = normalizeReasoningEffort(reasoningEffort),
        supportsImages = supportsImages
    )
    
    val updatedConfigs = modelConfigs.toMutableList()
    updatedConfigs[existingIndex] = updatedModel
    
    CUSTOM_MODELS = updatedConfigs
    val jsonText = json.encodeToString(CUSTOM_MODELS)
    val modelFile = File(getUserConfigDirectory(), "models.json")
    modelFile.writeText(jsonText)
}

fun formatReasoningEffort(reasoningEffort: String?): String {
    return normalizeReasoningEffort(reasoningEffort) ?: "默认"
}

private fun normalizeReasoningEffort(reasoningEffort: String?): String? {
    val normalized = reasoningEffort?.trim()?.lowercase()?.takeIf { it.isNotBlank() } ?: return null
    return normalized.takeIf { it in REASONING_EFFORTS }
}

private fun normalizeModelConfig(modelConfig: ModelConfig): ModelConfig {
    return modelConfig.copy(
        apiStyle = modelConfig.resolvedApiStyle,
        reasoningEffort = modelConfig.resolvedReasoningEffort,
        icon = AllIcons.Nodes.Plugin,
    )
}

private suspend fun getModelConfig(modelId: String): ModelConfig {
    val modelConfigs = loadModelConfigs()
    return modelConfigs[modelId] ?: throw IllegalArgumentException("model $modelId not found")
}

suspend fun executeChatRequest(modelId: String, request: ChatRequest): ChatResponse {
    val modelConfig = getModelConfig(modelId)
    if (modelConfig.resolvedApiStyle == ModelApiStyle.CODEX_CLI) {
        return CodexCliService.completeChatRequest(modelConfig, request)
    }
    val chatModel = buildChatModel(modelConfig)
    return if (chatModel != null) {
        chatModel.chat(request)
    } else {
        completeChatWithStreamingModel(buildStreamingChatModel(modelConfig), request)
    }
}

private fun buildChatModel(modelConfig: ModelConfig): ChatModel? {
    if (modelConfig.resolvedApiStyle != ModelApiStyle.CHAT_COMPLETIONS) return null

    val builder = OpenAiChatModel.builder()
        .baseUrl(modelConfig.endpoint)
        .apiKey(modelConfig.apiKey)
        .modelName(modelConfig.model)
        .timeout(Duration.ofSeconds(120))
        .strictTools(true)
        .strictJsonSchema(true)

    modelConfig.resolvedReasoningEffort?.let(builder::reasoningEffort)
    modelConfig.maxOutputTokens?.let(builder::maxCompletionTokens)
    return builder.build()
}

private fun buildStreamingChatModel(modelConfig: ModelConfig): StreamingChatModel {
    if (modelConfig.resolvedApiStyle == ModelApiStyle.RESPONSES) {
        val builder = OpenAiResponsesStreamingChatModel.builder()
            .baseUrl(modelConfig.endpoint)
            .apiKey(modelConfig.apiKey)
            .modelName(modelConfig.model)
            .strict(true)

        modelConfig.resolvedReasoningEffort?.let(builder::reasoningEffort)
        modelConfig.maxOutputTokens?.let(builder::maxOutputTokens)
        return builder.build()
    }

    if (modelConfig.resolvedApiStyle == ModelApiStyle.CODEX_CLI) {
        throw IllegalStateException("Codex CLI 模型不走内部流式模型")
    }

    val builder = OpenAiStreamingChatModel.builder()
        .baseUrl(modelConfig.endpoint)
        .apiKey(modelConfig.apiKey)
        .modelName(modelConfig.model)
        .timeout(Duration.ofSeconds(300))
        .strictTools(true)
        .strictJsonSchema(true)

    modelConfig.resolvedReasoningEffort?.let(builder::reasoningEffort)
    modelConfig.maxOutputTokens?.let(builder::maxCompletionTokens)

    if (modelConfig.alias == "kimi-k2.5") {
        builder.customParameters(
            mapOf(
                "thinking" to mapOf("type" to "disabled"),  // official API
                "extra_body" to mapOf("thinking" to mapOf("type" to "disabled")),  // 兼容litellm
                "chat_template_kwargs" to mapOf("thinking" to false)  // vLLM or SGLang
            )
        )
        builder.temperature(0.6)  // 非思考模式 0.6， 思考模式1.0
    } else if (modelConfig.alias.startsWith("glm-")) {
        builder.customParameters(
            mapOf(
                "thinking" to mapOf("type" to "disabled"),  // official API
                "extra_body" to mapOf("thinking" to mapOf("type" to "disabled")),  // 兼容litellm
                "chat_template_kwargs" to mapOf("thinking" to false)  // vLLM or SGLang
            )
        )
        builder.temperature(0.7)  // 非思考模式 0.7， 思考模式1.0
    }

    return builder.build()
}

private suspend fun completeChatWithStreamingModel(model: StreamingChatModel, request: ChatRequest): ChatResponse {
    val future = CompletableFuture<ChatResponse>()
    model.chat(
        request,
        object : StreamingChatResponseHandler {
            override fun onCompleteResponse(completeResponse: ChatResponse) {
                future.complete(completeResponse)
            }

            override fun onError(error: Throwable) {
                future.completeExceptionally(error)
            }
        }
    )
    return future.await()
}
