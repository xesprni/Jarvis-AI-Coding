package com.qifu.agent

import com.intellij.openapi.diagnostic.thisLogger
import com.qifu.agent.parser.SseMessageParser
import com.qifu.agent.parser.TextSegment
import com.qifu.agent.tool.ToolExecutor
import com.qifu.utils.TOKEN_USAGE_KEY
import com.qifu.utils.extensions.withAttribute
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

    private val parser = SseMessageParser()
    private val textBuffer = StringBuilder()
    private val allTextBuffer = StringBuilder()
    private val toolParamsBuffer = StringBuilder()
    private var lastSendTime = 0L

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
        if (textBuffer.isEmpty()) return
        if (!forceSend && System.currentTimeMillis() - lastSendTime < SEND_INTERVAL) return

        val segments = parser.parse(textBuffer.toString())
        textBuffer.clear()
        if (segments.isEmpty()) return

        segments.filter { it is TextSegment }.map { (it as TextSegment).eventId = taskState.curMessageId }
        taskState.emit!!(JarvisSay(id = "AI_STREAMING_RESP", type = AgentMessageType.TEXT, data = segments, isPartial = !forceSend))
        lastSendTime = System.currentTimeMillis()
    }
}