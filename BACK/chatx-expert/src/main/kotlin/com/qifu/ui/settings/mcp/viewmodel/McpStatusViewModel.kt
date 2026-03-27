package com.qifu.ui.settings.mcp.viewmodel

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.qifu.agent.mcp.McpClientHub
import com.qifu.agent.mcp.McpServerConfig
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.coroutines.runBlocking

/**
 * MCP 状态面板的 ViewModel，负责数据获取和业务逻辑
 */
class McpStatusViewModel(private val project: Project) {

    private val log = Logger.getInstance(McpStatusViewModel::class.java)

    /**
     * MCP 状态面板的 UI 状态
     */
    sealed class UiState {
        object Loading : UiState()
        data class Content(val servers: List<ServerInfo>) : UiState()
        data class Error(val message: String) : UiState()
        object Empty : UiState()
    }

    /**
     * 单个服务器的信息
     */
    data class ServerInfo(
        val serverName: String,
        val displayName: String,
        val enabled: Boolean,
        val connected: Boolean,
        val tools: List<Tool>,
        val connectionError: String?,
        val space: String?
    )

    private var stateListener: ((UiState) -> Unit)? = null

    /**
     * 设置状态监听器
     */
    fun setStateListener(listener: (UiState) -> Unit) {
        this.stateListener = listener
    }

    /**
     * 刷新 MCP 服务状态
     */
    fun refresh() {
        stateListener?.invoke(UiState.Loading)

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val hub = McpClientHub.getInstance(project)
                hub.ensureInitialized()

                val statuses = hub.getAllServerStatus()
                val connectedServers = hub.getAllClients().keys
                val toolMap = runBlocking { hub.getAllServerTools() }
                val connectionErrors = hub.getConnectionErrors()

                if (statuses.isEmpty()) {
                    notifyState(UiState.Empty)
                    return@executeOnPooledThread
                }

                val servers = statuses.toSortedMap().map { (serverName, enabled) ->
                    val config = hub.getServerConfig(serverName)
                    ServerInfo(
                        serverName = serverName,
                        displayName = config?.name ?: serverName,
                        enabled = enabled,
                        connected = connectedServers.contains(serverName),
                        tools = toolMap[serverName].orEmpty(),
                        connectionError = connectionErrors[serverName],
                        space = config?.space
                    )
                }

                notifyState(UiState.Content(servers))
            } catch (e: Exception) {
                log.warn("Failed to load MCP status", e)
                notifyState(UiState.Error("获取 MCP 状态失败：${e.message ?: "未知错误"}"))
            }
        }
    }

    /**
     * 切换服务器启用状态
     */
    fun setServerEnabled(serverName: String, enabled: Boolean) {
        val hub = McpClientHub.getInstance(project)
        if (enabled) {
            hub.refreshServer(serverName)
        } else {
            hub.disableServer(serverName)
        }
    }

    /**
     * 卸载指定服务器
     */
    fun uninstallServer(serverName: String) {
        val hub = McpClientHub.getInstance(project)
        hub.uninstallMcpServer(serverName)
    }

    /**
     * 获取服务器配置
     */
    fun getServerConfig(serverName: String): McpServerConfig? {
        return McpClientHub.getInstance(project).getServerConfig(serverName)
    }

    private fun notifyState(state: UiState) {
        ApplicationManager.getApplication().invokeLater {
            stateListener?.invoke(state)
        }
    }
}
