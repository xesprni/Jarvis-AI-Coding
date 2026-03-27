package com.qifu.agent.tool

import com.qifu.agent.TaskState
import com.qifu.agent.parser.ToolSegment
import com.qifu.agent.parser.UiToolName
import com.qifu.ui.smartconversation.settings.configuration.ChatMode
import com.qifu.utils.JsonField
import com.qifu.utils.getPlanDirectory
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
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
            """User has approved your plan. You can now start coding. Start with updating your todo list if applicable.

Your plan has been saved to: ${output.planFilePath}
You can refer back to it if needed during implementation.

## Approved Plan:
${output.planContent}
"""
        } else {
            "Error: ${output.errorMessage}"
        }
    }

    override suspend fun validateInput(input: JsonElement, taskState: TaskState) {
        // plan_file is optional, no required validation needed
    }

    override suspend fun handlePartialBlock(
        toolRequestId: String,
        partialArgs: Map<String, JsonField>,
        taskState: TaskState,
        isPartial: Boolean
    ): ToolSegment? {
        if (isPartial) return null
        
        val planFile = partialArgs["plan_file"]?.value
        val content = planFile
            ?.takeIf { it.isNotBlank() }
            ?.let { File(it).readText() }
        return ToolSegment(
            name = UiToolName.EXIT_PLAN_MODE,
            toolCommand = planFile ?: "Exit Plan Mode",
            toolContent = content?:"Planning phase completed. Ready to proceed with implementation.",
            params = mutableMapOf<String, JsonElement>().apply {
                planFile?.let { put("plan_file", JsonPrimitive(it)) }
                put("agent_name", JsonPrimitive(
                    if (taskState.agentId != "default") taskState.agentId else "Jarvis"
                ))
            }
        )
    }

    fun execute(taskState: TaskState, planFile: String? = null): ToolCallResult<ExitPlanModeOutput> {
        val planDir = getPlanDirectory(taskState.project, taskState.convId)
        
        // 查找计划文件
        val planFilePath = if (!planFile.isNullOrBlank()) {
            planFile
        } else {
            findPlanFile(planDir)
        }
        
        if (planFilePath == null) {
            val output = ExitPlanModeOutput(
                success = false,
                planFilePath = null,
                planContent = null,
                errorMessage = "No plan file found at $planDir. Please write your plan to this directory before calling ExitPlanMode."
            )
            return ToolCallResult(
                type = "error",
                data = output,
                resultForAssistant = renderResultForAssistant(output)
            )
        }
        
        val planFile = File(planFilePath)
        if (!planFile.exists()) {
            val output = ExitPlanModeOutput(
                success = false,
                planFilePath = planFilePath,
                planContent = null,
                errorMessage = "Plan file not found at $planFilePath. Please write your plan to this file before calling ExitPlanMode."
            )
            return ToolCallResult(
                type = "error",
                data = output,
                resultForAssistant = renderResultForAssistant(output)
            )
        }
        
        val planContent = planFile.readText()
        
        // 恢复原始模式
        if (taskState.isEmbeddedPlanMode) {
            taskState.chatMode = taskState.originalChatMode ?: ChatMode.AGENT
            taskState.isEmbeddedPlanMode = false
            taskState.originalChatMode = null
            // 同步状态到SystemReminderService
            taskState.syncPlanModeState()
            // 刷新工具规范，恢复到原始模式的工具集合
            taskState.refreshToolSpecs?.invoke()
        }
        
        val output = ExitPlanModeOutput(
            success = true,
            planFilePath = planFilePath,
            planContent = planContent,
            errorMessage = null
        )
        
        return ToolCallResult(
            type = "result",
            data = output,
            resultForAssistant = renderResultForAssistant(output)
        )
    }
    
    /**
     * 在计划目录中查找计划文件
     * 优先查找 .md 文件，按修改时间倒序排列
     */
    private fun findPlanFile(planDir: String): String? {
        val dir = File(planDir)
        if (!dir.exists() || !dir.isDirectory) return null
        
        val planFiles = dir.listFiles { file ->
            file.isFile && (file.extension == "md" || file.extension == "txt")
        }?.sortedByDescending { it.lastModified() }
        
        return planFiles?.firstOrNull()?.absolutePath
    }
}

data class ExitPlanModeOutput(
    val success: Boolean,
    val planFilePath: String?,
    val planContent: String?,
    val errorMessage: String?
)
