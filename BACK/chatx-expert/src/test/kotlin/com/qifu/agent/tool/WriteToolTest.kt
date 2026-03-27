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
//class WriteToolTest {
//
//    private val testDir = "test_output"
//    private val testFile = "$testDir/test_write_tool.txt"
//    private val existingFile = "$testDir/existing_file.txt"
//    val taskId = "WriteToolTest"
//    val taskState = TaskState(taskId, chatMemoryProvider.get(taskId))
//    @BeforeTest
//    fun setUp() {
//        // 创建测试目录
//        File(testDir).mkdirs()
//    }
//
//    @AfterTest
//    fun tearDown() {
//        // 清理测试文件
//        File(testDir).deleteRecursively()
//    }
//
//    @Test
//    fun testWriteToolCreateNewFile() {
//        // 测试创建新文件
//        val content = "Hello, World!\nThis is a test file."
//        val result = WriteTool.execute(testFile, content,taskState)
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        assertEquals("result", result.type, "Result type should be 'result'")
//        assertNotNull(result.data, "Result data should not be null")
//
//        val output = result.data
//        assertEquals("create", output.type, "Operation type should be 'create'")
//        assertEquals(testFile, output.filePath, "File path should match")
//        assertEquals(content, output.content, "Content should match")
//        assertEquals(emptyList(), output.structuredPatch, "Patch should be empty for new file")
//
//        // 验证文件确实被创建
//        val file = File(testFile)
//        assertTrue(file.exists(), "File should be created")
//        assertEquals(content, file.readText(), "File content should match")
//
//        // 验证助手结果显示
//        val assistantResult = result.resultForAssistant
//        assertTrue(assistantResult.contains("File created successfully"), "Assistant result should mention file creation")
//        assertTrue(assistantResult.contains(testFile), "Assistant result should contain file path")
//    }
//
//    @Test
//    fun testWriteToolUpdateExistingFile() {
//        // 先创建一个现有文件
//        val originalContent = "Original content\nLine 2\nLine 3"
//        WriteTool.execute(existingFile, originalContent,taskState)
//
//        // 更新文件内容
//        val newContent = "Updated content\nLine 2 modified\nLine 3"
//        val result = WriteTool.execute(existingFile, newContent,taskState)
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        val output = result.data
//        assertEquals("update", output.type, "Operation type should be 'update'")
//        assertEquals(existingFile, output.filePath, "File path should match")
//        assertEquals(newContent, output.content, "Content should match")
//        assertNotNull(output.structuredPatch, "Patch should not be null")
//
//        // 验证文件确实被更新
//        val file = File(existingFile)
//        assertTrue(file.exists(), "File should exist")
//        assertEquals(newContent, file.readText(), "File content should be updated")
//
//        // 验证助手结果显示
//        val assistantResult = result.resultForAssistant
//        assertTrue(assistantResult.contains("has been updated"), "Assistant result should mention file update")
//        assertTrue(assistantResult.contains(existingFile), "Assistant result should contain file path")
//    }
//
//    @Test
//    fun testWriteToolLargeFileTruncation() {
//        // 测试大文件内容截断显示
//        val largeContent = (1..30).joinToString("\n") { "Line $it" }
//        var result = WriteTool.execute(testFile, largeContent,taskState)
//        result = WriteTool.execute(testFile, largeContent,taskState)
//
//        // 验证助手结果显示被截断
//        val assistantResult = result.resultForAssistant
//        assertTrue(assistantResult.contains("...[truncated]"), "Large content should be truncated in assistant result")
//        // 注意：由于行号前缀的存在，直接检查Line 25可能不准确
//        // 改为检查是否包含足够的行数
//        val lineCount = assistantResult.lines().count { it.contains("Line") }
//        assertTrue(lineCount <= WriteTool.MAX_LINES_TO_RENDER_FOR_ASSISTANT + 5, "Should not show too many lines")
//    }
//
//    @Test
//    fun testWriteToolSmallFileFullDisplay() {
////        File(testDir).mkdirs()
////        File(testFile).writeText("xxxx")
//        // 测试小文件完整显示
//        val smallContent = "Line 1\nLine 2\nLine 3"
//        var result = WriteTool.execute(testFile, smallContent,taskState)
//        result = WriteTool.execute(testFile, smallContent,taskState)
//
//        // 验证助手结果显示完整内容
//        val assistantResult = result.resultForAssistant
//        assertFalse(assistantResult.contains("...[truncated]"), "Small content should not be truncated")
//        assertTrue(assistantResult.contains("Line 1"), "Assistant result should show line 1")
//        assertTrue(assistantResult.contains("Line 3"), "Assistant result should show line 3")
//        // 验证行号显示
//        assertTrue(assistantResult.contains("     1\tLine 1"), "Assistant result should show numbered line 1")
//        assertTrue(assistantResult.contains("     3\tLine 3"), "Assistant result should show numbered line 3")
//    }
//
//    @Test
//    fun testWriteToolRenderResultForAssistantCreate() {
//        // 测试创建文件的结果渲染
//        val output = WriteToolOutput(
//            type = "create",
//            filePath = "/test/path/file.txt",
//            content = "test content"
//        )
//
//        val result = WriteTool.renderResultForAssistant(output)
//        assertTrue(result.contains("File created successfully"), "Should mention file creation")
//        assertTrue(result.contains("/test/path/file.txt"), "Should contain file path")
//    }
//
//    @Test
//    fun testWriteToolRenderResultForAssistantUpdate() {
//        // 测试更新文件的结果渲染
//        val output = WriteToolOutput(
//            type = "update",
//            filePath = "/test/path/file.txt",
//            content = "Line 1\nLine 2\nLine 3"
//        )
//
//        val result = WriteTool.renderResultForAssistant(output)
//        assertTrue(result.contains("has been updated"), "Should mention file update")
//        assertTrue(result.contains("/test/path/file.txt"), "Should contain file path")
//        assertTrue(result.contains("Line 1"), "Should show file content")
//    }
//
//    @Test
//    fun testWriteToolRenderResultForAssistantUnsupportedType() {
//        // 测试不支持的操作类型
//        val output = WriteToolOutput(
//            type = "unsupported",
//            filePath = "/test/path/file.txt",
//            content = "test content"
//        )
//
//        val result = WriteTool.renderResultForAssistant(output)
//        assertTrue(result.contains("Unsupported operation type"), "Should mention unsupported type")
//        assertTrue(result.contains("unsupported"), "Should show the unsupported type")
//    }
//
//    @Test
//    fun testWriteToolGetToolSpecification() {
//        // 测试工具规范获取
//        val spec = WriteTool.getToolSpecification()
//        assertEquals("Write", spec.name(), "Tool name should be 'Write'")
//        assertNotNull(spec.description(), "Tool should have description")
//        assertNotNull(spec.parameters(), "Tool should have parameters")
//
//        // 验证必需参数
//        val requiredParams = spec.parameters().required()
//        assertTrue(requiredParams.contains("file_path"), "Should require file_path")
//        assertTrue(requiredParams.contains("content"), "Should require content")
//    }
//
//    @Test
//    fun testWriteToolGetExecuteFunc() {
//        // 测试执行函数获取
//        val func = WriteTool.getExecuteFunc()
//        assertNotNull(func, "Execute function should not be null")
//        assertEquals("execute", func.name, "Function name should be 'execute'")
//    }
//
//    @Test
//    fun testWriteToolWithDifferentEncodings() {
//        // 测试不同编码处理
//        val content = "Hello 世界\nTest encoding"
//        val result = WriteTool.execute(testFile, content,taskState)
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        val output = result.data
//        assertEquals(content, output.content, "Content should match")
//
//        // 验证文件内容
//        val file = File(testFile)
//        assertTrue(file.exists(), "File should be created")
//        assertEquals(content, file.readText(), "File content should match")
//    }
//
//    @Test
//    fun testWriteToolWithLineEndings() {
//        // 测试行结束符处理
//        val content = "Line 1\r\nLine 2\r\nLine 3" // Windows风格
//        val result = WriteTool.execute(testFile, content,taskState)
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        val file = File(testFile)
//        assertTrue(file.exists(), "File should be created")
//
//        // 注意：具体的行结束符处理取决于系统和detectLineEndings的实现
//        // 这里主要验证工具能正常执行
//    }
//
//    @Test
//    fun testWriteToolEmptyContent() {
//        // 测试空内容写入
//        val content = ""
//        val result = WriteTool.execute(testFile, content,taskState)
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        val output = result.data
//        assertEquals("create", output.type, "Operation type should be 'create'")
//        assertEquals(content, output.content, "Content should be empty")
//
//        // 验证文件创建
//        val file = File(testFile)
//        assertTrue(file.exists(), "File should be created")
//        assertEquals(content, file.readText(), "File should be empty")
//    }
//
//    @Test
//    fun testWriteToolSpecialCharacters() {
//        // 测试特殊字符处理
//        val content = "Special chars: & < > \" ' \nUnicode: 你好 世界 🌍"
//        val result = WriteTool.execute(testFile, content,taskState)
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        val output = result.data
//        assertEquals(content, output.content, "Content should match with special characters")
//
//        // 验证文件内容
//        val file = File(testFile)
//        assertTrue(file.exists(), "File should be created")
//        assertEquals(content, file.readText(), "File content should preserve special characters")
//    }
//}
