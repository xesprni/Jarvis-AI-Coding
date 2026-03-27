//package com.miracle.agent.tool
//
//import com.miracle.agent.TaskState
//import com.miracle.agent.chatMemoryProvider
//import com.miracle.utils.*
//import kotlinx.serialization.json.*
//import org.junit.FixMethodOrder
//import org.junit.runners.MethodSorters
//import kotlin.test.*
//
//@FixMethodOrder(MethodSorters.NAME_ASCENDING)
//class ToDoWriteToolTest {
//
//    val taskId = "ToDoWriteToolTest"
//    @BeforeTest
//    fun setUp() {
//        // Clear todos before each test
//        clearTodos(taskId)
//    }
//
//    @AfterTest
//    fun tearDown() {
//        // Clear todos after each test
//        clearTodos(taskId)
//    }
//
//    @Test
//    fun test01_BasicTodoCreation() {
//        // Test basic todo creation
//        val input = listOf(
//            TodoWriteInput("1", "Implement feature A", "pending", "high"),
//            TodoWriteInput("2", "Write tests", "pending", "medium"),
//            TodoWriteInput("3", "Update documentation", "pending", "low")
//        )
//        val taskState = TaskState(taskId, chatMemoryProvider.get(taskId),mutableMapOf())
//        val result = TodoWriteTool.execute(input,null,taskState)
//
//        assertNotNull(result, "Result should not be null")
//        assertEquals("result", result.type, "Result type should be 'result'")
//
//        val output = result.data
//        assertEquals(3, output.todos.size, "Should have 3 todos")
//        assertEquals(3, output.stats.total, "Total should be 3")
//        assertEquals(3, output.stats.pending, "Pending should be 3")
//        assertEquals(0, output.stats.inProgress, "InProgress should be 0")
//        assertEquals(0, output.stats.completed, "Completed should be 0")
//
//        // Verify todos are stored
//        val storedTodos = getTodos(taskId)
//        assertEquals(3, storedTodos.size, "Should have 3 todos in storage")
//    }
//
//    @Test
//    fun test02_TodoWithInProgressStatus() {
//        // Test todo with in_progress status
//        val input = listOf(
//            TodoWriteInput("1", "Implement feature A", "in_progress", "high"),
//            TodoWriteInput("2", "Write tests", "pending", "medium")
//        )
//
//        val taskState = TaskState(taskId, chatMemoryProvider.get(taskId),mutableMapOf())
//        val result = TodoWriteTool.execute(input,null,taskState)
//
//        val output = result.data
//        assertEquals(2, output.stats.total, "Total should be 2")
//        assertEquals(1, output.stats.pending, "Pending should be 1")
//        assertEquals(1, output.stats.inProgress, "InProgress should be 1")
//        assertEquals(0, output.stats.completed, "Completed should be 0")
//    }
//
//    @Test
//    fun test03_TodoWithCompletedStatus() {
//        // Test todo with completed status
//        val input = listOf(
//            TodoWriteInput("1", "Implement feature A", "completed", "high"),
//            TodoWriteInput("2", "Write tests", "pending", "medium"),
//            TodoWriteInput("3", "Update documentation", "completed", "low")
//        )
//
//        val taskState = TaskState(taskId, chatMemoryProvider.get(taskId),mutableMapOf())
//        val result = TodoWriteTool.execute(input,null,taskState)
//
//        val output = result.data
//        assertEquals(3, output.stats.total, "Total should be 3")
//        assertEquals(1, output.stats.pending, "Pending should be 1")
//        assertEquals(0, output.stats.inProgress, "InProgress should be 0")
//        assertEquals(2, output.stats.completed, "Completed should be 2")
//    }
//
//    @Test
//    fun test04_UpdateExistingTodo() {
//        // First create todos
//        val initialInput = listOf(
//            TodoWriteInput("1", "Implement feature A", "pending", "high"),
//            TodoWriteInput("2", "Write tests", "pending", "medium")
//        )
//        val initialTaskState = TaskState(taskId, chatMemoryProvider.get(taskId),mutableMapOf())
//        TodoWriteTool.execute(initialInput, null,initialTaskState)
//
//        // Update todo status
//        val updatedInput = listOf(
//            TodoWriteInput("1", "Implement feature A", "in_progress", "high"),
//            TodoWriteInput("2", "Write tests", "pending", "medium")
//        )
//        val updatedTaskState = TaskState(taskId, chatMemoryProvider.get(taskId),mutableMapOf())
//        val result = TodoWriteTool.execute(updatedInput, null,updatedTaskState)
//
//        val output = result.data
//        assertEquals(2, output.stats.total, "Total should be 2")
//        assertEquals(1, output.stats.pending, "Pending should be 1")
//        assertEquals(1, output.stats.inProgress, "InProgress should be 1")
//        assertEquals(0, output.stats.completed, "Completed should be 0")
//
//        // Verify the first todo is in_progress
//        val storedTodos = getTodos(taskId)
//        val firstTodo = storedTodos.find { it.id == "1" }
//        assertEquals(TodoItem.Status.IN_PROGRESS, firstTodo?.status, "First todo should be in_progress")
//    }
//
//    @Test
//    fun test05_ValidateInputDuplicateIds() {
//        // Test validation with duplicate IDs
//        val json = buildJsonObject {
//            putJsonArray("todos") {
//                addJsonObject {
//                    put("id", "1")
//                    put("content", "Task 1")
//                    put("status", "pending")
//                    put("priority", "medium")
//                }
//                addJsonObject {
//                    put("id", "1")
//                    put("content", "Task 2")
//                    put("status", "pending")
//                    put("priority", "medium")
//                }
//            }
//        }
//        val taskState = TaskState(taskId, chatMemoryProvider.get(taskId),mutableMapOf())
//        assertFailsWith<ToolParameterException> {
//            TodoWriteTool.validateInput(json,taskState)
//        }
//    }
//
//    @Test
//    fun test06_ValidateInputMultipleInProgress() {
//        // Test validation with multiple in_progress tasks
//        val json = buildJsonObject {
//            putJsonArray("todos") {
//                addJsonObject {
//                    put("id", "1")
//                    put("content", "Task 1")
//                    put("status", "in_progress")
//                    put("priority", "medium")
//                }
//                addJsonObject {
//                    put("id", "2")
//                    put("content", "Task 2")
//                    put("status", "in_progress")
//                    put("priority", "medium")
//                }
//            }
//        }
//        val taskState = TaskState(taskId, chatMemoryProvider.get(taskId),mutableMapOf())
//        assertFailsWith<ToolParameterException> {
//            TodoWriteTool.validateInput(json,taskState)
//        }
//    }
//
//    @Test
//    fun test07_ValidateInputEmptyContent() {
//        // Test validation with empty content
//        val json = buildJsonObject {
//            putJsonArray("todos") {
//                addJsonObject {
//                    put("id", "1")
//                    put("content", "")
//                    put("status", "pending")
//                    put("priority", "medium")
//                }
//            }
//        }
//        val taskState = TaskState(taskId, chatMemoryProvider.get(taskId),mutableMapOf())
//        assertFailsWith<ToolParameterException> {
//            TodoWriteTool.validateInput(json,taskState)
//        }
//    }
//
//    @Test
//    fun test08_ValidateInputInvalidStatus() {
//        // Test validation with invalid status
//        val json = buildJsonObject {
//            putJsonArray("todos") {
//                addJsonObject {
//                    put("id", "1")
//                    put("content", "Task 1")
//                    put("status", "invalid_status")
//                    put("priority", "medium")
//                }
//            }
//        }
//
//        val taskState = TaskState(taskId, chatMemoryProvider.get(taskId),mutableMapOf())
//        assertFailsWith<ToolParameterException> {
//            TodoWriteTool.validateInput(json,taskState)
//        }
//    }
//
//    @Test
//    fun test09_ValidateInputInvalidPriority() {
//        // Test validation with invalid priority
//        val json = buildJsonObject {
//            putJsonArray("todos") {
//                addJsonObject {
//                    put("id", "1")
//                    put("content", "Task 1")
//                    put("status", "pending")
//                    put("priority", "invalid_priority")
//                }
//            }
//        }
//        val taskState = TaskState(taskId, chatMemoryProvider.get(taskId),mutableMapOf())
//        assertFailsWith<ToolParameterException> {
//            TodoWriteTool.validateInput(json,taskState)
//        }
//    }
//
//    @Test
//    fun test10_ValidateInputMissingTodosArray() {
//        // Test validation with missing todos array
//        val json = buildJsonObject {
//            put("other_field", "value")
//        }
//        val taskState = TaskState(taskId, chatMemoryProvider.get(taskId),mutableMapOf())
//        assertFailsWith<MissingToolParameterException> {
//            TodoWriteTool.validateInput(json,taskState)
//        }
//    }
//
//    @Test
//    fun test11_ValidateInputEmptyTodosArray() {
//        // Test validation with empty todos array
//        val json = buildJsonObject {
//            putJsonArray("todos") {}
//        }
//
//        val taskState = TaskState(taskId, chatMemoryProvider.get(taskId),mutableMapOf())
//        assertFailsWith<ToolParameterException> {
//            TodoWriteTool.validateInput(json,taskState)
//        }
//    }
//
//    @Test
//    fun test12_ValidateInputMissingId() {
//        // Test validation with missing id
//        val json = buildJsonObject {
//            putJsonArray("todos") {
//                addJsonObject {
//                    put("content", "Task 1")
//                    put("status", "pending")
//                    put("priority", "medium")
//                }
//            }
//        }
//
//        val taskState = TaskState(taskId, chatMemoryProvider.get(taskId),mutableMapOf())
//        assertFailsWith<MissingToolParameterException> {
//            TodoWriteTool.validateInput(json,taskState)
//        }
//    }
//
//    @Test
//    fun test13_ValidateInputMissingContent() {
//        // Test validation with missing content
//        val json = buildJsonObject {
//            putJsonArray("todos") {
//                addJsonObject {
//                    put("id", "1")
//                    put("status", "pending")
//                    put("priority", "medium")
//                }
//            }
//        }
//
//        val taskState = TaskState(taskId, chatMemoryProvider.get(taskId),mutableMapOf())
//        assertFailsWith<MissingToolParameterException> {
//            TodoWriteTool.validateInput(json,taskState)
//        }
//    }
//
//    @Test
//    fun test14_ValidateInputMissingStatus() {
//        // Test validation with missing status
//        val json = buildJsonObject {
//            putJsonArray("todos") {
//                addJsonObject {
//                    put("id", "1")
//                    put("content", "Task 1")
//                    put("priority", "medium")
//                }
//            }
//        }
//
//        val taskState = TaskState(taskId, chatMemoryProvider.get(taskId),mutableMapOf())
//        assertFailsWith<MissingToolParameterException> {
//            TodoWriteTool.validateInput(json,taskState)
//        }
//    }
//
//    @Test
//    fun test15_ValidateInputDefaultPriority() {
//        // Test validation with default priority (missing priority should default to medium)
//        val json = buildJsonObject {
//            putJsonArray("todos") {
//                addJsonObject {
//                    put("id", "1")
//                    put("content", "Task 1")
//                    put("status", "pending")
//                }
//            }
//        }
//        val taskState = TaskState(taskId, chatMemoryProvider.get(taskId),mutableMapOf())
//        // Should not throw exception
//        TodoWriteTool.validateInput(json,taskState)
//    }
//
//    @Test
//    fun test16_ParseStatusCaseSensitivity() {
//        // Test status parsing with different cases
//        val inputs = listOf(
//            TodoWriteInput("1", "Task 1", "pending", "medium"),
//            TodoWriteInput("2", "Task 2", "PENDING", "medium"),
//            TodoWriteInput("3", "Task 3", "Pending", "medium")
//        )
//
//        // All should parse correctly (assuming case-insensitive parsing)
//        inputs.forEach { input ->
//            val taskState = TaskState(taskId, chatMemoryProvider.get(taskId),mutableMapOf())
//            val result = TodoWriteTool.execute(listOf(input), null,taskState)
//            assertEquals(1, result.data.todos.size, "Should parse status correctly")
//            clearTodos(taskId)
//        }
//    }
//
//    @Test
//    fun test17_ParsePriorityCaseSensitivity() {
//        // Test priority parsing with different cases
//        val inputs = listOf(
//            TodoWriteInput("1", "Task 1", "pending", "high"),
//            TodoWriteInput("2", "Task 2", "pending", "HIGH"),
//            TodoWriteInput("3", "Task 3", "pending", "High")
//        )
//
//        // All should parse correctly (assuming case-insensitive parsing)
//        inputs.forEach { input ->
//            val taskState = TaskState(taskId, chatMemoryProvider.get(taskId),mutableMapOf())
//            val result = TodoWriteTool.execute(listOf(input), null,taskState)
//            assertEquals(1, result.data.todos.size, "Should parse priority correctly")
//            clearTodos(taskId)
//        }
//    }
//
//    @Test
//    fun test18_GenerateSummaryWithStats() {
//        // Test summary generation
//        val input = listOf(
//            TodoWriteInput("1", "Implement feature A", "in_progress", "high"),
//            TodoWriteInput("2", "Write tests", "pending", "medium"),
//            TodoWriteInput("3", "Update documentation", "completed", "low"),
//            TodoWriteInput("4", "Review code", "pending", "medium")
//        )
//
//        val taskState = TaskState(taskId, chatMemoryProvider.get(taskId),mutableMapOf())
//        val result = TodoWriteTool.execute(input,null,taskState)
//
//        val summary = result.data.summary
//        assertTrue(summary.contains("4 todo(s)"), "Summary should mention total todos")
//        assertTrue(summary.contains("2 pending"), "Summary should mention pending count")
//        assertTrue(summary.contains("1 in progress"), "Summary should mention in progress count")
//        assertTrue(summary.contains("1 completed"), "Summary should mention completed count")
//    }
//
//    @Test
//    fun test19_RenderResultForAssistant() {
//        // Test rendering result for assistant
//        val output = TodoWriteToolOutput(
//            summary = "Updated 2 todo(s) (1 pending, 1 in progress, 0 completed). Continue tracking your progress with the todo list.",
//            todos = listOf(
//                TodoItem("1", "Task 1", TodoItem.Status.PENDING, TodoItem.Priority.MEDIUM),
//                TodoItem("2", "Task 2", TodoItem.Status.IN_PROGRESS, TodoItem.Priority.HIGH)
//            ),
//            stats = TodoStats(2, 1, 1, 0)
//        )
//
//        val result = TodoWriteTool.renderResultForAssistant(output)
//        assertTrue(result.contains("Todos have been modified successfully"), "Should contain success message")
//    }
//
//    @Test
//    fun test20_GetToolSpecification() {
//        // Test tool specification
//        val spec = TodoWriteTool.getToolSpecification()
//        assertEquals("todo_write", spec.name(), "Tool name should be 'todo_write'")
//        assertNotNull(spec.description(), "Tool should have description")
//        assertNotNull(spec.parameters(), "Tool should have parameters")
//
//        val requiredParams = spec.parameters().required()
//        assertTrue(requiredParams.contains("todos"), "Should require todos parameter")
//    }
//
//    @Test
//    fun test21_GetExecuteFunc() {
//        // Test execute function retrieval
//        val func = TodoWriteTool.getExecuteFunc()
//        assertNotNull(func, "Execute function should not be null")
//        assertEquals("execute", func.name, "Function name should be 'execute'")
//    }
//
//    @Test
//    fun test22_EmptyTodosList() {
//        // Test with empty todos list in execute
//        val input = emptyList<TodoWriteInput>()
//
//        // This should result in storing empty list
//        val taskState = TaskState(taskId, chatMemoryProvider.get(taskId),mutableMapOf())
//        val result = TodoWriteTool.execute(input,null,taskState)
//
//        val output = result.data
//        assertEquals(0, output.stats.total, "Total should be 0")
//        assertEquals(0, output.stats.pending, "Pending should be 0")
//        assertEquals(0, output.stats.inProgress, "InProgress should be 0")
//        assertEquals(0, output.stats.completed, "Completed should be 0")
//    }
//
//    @Test
//    fun test23_TodoPriorities() {
//        // Test all priority levels
//        val input = listOf(
//            TodoWriteInput("1", "High priority task", "pending", "high"),
//            TodoWriteInput("2", "Medium priority task", "pending", "medium"),
//            TodoWriteInput("3", "Low priority task", "pending", "low")
//        )
//
//        val taskState = TaskState(taskId, chatMemoryProvider.get(taskId),mutableMapOf())
//        val result = TodoWriteTool.execute(input,null,taskState)
//
//        val storedTodos = getTodos(taskId)
//        assertEquals(TodoItem.Priority.HIGH, storedTodos.find { it.id == "1" }?.priority)
//        assertEquals(TodoItem.Priority.MEDIUM, storedTodos.find { it.id == "2" }?.priority)
//        assertEquals(TodoItem.Priority.LOW, storedTodos.find { it.id == "3" }?.priority)
//    }
//
//    @Test
//    fun test24_TodoTimestamps() {
//        // Test that timestamps are set
//        val input = listOf(
//            TodoWriteInput("1", "Task with timestamps", "pending", "medium")
//        )
//
//        val taskState = TaskState(taskId, chatMemoryProvider.get(taskId),mutableMapOf())
//        val result = TodoWriteTool.execute(input,null,taskState)
//
//        val storedTodos = getTodos(taskId)
//        val todo = storedTodos.first()
//        assertNotNull(todo.createdAt, "CreatedAt should be set")
//        assertNotNull(todo.updatedAt, "UpdatedAt should be set")
//    }
//
//    @Test
//    fun test25_TodoUpdate_TimestampChange() {
//        // Test that updatedAt changes on update
//        val initialInput = listOf(
//            TodoWriteInput("1", "Task 1", "pending", "medium")
//        )
//        val initialTaskState = TaskState(taskId, chatMemoryProvider.get(taskId),mutableMapOf())
//        TodoWriteTool.execute(initialInput, null,initialTaskState)
//
//        val firstTodo = getTodos(taskId).first()
//        val initialUpdatedAt = firstTodo.updatedAt
//
//        // Wait a bit to ensure timestamp changes
//        Thread.sleep(10)
//
//        // Update the todo
//        val updatedInput = listOf(
//            TodoWriteInput("1", "Task 1", "in_progress", "medium")
//        )
//        val updatedTaskState = TaskState(taskId, chatMemoryProvider.get(taskId),mutableMapOf())
//        TodoWriteTool.execute(updatedInput, null,updatedTaskState)
//
//        val updatedTodo = getTodos(taskId).first()
//        assertNotEquals(initialUpdatedAt, updatedTodo.updatedAt, "UpdatedAt should change on update")
//    }
//
//    @Test
//    fun test26_MultipleTodosOrdering() {
//        // Test that todos are ordered correctly (by status priority)
//        val input = listOf(
//            TodoWriteInput("1", "Completed task", "completed", "high"),
//            TodoWriteInput("2", "In progress task", "in_progress", "high"),
//            TodoWriteInput("3", "Pending task", "pending", "high")
//        )
//
//        val taskState = TaskState(taskId, chatMemoryProvider.get(taskId),mutableMapOf())
//        val result = TodoWriteTool.execute(input,null,taskState)
//
//        val storedTodos = getTodos(taskId)
//        println(storedTodos)
//        // Verify ordering: IN_PROGRESS, PENDING, COMPLETED
//        assertEquals("2", storedTodos[0].id, "First should be in_progress")
//        assertEquals("3", storedTodos[1].id, "Second should be pending")
//        assertEquals("1", storedTodos[2].id, "Third should be completed")
//    }
//
//    @Test
//    fun test27_TodoContentWithSpecialCharacters() {
//        // Test todo content with special characters
//        val input = listOf(
//            TodoWriteInput("1", "Task with \"quotes\" and 'apostrophes'", "pending", "medium"),
//            TodoWriteInput("2", "Task with <angle> brackets & ampersand", "pending", "medium"),
//            TodoWriteInput("3", "Task with émojis 🎉 and unicode ñ", "pending", "medium")
//        )
//
//        val taskState = TaskState(taskId, chatMemoryProvider.get(taskId),mutableMapOf())
//        val result = TodoWriteTool.execute(input,null,taskState)
//
//        val storedTodos = getTodos(taskId)
//        assertEquals(3, storedTodos.size, "Should store all todos with special characters")
//        assertEquals("Task with \"quotes\" and 'apostrophes'", storedTodos.find { it.id == "1" }?.content)
//    }
//
//    @Test
//    fun test28_ValidateInputWhitespaceContent() {
//        // Test validation with whitespace-only content
//        val json = buildJsonObject {
//            putJsonArray("todos") {
//                addJsonObject {
//                    put("id", "1")
//                    put("content", "   ")
//                    put("status", "pending")
//                    put("priority", "medium")
//                }
//            }
//        }
//
//        val taskState = TaskState(taskId, chatMemoryProvider.get(taskId),mutableMapOf())
//        assertFailsWith<ToolParameterException> {
//            TodoWriteTool.validateInput(json,taskState)
//        }
//    }
//
//    @Test
//    fun test29_ErrorHandling() {
//        // Test error handling with invalid input
//        val invalidInput = listOf(
//            TodoWriteInput("1", "Task 1", "invalid_status", "medium")
//        )
//
//        // Should catch the error and return error output
//        val taskState = TaskState(taskId, chatMemoryProvider.get(taskId),mutableMapOf())
//        val result = TodoWriteTool.execute(invalidInput, null,taskState)
//
//        assertTrue(result.data.summary.contains("Error"), "Should contain error message")
//        assertEquals(0, result.data.todos.size, "Should have no todos on error")
//    }
//
//    @Test
//    fun test30_ValidInputWithAllFields() {
//        // Test valid input with all fields
//        val json = buildJsonObject {
//            putJsonArray("todos") {
//                addJsonObject {
//                    put("id", "task-1")
//                    put("content", "Implement authentication")
//                    put("status", "in_progress")
//                    put("priority", "high")
//                }
//                addJsonObject {
//                    put("id", "task-2")
//                    put("content", "Write unit tests")
//                    put("status", "pending")
//                    put("priority", "medium")
//                }
//            }
//        }
//        val taskState = TaskState(taskId, chatMemoryProvider.get(taskId),mutableMapOf())
//        // Should not throw exception
//        assertDoesNotThrow {
//            TodoWriteTool.validateInput(json,taskState)
//        }
//    }
//}
//
//// Helper function to avoid repeating assertDoesNotThrow pattern
//private fun assertDoesNotThrow(block: () -> Unit) {
//    try {
//        block()
//    } catch (e: Exception) {
//        fail("Expected no exception but got: ${e.message}")
//    }
//}
