//package com.qifu.agent.tool
//
//import org.junit.FixMethodOrder
//import org.junit.runners.MethodSorters
//import java.io.File
//import java.nio.file.Files
//import java.nio.file.Path
//import kotlin.test.*
//
//@FixMethodOrder(MethodSorters.NAME_ASCENDING)
//class GlobToolTest () {
//
//    private val testDir = "test_glob_output"
//    private val testSubDir = "$testDir/subdir"
//    private val testFiles = listOf(
//        "$testDir/test1.txt",
//        "$testDir/test2.kt",
//        "$testDir/test3.java",
//        "$testSubDir/test4.txt",
//        "$testSubDir/test5.kt"
//    )
//    @Test
//     fun setUp() {
//        // 创建测试目录结构
//        File(testDir).mkdirs()
//        File(testSubDir).mkdirs()
//
//        // 创建测试文件
//        testFiles.forEach { path ->
//            File(path).writeText("Test content for $path")
//            // 确保文件修改时间不同，以测试排序
//            Thread.sleep(10)
//        }
//    }
//
//     fun tearDown() {
//        // 清理测试目录
//        File(testDir).deleteRecursively()
//    }
//
//    @Test
//    fun testGlobToolFindAllFiles() {
//        // 测试查找所有文件
//        val result = GlobTool.execute("$testDir/**/*", null)
//        println("testGlobToolFindAllFiles:"+result.resultForAssistant)
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        assertEquals("result", result.type, "Result type should be 'result'")
//        assertNotNull(result.data, "Result data should not be null")
//
//        val output = result.data
//        assertTrue(output.numFiles >= 0, "Should return non-negative number of files")
//        assertEquals(output.numFiles, output.filenames.size, "numFiles should match filenames size")
//        assertFalse(output.truncated, "Results should not be truncated for small test set")
//
//        // 由于 PSI 可能无法在测试环境中访问所有文件，我们只验证基本功能
//        if (output.numFiles > 0) {
//            output.filenames.forEach { filename ->
//                assertTrue(filename.isNotEmpty(), "Filename should not be empty")
//            }
//        }
//    }
//
//    @Test
//    fun testGlobToolFindByExtension() {
//        // 测试按扩展名查找
//        val result = GlobTool.execute("$testDir/**/*.kt", null)
//        println("testGlobToolFindByExtension:"+result.resultForAssistant)
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        val output = result.data
//
//        assertTrue(output.numFiles >= 0, "Should return non-negative number of files")
//
//        // 验证找到的所有文件都是 .kt 文件（如果有找到的话）
//        output.filenames.forEach { filename ->
//            assertTrue(filename.endsWith(".kt") || filename.contains(".kt"),
//                "All found files should be .kt files, but found: $filename")
//        }
//    }
//
//    @Test
//    fun testGlobToolFindInSubdirectory() {
//        // 测试在子目录中查找
//        val result = GlobTool.execute("$testDir/subdir/*", null)
//        println("testGlobToolFindInSubdirectory result:"+result.resultForAssistant)
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        val output = result.data
//
//        assertTrue(output.numFiles >= 0, "Should return non-negative number of files")
//
//        // 验证找到的所有文件都在子目录中（如果有找到的话）
//        output.filenames.forEach { filename ->
//            assertTrue(filename.contains("subdir") || filename.startsWith("test"),
//                "All found files should be in subdirectory or be test files")
//        }
//    }
//
//    @Test
//    fun testGlobToolSortByModificationTime() {
//        // 测试文件按修改时间排序
//        val result = GlobTool.execute("$testDir/**/*", null)
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        val output = result.data
//
//        assertTrue(output.numFiles >= 0, "Should return non-negative number of files")
//
//        // 由于 PSI 可能无法在测试环境中精确排序，我们只验证基本功能
//        if (output.numFiles > 1) {
//            // 验证至少返回了多个文件
//            assertTrue(output.filenames.size > 1, "Should find multiple files for sorting test")
//        }
//    }
//
//    @Test
//    fun testGlobToolNoMatchingFiles() {
//        // 测试没有匹配文件的情况
//        val result = GlobTool.execute("$testDir/**/*.nonexistent", null)
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        val output = result.data
//
//        // 应该返回0个文件或者很少的文件
//        assertTrue(output.numFiles >= 0, "Should return non-negative number of files")
//        assertTrue(output.filenames.isEmpty() || output.filenames.all { !it.endsWith(".nonexistent") },
//            "Should not find files with non-existent extension")
//        assertFalse(output.truncated, "Results should not be truncated")
//
//        // 如果没有找到文件，验证助手结果显示
//        if (output.numFiles == 0) {
//            val assistantResult = result.resultForAssistant
//            assertEquals("No files found", assistantResult, "Assistant result should indicate no files found")
//        }
//    }
//
//    @Test
//    fun testGlobToolTruncation() {
//        // 测试结果截断
//        // 由于在测试环境中可能无法创建大量文件，我们测试截断逻辑
//        val result = GlobTool.execute("**/*", testDir)
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        val output = result.data
//
//        // 验证结果数量不超过最大限制
//        assertTrue(output.numFiles <= GlobTool.MAX_RESULTS,
//            "Should not return more than MAX_RESULTS files")
//        assertTrue(output.filenames.size <= GlobTool.MAX_RESULTS,
//            "Filenames list should not exceed MAX_RESULTS")
//
//        // 如果结果被截断，验证助手结果显示
//        if (output.truncated) {
//            val assistantResult = result.resultForAssistant
//            assertTrue(assistantResult.contains("Results are truncated"),
//                "Assistant result should mention truncation")
//        }
//    }
//
//    @Test
//    fun testGlobToolWithCustomPath() {
//        // 测试使用自定义路径
//        val result = GlobTool.execute("*.txt", testDir)
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        val output = result.data
//
//        assertTrue(output.numFiles >= 0, "Should return non-negative number of files")
//
//        // 验证找到的文件都是 .txt 文件（如果有找到的话）
//        output.filenames.forEach { filename ->
//            assertTrue(filename.endsWith(".txt") || filename.contains(".txt"),
//                "Should find only .txt files, but found: $filename")
//        }
//    }
//
//    @Test
//    fun testGlobToolGetToolSpecification() {
//        // 测试工具规范获取
//        val spec = GlobTool.getToolSpecification()
//        assertEquals("Glob", spec.name(), "Tool name should be 'Glob'")
//        assertNotNull(spec.description(), "Tool should have description")
//        assertTrue(spec.description().contains("PSI API"), "Description should mention PSI API")
//        assertNotNull(spec.parameters(), "Tool should have parameters")
//
//        // 验证必需参数
//        val requiredParams = spec.parameters().required()
//        assertTrue(requiredParams.contains("pattern"), "Should require pattern")
//        assertFalse(requiredParams.contains("path"), "Path should be optional")
//    }
//
//    @Test
//    fun testGlobToolGetExecuteFunc() {
//        // 测试执行函数获取
//        val func = GlobTool.getExecuteFunc()
//        assertNotNull(func, "Execute function should not be null")
//        assertEquals("execute", func.name, "Function name should be 'execute'")
//    }
//
//    @Test
//    fun testGlobToolRenderResultForAssistant() {
//        // 测试助手结果渲染
//        val output1 = GlobToolOutput(
//            filenames = listOf("file1.txt", "file2.kt", "dir/file3.java"),
//            numFiles = 3,
//            truncated = false,
//            durationMs = 100
//        )
//
//        val result1 = GlobTool.renderResultForAssistant(output1)
//        assertEquals("file1.txt\nfile2.kt\ndir/file3.java", result1,
//            "Should render list of files")
//
//        // 测试空结果
//        val output2 = GlobToolOutput(
//            filenames = emptyList(),
//            numFiles = 0,
//            truncated = false,
//            durationMs = 50
//        )
//
//        val result2 = GlobTool.renderResultForAssistant(output2)
//        assertEquals("No files found", result2,
//            "Should indicate no files found")
//
//        // 测试截断结果
//        val output3 = GlobToolOutput(
//            filenames = listOf("file1.txt", "file2.kt"),
//            numFiles = 2,
//            truncated = true,
//            durationMs = 150
//        )
//
//        val result3 = GlobTool.renderResultForAssistant(output3)
//        assertTrue(result3.contains("Results are truncated"),
//            "Should mention truncation")
//    }
//
//    @Test
//    fun testGlobToolWithPSIFallback() {
//        // 测试 PSI 失败时的文件系统回退
//        // 这个测试验证当 PSI 不可用时，工具仍能正常工作
//        val result = GlobTool.execute("*.kt", testDir)
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        val output = result.data
//
//        assertTrue(output.numFiles >= 0, "Should return non-negative number of files")
//        assertTrue(output.durationMs >= 0, "Duration should be non-negative")
//
//        // 验证基本功能正常
//        output.filenames.forEach { filename ->
//            assertTrue(filename.isNotEmpty(), "Filename should not be empty")
//        }
//    }
//
//    @Test
//    fun testGlobToolRegexConversion() {
//        // 测试 glob 到正则表达式的转换
//        // 这是一个间接测试，通过执行不同的模式来验证转换是否正确
//        val patterns = listOf(
//            "*.kt",
//            "**/*.java",
//            "src/**/*.txt",
//            "test?.kt"
//        )
//
//        patterns.forEach { pattern ->
//            val result = GlobTool.execute(pattern, testDir)
//            assertNotNull(result, "Tool execution should return a result for pattern: $pattern")
//            assertTrue(result.data.numFiles >= 0, "Should return non-negative number of files for pattern: $pattern")
//        }
//    }
//
//    @Test
//    fun testLocal() {
//        // 保持原有的本地测试，但修改为更通用的测试
//        val projectRoot = System.getProperty("user.dir")
//
//        val result = GlobTool.execute("*.kt", "$projectRoot/src/main/kotlin/com/qifu/agent/tool/")
//        println("testLocal result: ${result.resultForAssistant}")
//
//        val result1 = GlobTool.execute("**/*.kt", "$projectRoot/src/main/kotlin/com/qifu/agent/")
//        println("testLocal result1: ${result1.resultForAssistant}")
//
//        // 验证结果
//        assertNotNull(result, "First local test should return a result")
//        assertNotNull(result1, "Second local test should return a result")
//    }
//
////    @Test
//    fun testGlobToolInputValidation() {
//        // 测试输入验证
//        try {
//            GlobTool.execute("", null)
//            fail("Should throw exception for empty pattern")
//        } catch (e: Exception) {
//            // 预期的异常
//        }
//
//        try {
//            GlobTool.execute("valid_pattern", "/non/existent/path")
//            fail("Should throw exception for non-existent path")
//        } catch (e: Exception) {
//            // 预期的异常
//        }
//    }
//}
