package com.qifu.services

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.IconLoader
import com.qifu.agent.tool.Tool
import com.qifu.agent.tool.ToolRegistry
import com.qifu.agent.tool.ToolUseContext
import com.qifu.external.LowcodeApi
import com.qifu.external.LowcodeApiUtils
import com.qifu.utils.Debug
import com.qifu.utils.getUserConfigDirectory
import com.qifu.utils.getCurrentRequest
import com.qifu.utils.markPhase
import com.qihoo.finance.lowcode.common.constants.Constants.Headers.MODEL_ID
import com.qihoo.finance.lowcode.common.util.Icons
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings
import com.squareup.wire.Duration
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.SystemMessage.systemMessage
import dev.langchain4j.kotlin.model.chat.chat
import dev.langchain4j.model.chat.response.ChatResponse
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiStreamingChatModel
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import javax.swing.Icon

private var LOG = Logger.getInstance("com.qifu.services.model")
@OptIn(ExperimentalSerializationApi::class)
private val json = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
}

enum class ModelProvider(val code: String, val desc: String) {
    JARVIS("JARVIS", "内置模型"),
    OPENAI_COMPATIBLE("OPENAI_COMPATIBLE", "自定义模型");

    companion object {
        fun fromCode(code: String): ModelProvider? {
            return entries.find { it.code == code }
        }
    }
}

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
    val supportsImages: Boolean = false,  // 是否支持图片
    val iconUrl: String = "", // icon 图标链接
    val isLimit: Boolean = false, // 是否限量使用
    @kotlinx.serialization.Transient
    var icon: Icon = Icons.LOGO_ROUND13
) {
    val id: String
        get() = "${provider}_${model}"
    val alias: String
        get() = _alias ?: model

    companion object {
        fun from(
            provider: ModelProvider,
            model: String,
            contextTokens: Int,
            endpoint: String,
            apiKey: String? = null,
            alias: String? = null,
            supportsImages: Boolean = false,
            iconUrl: String = "",
            isLimit: Boolean = false,
            icon: Icon = Icons.LOGO_ROUND13,
            maxOutputTokens: Int? = null,
        ): ModelConfig {
            return ModelConfig(
                provider = provider,
                model = model,
                _alias = alias,
                endpoint = endpoint,
                apiKey = apiKey,
                contextTokens = contextTokens,
                supportsImages = supportsImages,
                iconUrl = iconUrl,
                isLimit = isLimit,
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
    val model = getChatModel(options.model)
    return model.chat {
        this.messages += allMessages
        parameters {
            this.toolSpecifications = ToolRegistry.getToolSpecifications(tools)
        }
    }
}

suspend fun getStreamChatModel(modelId: String): OpenAiStreamingChatModel {
    val modelConfigs = loadModelConfigs()
    val modelConfig = modelConfigs[modelId] ?: throw IllegalArgumentException("model $modelId not found")
    val headers = LowcodeApiUtils.getCommonHeaders().toMutableMap()
    headers[MODEL_ID] = modelConfig.alias
    val builder = OpenAiStreamingChatModel.builder()
        .baseUrl(modelConfig.endpoint)
        .apiKey(modelConfig.apiKey)
        .customHeaders(headers) // 调用lowcode app 的 chat_completion用此鉴权
        .modelName(modelConfig.model)
        .timeout(Duration.ofSeconds(300))
        .strictTools(true)
        .strictJsonSchema(true)
    if (modelConfig.alias == "kimi-k2.5") {
        builder.customParameters(mapOf(
            "thinking" to mapOf("type" to "disabled"),  // official API
            "extra_body" to mapOf("thinking" to mapOf("type" to "disabled")),  // 兼容litellm
            "chat_template_kwargs" to mapOf("thinking" to false)  // vLLM or SGLang
        ))
        builder.temperature(0.6)  // 非思考模式 0.6， 思考模式1.0
//        builder.returnThinking(true)
//        builder.sendThinking(true)
    } else if (modelConfig.alias.startsWith("glm-")) {
        builder.customParameters(mapOf(
            "thinking" to mapOf("type" to "disabled"),  // official API
            "extra_body" to mapOf("thinking" to mapOf("type" to "disabled")),  // 兼容litellm
            "chat_template_kwargs" to mapOf("thinking" to false)  // vLLM or SGLang
        ))
        builder.temperature(0.7)  // 非思考模式 0.7， 思考模式1.0
    }
    return builder.build()
}

suspend fun getChatModel(modelId: String): OpenAiChatModel {
    val modelConfigs = loadModelConfigs()
    val modelConfig = modelConfigs[modelId] ?: throw IllegalArgumentException("model $modelId not found")
    val headers = LowcodeApiUtils.getCommonHeaders().toMutableMap()
    headers[MODEL_ID] = modelConfig.alias
    return OpenAiChatModel.builder()
        .baseUrl(modelConfig.endpoint)
        .apiKey(modelConfig.apiKey)
        .customHeaders(headers)  // 调用lowcode app 的 chat_completion用此鉴权
        .modelName(modelConfig.model)
        .timeout(Duration.ofSeconds(120))
        .strictTools(true)
        .strictJsonSchema(true)
        .build()
}

/**
 * 获取模型列表
 */
private var JARVIS_MODELS: List<ModelConfig> = listOf()
private var CUSTOM_MODELS: List<ModelConfig> = listOf()
suspend fun loadModelConfigs(forceUpdate: Boolean = false): Map<String, ModelConfig> {
    if (JARVIS_MODELS.isEmpty() || forceUpdate) {
        JARVIS_MODELS = LowcodeApi.getJarvisModels()
        CUSTOM_MODELS = loadCustomModelConfigs()

        JARVIS_MODELS.forEach { modelConfig ->
            val iconUrl = modelConfig.iconUrl
            if (iconUrl.isBlank()) return@forEach
            try {
                val dir = getIconDirectory()
                val fileName = iconUrl.substringAfterLast("/")  // e.g. a.svg
                val localFile = File(dir, fileName)
                if (!localFile.exists()) {
                    downloadFile(iconUrl, localFile)
                }
                modelConfig.icon = loadSvgIcon(localFile)
            } catch (e: Exception) {
                LOG.warn("⚠️ 图标加载失败: ${modelConfig.id} -> ${e.message}")
            }
        }

    }
    val modelConfigs = JARVIS_MODELS + CUSTOM_MODELS
    // 用户如果还没选过模型，则设置成默认模型
    if (ChatxApplicationSettings.settings().chatModelId == null) {
        modelConfigs.firstOrNull{ it.alias == ChatxApplicationSettings.settings().defaultJarvisModelId }?.let {
            ChatxApplicationSettings.settings().chatModelId = it.id
        }
    }
    return modelConfigs.associateBy { it.id }
}


private fun getIconDirectory(): File {
    val dir = File(getUserConfigDirectory(), "icons")

    if (!dir.exists()) {
        dir.mkdirs()  // 自动创建目录
    }

    return dir  // ✅ 返回目录 File 对象
}

private fun downloadFile(url: String, dest: File) {
    URL(url).openStream().use { input ->
        FileOutputStream(dest).use { output ->
            input.copyTo(output)
        }
    }
}

/**
 * 从 SVG 文件加载 Icon，直接在目标大小渲染，避免马赛克
 */
private fun loadSvgIcon(file: File): Icon {
    return try {
        IconLoader.findIcon(file.toURI().toURL())!!
    } catch (e: Exception) {
        LOG.warn("⚠️ 图标解析失败: ${file.name} (${e.message})")
        Icons.LOGO_ROUND13 // fallback 默认图标
    }
}

private fun loadCustomModelConfigs(): List<ModelConfig> {
    return runCatching {
        val modelFile = File(getUserConfigDirectory(), "models.json")
        if (!modelFile.exists()) {
            return emptyList()
        }
        val jsonText = modelFile.readText()
        val models = Json.decodeFromString<List<ModelConfig>>(jsonText)
        return models
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

fun addCustomModel(model: String, endpoint: String, apiKey: String, contextTokens: Int, alias: String? = null, supportsImages: Boolean = false) {
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
        endpoint = endpoint,
        apiKey = apiKey,
        contextTokens = contextTokens,
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
 * 更新自定义模型
 */
fun updateCustomModel(oldModel: String, newModel: String, endpoint: String, apiKey: String?, contextTokens: Int, alias: String? = null, supportsImages: Boolean = false) {
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
        endpoint = endpoint,
        apiKey = resolvedApiKey,
        contextTokens = contextTokens,
        supportsImages = supportsImages
    )
    
    val updatedConfigs = modelConfigs.toMutableList()
    updatedConfigs[existingIndex] = updatedModel
    
    CUSTOM_MODELS = updatedConfigs
    val jsonText = json.encodeToString(CUSTOM_MODELS)
    val modelFile = File(getUserConfigDirectory(), "models.json")
    modelFile.writeText(jsonText)
}
