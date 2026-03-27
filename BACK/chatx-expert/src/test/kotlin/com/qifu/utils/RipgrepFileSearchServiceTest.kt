package com.qifu.utils

import com.intellij.openapi.vfs.LocalFileSystem
import com.qifu.external.RipGrepUtil
import io.mockk.*
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 测试 RipgrepFileSearchService 的性能和功能
 * 
 * 注意: 此测试需要 Project 实例，通常在IDE环境中运行
 * 这里主要测试 ripgrep 的基本功能和性能
 */
class RipgrepFileSearchServiceTest {

    @Test
    fun testRipgrepListFiles() = runTest {
        RipGrepUtil.install()
        
        val cwd = System.getProperty("user.dir")
        println("当前工作目录: $cwd")
        
        // 测试列出所有文件
        val startTime = System.currentTimeMillis()
        val files = mutableListOf<String>()
        
        RipGrepUtil.glob("*", null, cwd).collect { filePath ->
            files.add(filePath)
        }
        
        val duration = System.currentTimeMillis() - startTime
        
        println("ripgrep glob 找到 ${files.size} 个文件，耗时 ${duration}ms")
        assertTrue(files.isNotEmpty(), "应该找到至少一个文件")
        assertTrue(duration < 10000, "搜索时间应该少于10秒，实际耗时: ${duration}ms")
    }

    @Test
    fun testRipgrepSearchFiles() = runTest {
        RipGrepUtil.install()
        
        val cwd = System.getProperty("user.dir")
        
        // 测试搜索 Kotlin 文件
        val startTime = System.currentTimeMillis()
        val ktFiles = mutableListOf<String>()
        
        RipGrepUtil.glob("*.kt", null, cwd).collect { filePath ->
            ktFiles.add(filePath)
        }
        
        val duration = System.currentTimeMillis() - startTime
        
        println("ripgrep 搜索 '*.kt' 找到 ${ktFiles.size} 个文件，耗时 ${duration}ms")
        assertTrue(duration < 5000, "搜索时间应该少于5秒，实际耗时: ${duration}ms")
        
        // 验证所有文件都是 .kt 文件
        ktFiles.take(10).forEach { filePath ->
            assertTrue(
                filePath.endsWith(".kt"),
                "文件路径 $filePath 应该以 .kt 结尾"
            )
        }
    }

    @Test
    fun testRipgrepCaseInsensitive() = runTest {
        RipGrepUtil.install()
        
        val cwd = System.getProperty("user.dir")
        
        // 测试大小写不敏感搜索
        val pattern = "service"
        val startTime = System.currentTimeMillis()
        val files = mutableListOf<String>()
        
        RipGrepUtil.grep(
            pattern = pattern,
            path = null,
            cwd = cwd,
            outputMode = "files_with_matches",
            caseInsensitive = true
        ).collect { filePath ->
            files.add(filePath)
        }
        
        val duration = System.currentTimeMillis() - startTime
        
        println("ripgrep 大小写不敏感搜索 '$pattern' 找到 ${files.size} 个文件，耗时 ${duration}ms")
        assertTrue(duration < 5000, "搜索时间应该少于5秒，实际耗时: ${duration}ms")
    }

    @Test
    fun testRipgrepPerformanceComparison() = runTest {
        RipGrepUtil.install()
        
        val cwd = System.getProperty("user.dir")
        
        println("\n========== 性能对比测试 ==========")
        
        // 测试1: 列出所有文件
        val startTime1 = System.currentTimeMillis()
        var fileCount1 = 0
        RipGrepUtil.glob("*", null, cwd).collect { 
            fileCount1++
        }
        val duration1 = System.currentTimeMillis() - startTime1
        println("1. 列出所有文件: ${fileCount1} 个，耗时 ${duration1}ms")
        
        // 测试2: 搜索特定模式
        val startTime2 = System.currentTimeMillis()
        var fileCount2 = 0
        RipGrepUtil.glob("*test*", null, cwd).collect { 
            fileCount2++
        }
        val duration2 = System.currentTimeMillis() - startTime2
        println("2. 搜索包含'test'的文件: ${fileCount2} 个，耗时 ${duration2}ms")
        
        // 测试3: 搜索 Kotlin 文件
        val startTime3 = System.currentTimeMillis()
        var fileCount3 = 0
        RipGrepUtil.glob("*.kt", null, cwd).collect { 
            fileCount3++
        }
        val duration3 = System.currentTimeMillis() - startTime3
        println("3. 搜索 Kotlin 文件: ${fileCount3} 个，耗时 ${duration3}ms")
        
        // 测试4: 内容搜索
        val startTime4 = System.currentTimeMillis()
        var fileCount4 = 0
        RipGrepUtil.grep(
            pattern = "fun ",
            path = null,
            cwd = cwd,
            glob = "*.kt",
            outputMode = "files_with_matches",
            caseInsensitive = false
        ).collect { 
            fileCount4++
        }
        val duration4 = System.currentTimeMillis() - startTime4
        println("4. 内容搜索'fun '在 .kt 文件中: ${fileCount4} 个，耗时 ${duration4}ms")
        
        println("========== 性能对比测试完成 ==========\n")
        
        // 所有测试应该在合理时间内完成
        assertTrue(duration1 < 10000, "测试1耗时过长")
        assertTrue(duration2 < 5000, "测试2耗时过长")
        assertTrue(duration3 < 5000, "测试3耗时过长")
        assertTrue(duration4 < 5000, "测试4耗时过长")
    }

    @Test
    fun testDirectoryTraversalIncludesEmptyFolders() {
        // 创建临时测试目录结构
        val tempDir = File(System.getProperty("java.io.tmpdir"), "test-empty-folders-${System.currentTimeMillis()}")
        try {
            tempDir.mkdirs()
            
            // 创建包含文件的文件夹
            val folderWithFile = File(tempDir, "folder-with-file")
            folderWithFile.mkdirs()
            File(folderWithFile, "test.txt").writeText("test content")
            
            // 创建空文件夹
            val emptyFolder = File(tempDir, "empty-folder")
            emptyFolder.mkdirs()
            
            // 创建嵌套的空文件夹
            val parentFolder = File(tempDir, "parent")
            parentFolder.mkdirs()
            val nestedEmptyFolder = File(parentFolder, "nested-empty")
            nestedEmptyFolder.mkdirs()
            
            // 测试遍历逻辑（模拟修复后的代码）
            val folders = mutableSetOf<File>()
            
            fun traverseDirectories(dir: File) {
                if (!dir.isDirectory) return
                if (!dir.name.startsWith(".")) {
                    folders.add(dir)
                }
                dir.listFiles()?.forEach { child ->
                    if (child.isDirectory && !child.name.startsWith(".")) {
                        traverseDirectories(child)
                    }
                }
            }
            
            // 添加根目录
            folders.add(tempDir)
            
            // 遍历所有子目录
            tempDir.listFiles()?.forEach { child ->
                if (child.isDirectory && !child.name.startsWith(".")) {
                    traverseDirectories(child)
                }
            }
            
            // 验证结果
            val folderNames = folders.map { it.name }
            println("找到的文件夹: $folderNames")
            println("文件夹数量: ${folders.size}")
            
            assertTrue(folderNames.contains("folder-with-file"), "应该包含有文件的文件夹")
            assertTrue(folderNames.contains("empty-folder"), "应该包含空文件夹")
            assertTrue(folderNames.contains("parent"), "应该包含父文件夹")
            assertTrue(folderNames.contains("nested-empty"), "应该包含嵌套的空文件夹")
            assertTrue(folders.contains(tempDir), "应该包含根目录")
            
            // 验证总数：根目录 + folder-with-file + empty-folder + parent + nested-empty = 5
            assertTrue(folders.size >= 5, "应该至少找到5个文件夹，实际找到: ${folders.size}")
            
        } finally {
            // 清理临时目录
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testDirectoryTraversalWithFilter() {
        // 创建临时测试目录结构
        val tempDir = File(System.getProperty("java.io.tmpdir"), "test-search-pattern-${System.currentTimeMillis()}")
        try {
            tempDir.mkdirs()
            
            // 创建多个文件夹
            val serviceFolder = File(tempDir, "service")
            serviceFolder.mkdirs()
            
            val utilFolder = File(tempDir, "util")
            utilFolder.mkdirs()
            
            val testServiceFolder = File(tempDir, "test-service")
            testServiceFolder.mkdirs()
            
            // 测试过滤逻辑
            val searchPattern = "service"
            val folders = mutableSetOf<File>()
            
            fun traverseDirectories(dir: File) {
                if (!dir.isDirectory) return
                if (!dir.name.startsWith(".")) {
                    if (dir.absolutePath.contains(searchPattern, ignoreCase = true)) {
                        folders.add(dir)
                    }
                }
                dir.listFiles()?.forEach { child ->
                    if (child.isDirectory && !child.name.startsWith(".")) {
                        traverseDirectories(child)
                    }
                }
            }
            
            // 遍历所有子目录
            tempDir.listFiles()?.forEach { child ->
                if (child.isDirectory && !child.name.startsWith(".")) {
                    traverseDirectories(child)
                }
            }
            
            // 验证结果
            val folderNames = folders.map { it.name }
            println("包含'service'的文件夹: $folderNames")
            
            assertTrue(folderNames.contains("service"), "应该找到 service 文件夹")
            assertTrue(folderNames.contains("test-service"), "应该找到 test-service 文件夹")
            assertTrue(!folderNames.contains("util"), "不应该包含 util 文件夹")
            
        } finally {
            // 清理临时目录
            tempDir.deleteRecursively()
        }
    }

    @AfterTest
    fun cleanup() {
        unmockkAll()
    }
}
