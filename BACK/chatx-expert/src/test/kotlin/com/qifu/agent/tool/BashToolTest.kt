//package com.qifu.agent.tool
//
//import com.qifu.agent.TaskState
//import com.qifu.agent.chatMemoryProvider
//import org.junit.FixMethodOrder
//import org.junit.runners.MethodSorters
//import java.io.File
//import kotlin.collections.mutableMapOf
//import kotlin.test.*
//
//@FixMethodOrder(MethodSorters.NAME_ASCENDING)
//class BashToolTest {
//
//    private val testDir = "test_output"
//    val taskId = "BashToolTest"
//    val taskState = TaskState(taskId, chatMemoryProvider.get(taskId),mutableMapOf(), modelId = "default-model")
//    @BeforeTest
//    fun setUp() {
//        // 创建测试目录
//        File(testDir).mkdirs()
//    }
//
//    @AfterTest
//    fun tearDown() {
//        // 清理测试文件
//        try {
//            File(testDir).deleteRecursively()
//        } catch (e: Exception) {
//            // 忽略清理错误
//        }
//    }
//
//    @Test
//    fun testBashToolSimpleCommand() {
//        // 测试简单命令执行
//        val result = BashTool.execute("echo 'testBashToolSimpleCommand 测试验证'")
//        println("Result: $result")
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        assertEquals("result", result.type, "Result type should be 'result'")
//        assertNotNull(result.data, "Result data should not be null")
//
//        val output = result.data
//        assertTrue(output.stdout.contains("testBashToolSimpleCommand 测试验证"), "Output should contain 'testBashToolSimpleCommand 测试验证'")
//        assertEquals(0, output.exitCode, "Exit code should be 0 for successful command")
//        assertFalse(output.interrupted, "Command should not be interrupted")
//        assertTrue(output.stderr.isEmpty(), "Stderr should be empty for successful command")
//    }
//
////    @Test
//    fun testBashToolCommandWithError() {
//        // 测试执行失败的命令
//        val result = BashTool.execute("nonexistentcommand12345")
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        val output = result.data
//        assertNotEquals(0, output.exitCode, "Exit code should not be 0 for failed command")
//        assertTrue(output.stderr.isNotEmpty() || output.stdout.contains("not found") || output.stdout.contains("not recognized"),
//                  "Should have error information")
//    }
//
//    @Test
//    fun testBashToolTimeout() {
//        // 测试超时处理
//        val shortTimeout = 100L // 100ms
//        val result = BashTool.execute("sleep 1000", shortTimeout) // sleep 1 second
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        val output = result.data
//        // 由于超时可能被中断，检查是否有相关错误信息
//        assertTrue(output.interrupted || output.exitCode != 0,
//                  "Command should be interrupted or fail due to timeout")
//    }
//
//    @Test
//    fun testBashToolGetToolSpecification() {
//        // 测试工具规范获取
//        val spec = BashTool.getToolSpecification()
//        assertEquals("Bash", spec.name(), "Tool name should be 'Bash'")
//        assertNotNull(spec.description(), "Tool should have description")
//        assertNotNull(spec.parameters(), "Tool should have parameters")
//
//        // 验证必需参数
//        val requiredParams = spec.parameters().required()
//        assertTrue(requiredParams.contains("command"), "Should require command")
//    }
//
//    @Test
//    fun testBashToolGetExecuteFunc() {
//        // 测试执行函数获取
//        val func = BashTool.getExecuteFunc()
//        assertNotNull(func, "Execute function should not be null")
//        assertEquals("execute", func.name, "Function name should be 'execute'")
//    }
//
//    @Test
//    fun testBashToolRenderResultForAssistant() {
//        // 测试成功结果渲染
//        val output = BashToolOutput(
//            stdout = "Command output",
//            stdoutLines = 1,
//            stderr = "",
//            stderrLines = 0,
//            interrupted = false,
//            exitCode = 0
//        )
//
//        val result = BashTool.renderResultForAssistant(output)
//        assertEquals("Command output", result, "Should render stdout content")
//    }
//
//    @Test
//    fun testBashToolRenderResultForAssistantWithError() {
//        // 测试错误结果渲染
//        val output = BashToolOutput(
//            stdout = "Some output",
//            stdoutLines = 1,
//            stderr = "Error message",
//            stderrLines = 1,
//            interrupted = false,
//            exitCode = 1
//        )
//
//        val result = BashTool.renderResultForAssistant(output)
//        assertTrue(result.contains("Some output"), "Should contain stdout")
//        assertTrue(result.contains("Error message"), "Should contain stderr")
//    }
//
//    @Test
//    fun testBashToolRenderResultForAssistantInterrupted() {
//        // 测试中断结果渲染
//        val output = BashToolOutput(
//            stdout = "Partial output",
//            stdoutLines = 1,
//            stderr = "",
//            stderrLines = 0,
//            interrupted = true,
//            exitCode = 143
//        )
//
//        val result = BashTool.renderResultForAssistant(output)
//        assertTrue(result.contains("Partial output"), "Should contain stdout")
//        assertTrue(result.contains("aborted before completion"), "Should mention interruption")
//    }
//
//    @Test
//    fun testBashToolValidateInputMissingCommand() {
//        // 测试缺少命令参数
//        val input = kotlinx.serialization.json.buildJsonObject {}
//
//        assertFailsWith<MissingToolParameterException> {
//            BashTool.validateInput(input,taskState)
//        }
//    }
//
//    @Test
//    fun testBashToolValidateInputInvalidTimeout() {
//        // 测试无效超时时间
//        val input = kotlinx.serialization.json.buildJsonObject {
//            put("command", kotlinx.serialization.json.JsonPrimitive("echo test"))
//            put("timeout", kotlinx.serialization.json.JsonPrimitive("700000")) // 超过最大值
//        }
//
//        assertFailsWith<ToolParameterException> {
//            BashTool.validateInput(input,taskState)
//        }
//    }
//
//    @Test
//    fun testBashToolValidateInputBannedCommand() {
//        // 测试被禁止的命令
//        val input = kotlinx.serialization.json.buildJsonObject {
//            put("command", kotlinx.serialization.json.JsonPrimitive("rm -rf /"))
//        }
//
//        assertFailsWith<ToolExecutionException> {
//            BashTool.validateInput(input,taskState)
//        }
//    }
//
//    @Test
//    fun testBashToolValidateInputMultilineCommand() {
//        // 测试多行命令（应该被拒绝）
//        val input = kotlinx.serialization.json.buildJsonObject {
//            put("command", kotlinx.serialization.json.JsonPrimitive("echo line1\necho line2"))
//        }
//
//        assertFailsWith<ToolParameterException> {
//            BashTool.validateInput(input,taskState)
//        }
//    }
//
//    @Test
//    fun testBashToolValidateInputEmptyCommand() {
//        // 测试空命令
//        val input = kotlinx.serialization.json.buildJsonObject {
//            put("command", kotlinx.serialization.json.JsonPrimitive(""))
//        }
//
//        assertFailsWith<ToolParameterException> {
//            BashTool.validateInput(input,taskState)
//        }
//    }
//
////    @Test
////    fun testBashToolFormatOutput() {
////        // 测试输出格式化（通过执行一个产生大量输出的命令）
////        val longOutput = (1..1000).joinToString("\n") { "Line $it" }
////        val command = if (System.getProperty("os.name").lowercase().contains("win")) {
////            // Windows
////            "powershell -Command \"1..1000 | ForEach-Object { Write-Output \\\"Line `$_\\\" }\""
////        } else {
////            // Unix/Linux
////            "seq 1 1000 | sed 's/^/Line /'"
////        }
////
////        val result = BashTool.execute(command)
////
////        // 验证输出被适当格式化
////        assertNotNull(result, "Tool execution should return a result")
////        val output = result.data
////        assertTrue(output.stdoutLines > BashTool.MAX_RENDERED_LINES, "Should have many lines")
////
////        // 如果输出很长，应该被截断
////        if (output.stdout.length > BashTool.MAX_OUTPUT_LENGTH) {
////            assertTrue(output.stdout.contains("truncated"), "Long output should be truncated")
////        }
////    }
//
//    @Test
//    fun testBashToolExecuteWithDescription() {
//        // 测试带描述的执行
//        val result = BashTool.execute("echo test", BashTool.DEFAULT_TIMEOUT, "Test echo command")
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        val output = result.data
//        assertTrue(output.stdout.contains("test"), "Output should contain 'test'")
//        assertEquals(0, output.exitCode, "Exit code should be 0")
//    }
//
//    @Test
//    fun testBashToolExecuteDirectoryListing() {
//        // 测试目录列表命令
//        val command = if (System.getProperty("os.name").lowercase().contains("win")) {
//            "dir"
//        } else {
//            "ls"
//        }
//
//        val result = BashTool.execute(command)
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        val output = result.data
//        assertEquals(0, output.exitCode, "Directory listing should succeed")
//        assertFalse(output.interrupted, "Command should not be interrupted")
//    }
//
//    @Test
//    fun testBashToolExecuteWithValidTimeout() {
//        // 测试有效超时时间
//        val result = BashTool.execute("echo timeout_test", 5000L)
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        val output = result.data
//        assertTrue(output.stdout.contains("timeout_test"), "Output should contain test string")
//        assertEquals(0, output.exitCode, "Exit code should be 0")
//        assertFalse(output.interrupted, "Command should complete successfully")
//    }
//
//    @Test
//    fun testBashToolMultipleCommands() {
//        // 测试多个命令（用分号分隔）
//        val result = BashTool.execute("echo first; echo second")
//
//        // 验证结果
//        assertNotNull(result, "Tool execution should return a result")
//        val output = result.data
//        assertTrue(output.stdout.contains("first") && output.stdout.contains("second"),
//                  "Output should contain both commands' results")
//        assertEquals(0, output.exitCode, "Exit code should be 0")
//    }
//}
