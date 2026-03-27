package com.qifu.agent.mcp

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.qifu.external.McpMarketServiceTemplate
import com.qifu.utils.*
import com.qifu.utils.file.FileUtil
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
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
    val useJarvisAuth: Boolean = false,
    val headers: Map<String, String> = emptyMap(),
    val timeout: Long? = null,
    var space: String?,
    val name: String? = null,
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

data class McpTemplateSelection(
    val template: McpMarketServiceTemplate.Template,
    val type: String
)

object McpConfigManager {
    private data class ConfigCacheEntry(
        val config: McpConfig,
    )

    private val cachedConfigs = ConcurrentHashMap<String, ConfigCacheEntry>()
    const val CONFIG_FILE_NAME = "mcp_settings.json"
    private const val DEFAULT_CONFIG_TEMPLATE = "{\n  \"mcpServers\": {\n    \n  }\n}"
    val LOG: Logger = Logger.getInstance(McpConfigManager::class.java)
    private val PLACEHOLDER_PATTERN = Regex("#\\{([^}]+)}")

    fun selectInstallTemplate(templateContainer: McpMarketServiceTemplate?): McpTemplateSelection? {
        val ordered = listOf(
            templateContainer?.npxTemplate to "npx",
            templateContainer?.uvxTemplate to "uvx",
            templateContainer?.dockerTemplate to "docker",
            templateContainer?.sseTemplate to "sse"
        )
        val (template, type) = ordered.firstOrNull { (template, _) ->
            template?.content?.isNotBlank() == true
        } ?: return null
        return McpTemplateSelection(template!!, type)
    }

    fun extractTemplatePlaceholders(content: String?): Set<String> {
        if (content.isNullOrBlank()) {
            return emptySet()
        }
        return PLACEHOLDER_PATTERN.findAll(content).map { it.groupValues[1] }.toSet()
    }

    private fun projectKey(project: Project?): String {
        val basePath = project?.basePath ?: getCurrentProjectRootPath()
        return Paths.get(basePath).normalize().toString()
    }

    fun loadConfig(project: Project? = null): McpConfig {
        val key = projectKey(project)
        val json = Json { ignoreUnknownKeys = true }
        val mergedServers = LinkedHashMap<String, McpServerConfig>()
        val userConfigFile = Paths.get(getUserConfigDirectory(),MCP_CONFIG_DIRECTORY, CONFIG_FILE_NAME).toFile()
        val projectConfigFile = Paths.get(getProjectConfigDirectory(), MCP_CONFIG_DIRECTORY,CONFIG_FILE_NAME).toFile()
        //先读取用户home配置
        userConfigFile.takeIf { it.exists() && userConfigFile.isValidJson() }.let {
            readConfigFile(userConfigFile, json)?.let { map ->
                map.forEach { (name, cfg) ->
                    val normalizeConfig = normalizeConfig(cfg)
                    normalizeConfig.space = "Global"
                    mergedServers[name] = normalizeConfig

                }
            }
        }
        //项目配置覆盖
        projectConfigFile.takeIf { it.exists() && userConfigFile.isValidJson() }.let {
            readConfigFile(projectConfigFile, json)?.let { map ->
                map.forEach { (name, cfg) ->
                    val normalizeConfig = normalizeConfig(cfg)
                    normalizeConfig.space = "Project"
                    mergedServers[name] = normalizeConfig
                }
            }
        }
        //如果配置文件有错误，则使用上一次正确的配置
        val finalConfig =
            if ((projectConfigFile.exists() && !projectConfigFile.isValidJson()) ||
                (userConfigFile.exists() && !userConfigFile.isValidJson())) {
                //使用上一次的配置
                cachedConfigs[key]?.config ?: McpConfig(emptyMap())
            } else {
                McpConfig(mergedServers)
            }
        cachedConfigs[key] = ConfigCacheEntry(finalConfig)
        LOG.debug("Loaded MCP config for $key from project file")
        return finalConfig
    }

    private fun readConfigFile(file: File, json: Json): Map<String, McpServerConfig>? {
        if (!file.exists()) {
            return null
        }
        return try {
            val content = file.readText()
            val element = json.parseToJsonElement(content)
            if (element is JsonObject && element.containsKey("mcpServers")) {
                parseMapSchema(element["mcpServers"]?.jsonObject ?: JsonObject(emptyMap()))
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            LOG.warn("Failed to load MCP config from ${file.absolutePath}: ${e.message}")
            null
        }
    }

    fun getConfig(project: Project? = null): McpConfig {
        val key = projectKey(project)
        return cachedConfigs[key]?.config ?: loadConfig(project)
    }

    fun getServerConfig(project: Project? = null, serverName: String): McpServerConfig? {
        return getConfig(project).servers[serverName]?.let { normalizeConfig(it) }
    }

    fun getEnabledServers(project: Project? = null): List<Pair<String, McpServerConfig>> {
        return getConfig(project).servers.mapNotNull { (name, cfg) ->
            val normalized = normalizeConfig(cfg)
            if (normalized.isEnabled()) name to normalized else null
        }
    }

    fun invalidate(project: Project? = null) {
        cachedConfigs.remove(projectKey(project))
    }

    /**
     * 安装MCP服务器配置
     * 根据模板解析配置并写入全局配置文件
     */
    fun installMcpServer(
        template: McpMarketServiceTemplate?,
        serverName: String,
        project: Project? = null,
        name: String? = null,
        scope: McpInstallScope = McpInstallScope.GLOBAL,
        parameterOverrides: Map<String, String> = emptyMap(),
    ){
        if (template == null) {
            LOG.warn("Service template is null, cannot install MCP server")
            return
        }
        val selection = selectInstallTemplate(template) ?: run {
            LOG.warn("No valid template found in service template")
            return
        }
        val content = selection.template.content
        //将content解析成TemplateContent
        if (content.isNullOrBlank()) {
            LOG.warn("Template content is empty, cannot install MCP server")
            return
        }
        val parameterList = selection.template.parameters ?: emptyList()
        // 解析模板内容并生成服务器配置
        val serverConfig = parseTemplateToConfig(
            content,
            selection.type,
            parameterList,
            scope,
            parameterOverrides,
            name
        )
        // 读取当前配置
        val configDir = when (scope) {
            McpInstallScope.PROJECT -> getProjectConfigDirectory(project)
            McpInstallScope.GLOBAL -> getUserConfigDirectory()
        }
        val configFile = Paths.get(configDir, MCP_CONFIG_DIRECTORY, CONFIG_FILE_NAME).toFile()
        val currentServers = readConfigFileServers(configFile)
        // 添加新的服务器配置
        val updatedServers = currentServers.toMutableMap()
        updatedServers[serverName] = serverConfig
        // 写入配置文件
        writeConfigFile(configFile, updatedServers)
        openConfigFileInEditor(project, configFile)
        LOG.info("Successfully installed MCP server: $serverName with type: ${selection.type} scope: ${scope.spaceName}")
    }

    /**
     * 解析模板内容为服务器配置
     */
    private fun parseTemplateToConfig(
        content: String,
        type: String,
        parameters: List<McpMarketServiceTemplate.Parameter>,
        scope: McpInstallScope,
        overrides: Map<String, String>,
        name: String? = null
    ): McpServerConfig {
        val templateContent = try {
            val json = Json { ignoreUnknownKeys = true }
            val replacedContent = replaceTemplateParameters(content, parameters, overrides)
            val jsonObject = json.parseToJsonElement(replacedContent).jsonObject
            McpMarketServiceTemplate.TemplateContent(
                disable = jsonObject["disable"]?.jsonPrimitive?.booleanOrNull ?: false,
                timeout = jsonObject["timeout"]?.jsonPrimitive?.doubleOrNull,
                type = jsonObject["type"]?.jsonPrimitive?.contentOrNull,
                command = jsonObject["command"]?.jsonPrimitive?.contentOrNull,
                args = jsonObject["args"]?.jsonArray?.map { it.jsonPrimitive.content },
                env = jsonObject["env"]?.jsonObject?.mapValues { it.value.jsonPrimitive.content },
                useJarvisAuth = jsonObject["useJarvisAuth"]?.jsonPrimitive?.booleanOrNull ?: false,
                url = jsonObject["url"]?.jsonPrimitive?.contentOrNull,
            )
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse template content: ${e.message}")
        }
        return when (type) {
            "npx", "uvx", "docker" -> {
                val cmd = templateContent.command
                    ?: throw IllegalArgumentException("Command cannot be blank for npx/uvx template")
                //判断系统是否安装了该命令
//                val installed = CommonUtil.isCommandAvailable(cmd)
//                if (!installed) {
//                    LOG.warn("Command '$cmd' not found on system PATH, will mark the server disabled.")
//                }
//                val disabledFlag = templateContent.disable || !installed
                McpServerConfig(
                    type = "stdio",
                    command = cmd,
                    args = templateContent.args?.takeIf { it.isNotEmpty() } ?: emptyList(),
                    env = templateContent.env ?: emptyMap(),
                    disabled = false,
                    timeout = templateContent.timeout?.toLong(),
                    space = scope.spaceName,
                    name = name,
                )
            }
            "sse" -> {
                // SSE 格式通常是一个 URL
                McpServerConfig(
                    type = "streamableHttp",
                    url = templateContent.url ?: throw IllegalArgumentException("URL cannot be blank for sse template"),
                    args = templateContent.args?.takeIf { it.isNotEmpty() } ?: emptyList(),
                    env = templateContent.env ?: emptyMap(),
                    disabled = false,
                    useJarvisAuth = templateContent.useJarvisAuth,
                    timeout = templateContent.timeout?.toLong(),
                    space = scope.spaceName,
                    name = name,
                )
            }
            else -> {
                throw IllegalArgumentException("Unsupported type $type")
            }
        }
    }

    private fun replaceTemplateParameters(
        content: String,
        parameters: List<McpMarketServiceTemplate.Parameter>,
        overrides: Map<String, String>
    ): String {
        var replacedContent = content
        val resolvedValues = LinkedHashMap<String, String>()
        parameters.forEach { param ->
            val name = param.name ?: return@forEach
            val defaultValue = param.defaultValue?.content
            if (defaultValue != null) {
                resolvedValues[name] = defaultValue
            }
        }
        overrides.forEach { (name, value) ->
            resolvedValues[name] = value
        }
        resolvedValues.forEach { (name, rawValue) ->
            val placeholder = "#${'{' }$name${'}'}"
            val safeValue = escapePlaceholderValue(rawValue)
            replacedContent = replacedContent.replace(placeholder, safeValue)
        }
        val remaining = extractTemplatePlaceholders(replacedContent)
        if (remaining.isNotEmpty()) {
            throw IllegalArgumentException("Missing values for parameters: ${remaining.joinToString(", ")}")
        }
        return replacedContent
    }

    private fun escapePlaceholderValue(value: String): String {
        if (value.isEmpty()) {
            return value
        }
        val builder = StringBuilder(value.length)
        value.forEach { ch ->
            when (ch) {
                '\\' -> builder.append("\\\\")
                '"' -> builder.append("\\\"")
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                '\t' -> builder.append("\\t")
                else -> builder.append(ch)
            }
        }
        return builder.toString()
    }

    /**
     * 读取配置文件中的服务器配置
     *
     * @param file 配置文件
     * @return 服务器配置映射
     */
    fun readConfigFileServers(file: File): Map<String, McpServerConfig> {
        if (!file.exists()) {
            return emptyMap()
        }
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val content = file.readText()
            val element = json.parseToJsonElement(content)
            if (element is JsonObject && element.containsKey("mcpServers")) {
                parseMapSchema(element["mcpServers"]?.jsonObject ?: JsonObject(emptyMap()))
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            LOG.warn("Failed to read config file: ${e.message}")
            emptyMap()
        }
    }

    /**
     * 写入配置文件
     *
     * @param file 配置文件
     * @param servers 服务器配置映射
     */
    fun writeConfigFile(file: File, servers: Map<String, McpServerConfig>) {
        try {
            file.parentFile?.mkdirs()
            val json = Json {
                prettyPrint = true
                encodeDefaults = false
            }
            val config = McpConfig(servers)
            val configJson = json.encodeToJsonElement(McpConfig.serializer(), config).jsonObject
            val rootJson = buildJsonObject {
                put("mcpServers", configJson["servers"] ?: JsonObject(emptyMap()))
            }
            val payload = json.encodeToString(JsonObject.serializer(), rootJson)
            forceWriteAndSync(file, payload)
            LOG.info("Successfully wrote config to ${file.absolutePath}")
        } catch (e: Exception) {
            LOG.warn("Failed to write config file: ${e.message}", e)
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
        return try {
            writeConfigFile(file, updatedServers)
            true
        } catch (e: Exception) {
            LOG.warn("Failed to remove MCP server $serverName from ${file.absolutePath}: ${e.message}")
            false
        }
    }

    private fun parseMapSchema(obj: JsonObject): Map<String, McpServerConfig> {
        return obj.mapValuesTo(LinkedHashMap()) { (_, element) ->
            val e = element.jsonObject
            McpServerConfig(
                type = e["type"]?.jsonPrimitive?.contentOrNull ?: "",
                url = e["url"]?.jsonPrimitive?.contentOrNull ?: "",
                command = e["command"]?.jsonPrimitive?.contentOrNull ?: "",
                args = e["args"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                env = e["env"]?.jsonObject?.mapValues { it.value.jsonPrimitive.content } ?: emptyMap(),
                disabled = e["disabled"]?.jsonPrimitive?.booleanOrNull,
                token = e["token"]?.jsonPrimitive?.contentOrNull ?: "",
                useJarvisAuth = e["useJarvisAuth"]?.jsonPrimitive?.booleanOrNull ?: false,
                headers = e["headers"]?.jsonObject?.mapValues { it.value.jsonPrimitive.content } ?: emptyMap(),
                timeout = e["timeout"]?.jsonPrimitive?.longOrNull,
                space = e["space"]?.jsonPrimitive?.contentOrNull ?: "Global",
                name= e["name"]?.jsonPrimitive?.contentOrNull
            )
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

    fun File.isValidJson(): Boolean {
        return try {
            Json.parseToJsonElement(this.readText())
            true
        } catch (_: Exception) {
            false
        }
    }

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
            val virtualFile = LocalFileSystem.getInstance()
                .refreshAndFindFileByIoFile(file)
                ?: return@invokeLater
            FileUtil.reloadFilesFromDisk(virtualFile)
            FileEditorManager.getInstance(project).openFile(virtualFile, true)
        }
    }

    /**
     * 打开 MCP 配置文件
     * 
     * @param project 当前项目
     * @param scope 配置作用域（项目级或全局）
     * @return 成功返回 null，失败返回错误信息
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

}
