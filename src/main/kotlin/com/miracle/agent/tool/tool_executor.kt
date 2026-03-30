package com.miracle.agent.tool

import com.intellij.openapi.diagnostic.Logger
import com.miracle.agent.*
import com.miracle.agent.parser.ErrorSegment
import com.miracle.agent.parser.Segment
import com.miracle.agent.parser.ToolSegment
import com.miracle.utils.AgentTraceUtils
import com.miracle.utils.ChatHistoryAssistantMessage
import com.miracle.utils.ChatHistoryUserMessage
import com.miracle.utils.ChatMessageStore
import com.miracle.utils.CheckpointStorage
import com.miracle.utils.JsonFieldExtractor
import com.miracle.ui.smartconversation.settings.configuration.ChatMode
import com.miracle.utils.extensions.asBooleanOrNull
import com.miracle.utils.extensions.asDoubleOrNull
import com.miracle.utils.extensions.asFloatOrNull
import com.miracle.utils.extensions.asIntOrNull
import com.miracle.utils.extensions.asLongOrNull
import com.miracle.utils.extensions.asStringOrNull
import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.ToolExecutionResultMessage
import dev.langchain4j.data.message.UserMessage.userMessage
import dev.langchain4j.memory.ChatMemory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.future.await
import kotlinx.serialization.json.*
import kotlinx.serialization.serializerOrNull
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspendBy
import kotlin.reflect.jvm.isAccessible


/**
 * 工具执行器，负责工具调用请求的校验、参数转换、用户授权和实际执行。
 * 处理工具调用的完整生命周期：参数语法修复 -> 参数校验 -> 用户授权 -> 参数转换 -> 执行 -> 结果返回。
 */
object ToolExecutor{

    private val LOG = Logger.getInstance(ToolExecutor::class.java)
    // JSON 解析器实例，忽略未知键以兼容不同版本的工具参数
    private val json = Json {
        ignoreUnknownKeys = true
    }

    /**
     * 检查工具是否存在
     * @param toolRequest 工具调用请求
     * @param taskState 当前任务状态
     * @return 工具实例，不存在时返回 null
     */
    fun checkToolExists(toolRequest: ToolExecutionRequest, taskState: TaskState): Tool<*>? {
        val tool = taskState.tools[toolRequest.name()]
        if (tool == null) {
//            val uiErrorMsg = "Jarvis tried to use ${toolRequest.name()} but it does not exist. Retrying..."
            val availableToolNames = taskState.tools.values.map { it.getName() }.toList().joinToString(", ")
            val aiErrorMsg = AiResponse.toolNotExists(toolRequest.name(), availableToolNames)
            toolCallError(taskState, toolRequest.id(), toolRequest.name(), aiErrorMsg, null)
        }
        return tool
    }

    /**
     * 尝试修复一些参数的语法错误，这里不校验工具本身是否存在
     * @return
     * - Boolean: json 是否可以继续
     * - ToolExecutionRequest：修复后的请求
     */
    private fun tryRepairParamSyntax(toolRequest: ToolExecutionRequest, taskState: TaskState): Pair<Boolean, ToolExecutionRequest> {
        var newToolRequest = toolRequest
        var finalJsonArgs = toolRequest.arguments().trim()
        var needReplaceToolArgs = false
        if (finalJsonArgs.isEmpty()) {
            finalJsonArgs = "{}"
            needReplaceToolArgs = true
        }
        if (!finalJsonArgs.startsWith("{")) {
            finalJsonArgs = "{ $finalJsonArgs"
            needReplaceToolArgs = true
        }
        if (!finalJsonArgs.endsWith("}")) {
            finalJsonArgs = "$finalJsonArgs }"
            needReplaceToolArgs = true
        }
        try {
            Json.decodeFromString<JsonElement>(finalJsonArgs)
        } catch (_: Exception) {
            LOG.info("Error parsing JSON arguments for ${toolRequest.name()}: \n  $finalJsonArgs")
//            val uiErrorMsg = "Jarvis tried to use ${toolRequest.name()} with an invalid JSON argument. Retrying..."
            val aiErrorMsg = AiResponse.toolError("Invalid JSON argument for ${toolRequest.name()}. Please retry with a properly formatted JSON argument.")
            newToolRequest = replaceToolCallParams(taskState.chatMemory, "{}", toolRequest)
            toolCallError(taskState, toolRequest.id(), toolRequest.name(), aiErrorMsg, null)
            return false to newToolRequest
        }

        if (needReplaceToolArgs) {
            newToolRequest = replaceToolCallParams(taskState.chatMemory, finalJsonArgs, toolRequest)
        }
        return true to newToolRequest
    }

    /**
     * 校验参数是否合法
     */
    private suspend fun validateParam(toolRequest: ToolExecutionRequest, taskState: TaskState): Boolean {
        val tool = taskState.tools[toolRequest.name()]!!
        try {
            val element = Json.decodeFromString<JsonElement>(toolRequest.arguments())
            tool.validateInput(element, taskState)
        } catch (e: MissingToolParameterException) {
            val aiErrorMsg = AiResponse.toolError("Missing value for required parameter '${e.parameterName}'. Please retry with complete response.")
//            val uiErrorMsg = "Jarvis tried to use ${e.toolName}${e.filePath?.let { " for $it" } ?: ""} without value for required parameter '${e.parameterName}'. Retrying..."
            toolCallError(taskState, toolRequest.id(), toolRequest.name(), aiErrorMsg, null)
            return false
        } catch (e: Throwable) {
            LOG.warn("Error validating arguments for ${toolRequest.name()}: ${e.message}")
//            val uiErrorMsg = "Jarvis tried to use ${toolRequest.name()} got an error: ${e.message}. Retrying..."
            val aiErrorMsg = AiResponse.toolError(e.message)
            toolCallError(taskState, toolRequest.id(), toolRequest.name(), aiErrorMsg, null)
            return false
        }
        return true
    }

    /**
     * 替换聊天记录中 AI 消息里指定工具请求的参数内容
     * @param chatMemory 聊天记忆存储
     * @param args 新的参数 JSON 字符串
     * @param curToolRequest 当前工具请求
     * @return 更新后的工具请求
     */
    private fun replaceToolCallParams(chatMemory: ChatMessageStore<ChatMessage>, args: String, curToolRequest: ToolExecutionRequest): ToolExecutionRequest {
        var messages = chatMemory.messages()
        val aiMessageIndex = messages.indexOfLast { it is AiMessage }.takeIf { it != -1 } ?: return curToolRequest
        messages = messages.toMutableList()
        messages[aiMessageIndex].let { aiMessage ->
            aiMessage as AiMessage
            val requests = aiMessage.toolExecutionRequests().toMutableList()
            val requestIndex = requests.indexOfFirst { it.id() == curToolRequest.id() }.takeIf { it != -1 } ?: return curToolRequest
            requests[requestIndex] = ToolExecutionRequest.builder()
                .id(curToolRequest.id())
                .name(curToolRequest.name())
                .arguments(args)
                .build()

            messages[aiMessageIndex] = AiMessage.builder()
                .text(aiMessage.text())
                .toolExecutionRequests(requests)
                .attributes(aiMessage.attributes())
                .build()
            chatMemory.update(messages)
            return requests[requestIndex]
        }
    }

    /**
     * 记录工具调用错误信息，包括 UI 展示和 AI 上下文
     * @param taskState 当前任务状态
     * @param toolRequestId 工具请求 ID
     * @param toolName 工具名称
     * @param aiErrorMsg 返回给 AI 的错误信息
     * @param uiErrorMsg 展示给用户的错误信息（可选）
     */
    private fun toolCallError(taskState: TaskState, toolRequestId: String, toolName: String, aiErrorMsg: String, uiErrorMsg: String? = null) {
        uiErrorMsg?.let {
            taskState.emit!!(JarvisSay(toolRequestId, AgentMessageType.ERROR, listOf(ErrorSegment(uiErrorMsg))))
        }
        val toolResult = ToolExecutionResultMessage.from(toolRequestId, toolName, aiErrorMsg)
        taskState.chatMemory.add(toolResult)

        AgentTraceUtils.saveToolResponseError(taskState.convId, taskState.taskId, taskState.curMessageId,
            taskState.modelConfig!!, aiErrorMsg, toolName, taskState.toolRequestLog, project = taskState.project)
    }

    /**
     * 工具调用请求流式返回，参数此时未打印全
     * 模型在流式返回工具参数得时候，会循环调用此方法
     * 像Write工具，content参数较大，此时可以考虑流式渲染内容
     */
    suspend fun handlePartialBlock(id: String, toolName: String, partialArguments: String, taskState: TaskState) {
        // NOTE: We don't push tool results in partial blocks because this is only for UI streaming.
        // The ToolExecutor will add tool result message to AI when the complete block is processed.
        // This maintains separation of concerns: partial = UI updates, complete = final state changes.
        if (partialArguments.length < 16) {
            return
        }
        val tool = taskState.tools[toolName] ?: return
        try {
            val fields = JsonFieldExtractor.extractFields(partialArguments)
            val toolSegment = tool.handlePartialBlock(id, fields, taskState)
            toolSegment?.let {
                val autoApprove = shouldAutoApproveTool(toolName, toolSegment.toolCommand, taskState.project)
                if (autoApprove) {
                    taskState.emit!!(JarvisSay(id, AgentMessageType.TOOL, listOf(toolSegment), true))
                } else {
                    taskState.emit!!(JarvisAsk(id, AgentMessageType.TOOL, listOf(toolSegment), true))
                }
            }
        } catch (e: ToolParameterException) {
            throw e
        } catch (_: Throwable) {}

    }

    /**
     * 调用工具，此时模型已经将工具得所有参数输出完成
     *
     * @param toolRequest 工具调用请求
     * @param taskState 任务状态
     * @return 调用工具是否成功
     */
    suspend fun callTool(toolRequest: ToolExecutionRequest, taskState: TaskState): Boolean {
        // 1. 调用工具前进行校验
        val tool = checkToolExists(toolRequest, taskState) ?: return false
        val (isParamValid, newToolRequest) = tryRepairParamSyntax(toolRequest, taskState)
        if (!isParamValid) return false
        val validateResult = validateParam(newToolRequest, taskState)
        if (!validateResult) return false

        try {
            // 2. 询问用户是否允许执行此工具
            val json = Json.parseToJsonElement(newToolRequest.arguments()).jsonObject
            // 是否允许工具执行
            if (!ensureToolAuthorization(newToolRequest.arguments(), newToolRequest, taskState)) return false

            // 3. 组装参数，返回工具的execute函数引用
            val func = tool.getExecuteFunc()
            val params = func.parameters.filter { it.kind == KParameter.Kind.VALUE }
            val args = convertToolArgs(params, json, taskState, newToolRequest)

            // 4. 调用工具
            func.isAccessible = true
            val result = if (func.isSuspend) {
                func.callSuspendBy(args)
            } else {
                func.callBy(args)
            }

            // 5. 组装工具调用结果给AI模型
            taskState.chatMemory.add(ToolExecutionResultMessage.from(newToolRequest, result.resultForAssistant))
            AgentTraceUtils.saveToolResponse(taskState.convId, taskState.taskId, taskState.curMessageId, taskState.modelConfig!!,
                result.resultForAssistant, toolRequest.name(), AgentTraceUtils.Status.SUCCESS, taskState.toolRequestLog, project = taskState.project)
            tool.afterAddToolResult(result.data, taskState)
            return true
        } catch (e: Throwable) {
            if (e is CancellationException) {
                LOG.info("Cancel execute tool ${tool.getName()} due to job was cancelled")
                val toolResult = ToolExecutionResultMessage.from(toolRequest, AiResponse.toolCancelled())
                taskState.chatMemory.add(toolResult)
                AgentTraceUtils.saveToolResponse(taskState.convId, taskState.taskId, taskState.curMessageId, taskState.modelConfig!!,
                    toolResult.text(), toolRequest.name(), AgentTraceUtils.Status.CANCELLED, taskState.toolRequestLog, project = taskState.project)
            } else {
                val uiErrorMsg = if (e is InvocationTargetException) e.targetException.message else {
                    "Error executing ${newToolRequest.name()}: ${e.message}"
                }
                val aiErrorMsg = AiResponse.toolError(uiErrorMsg)
                LOG.warn(uiErrorMsg, e)
                toolCallError(taskState, toolRequest.id(), toolRequest.name(), aiErrorMsg, uiErrorMsg)
            }
            return false
        } finally {
            taskState.askUserResponse = null
        }
    }

    /**
     * 在工具执行前，获取/判断是否有授权
     * @return 如果授权校验通过，返回True
     */
    private suspend fun ensureToolAuthorization(jsonStr: String, toolRequest: ToolExecutionRequest, taskState: TaskState): Boolean {
        // 通知UI，本次工具调用的展示已经结束
        val tool = taskState.tools[toolRequest.name()]!!
        val args = JsonFieldExtractor.extractFields(jsonStr)
        val toolSegment = tool.handlePartialBlock(toolRequest.id(), args, taskState, isPartial = false)!!
        taskState.historyAiMessage.segments.add(toolSegment)
        // 更新 ToolRequest 的额外字段
        if (tool.getName() == TaskTool.getName()) {
            toolSegment.params["subagent_type"]?.jsonPrimitive?.contentOrNull?.let {
                taskState.toolRequestLog!!.subAgentType = it
                AgentTraceUtils.saveTraceLog(taskState.toolRequestLog!!)
            }
        } else {
            // 需要更新 MCP 相关字段
            val mcpMetaInfo = tool.getMcpMetaInfo()
            if (mcpMetaInfo.isNotEmpty()) {
                val mcpRequestLog = taskState.toolRequestLog!!
                mcpRequestLog.mcpServer = mcpMetaInfo[Tool.MCP_SERVER_NAME_KEY]
                mcpRequestLog.mcpTool = mcpMetaInfo[Tool.MCP_TOOL_NAME_KEY]
                AgentTraceUtils.saveTraceLog(mcpRequestLog)
            }
        }

        // 工具调用授权处理
        val filePath = toolSegment.toolCommand
        val autoApprove = shouldAutoApproveTool(toolRequest.name(), filePath, taskState.project)
        if (autoApprove) {
            recordCheckpointForWriteEdit(toolRequest, taskState, toolSegment)
            taskState.emit!!(JarvisSay(toolRequest.id(), AgentMessageType.TOOL, listOf(toolSegment)))
        } else {
            taskState.askFutures[toolRequest.id()] = CompletableFuture<AskResponse>()
            taskState.emit!!(JarvisAsk(toolRequest.id(), AgentMessageType.TOOL, listOf(toolSegment)))
            // 阻塞等待用户输入
            val askResp = taskState.askFutures[toolRequest.id()]!!.await()
            taskState.askFutures.remove(toolRequest.id())
            return when(askResp.type) {
                AskResponse.ResponseType.YES -> {
                    recordCheckpointForWriteEdit(toolRequest, taskState, toolSegment)
                    true
                }
                AskResponse.ResponseType.NO -> {
                    val toolResult = ToolExecutionResultMessage.from(toolRequest, AiResponse.toolDenied())
                    taskState.chatMemory.add(toolResult)
                    AgentTraceUtils.saveToolResponse(taskState.convId, taskState.taskId, taskState.curMessageId,
                        taskState.modelConfig!!, toolResult.text(), toolRequest.name(),
                        AgentTraceUtils.Status.CANCELLED, taskState.toolRequestLog, project = taskState.project)
                    false
                }
                AskResponse.ResponseType.MESSAGE -> {
                    val toolResult = ToolExecutionResultMessage.from(toolRequest, "The user provided the following feedback:\n<feedback>\n${askResp.message}\n</feedback>")
                    taskState.chatMemory.add(toolResult)
                    taskState.chatMemory.add(userMessage(askResp.message))
                    AgentTraceUtils.saveToolResponse(taskState.convId, taskState.taskId, taskState.curMessageId,
                        taskState.modelConfig!!, toolResult.text(), toolRequest.name(),
                        AgentTraceUtils.Status.SUCCESS, taskState.toolRequestLog, project = taskState.project)
                    // 有新的用户消息，需要结束之前的AiMessage
                    if (taskState.historyAiMessage.segments.isNotEmpty()) {
                        val segments = mutableListOf<Segment>()
                        segments.addAll(taskState.historyAiMessage.segments)
                        val historyAiMessageCopy = ChatHistoryAssistantMessage(segments = segments)
                        taskState.chatHistory.add(historyAiMessageCopy)
                    }
                    // 用户本次答复放到UserMessage
                    taskState.chatHistory.add(ChatHistoryUserMessage(askResp.message))
                    taskState.historyAiMessage.segments.clear()

                    false
                }
            }
        }
        return true
    }

    /**
     * 在工具执行完成后，为 Write/Edit 工具记录文件检查点
     * @param toolRequest 工具调用请求
     * @param taskState 当前任务状态
     * @param toolSegment 工具展示片段
     */
    private fun recordCheckpointForWriteEdit(toolRequest: ToolExecutionRequest, taskState: TaskState, toolSegment: ToolSegment) {
        if ((toolRequest.name() == WriteTool.getName() || toolRequest.name() == EditTool.getName()) &&
            !taskState.userMessageId.isNullOrBlank()
        ) {
            CheckpointStorage.recordFileCheckpointIfAbsent(
                taskState.project,
                taskState.convId,
                taskState.userMessageId!!,
                toolSegment.toolCommand
            )
        }
    }

    /**
     * 将Json中的字段，转换成工具需要的参数列表
     * @param params 工具参数列表
     * @param jsonObj 模型返回的工具调用参数
     */
    private fun convertToolArgs(params: List<KParameter>, jsonObj: JsonObject, taskState: TaskState, toolRequest: ToolExecutionRequest): Map<KParameter, Any?> {
        val args = mutableMapOf<KParameter, Any?>()
        params.forEach { param ->
            val paramName = param.name ?: throw ToolParameterException("Unnamed parameter")
            val paramType = param.type.classifier as? KClass<*> ?: throw ToolParameterException("Unknown parameter type: ${param.type} for param $paramName")
            if (paramType == TaskState::class) {
                args[param] = taskState
                return@forEach
            }
            if (paramType == ToolExecutionRequest::class) {
                args[param] = toolRequest
                return@forEach
            }
            if (paramType == JsonObject::class && paramName == "originParams") {
                args[param] = jsonObj
            }

            // 检查参数是否在JSON中提供
            val jsonElement = jsonObj[paramName]
            if (jsonElement != null) {
                // 参数在JSON中提供，进行解析
                try {
                    val value = when (paramType) {
                        String::class -> jsonElement.asStringOrNull()
                        Int::class -> jsonElement.asIntOrNull()
                        Boolean::class -> jsonElement.asBooleanOrNull()
                        Double::class -> jsonElement.asDoubleOrNull()
                        Long::class -> jsonElement.asLongOrNull()
                        Float::class -> jsonElement.asFloatOrNull()
                        else -> {
                            // 复杂类型必须标记kotlin的@Serializable注解
                            val serializer = serializerOrNull(param.type)
                                ?: throw ToolParameterException("Cannot get serializer for parameter: ${param.name}")
                            if (jsonElement is JsonPrimitive && jsonElement.isString) {
                                val innerJson = Json.parseToJsonElement(jsonElement.content)
                                json.decodeFromJsonElement(serializer, innerJson)
                            } else {
                                json.decodeFromJsonElement(serializer, jsonElement)
                            }
                        }
                    }
                    args[param] = value
                } catch (_: Exception) {
                    throw ToolParameterException("Invalid parameter value for param $paramName: ${jsonElement.jsonPrimitive.content}")
                }
            } else if (param.isOptional) {
                // 参数是可选的（有默认值），不提供则使用默认值
                // 不添加到args中，Kotlin反射会自动使用默认值
            } else {
                throw ToolParameterException("Missing required parameter: $paramName")
            }
        }
        return args
    }
}
