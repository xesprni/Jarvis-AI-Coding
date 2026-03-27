package com.miracle.agent.tool

import com.miracle.agent.TaskState
import com.miracle.agent.parser.ToolSegment
import com.miracle.agent.parser.UiToolName
import com.miracle.utils.JsonField
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.reflect.KFunction

/**
 * ExitPlanMode 工具
 * 
 * 当AI完成计划编写后，调用此工具退出计划模式并提交计划给用户审批。
 * 退出后恢复到原始的聊天模式（通常是Agent模式）。
 */
object ExitPlanModeTool : Tool<ExitPlanModeOutput> {

    val SPEC = ToolSpecification.builder()
        .name("ExitPlanMode")
        .description("""Use this tool when you are in plan mode and have finished writing your plan to the plan file and are ready for user approval.

## How This Tool Works
- You should have already written your plan to the plan file in the plan directory
- This tool does NOT take the plan content as a parameter - it will read the plan from the file you wrote
- This tool simply signals that you're done planning and ready for the user to review and approve
- The user will see the contents of your plan file when they review it

## When to Use This Tool
IMPORTANT: Only use this tool when the task requires planning the implementation steps of a task that requires writing code. For research tasks where you're gathering information, searching files, reading files or in general trying to understand the codebase - do NOT use this tool.

## Handling Ambiguity in Plans
Before using this tool, ensure your plan is clear and unambiguous. If there are multiple valid approaches or unclear requirements:
1. Use the AskUserQuestion tool to clarify with the user
2. Ask about specific implementation choices (e.g., architectural patterns, which library to use)
3. Clarify any assumptions that could affect the implementation
4. Edit your plan file to incorporate user feedback
5. Only proceed with ExitPlanMode after resolving ambiguities and updating the plan file

## Examples

1. Initial task: "Search for and understand the implementation of vim mode in the codebase" - Do not use the exit plan mode tool because you are not planning the implementation steps of a task.
2. Initial task: "Help me implement yank mode for vim" - Use the exit plan mode tool after you have finished planning the implementation steps of the task.
3. Initial task: "Add a new feature to handle user authentication" - If unsure about auth method (OAuth, JWT, etc.), use AskUserQuestion first, then use exit plan mode tool after clarifying the approach.
""")
        .parameters(
            JsonObjectSchema.builder()
                .addStringProperty("plan_file", "Optional: The specific plan file path. If not provided, will look for plan files in the plan directory.")
                .build()
        )
        .build()

    override fun getToolSpecification(): ToolSpecification = SPEC

    override fun getExecuteFunc(): KFunction<ToolCallResult<ExitPlanModeOutput>> = ::execute

    override fun renderResultForAssistant(output: ExitPlanModeOutput): String {
        return if (output.success) {
            "ExitPlanMode is deprecated. Emit a <proposed_plan> block in Plan mode instead."
        } else {
            "Error: ${output.errorMessage}"
        }
    }

    override suspend fun validateInput(input: JsonElement, taskState: TaskState) {
        input as? JsonObject ?: throw ToolParameterException("Invalid input format")
    }

    override suspend fun handlePartialBlock(
        toolRequestId: String,
        partialArgs: Map<String, JsonField>,
        taskState: TaskState,
        isPartial: Boolean
    ): ToolSegment? {
        if (isPartial) return null
        
        val planFile = partialArgs["plan_file"]?.value
        return ToolSegment(
            name = UiToolName.EXIT_PLAN_MODE,
            toolCommand = planFile ?: "Exit Plan Mode",
            toolContent = "ExitPlanMode is deprecated. Use a <proposed_plan> block in Plan mode instead.",
            params = mutableMapOf<String, JsonElement>().apply {
                planFile?.let { put("plan_file", JsonPrimitive(it)) }
                put("agent_name", JsonPrimitive(
                    if (taskState.agentId != "default") taskState.agentId else "Jarvis"
                ))
            }
        )
    }

    fun execute(taskState: TaskState, planFile: String? = null): ToolCallResult<ExitPlanModeOutput> {
        val output = ExitPlanModeOutput(
            success = false,
            planFilePath = planFile,
            planContent = null,
            errorMessage = "ExitPlanMode is deprecated. Use a <proposed_plan> block in Plan mode instead."
        )

        return ToolCallResult(
            type = "error",
            data = output,
            resultForAssistant = renderResultForAssistant(output)
        )
    }
}

data class ExitPlanModeOutput(
    val success: Boolean,
    val planFilePath: String?,
    val planContent: String?,
    val errorMessage: String?
)
