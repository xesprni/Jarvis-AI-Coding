package com.qifu.agent.mcp
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.openapi.diagnostic.Logger
import com.qihoo.finance.lowcode.common.configuration.UserInfoPersistentState
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

class McpClient(
    clientInfo:Implementation = Implementation(name = "default-mcp-client", version = "1.0.0")
) : AutoCloseable {
    // Initialize MCP client
    private val mcp: Client = Client(clientInfo)
    private val LOG = Logger.getInstance(McpClient::class.java)

    // Connect to the server using the path to the server
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
        val transport = StdioClientTransport(
            input = process.inputStream.asSource().buffered(),
            output = process.outputStream.asSink().buffered(),
        )
        // Connect the MCP client to the server using the transport
        try {
            withTimeout(startupTimeout) {
                mcp.connect(transport)
            }
        } catch (t: Throwable) {
            process.destroyForcibly()
            throw t
        }
    }

    private fun DefaultRequest.DefaultRequestBuilder.applyMcpHeaders(serverConfig: McpServerConfig,block: DefaultRequest.DefaultRequestBuilder.() -> kotlin.Unit) {
        if (serverConfig.useJarvisAuth) {
            UserInfoPersistentState.getUserInfo().token?.let {
                header(HttpHeaders.Authorization, "Bearer jarvis::$it")
            }
        } else {
            header(HttpHeaders.Authorization, "Bearer jarvis::${serverConfig.token}")
        }

        if (serverConfig.headers.isNotEmpty()) {
            serverConfig.headers.forEach { (key, value) ->
                header(key, value)
            }
        }
    }


    private suspend fun connectMcpStreamableHttp(serverConfig: McpServerConfig) = withContext(Dispatchers.IO) {
        val baseUrl = serverConfig.url.ifEmpty { serverConfig.command }
        var sessionId: String? = null // 按规范需要在初始化后带回此 Header
        val http = HttpClient(CIO) {
            install(SSE)
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(HttpTimeout) {
                requestTimeoutMillis = calculateTimeoutMs(serverConfig.timeout)
                connectTimeoutMillis = 5_000
            }
            defaultRequest {
                //从redis中读取jarvis token 如果没有则读取配置文件中的token
                applyMcpHeaders(serverConfig){
                    sessionId?.let { header("Mcp-Session-Id", it) }
                    header(HttpHeaders.Accept, "*/*")
                }
            }
            install(HttpCallValidator) {
                validateResponse { response ->
                    response.headers["Mcp-Session-Id"]?.let { sessionId = it }
                }
            }
        }
        // 构建 Kotlin MCP 客户端，并用 Streamable HTTP 传输连接
        mcp.connect(StreamableHttpClientTransport(url = baseUrl, client = http))
    }

    private suspend fun connectMcpSse(serverConfig: McpServerConfig) = withContext(Dispatchers.IO) {
        val baseUrl = serverConfig.url.ifEmpty { serverConfig.command }
        val http = HttpClient(CIO) {
            install(SSE)
            install(HttpTimeout) {
                requestTimeoutMillis = calculateTimeoutMs(serverConfig.timeout)
                connectTimeoutMillis = 5_000
            }
            defaultRequest {
                //从redis中读取jarvis token 如果没有则读取配置文件中的token
                applyMcpHeaders(serverConfig){
                    header(HttpHeaders.Accept, "text/event-stream") // SSE 事件流
                    header(HttpHeaders.CacheControl, "no-store")
                }
            }
        }
        mcp.connect(SseClientTransport(urlString = baseUrl, client = http))
    }
    /**
     * 获取已连接的客户端实例
     */
    fun getClient(): Client {
        return mcp
    }

    override fun close() {
        runBlocking {
            mcp.close()
        }
    }

    private fun buildCommand(serverConfig: McpServerConfig): List<String> {
        return if (serverConfig.args.isNotEmpty()) {
            listOf(serverConfig.command) + serverConfig.args
        } else {
            listOf(serverConfig.command)
        }
    }

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
            addAll(fallbackExecutableDirectories().filter { File(it).exists() })
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

    private fun resolveExecutable(command: String): String {
        val commandFile = File(command)
        if (commandFile.isAbsolute) {
            if (!commandFile.exists() || !commandFile.canExecute()) {
                throw IOException("Command $command is not executable or does not exist")
            }
            return commandFile.absolutePath
        }
        PathEnvironmentVariableUtil.findExecutableInPathOnAnyOS(command)?.let { return it.absolutePath }
        val fallbackDirs = fallbackExecutableDirectories()
        fallbackDirs.map { File(it, command) }
            .firstOrNull { it.exists() && it.canExecute() }
            ?.let { return it.absolutePath }

        throw IOException("Executable '$command' not found. Provide an absolute path in the MCP configuration or ensure it is available on PATH.")
    }

    private fun fallbackExecutableDirectories(): List<String> {
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

    private fun calculateTimeoutMs(timeoutSeconds: Long?): Long {
        return timeoutSeconds?.takeIf { it > 0 }?.times(1000L) ?: CONNECT_TIMEOUT_MS
    }
}
