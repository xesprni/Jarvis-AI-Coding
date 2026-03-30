package com.miracle.agent.tool

import com.intellij.openapi.components.service
import com.miracle.agent.AgentMessageType
import com.miracle.agent.JarvisSay
import com.miracle.agent.Task
import com.miracle.agent.TaskState
import com.miracle.agent.parser.ToolSegment
import com.miracle.agent.parser.UiToolName
import com.miracle.services.AgentService
import com.miracle.services.PromptService
import com.miracle.services.PromptService.getTaskDescription
import com.miracle.utils.JsonField
import com.miracle.utils.sanitizeFileName
import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import kotlinx.serialization.json.*
import java.util.*
import kotlin.reflect.KFunction


/**
 * 子任务工具，用于将复杂任务委托给专门的子 Agent 执行
 */
object TaskTool: Tool<String> {

    /**
     * 获取工具名称
     * @return 工具名称 "Task"
     */
    override fun getName(): String {
        return "Task"
    }

    /**
     * 获取工具规格定义，动态获取任务描述以支持项目级别的子 Agent
     * @return Task 工具的规格定义
     */
    override fun getToolSpecification(): ToolSpecification {
        // 因为有项目级别的 subAgent，这里的描述需要每次动态获取
        return ToolSpecification.builder()
            .name("Task")
            .description(getTaskDescription())
            .parameters(JsonObjectSchema.builder()
                .addStringProperty("description", "A short (3-5 word) description of the task")
                .addStringProperty("prompt", "The task for the agent to perform")
                .addStringProperty("subagent_type", "The type of specialized agent to use for this task")
                .required("description", "prompt", "subagent_type")
                .build())
            .build()
    }

    /**
     * 获取工具的执行函数引用
     * @return execute 方法的函数引用
     */
    override fun getExecuteFunc(): KFunction<ToolCallResult<String>> {
        return ::execute
    }

    /**
     * 执行子任务，启动指定类型的子 Agent 进行工作
     * @param description 任务简短描述
     * @param prompt 传递给子 Agent 的任务提示
     * @param subagent_type 子 Agent 类型
     * @param taskState 当前任务状态
     * @param toolRequest 工具调用请求
     * @return 工具调用结果
     */
    suspend fun execute(description: String, prompt: String, subagent_type: String, taskState: TaskState, toolRequest: ToolExecutionRequest): ToolCallResult<String> {
        val agentLoader = taskState.project.service<AgentService>().agentLoader
        val agentConfig = agentLoader.getActiveAgents().firstOrNull { it.agentType == subagent_type } ?: run {
            val availableAgents = agentLoader.getActiveAgents().joinToString("\n") { "  • ${it.agentType}" }
            throw ToolExecutionException("Agent type '$subagent_type' not found.\n\nAvailable agents:\n$availableAgents")
        }
        var systemPrompt = PromptService.formatPrompt(agentConfig.systemPrompt, taskState.modelId, taskState.project)
        if (!taskState.mcpPrompt.isNullOrBlank()) { systemPrompt += "\n\n${taskState.mcpPrompt}" }
        var tools = if (agentConfig.tools as? String == "*") taskState.tools.values.toList()
        else run {
            (agentConfig.tools as? List<*>)?.toSet()?.let { agentTools ->
                taskState.tools.values.filter { agentTools.contains(it.getName()) }
            } ?: emptyList()
        }
        tools = tools.filter { it.getName() != getName() }

        val agentId = sanitizeFileName(subagent_type)
        val subTask = Task(
            taskId = UUID.randomUUID().toString().replace("-".toRegex(), ""),
            convId = taskState.convId,
            agentId = agentId,
            userMessageId = taskState.userMessageId,
            convTitle = description,
            systemPrompt = systemPrompt,
            userInput = prompt,
            tools = tools,
            modelId = taskState.modelId,
            chatMode = taskState.chatMode,
            historyAiMessage = taskState.historyAiMessage,
            isMainAgent = false,
        )
        try {
            taskState.subTask = subTask
            // 启动子任务循环
            subTask.startTaskLoop().collect { msg ->
                taskState.emit!!(msg)
            }
            val data =if (subTask.taskState.abort) {
                "[Request interrupted by user]"
            } else {
                val lastAiMessage = subTask.taskState.aiMessageFuture.get()
                lastAiMessage.text()
            }
            val taskEndSegment = ToolSegment(
                name = UiToolName.TASK_END,
                toolCommand = description,
                toolContent = data,
                params = mapOf(
                    "subagent_type" to JsonPrimitive(subagent_type)
                )
            )
            taskState.historyAiMessage.segments.add(taskEndSegment)
            taskState.emit!!(JarvisSay(
                id = toolRequest.id(),
                type = AgentMessageType.TOOL,
                data = listOf(taskEndSegment),
                isPartial = false
            ))
            return ToolCallResult(
                data=data,
                resultForAssistant = renderResultForAssistant(data)
            )
        } finally {
            taskState.subTask = null
        }
    }

    /**
     * 将工具输出渲染为返回给 AI 的文本
     * @param output 子任务的执行结果
     * @return 原始结果文本
     */
    override fun renderResultForAssistant(output: String): String {
        return output
    }

    /**
     * 校验工具输入参数的合法性
     * @param input 输入的 JSON 参数
     * @param taskState 当前任务状态
     */
    override suspend fun validateInput(input: JsonElement, taskState: TaskState) {
        (input as JsonObject).let {
            it["description"]?.jsonPrimitive?.contentOrNull
                ?: throw MissingToolParameterException(getName(), "description")
            it["prompt"]?.jsonPrimitive?.contentOrNull
                ?: throw MissingToolParameterException(getName(), "prompt")
            it["subagent_type"]?.jsonPrimitive?.contentOrNull
                ?: throw MissingToolParameterException(getName(), "subagent_type")
        }
    }

    /**
     * 处理工具参数流式返回，构建子任务启动的 UI 展示片段
     * @param toolRequestId 工具请求 ID
     * @param partialArgs 已解析的参数字段
     * @param taskState 当前任务状态
     * @param isPartial 是否为部分参数（流式传输中）
     * @return 工具展示片段
     */
    override suspend fun handlePartialBlock(
        toolRequestId: String,
        partialArgs: Map<String, JsonField>,
        taskState: TaskState,
        isPartial: Boolean
    ): ToolSegment? {
        if (isPartial) return null

        val description = partialArgs["description"]!!.value
        val prompt = partialArgs["prompt"]!!.value
        val subagent_type = partialArgs["subagent_type"]!!.value

        return ToolSegment(
            name = UiToolName.TASK_START,
            toolCommand = description,
            toolContent = prompt,
            params = mapOf(
                "subagent_type" to JsonPrimitive(subagent_type),
                "agent_name" to JsonPrimitive(
                    if (taskState.agentId != "default") taskState.agentId else "Jarvis"
                ),
            )
        )
    }
}
