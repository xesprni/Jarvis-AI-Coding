package com.qifu.utils

import kotlin.test.*

class DiffTest {

    @Test
    fun testBasicDiffReplacement() {
        // 测试基本的字符串替换功能
        val filePath = "test.txt"
        val fileContents = """
            line 1
            line 2
            line 3
            line 4
            line 5
        """.trimIndent()
        
        val oldStr = "line 3"
        val newStr = "updated line 3"
        
        val hunks = getPatch(filePath, fileContents, oldStr, newStr)
        
        assertNotNull(hunks, "getPatch should return a list of hunks")
        assertEquals(1, hunks.size, "Should generate exactly one hunk for single line replacement")
        
        val hunk = hunks[0]
        assertEquals(1, hunk.oldStart, "Hunk should start from line 1 with default context")
        assertEquals(5, hunk.oldLines, "Hunk should include all 5 lines with context")
        assertEquals(1, hunk.newStart, "New hunk should start from line 1")
        assertEquals(5, hunk.newLines, "New hunk should include all 5 lines")
        
        // 验证hunk内容
        val lines = hunk.lines
        assertTrue(lines.any { it == " line 1" }, "Should contain context line 1")
        assertTrue(lines.any { it == " line 2" }, "Should contain context line 2")
        assertTrue(lines.any { it == "-line 3" }, "Should contain removed line")
        assertTrue(lines.any { it == "+updated line 3" }, "Should contain added line")
        assertTrue(lines.any { it == " line 4" }, "Should contain context line 4")
        assertTrue(lines.any { it == " line 5" }, "Should contain context line 5")
    }

    @Test
    fun testMultiLineReplacement() {
        // 测试多行替换
        val filePath = "test.txt"
        val fileContents = """
            start
            old line 1
            old line 2
            old line 3
            end
        """.trimIndent()
        
        val oldStr = """
            old line 1
            old line 2
            old line 3
        """.trimIndent()
        
        val newStr = """
            new line 1
            new line 2
        """.trimIndent()
        
        val hunks = getPatch(filePath, fileContents, oldStr, newStr)
        
        assertNotNull(hunks, "getPatch should return a list of hunks")
        assertEquals(1, hunks.size, "Should generate exactly one hunk for multi-line replacement")
        
        val hunk = hunks[0]
        val lines = hunk.lines
        
        // 验证删除的行
        assertTrue(lines.any { it == "-old line 1" }, "Should contain removed line 1")
        assertTrue(lines.any { it == "-old line 2" }, "Should contain removed line 2")
        assertTrue(lines.any { it == "-old line 3" }, "Should contain removed line 3")
        
        // 验证添加的行
        assertTrue(lines.any { it == "+new line 1" }, "Should contain added line 1")
        assertTrue(lines.any { it == "+new line 2" }, "Should contain added line 2")
    }

    @Test
    fun testNoChanges() {
        // 测试没有变化的情况
        val filePath = "test.txt"
        val fileContents = """
            line 1
            line 2
            line 3
        """.trimIndent()
        
        val oldStr = "nonexistent"
        val newStr = "replacement"
        
        val hunks = getPatch(filePath, fileContents, oldStr, newStr)
        
        assertNotNull(hunks, "getPatch should return a list of hunks")
        assertEquals(0, hunks.size, "Should generate no hunks when no matches found")
    }

    @Test
    fun testEmptyStrings() {
        // 测试空字符串情况
        val filePath = "test.txt"
        val fileContents = ""
        val oldStr = ""
        val newStr = "new content"
        
        val hunks = getPatch(filePath, fileContents, oldStr, newStr)
        
        assertNotNull(hunks, "getPatch should handle empty strings")
        // 空字符串替换可能会产生特殊行为，这里验证不会抛出异常
    }

    @Test
    fun testSpecialCharactersHandling() {
        // 测试特殊字符处理（& 和 $ 符号）
        val filePath = "test.txt"
        val fileContents = "line with & symbol and \$ dollar sign"
        val oldStr = "&"
        val newStr = "@"
        
        val hunks = getPatch(filePath, fileContents, oldStr, newStr)
        
        assertNotNull(hunks, "getPatch should handle special characters")
        if (hunks.isNotEmpty()) {
            val hunk = hunks[0]
            val lines = hunk.lines
            // 验证替换后的行包含正确的符号
            assertTrue(lines.any { it.contains("@") || it.contains("+line with @ symbol and \$ dollar sign") }, 
                      "Should contain replaced @ symbol or full line")
            // 验证原始行被正确标识为删除
            assertTrue(lines.any { it.startsWith("-") }, "Should contain removed line starting with -")
        }
    }

    @Test
    fun testMultipleOccurrences() {
        // 测试多次出现的替换
        val filePath = "test.txt"
        val fileContents = """
            hello world
            hello universe
            goodbye world
            hello galaxy
        """.trimIndent()
        
        val oldStr = "hello"
        val newStr = "hi"
        
        val hunks = getPatch(filePath, fileContents, oldStr, newStr)
        
        assertNotNull(hunks, "getPatch should handle multiple occurrences")
        assertTrue(hunks.size > 0, "Should generate hunks for multiple replacements")
        
        // 验证所有替换都正确应用
        val allLines = hunks.flatMap { it.lines }
        assertTrue(allLines.any { it.contains("+hi") }, "Should contain added hi")
        assertTrue(allLines.any { it.contains("-hello") }, "Should contain removed hello")
    }

    @Test
    fun testContextLinesParameter() {
        // 测试contextLines参数
        val filePath = "test.txt"
        val fileContents = """
            line 1
            line 2
            line 3
            line 4
            line 5
            line 6
            line 7
        """.trimIndent()
        
        val oldStr = "line 4"
        val newStr = "modified line 4"
        
        // 测试不同的contextLines值
        val hunksDefault = getPatch(filePath, fileContents, oldStr, newStr, 3)
        val hunksZero = getPatch(filePath, fileContents, oldStr, newStr, 0)
        
        assertNotNull(hunksDefault, "Should handle default context lines")
        assertNotNull(hunksZero, "Should handle zero context lines")
        
        if (hunksZero.isNotEmpty()) {
            val zeroContextHunk = hunksZero[0]
            // 零上下文应该只包含变更的行
            assertEquals(2, zeroContextHunk.lines.size, "Zero context should only include changed lines")
            assertTrue(zeroContextHunk.lines.any { it == "-line 4" }, "Should contain removed line")
            assertTrue(zeroContextHunk.lines.any { it == "+modified line 4" }, "Should contain added line")
        }
    }

    @Test
    fun testHunkDataClass() {
        // 测试Hunk数据类的基本功能
        val hunk = Hunk(
            oldStart = 1,
            oldLines = 3,
            newStart = 1,
            newLines = 3,
            lines = listOf(" line1", "-removed", "+added")
        )
        
        assertEquals(1, hunk.oldStart, "Hunk oldStart should be correct")
        assertEquals(3, hunk.oldLines, "Hunk oldLines should be correct")
        assertEquals(1, hunk.newStart, "Hunk newStart should be correct")
        assertEquals(3, hunk.newLines, "Hunk newLines should be correct")
        assertEquals(3, hunk.lines.size, "Hunk lines should be correct")
    }
}
