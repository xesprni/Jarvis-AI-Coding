package com.miracle.agent

import com.miracle.agent.parser.Segment

/**
 * Agent 消息类型枚举，定义了 Agent 与 UI 之间通信的消息类型
 */
enum class AgentMessageType {
    REASONING,              // 推理内容
    TEXT,                   // 文本内容
    ERROR,                   // 错误提示
    TOOL,                   // 工具
    FOLLOWUP,               // 模型追问
    AUTO_APPROVAL_MAX_REQ_REACHED,  // 本轮自动API请求次数达到上线
    API_REQ_FAILED,         // API连续失败达到阈值
}

/**
 * Agent 消息基类，定义了 Agent 与 UI 之间通信的消息结构
 *
 * @param id 消息唯一标识
 * @param type 消息类型
 * @param data 需要渲染的 Segment 列表
 * @param isPartial 消息是否已经完整（流式渲染）
 */
sealed class AgentMessage (
    open val id: String,
    open val type: AgentMessageType,
    open val data: List<Segment>,        // 需要渲染的Segment
    open val isPartial: Boolean = false, // 消息是否已经完整（流式渲染）
)

/**
 * Jarvis向用户询问授权
 * isPartial = false 时，展示取消按钮；isPartial = true 时，展示Approve/Reject按钮
 */
data class JarvisAsk(
    override val id: String,
    override val type: AgentMessageType,
    override val data: List<Segment>,        // 需要渲染的Segment
    override val isPartial: Boolean = false, // 消息是否已经完整（流式渲染）
) : AgentMessage(id, type, data, isPartial)


/**
 * Jarvis向用户发送消息，不需要用户回复
 */
data class JarvisSay(
    override val id: String,
    override val type: AgentMessageType,
    override val data: List<Segment>,        // 需要渲染的Segment
    override val isPartial: Boolean = false, // 消息是否已经完整（流式渲染）
) : AgentMessage(id, type, data, isPartial)



/**
 * 用户对 Jarvis 询问的回答
 *
 * @param id 关联的询问消息 ID
 * @param type 回答类型
 * @param message 用户输入的附加文字信息
 */
data class AskResponse(
    val id: String,
    val type: ResponseType,
    val message: String? = null,
) {
    /**
     * 用户回答类型
     */
    enum class ResponseType {
        YES,
        NO,
        MESSAGE, // 用户输入文字返回
    }
}
