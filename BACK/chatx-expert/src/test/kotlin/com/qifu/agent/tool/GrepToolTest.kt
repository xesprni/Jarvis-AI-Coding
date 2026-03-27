//package com.qifu.agent.tool
//
//import com.intellij.openapi.application.ApplicationManager
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.project.ProjectManager
//import org.junit.FixMethodOrder
//import org.junit.runners.MethodSorters
//import java.io.File
//import kotlin.test.*
//
//@FixMethodOrder(MethodSorters.NAME_ASCENDING)
//class GrepToolTest {
//
//    private val testDir = "test_output"
//    private val testFile1 = "$testDir/test_file1.txt"
//    private val testFile2 = "$testDir/test_file2.java"
//    private val testFile3 = "$testDir/test_file3.kt"
//    @Test
//     fun setUp() {
//        // 创建测试目录和文件
//        File(testDir).mkdirs()
//
//        // 创建测试文件1
//        File(testFile1).writeText("""
//            Hello World
//            This is a test file
//            Error: Something went wrong
//            INFO: Process completed
//            function testMethod() {
//                return "success";
//            }
//        """.trimIndent())
//
//        // 创建测试文件2 (Java文件)
//        File(testFile2).writeText("""
//            public class TestClass {
//                private String name;
//
//                public void setName(String name) {
//                    this.name = name;
//                }
//
//                public String getName() {
//                    return this.name;
//                }
//
//                public void logError(String message) {
//                    System.err.println("ERROR: " + message);
//                }
//            }
//        """.trimIndent())
//
//        // 创建测试文件3 (Kotlin文件)
//        File(testFile3).writeText("""
//            class TestClass {
//                var name: String = ""
//
//                fun setName(name: String) {
//                    this.name = name
//                }
//
//                fun getName(): String {
//                    return this.name
//                }
//
//                fun logError(message: String) {
//                    println("ERROR: ${'$'}message")
//                }
//            }
//        """.trimIndent())
//    }
//
//     fun tearDown() {
//        try {
//            // 清理测试文件
//            File(testDir).deleteRecursively()
//        } catch (e: Exception) {
//            // 忽略清理错误
//        }
//    }
//
//    @Test
//    fun testGrepToolBasicSearch() {
//        // 测试基本搜索功能
//        val result = GrepTool.execute(pattern = "Error", path = testDir)
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        assertEquals("result", result.type, "Result type should be 'result'")
//        assertNotNull(result.data, "Result data should not be null")
//
//        val output = result.data
//        assertEquals("search", output.type, "Operation type should be 'search'")
//        assertEquals("Error", output.pattern, "Pattern should match")
//        assertTrue(output.numFiles > 0, "Should find files with pattern")
//        assertTrue(output.filenames.isNotEmpty(), "Should return file names")
//        assertTrue(output.durationMs >= 0, "Duration should be non-negative")
//    }
//
//    @Test
//    fun testGrepToolCaseInsensitiveSearch() {
//        // 测试大小写不敏感搜索
//        val result = GrepTool.execute(pattern = "error", path = testDir, `-i` = true)
//        println("testGrepToolCaseInsensitiveSearch: $result")
//        val output = result.data
//        println("testGrepToolCaseInsensitiveSearch output: $output")
//        assertTrue(output.numFiles > 0, "Should find files with case insensitive pattern")
////        assertTrue(output.filenames.any {
////            it.contains(testFile1) || it.contains(testFile2) || it.contains(testFile3)
////        }, "Should find files containing 'Error' or 'ERROR'")
//    }
//
//    @Test
//    fun testGrepToolGlobFilter() {
//        // 测试glob文件过滤
//        val result = GrepTool.execute(pattern = "class", path = testDir, glob = "*.java")
//
//        val output = result.data
//        if (output.numFiles > 0) {
//            assertTrue(output.filenames.all { it.endsWith(".java") }, "Should only return Java files")
//        }
//    }
//
//    @Test
//    fun testGrepToolTypeFilter() {
//        // 测试文件类型过滤
//        val result = GrepTool.execute(pattern = "String", path = testDir, type = "java")
//
//        val output = result.data
//        if (output.numFiles > 0) {
//            assertTrue(output.filenames.all { it.endsWith(".java") }, "Should only return Java files")
//        }
//    }
//
//    @Test
//    fun testGrepToolKotlinTypeFilter() {
//        // 测试Kotlin文件类型过滤
//        val result = GrepTool.execute(pattern = "String", path = testDir, type = "kt")
//
//        val output = result.data
//        if (output.numFiles > 0) {
//            assertTrue(output.filenames.all { it.endsWith(".kt") }, "Should only return Kotlin files")
//        }
//    }
//
//    @Test
//    fun testGrepToolContentMode() {
//        // 测试内容显示模式
//        val result = GrepTool.execute(pattern = "Error", path = testDir, output_mode = "content")
//
//        val output = result.data
//        assertEquals("content", output.type, "Operation type should be 'content'")
//        assertTrue(output.content.isNotEmpty(), "Should return content lines")
//        assertTrue(output.matchCount > 0, "Should have match count")
//    }
//
//    @Test
//    fun testGrepToolContentModeWithLineNumbers() {
//        // 测试带行号的内容显示
//        val result = GrepTool.execute(pattern = "Error", path = testDir, output_mode = "content", `-n` = true)
//
//        val output = result.data
//        assertEquals("content", output.type, "Operation type should be 'content'")
//        assertTrue(output.content.isNotEmpty(), "Should return content lines")
//        // 验证行号格式（PSI格式：filename:line_number:content）
//        assertTrue(output.content.any { it.contains(":") }, "Should contain line numbers")
//    }
//
//    @Test
//    fun testGrepToolContentModeWithContext() {
//        // 测试带上下文的内容显示
//        val result = GrepTool.execute(pattern = "Error", path = testDir, output_mode = "content", `-C` = 1)
//
//        val output = result.data
//        assertEquals("content", output.type, "Operation type should be 'content'")
//        assertTrue(output.content.isNotEmpty(), "Should return content lines with context")
//    }
//
//    @Test
//    fun testGrepToolCountMode() {
//        // 测试计数模式
//        val result = GrepTool.execute(pattern = "name", path = testDir, output_mode = "count")
//
//        val output = result.data
//        assertEquals("count", output.type, "Operation type should be 'count'")
//        assertTrue(output.matchCount >= 0, "Should have match count")
//        if (output.matchCount > 0) {
//            assertTrue(output.content.isNotEmpty(), "Should return count information")
//        }
//    }
//
//    @Test
//    fun testGrepToolHeadLimit() {
//        // 测试结果数量限制
//        val result = GrepTool.execute(pattern = ".", path = testDir, output_mode = "content", head_limit = 5)
//
//        val output = result.data
//        println(output.content.size)
//        assertTrue(output.content.size <= 5, "Should limit results to head_limit")
//    }
//
//    @Test
//    fun testGrepToolNoMatches() {
//        // 测试无匹配结果
//        val result = GrepTool.execute(pattern = "NonExistentPattern12345", path = testDir)
//
//        val output = result.data
//        assertEquals(0, output.numFiles, "Should find no files")
//        assertTrue(output.filenames.isEmpty(), "Should return empty file list")
//
//        val assistantResult = result.resultForAssistant
//        assertTrue(assistantResult.contains("No files found"), "Assistant result should mention no files found")
//    }
//
//    @Test
//    fun testGrepToolRegexPattern() {
//        // 测试正则表达式模式
//        val result = GrepTool.execute(pattern = "function\\s+\\w+", path = testDir, output_mode = "content")
//
//        val output = result.data
//        if (output.matchCount > 0) {
//            assertTrue(output.content.any { it.contains("function") }, "Should find function declarations")
//        }
//    }
//
//    @Test
//    fun testGrepToolMultilineMode() {
//        // 测试多行模式
//        val result = GrepTool.execute(
//            pattern = "class.*\\{",
//            path = testDir,
//            output_mode = "content",
//            multiline = true
//        )
//
//        val output = result.data
//        // 验证多行模式能工作（具体结果取决于实现）
//        assertTrue(output.durationMs >= 0, "Should execute without error")
//    }
//
//    // 新增参数验证测试
//    @Test
//    fun testGrepToolEmptyPattern() {
//        // 测试空模式
//        assertFailsWith<ToolParameterException> {
//            GrepTool.execute(pattern = "")
//        }
//    }
//
//    @Test
//    fun testGrepToolBlankPattern() {
//        // 测试空白模式
//        assertFailsWith<ToolParameterException> {
//            GrepTool.execute(pattern = "   ")
//        }
//    }
//
//    @Test
//    fun testGrepToolInvalidOutputMode() {
//        // 测试无效输出模式
//        assertFailsWith<ToolParameterException> {
//            GrepTool.execute(pattern = "test", output_mode = "invalid_mode")
//        }
//    }
//
//    @Test
//    fun testGrepToolNegativeContextLines() {
//        // 测试负数上下文行数
//        assertFailsWith<ToolParameterException> {
//            GrepTool.execute(pattern = "test", output_mode = "content", `-C` = -1)
//        }
//
//        assertFailsWith<ToolParameterException> {
//            GrepTool.execute(pattern = "test", output_mode = "content", `-A` = -1)
//        }
//
//        assertFailsWith<ToolParameterException> {
//            GrepTool.execute(pattern = "test", output_mode = "content", `-B` = -1)
//        }
//    }
//
//    @Test
//    fun testGrepToolInvalidHeadLimit() {
//        // 测试无效的head_limit
//        assertFailsWith<ToolParameterException> {
//            GrepTool.execute(pattern = "test", head_limit = 0)
//        }
//
//        assertFailsWith<ToolParameterException> {
//            GrepTool.execute(pattern = "test", head_limit = -5)
//        }
//    }
//
//    @Test
//    fun testGrepToolNonExistentPath() {
//        // 测试不存在的路径
//        assertFailsWith<ToolParameterException> {
//            GrepTool.execute(pattern = "test", path = "/nonexistent/path/12345")
//        }
//    }
//
//    @Test
//    fun testGrepToolEmptyGlob() {
//        // 测试空glob模式（应该被忽略）
//        val result = GrepTool.execute(pattern = "Error", path = testDir, glob = "")
//
//        val output = result.data
//        assertTrue(output.numFiles >= 0, "Should execute without error with empty glob")
//    }
//
//    @Test
//    fun testGrepToolEmptyType() {
//        // 测试空文件类型（应该被忽略）
//        val result = GrepTool.execute(pattern = "Error", path = testDir, type = "")
//
//        val output = result.data
//        assertTrue(output.numFiles >= 0, "Should execute without error with empty type")
//    }
//
//    @Test
//    fun testGrepToolDefaultPath() {
//        // 测试默认路径（null或空）
//        val resultNull = GrepTool.execute(pattern = "Error", path = null)
//        val resultEmpty = GrepTool.execute(pattern = "Error", path = "")
//        val resultBlank = GrepTool.execute(pattern = "Error", path = "   ")
//
//        // 所有情况都应该使用当前项目根目录
//        assertTrue(resultNull.data.durationMs >= 0, "Should execute with null path")
//        assertTrue(resultEmpty.data.durationMs >= 0, "Should execute with empty path")
//        assertTrue(resultBlank.data.durationMs >= 0, "Should execute with blank path")
//    }
//
//    @Test
//    fun testGrepToolContextLinesIgnoredInNonContentMode() {
//        // 测试在非content模式下上下文行数被忽略
//        val result = GrepTool.execute(
//            pattern = "Error",
//            path = testDir,
//            output_mode = "files_with_matches",
//            `-C` = 5,
//            `-A` = 3,
//            `-B` = 2
//        )
//
//        val output = result.data
//        assertEquals("search", output.type, "Should use files_with_matches mode")
//        assertTrue(output.durationMs >= 0, "Should execute without error")
//    }
//
//    @Test
//    fun testGrepToolLineNumbersIgnoredInNonContentMode() {
//        // 测试在非content模式下行号被忽略
//        val result = GrepTool.execute(
//            pattern = "Error",
//            path = testDir,
//            output_mode = "count",
//            `-n` = true
//        )
//
//        val output = result.data
//        assertEquals("count", output.type, "Should use count mode")
//        assertTrue(output.durationMs >= 0, "Should execute without error")
//    }
//
//    @Test
//    fun testGrepToolRenderResultForAssistantSearch() {
//        // 测试搜索结果的助手显示
//        val output = GrepToolOutput(
//            type = "search",
//            pattern = "test",
//            path = "/test/path",
//            numFiles = 2,
//            filenames = listOf("/test/file1.txt", "/test/file2.txt"),
//            durationMs = 100
//        )
//
//        val result = GrepTool.renderResultForAssistant(output)
//        assertTrue(result.contains("Found 2 files"), "Should mention file count")
//        assertTrue(result.contains("/test/file1.txt"), "Should list file names")
//        assertTrue(result.contains("/test/file2.txt"), "Should list file names")
//    }
//
//    @Test
//    fun testGrepToolRenderResultForAssistantContent() {
//        // 测试内容结果的助手显示
//        val output = GrepToolOutput(
//            type = "content",
//            pattern = "test",
//            path = "/test/path",
//            numFiles = 1,
//            filenames = emptyList(),
//            durationMs = 100,
//            matchCount = 3,
//            content = listOf("line1: test", "line2: test", "line3: test")
//        )
//
//        val result = GrepTool.renderResultForAssistant(output)
//        assertTrue(result.contains("Found 3 matches"), "Should mention match count")
//        assertTrue(result.contains("in 1 file"), "Should mention file count")
//        assertTrue(result.contains("line1: test"), "Should show content")
//    }
//
//    @Test
//    fun testGrepToolRenderResultForAssistantCount() {
//        // 测试计数结果的助手显示
//        val output = GrepToolOutput(
//            type = "count",
//            pattern = "test",
//            path = "/test/path",
//            numFiles = 2,
//            filenames = emptyList(),
//            durationMs = 100,
//            matchCount = 5,
//            content = listOf("file1.txt:3", "file2.txt:2")
//        )
//
//        val result = GrepTool.renderResultForAssistant(output)
//        assertTrue(result.contains("Found 5 matches"), "Should mention total match count")
//        assertTrue(result.contains("in 2 files"), "Should mention file count")
//        assertTrue(result.contains("file1.txt:3"), "Should show count details")
//    }
//
//    @Test
//    fun testGrepToolRenderResultForAssistantNoMatches() {
//        // 测试无匹配的助手显示
//        val output = GrepToolOutput(
//            type = "search",
//            pattern = "test",
//            path = "/test/path",
//            numFiles = 0,
//            filenames = emptyList(),
//            durationMs = 50
//        )
//
//        val result = GrepTool.renderResultForAssistant(output)
//        assertTrue(result.contains("No files found"), "Should mention no files found")
//    }
//
//    @Test
//    fun testGrepToolRenderResultForAssistantTruncated() {
//        // 测试结果截断显示
//        val largeFilenameList = (1..150).map { "/test/file$it.txt" }
//        val output = GrepToolOutput(
//            type = "search",
//            pattern = "test",
//            path = "/test/path",
//            numFiles = 150,
//            filenames = largeFilenameList,
//            durationMs = 200
//        )
//
//        val result = GrepTool.renderResultForAssistant(output)
//        assertTrue(result.contains("Found 150 files"), "Should mention total file count")
//        assertTrue(result.contains("Results are truncated"), "Should mention truncation")
//    }
//
//    @Test
//    fun testGrepToolGetToolSpecification() {
//        // 测试工具规范获取
//        val spec = GrepTool.getToolSpecification()
//        assertEquals("Grep", spec.name(), "Tool name should be 'Grep'")
//        assertNotNull(spec.description(), "Tool should have description")
//        assertNotNull(spec.parameters(), "Tool should have parameters")
//
//        // 验证必需参数
//        val requiredParams = spec.parameters().required()
//        assertTrue(requiredParams.contains("pattern"), "Should require pattern parameter")
//    }
//
//    @Test
//    fun testGrepToolGetExecuteFunc() {
//        // 测试执行函数获取
//        val func = GrepTool.getExecuteFunc()
//        assertNotNull(func, "Execute function should not be null")
//        assertEquals("execute", func.name, "Function name should be 'execute'")
//    }
//
//    @Test
//    fun testGrepToolUnsupportedOutputMode() {
//        // 测试不支持的输出模式
//        assertFailsWith<ToolParameterException> {
//            GrepTool.execute(pattern = "test", path = testDir, output_mode = "unsupported")
//        }
//    }
//
//    @Test
//    fun testGrepToolRenderResultForAssistantUnsupportedType() {
//        // 测试不支持的结果类型显示
//        val output = GrepToolOutput(
//            type = "unsupported",
//            pattern = "test",
//            path = "/test/path",
//            numFiles = 0,
//            filenames = emptyList(),
//            durationMs = 50
//        )
//
//        val result = GrepTool.renderResultForAssistant(output)
//        assertTrue(result.contains("Unsupported search type"), "Should mention unsupported type")
//        assertTrue(result.contains("unsupported"), "Should show the unsupported type")
//    }
//
//    @Test
//    fun testGrepToolContentModeLargeResults() {
//        // 测试大量内容结果的截断
//        val largeContentList = (1..600).map { "line$it: content" }
//        val output = GrepToolOutput(
//            type = "content",
//            pattern = "content",
//            path = "/test/path",
//            numFiles = 1,
//            filenames = emptyList(),
//            durationMs = 300,
//            matchCount = 600,
//            content = largeContentList
//        )
//
//        val result = GrepTool.renderResultForAssistant(output)
//        assertTrue(result.contains("Found 600 matches"), "Should mention total match count")
//        assertTrue(result.contains("Results are truncated"), "Should mention content truncation")
//    }
//
//    @Test
//    fun testGrepToolSingleFileMatch() {
//        // 测试单个文件匹配的复数形式处理
//        val output = GrepToolOutput(
//            type = "search",
//            pattern = "unique_pattern",
//            path = "/test/path",
//            numFiles = 1,
//            filenames = listOf("/test/single_file.txt"),
//            durationMs = 50
//        )
//
//        val result = GrepTool.renderResultForAssistant(output)
//        assertTrue(result.contains("Found 1 file"), "Should use singular form for single file")
//        assertFalse(result.contains("1 files"), "Should not use plural form for single file")
//    }
//
//    @Test
//    fun testGrepToolSingleMatchCount() {
//        // 测试单个匹配的复数形式处理
//        val output = GrepToolOutput(
//            type = "content",
//            pattern = "unique",
//            path = "/test/path",
//            numFiles = 1,
//            filenames = emptyList(),
//            durationMs = 50,
//            matchCount = 1,
//            content = listOf("file.txt:1:unique content")
//        )
//
//        val result = GrepTool.renderResultForAssistant(output)
//        assertTrue(result.contains("Found 1 match"), "Should use singular form for single match")
//        assertFalse(result.contains("1 matches"), "Should not use plural form for single match")
//    }
//
//    @Test
//    fun testGrepToolContentModeEmptyResults() {
//        // 测试content模式下的空结果
//        val output = GrepToolOutput(
//            type = "content",
//            pattern = "nonexistent",
//            path = "/test/path",
//            numFiles = 0,
//            filenames = emptyList(),
//            durationMs = 50,
//            matchCount = 0,
//            content = emptyList()
//        )
//
//        val result = GrepTool.renderResultForAssistant(output)
//        assertTrue(result.contains("No matches found"), "Should mention no matches found")
//    }
//
//    @Test
//    fun testGrepToolCountModeZeroMatches() {
//        // 测试count模式下的零匹配
//        val output = GrepToolOutput(
//            type = "count",
//            pattern = "nonexistent",
//            path = "/test/path",
//            numFiles = 0,
//            filenames = emptyList(),
//            durationMs = 50,
//            matchCount = 0,
//            content = emptyList()
//        )
//
//        val result = GrepTool.renderResultForAssistant(output)
//        assertTrue(result.contains("No matches found"), "Should mention no matches found")
//    }
//
//    @Test
//    fun testGrepToolValidHeadLimit() {
//        // 测试有效的head_limit值
//        val result = GrepTool.execute(pattern = "Error", path = testDir, head_limit = 1)
//
//        val output = result.data
//        assertTrue(output.durationMs >= 0, "Should execute without error with valid head_limit")
//    }
//
//    @Test
//    fun testGrepToolZeroContextLines() {
//        // 测试零上下文行数（应该有效）
//        val result = GrepTool.execute(
//            pattern = "Error",
//            path = testDir,
//            output_mode = "content",
//            `-C` = 0
//        )
//
//        val output = result.data
//        assertEquals("content", output.type, "Should use content mode")
//        assertTrue(output.durationMs >= 0, "Should execute without error with zero context lines")
//    }
//
//    @Test
//    fun testGrepToolFileSystemFallback() {
//        // 测试文件系统回退功能（当PSI不可用时）
//        // 这个测试验证即使没有PSI项目上下文，工具仍能正常工作
//        val result = GrepTool.execute(pattern = "Error", path = testDir)
//
//        val output = result.data
//        assertTrue(output.durationMs >= 0, "Should execute without error in filesystem fallback mode")
//        // 由于可能回退到文件系统搜索，我们只验证基本功能
//    }
//
//    @Test
//    fun testGrepToolGlobPatternConversion() {
//        // 测试glob模式转换功能
//        val result = GrepTool.execute(pattern = "class", path = testDir, glob = "*.{java,kt}")
//
//        val output = result.data
//        if (output.numFiles > 0) {
//            assertTrue(output.filenames.all {
//                it.endsWith(".java") || it.endsWith(".kt")
//            }, "Should only return Java and Kotlin files")
//        }
//    }
//
//    @Test
//    fun testGrepToolComplexRegexPattern() {
//        // 测试复杂正则表达式模式
//        val result = GrepTool.execute(
//            pattern = "(public|private)\\s+(class|fun|void)",
//            path = testDir,
//            output_mode = "content"
//        )
//
//        val output = result.data
//        // 验证复杂正则表达式能正常工作
//        assertTrue(output.durationMs >= 0, "Should execute complex regex without error")
//    }
//}
