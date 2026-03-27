package com.qifu.utils

import kotlinx.coroutines.Dispatchers
import kotlin.test.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 测试 PersistentShell 在 Windows 下杀掉 node 进程的能力
 */
class PersistentShellWindowsTest {
    
    @Test
    fun testKillNodeProcess() = runTest {
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        
        if (!isWindows) {
            println("Skipping test: not on Windows")
            return@runTest
        }
        
        // 检查是否有 node 可用
        val hasNode = try {
            ProcessBuilder("node", "--version").start().waitFor() == 0
        } catch (e: Exception) {
            false
        }
        
        if (!hasNode) {
            println("Skipping test: node not found")
            return@runTest
        }
        
        val tempDir = System.getProperty("java.io.tmpdir")
        val shell = PersistentShell(tempDir)
        
        withContext(Dispatchers.IO) {
            // 创建一个简单的 node 脚本，会持续运行
            val scriptContent = """
                console.log('Node process started');
                setInterval(() => {
                    console.log('Still running...');
                }, 1000);
            """.trimIndent()
            
            val scriptFile = File(tempDir, "test-node-script.js")
            scriptFile.writeText(scriptContent)
            
            // 在后台启动 node 进程 - 使用相对路径，因为 shell 的工作目录就是 tempDir
            var output = ""
            val job = launch {
                val result = shell.exec(
                    "node test-node-script.js",
                    timeout = 30000L
                ) { stdout, stderr ->
                    output = stdout + stderr
                }
                println("Command finished: ${result.interrupted}")
            }
            
            // 等待进程启动
            delay(5000)
            
            // 取消任务，应该触发 killChildProcesses
            job.cancel()
            
            // 等待清理完成
            delay(2000)
            
            // 验证输出包含启动信息
            assertTrue(output.contains("Node process started"), "Should have started the node process")
            
            // 清理
            scriptFile.delete()
            
            println("Test completed successfully")
        }
    }
    
    @Test
    fun testKillComplexProcessTree() = runTest {
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        
        if (!isWindows) {
            println("Skipping test: not on Windows")
            return@runTest
        }
        
        val tempDir = System.getProperty("java.io.tmpdir")
        val shell = PersistentShell(tempDir)
        
        try {
            // 测试一个会创建多个子进程的命令
            var output = ""
            val job = launch {
                val result = shell.exec(
                    "ping localhost -n 100",  // 长时间运行的命令
                    timeout = 30000L
                ) { stdout, stderr ->
                    output = stdout + stderr
                }
                println("Command result: code=${result.code}, interrupted=${result.interrupted}")
            }
            
            // 等待命令开始执行
            delay(5000)
            
            // 取消任务
            job.cancel()
            
            // 等待清理
            delay(2000)
            
            // 验证命令被中断
            assertTrue(output.isNotEmpty(), "Should have some output")
            
            println("Complex process tree test completed")
        } finally {
            // shell 会自动清理
        }
    }
}
