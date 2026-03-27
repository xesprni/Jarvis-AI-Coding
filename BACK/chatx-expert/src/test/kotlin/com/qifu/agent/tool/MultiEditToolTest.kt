//package com.qifu.agent.tool
//
//import com.qifu.agent.TaskState
//import com.qifu.agent.chatMemoryProvider
//import org.junit.FixMethodOrder
//import org.junit.runners.MethodSorters
//import java.io.File
//import kotlin.test.*
//
//@FixMethodOrder(MethodSorters.NAME_ASCENDING)
//class MultiEditToolTest {
//
//    private val testDir = "test_output"
//    private val testFile = "$testDir/test_multi_edit_tool.txt"
//    private val existingFile = "$testDir/existing_multi_edit_file.txt"
//    val taskId = "MultiEditToolTest"
//    val taskState = TaskState(taskId, chatMemoryProvider.get(taskId),mutableMapOf())
//    @BeforeTest
//    fun setUp() {
//        // 创建测试目录
//        File(testDir).mkdirs()
//
//        // 清理可能存在的文件
//        File(testFile).delete()
//        File(existingFile).delete()
//    }
//
//    @Test
//    fun testMultiEditToolCreateNewFile() {
//        // 测试创建新文件（单个编辑）
//        val edits = listOf(
//            EditOperation(
//                "",
//                "Hello, World!\nThis is a new file with multiple lines.\nEnd of file.",
//            )
//        )
//
//        val result = MultiEditTool.execute(testFile, edits, taskState)
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        assertEquals("result", result.type, "Result type should be 'result'")
//        assertNotNull(result.data, "Result data should not be null")
//
//        val output = result.data
//        assertEquals(testFile, output.filePath, "File path should match")
//        assertTrue(output.wasNewFile, "Should be marked as new file")
//        assertEquals(1, output.totalEdits, "Should have 1 edit")
//        assertEquals(1, output.editsApplied.size, "Should have 1 applied edit")
//        assertEquals("create", output.operation, "Operation should be 'create'")
//
//        // 验证文件确实被创建
//        val file = File(testFile)
//        assertTrue(file.exists(), "File should be created")
//        val expectedContent = "Hello, World!\nThis is a new file with multiple lines.\nEnd of file."
//        assertEquals(expectedContent, file.readText(), "File content should match")
//    }
//
//    @Test
//    fun testMultiEditToolMultipleEditsExistingFile() {
//        // 先创建一个现有文件
//        val originalContent = """Original line 1
//Original line 2
//Original line 3
//Original line 4
//Original line 5"""
//        File(existingFile).writeText(originalContent)
//
//        // 模拟文件已被读取
//        taskState.fileFreshnessService?.recordFileEdit(File(existingFile).absolutePath, originalContent)
//
//        // 执行多个编辑操作
//        val edits = listOf(
//            EditOperation("Original line 2", "Modified line 2"),
//            EditOperation("Original line 4", "Updated line 4"),
//            EditOperation( "line 5", "line FIVE")
//        )
//
//        val result = MultiEditTool.execute(existingFile, edits,taskState)
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        val output = result.data
//        assertFalse(output.wasNewFile, "Should not be marked as new file")
//        assertEquals(3, output.totalEdits, "Should have 3 edits")
//        assertEquals(3, output.editsApplied.size, "Should have 3 applied edits")
//        assertEquals("update", output.operation, "Operation should be 'update'")
//
//        // 验证每个编辑都成功应用
//        output.editsApplied.forEach { edit ->
//            assertTrue(edit.success, "Edit ${edit.editIndex} should be successful")
//            assertEquals(1, edit.occurrences, "Edit ${edit.editIndex} should have 1 occurrence")
//        }
//
//        // 验证文件内容
//        val file = File(existingFile)
//        val expectedContent = """Original line 1
//Modified line 2
//Original line 3
//Updated line 4
//Original line FIVE"""
//        assertEquals(expectedContent, file.readText(), "File content should reflect all edits")
//    }
//
//    @Test
//    fun testMultiEditToolReplaceAll() {
//        // 创建包含多个相同字符串的文件
//        val originalContent = "Hello world\nHello everyone\nHello again\nGoodbye world"
//        File(existingFile).writeText(originalContent)
//
//        // 模拟文件已被读取
//        taskState.fileFreshnessService?.recordFileEdit(File(existingFile).absolutePath, originalContent)
//
//        // 使用replace_all替换所有"Hello"，然后替换"world"
//        val edits = listOf(
//            EditOperation(
//                "Hello",
//                "Hi",
//                true
//            ),
//            EditOperation(
//                "world",
//                "everyone",
//                true
//            )
//        )
//
//        val result = MultiEditTool.execute(existingFile, edits,taskState)
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        val output = result.data
//        assertEquals(2, output.totalEdits, "Should have 2 edits")
//
//        // 验证第一个编辑替换了3个"Hello"
//        assertEquals(3, output.editsApplied[0].occurrences, "First edit should replace 3 occurrences")
//        // 验证第二个编辑替换了2个"world"（包括"Goodbye world"中的）
//        assertEquals(2, output.editsApplied[1].occurrences, "Second edit should replace 2 occurrences")
//
//        // 验证文件内容
//        val file = File(existingFile)
//        val expectedContent = "Hi everyone\nHi everyone\nHi again\nGoodbye everyone"
//        assertEquals(expectedContent, file.readText(), "All occurrences should be replaced")
//    }
//
//    @Test
//    fun testMultiEditToolSequentialEdits() {
//        // 测试编辑操作的顺序依赖性
//        val originalContent = "Step 1: Initial\nStep 2: Process\nStep 3: Final"
//        File(existingFile).writeText(originalContent)
//
//        // 模拟文件已被读取
//        taskState.fileFreshnessService?.recordFileEdit(File(existingFile).absolutePath, originalContent)
//
//        // 第一个编辑修改"Step 1"，第二个编辑基于第一个编辑的结果
//        val edits = listOf(
//            EditOperation("Step 1: Initial", "Step 1: Started"),
//            EditOperation("Step 1: Started", "Step 1: Completed")
//        )
//
//        val result = MultiEditTool.execute(existingFile, edits,taskState)
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        val output = result.data
//        assertEquals(2, output.totalEdits, "Should have 2 edits")
//
//        // 验证文件内容
//        val file = File(existingFile)
//        val expectedContent = "Step 1: Completed\nStep 2: Process\nStep 3: Final"
//        assertEquals(expectedContent, file.readText(), "Sequential edits should work correctly")
//    }
//
////    @Test
//    //修改为前置校验
//    fun testMultiEditToolValidationSameStrings() {
//        // 测试新旧字符串相同的情况
//        val content = "Test content"
//        File(existingFile).writeText(content)
//        taskState.fileFreshnessService?.recordFileEdit(File(existingFile).absolutePath, content)
//
//        val edits = listOf(
//            EditOperation("Test", "Test")
//        )
//
//        assertFailsWith<ToolExecutionException>("Should throw exception for same old_string and new_string") {
//            MultiEditTool.execute(existingFile, edits,taskState)
//        }
//    }
//
////    @Test
//    //修改为前置校验
//    fun testMultiEditToolValidationFileNotRead() {
//        // 测试文件未读取的情况
//        val content = "Test content"
//        File(existingFile).writeText(content)
//        // 不更新读取时间戳
//
//        val edits = listOf(
//            EditOperation("Test", "Modified")
//        )
//
//        assertFailsWith<ToolExecutionException>("Should throw exception for unread file") {
//            MultiEditTool.execute(existingFile, edits,taskState)
//        }
//    }
//
//    @Test
//    fun testMultiEditToolValidationFileNotExists() {
//        // 测试文件不存在且第一个编辑不是创建文件的情况
//        val edits = listOf(
//            EditOperation("old", "new")
//        )
//
//        assertFailsWith<ToolExecutionException>("Should throw exception for non-existent file with non-empty old_string") {
//            MultiEditTool.execute("non_existent_file.txt", edits,taskState)
//        }
//    }
//
//    @Test
//    fun testMultiEditToolValidationStringNotFound() {
//        // 测试要替换的字符串不存在的情况
//        val content = "Test content"
//        File(existingFile).writeText(content)
//        taskState.fileFreshnessService?.recordFileEdit(File(existingFile).absolutePath, content)
//
//        val edits = listOf(
//            EditOperation("NonExistent", "New")
//        )
//
//        assertFailsWith<ToolExecutionException>("Should throw exception for string not found") {
//            MultiEditTool.execute(existingFile, edits,taskState)
//        }
//    }
//
//    @Test
//    fun testMultiEditToolValidationNewFileWrongFormat() {
//        // 测试新文件但第一个编辑不是空old_string的情况
//        val edits = listOf(
//            EditOperation("something", "New content")
//        )
//
//        assertFailsWith<ToolExecutionException>("Should throw exception for new file without empty old_string") {
//            MultiEditTool.execute("new_file.txt", edits,taskState)
//        }
//    }
//
////    @Test
//    //修改为前置校验
//    fun testMultiEditToolValidationParentDirNotExists() {
//        // 测试父目录不存在的情况
//        val edits = listOf(
//            EditOperation("", "New content")
//        )
//
//        assertFailsWith<ToolExecutionException>("Should throw exception for non-existent parent directory") {
//            MultiEditTool.execute("non_existent_dir/new_file.txt", edits,taskState)
//        }
//    }
//
//    @Test
//    fun testMultiEditToolEmptyEditsList() {
//        // 测试空编辑列表
//        val edits = emptyList<EditOperation>()
//
//        assertFailsWith<ToolParameterException>("Should throw exception for empty edits list") {
//            MultiEditTool.execute(existingFile, edits,taskState)
//        }
//    }
//
////    @Test
//    fun testMultiEditToolMissingParameters() {
//        // 测试缺少参数的情况
//        val edits = listOf(
//            EditOperation("test","") // 缺少new_string
//        )
//
//        assertFailsWith<ToolParameterException>("Should throw exception for missing new_string") {
//            MultiEditTool.execute(existingFile, edits,taskState)
//        }
//    }
//
//    @Test
//    fun testMultiEditToolDeleteContent() {
//        // 测试删除内容（new_string为空）
//        val originalContent = "Line 1\nLine to delete\nLine 3\nAnother line to delete\nLine 5"
//        File(existingFile).writeText(originalContent)
//
//        // 模拟文件已被读取
//        taskState.fileFreshnessService?.recordFileEdit(File(existingFile).absolutePath, originalContent)
//
//        val edits = listOf(
//            EditOperation("Line to delete\n", ""),
//            EditOperation("Another line to delete\n", "")
//        )
//
//        val result = MultiEditTool.execute(existingFile, edits,taskState)
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        val output = result.data
//        assertEquals(2, output.totalEdits, "Should have 2 edits")
//
//        // 验证文件内容
//        val file = File(existingFile)
//        val expectedContent = "Line 1\nLine 3\nLine 5"
//        assertEquals(expectedContent, file.readText(), "Lines should be deleted")
//    }
//
//    @Test
//    fun testMultiEditToolGetToolSpecification() {
//        // 测试工具规范获取
//        val spec = MultiEditTool.getToolSpecification()
//        assertEquals("MultiEdit", spec.name(), "Tool name should be 'MultiEdit'")
//        assertNotNull(spec.description(), "Tool should have description")
//        assertNotNull(spec.parameters(), "Tool should have parameters")
//
//        // 验证必需参数
//        val requiredParams = spec.parameters().required()
//        assertTrue(requiredParams.contains("file_path"), "Should require file_path")
//        assertTrue(requiredParams.contains("edits"), "Should require edits")
//    }
//
//    @Test
//    fun testMultiEditToolGetExecuteFunc() {
//        // 测试执行函数获取
//        val func = MultiEditTool.getExecuteFunc()
//        assertNotNull(func, "Execute function should not be null")
//        assertEquals("execute", func.name, "Function name should be 'execute'")
//    }
//
//    @Test
//    fun testMultiEditToolRenderResultForAssistant() {
//        // 测试助手结果渲染
//        val output = MultiEditToolOutput(
//            filePath = "/test/path/file.txt",
//            wasNewFile = false,
//            editsApplied = listOf(
//                AppliedEdit(1, true, "old content", "new content", 1)
//            ),
//            totalEdits = 1,
//            summary = "Successfully applied 1 edits to /test/path/file.txt",
//            structuredPatch = emptyList(),
//            originalFile = "Line 1\nold content\nLine 3",
//            modifiedFile = "Line 1\nnew content\nLine 3",
//            operation = "update"
//        )
//
//        val result = MultiEditTool.renderResultForAssistant(output)
//        assertTrue(result.contains("has been updated"), "Should mention file update")
//        assertTrue(result.contains("/test/path/file.txt"), "Should contain file path")
//        assertTrue(result.contains("1 edits applied"), "Should mention number of edits")
//        assertTrue(result.contains("Successfully applied"), "Should contain summary")
//    }
//
//    @Test
//    fun testMultiEditToolComplexScenario() {
//        // 测试复杂场景：创建文件并进行多次编辑
//        val edits = listOf(
//            // 创建文件
//            EditOperation("", "Initial content\nLine 2\nLine 3"),
//            // 修改第一行
//            EditOperation("Initial content", "Modified content"),
//            // 添加新行
//            EditOperation("Line 3", "Line 3\nLine 4\nLine 5"),
//            // 批量替换
//            EditOperation("Line", "Row", true)
//        )
//
//        val result = MultiEditTool.execute(testFile, edits,taskState)
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        val output = result.data
//        assertTrue(output.wasNewFile, "Should be marked as new file")
//        assertEquals(4, output.totalEdits, "Should have 4 edits")
//        assertEquals(4, output.editsApplied.size, "Should have 4 applied edits")
//
//        // 验证最后一个编辑替换了多个"Line"
//        assertTrue(output.editsApplied[3].occurrences >= 3, "Last edit should replace multiple occurrences")
//
//        // 验证文件内容
//        val file = File(testFile)
//        val expectedContent = "Modified content\nRow 2\nRow 3\nRow 4\nRow 5"
//        assertEquals(expectedContent, file.readText(), "Complex scenario should work correctly")
//    }
//
//    @Test
//    fun testMultiEditToolApplyContentEdit() {
//        // 测试内容编辑函数（通过反射访问私有方法）
//        val method = MultiEditTool::class.java.getDeclaredMethod(
//            "applyContentEdit",
//            String::class.java,
//            String::class.java,
//            String::class.java,
//            Boolean::class.java
//        )
//        method.isAccessible = true
//
//        // 测试普通替换
//        val result1 = method.invoke(MultiEditTool, "Hello world", "world", "Kotlin", false) as ContentEditResult
//        assertEquals("Hello Kotlin", result1.newContent)
//        assertEquals(1, result1.occurrences)
//
//        // 测试replace_all
//        val result2 = method.invoke(MultiEditTool, "test test test", "test", "exam", true) as ContentEditResult
//        assertEquals("exam exam exam", result2.newContent)
//        assertEquals(3, result2.occurrences)
//    }
//
//    @Test
//    fun testMultiEditToolFailedEdit() {
//        // 测试编辑失败的情况（第二个编辑失败）
//        val originalContent = "Line 1\nLine 2\nLine 3"
//        File(existingFile).writeText(originalContent)
//        taskState.fileFreshnessService?.recordFileEdit(File(existingFile).absolutePath, originalContent)
//
//        val edits = listOf(
//            EditOperation("Line 1", "Modified Line 1"),
//            EditOperation("NonExistent", "This will fail")
//        )
//
//        // 第二个编辑应该失败，导致整个操作失败
//        assertFailsWith<ToolExecutionException>("Should throw exception when any edit fails") {
//            MultiEditTool.execute(existingFile, edits,taskState)
//        }
//
//        // 验证原文件未被修改（原子性）
//        val file = File(existingFile)
//        assertEquals(originalContent, file.readText(), "File should remain unchanged when operation fails")
//    }
//}
