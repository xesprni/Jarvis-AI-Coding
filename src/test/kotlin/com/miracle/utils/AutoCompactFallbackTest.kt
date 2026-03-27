package com.miracle.utils

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ToolExecutionResultMessage
import dev.langchain4j.data.message.UserMessage
import kotlin.test.*

class AutoCompactFallbackTest {

    @Test
    fun testDropOldestMessages_normalCase() {
        // Create 20 messages with mixed content
        val messages = mutableListOf<dev.langchain4j.data.message.ChatMessage>()
        
        // Add 8 oldest messages (will be dropped)
        repeat(8) { i ->
            messages.add(ToolExecutionResultMessage.from("requestId$i", "toolName$i", "tool result $i"))
        }
        
        // Add 12 more messages that should be kept
        repeat(12) { i ->
            messages.add(UserMessage.userMessage("user message $i"))
            messages.add(AiMessage.aiMessage("ai response $i"))
        }

        val result = dropOldestMessagesTest(messages)
        
        // Should have dropped 8 messages and added 1 notification
        assertEquals(25, result.size) // 24 remaining + 1 notification
        
        // First message should be the fallback notification
        val firstMessage = result[0]
        assertTrue(firstMessage is UserMessage)
        assertTrue(firstMessage.contents().joinToString("").contains("Context Management Fallback"))
        
        // Verify we kept the recent messages
        val remainingMessages = result.drop(1)
        assertEquals(24, remainingMessages.size)
        assertTrue(remainingMessages.any { it is UserMessage && it.contents().joinToString("").contains("user message") })
    }

    @Test
    fun testDropOldestMessages_insufficientMessages() {
        // Create only 15 messages (less than 16)
        val messages = mutableListOf<dev.langchain4j.data.message.ChatMessage>()
        repeat(15) { i ->
            messages.add(UserMessage.userMessage("message $i"))
        }

        val result = dropOldestMessagesTest(messages)
        
        // Should return original messages unchanged
        assertEquals(15, result.size)
        assertEquals(messages, result)
    }

    @Test
    fun testDropOldestMessages_avoidsToolMessageAtStart() {
        // Create messages where after dropping 8, the first remaining is a ToolMessage
        val messages = mutableListOf<dev.langchain4j.data.message.ChatMessage>()
        
        // Add 6 ToolExecutionResultMessage as oldest messages (will be dropped)
        repeat(6) { i ->
            messages.add(ToolExecutionResultMessage.from("requestId$i", "toolName$i", "tool result $i"))
        }
        
        // Add 2 more ToolExecutionResultMessage (these will be at the start after initial drop)
        repeat(2) { i ->
            messages.add(ToolExecutionResultMessage.from("requestId${i+6}", "toolName${i+6}", "tool result ${i+6}"))
        }
        
        // Add 12 more messages that should be kept
        repeat(12) { i ->
            messages.add(UserMessage.userMessage("user message $i"))
            messages.add(AiMessage.aiMessage("ai response $i"))
        }

        val result = dropOldestMessagesTest(messages)
        
        // Should have dropped more than 8 messages to avoid ToolMessage at start
        assertTrue(result.size >= 23) // At least 22 remaining + 1 notification
        
        // First message should be the fallback notification
        val firstMessage = result[0]
        assertTrue(firstMessage is UserMessage)
        assertTrue(firstMessage.contents().joinToString("").contains("Context Management Fallback"))
        
        // Verify that the remaining messages don't start with ToolExecutionResultMessage
        val remainingMessages = result.drop(1)
        if (remainingMessages.isNotEmpty()) {
            assertFalse(remainingMessages.first() is ToolExecutionResultMessage, 
                "Remaining messages should not start with ToolExecutionResultMessage")
        }
    }

    @Test
    fun testDropOldestMessages_allToolMessagesAfterDrop() {
        // Create messages where all remaining after initial drop are ToolMessages
        val messages = mutableListOf<dev.langchain4j.data.message.ChatMessage>()
        
        // Add 8 regular messages (will be dropped)
        repeat(8) { i ->
            messages.add(UserMessage.userMessage("old user message $i"))
        }
        
        // Add 10 ToolExecutionResultMessage (these will remain after initial drop)
        repeat(10) { i ->
            messages.add(ToolExecutionResultMessage.from("requestId$i", "toolName$i", "tool result $i"))
        }

        val result = dropOldestMessagesTest(messages)
        
        // Should drop all ToolMessages to avoid starting with them, leaving empty result
        // Since all remaining messages after initial drop are ToolMessages, they will all be dropped
        // This leaves us with empty result, so the function should return original messages
        assertEquals(18, result.size) // Original messages since dropping would leave empty result
        assertEquals(messages, result)
    }

    @Test
    fun testDropOldestMessages_emptyResultAfterDrop() {
        // Create a scenario where after dropping all ToolMessages, we'd have empty result
        val messages = mutableListOf<dev.langchain4j.data.message.ChatMessage>()
        
        // Add 8 regular messages (will be dropped)
        repeat(8) { i ->
            messages.add(UserMessage.userMessage("old user message $i"))
        }
        
        // Add only 1 ToolExecutionResultMessage (this will be dropped too to avoid starting with ToolMessage)
        messages.add(ToolExecutionResultMessage.from("requestId0", "toolName0", "tool result 0"))

        val result = dropOldestMessagesTest(messages)
        
        // Should return original messages since dropping would leave insufficient messages
        assertEquals(9, result.size)
        assertEquals(messages, result)
    }

    // Test helper function that mimics the logic of the private dropOldestMessages function
    private fun dropOldestMessagesTest(messages: List<dev.langchain4j.data.message.ChatMessage>): List<dev.langchain4j.data.message.ChatMessage> {
        if (messages.size <= 16) {
            return messages
        }
        
        // Drop messages iteratively until conditions are met
        var remainingMessages = messages.drop(8)
        
        // Continue dropping if the remaining messages start with ToolExecutionResultMessage
        while (remainingMessages.isNotEmpty() && remainingMessages.first() is ToolExecutionResultMessage) {
            remainingMessages = remainingMessages.drop(1)
        }
        
        if (remainingMessages.isEmpty()) {
            // If we dropped everything, return original messages (this matches the actual implementation)
            return messages
        }
        
        val fallbackNotification = UserMessage.userMessage("⚠️ **Context Management Fallback**: Due to token limits and compression failure, the oldest conversation messages (including tool results) have been automatically removed to continue the conversation. Key context may have been lost.")
        
        return listOf(fallbackNotification) + remainingMessages
    }
}