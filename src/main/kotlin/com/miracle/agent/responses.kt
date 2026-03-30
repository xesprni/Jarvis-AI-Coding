package com.miracle.agent

/**
 * AI 模型回复的工具相关响应消息构造器
 */
object AiResponse {

    /**
     * 生成工具不存在的错误提示
     *
     * @param toolName 请求的工具名称
     * @param availableToolNames 可用工具名称列表
     * @return 格式化的错误提示字符串
     */
    fun toolNotExists(toolName: String, availableToolNames: String) =
        """Error: The tool "$toolName" does not exist. Please choose one of the available tools: $availableToolNames."""

    /**
     * 生成用户拒绝操作时的提示
     *
     * @return 拒绝操作的提示字符串
     */
    fun toolDenied() = "The user denied this operation."

    /**
     * 生成用户取消操作时的提示
     *
     * @return 取消操作的提示字符串
     */
    fun toolCancelled() = "The user cancelled this operation."

    /**
     * 生成工具执行失败的错误提示
     *
     * @param error 错误信息
     * @return 格式化的错误提示字符串
     */
    fun toolError (error: String?) =
        "The tool execution failed with the following error:\n<error>\n${error}\n</error>"

}

/**
 * UI 展示用的响应消息构造器
 */
object UiResponse {

    /**
     * 生成自动审批请求次数达到上限时的提示
     *
     * @param maxApiRequestCount 最大允许的 API 请求数
     * @return 提示用户是否重置计数并继续的字符串
     */
    fun autoApprovalMaxReqReached(maxApiRequestCount: Int) =
        "Jarvis has auto-approved $maxApiRequestCount API requests. Would you like to reset the count and proceed with the task?"

    /**
     * 生成 API 连续失败时的提示
     *
     * @return 提示用户是否继续任务的字符串
     */
    fun consecutiveApiReqFailed() = "Jarvis is having trouble. Would you like to continue the task?"
}