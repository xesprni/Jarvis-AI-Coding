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

@Serializable
data class McpConfig(
    val servers: Map<String, McpServerConfig> = emptyMap(),
)

enum class McpInstallScope(val displayName: String, val spaceName: String) {
    PROJECT("项目(Project)", "Project"),
    GLOBAL("全局(Global)", "Global");

    override fun toString(): String = displayName
}

object McpConfigManager {
    private data class ConfigCacheEntry(val config: McpConfig)

    private val cachedConfigs = ConcurrentHashMap<String, ConfigCacheEntry>()
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = false
    }

    private const val DEFAULT_CONFIG_TEMPLATE = "{\n  \"mcpServers\": {}\n}\n"
    private const val LEGACY_AUTH_NOTE =
        "Legacy useJarvisAuth was removed. Add explicit token or headers to re-enable."

    const val CONFIG_FILE_NAME = "mcp_settings.json"
    val LOG: Logger = Logger.getInstance(McpConfigManager::class.java)

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

    fun getConfig(project: Project? = null): McpConfig {
        val key = projectKey(project)
        return cachedConfigs[key]?.config ?: loadConfig(project)
    }

    fun getServerConfig(project: Project? = null, serverName: String): McpServerConfig? {
        return getConfig(project).servers[serverName]?.let(::normalizeConfig)
    }

    fun getEnabledServers(project: Project? = null): List<Pair<String, McpServerConfig>> {
        return getConfig(project).servers.mapNotNull { (name, config) ->
            val normalized = normalizeConfig(config)
            if (normalized.isEnabled()) name to normalized else null
        }
    }

    fun invalidate(project: Project? = null) {
        cachedConfigs.remove(projectKey(project))
    }

    fun readConfigFileServers(file: File): Map<String, McpServerConfig> {
        val scope = inferScope(file)
        return readConfigFile(file, scope).orEmpty()
    }

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

    private fun projectKey(project: Project?): String {
        val basePath = project?.basePath ?: getCurrentProjectRootPath()
        return Paths.get(basePath).normalize().toString()
    }

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

    private fun inferScope(file: File): McpInstallScope? {
        val normalizedPath = file.absoluteFile.normalize().path
        val globalRoot = Paths.get(getUserConfigDirectory(), MCP_CONFIG_DIRECTORY).toFile().absoluteFile.normalize().path
        return if (normalizedPath.startsWith(globalRoot)) {
            McpInstallScope.GLOBAL
        } else {
            McpInstallScope.PROJECT
        }
    }

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
