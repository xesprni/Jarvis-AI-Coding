package com.qifu.external

import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.diagnostic.Logger
import com.qifu.services.ModelConfig
import com.qifu.services.ModelProvider
import com.qifu.utils.AgentConfigFileInfo
import com.qifu.utils.AgentConfigInfo
import com.qifu.utils.AgentConversation
import com.qifu.utils.AgentTraceLog
import com.qifu.utils.ConfigDeleteInfo
import com.qifu.utils.Message
import com.qifu.utils.TraceConversation
import com.qihoo.finance.lowcode.common.configuration.UserInfoPersistentState
import com.qihoo.finance.lowcode.common.constants.Constants
import com.qihoo.finance.lowcode.common.constants.ServiceErrorCode
import com.qihoo.finance.lowcode.common.util.LowCodeAppUtils
import com.qihoo.finance.lowcode.common.util.NotifyUtils
import com.qihoo.finance.lowcode.common.util.RestTemplateUtil
import com.qihoo.finance.lowcode.common.util.ResultHelper
import com.qihoo.finance.lowcode.gentracker.tool.PluginUtils
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private val LOG = Logger.getInstance("com.qifu.external.lowcode_api")

// --------------- DTO
@Serializable
data class LowcodeResult<T>(
    val errorMsg: String? = null,
    val errorCode: String? = null,
    val success: Boolean = false,
    val data: T? = null
) {
    fun isFail(): Boolean = !success
}

@Serializable
data class McpMarketService(
    val mcpId: String,
    val name: String? = null,
    val githubUrl: String? = null,
    val description: String? = null,
    val logoUrl: String? = null,
    val category: String? = null,
    val orgName: String? = null,
    val tags: List<String>? = null,
    val downloadCount: Int? = null,
    val isRecommended: Boolean? = null
)

@Serializable
data class McpMarketServiceTemplate(
    val npxTemplate: Template? = null,
    val dockerTemplate: Template? = null,
    val uvxTemplate: Template? = null,
    val sseTemplate: Template? = null,
) {
    @Serializable
    data class Template(
        val content: String? = null,
        val parameters: List<Parameter>? = null
    )
    @Serializable
    data class Parameter(
        val name: String? = null,
        val description: String? = null,
        val required: Boolean? = null,
        val defaultValue: JsonPrimitive? = null
    )

    @Serializable
    data class TemplateContent(
        val disable: Boolean = false,
        val timeout: Double? = null,
        val type: String? = null,
        val command: String? = null,
        val args: List<String>? = null,
        val env: Map<String, String>? = null,
        val useJarvisAuth: Boolean = false,
        val url: String? = null,
    )
}


// --------------- API
object LowcodeApi {

    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun getJarvisModels(): List<ModelConfig> {
        val response = client.get(Constants.Url.GET_JARVIS_MODEL) {
            headers {
                LowcodeApiUtils.getCommonHeaders().forEach { (k, v) -> append(k, v)}
            }
        }
        val result: LowcodeResult<List<JsonElement>> = response.body()
        LowcodeApiUtils.handleResult(result, Constants.Url.GET_JARVIS_MODEL, "获取Jarvis模型列表失败", true)
        return result.data?.mapNotNull  { model ->
            val modelId = model.jsonObject["modelId"]?.jsonPrimitive?.contentOrNull
            val modelAlias = model.jsonObject["jarvisModelId"]?.jsonPrimitive?.contentOrNull
            val contextTokens = model.jsonObject["context"]?.jsonPrimitive?.intOrNull
            val maxOutputTokens = model.jsonObject["maxOutput"]?.jsonPrimitive?.intOrNull
            val supportImages = model.jsonObject["supportsImages"]?.jsonPrimitive?.booleanOrNull ?: false
            val iconUrl = model.jsonObject["iconUrl"]?.jsonPrimitive?.contentOrNull ?: ""
            val isLimit = model.jsonObject["isLimit"]?.jsonPrimitive?.booleanOrNull ?: false

            if (modelId.isNullOrEmpty() || contextTokens == null) {
                null
            } else {
                ModelConfig.from(ModelProvider.JARVIS, modelId, contextTokens, "${Constants.Url.getHost()}/v2",
                    null, modelAlias, supportImages, iconUrl, isLimit, maxOutputTokens=maxOutputTokens)
            }
        } ?: emptyList()
    }

    suspend fun insertConversationInfo(conversation: TraceConversation): Boolean? {
        client.post(Constants.Url.CONVERSATION_TRACE_URL) {
            // 请求头
            headers {
                LowcodeApiUtils.getCommonHeaders().forEach { (k, v) -> append(k, v)}
            }
            // 2. 指定内容类型为 JSON
            contentType(ContentType.Application.Json)
            // 3. 设置请求体
            setBody(conversation)
        }
        return true;
    }

    suspend fun insertMessageInfo(message: Message): Boolean {
        client.post(Constants.Url.MESSAGE_TRACE_URL) {
            headers {
                LowcodeApiUtils.getCommonHeaders().forEach { (k, v) -> append(k, v)}
            }
            // 2. 指定内容类型为 JSON
            contentType(ContentType.Application.Json)
            // 3. 设置请求体
            setBody(message)
        }
        return true;
    }

    suspend fun getMcpMarketServices(): List<McpMarketService> {
        val response = client.post(Constants.Url.GET_MCP_MARKET_SERVERS) {
            headers {
                LowcodeApiUtils.getCommonHeaders().forEach { (k, v) -> append(k, v) }
            }
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { })
        }
        val result: LowcodeResult<List<McpMarketService>> = response.body()
        LowcodeApiUtils.handleResult(result, Constants.Url.GET_MCP_MARKET_SERVERS, "获取 MCP 市场数据失败", true)
        return result.data ?: emptyList()
    }

    suspend fun getMcpServerById(mcpId: String): McpMarketServiceTemplate? {
        val url = Constants.Url.GET_MCP_SERVER_BY_ID + URLEncoder.encode(mcpId, StandardCharsets.UTF_8).replace("+", "%20")
        val response = client.get(url) {
            headers {
                LowcodeApiUtils.getCommonHeaders().forEach { (k, v) -> append(k, v) }
            }
        }
        val result: LowcodeResult<McpMarketServiceTemplate> = response.body()
        LowcodeApiUtils.handleResult(result, url, "获取 MCP 服务配置失败", true)
        return result.data
    }

    suspend fun insertBusinessInfo(agentTraceLog: AgentTraceLog): Boolean {
        client.post(Constants.Url.BUSINESS_TRACE_URL) {
            headers {
                LowcodeApiUtils.getCommonHeaders().forEach { (k, v) -> append(k, v)}
            }
            // 2. 指定内容类型为 JSON
            contentType(ContentType.Application.Json)
            // 3. 设置请求体
            setBody(agentTraceLog)
        }
        return true;
    }

    suspend fun insertAgentConfig(agentConfig: AgentConfigInfo): String {
        val response = client.post(Constants.Url.AGENT_CONFIG_TRACE_URL) {
            headers {
                LowcodeApiUtils.getCommonHeaders().forEach { (k, v) -> append(k, v)}
            }
            // 2. 指定内容类型为 JSON
            contentType(ContentType.Application.Json)
            // 3. 设置请求体
            setBody(agentConfig)
        }
        val result: LowcodeResult<String> = response.body()
        return result.data ?: ""
    }

    suspend fun insertAgentConfigFile(configFile: AgentConfigFileInfo): String {
        val response = client.post(Constants.Url.AGENT_CONFIG_FILE_TRACE_URL) {
            headers {
                LowcodeApiUtils.getCommonHeaders().forEach { (k, v) -> append(k, v)}
            }
            // 2. 指定内容类型为 JSON
            contentType(ContentType.Application.Json)
            // 3. 设置请求体
            setBody(configFile)
        }
        val result: LowcodeResult<String> = response.body()
        return result.data ?: ""
    }

    suspend fun deleteAgentConfigFile(request: ConfigDeleteInfo): Boolean {
        client.post(Constants.Url.AGENT_CONFIG_FILE_CLEAR_URL) {
            headers {
                LowcodeApiUtils.getCommonHeaders().forEach { (k, v) -> append(k, v)}
            }
            // 2. 指定内容类型为 JSON
            contentType(ContentType.Application.Json)
            // 3. 设置请求体
            setBody(request)
        }
        return true
    }

    suspend fun addAgentConversation(agentConversation: AgentConversation): Boolean {
        client.post(Constants.Url.AGENT_CONVERSATION_ADD_URL) {
            headers {
                LowcodeApiUtils.getCommonHeaders().forEach { (k, v) -> append(k, v)}
            }
            // 2. 指定内容类型为 JSON
            contentType(ContentType.Application.Json)
            // 3. 设置请求体
            setBody(agentConversation)
        }
        return true
    }

}


// --------------- Utils
object LowcodeApiUtils {
    fun checkInvalidateToken(result: LowcodeResult<*>): Boolean {
        return result.isFail() && result.errorCode == Constants.ResponseCode.TOKEN_INVALID
    }

    fun checkForceUpdate(result: LowcodeResult<*>): Boolean {
        return result.isFail() && result.errorCode == ServiceErrorCode.PLUGIN_VERSION_TOO_OLD.code
    }

    fun getCommonHeaders(): Map<String, String> = buildMap {
        put(Constants.Headers.MAC, RestTemplateUtil.getLocalMac())
        put(Constants.Headers.VERSION, PluginUtils.getPluginVersion())
        put(Constants.Headers.IDE_VERSION, ApplicationInfo.getInstance().fullVersion)

        UserInfoPersistentState.getUserInfo()?.let { user ->
            put(Constants.Headers.EMAIL, user.email)
            put(Constants.Headers.TOKEN, user.token)
        }
    }

    fun notifyIfNeed(result: LowcodeResult<*>, url: String, notifyMsg: String?, notifyErr: Boolean) {
        var message = notifyMsg
        val errorCode = result.errorCode
        val serviceError = ServiceErrorCode.getByCode(errorCode)

        if (serviceError != null) {
            message = result.errorMsg
        }

        // 优先使用后端返回的具体错误信息
        if (message.isNullOrEmpty() && !result.errorMsg.isNullOrEmpty()) {
            message = result.errorMsg
        }

        if (!message.isNullOrEmpty()) {
            LOG.warn("$message, 错误码: ${result.errorCode}, 接口: $url", )
        } else {
            LOG.warn("Jarvis加载失败${LowCodeAppUtils.ADD_NOTIFY}, 错误码: ${result.errorCode}, 接口: $url")
        }

        if (notifyErr) {
            // 优先使用后端返回的具体错误信息，只有在没有具体错误信息时才使用默认提示
            val finalNotifyMsg = message ?: result.errorMsg ?: "Jarvis加载失败${LowCodeAppUtils.ADD_NOTIFY}"

            // 提醒时间间隔
            if (NotifyUtils.checkNotifyTimeInterval(finalNotifyMsg)) {
                NotifyUtils.notify(finalNotifyMsg, NotificationType.ERROR)
            }
        }
    }

    fun handleResult(
        result: LowcodeResult<*>,
        url: String,
        notifyIfFail: String,
        notifyErr: Boolean
    ) {
        if (result.success) return

        // token失效登录失效, 重定向到登录页面
        when {
            checkInvalidateToken(result) -> ResultHelper.logoutRedirect(url)
            checkForceUpdate(result) -> ResultHelper.notifyUpdateVersion(result.errorMsg)
            result.isFail() -> notifyIfNeed(result, url, notifyIfFail, notifyErr)
        }
    }
}
