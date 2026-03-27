package com.miracle.utils

data class TraceConversation(
    val id: String,
    val title: String,
)

data class Message(
    val id: String,
    val conversationId: String?,
    val input: String?,
    val output: String?,
)

data class AgentConversation(
    val id: String,
    val title: String,
    val source: String,
)

object TraceUtils {
    fun saveConversation(conversation: TraceConversation) {
        // Remote trace was removed.
    }

    suspend fun saveMessage(message: Message) {
        // Remote trace was removed.
    }

    fun saveAgentConversation(conversation: AgentConversation) {
        // Remote trace was removed.
    }
}
