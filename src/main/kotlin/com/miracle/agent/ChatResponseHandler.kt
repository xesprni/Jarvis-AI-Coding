package com.miracle.agent

import com.intellij.openapi.diagnostic.thisLogger
import com.miracle.agent.parser.SseMessageParser
import com.miracle.agent.parser.TextSegment
import com.miracle.agent.tool.ToolExecutor
import com.miracle.utils.TOKEN_USAGE_KEY
import com.miracle.utils.extensions.withAttribute
import dev.langchain4j.model.chat.response.*
import dev.langchain4j.model.output.TokenUsage
import kotlinx.coroutines.runBlocking

/**
 * 模型响应处理
 */
class ChatResponseHandler(
    private val taskState: TaskState,
) : StreamingChatResponseHandler {

    private companion object {
        const val SEND_INTERVAL = 100L
        val LOG = thisLogger()
    }

    private val textBuffer = StringBuilder()
    private val allTextBuffer = StringBuilder()
    private val toolParamsBuffer = StringBuilder()
    private var lastSendTime = 0L
    private var lastSegments: List<com.miracle.agent.parser.Segment> = emptyList()
    private var hasOpenPartialResponse = false

    override fun onPartialToolCall(partialToolCall: PartialToolCall, context: PartialToolCallContext) {
        if (taskState.complete) context.streamingHandle().cancel()

        sendBuffer(true)
        // 只流式渲染第一个工具调用，后续工具调用等AiMessage结束后，才会返回第一个工具调用结束的消息，进行下一步的动作
        if (partialToolCall.index() > 0) {
            return
        }
        if (partialToolCall.id().isNullOrBlank() || partialToolCall.name().isNullOrBlank()) {
            return
        }
        toolParamsBuffer.append(partialToolCall.partialArguments())
        try {
            runBlocking {
                ToolExecutor.handlePartialBlock(partialToolCall.id(), partialToolCall.name(), toolParamsBuffer.toString(), taskState)
            }
        } catch (e: Exception) {
            // 这里如果终止流，还需要自己组装一份 AIMessage 返回给 aiMessageFuture
//            context.streamingHandle().cancel()
            LOG.warn("handlePartialBlock error: ${e.message}")
        }

    }

    override fun onPartialResponse(partialResponse: PartialResponse, context: PartialResponseContext) {
        if (taskState.complete) context.streamingHandle().cancel()

        textBuffer.append(partialResponse.text())
        allTextBuffer.append(partialResponse.text())
        sendBuffer()
    }

    override fun onCompleteResponse(completeResponse: ChatResponse) {
        sendBuffer(forceSend = true)
        var aiMessage = completeResponse.aiMessage()
        val tokenUsage = completeResponse.metadata().tokenUsage()
        tokenUsage?.let {
            val compactTokenUsage =
                TokenUsage(tokenUsage.inputTokenCount(), tokenUsage.outputTokenCount(), tokenUsage.totalTokenCount())
            aiMessage = aiMessage.withAttribute(
                TOKEN_USAGE_KEY, mapOf(
                    "inputTokenCount" to compactTokenUsage.inputTokenCount(),
                    "outputTokenCount" to compactTokenUsage.outputTokenCount(),
                    "totalTokenCount" to compactTokenUsage.totalTokenCount(),
                )
            ).withAttribute("id", completeResponse.metadata().id())
        }
        taskState.aiMessageFuture.complete(aiMessage)
    }

    override fun onError(error: Throwable) {
        sendBuffer(forceSend = true)
        taskState.aiMessageFuture.completeExceptionally(error)
    }

    private fun sendBuffer(forceSend: Boolean = false) {
        if (textBuffer.isEmpty()) {
            if (forceSend && hasOpenPartialResponse && lastSegments.isNotEmpty()) {
                taskState.emit!!(JarvisSay(id = taskState.curMessageId, type = AgentMessageType.TEXT, data = lastSegments, isPartial = false))
                hasOpenPartialResponse = false
            }
            return
        }
        if (!forceSend && System.currentTimeMillis() - lastSendTime < SEND_INTERVAL) return

        textBuffer.clear()

        // 使用完整累积文本重新解析，确保包含所有已完成的 segment（如 code block 之前的文本）
        // SseMessageParser 的内部 buffer 会在遇到代码块等结构时消耗已完成的内容，
        // 导致后续调用丢失先前的 segment。使用 allTextBuffer 全量解析避免此问题。
        val segments = SseMessageParser().parse(allTextBuffer.toString())
        if (segments.isEmpty()) {
            if (forceSend && hasOpenPartialResponse && lastSegments.isNotEmpty()) {
                taskState.emit!!(JarvisSay(id = taskState.curMessageId, type = AgentMessageType.TEXT, data = lastSegments, isPartial = false))
                hasOpenPartialResponse = false
            }
            return
        }

        segments.filter { it is TextSegment }.map { (it as TextSegment).eventId = taskState.curMessageId }
        taskState.emit!!(JarvisSay(id = taskState.curMessageId, type = AgentMessageType.TEXT, data = segments, isPartial = !forceSend))
        lastSegments = segments
        hasOpenPartialResponse = !forceSend
        lastSendTime = System.currentTimeMillis()
    }
}
