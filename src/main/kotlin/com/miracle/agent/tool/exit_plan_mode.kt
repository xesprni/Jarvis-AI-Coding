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

    val SPEC = ToolSpecification.builder() // 工具规格定义，供模型识别和调用
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

    /**
     * 获取工具规格定义
     * @return 工具规格
     */
    override fun getToolSpecification(): ToolSpecification = SPEC

    /**
     * 获取工具执行函数的引用
     * @return 执行函数
     */
    override fun getExecuteFunc(): KFunction<ToolCallResult<ExitPlanModeOutput>> = ::execute

    /**
     * 将工具输出结果渲染为给助手的文本
     * @param output 工具输出
     * @return 渲染后的文本
     */
    override fun renderResultForAssistant(output: ExitPlanModeOutput): String {
        return if (output.success) {
            "ExitPlanMode is deprecated. Emit a <proposed_plan> block in Plan mode instead."
        } else {
            "Error: ${output.errorMessage}"
        }
    }

    /**
     * 校验工具输入参数
     * @param input 工具输入参数
     * @param taskState 当前任务状态
     */
    override suspend fun validateInput(input: JsonElement, taskState: TaskState) {
        input as? JsonObject ?: throw ToolParameterException("Invalid input format")
    }

    /**
     * 处理流式参数块，构建 UI 展示片段
     * @param toolRequestId 工具请求ID
     * @param partialArgs 部分参数
     * @param taskState 当前任务状态
     * @param isPartial 是否为部分参数
     * @return UI 展示片段
     */
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

    /**
     * 执行退出计划模式操作（已废弃，现在使用 proposed_plan 块替代）
     * @param taskState 当前任务状态
     * @param planFile 计划文件路径（可选）
     * @return 工具调用结果
     */
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

/**
 * ExitPlanMode 工具的输出结果
 */
data class ExitPlanModeOutput(
    val success: Boolean, // 操作是否成功
    val planFilePath: String?, // 计划文件路径
    val planContent: String?, // 计划文件内容
    val errorMessage: String? // 错误信息
)
