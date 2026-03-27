package com.qifu.utils.extensions

import dev.langchain4j.data.message.AiMessage

fun AiMessage.withAttribute(key: String, value: Any): AiMessage {
    return AiMessage.builder()
        .text(this.text())
        .thinking(this.thinking())
        .toolExecutionRequests(this.toolExecutionRequests())
        .attributes(this.attributes() + (key to value))
        .build()
}