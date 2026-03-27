package com.miracle.external

import com.miracle.services.ModelConfig
import com.miracle.services.ModelProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LowcodeApiTest {

    @Test
    fun testModelConfigFactoryBuildsOpenAiCompatibleModel() {
        val config = ModelConfig.from(
            provider = ModelProvider.OPENAI_COMPATIBLE,
            model = "demo-model",
            contextTokens = 128_000,
            endpoint = "https://example.com/v1",
            apiKey = "token",
            alias = "Demo",
            supportsImages = true,
        )

        assertEquals("OPENAI_COMPATIBLE_demo-model", config.id)
        assertEquals("Demo", config.alias)
        assertTrue(config.supportsImages)
    }
}
