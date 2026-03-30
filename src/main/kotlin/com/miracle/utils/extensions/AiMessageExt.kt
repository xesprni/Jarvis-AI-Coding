package com.miracle.utils.extensions

import dev.langchain4j.data.message.AiMessage

/**
 * 为 [AiMessage] 添加或覆盖指定属性，返回新的 [AiMessage] 实例。
 *
 * @param key 属性键
 * @param value 属性值
 * @return 包含新属性的 [AiMessage] 实例
 */
fun AiMessage.withAttribute(key: String, value: Any): AiMessage {
    return AiMessage.builder()
        .text(this.text())
        .thinking(this.thinking())
        .toolExecutionRequests(this.toolExecutionRequests())
        .attributes(this.attributes() + (key to value))
        .build()
}
