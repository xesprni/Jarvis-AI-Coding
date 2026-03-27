package com.qifu.external

import com.qifu.utils.toAbsolutePath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test

class RipGrepUtilTest {

    @Test
    fun testGlob() = runTest {
        RipGrepUtil.install()
        // 准备测试数据
        val pattern = "*.xml"
//        val path = null
//        val path = System.getProperty("user.dir")
        val path = "./"
        val cwd = System.getProperty("user.dir")

        withContext(Dispatchers.IO) {
            println("Glob test result: ")
            RipGrepUtil.glob(pattern, path, cwd).collect { line ->
                println(line)
            }
        }
    }

    @Test
    fun testGrep() = runTest {
        RipGrepUtil.install()
        val pattern = "convert"
//        val path = toAbsolutePath("../lowcode-eval", System.getProperty("user.dir"))
//        val path = null
        val path = "."
        val cwd = toAbsolutePath("../lowcode-eval", System.getProperty("user.dir"))
        withContext(Dispatchers.IO) {
            println("Grep test result: ")
            RipGrepUtil.grep(pattern, path, cwd).collect { line ->
                println(line)
            }
        }
    }

}