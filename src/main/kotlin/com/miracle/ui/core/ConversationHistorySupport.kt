package com.miracle.ui.core

import com.miracle.utils.Conversation

internal data class ConversationHistoryEntry(
    val id: String,
    val title: String,
    val updatedTime: Long,
)

internal object ConversationHistorySupport {

    fun buildEntries(
        conversations: List<Conversation>,
        currentConversationId: String?,
    ): List<ConversationHistoryEntry> {
        return conversations.asSequence()
            .filter { it.id != currentConversationId }
            .sortedWith(
                compareByDescending<Conversation> { it.updatedTime }
                    .thenByDescending { it.createdTime }
                    .thenBy { it.id }
            )
            .map { conversation ->
                ConversationHistoryEntry(
                    id = conversation.id,
                    title = conversation.title?.trim().takeUnless { it.isNullOrBlank() } ?: "新的会话",
                    updatedTime = conversation.updatedTime,
                )
            }
            .toList()
    }
}
