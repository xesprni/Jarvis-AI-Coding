package com.qifu.ui.settings.mcp.viewmodel

import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.qifu.agent.mcp.McpConfigManager
import com.qifu.external.LowcodeApi
import com.qifu.external.McpMarketService
import com.qifu.external.McpMarketServiceTemplate
import com.qifu.ui.settings.mcp.McpInstallDialogResult
import com.qifu.ui.settings.mcp.McpInstallParameterDescriptor
import kotlinx.coroutines.runBlocking
import java.util.LinkedHashSet
import java.util.concurrent.TimeUnit

/**
 * MCP 市场面板的 ViewModel，负责数据获取和业务逻辑
 */
class McpMarketViewModel(
    private val project: Project,
    private val hideInstalled: Boolean = false
) {

    private val log = Logger.getInstance(McpMarketViewModel::class.java)

    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.SECONDS)
        .build<String, List<McpMarketService>>()

    private var allServices: List<McpMarketService> = emptyList()
    private var currentSearchQuery: String = ""

    /**
     * MCP 市场面板的 UI 状态
     */
    sealed class UiState {
        object Loading : UiState()
        data class Content(
            val services: List<ServiceDisplayInfo>,
            val searchQuery: String
        ) : UiState()
        data class Error(val message: String) : UiState()
        object Empty : UiState()
        data class EmptySearch(val keyword: String?) : UiState()
    }

    /**
     * 服务显示信息
     */
    data class ServiceDisplayInfo(
        val service: McpMarketService,
        val isInstalled: Boolean
    )

    /**
     * 安装请求
     */
    data class InstallRequest(
        val service: McpMarketService,
        val serverTemplate: McpMarketServiceTemplate,
        val parameterDescriptors: List<McpInstallParameterDescriptor>,
        val onConfirm: (McpInstallDialogResult) -> Unit,
        val onCancel: () -> Unit,
        val onError: (Exception) -> Unit
    )

    private var stateListener: ((UiState) -> Unit)? = null
    private var installRequestListener: ((InstallRequest) -> Unit)? = null

    /**
     * 设置状态监听器
     */
    fun setStateListener(listener: (UiState) -> Unit) {
        this.stateListener = listener
    }

    /**
     * 设置安装请求监听器
     */
    fun setInstallRequestListener(listener: (InstallRequest) -> Unit) {
        this.installRequestListener = listener
    }

    /**
     * 刷新 MCP 市场数据
     */
    fun refresh() {
        stateListener?.invoke(UiState.Loading)

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val data = getOrLoadBlocking { LowcodeApi.getMcpMarketServices() }
                allServices = data

                if (allServices.isEmpty()) {
                    notifyState(UiState.Empty)
                } else {
                    filterAndNotify()
                }
            } catch (e: Exception) {
                log.warn("Failed to load MCP market", e)
                notifyState(UiState.Error("加载 MCP 市场失败：${e.message ?: "未知错误"}"))
            }
        }
    }

    /**
     * 搜索过滤
     */
    fun search(query: String) {
        currentSearchQuery = query
        filterAndNotify()
    }

    /**
     * 准备安装服务
     */
    fun prepareInstall(
        service: McpMarketService,
        onSuccess: (InstallRequest) -> Unit,
        onError: (Exception) -> Unit
    ) {
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val serverTemplate = runBlocking {
                    LowcodeApi.getMcpServerById(service.mcpId)
                } ?: throw IllegalStateException("未获取到服务模板")

                val selection = McpConfigManager.selectInstallTemplate(serverTemplate)
                    ?: throw IllegalStateException("该服务没有可用的安装模板")

                val parameterDescriptors = buildParameterDescriptors(selection.template)

                val request = InstallRequest(
                    service = service,
                    serverTemplate = serverTemplate,
                    parameterDescriptors = parameterDescriptors,
                    onConfirm = { dialogResult ->
                        executeInstall(service, serverTemplate, dialogResult)
                    },
                    onCancel = { /* no-op */ },
                    onError = onError
                )

                ApplicationManager.getApplication().invokeLater(
                    { onSuccess(request) },
                    ModalityState.any()
                )
            } catch (e: Exception) {
                log.warn("Failed to prepare install for ${service.mcpId}", e)
                ApplicationManager.getApplication().invokeLater(
                    { onError(e) },
                    ModalityState.any()
                )
            }
        }
    }

    /**
     * 执行安装
     */
    fun executeInstall(
        service: McpMarketService,
        serverTemplate: McpMarketServiceTemplate,
        dialogResult: McpInstallDialogResult,
        onSuccess: (() -> Unit)? = null,
        onError: ((Exception) -> Unit)? = null
    ) {
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val serverName = service.mcpId
                McpConfigManager.installMcpServer(
                    serverTemplate,
                    serverName,
                    project,
                    service.name,
                    dialogResult.scope,
                    dialogResult.parameterValues
                )

                ApplicationManager.getApplication().invokeLater(
                    {
                        onSuccess?.invoke()
                        // 刷新列表
                        refresh()
                    },
                    ModalityState.any()
                )
            } catch (e: Exception) {
                log.warn("Failed to install MCP service: ${service.mcpId}", e)
                ApplicationManager.getApplication().invokeLater(
                    { onError?.invoke(e) },
                    ModalityState.any()
                )
            }
        }
    }

    /**
     * 获取已安装的服务名称集合
     */
    fun getInstalledNames(): Set<String> {
        McpConfigManager.invalidate(project)
        return McpConfigManager.getConfig(project).servers.keys
            .mapNotNull { it.trim().takeIf { name -> name.isNotEmpty() }?.lowercase() }
            .toSet()
    }

    private fun filterAndNotify() {
        val searchName = currentSearchQuery.trim().lowercase().takeIf { it.isNotEmpty() }

        val filtered = if (searchName.isNullOrEmpty()) {
            allServices
        } else {
            allServices.filter { service ->
                val haystack = buildString {
                    appendLine(service.name ?: "")
                    appendLine(service.description ?: "")
                    appendLine(service.category ?: "")
                    appendLine(service.orgName ?: "")
                    service.tags?.joinToString(" ")?.let { appendLine(it) }
                }.lowercase()
                haystack.contains(searchName)
            }
        }

        val installedNames = getInstalledNames()

        val displayList = if (hideInstalled) {
            filtered.filterNot { service ->
                val serviceNameKey = service.name?.trim()?.lowercase()
                val serviceMcpIdKey = service.mcpId.trim().lowercase()
                (serviceNameKey != null && installedNames.contains(serviceNameKey)) ||
                    installedNames.contains(serviceMcpIdKey)
            }
        } else {
            filtered
        }

        if (displayList.isEmpty()) {
            notifyState(UiState.EmptySearch(searchName))
        } else {
            val services = displayList.map { service ->
                val serviceNameKey = service.name?.trim()?.lowercase()
                val serviceMcpIdKey = service.mcpId.trim().lowercase()
                val isInstalled = (serviceNameKey != null && installedNames.contains(serviceNameKey)) ||
                    installedNames.contains(serviceMcpIdKey)
                ServiceDisplayInfo(service, isInstalled)
            }
            notifyState(UiState.Content(services, currentSearchQuery))
        }
    }

    private fun buildParameterDescriptors(
        template: McpMarketServiceTemplate.Template
    ): List<McpInstallParameterDescriptor> {
        val placeholders = McpConfigManager.extractTemplatePlaceholders(template.content)
        val parameterMeta = template.parameters.orEmpty()
            .mapNotNull { param ->
                val name = param.name ?: return@mapNotNull null
                name to param
            }
            .toMap()

        val namesToPrompt = LinkedHashSet<String>()

        placeholders.forEach { placeholder ->
            parameterMeta[placeholder]?.let {
                if (it.defaultValue == null) {
                    namesToPrompt.add(placeholder)
                }
                it.defaultValue?.content?.takeIf { str -> str.isBlank() }?.let {
                    namesToPrompt.add(placeholder)
                }
            }
        }

        parameterMeta.values
            .filter { it.defaultValue == null }
            .forEach { param ->
                val name = param.name
                if (!name.isNullOrBlank()) {
                    namesToPrompt.add(name)
                }
            }

        if (namesToPrompt.isEmpty()) {
            return emptyList()
        }

        return namesToPrompt.map { name ->
            val meta = parameterMeta[name]
            McpInstallParameterDescriptor(
                name = name,
                description = meta?.description,
            )
        }
    }

    private fun getOrLoadBlocking(loader: suspend () -> List<McpMarketService>): List<McpMarketService> {
        return cache.get(MCP_MARKET_CACHE_KEY) {
            runBlocking { loader() }
        }
    }

    private fun notifyState(state: UiState) {
        ApplicationManager.getApplication().invokeLater(
            { stateListener?.invoke(state) },
            ModalityState.any()
        )
    }

    companion object {
        private const val MCP_MARKET_CACHE_KEY = "mcp_market_services"
    }
}
