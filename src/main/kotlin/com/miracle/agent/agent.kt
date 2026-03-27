package com.miracle.agent

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.miracle.agent.AskResponse.ResponseType
import com.miracle.agent.mcp.McpPromptIntegration
import com.miracle.agent.parser.*
import com.miracle.agent.tool.Tool
import com.miracle.agent.tool.ToolExecutor
import com.miracle.agent.tool.ToolRegistry
import com.miracle.config.AutoApproveSettings
import com.miracle.services.*
import com.miracle.ui.smartconversation.settings.configuration.ChatMode
import com.miracle.ui.smartconversation.settings.configuration.SmartCoroutineScope
import com.miracle.utils.*
import com.miracle.utils.image.ImageUtil
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.data.image.Image
import dev.langchain4j.data.message.*
import dev.langchain4j.data.message.ChatMessageSerializer.messageToJson
import dev.langchain4j.data.message.SystemMessage.systemMessage
import dev.langchain4j.data.message.UserMessage.userMessage
import dev.langchain4j.kotlin.model.chat.chat
import dev.langchain4j.kotlin.model.chat.request.chatRequest
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.chat.request.ResponseFormat
import dev.langchain4j.model.chat.request.ResponseFormatType
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import dev.langchain4j.model.chat.request.json.JsonSchema
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.math.min

// ---------------- data class
data class TaskState(
    val taskId: String,
    val convId: String,
    val userMessageId: String? = null,
    val chatMemory: ChatMessageStore<ChatMessage>,
    val chatHistory: ChatMessageStore<ChatHistoryMessage>,
    val tools: Map<String, Tool<*>>,
    var modelId: String,
    var chatMode: ChatMode = ChatMode.AGENT,
    val agentId: String,
    var complete: Boolean = false,       // 是否已结束
    var abort: Boolean = false,          // 是否被中断
    val project: Project,

    var apiRequestCount: Int = 0,
    var consecutiveMistakeCount: Int = 0,   // 连续的错误次数

    var aiMessageFuture: CompletableFuture<AiMessage> = CompletableFuture<AiMessage>(),  // 一次请求模型是否已经返回结束
    var askFutures: MutableMap<String, CompletableFuture<AskResponse>> = mutableMapOf(),
    var waitingAskUserQuestion: Boolean = false,
    var askUserResponse: String? = null,
    var toolCallFutures: List<CompletableFuture<ToolExecutionResultMessage>> = mutableListOf(),

    var emit: ((AgentMessage) -> Unit)? = null,      // 给UI发送事件
    var refreshToolSpecs: (() -> Unit)? = null,      // 刷新工具规范的回调

    var systemReminderService: SystemReminderService? = null,
    var fileFreshnessService: FileFreshnessService? = null,

    var subTask: Task? = null,
    var shell: PersistentShell? = null,
    var shellCreateException: Exception? = null,

    val historyAiMessage: ChatHistoryAssistantMessage = ChatHistoryAssistantMessage(),
    var mcpPrompt: String? = null,  // 临时方案，后续应该mcp工具注册到tool registry

    var curMessageId: String = UUID.randomUUID().toString(),
    var modelConfig: ModelConfig? = null,
    var toolRequestLog: AgentTraceLog? = null,  // 工具调用前保存，参数校验通过后需要更新（存储sub_agent, mcp等额外字段）
) {
    private data class CachedServices(
        val convId: String,
        val agentId: String,
        val project: Project,
        val systemReminderService: SystemReminderService,
        val fileFreshnessService: FileFreshnessService,
    )

    companion object {
        private val serviceCache = mutableMapOf<String, CachedServices>()

        @JvmStatic
        @Synchronized
        fun clearCachedServices(taskId: String) {
            serviceCache.remove(taskId)?.let { cached ->
                cached.systemReminderService.resetSession()
                cached.fileFreshnessService.resetSession()
            }
        }
    }

    init {
        val cachedServices = synchronized(TaskState::class.java) {
            serviceCache[taskId]?.takeIf { cached ->
                cached.convId == convId && cached.agentId == agentId && cached.project == project
            } ?: run {
                serviceCache[taskId]?.let { stale ->
                    stale.systemReminderService.resetSession()
                    stale.fileFreshnessService.resetSession()
                }
                val reminderService = SystemReminderService(convId, agentId, project)
                val freshnessService = FileFreshnessService(convId, reminderService::emitEvent)
                CachedServices(
                    convId = convId,
                    agentId = agentId,
                    project = project,
                    systemReminderService = reminderService,
                    fileFreshnessService = freshnessService,
                ).also { serviceCache[taskId] = it }
            }
        }
        systemReminderService = cachedServices.systemReminderService
        systemReminderService!!.chatMode = chatMode
        fileFreshnessService = cachedServices.fileFreshnessService
        try {
            // 创建持久化的shell实例用于执行命令
            shell = PersistentShell(getCurrentProjectRootPath())
        } catch (e: Exception) {
            shellCreateException = e
        }
        // 生成MCP提示词
        mcpPrompt = McpPromptIntegration.generateMcpPromptSection(project)
    }
}


/**
 * task service
 * @param userInput user text inputs
 * @param images the images file path
 * @param files the file paths user input
 */
class Task(
    var taskId: String? = null,
    var convId: String? = null,
    val agentId: String = getDefaultAgentId(),
    val userMessageId: String? = null,
    private val userInput: String? = null,
    private val tools: List<Tool<*>>,
    private val images: List<String>? = null,
    private val files: List<String>? = null,
    private val modelId: String,
    private val chatMode: ChatMode = ChatMode.AGENT,
    private var convTitle: String? = null,
    private val systemPrompt: String? = null,
    val historyAiMessage: ChatHistoryAssistantMessage = ChatHistoryAssistantMessage(),
    val isMainAgent: Boolean = true,
    val project: Project = getCurrentProject()!!,
    private val updateConvTitle: (taskId: String, title: String) -> Unit = {_, _ -> },
) {

    private val LOG = Logger.getInstance(Task::class.java)
    val taskState: TaskState
    private var model: StreamingChatModel? = null
    private var toolSpecs: List<ToolSpecification>? = null
    private var currentModelId: String = modelId
    private val effectiveTools = ToolRegistry.filterToolsForMode(chatMode, tools)

    init {
        val hasNewUserInput = userInput != null || images != null || files != null
        if (isMainAgent) {
            if (convId == null) convId = taskId
            if (convId != null) {
                var conv = ConversationStore.getConversation(project, convId!!)
                if (conv == null) {
                    conv = Conversation(id = convId!!, title = convTitle ?: "chat", projectPath = toPosixPath(project.basePath!!))
                    ConversationStore.updateConversation(project, conv)
                } else {
                    convTitle = conv.title
                }
            } else if (userInput != null || images != null || files != null) {
                val conv = Conversation(title = convTitle ?: "chat", projectPath = toPosixPath(project.basePath!!))
                ConversationStore.updateConversation(project, conv)
                taskId = conv.id
                convId = conv.id
            } else {
                throw IllegalArgumentException("taskId or userInput/images must be provided")
            }
            if (hasNewUserInput) {
                ConversationStore.getConversation(project, convId!!)?.let { conversation ->
                    conversation.updatedTime = System.currentTimeMillis()
                    ConversationStore.updateConversation(project, conversation)
                }
            }
        }

        // message store
        val memoryStore = JsonLineChatMemory(convId!!, agentId, project)
        val historyStore = JsonLineChatHistory(convId!!, project)
        // 组装 userMessage
        taskState = TaskState(
            taskId = taskId!!,
            convId = convId!!,
            userMessageId = userMessageId,
            agentId = agentId,
            chatMemory = memoryStore,
            chatHistory = historyStore,
            modelId = currentModelId,
            chatMode = chatMode,
            tools = effectiveTools.associateBy { it.getName() },
            historyAiMessage = historyAiMessage,
            project = project,
        )
        taskState.systemReminderService!!.chatMode = chatMode
        if (userInput != null || images != null || files != null) {
            val contents = mutableListOf<Content>()

            files?.forEach {
                val fullPath = normalizeFilePath(it, project)
                val file = File(fullPath)
                if (!file.exists() || !file.isFile) {
                    contents.add(TextContent("<file_content path=\"$fullPath\">\nError fetching content: File not exists.\n</file_content>"))
                    return@forEach
                }
                taskState.fileFreshnessService!!.recordFileRead(fullPath)
                var fileContent = readTextContent(it, 0)
                fileContent = addLineNumbers(fileContent)
                fileContent = truncateToCharBudget(fileContent)
                contents.add(TextContent("<file_content path=\"$fullPath\">\n$fileContent\n</file_content>"))
            }
            images?.forEach {
                val base64Str = ImageUtil.pathToImageDetails(it)
                contents.add(
                    ImageContent(
                        Image.builder()
                            .url(base64Str)
                            .build(),
                        ImageContent.DetailLevel.AUTO
                    )
                )
                if (isMainAgent) {
                    val conv = ConversationStore.getConversation(project, convId!!)!!
                    conv.containsImg = true
                    ConversationStore.updateConversation(project, conv)
                }
            }
            if (!userInput.isNullOrEmpty()) {
                contents.add(TextContent(userInput))
            }
            taskState.chatMemory.add(userMessage(contents))
            if (isMainAgent) {
                taskState.chatHistory.add(ChatHistoryUserMessage(
                    text=userInput,
                    messageId = userMessageId,
                    referencedFilePaths = files,
                    imageFilePaths = images,
                ))
            }

            // 业务观测
            if (isMainAgent) {
                SmartCoroutineScope.get()?.launch {
                    val modelConfig = loadModelConfigs()[currentModelId]!!
                    AgentTraceUtils.saveUserRequest(convId?: taskId, taskId, UUID.randomUUID().toString(), modelConfig, userInput, project = project);
                }
            }

            // 生成标题
            if (isMainAgent) {
                convTitle ?: SmartCoroutineScope.get()?.launch {
                    generateConversationTitle(taskState)
                    // 上报观测会话接口
                    TraceUtils.saveConversation(TraceConversation(taskId!!, convTitle ?: "Code Agent Chat (JetBrain Plugin)"))
                    TraceUtils.saveAgentConversation(AgentConversation(taskId!!, convTitle ?: "Code Agent Chat (JetBrain Plugin)",
                        "local"
                    ))
                }
            }
        }
    }

    /**
     * 开启task循环
     * // 调用示例：
     * val job = CoroutineScope(Dispatchers.Default).launch {
     *     startTaskLoop().collect {
     *         println(it)
     *     }
     * }
     *
     * // 取消示例
     * delay(5000)
     * job.cancel()
     */
    fun startTaskLoop(): Flow<AgentMessage> = callbackFlow {
        try {
            taskState.emit = { trySend(it) }
            taskState.refreshToolSpecs = { refreshToolSpecs() }
            toolSpecs = ToolRegistry.getToolSpecifications(taskState.tools.values.toList())
            taskState.apiRequestCount = 0

            refreshModelIfNeeded(force = true)

            while (isActive) {
                // 如果超过了自动允许的api请求数，询问用户是否继续
                if (isOverAutoAllowLimit()) break
                // 每轮对话messageId
                taskState.curMessageId = UUID.randomUUID().toString()
                try {
                    refreshModelIfNeeded()
                    // 请求模型
                    val aiMessage = recursivelyMakeJarvisRequests() ?: continue
                    if (!aiMessage.text().isNullOrBlank()) {
                        val parser = CompleteMessageParser()
                        val segments = parser.parse(aiMessage.text()!!)
                            .filter { it is TextSegment || it is Code || it is SearchReplace || it is ProposedPlanSegment }
                        taskState.historyAiMessage.segments.addAll(segments)
                    }
                    // 工具调用
                    if (aiMessage.hasToolExecutionRequests()) {
                        for (toolRequest in aiMessage.toolExecutionRequests()) {
                            // 埋点，工具观测
                            taskState.toolRequestLog = AgentTraceUtils.saveToolRequest(convId, taskId, taskState.curMessageId,
                                taskState.modelConfig!!, toolRequest.arguments(), toolRequest.name(),
                                taskState.agentId.takeIf { it != "default" }, project = project)
                            // 前面工具调用失败，继续执行后续工具
                            if (!ToolExecutor.callTool(toolRequest, taskState)) {
                                processRequestError()
                            } else {
                                taskState.consecutiveMistakeCount = 0
                            }
                        }
                    } else {
                        // deepbank观测终态数据
                        TraceUtils.saveMessage(Message(taskState.curMessageId, taskId, userInput, messageToJson(aiMessage)))
                        break
                    }
                } catch (e: Throwable) {
                    // 埋点观测
                    if (e is CancellationException) {
                        AgentTraceUtils.saveAiResponse(convId, taskId, taskState.curMessageId, taskState.modelConfig!!, "Job was cancelled.",
                            AgentTraceUtils.Status.CANCELLED, taskState.agentId.takeIf { it != "default" }, project = project)
                        taskState.abort = true
                    } else {
                        AgentTraceUtils.saveAiResponseError(convId, taskId, taskState.curMessageId, taskState.modelConfig!!, e.message,
                            taskState.agentId.takeIf { it != "default" }, project = project)
                        // deepbank观测
                        TraceUtils.saveMessage(Message(taskState.curMessageId, taskId, userInput, e.message))
                        if (!processRequestError(e)) break
                    }
                }
            }
        } catch (e: Throwable) {
            processRequestError(e, true)
        }

        // 通知调用者流已经结束
        taskState.complete = true
        taskState.shell?.close()
        if (isMainAgent && taskState.historyAiMessage.segments.isNotEmpty()) {
            taskState.chatHistory.add(taskState.historyAiMessage)
        }
        close()
    }

    private suspend fun refreshModelIfNeeded(force: Boolean = false) {
        val desiredModelId = taskState.modelId
        if (!force && desiredModelId == currentModelId && model != null && taskState.modelConfig != null) return

        val modelConfig = loadModelConfigs()[desiredModelId]
            ?: throw IllegalArgumentException("model $desiredModelId not found")
        validateImageSupport(modelConfig, taskState.project)
        model = getStreamChatModel(desiredModelId)
        taskState.modelConfig = modelConfig
        currentModelId = desiredModelId
    }

    /**
     * 根据当前 chatMode 刷新工具规范
     * 当 chatMode 动态切换时（如进入/退出 Plan 模式）需要调用此方法
     */
    fun refreshToolSpecs() {
        toolSpecs = ToolRegistry.getToolSpecifications(taskState.tools.values.toList())
    }

    private fun validateImageSupport(modelConfig: ModelConfig, project: Project) {
        val conversationId = convId ?: return
        val conversation = ConversationStore.getConversation(project, conversationId) ?: return
        if (conversation.containsImg && !modelConfig.supportsImages) {
            throw IllegalArgumentException("模型 ${modelConfig.alias} 不支持图片，请更换模型或开启一个新会话。")
        }
    }

    // 发起模型请求
    private suspend fun recursivelyMakeJarvisRequests(): AiMessage? {
        taskState.apiRequestCount++

        // 注入system reminder 和 project instructions
        taskState.systemReminderService!!.injectSystemReminder(taskState.chatMemory)

        // 上下文压缩
        var curMessages = taskState.chatMemory.messages()
        val lastUserMessage = curMessages.lastOrNull().takeIf { it is UserMessage }
        val autoCompactResult = checkAutoCompact(
            currentModelId,
            lastUserMessage?.let{ curMessages.toMutableList().apply { removeLast() }} ?: curMessages,
            taskState
        )
        if (autoCompactResult.wasCompacted) {
            curMessages = autoCompactResult.messages.toMutableList()
            lastUserMessage?.let { curMessages.add(it) }
            taskState.chatMemory.update(curMessages)
            taskState.systemReminderService!!.resetSession()
        } else {
            curMessages = taskState.chatMemory.messages()
        }

        // 发起请求
        val systemPrompt = systemMessage(this.systemPrompt ?: PromptService.getSystemPrompt(currentModelId, taskState.project, taskState.chatMode, taskState.convId))
        taskState.aiMessageFuture = CompletableFuture<AiMessage>()
        model!!.chat(chatRequest {
            messages += systemPrompt
            messages += curMessages
            parameters {
                if (taskState.modelConfig!!.alias.contains("claude")) {
                    // 最后一条消息的token数不是太准，这里留 4k 的buff
                    val leftToken = taskState.modelConfig!!.contextTokens - countTokens(messages) - 4096
                    maxOutputTokens = min(leftToken, taskState.modelConfig!!.maxOutputTokens ?: 0)
                    maxOutputTokens?.let { if (it <= 0) maxOutputTokens = null }
                }
                toolSpecifications = toolSpecs
            }
        }, ChatResponseHandler(taskState))

        // 等待请求完成
        val aiMessage = taskState.aiMessageFuture.await()

        // 有时候模型会返回空消息
        if (aiMessage.text() == null && !aiMessage.hasToolExecutionRequests()) {
            LOG.warn("empty ai message: ${messageToJson(aiMessage)}")
            processRequestError()
            return null
        }
        // glm 有时候会返回不带工具id或者name的调用，会导致之后的失败
        if (aiMessage.hasToolExecutionRequests()) {
            for (toolRequest in aiMessage.toolExecutionRequests()) {
                if (toolRequest.id().isNullOrBlank() || toolRequest.name().isNullOrBlank()) {
                    LOG.warn("tool request without id or name: ${messageToJson(aiMessage)}")
                    return null
                }
            }
        }

        AgentTraceUtils.saveAiResponse(convId, taskId, taskState.curMessageId, taskState.modelConfig!!, messageToJson(aiMessage), subAgentType = taskState.agentId.takeIf { it != "default" }, project = project)
        taskState.chatMemory.add(aiMessage)
        return aiMessage
    }

    // 检查是否超过了自动允许的api请求数
    private suspend fun isOverAutoAllowLimit(): Boolean {
        val maxApiRequestCount = AutoApproveSettings.state.maxRequests
        if (taskState.apiRequestCount >= maxApiRequestCount) {
            val ask = JarvisAsk(
                AgentMessageType.AUTO_APPROVAL_MAX_REQ_REACHED.name,
                type = AgentMessageType.AUTO_APPROVAL_MAX_REQ_REACHED,
                data = listOf(TextSegment(UiResponse.autoApprovalMaxReqReached(maxApiRequestCount)))
            )
            taskState.askFutures[ask.id] = CompletableFuture<AskResponse>()
            taskState.emit!!(ask)

            val askResp = taskState.askFutures[ask.id]!!.await()
            return when (askResp.type) {
                AskResponse.ResponseType.YES -> {
                    taskState.apiRequestCount = 0
                    false
                }

                AskResponse.ResponseType.NO -> {
                    taskState.abort = true
                    true
                }

                else -> {
                    taskState.apiRequestCount = 0
                    askResp.message?.let {
                        taskState.chatMemory.add(userMessage(askResp.message))
                    }
                    false
                }
            }

        }
        return false
    }

    /**
     * 处理请求模型的错误
     * @return true 表示已经处理成功，false 则代表要终止 loop
     */
    private suspend fun processRequestError(e: Throwable? = null, forceStop: Boolean = false): Boolean {
        // 通知用户请求报错
        e?.let {
            LOG.warn("Jarvis request error:", e)
            taskState.emit?.let {
                val segment = ErrorSegment("Error: ${e.message ?: e::class.simpleName}")
                taskState.historyAiMessage.segments.add(segment)
                it(
                    JarvisSay(
                        id = AgentMessageType.ERROR.name,
                        type = AgentMessageType.ERROR,
                        data = listOf(segment)
                    )
                )
            }
        }

        if (forceStop) {
            taskState.complete = true
            return false
        }

        taskState.consecutiveMistakeCount++
        if (taskState.consecutiveMistakeCount < 3) return true

        // 连续错误超过3次，询问用户是否继续
        taskState.askFutures[AgentMessageType.API_REQ_FAILED.name] = CompletableFuture<AskResponse>()
        taskState.emit!!(
            JarvisAsk(
                id = AgentMessageType.API_REQ_FAILED.name, type = AgentMessageType.API_REQ_FAILED,
                data = listOf(TextSegment(UiResponse.consecutiveApiReqFailed()))
            )
        )

        val askResp = taskState.askFutures[AgentMessageType.API_REQ_FAILED.name]!!.await()
        taskState.consecutiveMistakeCount = 0
        return when (askResp.type) {
            AskResponse.ResponseType.YES -> {
                true
            }

            AskResponse.ResponseType.NO -> {
                taskState.abort = true
                false
            }

            AskResponse.ResponseType.MESSAGE -> {
                taskState.chatMemory.add(userMessage(askResp.message))
                true
            }
        }
    }

    /**
     * Jarvis 询问用户，用户响应后调用此方法继续
     */
    fun switchModel(newModelId: String) {
        if (newModelId.isBlank()) return
        taskState.modelId = newModelId
        taskState.subTask?.switchModel(newModelId)
    }

    fun askResponse(response: AskResponse) {
            taskState.subTask?.let {
            taskState.subTask!!.askResponse(response)
        } ?: run {
            taskState.askUserResponse = response.message
            if (taskState.waitingAskUserQuestion) {
                taskState.askFutures[response.id]!!.complete(AskResponse(response.id, ResponseType.YES))
            } else {
                if (response.type == ResponseType.NO) {
                    CodeAudit.markRejected(response.id)
                }
                taskState.askFutures[response.id]!!.complete(response)
            }
        }
    }

    /**
     * 生成会话标题
     */
    @Serializable
    data class ConversationTitle(
        val isNewTopic: Boolean,
        val title: String?,
    )

    private suspend fun generateConversationTitle(taskState: TaskState) {
        val messages = taskState.chatMemory.messages()
        val systemMessage = systemMessage(PromptService.getConvTitlePrompt())
        try {
            val conv = ConversationStore.getConversation(taskState.project, taskState.convId)!!
            val response = executeChatRequest(
                modelId = currentModelId,
                request = chatRequest {
                    this.messages.add(systemMessage)
                    this.messages.addAll(messages)
                    parameters {
                        this.responseFormat = ResponseFormat.builder()
                            .type(ResponseFormatType.JSON)
                            .jsonSchema(
                                JsonSchema.builder()
                                    .name("Conversation")
                                    .rootElement(
                                        JsonObjectSchema.builder()
                                            .addBooleanProperty("isNewTopic")
                                            .addStringProperty("title")
                                            .required("isNewTopic")
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    }
                }
            )
            response.aiMessage().text()?.let {jsonStr ->
                val cleaned = jsonStr
                    .replace(Regex("^```(?:json)?", RegexOption.MULTILINE), "")
                    .replace(Regex("```$", RegexOption.MULTILINE), "")
                    .trim()
                // 更新会话标题
                convTitle = Json.decodeFromString<ConversationTitle>(cleaned).title
                conv.title = convTitle ?: "chat"
                ConversationStore.updateConversation(taskState.project, conv)
                conv.title?.let { updateConvTitle(taskState.taskId, it) }
            }
        } catch (e: Throwable) {
            LOG.warn("generate conversation title error:", e)
        }
    }

}
