package com.qifu.utils

import kotlin.test.Test

class StreamParserTest {

    @Test
    fun testParse1() {
        val invalidJson = """{"file_path": "D:\\idea_work\\lowcode\\lowcode-eval\\src\\main\\java\\com\\qihoo\\finance\\lowcode\\eval\\case3\\Case3Test.java" """
        val fields = JsonFieldExtractor.extractFields(invalidJson)
        print(fields)
    }
}