package com.miracle.ui.core

import com.miracle.utils.Conversation
import kotlin.test.Test
import kotlin.test.assertEquals

class ConversationHistorySupportTest {

    @Test
    fun testBuildEntriesFiltersCurrentConversationAndSortsByUpdatedTime() {
        val currentId = "current"
        val entries = ConversationHistorySupport.buildEntries(
            conversations = listOf(
                Conversation(id = currentId, title = "Current", createdTime = 100, updatedTime = 300),
                Conversation(id = "older", title = "Older", createdTime = 100, updatedTime = 200),
                Conversation(id = "newer", title = "Newer", createdTime = 100, updatedTime = 400),
            ),
            currentConversationId = currentId,
        )

        assertEquals(listOf("newer", "older"), entries.map { it.id })
    }

    @Test
    fun testBuildEntriesFallsBackToDefaultTitle() {
        val entries = ConversationHistorySupport.buildEntries(
            conversations = listOf(
                Conversation(id = "blank", title = "   ", createdTime = 100, updatedTime = 100),
                Conversation(id = "null", title = null, createdTime = 100, updatedTime = 90),
            ),
            currentConversationId = null,
        )

        assertEquals(listOf("新的会话", "新的会话"), entries.map { it.title })
    }
}
