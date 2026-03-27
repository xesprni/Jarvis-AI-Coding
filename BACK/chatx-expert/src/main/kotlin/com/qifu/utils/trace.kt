package com.qifu.utils

import com.intellij.openapi.diagnostic.Logger
import com.qifu.external.LowcodeApi
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * 日志跟踪工具类
 * 提供消息保存接口，并通过内部线程异步处理消息
 */

@Serializable
data class TraceConversation(
    // 会话主键
    val conversationId: String,
    // 会话摘要
    var summary: String,
)

@Serializable
data class Message(
    // 消息主键
    val messageId: String? = null,
    // 会话主键
    val conversationId: String? = null,
    // 用户提问
    var query: String? = null,
    // 模型返回的答案
    var answer: String? = null
)

@Serializable
data class AgentConversation(
    // 会话主键
    val taskId: String,
    // 会话摘要
    val convTitle: String,
    // 来源
    val source: String,
)

/**
 * conversation采用同步处理
 * message采用异步处理
 */
object TraceUtils {

    private val LOG = Logger.getInstance(TraceUtils::class.java)
    private val messageQueue = Channel<Message>(capacity = Channel.UNLIMITED)
    // TODO map需要设置过期清除时间，避免内存泄漏
    private val conversationMap = ConcurrentHashMap<String, TraceConversation>()

    // 初始化单线程池用于异步处理消息和conversation
    private val SCOPE = CoroutineScope(SupervisorJob() + Executors.newSingleThreadExecutor {
        Thread(it, "TraceUtils-Processor").apply {
            isDaemon = true // 设置为守护线程，避免阻塞程序退出
        }
    }.asCoroutineDispatcher())

    init {
        // 启动消息处理线程
        startMessageConsumer()
    }

    /**
     * 保存消息到队列
     * @param conversation 要保存的会话内容
     */
    fun saveConversation(conversation: TraceConversation) {
        conversationMap[conversation.conversationId] = conversation
        SCOPE.launch {
            try {
                retryUtils.retry(times = 3) {
                    LowcodeApi.insertConversationInfo(conversation)
                }
            } catch (e: Exception) {
                LOG.warn("TraceUtils: 保存会话失败: $conversation", e)
            }
        }
    }

    suspend fun saveMessage(message: Message) {
        messageQueue.send(message)
    }

    /**
     * 启动处理器，循环从队列中获取消息并处理
     */
    private fun startMessageConsumer() {
        SCOPE.launch {
            for (dto in messageQueue) {
                try {
                    retryUtils.retry(times = 3) {
                        LowcodeApi.insertMessageInfo(dto)
                    }
                } catch (e: Exception) {
                    LOG.warn("TraceUtils: 处理消息失败: $dto", e)
                }
            }
        }
    }

    /**
     * 保存agentConversation
     */
    fun saveAgentConversation(conversation: AgentConversation) {
        SCOPE.launch {
            retryUtils.retry(times = 3) {
                LowcodeApi.addAgentConversation(conversation)
            }
        }
    }

}