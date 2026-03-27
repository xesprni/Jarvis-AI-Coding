package com.miracle.agent.tool

import com.miracle.agent.TaskState
import com.miracle.agent.parser.ToolSegment
import com.miracle.utils.JsonField
import dev.langchain4j.agent.tool.ToolSpecification
import kotlinx.serialization.json.JsonElement
import kotlin.reflect.KFunction


data class ToolCallResult<T>(
    val type: String = "result",
    val data: T,
    val resultForAssistant: String
)

/**
 * Agent 工具接口
 */
interface Tool<TOutput> {

    companion object {
        const val MCP_SERVER_NAME_KEY = "mcpServerName"
        const val MCP_TOOL_NAME_KEY = "mcpToolName"
        const val MCP_REMOTE_SERVER_NAME_KEY = "mcpRemoteServerName"
    }
    /**
     * 工具名称
     */
    fun getName(): String {
        return getToolSpecification().name()
    }

    /**
     * 校验入参
     */
    suspend fun validateInput(input: JsonElement, taskState: TaskState) {}

    /**
     * 获取工具的描述
     */
    fun getToolSpecification(): ToolSpecification

    /**
     * 工具的参数会流式输出，每次流式输出时会触发此方法
     * handlePartialBlock 方法负责UI更新，不负责推送工具结果
     */
    suspend fun handlePartialBlock(toolRequestId: String, partialArgs: Map<String, JsonField>, taskState: TaskState, isPartial: Boolean = true): ToolSegment? = null

    /**
     * 返回工具执行方法实例（返回的方法可以是 suspend）
     */
    fun getExecuteFunc(): KFunction<ToolCallResult<TOutput>>

    /**
     * 渲染工具执行结果给Assistant
     */
    fun renderResultForAssistant(output: TOutput): String


    fun getMcpMetaInfo(): Map<String, String> = emptyMap()

    /**
     * 添加工具执行结果后触发
     */
    suspend fun afterAddToolResult(data: Any?, taskState: TaskState) {}

}

data class ToolUseContext(
    val requestId: String? = null,
    val messageId: String? = null,
    val agentId: String? = null,
    val safeMode: Boolean? = null,
    val readFileTimestamps: Map<String, Long> = emptyMap(),
    val options: Options = Options(),
    val responseState: ResponseState? = null
) {
    data class Options(
        val commands: List<Any>? = null,
        val tools: List<Tool<*>> = emptyList(),
        val verbose: Boolean? = null,
        val slowAndCapableModel: String? = null,
        val safeMode: Boolean? = null,
        val forkNumber: Int? = null,
        val messageLogName: String? = null,
        val maxThinkingTokens: Any? = null,
        val isKodingRequest: Boolean? = null,
        val kodingContext: String? = null,
        val isCustomCommand: Boolean? = null
    )

    data class ResponseState(
        val previousResponseId: String? = null,
        val conversationId: String? = null
    )
}

//----- Exceptions
sealed class ToolException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

// 工具未找到
class ToolNotFoundException(name: String) :
    ToolException("Tool not found: $name")

// 必填参数缺失
class MissingToolParameterException(val toolName: String, val parameterName: String, val filePath: String? = null) :
    ToolException("Missing required tool parameter: $parameterName")

// 参数相关错误（解析失败、类型不匹配等）
class ToolParameterException(reason: String) :
    ToolException(reason)

// 工具执行时抛出的异常（如反射调用异常）
class ToolExecutionException(message: String, cause: Throwable? = null) :
    ToolException(message, cause)