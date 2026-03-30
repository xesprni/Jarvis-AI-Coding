package com.miracle.utils

/**
 * 追踪会话数据类
 */
data class TraceConversation(
    /** 会话 ID */
    val id: String,
    /** 会话标题 */
    val title: String,
)

/**
 * 消息数据类
 */
data class Message(
    /** 消息 ID */
    val id: String,
    /** 所属会话 ID */
    val conversationId: String?,
    /** 输入内容 */
    val input: String?,
    /** 输出内容 */
    val output: String?,
)

/**
 * Agent 会话数据类
 */
data class AgentConversation(
    /** 会话 ID */
    val id: String,
    /** 会话标题 */
    val title: String,
    /** 来源标识 */
    val source: String,
)

/**
 * 追踪工具类（当前为空实现，远程追踪已移除）
 */
object TraceUtils {
    /**
     * 保存会话追踪信息
     * @param conversation 会话数据
     */
    fun saveConversation(conversation: TraceConversation) {
        // Remote trace was removed.
    }

    /**
     * 保存消息追踪信息
     * @param message 消息数据
     */
    suspend fun saveMessage(message: Message) {
        // Remote trace was removed.
    }

    /**
     * 保存 Agent 会话追踪信息
     * @param conversation Agent 会话数据
     */
    fun saveAgentConversation(conversation: AgentConversation) {
        // Remote trace was removed.
    }
}
