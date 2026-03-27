package com.qifu.agent.tool

import com.intellij.openapi.components.service
import com.qifu.agent.TaskState
import com.qifu.agent.parser.ToolSegment
import com.qifu.agent.parser.UiToolName
import com.qifu.services.AgentService
import com.qifu.services.PromptService.getSkillDescription
import com.qifu.utils.JsonField
import com.qifu.utils.SkillConfig
import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.data.message.TextContent
import dev.langchain4j.data.message.UserMessage.userMessage
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import kotlinx.serialization.json.*
import kotlin.reflect.KFunction


object SkillTool: Tool<SkillConfig> {

    override fun getName(): String {
        // 这里不重写会报：Consider using instance of the service on-demand instead.
        return "Skill"
    }

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

    override fun getExecuteFunc(): KFunction<ToolCallResult<SkillConfig>> {
        return ::execute
    }

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

    override fun renderResultForAssistant(output: SkillConfig): String {
        return "Launching skill: ${output.name}"
    }

    override suspend fun afterAddToolResult(data: Any?, taskState: TaskState) {
        data as SkillConfig
        val userMessage = userMessage(
            TextContent("Base directory for this skill: ${data.baseDir}\n\n${data.content}")
        )
        taskState.chatMemory.add(userMessage)
    }

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