package com.qifu.utils

import kotlin.test.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest

class WindowsJobObjectTest {
    
    @Test
    fun testJobObjectCreation() {
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        
        if (!isWindows) {
            println("Skipping test: not on Windows")
            return
        }
        
        val job = WindowsJobObject.create()
        assertNotNull(job, "Job object should be created on Windows")
        job?.close()
    }
    
    @Test
    fun testProcessAssignment() {
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        
        if (!isWindows) {
            println("Skipping test: not on Windows")
            return
        }
        
        val job = WindowsJobObject.create()
        assertNotNull(job, "Job object should be created")
        
        // 创建一个简单的进程
        val process = ProcessBuilder("cmd", "/c", "ping localhost -n 10").start()
        
        val assigned = job?.assignProcess(process)
        assertTrue(assigned == true, "Process should be assigned to job")
        
        // 关闭 Job，进程应该被终止
        job?.close()
        
        // 等待一小段时间确保进程被终止
        Thread.sleep(1000)
        
        assertFalse(process.isAlive, "Process should be terminated after job is closed")
    }
    
    @Test
    fun testKillProcessTree() = runTest {
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        
        if (!isWindows) {
            println("Skipping test: not on Windows")
            return@runTest
        }
        
        val job = WindowsJobObject.create()
        assertNotNull(job, "Job object should be created")
        
        // 启动一个会创建子进程的命令
        // 使用 cmd /c start 创建子进程
        val process = ProcessBuilder(
            "cmd", "/c", 
            "ping localhost -n 100"  // 长时间运行的命令
        ).start()
        
        job?.assignProcess(process)
        
        // 等待进程启动
        delay(500)
        assertTrue(process.isAlive, "Process should be running")
        
        // 关闭 Job
        job?.close()
        
        // 等待进程被终止
        delay(1000)
        
        assertFalse(process.isAlive, "Process should be killed after job closed")
    }
}
