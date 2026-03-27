package com.qifu.utils

import com.qifu.external.LowcodeApiUtils
import com.qifu.services.QueryLLMOptions
import com.qifu.services.chatCompletion
import com.qifu.services.getChatModel
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.UserMessage
import io.mockk.every
import io.mockk.mockkObject
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ModelTest {

    @BeforeTest
    fun setUp() {
        mockkObject(LowcodeApiUtils) // 注意：Kt 后缀
        every { LowcodeApiUtils.getCommonHeaders() } returns mapOf(
            "MAC" to "00:11:22:33:44:55",
            "VERSION" to "1.0.0",
            "IDE_VERSION" to "2022.2",
            "EMAIL" to "test@example.com",
            "TOKEN" to "fake-token"
        )
    }

    @Test
    fun testChatWithUserMessages() = runTest {
        // 准备测试数据
        val messages = listOf(UserMessage("Who are you?"))
        val systemPrompt = listOf("You are Jarvis, Qifu Tech’s in-IDE AI assistant.")
        val options = QueryLLMOptions(model = "OPENAI_COMPATIBLE_360-qwen3-coder-480b-a35b")

        // 执行测试
        val result = chatCompletion(messages, systemPrompt, emptyList(), options)
        assertNotNull(result, "Chat should return a message")
        assertTrue(result.aiMessage() is AiMessage, "Result should be an AiMessage")
        print("${result.aiMessage().text()}")
    }

    @Test
    fun testGetChatModelWithInvalidModelId() = runTest {
        // 测试获取无效的聊天模型
        val invalidModelId = "INVALID_MODEL_ID"
        
        assertFailsWith<IllegalArgumentException>("Invalid model ID should throw an exception") {
            getChatModel(invalidModelId)
        }
    }
}
