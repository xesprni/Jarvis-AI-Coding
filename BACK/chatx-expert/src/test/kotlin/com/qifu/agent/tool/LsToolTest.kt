//package com.qifu.agent.tool
//
//import org.junit.FixMethodOrder
//import org.junit.runners.MethodSorters
//import java.io.File
//import kotlin.test.*
//
//@FixMethodOrder(MethodSorters.NAME_ASCENDING)
//class LsToolTest {
//
//    private val testDir = "test_output"
//    private val testSubDir = "$testDir/subdir"
//    private val testFile1 = "$testDir/test1.txt"
//    private val testFile2 = "$testDir/test2.txt"
//    private val testFile3 = "$testSubDir/test3.txt"
//    private val hiddenFile = "$testDir/.hidden"
//
//    @BeforeTest
//    fun setUp() {
//        // 创建测试目录结构
//        File(testDir).mkdirs()
//        File(testSubDir).mkdirs()
//
//        // 创建测试文件
//        File(testFile1).writeText("Test file 1")
//        File(testFile2).writeText("Test file 2")
//        File(testFile3).writeText("Test file 3")
//        File(hiddenFile).writeText("Hidden file")
//    }
//
//    @AfterTest
//    fun tearDown() {
//        // 清理测试目录
//        File(testDir).deleteRecursively()
//    }
//
//    @Test
//    fun testLsToolListDirectory() {
//        // 测试列出目录内容
//        val result = LsTool.execute(testDir)
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        assertEquals("result", result.type, "Result type should be 'result'")
//        assertNotNull(result.data, "Result data should not be null")
//
//        val output = result.data
//        assertEquals("list", output.type, "Operation type should be 'list'")
//        assertEquals(testDir, output.path, "Path should match")
//        assertTrue(output.isDirectory, "Should be recognized as directory")
//
//        // 验证文件列表
//        val files = output.files
//        assertTrue(files.contains("test1.txt"), "Should contain test1.txt")
//        assertTrue(files.contains("test2.txt"), "Should contain test2.txt")
//        assertTrue(files.contains("subdir/"), "Should contain subdir/")
//        assertFalse(files.contains(".hidden"), "Should not contain hidden file")
//
//        // 验证助手结果显示
//        val assistantResult = result.resultForAssistant
//        assertTrue(assistantResult.contains("Contents of directory"), "Assistant result should mention directory contents")
//        assertTrue(assistantResult.contains(testDir), "Assistant result should contain directory path")
//        println(result)
//        // 验证文件树格式
//        val expectedFormat = """
//            - $testDir/
//              - subdir/
//                - test3.txt
//              - test1.txt
//              - test2.txt
//        """.trimIndent()
//
//        // 移除前缀内容并整理格式以便比较
//        val actualTree = assistantResult.substringAfter("Contents of directory $testDir:").trim()
//        val normalizedActual = actualTree.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
//        val normalizedExpected = expectedFormat.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
//
//        assertEquals(normalizedExpected, normalizedActual, "File tree format should match expected format")
//    }
//
//    @Test
//    fun testLsToolNonExistentPath() {
//        // 测试不存在的路径
//        val nonExistentPath = "$testDir/non_existent"
//
//        // 验证异常抛出
//        val exception = assertFailsWith<ToolExecutionException> {
//            LsTool.execute(nonExistentPath)
//        }
//
//        assertTrue(exception.message?.contains("Path does not exist") == true,
//            "Exception message should mention path does not exist")
//    }
//
//    @Test
//    fun testLsToolFileInsteadOfDirectory() {
//        // 测试传入文件而不是目录
//        val result = LsTool.execute(testFile1)
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        val output = result.data
//        assertEquals("list", output.type, "Operation type should be 'list'")
//        assertEquals(testFile1, output.path, "Path should match")
//        assertFalse(output.isDirectory, "Should not be recognized as directory")
//        assertTrue(output.files.isEmpty(), "File list should be empty")
//
//        // 验证助手结果显示
//        val assistantResult = result.resultForAssistant
//        assertTrue(assistantResult.contains("Not a directory"), "Assistant result should mention not a directory")
//    }
//
//    @Test
//    fun testLsToolRenderResultForAssistantNotDirectory() {
//        // 测试非目录的结果渲染
//        val output = LsToolOutput(
//            type = "list",
//            path = "/test/path/file.txt",
//            files = emptyList(),
//            isDirectory = false
//        )
//
//        val result = LsTool.renderResultForAssistant(output)
//        assertTrue(result.contains("Not a directory"), "Should mention not a directory")
//        assertTrue(result.contains("/test/path/file.txt"), "Should contain file path")
//    }
//
//    @Test
//    fun testLsToolRenderResultForAssistantTruncation() {
//        // 测试大量文件的截断显示
//        val manyFiles = (1..30).map { "file$it.txt" }
//        val output = LsToolOutput(
//            type = "list",
//            path = "/test/path",
//            files = manyFiles,
//            isDirectory = true
//        )
//
//        val result = LsTool.renderResultForAssistant(output)
////        assertTrue(result.contains("...[truncated"), "Large file list should be truncated")
//
//        // 验证显示的文件数量
//        val fileCount = result.lines().count { it.contains("file") }
//        assertTrue(fileCount <= LsTool.MAX_LINES_TO_RENDER_FOR_ASSISTANT + 1, "Should not show too many files")
//    }
//
//    @Test
//    fun testLsToolGetToolSpecification() {
//        // 测试工具规范获取
//        val spec = LsTool.getToolSpecification()
//        assertEquals("Ls", spec.name(), "Tool name should be 'Ls'")
//        assertNotNull(spec.description(), "Tool should have description")
//        assertNotNull(spec.parameters(), "Tool should have parameters")
//
//        // 验证必需参数
//        val requiredParams = spec.parameters().required()
//        assertTrue(requiredParams.contains("path"), "Should require path")
//    }
//
//    @Test
//    fun testLsToolGetExecuteFunc() {
//        // 测试执行函数获取
//        val func = LsTool.getExecuteFunc()
//        assertNotNull(func, "Execute function should not be null")
//        assertEquals("execute", func.name, "Function name should be 'execute'")
//    }
//
//    @Test
//    fun testNestedDirectoryStructure() {
//        // 创建更复杂的嵌套目录结构进行测试
//        val nestedDir = "$testSubDir/nested"
//        val deepNestedDir = "$nestedDir/deep"
//        File(nestedDir).mkdirs()
//        File(deepNestedDir).mkdirs()
//
//        File("$nestedDir/nested1.txt").writeText("Nested file 1")
//        File("$nestedDir/nested2.txt").writeText("Nested file 2")
//        File("$deepNestedDir/deep1.txt").writeText("Deep nested file 1")
//
//        val result = LsTool.execute(testDir)
//        val assistantResult = result.resultForAssistant
//
//        // 验证嵌套目录结构的格式
//        val expectedFormat = """
//            - $testDir/
//              - subdir/
//                - nested/
//                  - deep/
//                    - deep1.txt
//                  - nested1.txt
//                  - nested2.txt
//                - test3.txt
//              - test1.txt
//              - test2.txt
//        """.trimIndent()
//
//        // 移除前缀内容并整理格式以便比较
//        val actualTree = assistantResult.substringAfter("Contents of directory $testDir:").trim()
//        val normalizedActual = actualTree.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
//        val normalizedExpected = expectedFormat.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
//
//        assertEquals(normalizedExpected, normalizedActual, "Nested directory structure format should match expected format")
//    }
//
//    @Test
//    fun testLocal(){
//        val path = "C:\\Users\\xiaozongliu-jk\\IdeaProjects\\lowcode\\chatx-expert\\src\\main\\kotlin\\com\\qifu\\"
//        val result = LsTool.execute(path)
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        assertEquals("result", result.type, "Result type should be 'result'")
//        assertNotNull(result.data, "Result data should not be null")
//
//        val output = result.data
//        assertEquals("list", output.type, "Operation type should be 'list'")
//        assertEquals(path, output.path, "Path should match")
//        assertTrue(output.isDirectory, "Should be recognized as directory")
//
//        // 验证助手结果显示
//        val assistantResult = result.resultForAssistant
//        println("==== 格式化输出结果 ====")
//        println(assistantResult)
//        println("==== 格式化输出结果结束 ====")
//
//        assertTrue(assistantResult.contains("Contents of directory"), "Assistant result should mention directory contents")
//        assertTrue(assistantResult.contains(path), "Assistant result should contain directory path")
//
//        // 验证文件树格式 - 检查缩进格式是否正确
//        val lines = assistantResult.lines()
//        val indentPattern = Regex("^(\\s*)- (.+)$")
//
//        var lastIndentLevel = -1
//        var lastIndentText = ""
//
//        for (line in lines.drop(1)) { // 跳过第一行 (根目录)
//            if (line.isBlank()) continue
//
//            val match = indentPattern.find(line)
//            if (match != null) {
//                val (indent, item) = match.destructured
//                val currentIndentLevel = indent.length / 2
//
//                // 验证缩进逻辑 - 目录项后面的条目应该缩进更多
//                if (lastIndentText.endsWith("/") && currentIndentLevel <= lastIndentLevel) {
//                    fail("Incorrect indentation: directory '${lastIndentText}' should have indented children, but found '$item' with same or less indentation")
//                }
//
//                // 验证缩进增量 - 应该每次增加2个空格
//                if (currentIndentLevel > lastIndentLevel && currentIndentLevel != lastIndentLevel + 1) {
//                    fail("Incorrect indentation increment: from level $lastIndentLevel to $currentIndentLevel")
//                }
//
//                lastIndentLevel = currentIndentLevel
//                lastIndentText = item
//            }
//        }
//    }
//}
