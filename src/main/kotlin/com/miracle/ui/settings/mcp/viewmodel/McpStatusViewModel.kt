package com.miracle.ui.settings.mcp.viewmodel

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.miracle.agent.mcp.McpClientHub
import com.miracle.agent.mcp.McpConfigManager
import com.miracle.agent.mcp.McpInstallScope
import com.miracle.agent.mcp.McpServerConfig
import com.miracle.utils.MCP_CONFIG_DIRECTORY
import com.miracle.utils.getProjectConfigDirectory
import com.miracle.utils.getUserConfigDirectory
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.nio.file.Paths

/**
 * MCP 状态面板的 ViewModel，负责数据获取和业务逻辑
 */
class McpStatusViewModel(private val project: Project) {

    private val log = Logger.getInstance(McpStatusViewModel::class.java)
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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
        val space: String?,
        val note: String?
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

        viewModelScope.launch {
            try {
                val hub = McpClientHub.getInstance(project)
                hub.ensureInitialized()

                val statuses = hub.getAllServerStatus()
                val connectedServers = hub.getAllClients().keys
                val toolMap = hub.getAllServerTools()
                val connectionErrors = hub.getConnectionErrors()

                if (statuses.isEmpty()) {
                    notifyState(UiState.Empty)
                    return@launch
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
                        space = config?.space,
                        note = config?.note
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
     * 添加新的 MCP 服务器配置
     *
     * @param serverName 服务器名称
     * @param config 服务器配置
     * @param scope 配置范围（项目级或全局级）
     * @return 是否成功添加
     */
    fun addServer(serverName: String, config: McpServerConfig, scope: McpInstallScope): Boolean {
        val configFile = resolveConfigFile(scope)
        val added = McpConfigManager.addServerToFile(configFile, serverName, config)
        if (!added) {
            return false
        }
        McpConfigManager.invalidate(project)
        // 触发连接
        val hub = McpClientHub.getInstance(project)
        hub.addServer(serverName, config, notifyUi = true)
        return true
    }

    /**
     * 根据配置范围解析配置文件路径
     */
    private fun resolveConfigFile(scope: McpInstallScope): java.io.File {
        val configDir = when (scope) {
            McpInstallScope.PROJECT -> getProjectConfigDirectory(project)
            McpInstallScope.GLOBAL -> getUserConfigDirectory()
        }
        return Paths.get(configDir, MCP_CONFIG_DIRECTORY, McpConfigManager.CONFIG_FILE_NAME).toFile()
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

    /**
     * 释放 ViewModel 资源，取消所有进行中的协程任务
     */
    fun dispose() {
        viewModelScope.cancel()
    }
}
