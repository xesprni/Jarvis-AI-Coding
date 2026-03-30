package com.miracle.utils

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.ContentType
import dev.langchain4j.data.message.TextContent
import dev.langchain4j.data.message.ToolExecutionResultMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.output.TokenUsage

/** Token 使用情况的属性键名 */
const val TOKEN_USAGE_KEY = "tokenUsage"


/**
 * 统计消息列表中的 Token 用量，从最新的 AiMessage 开始累加
 * @param messages 聊天消息列表
 * @return 估算的 Token 总数
 */
fun countTokens(messages: List<ChatMessage>): Int {
    var tokens = 0
    messages.asReversed().forEach { message ->
        if (message is AiMessage) {
            message.attribute(TOKEN_USAGE_KEY, Map::class.java)?.let { tokenUsageMap ->
                val tokenUsage = mapToTokenUsage(tokenUsageMap as Map<String, Int>)
                tokens += tokenUsage.totalTokenCount()
                return tokens
            }
        } else if (message is ToolExecutionResultMessage) {
            // 英文简单的按照 4 字符一个token 估算
            tokens += message.text().length / 4
        } else if (message is UserMessage) {
            // 正常不会走这里，除非最近的一条 AiMessage 没有 token 使用情况
            tokens += message.contents().filter { it.type() == ContentType.TEXT }.joinToString("\n").length / 4
        }
    }
    return tokens
}

/**
 * 统计消息列表中的缓存 Token 数量（当前未实现）
 * @param messages 聊天消息列表
 * @return 缓存的 Token 数量，当前始终返回 0
 */
fun countCachedTokens(messages: List<ChatMessage>): Int {
//    messages.asReversed().forEach { message ->
//        (message as? AiMessage)?.attribute(TOKEN_USAGE_KEY, TokenUsage::class.java)?.let { tokenUsage ->
//            (tokenUsage as? OpenAiTokenUsage)?.let { openAiTokenUsage ->
//                return openAiTokenUsage.inputTokensDetails()?.cachedTokens() ?: 0
//            }
//        }
//    }
    return 0
}


/**
 * 将 Map 转换为 TokenUsage 对象
 * @param map 包含 inputTokenCount、outputTokenCount、totalTokenCount 的 Map
 * @return TokenUsage 对象
 */
private fun mapToTokenUsage(map: Map<String, Int>): TokenUsage {
    return TokenUsage(
        map["inputTokenCount"] ?: 0,
        map["outputTokenCount"] ?: 0,
        map["totalTokenCount"] ?: 0
    )
}