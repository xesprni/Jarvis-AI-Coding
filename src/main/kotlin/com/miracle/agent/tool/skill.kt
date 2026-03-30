package com.miracle.agent.tool

import com.intellij.openapi.components.service
import com.miracle.agent.TaskState
import com.miracle.agent.parser.ToolSegment
import com.miracle.agent.parser.UiToolName
import com.miracle.services.AgentService
import com.miracle.services.PromptService.getSkillDescription
import com.miracle.utils.JsonField
import com.miracle.utils.SkillConfig
import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.data.message.TextContent
import dev.langchain4j.data.message.UserMessage.userMessage
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import kotlinx.serialization.json.*
import kotlin.reflect.KFunction


/**
 * 技能调用工具，用于加载和执行预定义的技能配置
 */
object SkillTool: Tool<SkillConfig> {

    /**
     * 获取工具名称
     * @return 工具名称 "Skill"
     */
    override fun getName(): String {
        // 这里不重写会报：Consider using instance of the service on-demand instead.
        return "Skill"
    }

    /**
     * 获取工具规格定义，动态获取技能描述以支持项目级别的技能配置
     * @return Skill 工具的规格定义
     */
    override fun getToolSpecification(): ToolSpecification {
        // 因为有项目级别的 subAgent，这里的描述需要每次动态获取
        return ToolSpecification.builder()
            .name("Skill")
            .description(getSkillDescription())
            .parameters(JsonObjectSchema.builder()
                .addStringProperty("skill", "The skill name. E.g., \"commit\", \"review-pr\", or \"pdf\"")
                .addStringProperty("args", "Optional arguments for the skill")
                .required("skill")
                .build())
            .build()
    }

    /**
     * 获取工具的执行函数引用
     * @return execute 方法的函数引用
     */
    override fun getExecuteFunc(): KFunction<ToolCallResult<SkillConfig>> {
        return ::execute
    }

    /**
     * 执行技能加载操作
     * @param skill 技能名称
     * @param taskState 当前任务状态
     * @param toolRequest 工具调用请求
     * @return 工具调用结果
     */
    suspend fun execute(skill: String, taskState: TaskState, toolRequest: ToolExecutionRequest): ToolCallResult<SkillConfig> {
        val skillLoader = taskState.project.service<AgentService>().skillLoader
        val skills = skillLoader.getActiveSkills()
        val skillConfig = skills.first { s -> s.name == skill }
        skillConfig.content = skillConfig.content.replace("{{PROJECT_BASE_DIR}}", taskState.project.basePath!!)

        return ToolCallResult(
            data=skillConfig,
            resultForAssistant = renderResultForAssistant(skillConfig)
        )

    }

    /**
     * 将工具输出渲染为返回给 AI 的文本
     * @param output 技能配置
     * @return 格式化后的结果文本
     */
    override fun renderResultForAssistant(output: SkillConfig): String {
        return "Launching skill: ${output.name}"
    }

    /**
     * 技能加载完成后的后置处理，将技能内容作为用户消息添加到对话记忆中
     * @param data 技能配置数据
     * @param taskState 当前任务状态
     */
    override suspend fun afterAddToolResult(data: Any?, taskState: TaskState) {
        data as SkillConfig
        val userMessage = userMessage(
            TextContent("Base directory for this skill: ${data.baseDir}\n\n${data.content}")
        )
        taskState.chatMemory.add(userMessage)
    }

    /**
     * 校验工具输入参数的合法性
     * @param input 输入的 JSON 参数
     * @param taskState 当前任务状态
     */
    override suspend fun validateInput(input: JsonElement, taskState: TaskState) {
        (input as JsonObject).let {
            val skillName = it["skill"]?.jsonPrimitive?.contentOrNull
                ?: throw MissingToolParameterException(getName(), "skill")
            val skillLoader = taskState.project.service<AgentService>().skillLoader
            val skills = skillLoader.getActiveSkills()
            skills.firstOrNull { s -> s.name == skillName } ?: run {
                val availableSkillNames = skills.joinToString(", ") { s -> s.name }
                val errorMsg = """The skill "$skillName" does not exist. Please choose one of the available tools: $availableSkillNames."""
                throw ToolExecutionException(availableSkillNames)
            }
        }
    }

    /**
     * 处理工具参数流式返回，构建技能使用的 UI 展示片段
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

        val skillName = partialArgs["skill"]?.value ?: return null
        val skillLoader = taskState.project.service<AgentService>().skillLoader
        val skills = skillLoader.getActiveSkills()
        val skill = skills.firstOrNull { s -> s.name == skillName } ?: return null

        return ToolSegment(
            name = UiToolName.USE_SKILL,
            toolCommand = "Use Skill $skillName",
            toolContent = skill.description,
            params = mapOf(
                "skill_name" to JsonPrimitive(skillName),
                "agent_name" to JsonPrimitive(
                    if (taskState.agentId != "default") taskState.agentId else "Jarvis"
                ),
            )
        )
    }
}