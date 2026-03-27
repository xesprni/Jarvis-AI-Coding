package com.miracle.agent.mcp

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import com.miracle.agent.mcp.McpConfigManager.removeServerFromFile
import com.miracle.utils.MCP_CONFIG_DIRECTORY
import com.miracle.utils.getCurrentProject
import com.miracle.utils.getProjectConfigDirectory
import com.miracle.utils.getUserConfigDirectory
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.shared.RequestOptions
import kotlinx.coroutines.*
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor
import org.apache.commons.io.monitor.FileAlterationMonitor
import org.apache.commons.io.monitor.FileAlterationObserver
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * MCP客户端中心，负责按项目管理所有MCP服务器连接
 */
@Service(Service.Level.PROJECT)
class McpClientHub(private val project: Project) : Disposable {
    private val clients: MutableMap<String, McpClient> = ConcurrentHashMap()
    private val serverStatus: MutableMap<String, Boolean> = ConcurrentHashMap() // true表示启用，false表示禁用
    private val connectionErrors: MutableMap<String, String> = ConcurrentHashMap()
    private val scopeJob = SupervisorJob()
    private val connectionScope = CoroutineScope(scopeJob + Dispatchers.IO)
    private val connectionJobs: MutableMap<String, Job> = ConcurrentHashMap()
    private val toggleJobs: MutableMap<String, Job> = ConcurrentHashMap()
    private val desiredToggleState: MutableMap<String, Boolean> = ConcurrentHashMap()
    private var configMonitor: FileAlterationMonitor? = null
    private val configObservers: MutableList<FileAlterationObserver> = mutableListOf()

    @Volatile
    private var currentConfig: McpConfig = McpConfig()

    @Volatile
    private var initialized = false
    private val initLock = Any()
    private val lastReloadTimestamp = AtomicLong(0L)
    private val LOG = Logger.getInstance(McpClientHub::class.java)

    companion object {
        private const val CONFIG_WATCH_INTERVAL_MS = 1000L
        private const val CONFIG_FILE_NAME = "mcp_settings.json"
        private const val TOGGLE_DEBOUNCE_MS = 200L
        const val MCP_SERVER_PREFIX = "mcp__"
        val MCP_CONFIG_TOPIC: Topic<McpConfigListener> = Topic.create(
            "qifu-mcp-config-events",
            McpConfigListener::class.java
        )

        @JvmStatic
        fun getInstance(project: Project): McpClientHub = project.service()

        @JvmStatic
        fun instantiate(): List<com.miracle.agent.tool.Tool<*>> {
            val currentProject = getCurrentProject()?.takeIf { true } ?: return emptyList()
            val hub = getInstance(currentProject).apply { ensureInitialized() }
            return runBlocking {
                hub.instantiate()
            }
        }
    }

    /** 描述一个已连接的 MCP 工具定义 */
    data class ConnectedMcpTool(
        val serverName: String,
        val tool: Tool,
        val remoteServerName: String,
    )

    /**
     * 初始化MCP客户端中心
     */
    fun initialize() {
        if (initialized) return
        synchronized(initLock) {
            if (initialized) return
            loadAllServers()
            startConfigWatcher()
            initialized = true
        }
    }

    fun ensureInitialized() {
        if (!initialized) {
            initialize()
        }
    }

    /**
     * 加载所有启用的服务器
     */
    private fun loadAllServers() {
        val config = McpConfigManager.getConfig(project)
        currentConfig = config
        val configuredServers = config.servers.keys
        (serverStatus.keys - configuredServers).forEach { serverName ->
            serverStatus.remove(serverName)
            connectionErrors.remove(serverName)
            connectionJobs.remove(serverName)?.cancel()
            clients.remove(serverName)?.close()
        }
        config.servers.forEach { (name, serverConfig) ->
            if (serverConfig.isEnabled()) {
                try {
                    addServer(name, serverConfig)
                } catch (e: Exception) {
                    LOG.warn("Failed to add server $name: ${e.message}")
                }
            } else {
                serverStatus[name] = false
                connectionErrors[name] = serverConfig.note.orEmpty()
                if (serverConfig.note.isNullOrBlank()) {
                    connectionErrors.remove(name)
                }
            }
        }
    }

    /**
     * 添加服务器
     */
    fun addServer(name: String, serverConfig: McpServerConfig, notifyUi: Boolean = true): Boolean {
        val resolvedConfig = serverConfig.withResolvedType()
        serverStatus[name] = true
        connectionErrors.remove(name)
        connectionJobs.remove(name)?.cancel()
        clients.remove(name)?.close()
        val client = McpClient(Implementation(name = name, version = "1.0.0"))
        val job = connectionScope.launch {
            try {
                client.connectToServer(resolvedConfig)
                if (!isActive) {
                    runCatching { client.close() }
                    return@launch
                }
                clients[name] = client
                connectionErrors.remove(name)
                LOG.info("Connected to MCP server $name")
            } catch (ce: CancellationException) {
                LOG.info("Connection to MCP server $name cancelled")
                serverStatus[name] = false
                connectionErrors.remove(name)
                runCatching { client.close() }
                throw ce
            } catch (e: Exception) {
                LOG.warn("Failed to add server $name: ${e.message}", e)
                serverStatus[name] = false
                connectionErrors[name] = e.message ?: "未知错误"
                runCatching { client.close() }
            } finally {
                connectionJobs.remove(name, this as Job)
                // 发送配置更新事件
                if (notifyUi) {
                    notifyConfigReloaded(refreshStatusPanel = true, refreshMarketPanel = false)
                }
            }
        }
        connectionJobs[name] = job
        return true
    }

    /**
     * 移除服务器
     */
    fun removeServer(serverName: String): Boolean {
        val job = connectionJobs.remove(serverName)
        job?.cancel()

        val client = clients.remove(serverName)
        client?.close()

        val removedStatus = serverStatus.remove(serverName) != null
        val removedError = connectionErrors.remove(serverName) != null
        return job != null || client != null || removedStatus || removedError
    }

    /**
     * 重启服务器连接
     */
    fun restartServer(serverName: String): Boolean {
        val serverConfig = McpConfigManager.getServerConfig(project, serverName)
        return if (serverConfig != null) {
            removeServer(serverName)
            addServer(serverName, serverConfig, notifyUi = false)
        } else {
            false
        }
    }

    /**
     * 启用服务器
     */
    fun refreshServer(serverName: String) {
        updateServerEnabledState(serverName, enable = true)
    }

    /**
     * 禁用服务器
     */
    fun disableServer(serverName: String) {
        updateServerEnabledState(serverName, enable = false)
    }

    private fun updateServerEnabledState(serverName: String, enable: Boolean) {
        desiredToggleState[serverName] = enable
        toggleJobs.remove(serverName)?.cancel()
        val job = connectionScope.launch {
            delay(TOGGLE_DEBOUNCE_MS)
            val target = desiredToggleState[serverName]
            if (!isActive || (target != null && target != enable)) {
                return@launch
            }
            applyToggleState(serverName, enable)
            toggleJobs.remove(serverName, this as Job)
        }
        toggleJobs[serverName] = job
    }

    /**
     * 应用服务器启用/禁用状态
     */
    private fun applyToggleState(serverName: String, enable: Boolean) {
        // 获取当前服务器的完整配置(如果不存在则直接返回)
        val serverConfig = McpConfigManager.getServerConfig(project, serverName)?.withResolvedType() ?: return
        
        // 定位项目级配置文件路径 (.qifu/mcp/mcp_settings.json)
        val projectConfigFile = Paths
            .get(getProjectConfigDirectory(project), MCP_CONFIG_DIRECTORY, McpConfigManager.CONFIG_FILE_NAME)
            .toFile()
        
        // 读取项目级配置文件中的所有服务器配置(如果文件不存在则创建空配置)
        val projectServers = if (projectConfigFile.exists()) {
            McpConfigManager.readConfigFileServers(projectConfigFile).toMutableMap()
        } else {
            mutableMapOf()
        }
        
        // 确定基准配置: 优先使用项目级配置,不存在则使用合并后的配置
        val baseConfig = (projectServers[serverName] ?: serverConfig).withResolvedType()
        
        // 创建更新后的配置: 设置 disabled 字段,并标记为项目级配置(space = "Project")
        val updatedConfig = baseConfig.copy(disabled = !enable, space = "Project")
        projectServers[serverName] = updatedConfig
        
        // 将更新后的配置写回项目级配置文件
        McpConfigManager.writeConfigFile(projectConfigFile, projectServers)
        
        // 使配置缓存失效,强制下次读取时重新加载
        McpConfigManager.invalidate(project)
        
        // 重新加载最新配置到内存
        currentConfig = McpConfigManager.loadConfig(project)

        // 检查当前运行状态是否已经符合目标状态
        val alreadyInDesiredState = enable == currentConfig.servers[serverName]?.isEnabled()
        val isConnecting = connectionJobs[serverName]?.isActive == true

        if (enable) {
            // 启用服务器: 如果已经处于启用状态且已连接,只刷新UI即可
            if (alreadyInDesiredState && clients.containsKey(serverName) && !isConnecting) {
                notifyConfigReloaded(refreshStatusPanel = true, refreshMarketPanel = false)
                return
            }
            // 否则建立新连接
            addServer(serverName, updatedConfig, notifyUi = true)
        } else {
            // 禁用服务器: 如果已经处于禁用状态且未连接,只刷新UI即可
            if (!clients.containsKey(serverName) && !isConnecting && serverStatus[serverName] == false) {
                notifyConfigReloaded(refreshStatusPanel = true, refreshMarketPanel = false)
                return
            }
            // 否则取消连接任务、关闭客户端、清理状态并通知UI
            connectionJobs.remove(serverName)?.cancel()
            clients.remove(serverName)?.close()
            serverStatus[serverName] = false
            connectionErrors.remove(serverName)
            notifyConfigReloaded(refreshStatusPanel = true, refreshMarketPanel = false)
        }
    }

    fun uninstallMcpServer(serverName: String) {
        val userConfigFile = Paths.get(
            getUserConfigDirectory(), MCP_CONFIG_DIRECTORY,
            McpConfigManager.CONFIG_FILE_NAME
        ).toFile()
        val projectConfigFile = Paths.get(
            getProjectConfigDirectory(project), MCP_CONFIG_DIRECTORY,
            McpConfigManager.CONFIG_FILE_NAME
        ).toFile()
        val removedFromUser = removeServerFromFile(userConfigFile, serverName)
        val removedFromProject = removeServerFromFile(projectConfigFile, serverName)
        val removedFromRuntime = removeServer(serverName)
        val configChanged = removedFromUser || removedFromProject
        if (configChanged || removedFromRuntime) {
            McpConfigManager.invalidate(project)
            if (shouldNotifyConfigReload(configChanged, removedFromRuntime)) {
                notifyConfigReloaded(refreshStatusPanel = true, refreshMarketPanel = true)
            }
        }
    }

    private fun shouldNotifyConfigReload(configChanged: Boolean, removedFromRuntime: Boolean): Boolean {
        if (!configChanged && !removedFromRuntime) {
            return false
        }
        if (!configChanged) {
            return true
        }
        return configMonitor == null
    }

    /**
     * 获取所有服务器状态
     */
    fun getAllServerStatus(): Map<String, Boolean> {
        return serverStatus.toMap()
    }

    fun getConnectionErrors(): Map<String, String> {
        return connectionErrors.toMap()
    }

    /**
     * 获取服务器客户端
     */
    fun getClient(serverName: String): McpClient? {
        return clients[serverName]
    }

    /**
     * 获取所有客户端
     */
    fun getAllClients(): Map<String, McpClient> {
        return clients.toMap()
    }

    //get service config
    fun getServerConfig(serverName: String): McpServerConfig? {
        return McpConfigManager.getServerConfig(project, serverName)
    }

    /**
     * 获取服务器提供的工具列表
     */
    suspend fun getServerTools(serverName: String): List<Tool> {
        if (!serverStatus.getOrDefault(serverName, false)) {
            return emptyList()
        }
        val requestOpt = RequestOptions(
            timeout = 5.seconds
        )
        return runCatching {
            withConnectedClient(serverName) { client ->
                client.listTools(options = requestOpt).tools
            }
        }.getOrElse { e ->
            LOG.warn("Failed to get tools for server $serverName: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * 获取所有服务器的工具列表
     */
    suspend fun getAllServerTools(): Map<String, List<Tool>> {
        return coroutineScope {
            clients.keys.map { serverName ->
                async {
                    serverName to getServerTools(serverName)
                }
            }.awaitAll().toMap()
        }
    }

    /**
     * 获取当前项目下所有已连接 MCP 工具对应的 Agent Tool 实例
     */
    suspend fun instantiate(): List<com.miracle.agent.tool.Tool<*>> {
        val currentProject = getCurrentProject()?.takeIf { true } ?: return emptyList()
        val hub = getInstance(currentProject).apply { ensureInitialized() }
        val descriptors = hub.getConnectedMcpTools()
        if (descriptors.isEmpty()) {
            return emptyList()
        }
        return descriptors.mapNotNull { descriptor ->
            runCatching { McpServerToolInstance(descriptor) }
                .onFailure {
                    LOG.warn("Failed to instantiate MCP tool ${descriptor.serverName}/${descriptor.tool.name}", it)
                }
                .getOrNull()
        }
    }

    /**
     * 获取当前项目所有已连接 MCP 服务器暴露的工具，返回平铺后的列表
     */
    suspend fun getConnectedMcpTools(): List<ConnectedMcpTool> {
        val toolsByServer = getAllServerTools()
        if (toolsByServer.isEmpty()) {
            return emptyList()
        }
        return toolsByServer.entries.flatMap { (serverName, tools) ->
            clients[serverName]?.let { client ->
                val remoteServerName = client.getClient().serverVersion?.name ?: serverName
                tools.map { ConnectedMcpTool(serverName, it, remoteServerName) }
            } ?: tools.map { ConnectedMcpTool(serverName, it,serverName) }
        }
    }

    /**
     * 启动配置文件监控
     */
    private fun startConfigWatcher() {
        try {
            val targetDirectories = buildList {
                add(Paths.get(getUserConfigDirectory(), MCP_CONFIG_DIRECTORY))
                add(Paths.get(getProjectConfigDirectory(project), MCP_CONFIG_DIRECTORY))
            }.map { it.toAbsolutePath().normalize() }
                .distinct()
            if (targetDirectories.isEmpty()) {
                return
            }
            //保证targetDirectories目录创建
            targetDirectories.forEach(fun(dir: Path) {
                if (!Files.exists(dir)) {
                    Files.createDirectories(dir)
                }
            })
            val monitor = FileAlterationMonitor(CONFIG_WATCH_INTERVAL_MS)
            val observers = mutableListOf<FileAlterationObserver>()
            targetDirectories.forEach { dir ->
                val normalizedDir = dir.toAbsolutePath().normalize()
                if (!Files.exists(normalizedDir) || !Files.isDirectory(normalizedDir)) {
                    return@forEach
                }
                try {
                    val observer = FileAlterationObserver(normalizedDir.toFile())
                    observer.addListener(object : FileAlterationListenerAdaptor() {
                        private fun handleChange(file: File?) {
                            if (file?.name == CONFIG_FILE_NAME) {
                                LOG.debug("MCP config file changed for ${project.name} in $normalizedDir, triggering reload...")
                                reloadConfig()
                            }
                        }

                        override fun onFileCreate(file: File) = handleChange(file)
                        override fun onFileChange(file: File) = handleChange(file)
                        override fun onFileDelete(file: File) = handleChange(file)
                    })
                    monitor.addObserver(observer)
                    observers.add(observer)
                    LOG.debug("Watching MCP config directory (commons-io): $normalizedDir")
                } catch (e: Exception) {
                    LOG.warn("Failed to register commons-io watcher for $normalizedDir: ${e.message}")
                }
            }
            if (observers.isEmpty()) {
                return
            }
            configMonitor = monitor
            configObservers.clear()
            configObservers.addAll(observers)
            try {
                monitor.start()
            } catch (e: Exception) {
                observers.forEach { monitor.removeObserver(it) }
                configObservers.clear()
                configMonitor = null
                LOG.warn("Failed to start config watcher for ${project.name}: ${e.message}")
            }
        } catch (e: Exception) {
            LOG.warn("Failed to start config watcher for ${project.name}: ${e.message}")
        }
    }


    /**
     * 重新加载配置
     * 
     */
    private fun reloadConfig() {
        try {
            // 从磁盘重新加载配置文件(合并用户级和项目级配置)
            val newConfig = McpConfigManager.loadConfig(project)
            val previousServers = currentConfig.servers
            val newServers = newConfig.servers
            if (previousServers == newServers) {
                // 如果配置完全相同,直接返回避免无意义的重连操作
                return
            }
            // 计算配置差异: 哪些服务器被移除、新增、状态变化、配置修改
            val removedServers = previousServers.keys - newServers.keys
            val addedServers = newServers.keys - previousServers.keys
            val statusChanged = mutableSetOf<String>()  // 启用/禁用状态变化的服务器
            val configChanged = mutableSetOf<String>()  // 配置参数修改的服务器
            // 移除配置中已删除的服务器(取消连接任务、关闭客户端、清理状态)
            removedServers.forEach { serverName ->
                removeServer(serverName)
            }
            // 处理现有和新增的服务器配置
            val adjustedServers = LinkedHashMap<String, McpServerConfig>()
            newServers.forEach { (name, config) ->
                val previous = previousServers[name]
                // 检测启用状态是否变化(disabled字段)
                val enabledChanged = previous?.isEnabled() != config.isEnabled()
                // 标准化配置用于对比,去除disabled/space等不影响连接的字段
                val normalizedPrevious = previous?.normalizedForRuntime()
                val normalizedCurrent = config.normalizedForRuntime()
                // 检测配置参数是否发生实质性变化(如command、url、env等)
                val meaningfulChange = normalizedPrevious != null && normalizedPrevious != normalizedCurrent
                if (config.isEnabled()) {
                    // 判断是否需要建立新连接(新增、从禁用到启用、配置改变、连接失败等情况)
                    val needsConnect = previous == null ||
                        !previous.isEnabled() ||
                        !clients.containsKey(name) ||
                        meaningfulChange ||
                        serverStatus[name] == false
                    if (needsConnect) {
                        addServer(name, config, notifyUi = false)
                    }
                } else {
                    // 禁用状态: 取消连接任务、关闭客户端、标记为禁用并清除错误记录
                    connectionJobs.remove(name)?.cancel()
                    clients.remove(name)?.close()
                    serverStatus[name] = false
                    if (config.note.isNullOrBlank()) {
                        connectionErrors.remove(name)
                    } else {
                        connectionErrors[name] = config.note
                    }
                }
                // 记录发生了哪些类型的变化,用于后续决定是否需要刷新UI
                if (enabledChanged) {
                    statusChanged.add(name)
                }
                if (meaningfulChange) {
                    configChanged.add(name)
                }
                adjustedServers[name] = config
            }
            // 更新当前配置为最新状态
            currentConfig = McpConfig(adjustedServers)
            // 根据变化类型决定是否需要刷新UI面板
            // refreshMarketPanel: 服务器增删或配置修改时需要刷新市场面板
            val refreshMarketPanel = addedServers.isNotEmpty() || removedServers.isNotEmpty() || configChanged.isNotEmpty()
            // refreshStatusPanel: 状态变化或需要刷新市场面板时都要刷新状态面板
            val refreshStatusPanel = refreshMarketPanel || statusChanged.isNotEmpty()
            if (refreshStatusPanel) {
                notifyConfigReloaded(
                    refreshStatusPanel = true,
                    refreshMarketPanel = refreshMarketPanel
                )
            }
        } catch (e: Exception) {
            LOG.warn("Failed to reload config for ${project.name}: ${e.message}")
        }
    }

    @Suppress("SameParameterValue")
    private fun notifyConfigReloaded(
        refreshStatusPanel: Boolean = true,
        refreshMarketPanel: Boolean = true
    ) {
        ApplicationManager.getApplication().invokeLater {
            project.messageBus.syncPublisher(MCP_CONFIG_TOPIC)
                .onConfigUpdated(project, refreshStatusPanel, refreshMarketPanel)
        }
    }

    /**
     * 停止配置文件监控
     */
    private fun stopConfigWatcher() {
        val monitor = configMonitor
        val observers = configObservers.toList()
        lastReloadTimestamp.set(0L)
        try {
            observers.forEach { observer ->
                monitor?.removeObserver(observer)
            }
        } finally {
            monitor?.stop()
            configMonitor = null
            configObservers.clear()
        }
    }

    /**
     * 关闭所有连接
     */
    fun shutdown() {
        stopConfigWatcher()
        connectionJobs.values.forEach { it.cancel() }
        connectionJobs.clear()
        toggleJobs.values.forEach { it.cancel() }
        toggleJobs.clear()
        desiredToggleState.clear()
        scopeJob.cancelChildren()
        clients.values.forEach { it.close() }
        clients.clear()
        serverStatus.clear()
        connectionErrors.clear()
        synchronized(initLock) {
            initialized = false
        }
        currentConfig = McpConfig()
        McpConfigManager.invalidate(project)
    }

    override fun dispose() {
        shutdown()
    }

    /**
     * 使用已连接的客户端执行操作，如果 会话失效则自动重连并重试一次
     */
    suspend fun <T> withConnectedClient(serverName: String, action: suspend (Client) -> T): T {
        waitForConnectionJob(serverName)
        val client = clients[serverName]
        if (client == null || !serverStatus.getOrDefault(serverName, false)) {
            throw IllegalStateException("MCP server '$serverName' is not connected or disabled.")
        }
        //对于非 streamableHttp 类型的连接，直接使用现有连接
        currentConfig.servers[serverName]?.takeIf { "streamableHttp" != it.type }?.run {
            return action(client.getClient())
        }
        return try {
            action(client.getClient())
        } catch (e: Exception) {
            LOG.warn("Error using MCP client for server $serverName: ${e.message}, attempting to reconnect...", e)
            val reconnected = reconnectServer(serverName)
            if (!reconnected) {
                throw e
            }
            val refreshed = clients[serverName] ?: throw e
            action(refreshed.getClient())
        }
    }

    private suspend fun waitForConnectionJob(serverName: String) {
        try {
            connectionJobs[serverName]?.takeIf { it.isActive }?.join()
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) {
            // ignore other errors, higher-level logic will handle connection availability
        }
    }

    private suspend fun reconnectServer(serverName: String): Boolean {
        val restarted = restartServer(serverName)
        if (!restarted) {
            return false
        }
        return try {
            waitForConnectionJob(serverName)
            serverStatus.getOrDefault(serverName, false) && clients.containsKey(serverName)
        }catch (_: Exception) {
            false
        }
    }

}

private fun McpServerConfig.isEnabled(): Boolean {
    return disabled != true
}

private fun McpServerConfig.normalizedForRuntime(): McpServerConfig {
    return copy(disabled = null, space = null)
}

private fun McpServerConfig.withResolvedType(): McpServerConfig {
    val resolvedType = when {
        type.isNotBlank() -> type
        command.isNotBlank() -> "stdio"
        url.isNotBlank() -> "streamableHttp"
        else -> "stdio"
    }
    return copy(type = resolvedType)
}

fun interface McpConfigListener {
    fun onConfigUpdated(project: Project, refreshStatusPanel: Boolean, refreshMarketPanel: Boolean)
}
