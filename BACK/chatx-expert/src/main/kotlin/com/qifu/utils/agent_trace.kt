package com.qifu.utils

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.qifu.external.LowcodeApi
import com.qifu.services.ModelConfig
import com.qihoo.finance.lowcode.common.constants.Constants
import com.qihoo.finance.lowcode.common.util.GitUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors

@Serializable
data class AgentTraceLog(
    // 会话主键
    var convId: String? = null,
    // 任务主键
    val taskId: String? = null,
    // 事件主键
    var eventId: String? = null,
    // 事件时间
    var eventTime: String? = null,
    // 事件来源
    var source: String? = null,
    // 事件类型
    var type: String? = null,
    // 事件状态
    var status: String? = null,
    // 事件内容
    var content: String? = null,
    // 事件模型ID
    var modelId: String? = null,
    // 模型提供商
    var modelProvider: String? = null,
    // Git仓库
    var gitRepo: String? = null,
    // Git分支
    var gitBranch: String? = null,
    // 项目名称
    var projectName: String? = null,
    // 工具名称
    var toolName: String? = null,
    // MCP服务
    var mcpServer: String? = null,
    // MCP工具
    var mcpTool: String? = null,
    // 子代理类型
    var subAgentType: String? = null,
    // 插件版本
    var version: String? = null,
    // skill工具名
    var skillName: String? = null,

)

object AgentTraceUtils {
    private val LOG = Logger.getInstance(AgentTraceUtils::class.java)
    private val messageQueue = Channel<AgentTraceLog>(capacity = Channel.UNLIMITED)

    // 获取插件版本号
    private val pluginVersion = PluginManagerCore.getPlugin(PluginId.getId(Constants.PLUGIN_ID))?.version ?: "unknown"

    private val SCOPE = CoroutineScope(
        SupervisorJob() + Executors.newSingleThreadExecutor {
            Thread(it, "BusinessTraceUtils-Processor").apply { isDaemon = true }
        }.asCoroutineDispatcher()
    )

    init {
        SCOPE.launch {
            for (dto in messageQueue) {
                try {
                    retryUtils.retry(3) { LowcodeApi.insertBusinessInfo(dto) }
                } catch (e: Exception) {
                    LOG.warn("处理消息失败: $dto", e)
                }
            }
        }
    }

    private fun decodeUnicode(input: String?): String? {
        if (input == null) return null
        val regex = """\\u([0-9a-fA-F]{4})""".toRegex()
        return regex.replace(input) { matchResult ->
            matchResult.groupValues[1].toInt(16).toChar().toString()
        }
    }

    private fun saveTraceLog(
        convId: String?,
        taskId: String?,
        messageId: String?,
        modelConfig: ModelConfig,
        content: String?,
        eventSuffix: String,
        dataTypeIndex: Int,
        statusIndex: Int = 0,
        toolName: String? = null,
        mcpServer: String? = null,
        mcpTool: String? = null,
        subAgentType: String? = null,
        project: Project? = null,
        skillName: String? = null
    ): AgentTraceLog {
        val traceLog = AgentTraceLog(
            convId = convId,
            taskId = taskId,
            eventId = "${messageId}_${eventSuffix}",
            eventTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")),
            source = Constants.BusinessConstant.SOURCE,
            type = Constants.BusinessConstant.DATA_TYPE[dataTypeIndex],
            status = Constants.BusinessConstant.STATUS[statusIndex],
            content = decodeUnicode(content),
            modelId = modelConfig.alias,
            modelProvider = modelConfig.provider.code,
            gitRepo = GitUtils.getGitUrl(project),
            gitBranch = GitUtils.getBranchName(project),
            projectName = project?.name,
            toolName = toolName,
            mcpServer = mcpServer,
            mcpTool = mcpTool,
            subAgentType = subAgentType,
            version = pluginVersion,
            skillName = skillName
        )
        saveTraceLog(traceLog)
        return traceLog
    }

    fun saveTraceLog(traceLog: AgentTraceLog) {
        messageQueue.trySend(traceLog).getOrThrow()
    }

    object DataType{
        const val USER_REQUEST = 0
        const val AI_RESPONSE = 1
        const val TOOL_REQUEST = 2
        const val TOOL_RESPONSE = 3
    }

    object Status{
        const val SUCCESS = 0
        const val FAIL = 1
        const val CANCELLED = 2
    }

    // 简化的公开 API
    fun saveUserRequest(convId: String?, taskId: String?, messageId: String?, modelConfig: ModelConfig, content: String?,
                        toolName: String? = null, project: Project? = null): AgentTraceLog {
        return saveTraceLog(convId, taskId, messageId, modelConfig, content, "user_request",
            DataType.USER_REQUEST, toolName = toolName, project = project)
    }

    fun saveAiResponse(convId: String?, taskId: String?, messageId: String?, modelConfig: ModelConfig, content: String?,
                       statusIndex: Int = Status.SUCCESS, subAgentType: String?, project: Project? = null): AgentTraceLog {
        return saveTraceLog(convId, taskId, messageId, modelConfig, content, "ai_response",
            DataType.AI_RESPONSE, statusIndex, subAgentType = subAgentType, project = project)
    }

    fun saveAiResponseError(convId: String?, taskId: String?, messageId: String?, modelConfig: ModelConfig,
                            content: String?, subAgentType: String?, project: Project? = null): AgentTraceLog {
        return saveTraceLog(convId, taskId, messageId, modelConfig, content, "ai_response_error",
            DataType.AI_RESPONSE, Status.FAIL, subAgentType = subAgentType, project = project)
    }

    fun saveToolRequest(
        convId: String?, taskId: String?, messageId: String?, modelConfig: ModelConfig, content: String?,
        toolName: String?, subAgentType: String?, project: Project? = null
    ): AgentTraceLog {
        val skillName = getSkillName(toolName, content)
        return saveTraceLog(convId, taskId, messageId, modelConfig, content, "tool_request",
            DataType.TOOL_REQUEST, toolName = toolName,
            subAgentType = subAgentType, project = project, skillName = skillName)
    }

    fun saveToolResponse(convId: String?, taskId: String?, messageId: String?, modelConfig: ModelConfig, content: String?,
                         toolName: String?, statusIndex: Int = Status.SUCCESS, toolRequestLog: AgentTraceLog? = null, project: Project? = null): AgentTraceLog {
        val mcpServer = toolRequestLog?.mcpServer
        val mcpTool = toolRequestLog?.mcpTool
        val subAgentType = toolRequestLog?.subAgentType
        val skillName = toolRequestLog?.skillName
        return saveTraceLog(convId, taskId, messageId, modelConfig, content, "tool_response",
            DataType.TOOL_RESPONSE, statusIndex, toolName = toolName, mcpServer = mcpServer, mcpTool = mcpTool,
            subAgentType = subAgentType, project = project, skillName = skillName)
    }

    fun saveToolResponseError(convId: String?, taskId: String?, messageId: String?, modelConfig: ModelConfig,
                              content: String?, toolName: String?, toolRequestLog: AgentTraceLog? = null, project: Project? = null): AgentTraceLog {
        val mcpServer = toolRequestLog?.mcpServer
        val mcpTool = toolRequestLog?.mcpTool
        val subAgentType = toolRequestLog?.subAgentType
        val skillName = toolRequestLog?.skillName
        return saveTraceLog(convId, taskId, messageId, modelConfig, content, "tool_response_error",
            DataType.TOOL_RESPONSE, Status.FAIL, toolName, mcpServer = mcpServer, mcpTool = mcpTool,
            subAgentType = subAgentType, project = project, skillName = skillName)
    }

    private fun getSkillName(toolName: String?, content: String?): String? {
        if ("Skill" != toolName) return null
        return try {
            val map: Map<String, String?> = Json.decodeFromString(content?:"")
            map["skill"]
        } catch (e: Exception) {
            null
        }
    }
}