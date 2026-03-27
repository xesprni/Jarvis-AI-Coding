//package com.qifu.agent.tool
//
//import com.qifu.agent.TaskState
//import com.qifu.agent.chatMemoryProvider
//import kotlinx.coroutines.test.runTest
//import org.junit.FixMethodOrder
//import org.junit.runners.MethodSorters
//import java.io.File
//import kotlin.test.*
//
//@FixMethodOrder(MethodSorters.NAME_ASCENDING)
//class EditToolTest {
//
//    private val testDir = "test_output"
//    private val testFile = "$testDir/test_edit_tool.txt"
//    private val existingFile = "$testDir/existing_edit_file.txt"
//    val taskId = "BashToolTest"
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
////    @AfterTest
//    fun tearDown() {
//        // 清理测试文件
//        File(testDir).deleteRecursively()
//    }
//
//    @Test
//    fun testEditToolCreateNewFile() = runTest {
//        // 测试创建新文件
//        val content = "Hello, World!\nThis is a new file."
//
//        val result = EditTool.execute(testFile, "", content,false,taskState)
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        assertEquals("result", result.type, "Result type should be 'result'")
//        assertNotNull(result.data, "Result data should not be null")
//
//        val output = result.data
//        assertEquals(testFile, output.filePath, "File path should match")
//        assertEquals("", output.oldString, "Old string should be empty for new file")
//        assertEquals(content, output.newString, "New string should match")
//        assertEquals("", output.originalContent, "Original file should be empty for new file")
//        assertEquals("create", output.operation, "Operation should be 'create'")
//
//        // 验证文件确实被创建
//        val file = File(testFile)
//        assertTrue(file.exists(), "File should be created")
//        assertEquals(content, file.readText(), "File content should match")
//
//        // 验证助手结果显示
//        val assistantResult = result.resultForAssistant
//        assertTrue(assistantResult.contains("has been updated"), "Assistant result should mention file update")
//        assertTrue(assistantResult.contains(testFile), "Assistant result should contain file path")
//    }
//
//    @Test
//    fun testEditToolUpdateExistingFile() {
//        // 先创建一个现有文件
//        val originalContent = "Original line 1\nOriginal line 2\nOriginal line 3"
//        File(existingFile).writeText(originalContent)
//
//        // 模拟文件已被读取
//        taskState.fileFreshnessService?.recordFileEdit(File(existingFile).absolutePath, originalContent)
//
//        // 更新文件内容
//        val oldString = "Original line 2"
//        val newString = "Modified line 2"
//        val result = EditTool.execute(existingFile, oldString, newString,false,taskState)
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        val output = result.data
//        assertEquals("update", output.operation, "Operation should be 'update'")
//        assertEquals(existingFile, output.filePath, "File path should match")
//        assertEquals(oldString, output.oldString, "Old string should match")
//        assertEquals(newString, output.newString, "New string should match")
//        assertEquals(originalContent, output.originalContent, "Original file content should match")
//
//        // 验证文件确实被更新
//        val file = File(existingFile)
//        assertTrue(file.exists(), "File should exist")
//        val expectedContent = originalContent.replace(oldString, newString)
//        assertEquals(expectedContent, file.readText(), "File content should be updated")
//
//        // 验证助手结果显示
//        val assistantResult = result.resultForAssistant
//        assertTrue(assistantResult.contains("has been updated"), "Assistant result should mention file update")
//        assertTrue(assistantResult.contains(existingFile), "Assistant result should contain file path")
//    }
//
//    @Test
//    fun testEditToolReplaceAll() {
//        // 创建包含多个相同字符串的文件
//        val originalContent = "Hello world\nHello everyone\nHello again"
//        File(existingFile).writeText(originalContent)
//
//        // 模拟文件已被读取
//        taskState.fileFreshnessService?.recordFileEdit(File(existingFile).absolutePath, originalContent)
//
//        // 使用replace_all替换所有"Hello"
//        val oldString = "Hello"
//        val newString = "Hi"
//        val result = EditTool.execute(existingFile, oldString, newString, replace_all = true,taskState)
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        val output = result.data
//        assertEquals("update", output.operation, "Operation should be 'update'")
//
//        // 验证文件内容
//        val file = File(existingFile)
//        val expectedContent = "Hi world\nHi everyone\nHi again"
//        assertEquals(expectedContent, file.readText(), "All occurrences should be replaced")
//    }
//
//    @Test
//    fun testEditToolDeleteContent() {
//        // 创建文件
//        val originalContent = "Line 1\nLine to delete\nLine 3"
//        File(existingFile).writeText(originalContent)
//
//        // 模拟文件已被读取
//        taskState.fileFreshnessService?.recordFileEdit(File(existingFile).absolutePath, originalContent)
//
//        // 删除一行内容
//        val oldString = "Line to delete\n"
//        val newString = ""
//        val result = EditTool.execute(existingFile, oldString, newString,false,taskState)
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        val output = result.data
//        assertEquals("delete", output.operation, "Operation should be 'delete'")
//
//        // 验证文件内容
//        val file = File(existingFile)
//        val expectedContent = "Line 1\nLine 3"
//        assertEquals(expectedContent, file.readText(), "Line should be deleted")
//    }
//
//    @Test
//    fun testEditToolValidationSameStrings() {
//        // 测试新旧字符串相同的情况
//        val originalContent = "Test content"
//        File(existingFile).writeText(originalContent)
//        taskState.fileFreshnessService?.recordFileEdit(File(existingFile).absolutePath, originalContent)
//
//        assertFailsWith<ToolExecutionException>("Should throw exception for same old_string and new_string") {
//            EditTool.execute(existingFile, "Test", "Test",false,taskState)
//        }
//    }
//
////    @Test
////修改为前置校验
//    fun testEditToolValidationFileNotRead() {
//        // 测试文件未读取的情况
//        val content = "Test content"
//        File(existingFile).writeText(content)
//        // 不更新读取时间戳
//
//        assertFailsWith<ToolExecutionException>("Should throw exception for unread file") {
//            EditTool.execute(existingFile, "Test", "Modified",false,taskState)
//        }
//    }
//
////    @Test
////修改为前置校验
//    fun testEditToolValidationFileNotExists() {
//        // 测试文件不存在的情况
//        assertFailsWith<ToolExecutionException>("Should throw exception for non-existent file") {
//            EditTool.execute("non_existent_file.txt", "old", "new",false,taskState)
//        }
//    }
//
//    @Test
//    fun testEditToolValidationStringNotFound() {
//        // 测试要替换的字符串不存在的情况
//        val originalContent = "Test content"
//        File(existingFile).writeText(originalContent)
//        taskState.fileFreshnessService?.recordFileEdit(File(existingFile).absolutePath, originalContent)
//
//        assertFailsWith<ToolExecutionException>("Should throw exception for string not found") {
//            EditTool.execute(existingFile, "NonExistent", "New",false,taskState)
//        }
//    }
//
////    @Test
////修改为前置校验
//    fun testEditToolValidationMultipleMatches() {
//        // 测试多个匹配项的情况（非replace_all模式）
//        val originalContent = "Test test Test"
//        File(existingFile).writeText(originalContent)
//    taskState.fileFreshnessService?.recordFileEdit(File(existingFile).absolutePath, originalContent)
//
//        assertFailsWith<ToolExecutionException>("Should throw exception for multiple matches without replace_all") {
//            EditTool.execute(existingFile, "Test", "Modified", replace_all = false,taskState)
//        }
//    }
//
////    @Test
//    //修改为前置校验
//    fun testEditToolValidationCreateExistingFile() {
//        // 测试创建已存在文件的情况
//        val content = "Existing content"
//        File(existingFile).writeText(content)
//
//        assertFailsWith<ToolExecutionException>("Should throw exception for creating existing file") {
//            EditTool.execute(existingFile, "", "New content",false,taskState)
//        }
//    }
//
//    @Test
//    fun testEditToolGetSnippet() {
//        // 测试获取代码片段功能
//        val initialText = "Line 1\nLine 2\nLine 3\nLine 4\nLine 5\nLine 6\nLine 7\nLine 8\nLine 9\nLine 10"
//        val oldStr = "Line 5"
//        val newStr = "Modified Line 5"
//
//        val (snippet, startLine) = EditTool.getSnippet(initialText, oldStr, newStr)
//
//        // 验证片段包含正确的行数和内容
//        assertTrue(snippet.contains("Modified Line 5"), "Snippet should contain modified line")
//        assertTrue(snippet.contains("Line 1"), "Snippet should contain context before")
//        assertTrue(snippet.contains("Line 9"), "Snippet should contain context after")
//        assertEquals(1, startLine, "Start line should be correct")
//    }
//
//    @Test
//    fun testEditToolRenderResultForAssistant() {
//        // 测试助手结果渲染
//        val output = EditToolOutput(
//            filePath = "/test/path/file.txt",
//            oldString = "old content",
//            newString = "new content",
//            originalContent = "Line 1\nold content\nLine 3",
//            structuredPatch = emptyList(),
//            operation = "update"
//        )
//
//        val result = EditTool.renderResultForAssistant(output)
//        assertTrue(result.contains("has been updated"), "Should mention file update")
//        assertTrue(result.contains("/test/path/file.txt"), "Should contain file path")
//        assertTrue(result.contains("cat -n"), "Should mention cat command")
//    }
//
//    @Test
//    fun testEditToolGetToolSpecification() {
//        // 测试工具规范获取
//        val spec = EditTool.getToolSpecification()
//        assertEquals("Edit", spec.name(), "Tool name should be 'Edit'")
//        assertNotNull(spec.description(), "Tool should have description")
//        assertNotNull(spec.parameters(), "Tool should have parameters")
//
//        // 验证必需参数
//        val requiredParams = spec.parameters().required()
//        assertTrue(requiredParams.contains("file_path"), "Should require file_path")
//        assertTrue(requiredParams.contains("old_string"), "Should require old_string")
//        assertTrue(requiredParams.contains("new_string"), "Should require new_string")
//    }
//
//    @Test
//    fun testEditToolGetExecuteFunc() {
//        // 测试执行函数获取
//        val func = EditTool.getExecuteFunc()
//        assertNotNull(func, "Execute function should not be null")
//        assertEquals("execute", func.name, "Function name should be 'execute'")
//    }
//
//    @Test
//    fun testEditToolWithSpecialCharacters() {
//        // 测试特殊字符处理
//        val originalContent = "Special chars: & < > \" '\nUnicode: 你好 世界"
//        File(existingFile).writeText(originalContent)
//        taskState.fileFreshnessService?.recordFileEdit(File(existingFile).absolutePath, originalContent)
//
//        val oldString = "你好 世界"
//        val newString = "Hello World 🌍"
//        val result = EditTool.execute(existingFile, oldString, newString,false,taskState)
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        val output = result.data
//        assertEquals(oldString, output.oldString, "Old string should match with special characters")
//        assertEquals(newString, output.newString, "New string should match with special characters")
//
//        // 验证文件内容
//        val file = File(existingFile)
//        val expectedContent = originalContent.replace(oldString, newString)
//        assertEquals(expectedContent, file.readText(), "File content should preserve special characters")
//    }
//
//    @Test
//    fun testEditToolWithEmptyNewString() {
//        // 测试新字符串为空的情况
//        val originalContent = "Keep this\nRemove this line\nKeep this too"
//        File(existingFile).writeText(originalContent)
//        taskState.fileFreshnessService?.recordFileEdit(File(existingFile).absolutePath, originalContent)
//
//        val oldString = "Remove this line\n"
//        val newString = ""
//        val result = EditTool.execute(existingFile, oldString, newString,false,taskState)
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        val output = result.data
//        assertEquals("delete", output.operation, "Operation should be 'delete' when new_string is empty")
//
//        // 验证文件内容
//        val file = File(existingFile)
//        val expectedContent = "Keep this\nKeep this too"
//        assertEquals(expectedContent, file.readText(), "Line should be removed")
//    }
//
//    @Test
//    fun testEditToolApplyEditFunction() {
//        // 测试applyEdit函数
//        val testFilePath = "$testDir/apply_edit_test1.txt"
//        val content = "Line 1\nLine 2\nLine 3"
//        File(testFilePath).writeText(content)
//
//        val (patch, updatedFile, originalFile, operation) = EditTool.applyEdit(
//            testFilePath, "Line 2", "Modified Line 2", true
//        )
//
//        assertEquals(content, originalFile, "Original file should match")
//        assertEquals("Line 1\nModified Line 2\nLine 3", updatedFile, "Updated file should be correct")
//        assertEquals("update", operation, "Operation should be 'update'")
//        assertNotNull(patch, "Patch should not be null")
//    }
//}
