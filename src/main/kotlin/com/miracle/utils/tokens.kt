package com.miracle.utils

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.ContentType
import dev.langchain4j.data.message.TextContent
import dev.langchain4j.data.message.ToolExecutionResultMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.output.TokenUsage

const val TOKEN_USAGE_KEY = "tokenUsage"


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


private fun mapToTokenUsage(map: Map<String, Int>): TokenUsage {
    return TokenUsage(
        map["inputTokenCount"] ?: 0,
        map["outputTokenCount"] ?: 0,
        map["totalTokenCount"] ?: 0
    )
}