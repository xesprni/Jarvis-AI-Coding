package com.miracle.utils

import com.miracle.services.ModelConfig
import com.miracle.services.ModelProvider
import com.miracle.services.QueryLLMOptions
import com.miracle.services.getChatModel
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ModelTest {

    @Test
    fun testQueryOptionsPreservesSelectedModel() {
        val options = QueryLLMOptions(model = "OPENAI_COMPATIBLE_demo-model", streaming = true)

        assertEquals("OPENAI_COMPATIBLE_demo-model", options.model)
        assertTrue(options.streaming)
    }

    @Test
    fun testModelConfigAliasFallsBackToModelName() {
        val config = ModelConfig.from(
            provider = ModelProvider.OPENAI_COMPATIBLE,
            model = "demo-model",
            contextTokens = 32_000,
            endpoint = "https://example.com/v1",
        )

        assertEquals("demo-model", config.alias)
    }

    @Test
    fun testGetChatModelWithInvalidModelId() = runTest {
        val invalidModelId = "INVALID_MODEL_ID"

        assertFailsWith<IllegalArgumentException>("Invalid model ID should throw an exception") {
            getChatModel(invalidModelId)
        }
    }
}
