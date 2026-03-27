package com.miracle.utils

import com.intellij.openapi.project.Project
import com.miracle.services.ModelConfig

data class AgentTraceLog(
    val convId: String? = null,
    val taskId: String? = null,
    val messageId: String? = null,
    val modelId: String? = null,
    val content: String? = null,
    val toolName: String? = null,
    var subAgentType: String? = null,
    var mcpServer: String? = null,
    var mcpTool: String? = null,
)

object AgentTraceUtils {
    object Status {
        const val SUCCESS = 0
        const val CANCELLED = 1
        const val ERROR = 2
    }

    private fun buildLog(
        convId: String?,
        taskId: String?,
        messageId: String?,
        modelConfig: ModelConfig?,
        content: String?,
        toolName: String? = null,
        subAgentType: String? = null,
    ): AgentTraceLog {
        return AgentTraceLog(
            convId = convId,
            taskId = taskId,
            messageId = messageId,
            modelId = modelConfig?.id,
            content = content,
            toolName = toolName,
            subAgentType = subAgentType,
        )
    }

    fun saveTraceLog(traceLog: AgentTraceLog) {
        // Remote trace was removed.
    }

    fun saveUserRequest(
        convId: String?,
        taskId: String?,
        messageId: String?,
        modelConfig: ModelConfig,
        content: String?,
        project: Project? = null,
    ): AgentTraceLog = buildLog(convId, taskId, messageId, modelConfig, content)

    fun saveToolRequest(
        convId: String?,
        taskId: String?,
        messageId: String?,
        modelConfig: ModelConfig,
        content: String?,
        toolName: String?,
        subAgentType: String? = null,
        project: Project? = null,
    ): AgentTraceLog = buildLog(convId, taskId, messageId, modelConfig, content, toolName, subAgentType)

    fun saveAiResponse(
        convId: String?,
        taskId: String?,
        messageId: String?,
        modelConfig: ModelConfig,
        content: String?,
        statusIndex: Int = Status.SUCCESS,
        subAgentType: String? = null,
        project: Project? = null,
    ): AgentTraceLog = buildLog(convId, taskId, messageId, modelConfig, content, subAgentType = subAgentType)

    fun saveAiResponseError(
        convId: String?,
        taskId: String?,
        messageId: String?,
        modelConfig: ModelConfig,
        content: String?,
        subAgentType: String? = null,
        project: Project? = null,
    ): AgentTraceLog = buildLog(convId, taskId, messageId, modelConfig, content, subAgentType = subAgentType)

    fun saveToolResponse(
        convId: String?,
        taskId: String?,
        messageId: String?,
        modelConfig: ModelConfig,
        content: String?,
        toolName: String?,
        statusIndex: Int = Status.SUCCESS,
        toolRequestLog: AgentTraceLog? = null,
        project: Project? = null,
    ): AgentTraceLog = (toolRequestLog ?: buildLog(convId, taskId, messageId, modelConfig, content, toolName)).copy(
        content = content,
        toolName = toolName,
    )

    fun saveToolResponseError(
        convId: String?,
        taskId: String?,
        messageId: String?,
        modelConfig: ModelConfig,
        content: String?,
        toolName: String?,
        toolRequestLog: AgentTraceLog? = null,
        project: Project? = null,
    ): AgentTraceLog = saveToolResponse(convId, taskId, messageId, modelConfig, content, toolName, Status.ERROR, toolRequestLog, project)
}
