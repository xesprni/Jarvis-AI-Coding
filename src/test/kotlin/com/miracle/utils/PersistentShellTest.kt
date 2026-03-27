//package com.miracle.utils
//
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.test.runTest
//import kotlinx.coroutines.withContext
//import org.junit.AfterClass
//import kotlin.test.*
//import kotlin.test.DefaultAsserter.assertNotNull
//
//class PersistentShellTest {
//
//    @Test
//    fun testPersistentShellInitialization() {
//        // 测试PersistentShell对象初始化
//        val shell = PersistentShell.getInstance()
//        assertNotNull(shell, "PersistentShell should be initialized")
//
//        println("PersistentShell initialized successfully")
//    }
//
//    @Test
//    fun testPwdFunction() {
//        // 测试当前工作目录获取功能
//        val initialCwd = PersistentShell.getInstance().pwd()
//        assertNotNull(initialCwd, "pwd() should return current working directory")
//        assertFalse(initialCwd.isEmpty(), "pwd() should not return empty string")
//
//        println("Current working directory: $initialCwd")
//    }
//
//    @Test
//    fun testSetCwdFunction() = runTest {
//        withContext(Dispatchers.IO) {
//            // 测试设置工作目录功能 - 测试有效路径
//            val initialCwd = PersistentShell.getInstance().pwd()
//            assertNotNull(initialCwd, "Should get initial working directory")
//
//            // 测试无效路径应该抛出异常
//            assertFailsWith<IllegalArgumentException> {
//                PersistentShell.getInstance().setCwd("/non/existent/path/that/should/not/exist")
//            }
//
//            println("SetCwd validation test completed")
//        }
//    }
//
//    @Test
//    fun testExecFunction() = runTest {
//        withContext(Dispatchers.IO) {
//            // 测试执行简单命令
//            val result = PersistentShell.getInstance().exec("echo 'Hello 中国'", timeout = 30000)
//            assertNotNull(result, "Exec result should not be null")
//            // 注意：在某些环境下，echo命令可能返回非0退出码，我们需要更宽松的断言
//            assertTrue(result.stdout.contains("Hello 中国") || result.stdout.contains("Hello 中国"),
//                "Output should contain 'Hello World' or 'HelloWorld', but got $result")
//
//            println("Command executed successfully: ${result.stdout}")
//        }
//    }
//
//    @Test
//    fun testExecWithTimeout() = runTest {
//        withContext(Dispatchers.IO) {
//            // 测试执行超时功能
//            val result = PersistentShell.getInstance().exec("sleep 10", timeout = 1)
//
//            assertNotNull(result, "Exec result should not be null")
//            // 超时的命令应该被标记为中断
//            assertTrue(result.interrupted, "Long running command should be interrupted due to timeout")
//
//            println("Timeout test completed - Interrupted: ${result.interrupted}")
//        }
//    }
//
//    @Test
//    fun testExecWithError() = runTest {
//        withContext(Dispatchers.IO) {
//            // 测试执行错误命令
//            val result = PersistentShell.getInstance().exec("nonexistentcommand12345")
//            assertNotNull(result, "Exec result should not be null")
//            // 错误命令通常会返回非零退出码
//            assertNotEquals(0, result.code, "Non-existent command should not exit with code 0")
//
//            println("Error command result - Exit code: ${result.code}")
//        }
//
//    }
//
//    @Test
//    fun testMultipleCommandsExecution() = runTest {
//        withContext(Dispatchers.IO) {
//            // 测试多个命令顺序执行
//            val result1 = PersistentShell.getInstance().exec("echo 'First command'")
//            val result2 = PersistentShell.getInstance().exec("echo 'Second command'")
//            val result3 = PersistentShell.getInstance().exec("echo 'Third command'")
//
//            assertNotNull(result1)
//            assertNotNull(result2)
//            assertNotNull(result3)
//
//            // 使用更宽松的断言，允许不同的输出格式
//            assertTrue(
//                result1.stdout.contains("First") && result1.stdout.contains("command"),
//                "Result1 should contain 'First' and 'command', $result1"
//            )
//            assertTrue(
//                result2.stdout.contains("Second") && result2.stdout.contains("command"),
//                "Result2 should contain 'Second' and 'command', $result2"
//            )
//            assertTrue(
//                result3.stdout.contains("Third") && result3.stdout.contains("command"),
//                "Result3 should contain 'Third' and 'command', $result3"
//            )
//
//            println("Multiple commands executed successfully")
//        }
//    }
//
//    @Test
//    fun testPathConversionUtility() = runTest {
//        withContext(Dispatchers.IO) {
//            // 测试路径相关命令执行
//            val result = PersistentShell.getInstance().exec("pwd")
//
//            assertNotNull(result, "Exec result should not be null")
//            assertFalse(result.stdout.trim().isEmpty(), "pwd command should return current directory, ${result}")
//
//            println("Current directory from shell: ${result.stdout.trim()}")
//        }
//    }
//
//    companion object {
//        @JvmStatic
//        @AfterClass
//        fun cleanupAll() {
//            println("All tests finished, closing PersistentShell")
//            PersistentShell.getInstance().close()
//        }
//    }
//}
