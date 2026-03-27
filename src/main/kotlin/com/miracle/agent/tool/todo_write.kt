package com.miracle.agent.tool

import com.intellij.openapi.diagnostic.Logger
import com.miracle.agent.TaskState
import com.miracle.agent.parser.ToolSegment
import com.miracle.agent.parser.UiToolName
import com.miracle.services.ReminderEvent
import com.miracle.utils.JsonField
import com.miracle.utils.TodoItem
import com.miracle.utils.TodoStorage
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.model.chat.request.json.JsonArraySchema
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import dev.langchain4j.model.chat.request.json.JsonRawSchema
import kotlinx.serialization.json.*
import kotlin.math.max
import kotlin.reflect.KFunction


data class TodoWriteToolOutput(
    val summary: String,
    val todos: List<TodoItem>,
    val stats: TodoStats
)

data class TodoStats(
    val total: Int,
    val pending: Int,
    val inProgress: Int,
    val completed: Int
)

private val json = Json {
    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true
}

/**
 * Todo写入工具
 */
object TodoWriteTool : Tool<TodoWriteToolOutput> {

    private val LOG = Logger.getInstance(TodoWriteTool::class.java)
    val SPEC = ToolSpecification.builder()
        .name("TodoWrite")
        .description("""Use this tool to create and manage a structured task list for your current coding session. This helps you track progress, organize complex tasks, and demonstrate thoroughness to the user.
It also helps the user understand the progress of the task and overall progress of their requests.

## When to Use This Tool
Use this tool proactively in these scenarios:

1. Complex multi-step tasks - When a task requires 3 or more distinct steps or actions
2. Non-trivial and complex tasks - Tasks that require careful planning or multiple operations
3. User explicitly requests todo list - When the user directly asks you to use the todo list
4. User provides multiple tasks - When users provide a list of things to be done (numbered or comma-separated)
5. After receiving new instructions - Immediately capture user requirements as todos
6. When you start working on a task - Mark it as in_progress BEFORE beginning work. Ideally you should only have one todo as in_progress at a time
7. After completing a task - Mark it as completed and add any new follow-up tasks discovered during implementation

## When NOT to Use This Tool

Skip using this tool when:
1. There is only a single, straightforward task
2. The task is trivial and tracking it provides no organizational benefit
3. The task can be completed in less than 3 trivial steps
4. The task is purely conversational or informational

NOTE that you should not use this tool if there is only one trivial task to do. In this case you are better off just doing the task directly.

## Examples of When to Use the Todo List

<example>
User: I want to add a dark mode toggle to the application settings. Make sure you run the tests and build when you're done!
Assistant: I'll help add a dark mode toggle to your application settings. Let me create a todo list to track this implementation.
*Creates todo list with the following items:*
1. Creating dark mode toggle component in Settings page
2. Adding dark mode state management (context/store)
3. Implementing CSS-in-JS styles for dark theme
4. Updating existing components to support theme switching
5. Running tests and build process, addressing any failures or errors that occur
*Begins working on the first task*

<reasoning>
The assistant used the todo list because:
1. Adding dark mode is a multi-step feature requiring UI, state management, and styling changes
2. The user explicitly requested tests and build be run afterward
3. The assistant inferred that tests and build need to pass by adding "Ensure tests and build succeed" as the final task
</reasoning>
</example>

<example>
User: Help me rename the function getCwd to getCurrentWorkingDirectory across my project
Assistant: Let me first search through your codebase to find all occurrences of 'getCwd'.
*Uses grep or search tools to locate all instances of getCwd in the codebase*
Assistant: I've found 15 instances of 'getCwd' across 8 different files. Let me create a todo list to track these changes.
*Creates todo list with specific items for each file that needs updating*

<reasoning>
The assistant used the todo list because:
1. First, the assistant searched to understand the scope of the task
2. Upon finding multiple occurrences across different files, it determined this was a complex task with multiple steps
3. The todo list helps ensure every instance is tracked and updated systematically
4. This approach prevents missing any occurrences and maintains code consistency
</reasoning>
</example>


<example>
User: I need to implement these features for my e-commerce site: user registration, product catalog, shopping cart, and checkout flow.
Assistant: I'll help implement these features. First, let's add all the features to the todo list.
*Creates a todo list breaking down each feature into specific tasks based on the project architecture*
Assistant: Let's start with user registration. This will involve creating database models, API endpoints, and frontend forms that integrate with your existing authentication system.

<reasoning>
The assistant used the todo list because:
1. The user provided multiple complex features to implement in a comma separated list
2. The todo list helps organize these large features into manageable tasks
3. This approach allows for tracking progress across the entire implementation
</reasoning>
</example>

<example>
User: Can you help optimize my React application? It's rendering slowly and has performance issues.</user>
Assistant: I'll help optimize your React application. First, let me examine your codebase to identify potential performance bottlenecks.
*Reviews component structure, render patterns, state management, and data fetching*
Assistant: After analyzing your codebase, I've identified several performance issues. Let me create a todo list to track our optimization efforts.
*Creates todo list with items like: 1) Implementing memoization for expensive calculations in ProductList, 2) Adding virtualization for long lists in Dashboard, 3) Optimizing image loading in Gallery component, 4) Fixing state update loops in ShoppingCart, 5) Reviewing bundle size and implementing code splitting*
Let's start by implementing memoization for the expensive calculations in your ProductList component.</assistant>

<reasoning>
The assistant used the todo list because:
1. First, the assistant examined the codebase to identify specific performance issues
2. Based on this analysis, it identified multiple optimization opportunities
3. Performance optimization is a non-trivial task requiring multiple steps
4. The todo list helps methodically track improvements across different components
5. This systematic approach ensures all performance bottlenecks are addressed
</reasoning>
</example>

## Examples of When NOT to Use the Todo List

<example>
User: How do I print 'Hello World' in Python?
Assistant: In Python, you can print "Hello World" with this simple code:

python
print("Hello World")

This will output the text "Hello World" to the console when executed.</assistant>

<reasoning>
The assistant did not use the todo list because this is a single, trivial task that can be completed in one step. There's no need to track multiple tasks or steps for such a straightforward request.
</reasoning>
</example>

<example>
User: What does the git status command do?
Assistant: The git status command shows the current state of your working directory and staging area. It displays which changes have been staged, which haven't, and which files aren't being tracked by Git. This is a fundamental command for understanding the current state of your repository before making commits.

<reasoning>
The assistant did not use the todo list because this is an informational request with no actual coding task to complete. The user is simply asking for an explanation, not for the assistant to perform multiple steps or tasks.
</reasoning>
</example>

<example>
User: Can you add a comment to the calculateTotal function to explain what it does?
Assistant: Sure, let me add a comment to the calculateTotal function to explain what it does.
* Uses the Edit tool to add a comment to the calculateTotal function *

<reasoning>
The assistant did not use the todo list because this is a single, straightforward task confined to one location in the code. Adding a comment doesn't require tracking multiple steps or systematic organization.
</reasoning>
</example>

<example>
User: Run npm install for me and tell me what happens.
Assistant: I'll run the npm install command for you.

*Executes: npm install*

The command completed successfully. Here's the output:
[Output of npm install command]

All dependencies have been installed according to your package.json file.

<reasoning>
The assistant did not use the todo list because this is a single command execution with immediate results. There are no multiple steps to track or organize, making the todo list unnecessary for this straightforward task.
</reasoning>
</example>

## Task States and Management

1. **Task States**: Use these states to track progress:
   - pending: Task not yet started
   - in_progress: Currently working on (limit to ONE task at a time)
   - completed: Task finished successfully

   **IMPORTANT**: Task descriptions must have two forms:
   - content: The imperative form describing what needs to be done (e.g., "Run tests", "Build the project")
   - activeForm: The present continuous form shown during execution (e.g., "Running tests", "Building the project")

2. **Task Management**:
   - Update task status in real-time as you work
   - Mark tasks complete IMMEDIATELY after finishing (don't batch completions)
   - Exactly ONE task must be in_progress at any time (not less, not more)
   - Complete current tasks before starting new ones
   - Remove tasks that are no longer relevant from the list entirely

3. **Task Completion Requirements**:
   - ONLY mark a task as completed when you have FULLY accomplished it
   - If you encounter errors, blockers, or cannot finish, keep the task as in_progress
   - When blocked, create a new task describing what needs to be resolved
   - Never mark a task as completed if:
     - Tests are failing
     - Implementation is partial
     - You encountered unresolved errors
     - You couldn't find necessary files or dependencies

4. **Task Breakdown**:
   - Create specific, actionable items
   - Break complex tasks into smaller, manageable steps
   - Use clear, descriptive task names
   - Always provide both forms:
     - content: "Fix authentication bug"
     - activeForm: "Fixing authentication bug"

When in doubt, use this tool. Being proactive with task management demonstrates attentiveness and ensures you complete all requirements successfully.""")
        .parameters(
            JsonObjectSchema.builder()
                .addProperty("todos",
                    JsonArraySchema.builder()
                        .items(
                            JsonObjectSchema.builder()
                                .addProperty("content", JsonRawSchema.from(
                                    """
                                    {
                                      "type": "string",
                                      "minLength": 1
                                    }
                                    """.trimIndent()))
                                .addEnumProperty("status", listOf("pending", "in_progress", "completed"))
                                .addProperty("activeForm",JsonRawSchema.from(
                                    """
                                    {
                                      "type": "string",
                                      "minLength": 1
                                    }
                                    """.trimIndent()))
                                .required("content", "status", "activeForm")
                                .build()
                        )
                        .description("The updated todo list")
                        .build()
                )
                .required("todos")
                .build()
        )
        .build()

    override fun getToolSpecification(): ToolSpecification {
        return SPEC
    }

    override fun getExecuteFunc(): KFunction<ToolCallResult<TodoWriteToolOutput>> {
        return ::execute
    }

    override fun renderResultForAssistant(output: TodoWriteToolOutput): String {
        return output.summary
    }

    /**
     * 工具的参数会流式输出，每次流式输出时会触发此方法
     * handlePartialBlock 方法负责UI更新，不负责推送工具结果
     */
    override suspend fun handlePartialBlock(toolRequestId: String, partialArgs: Map<String, JsonField>, taskState: TaskState, isPartial: Boolean): ToolSegment? {
        if (isPartial) return null

        // 构建 todo 列表的预览信息
        val todos = json.decodeFromString<List<TodoItem>>(partialArgs["todos"]!!.value)
        var remainCount = 0
        val todosSummary = buildString {
            todos.forEach { todo ->
                when (todo.status) {
                    TodoItem.Status.IN_PROGRESS -> append("[*] ${todo.activeForm.takeIf { it.isNotBlank() } ?: todo.content}").append("\n")
                    TodoItem.Status.COMPLETED -> append("[✔] ${todo.content}").append("\n")
                    else -> {
                        remainCount++
                        append("[○] ${todo.content}").append("\n")
                    }
                }
            }
        }
        val curIndex = todos.size - remainCount
        val currentTodo = todos.getOrNull(max(curIndex - 1, 0))
        val todoTitle = currentTodo?.let{
            if (it.status == TodoItem.Status.IN_PROGRESS) it.activeForm else it.content
        } ?: "待办事项"
        return ToolSegment(
            name = UiToolName.TODO_UPDATE,
            toolContent = todosSummary.trim(),
            toolCommand = todoTitle,
            params = mapOf(
                "todos" to json.encodeToJsonElement( todos),
                "curIndex" to JsonPrimitive(curIndex),
                "agent_name" to JsonPrimitive(
                    if (taskState.agentId != "default") taskState.agentId else "Jarvis"
                ),
            )
        )
    }

    fun execute(todos: List<TodoItem>, taskState: TaskState): ToolCallResult<TodoWriteToolOutput> {
        try {
            val previousTodos = TodoStorage.getTodos(taskState.taskId, taskState.agentId, taskState.project)
            TodoStorage.setTodos(taskState.convId, taskState.agentId,todos, taskState.project)

            val stats = generateStats(todos)
            val hasChanged = todosChanged(previousTodos, todos)
            if (hasChanged) {
                taskState.systemReminderService?.emitEvent(
                    ReminderEvent.TODO_CHANGED.value,
                    ReminderEvent.TodoChangedContext(
                        agentId = taskState.agentId
                    )
                )
            }
            // Generate summary
            val summary = generateSummary(stats)
            val output = TodoWriteToolOutput(
                summary = summary,
                todos = todos,
                stats = stats
            )
            return ToolCallResult(
                type = "result",
                data = output,
                resultForAssistant = renderResultForAssistant(output)
            )
        } catch (e: Exception) {
            val errorMessage = "Error updating todos: ${e.message}"
            val errorOutput = TodoWriteToolOutput(
                summary = errorMessage,
                todos = emptyList(),
                stats = TodoStats(0, 0, 0, 0)
            )
            return ToolCallResult(
                type = "result",
                data = errorOutput,
                resultForAssistant = errorMessage
            )
        }
    }

    override suspend fun validateInput(input: JsonElement, taskState: TaskState) {
        val jsonObject = input as JsonObject
        val todosElement = jsonObject["todos"]
            ?: throw MissingToolParameterException("todo_write", "todos")
        val todosArray = when (todosElement) {
            is JsonArray -> todosElement
            is JsonPrimitive -> {
                // 如果是字符串，则解析为 JsonArray
                try {
                    json.parseToJsonElement(todosElement.content) as? JsonArray
                        ?: throw ToolParameterException("Failed to parse todos string as JSON array")
                } catch (e: Exception) {
                    throw ToolParameterException("Invalid todos format: ${e.message}")
                }
            }
            else -> throw ToolParameterException("Invalid todos format")
        }
        if (todosArray.isEmpty()) {
            throw ToolParameterException("Todos array cannot be empty")
        }

        val todos = mutableListOf<TodoItem>()
        todosArray.forEach { element ->
            val todoObj = element.jsonObject

            val content = todoObj["content"]?.jsonPrimitive?.contentOrNull
                ?: throw MissingToolParameterException("todo_write", "content")
            val status = todoObj["status"]?.jsonPrimitive?.contentOrNull
                ?: throw MissingToolParameterException("todo_write", "status")
            val activeForm = todoObj["activeForm"]?.jsonPrimitive?.contentOrNull
                ?: throw MissingToolParameterException("todo_write", "activeForm")
            if (status !in listOf("pending", "in_progress", "completed")) {
                throw ToolParameterException("Invalid status '$status'. Must be one of: pending, in_progress, completed")
            }
            if (content.trim().isEmpty()) {
                throw ToolParameterException("Todo content cannot be empty")
            }
            if (activeForm.trim().isEmpty()) {
                throw ToolParameterException("Todo activeForm cannot be empty")
            }
            todos.add(TodoItem(content, TodoItem.Status.valueOf(status.uppercase()), activeForm))
        }

        // Check for duplicate IDs
        val contents = todos.map { it.content }
        val uniqueContents = contents.toSet()
        if (contents.size != uniqueContents.size) {
            val duplicates = uniqueContents.groupBy { it }.filter { it.value.size > 1 }.keys
            throw ToolParameterException("Duplicate todo content found: ${duplicates.joinToString(", ")}")
        }
    }

    private fun generateStats(todos: List<TodoItem>): TodoStats {
        return TodoStats(
            total = todos.size,
            pending = todos.count { it.status == TodoItem.Status.PENDING },
            inProgress = todos.count { it.status == TodoItem.Status.IN_PROGRESS },
            completed = todos.count { it.status == TodoItem.Status.COMPLETED }
        )
    }

    private fun generateSummary(stats: TodoStats): String {
        var summary = "Updated ${stats.total} todo(s)"
        if (stats.total > 0) {
            summary += " (${stats.pending} pending, ${stats.inProgress} in progress, ${stats.completed} completed)"
        }
        summary += ". Continue tracking your progress with the todo list."
        return summary
    }

    /**
     * 检查 todos 是否发生变化
     * 比较两个 todo 列表的内容和状态
     */
    private fun todosChanged(previousTodos: List<TodoItem>, newTodos: List<TodoItem>): Boolean {
        if (previousTodos.size != newTodos.size) return true
        
        // 创建内容到状态的映射进行比较
        val previousMap = previousTodos.associate { 
            it.content.trim().lowercase() to it.status 
        }
        val newMap = newTodos.associate { 
            it.content.trim().lowercase() to it.status 
        }
        return previousMap != newMap
    }
}
