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
    ignoreUnknownKeys = true
    coerceInputValues = true
}

/**
 * 模型提供商枚举，定义支持的模型服务类型
 */
enum class ModelProvider(val code: String, val desc: String) {
    OPENAI_COMPATIBLE("OPENAI_COMPATIBLE", "自定义模型");

    companion object {
        /**
         * 根据提供商代码获取对应的枚举值
         *
         * @param code 提供商代码
         * @return 对应的 ModelProvider 枚举值，未找到则返回 null
         */
        fun fromCode(code: String): ModelProvider? {
            return entries.find { it.code == code }
        }
    }
}

/**
 * 模型 API 协议类型，用于区分不同的接口风格
 */
@Serializable
enum class ModelApiStyle(val desc: String) {
    CHAT_COMPLETIONS("Chat Completions"),
    RESPONSES("Responses");

    override fun toString(): String = desc
}

private val REASONING_EFFORTS = setOf("low", "medium", "high", "xhigh")

/**
 * 模型配置数据类，包含模型连接和使用所需的全部参数
 */
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
    val reasoningEffort: String? = null,  // reasoning effort，主要给 Responses 模型使用
    val supportsImages: Boolean = false,  // 是否支持图片
    @kotlinx.serialization.Transient
    var icon: Icon = AllIcons.Nodes.Plugin
) {
    /** 模型的唯一标识，格式为 "提供商_模型名" */
    val id: String
        get() = "${provider}_${model}"
    /** 模型的显示别名，未设置时返回模型名 */
    val alias: String
        get() = _alias ?: model
    /** 解析后的 API 协议类型 */
    val resolvedApiStyle: ModelApiStyle
        get() = apiStyle
    /** 解析后的推理努力等级，经过标准化处理 */
    val resolvedReasoningEffort: String?
        get() = normalizeReasoningEffort(reasoningEffort)

    companion object {
        /**
         * 工厂方法，用于构建 ModelConfig 实例
         *
         * @param provider 模型提供商
         * @param model 模型名称
         * @param contextTokens 上下文最大 token 数
         * @param endpoint 接口地址
         * @param apiKey API 密钥
         * @param alias 显示别名
         * @param apiStyle API 协议类型
         * @param reasoningEffort 推理努力等级
         * @param supportsImages 是否支持图片
         * @param icon 模型图标
         * @param maxOutputTokens 最大输出 token 数
         * @return 构建好的 ModelConfig 实例
         */
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

/**
 * LLM 查询选项，包含模型标识和流式输出等配置
 *
 * @param model 模型 ID
 * @param streaming 是否启用流式输出
 * @param toolUseContext 工具调用上下文，可选
 */
data class QueryLLMOptions(
    val model: String,
    val streaming: Boolean = false,
    val toolUseContext: ToolUseContext? = null // 可空类型对应可选属性
)

/**
 * 执行聊天补全请求，将系统提示词与工具定义组合后调用 LLM
 *
 * @param messages 对话消息列表
 * @param systemPrompt 系统提示词列表
 * @param tools 可调用的工具列表
 * @param options LLM 查询选项
 * @return LLM 的聊天响应
 */
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

/**
 * 根据模型 ID 获取流式聊天模型实例
 *
 * @param modelId 模型 ID
 * @return 对应的流式聊天模型实例
 */
suspend fun getStreamChatModel(modelId: String): StreamingChatModel {
    val modelConfig = getModelConfig(modelId)
    return buildStreamingChatModel(modelConfig)
}

/**
 * 根据模型 ID 获取同步聊天模型实例，仅支持 Chat Completions 协议
 *
 * @param modelId 模型 ID
 * @return 同步聊天模型实例，不支持时返回 null
 */
suspend fun getChatModel(modelId: String): ChatModel? {
    val modelConfig = getModelConfig(modelId)
    return if (modelConfig.resolvedApiStyle != ModelApiStyle.CHAT_COMPLETIONS) null else buildChatModel(modelConfig)
}

/**
 * 获取模型列表
 */
private var CUSTOM_MODELS: List<ModelConfig> = listOf()

/**
 * 加载所有模型配置，支持强制刷新缓存
 *
 * @param forceUpdate 是否强制从文件重新加载
 * @return 以模型 ID 为键的模型配置映射
 */
suspend fun loadModelConfigs(forceUpdate: Boolean = false): Map<String, ModelConfig> {
    if (CUSTOM_MODELS.isEmpty() || forceUpdate) {
        CUSTOM_MODELS = loadCustomModelConfigs()
    }
    val modelConfigs = CUSTOM_MODELS
    JarvisCoreSettings.getInstance().ensureValidSelectedModel(modelConfigs.map { it.id }.toSet())
    return modelConfigs.associateBy { it.id }
}

/**
 * 从用户配置目录的 models.json 文件中加载自定义模型配置列表
 *
 * @return 自定义模型配置列表，加载失败返回空列表
 */
private fun loadCustomModelConfigs(): List<ModelConfig> {
    return runCatching {
        val modelFile = File(getUserConfigDirectory(), "models.json")
        if (!modelFile.exists()) {
            return emptyList()
        }
        val jsonText = modelFile.readText()
            .replace("\"CODEX_CLI\"", "\"CHAT_COMPLETIONS\"")
        val models = json.decodeFromString<List<ModelConfig>>(jsonText)
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

/**
 * 添加自定义模型配置，并持久化到 models.json 文件
 *
 * @param model 模型名称
 * @param endpoint 接口地址
 * @param apiKey API 密钥
 * @param contextTokens 上下文最大 token 数
 * @param alias 显示别名
 * @param apiStyle API 协议类型
 * @param reasoningEffort 推理努力等级
 * @param supportsImages 是否支持图片
 * @throws IllegalArgumentException 模型已存在时抛出
 */
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

/**
 * 获取当前选中的模型 ID
 *
 * @return 选中的模型 ID，未选中时返回 null
 */
fun getSelectedModelId(): String? {
    return JarvisCoreSettings.getInstance().selectedChatModelId
}

/**
 * 设置当前选中的模型，并同步更新图片支持状态
 *
 * @param modelId 要选中的模型 ID，传入 null 取消选中
 */
fun setSelectedModel(modelId: String?) {
    val settings = JarvisCoreSettings.getInstance()
    settings.selectedChatModelId = modelId
    val selectedModel = getCustomModels().firstOrNull { it.id == modelId }
    settings.modelSupportsImages = selectedModel?.supportsImages ?: false
}

/**
 * 获取当前选中的模型配置
 *
 * @return 选中的模型配置，未选中时返回 null
 */
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

/**
 * 格式化推理努力等级为可读字符串
 *
 * @param reasoningEffort 原始推理努力等级
 * @return 格式化后的字符串，为空时返回 "默认"
 */
fun formatReasoningEffort(reasoningEffort: String?): String {
    return normalizeReasoningEffort(reasoningEffort) ?: "默认"
}

/**
 * 标准化推理努力等级，仅保留合法值
 *
 * @param reasoningEffort 原始推理努力等级
 * @return 标准化后的合法值，不合法或为空时返回 null
 */
private fun normalizeReasoningEffort(reasoningEffort: String?): String? {
    val normalized = reasoningEffort?.trim()?.lowercase()?.takeIf { it.isNotBlank() } ?: return null
    return normalized.takeIf { it in REASONING_EFFORTS }
}

/**
 * 标准化模型配置，统一处理 API 协议类型和推理努力等级
 *
 * @param modelConfig 原始模型配置
 * @return 标准化后的模型配置
 */
private fun normalizeModelConfig(modelConfig: ModelConfig): ModelConfig {
    return modelConfig.copy(
        apiStyle = modelConfig.resolvedApiStyle,
        reasoningEffort = modelConfig.resolvedReasoningEffort,
        icon = AllIcons.Nodes.Plugin,
    )
}

/**
 * 根据模型 ID 获取模型配置
 *
 * @param modelId 模型 ID
 * @return 对应的模型配置
 * @throws IllegalArgumentException 模型未找到时抛出
 */
private suspend fun getModelConfig(modelId: String): ModelConfig {
    val modelConfigs = loadModelConfigs()
    return modelConfigs[modelId] ?: throw IllegalArgumentException("model $modelId not found")
}

/**
 * 执行聊天请求，优先使用同步模型，不支持时回退到流式模型收集完整响应
 *
 * @param modelId 模型 ID
 * @param request 聊天请求
 * @return 聊天响应
 */
suspend fun executeChatRequest(modelId: String, request: ChatRequest): ChatResponse {
    val modelConfig = getModelConfig(modelId)
    val chatModel = buildChatModel(modelConfig)
    return if (chatModel != null) {
        chatModel.chat(request)
    } else {
        completeChatWithStreamingModel(buildStreamingChatModel(modelConfig), request)
    }
}

/**
 * 根据模型配置构建同步聊天模型（仅支持 Chat Completions 协议）
 *
 * @param modelConfig 模型配置
 * @return 同步聊天模型实例，协议不匹配时返回 null
 */
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

/**
 * 根据模型配置构建流式聊天模型，支持 Responses 和 Chat Completions 两种协议
 *
 * @param modelConfig 模型配置
 * @return 流式聊天模型实例
 */
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

/**
 * 使用流式模型收集完整响应，通过 CompletableFuture 等待流式回调完成
 *
 * @param model 流式聊天模型
 * @param request 聊天请求
 * @return 完整的聊天响应
 */
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
