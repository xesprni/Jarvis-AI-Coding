package com.qifu.ui.smartconversation.sse

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.ui.content.Content
import com.qifu.agent.*
import com.qifu.agent.parser.Segment
import com.qifu.agent.parser.ToolSegment
import com.qifu.agent.parser.UiToolName
import com.qifu.agent.tool.Tool
import com.qifu.agent.tool.ToolRegistry
import com.qifu.ui.smartconversation.panels.DiffWindowHolder.closeDiffView
import com.qifu.ui.smartconversation.settings.configuration.ChatMode
import com.qifu.ui.smartconversation.settings.configuration.SmartCoroutineScope
import com.qifu.ui.smartconversation.settings.service.TaskCompletionParameters
import com.qifu.utils.CheckpointStorage
import com.qihoo.finance.lowcode.common.factory.ChatXToolWindowFactory
import com.qihoo.finance.lowcode.common.factory.ChatXToolWindowFactory.TAB_AGENT_NAME
import com.qihoo.finance.lowcode.smartconversation.panels.SmartToolWindowPanel
import com.qihoo.finance.lowcode.smartconversation.service.CompletionEventListener
import com.qihoo.finance.lowcode.smartconversation.utils.ErrorDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException


/**
 * @author weiyichao
 * @date 2025-09-30
 **/
@Service(Service.Level.PROJECT)
class AgentCompletionRequestService(project: Project) {
    private val taskMap = ConcurrentHashMap<String, Task>()
    private val jobMap = ConcurrentHashMap<String, Job>()
    private val askMap = ConcurrentHashMap<String, String>()
    private val callbackEventMap = ConcurrentHashMap<String, CompletionEventListener<Segment>>()


    /**
     * 聊天完成（异步）- 直接替换原有的getChatCompletionAsync
     */
    fun getChatCompletionAsync(
        callParameters: TaskCompletionParameters, eventListener: CompletionEventListener<Segment>?, project: Project
    ): AgentJob {
        // 取消之前的任务
        //jobMap[taskId]?.cancel()

        return startAgentCompletion(callParameters, eventListener, project)
    }

    /**
     * 启动Agent完成任务（你的核心逻辑）
     */
    private fun startAgentCompletion(
        callParameters: TaskCompletionParameters, eventListener: CompletionEventListener<Segment>?, project: Project
    ): AgentJob {
        val taskId = callParameters.taskId
        val userMessageId = callParameters.message.id.toString()
        val userInput = callParameters.message.prompt
        val modelId = callParameters.modelId
        val chatModel = callParameters.chatMode
        val referencedFiles = callParameters.referencedFiles?.stream()?.map { file -> file.filePath }?.toList()
        val imageFiles = callParameters.imageDetailsList
        val hasCustomInput =  callParameters.hasCustomInput
        // 初始化任务
        if (taskMap[taskId] == null) {
            CheckpointStorage.initMessageContextCheckpoint(project, taskId, userMessageId)
            val tools: List<Tool<*>> = when (chatModel) {
                ChatMode.ASK -> ToolRegistry.getAskTools().values.toList()
                ChatMode.PLAN -> ToolRegistry.getPlanTools().values.toList()
                else -> ToolRegistry.getAll().values.toList()
            }
            taskMap[taskId] = Task(
                taskId = taskId,
                userMessageId = userMessageId,
                userInput = userInput,
                tools = tools,
                images = imageFiles,
                files = referencedFiles,
                modelId = modelId,
                chatMode = chatModel,
                project = project,
                updateConvTitle = this::handleUpdateTitle
            )

            // 解决用户自定义输入时UI适配
            callbackEventMap[taskId] = eventListener!!

            val smartCoroutineScope = ApplicationManager.getApplication().getService(SmartCoroutineScope::class.java)

            val job = smartCoroutineScope.coroutineScope().launch {
                try {
                    // 通知开始
                    withContext(Dispatchers.EDT) {
                        callbackEventMap[taskId]!!.onOpen()
                    }

                    // 执行你的Agent任务循环
                    taskMap[taskId]!!.startTaskLoop().collect { event ->
                        withContext(Dispatchers.EDT) {
                            processAgentEvent(event, callbackEventMap[taskId]!!, taskId)
                        }
                    }

                    // 任务结束
                    taskMap.remove(taskId)
                    withContext(Dispatchers.EDT) {
                        callbackEventMap[taskId]!!.onComplete(StringBuilder())
                    }

                } catch (e: CancellationException) {
                    withContext(Dispatchers.EDT) {
                        callbackEventMap[taskId]!!.onError(ErrorDetails(e.message), e)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.EDT) {
                        callbackEventMap[taskId]!!.onError(ErrorDetails(e.message), e)
                    }
                }
            }
            jobMap[taskId] = job
            return AgentJob(job, project)
        } else {
            // 关闭 Diff 窗口
            closeDiffView(taskId)
            var message: String? = null
            if (hasCustomInput) {
                callbackEventMap[taskId] = eventListener!!
            }
            val type = when {
                userInput.trim().startsWith("approve", ignoreCase = true) -> {
                    val userEdit = userInput.substringAfter("approve").trim()
                    if (userEdit.isNotEmpty()) {
                        message = userEdit
                    }
                    AskResponse.ResponseType.YES
                }
                userInput.trim().equals("reject", ignoreCase = true) -> AskResponse.ResponseType.NO
                else -> {
                    message = userInput
                    AskResponse.ResponseType.MESSAGE
                }
            }
            val askId = askMap[taskId]
            val askResp = AskResponse(id = askId!!, type = type, message = message)
            taskMap[taskId]!!.askResponse(askResp)
            askMap.remove(taskId)
            callbackEventMap[taskId]?.onPartialMessageComplete(
                taskId,
                askId,
                hasCustomInput,
                !userInput.trim().equals("reject", ignoreCase = true)
            )

            return AgentJob(jobMap[taskId]!!, project)
        }


    }

    /**
     * 处理Agent事件
     */
    private fun processAgentEvent(
        event: AgentMessage, eventListener: CompletionEventListener<Segment>?, taskId: String
    ) {
        when (event.type) {
            AgentMessageType.ERROR -> {
                event.data.let { segments ->
                    segments.forEach { segment ->
                        eventListener!!.onMessage(segment)
                    }
                }
            }

            AgentMessageType.TOOL -> {
                event.data.let { segments ->
                    segments.forEach { segment ->
                        eventListener!!.onToolMessage(event.id, segment, event.isPartial)
                    }
                }
            }

            else -> {
                // 普通 AI 响应
                event.data.forEach { segment ->
                    eventListener!!.onMessage(segment)
                }
            }
        }
        if (!event.isPartial && event is JarvisAsk) {
            // 模拟展示 Approve/Reject 按钮
            askMap[taskId] = event.id
            var hasAskUserQuestion = false
            event.data.forEach { segment ->
                if (segment is ToolSegment) {
                    val name: UiToolName = segment.name
                    if (name == UiToolName.ASK_USER_QUESTION) {
                        hasAskUserQuestion = true
                    }
                }
            }
            eventListener!!.onPartialMessage(taskId, hasAskUserQuestion)
        }
    }


    private fun handleUpdateTitle(taskId: String, title: String) {
        val tabbPanel = Arrays.stream(ChatXToolWindowFactory.getToolWindow().contentManager.contents)
            .filter { content: Content? -> TAB_AGENT_NAME == content!!.tabName }.findFirst();
        tabbPanel.ifPresent {
            val chatToolWindowPanel = tabbPanel.get().component as SmartToolWindowPanel
            chatToolWindowPanel.chatTabbedPane.renameTab(taskId, title)
        }
        return
    }

    fun switchModel(taskId: String, modelId: String) {
        taskMap[taskId]?.switchModel(modelId)
    }

    fun cancel(taskId: String) {
        taskMap.remove(taskId)
        askMap.remove(taskId)
        jobMap[taskId]?.cancel()
    }
}
