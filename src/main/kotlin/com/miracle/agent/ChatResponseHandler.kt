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
 * 模型响应处理器，负责处理流式模型响应并将解析后的内容发送到 UI
 *
 * @param taskState 当前任务状态
 */
class ChatResponseHandler(
    private val taskState: TaskState,
) : StreamingChatResponseHandler {

    private companion object {
        /** 流式消息发送的最小间隔时间（毫秒） */
        const val SEND_INTERVAL = 100L
        val LOG = thisLogger()
    }

    /** 当前待发送的文本缓冲区 */
    private val textBuffer = StringBuilder()
    /** 累积的全部文本缓冲区，用于全量重新解析 */
    private val allTextBuffer = StringBuilder()
    /** 工具调用参数的累积缓冲区 */
    private val toolParamsBuffer = StringBuilder()
    /** 上一次发送消息的时间戳 */
    private var lastSendTime = 0L
    /** 上一次发送的 Segment 列表 */
    private var lastSegments: List<com.miracle.agent.parser.Segment> = emptyList()
    /** 是否存在尚未关闭的 partial 消息 */
    private var hasOpenPartialResponse = false

    /**
     * 处理流式工具调用片段，负责实时渲染工具调用进度
     *
     * @param partialToolCall 当前工具调用的片段数据
     * @param context 流式响应上下文
     */
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

    /**
     * 处理流式文本响应片段，将文本追加到缓冲区并按间隔发送
     *
     * @param partialResponse 当前文本响应片段
     * @param context 流式响应上下文
     */
    override fun onPartialResponse(partialResponse: PartialResponse, context: PartialResponseContext) {
        if (taskState.complete) context.streamingHandle().cancel()

        textBuffer.append(partialResponse.text())
        allTextBuffer.append(partialResponse.text())
        sendBuffer()
    }

    /**
     * 处理完整的模型响应，将 Token 用量信息附加到 AiMessage 并完成 Future
     *
     * @param completeResponse 完整的聊天响应
     */
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

    /**
     * 处理流式响应中的错误，将错误传递给 aiMessageFuture
     *
     * @param error 发生的异常
     */
    override fun onError(error: Throwable) {
        sendBuffer(forceSend = true)
        taskState.aiMessageFuture.completeExceptionally(error)
    }

    /**
     * 将文本缓冲区中的内容解析为 Segment 并发送到 UI
     *
     * @param forceSend 是否强制发送（忽略时间间隔限制），为 true 时同时关闭 partial 状态
     */
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
