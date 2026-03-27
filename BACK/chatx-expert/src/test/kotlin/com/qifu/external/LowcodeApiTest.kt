package com.qifu.external

import io.mockk.every
import io.mockk.mockkObject
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class LowcodeApiTest {

    @Test
    fun testGetJarvisModels() = runTest {
        // mock 单例对象
        mockkObject(LowcodeApiUtils) // 注意：Kt 后缀
        every { LowcodeApiUtils.getCommonHeaders() } returns mapOf(
            "MAC" to "00:11:22:33:44:55",
            "VERSION" to "1.0.0",
            "IDE_VERSION" to "2022.2",
            "EMAIL" to "test@example.com",
            "TOKEN" to "fake-token"
        )
        val models = LowcodeApi.getJarvisModels()
        assertTrue("Jarvis Models must contains qwen3-coder") {
            models.any { it.model == "qwen3-coder" && it.contextTokens == 128000 }
        }
//        assertTrue(models.any { it.modelId == "qwen3-coder" && it.contextLength == 128000}, "Must contain qwen3-coder")
    }

}
