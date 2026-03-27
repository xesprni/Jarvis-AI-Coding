package com.miracle.utils

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class JsonTest {

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testSerialization() {
        val json = Json {
            encodeDefaults = true
            explicitNulls = false
            ignoreUnknownKeys = true
        }
        for (i in 1..100) {
            val conv = Conversation(title="chat1")
            println(json.encodeToString(conv))
        }

    }
}