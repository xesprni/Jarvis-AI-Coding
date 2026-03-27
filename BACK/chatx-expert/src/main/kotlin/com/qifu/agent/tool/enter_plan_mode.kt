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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.reflect.KFunction

/**
 * EnterPlanMode 工具
 * 
 * 当AI认为当前任务较为复杂，需要先进行规划时，主动调用此工具询问用户是否进入计划模式。
 * 进入计划模式后，除了写入计划文件外，其他操作均为只读。
 */
object EnterPlanModeTool : Tool<EnterPlanModeOutput> {

    val SPEC = ToolSpecification.builder()
        .name("EnterPlanMode")
        .description("""Use this tool proactively when you're about to start a non-trivial implementation task. Getting user sign-off on your approach before writing code prevents wasted effort and ensures alignment. This tool transitions you into plan mode where you can explore the codebase and design an implementation approach for user approval.

## When to Use This Tool

**Prefer using EnterPlanMode** for implementation tasks unless they're simple. Use it when ANY of these conditions apply:

1. **New Feature Implementation**: Adding meaningful new functionality
   - Example: "Add a logout button" - where should it go? What should happen on click?
   - Example: "Add form validation" - what rules? What error messages?

2. **Multiple Valid Approaches**: The task can be solved in several different ways
   - Example: "Add caching to the API" - could use Redis, in-memory, file-based, etc.
   - Example: "Improve performance" - many optimization strategies possible

3. **Code Modifications**: Changes that affect existing behavior or structure
   - Example: "Update the login flow" - what exactly should change?
   - Example: "Refactor this component" - what's the target architecture?

4. **Architectural Decisions**: The task requires choosing between patterns or technologies
   - Example: "Add real-time updates" - WebSockets vs SSE vs polling
   - Example: "Implement state management" - Redux vs Context vs custom solution

5. **Multi-File Changes**: The task will likely touch more than 2-3 files
   - Example: "Refactor the authentication system"
   - Example: "Add a new API endpoint with tests"

6. **Unclear Requirements**: You need to explore before understanding the full scope
   - Example: "Make the app faster" - need to profile and identify bottlenecks
   - Example: "Fix the bug in checkout" - need to investigate root cause

7. **User Preferences Matter**: The implementation could reasonably go multiple ways
   - If you would use AskUserQuestion to clarify the approach, use EnterPlanMode instead
   - Plan mode lets you explore first, then present options with context

## When NOT to Use This Tool

Only skip EnterPlanMode for simple tasks:
- Single-line or few-line fixes (typos, obvious bugs, small tweaks)
- Adding a single function with clear requirements
- Tasks where the user has given very specific, detailed instructions
- Pure research/exploration tasks (use the Task tool with explore agent instead)

## What Happens in Plan Mode

In plan mode, you'll:
1. Thoroughly explore the codebase using Glob, Grep, and Read tools
2. Understand existing patterns and architecture
3. Design an implementation approach
4. Present your plan to the user for approval
5. Use AskUserQuestion if you need to clarify approaches
6. Exit plan mode with ExitPlanMode when ready to implement

## Important Notes

- This tool REQUIRES user approval - they must consent to entering plan mode
- If the user declines, continue with the task without planning
- If unsure whether to use it, err on the side of planning - it's better to get alignment upfront than to redo work
- Users appreciate being consulted before significant changes are made to their codebase
""")
        .parameters(
            JsonObjectSchema.builder()
                .addStringProperty("reason", "A brief explanation of why you think this task needs planning. This helps the user understand your reasoning.")
                .required("reason")
                .build()
        )
        .build()

    override fun getToolSpecification(): ToolSpecification = SPEC

    override fun getExecuteFunc(): KFunction<ToolCallResult<EnterPlanModeOutput>> = ::execute

    override fun renderResultForAssistant(output: EnterPlanModeOutput): String {
        return if (output.approved) {
            val planDir = output.planDirectory
            """Entered plan mode. You should now focus on exploring the codebase and designing an implementation approach.

In plan mode, you should:
1. Thoroughly explore the codebase to understand existing patterns
2. Identify similar features and architectural approaches
3. Consider multiple approaches and their trade-offs
4. Use AskUserQuestion if you need to clarify the approach
5. Design a concrete implementation strategy
6. When ready, use ExitPlanMode to present your plan for approval

Remember: DO NOT write or edit any files yet (except the plan file in $planDir). This is a read-only exploration and planning phase.
"""
        } else {
            "User declined to enter plan mode. Continue with the task execution directly without the planning phase."
        }
    }

    override suspend fun validateInput(input: JsonElement, taskState: TaskState) {
        val jsonObject = input as? JsonObject
            ?: throw ToolParameterException("Invalid input format")
        
        jsonObject["reason"]?.let { 
            it as? JsonPrimitive
        }?.contentOrNull ?: throw MissingToolParameterException(getName(), "reason")
    }

    override suspend fun handlePartialBlock(
        toolRequestId: String,
        partialArgs: Map<String, JsonField>,
        taskState: TaskState,
        isPartial: Boolean
    ): ToolSegment? {
        if (isPartial) return null
        
        val reason = partialArgs["reason"]?.value ?: "This task appears complex and may benefit from planning."
        
        return ToolSegment(
            name = UiToolName.ENTER_PLAN_MODE,
            toolCommand = reason,
            toolContent = "Do you want me to create a plan before implementing this task?",
            params = mutableMapOf(
                "reason" to kotlinx.serialization.json.JsonPrimitive(reason),
                "agent_name" to kotlinx.serialization.json.JsonPrimitive(
                    if (taskState.agentId != "default") taskState.agentId else "Jarvis"
                ),
            )
        )
    }

    fun execute(taskState: TaskState, reason: String): ToolCallResult<EnterPlanModeOutput> {

        val planDir = getPlanDirectory(taskState.project, taskState.convId)

        // 保存原始模式并切换到计划模式
        taskState.originalChatMode = taskState.chatMode
        taskState.chatMode = ChatMode.PLAN
        taskState.isEmbeddedPlanMode = true
        // 同步状态到SystemReminderService
        taskState.syncPlanModeState()
        // 刷新工具规范，只暴露 Plan 模式下允许的工具
        taskState.refreshToolSpecs?.invoke()

        val output = EnterPlanModeOutput(
            approved = true,
            reason = reason,
            planDirectory = planDir
        )
        
        return ToolCallResult(
            type = "result",
            data = output,
            resultForAssistant = renderResultForAssistant(output)
        )
    }
}

data class EnterPlanModeOutput(
    val approved: Boolean,
    val reason: String,
    val planDirectory: String
)
