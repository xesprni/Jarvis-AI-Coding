package com.miracle.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertTrue

class CommandUtilTest {

    @Test
    fun testRunWithRgCommand() = runTest {
        val rgPath = "${getJarvisBinDirectory()}/rg"
        val command = mutableListOf(rgPath, "--hidden", "--heading", "-n", "ROLE_USER")
        command.add(System.getProperty("user.dir"))

        withContext(Dispatchers.IO) {
            val outputLines = CommandUtil.run(command, timeoutMillis = 10_000).toList()
            // 验证输出不为空
            assertTrue(outputLines.isNotEmpty(), "Command output should not be empty")
            println(outputLines)
        }
    }

}