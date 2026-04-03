package com.miracle.agent.mcp

import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.openapi.diagnostic.Logger
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

/**
 * MCP 协议客户端，负责与 MCP 服务器建立连接并管理通信。
 * 支持三种传输方式：stdio、sse、streamableHttp。
 *
 * @param clientInfo 客户端身份信息，默认为 "default-mcp-client"
 */
class McpClient(
    clientInfo: Implementation = Implementation(name = "default-mcp-client", version = "1.0.0")
) {
    /** MCP 协议客户端实例 */
    private val mcp: Client = Client(clientInfo)
    private val LOG = Logger.getInstance(McpClient::class.java)
    /** stdio 模式下的子进程引用，用于关闭时释放资源 */
    @Volatile
    private var process: Process? = null

    /**
     * 根据服务器配置连接到 MCP 服务器
     *
     * @param serverConfig MCP 服务器配置信息
     * @return 已连接的 MCP 客户端实例
     * @throws IllegalArgumentException 当服务器类型无效时
     * @throws Exception 连接失败时抛出异常
     */
    suspend fun connectToServer(serverConfig: McpServerConfig): Client {
        try {
            when (serverConfig.type) {
                "stdio" -> connectMcpStdio(serverConfig)
                "sse" -> connectMcpSse(serverConfig)
                "streamableHttp" -> connectMcpStreamableHttp(serverConfig)
                else -> throw IllegalArgumentException("Invalid server type: ${serverConfig.type}")
            }
        } catch (e: Exception) {
            LOG.warn("Failed to connect to MCP server: ",e)
            throw e
        }
        return mcp
    }

    /**
     * 通过 stdio 方式连接 MCP 服务器
     *
     * @param serverConfig MCP 服务器配置信息
     */
    private suspend fun connectMcpStdio(serverConfig: McpServerConfig) = withContext(Dispatchers.IO) {
        require(serverConfig.command.isNotEmpty()) { "Command cannot be null or empty" }
        val fullCommand = buildCommand(serverConfig)
        //兼容可能出现的 path 路径问题（Cannot run program "npx":error=2 ,No such file or directory）
        val resolvedCommand = resolveExecutable(fullCommand.first())
        val startupTimeout = calculateTimeoutMs(serverConfig.timeout)
        val process = ProcessBuilder(listOf(resolvedCommand) + fullCommand.drop(1)).apply {
            val env = environment()
            // 设置环境变量
            serverConfig.env.forEach { (key, value) ->
                env[key] = value
            }
            ensureCommandDirectoryInPath(env, resolvedCommand)
        }.start()
        this.process = process
        val transport = StdioClientTransport(
            input = process.inputStream.asSource().buffered(),
            output = process.outputStream.asSink().buffered(),
        )
        // Connect the MCP client to the server using the transport
        try {
            withTimeout(startupTimeout.milliseconds) {
                mcp.connect(transport)
            }
        } catch (t: Throwable) {
            this.process = null
            process.destroyForcibly()
            throw t
        }
    }

    /**
     * 为 HTTP 请求添加 MCP 认证头和自定义头信息
     *
     * @param serverConfig 包含 token 和自定义 headers 的服务器配置
     */
    private fun DefaultRequest.DefaultRequestBuilder.applyMcpHeaders(serverConfig: McpServerConfig) {
        if (serverConfig.token.isNotBlank()) {
            header(HttpHeaders.Authorization, "Bearer ${serverConfig.token}")
        }
        serverConfig.headers.forEach { (key, value) ->
            header(key, value)
        }
    }

    /**
     * 通过 Streamable HTTP 方式连接 MCP 服务器
     *
     * @param serverConfig MCP 服务器配置信息
     */
    private suspend fun connectMcpStreamableHttp(serverConfig: McpServerConfig) = withContext(Dispatchers.IO) {
        val baseUrl = serverConfig.url.ifEmpty { serverConfig.command }
        var sessionId: String? = null
        val http = HttpClient(CIO) {
            install(SSE)
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(HttpTimeout) {
                requestTimeoutMillis = calculateTimeoutMs(serverConfig.timeout)
                connectTimeoutMillis = 5_000
            }
            defaultRequest {
                applyMcpHeaders(serverConfig)
                sessionId?.let { header("Mcp-Session-Id", it) }
                header(HttpHeaders.Accept, "*/*")
            }
            install(HttpCallValidator) {
                validateResponse { response ->
                    response.headers["Mcp-Session-Id"]?.let { sessionId = it }
                }
            }
        }
        mcp.connect(StreamableHttpClientTransport(url = baseUrl, client = http))
    }

    /**
     * 通过 SSE（Server-Sent Events）方式连接 MCP 服务器
     *
     * @param serverConfig MCP 服务器配置信息
     */
    private suspend fun connectMcpSse(serverConfig: McpServerConfig) = withContext(Dispatchers.IO) {
        val baseUrl = serverConfig.url.ifEmpty { serverConfig.command }
        val http = HttpClient(CIO) {
            install(SSE)
            install(HttpTimeout) {
                requestTimeoutMillis = calculateTimeoutMs(serverConfig.timeout)
                connectTimeoutMillis = 5_000
            }
            defaultRequest {
                applyMcpHeaders(serverConfig)
                header(HttpHeaders.Accept, "text/event-stream")
                header(HttpHeaders.CacheControl, "no-store")
            }
        }
        mcp.connect(SseClientTransport(urlString = baseUrl, client = http))
    }

    /**
     * 获取 MCP 客户端实例
     *
     * @return MCP 客户端实例
     */
    fun getClient(): Client {
        return mcp
    }

    /**
     * 关闭 MCP 客户端连接并释放资源
     */
    suspend fun close() {
        runCatching { mcp.close() }
        process?.let {
            this.process = null
            it.destroyForcibly()
        }
    }

    /**
     * 将服务器配置中的命令和参数组装为完整的命令列表
     *
     * @param serverConfig MCP 服务器配置信息
     * @return 命令及其参数的列表
     */
    private fun buildCommand(serverConfig: McpServerConfig): List<String> {
        return if (serverConfig.args.isNotEmpty()) {
            listOf(serverConfig.command) + serverConfig.args
        } else {
            listOf(serverConfig.command)
        }
    }

    /**
     * 确保可执行文件所在目录已加入 PATH 环境变量中，避免子进程找不到相关依赖
     *
     * @param environment 进程环境变量映射
     * @param commandPath 可执行文件的绝对路径
     */
    private fun ensureCommandDirectoryInPath(environment: MutableMap<String, String>, commandPath: String) {
        val commandDir = File(commandPath).parentFile?.absolutePath ?: return
        val pathKey = environment.keys.firstOrNull { it.equals("PATH", ignoreCase = true) } ?: "PATH"
        val separator = File.pathSeparator
        val existingEntries = environment[pathKey]
            ?.split(separator)
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        val orderedEntries = buildList {
            add(commandDir)
            addAll(fallbackDirs.filter { File(it).exists() })
            addAll(existingEntries)
        }
        val seen = mutableSetOf<String>()
        val normalizedEntries = buildList {
            orderedEntries.forEach { entry ->
                val trimmed = entry.trim()
                if (trimmed.isEmpty()) return@forEach
                val normalized = File(trimmed).absolutePath
                if (seen.add(normalized)) {
                    add(trimmed)
                }
            }
        }
        environment[pathKey] = normalizedEntries.joinToString(separator)
    }

    /**
     * 解析可执行文件的完整路径，支持绝对路径、PATH 查找和常见包管理器目录回退
     *
     * @param command 待解析的命令名称或路径
     * @return 可执行文件的绝对路径
     * @throws IOException 当可执行文件未找到或不可执行时
     */
    private fun resolveExecutable(command: String): String {
        val commandFile = File(command)
        if (commandFile.isAbsolute) {
            if (!commandFile.exists() || !commandFile.canExecute()) {
                throw IOException("Command $command is not executable or does not exist")
            }
            return commandFile.absolutePath
        }
        PathEnvironmentVariableUtil.findExecutableInPathOnAnyOS(command)?.let { return it.absolutePath }
        val candidateDirs = fallbackDirs
        candidateDirs.map { File(it, command) }
            .firstOrNull { it.exists() && it.canExecute() }
            ?.let { return it.absolutePath }

        throw IOException("Executable '$command' not found. Provide an absolute path in the MCP configuration or ensure it is available on PATH.")
    }

    /**
     * 获取常见包管理器和运行时的可执行文件目录列表，作为 PATH 查找的回退方案
     * 使用 lazy 缓存避免重复构建
     */
    private val fallbackDirs: List<String> by lazy { buildFallbackDirectories() }

    private fun buildFallbackDirectories(): List<String> {
        val home = System.getProperty("user.home")
        val osName = System.getProperty("os.name")?.lowercase()
        return buildList {
            add("/usr/local/bin")
            add("/usr/bin")
            add("/opt/homebrew/bin")
            add("/usr/local/sbin")
            System.getenv("NVM_BIN")?.let { add(it) }
            System.getenv("PNPM_HOME")?.let { add(it) }
            System.getenv("VOLTA_HOME")?.let { add("$it/bin") }
            if (home != null) {
                add("$home/.local/bin")
                add("$home/.npm-global/bin")
                add("$home/.nvm/versions/node/current/bin")
                add("$home/.nodebrew/current/bin")
                add("$home/.volta/bin")
            }
            if (osName != null && osName.contains("windows")) {
                add("C:/Program Files/nodejs")
                add("C:/Program Files (x86)/nodejs")
            }
        }
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 30_000L
    }

    /**
     * 根据配置的超时秒数计算毫秒级超时时间，未配置时使用默认值
     *
     * @param timeoutSeconds 配置的超时秒数，为 null 或小于等于 0 时使用默认值
     * @return 超时时间（毫秒）
     */
    private fun calculateTimeoutMs(timeoutSeconds: Long?): Long {
        return timeoutSeconds?.takeIf { it > 0 }?.times(1000L) ?: CONNECT_TIMEOUT_MS
    }
}
