//package com.miracle.agent.tool
//
//import kotlinx.coroutines.test.runTest
//import kotlin.test.*
//
//class ToolInvokerTest {
//
//    @Test
//    fun testCallToolWithValidStringParameter() = runTest {
//        // 测试调用具有字符串参数的工具
//        val jsonArgs = """{"file_path": "README.md"}"""
//        val result = ToolExecutor.callTool("Read", jsonArgs)
//        assertNotNull(result, "Tool invocation should return a result")
//        println("Read tool result: $result")
//    }
//
//    @Test
//    fun testCallToolWithMultipleParameters() = runTest {
//        // 测试调用具有多个参数的工具
//        val jsonArgs = """{"file_path": "build.gradle.kts", "offset": 10, "limit": 100}"""
//        val result = ToolExecutor.callTool("Read", jsonArgs)
//        assertNotNull(result, "Tool invocation with multiple parameters should return a result")
//        println("Read tool result with multiple params: $result")
//    }
//
//    @Test
//    fun testCallToolWithNonExistentTool() = runTest {
//        // 测试调用不存在的工具应该抛出异常
//        val jsonArgs = """{"param": "value"}"""
//        assertFailsWith<ToolNotFoundException> {
//            ToolExecutor.callTool("NonExistentTool", jsonArgs)
//        }.also {
//            assertEquals(it.message?.contains("Tool not found"), true)
//        }
//    }
//
//    @Test
//    fun testCallToolWithMissingParameter() = runTest {
//        // 测试调用工具时缺少必需参数
//        // 根据要求，应当传入一个空json，因为Read tool只有一个必填参数file_path
//        val jsonArgs = """{}""" // 空JSON，缺少必需的file_path参数
//        assertFailsWith<ToolParameterException> {
//            ToolExecutor.callTool("Read", jsonArgs)
//        }.also {
//            assertEquals(it.message?.contains("Missing required parameter: file_path"), true)
//        }
//    }
//
//    @Test
//    fun testCallToolWithInvalidJson() = runTest {
//        // 测试调用工具时传入无效JSON
//        val invalidJsonArgs = """{"invalid": json}""" // 无效的JSON格式
//        assertFailsWith<Exception> {
//            ToolExecutor.callTool("Read", invalidJsonArgs)
//        }
//    }
//
//    @Test
//    fun testCallListTools() = runTest {
//        val json = """{"file_path": "D:/idea_work/lowcode/lowcode-eval/pom.xml", "edits": "[{\"old_string\": \"        <msf.version>3.8.1</msf.version>\", \"new_string\": \"        <msf.version>3.8.0</msf.version>\"}, {\"old_string\": \"        <lombok.version>1.18.30</lombok.version>\", \"new_string\": \"        <lombok.version>1.18.31</lombok.version>\"}]"}"""
//        ToolExecutor.callTool("MultiEdit", json)
//    }
//
//    @Test
//    fun testCallToolWithUnsupportedParameterType() = runTest {
//        // 测试调用工具时传入不支持的参数类型
//        // 可以尝试offset传一个非数值
//        val jsonArgs = """{"file_path": "build.gradle.kts", "offset": "not_a_number"}"""
//        assertFailsWith<ToolParameterException> {
//            ToolExecutor.callTool("Read", jsonArgs)
//        }.also {
//            assertEquals(it.message?.contains("Invalid parameter value for param"), true)
//        }
//    }
//}
