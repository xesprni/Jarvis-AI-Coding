package com.miracle.agent.mcp

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.miracle.utils.MCP_CONFIG_DIRECTORY
import com.miracle.utils.getCurrentProjectRootPath
import com.miracle.utils.getProjectConfigDirectory
import com.miracle.utils.getUserConfigDirectory
import com.miracle.utils.file.FileUtil
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * MCP 单个服务器的配置信息
 */
@Serializable
data class McpServerConfig(
    val type: String = "stdio",
    val url: String = "",
    val command: String = "",
    val args: List<String> = emptyList(),
    val env: Map<String, String> = emptyMap(),
    val disabled: Boolean? = null,
    val token: String = "",
    val headers: Map<String, String> = emptyMap(),
    val timeout: Long? = null,
    var space: String? = null,
    val name: String? = null,
    val note: String? = null,
)

/**
 * MCP 配置，包含所有服务器的配置映射
 */
@Serializable
data class McpConfig(
    val servers: Map<String, McpServerConfig> = emptyMap(),
)

/**
 * MCP 服务器安装范围枚举，区分项目级和全局级配置
 */
enum class McpInstallScope(val displayName: String, val spaceName: String) {
    PROJECT("项目(Project)", "Project"),
    GLOBAL("全局(Global)", "Global");

    override fun toString(): String = displayName
}

/**
 * MCP 配置管理器，负责配置文件的读取、写入、合并和缓存管理。
 * 支持用户级（全局）和项目级两级配置，项目级配置优先于全局配置。
 */
object McpConfigManager {
    /** 配置缓存条目 */
    private data class ConfigCacheEntry(val config: McpConfig)

    /** 配置缓存，key 为项目路径 */
    private val cachedConfigs = ConcurrentHashMap<String, ConfigCacheEntry>()
    /** JSON 序列化配置 */
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = false
    }

    /** 默认配置文件模板 */
    private const val DEFAULT_CONFIG_TEMPLATE = "{\n  \"mcpServers\": {}\n}\n"
    /** 旧版 useJarvisAuth 认证方式移除后的提示信息 */
    private const val LEGACY_AUTH_NOTE =
        "Legacy useJarvisAuth was removed. Add explicit token or headers to re-enable."

    /** 配置文件名 */
    const val CONFIG_FILE_NAME = "mcp_settings.json"
    val LOG: Logger = Logger.getInstance(McpConfigManager::class.java)

    /**
     * 从磁盘加载并合并用户级和项目级配置，同时更新缓存
     *
     * @param project IntelliJ 项目实例，为 null 时仅加载全局配置
     * @return 合并后的 MCP 配置
     */
    fun loadConfig(project: Project? = null): McpConfig {
        val key = projectKey(project)
        val userConfigFile = Paths.get(getUserConfigDirectory(), MCP_CONFIG_DIRECTORY, CONFIG_FILE_NAME).toFile()
        val projectConfigFile = Paths.get(getProjectConfigDirectory(project), MCP_CONFIG_DIRECTORY, CONFIG_FILE_NAME).toFile()
        val mergedServers = LinkedHashMap<String, McpServerConfig>()

        val userServers = readConfigFile(userConfigFile, McpInstallScope.GLOBAL)
        val projectServers = readConfigFile(projectConfigFile, McpInstallScope.PROJECT)
        val invalidJsonDetected =
            (userConfigFile.exists() && userServers == null) || (projectConfigFile.exists() && projectServers == null)

        userServers.orEmpty().forEach { (name, config) ->
            mergedServers[name] = normalizeConfig(config).copy(space = McpInstallScope.GLOBAL.spaceName)
        }
        projectServers.orEmpty().forEach { (name, config) ->
            mergedServers[name] = normalizeConfig(config).copy(space = McpInstallScope.PROJECT.spaceName)
        }

        val finalConfig = if (invalidJsonDetected) {
            cachedConfigs[key]?.config ?: McpConfig(emptyMap())
        } else {
            McpConfig(mergedServers)
        }
        cachedConfigs[key] = ConfigCacheEntry(finalConfig)
        return finalConfig
    }

    /**
     * 获取缓存的配置，缓存未命中时从磁盘加载
     *
     * @param project IntelliJ 项目实例，为 null 时获取全局配置
     * @return MCP 配置
     */
    fun getConfig(project: Project? = null): McpConfig {
        val key = projectKey(project)
        return cachedConfigs[key]?.config ?: loadConfig(project)
    }

    /**
     * 获取指定服务器的配置信息
     *
     * @param project IntelliJ 项目实例
     * @param serverName 服务器名称
     * @return 服务器配置，不存在时返回 null
     */
    fun getServerConfig(project: Project? = null, serverName: String): McpServerConfig? {
        return getConfig(project).servers[serverName]?.let(::normalizeConfig)
    }

    /**
     * 获取所有已启用的服务器配置列表
     *
     * @param project IntelliJ 项目实例，为 null 时获取全局配置
     * @return 服务器名称与配置的配对列表
     */
    fun getEnabledServers(project: Project? = null): List<Pair<String, McpServerConfig>> {
        return getConfig(project).servers.mapNotNull { (name, config) ->
            val normalized = normalizeConfig(config)
            if (normalized.isEnabled()) name to normalized else null
        }
    }

    /**
     * 使指定项目的配置缓存失效，强制下次读取时重新加载
     *
     * @param project IntelliJ 项目实例，为 null 时清除全局配置缓存
     */
    fun invalidate(project: Project? = null) {
        cachedConfigs.remove(projectKey(project))
    }

    /**
     * 从指定配置文件中读取服务器配置映射
     *
     * @param file 配置文件
     * @return 服务器名称到配置的映射
     */
    fun readConfigFileServers(file: File): Map<String, McpServerConfig> {
        val scope = inferScope(file)
        return readConfigFile(file, scope).orEmpty()
    }

    /**
     * 将服务器配置写入指定的配置文件
     *
     * @param file 目标配置文件
     * @param servers 服务器名称到配置的映射
     * @throws Exception 写入失败时抛出异常
     */
    fun writeConfigFile(file: File, servers: Map<String, McpServerConfig>) {
        try {
            file.parentFile?.mkdirs()
            val config = McpConfig(servers)
            val configJson = json.encodeToJsonElement(McpConfig.serializer(), config).jsonObject
            val rootJson = buildJsonObject {
                put("mcpServers", configJson["servers"] ?: JsonObject(emptyMap()))
            }
            val payload = json.encodeToString(JsonObject.serializer(), rootJson)
            forceWriteAndSync(file, payload)
        } catch (e: Exception) {
            LOG.warn("Failed to write MCP config ${file.absolutePath}: ${e.message}", e)
            throw e
        }
    }

    /**
     * 从指定配置文件中移除某个服务器配置
     *
     * @param file 配置文件
     * @param serverName 待移除的服务器名称
     * @return 是否成功移除
     */
    fun removeServerFromFile(file: File, serverName: String): Boolean {
        if (!file.exists()) {
            return false
        }
        val currentServers = readConfigFileServers(file)
        if (!currentServers.containsKey(serverName)) {
            return false
        }
        val updatedServers = currentServers.toMutableMap()
        updatedServers.remove(serverName)
        return runCatching {
            writeConfigFile(file, updatedServers)
            true
        }.getOrElse {
            LOG.warn("Failed to remove MCP server $serverName from ${file.absolutePath}: ${it.message}")
            false
        }
    }

    /**
     * 在编辑器中打开指定范围的 MCP 配置文件，文件不存在时自动创建
     *
     * @param project IntelliJ 项目实例
     * @param scope 配置范围（项目级或全局级）
     * @return 错误信息，成功时返回 null
     */
    fun openConfigFile(project: Project, scope: McpInstallScope): String? {
        return try {
            val configDir = when (scope) {
                McpInstallScope.PROJECT -> getProjectConfigDirectory(project)
                McpInstallScope.GLOBAL -> getUserConfigDirectory()
            }
            val configFile = Paths.get(configDir, MCP_CONFIG_DIRECTORY, CONFIG_FILE_NAME).toFile()
            if (!configFile.exists()) {
                configFile.parentFile?.mkdirs()
                configFile.writeText(DEFAULT_CONFIG_TEMPLATE)
            }
            openConfigFileInEditor(project, configFile)
            null
        } catch (e: Exception) {
            LOG.warn("Failed to open MCP config for scope $scope", e)
            e.message ?: "未知错误"
        }
    }

    /**
     * 生成项目缓存 key
     *
     * @param project IntelliJ 项目实例
     * @return 项目路径的标准化字符串
     */
    private fun projectKey(project: Project?): String {
        val basePath = project?.basePath ?: getCurrentProjectRootPath()
        return Paths.get(basePath).normalize().toString()
    }

    /**
     * 从配置文件中读取服务器配置
     *
     * @param file 配置文件
     * @param scope 配置范围
     * @return 服务器配置映射，解析失败时返回 null
     */
    private fun readConfigFile(file: File, scope: McpInstallScope?): Map<String, McpServerConfig>? {
        if (!file.exists()) {
            return emptyMap()
        }
        return try {
            val element = json.parseToJsonElement(file.readText())
            val serversNode = (element as? JsonObject)?.get("mcpServers")?.jsonObject ?: JsonObject(emptyMap())
            parseServers(serversNode, scope)
        } catch (e: Exception) {
            LOG.warn("Failed to load MCP config from ${file.absolutePath}: ${e.message}")
            null
        }
    }

    /**
     * 解析 JSON 对象中的服务器配置节点
     *
     * @param objectNode 包含服务器配置的 JSON 对象
     * @param scope 配置范围
     * @return 服务器名称到配置的映射
     */
    private fun parseServers(
        objectNode: JsonObject,
        scope: McpInstallScope?,
    ): Map<String, McpServerConfig> {
        return objectNode.mapValuesTo(LinkedHashMap()) { (serverName, element) ->
            val obj = element.jsonObject
            val legacyAuthEnabled = obj["useJarvisAuth"]?.jsonPrimitive?.booleanOrNull == true
            val explicitNote = obj["note"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val note = when {
                legacyAuthEnabled -> LEGACY_AUTH_NOTE
                explicitNote.isNotBlank() -> explicitNote
                else -> null
            }

            McpServerConfig(
                type = obj["type"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                url = obj["url"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                command = obj["command"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                args = obj["args"]?.jsonArray?.map { it.jsonPrimitive.contentOrNull.orEmpty() } ?: emptyList(),
                env = obj["env"]?.jsonObject?.mapValues { it.value.jsonPrimitive.contentOrNull.orEmpty() } ?: emptyMap(),
                disabled = if (legacyAuthEnabled) true else obj["disabled"]?.jsonPrimitive?.booleanOrNull,
                token = obj["token"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                headers = obj["headers"]?.jsonObject?.mapValues { it.value.jsonPrimitive.contentOrNull.orEmpty() } ?: emptyMap(),
                timeout = obj["timeout"]?.jsonPrimitive?.longOrNull,
                space = obj["space"]?.jsonPrimitive?.contentOrNull ?: scope?.spaceName,
                name = obj["name"]?.jsonPrimitive?.contentOrNull ?: serverName,
                note = note,
            )
        }
    }

    /**
     * 根据配置文件路径推断其所属范围（全局或项目级）
     *
     * @param file 配置文件
     * @return 推断出的配置范围
     */
    private fun inferScope(file: File): McpInstallScope? {
        val normalizedPath = file.absoluteFile.normalize().path
        val globalRoot = Paths.get(getUserConfigDirectory(), MCP_CONFIG_DIRECTORY).toFile().absoluteFile.normalize().path
        return if (normalizedPath.startsWith(globalRoot)) {
            McpInstallScope.GLOBAL
        } else {
            McpInstallScope.PROJECT
        }
    }

    /**
     * 标准化服务器配置，自动补全传输类型
     *
     * @param config 原始服务器配置
     * @return 标准化后的配置副本
     */
    private fun normalizeConfig(config: McpServerConfig): McpServerConfig {
        val resolvedType = when {
            config.type.isNotBlank() -> config.type
            config.command.isNotBlank() -> "stdio"
            config.url.isNotBlank() -> "streamableHttp"
            else -> "stdio"
        }
        return config.copy(type = resolvedType)
    }

    private fun McpServerConfig.isEnabled(): Boolean = disabled != true

    /**
     * 将内容强制写入文件并同步到磁盘，确保数据持久化
     *
     * @param file 目标文件
     * @param content 待写入的内容
     */
    private fun forceWriteAndSync(file: File, content: String) {
        FileOutputStream(file, false).channel.use { channel ->
            val bytes = content.toByteArray(StandardCharsets.UTF_8)
            val buffer = ByteBuffer.wrap(bytes)
            channel.truncate(0)
            while (buffer.hasRemaining()) {
                channel.write(buffer)
            }
            channel.force(true)
        }
    }

    /**
     * 在 IntelliJ 编辑器中打开指定文件
     *
     * @param project IntelliJ 项目实例
     * @param file 待打开的文件
     */
    private fun openConfigFileInEditor(project: Project?, file: File) {
        if (project == null) {
            return
        }
        ApplicationManager.getApplication().invokeLater {
            if (project.isDisposed) {
                return@invokeLater
            }
            val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file) ?: return@invokeLater
            FileUtil.reloadFilesFromDisk(virtualFile)
            FileEditorManager.getInstance(project).openFile(virtualFile, true)
        }
    }
}
