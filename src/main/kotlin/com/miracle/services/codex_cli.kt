package com.miracle.services

import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.awaitExit
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.ToolExecutionResultMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.chat.request.ResponseFormatType
import dev.langchain4j.model.chat.response.ChatResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.IOException
import java.nio.file.Files

data class CodexCliTurnResult(
    val threadId: String?,
    val text: String,
)

object CodexCliService {

    private val LOG = Logger.getInstance("com.miracle.services.CodexCliService")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun runConversationTurn(
        modelConfig: ModelConfig,
        prompt: String,
        projectPath: String?,
        threadId: String?,
    ): CodexCliTurnResult {
        return runCodexCommand(
            modelConfig = modelConfig,
            prompt = prompt,
            projectPath = projectPath,
            threadId = threadId,
            ephemeral = false,
        )
    }

    suspend fun completeChatRequest(
        modelConfig: ModelConfig,
        request: ChatRequest,
    ): ChatResponse {
        val result = runCodexCommand(
            modelConfig = modelConfig,
            prompt = buildPromptFromRequest(request),
            projectPath = null,
            threadId = null,
            ephemeral = true,
        )
        return ChatResponse.builder()
            .aiMessage(AiMessage(result.text))
            .id(result.threadId)
            .modelName(modelConfig.model)
            .build()
    }

    private suspend fun runCodexCommand(
        modelConfig: ModelConfig,
        prompt: String,
        projectPath: String?,
        threadId: String?,
        ephemeral: Boolean,
    ): CodexCliTurnResult = withContext(Dispatchers.IO) {
        val executable = resolveExecutable()
        val outputFile = Files.createTempFile("jarvis-codex-last-message", ".txt").toFile()
        val command = buildCommand(
            executable = executable,
            modelConfig = modelConfig,
            projectPath = projectPath,
            threadId = threadId,
            outputFile = outputFile,
            ephemeral = ephemeral,
        )
        val process = ProcessBuilder(command).apply {
            redirectErrorStream(true)
            projectPath?.takeIf { it.isNotBlank() }?.let { directory(File(it)) }
        }.start()

        val cancellationHandle = currentCoroutineContext().job.invokeOnCompletion {
            runCatching { process.destroyForcibly() }
        }

        var resolvedThreadId = threadId
        var fallbackAssistantText: String? = null
        val rawLines = mutableListOf<String>()

        try {
            process.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(prompt)
                writer.flush()
            }

            process.inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.forEach { line ->
                    rawLines += line
                    val event = parseEvent(line) ?: return@forEach
                    resolvedThreadId = extractThreadId(event) ?: resolvedThreadId
                    fallbackAssistantText = extractAssistantText(event) ?: fallbackAssistantText
                }
            }

            val exitCode = process.awaitExit()
            currentCoroutineContext().ensureActive()

            val outputText = outputFile.takeIf { it.exists() }?.readText()?.trim().orEmpty()
            val finalText = outputText.ifBlank { fallbackAssistantText.orEmpty().trim() }
            if (exitCode != 0) {
                throw IOException(buildFailureMessage(rawLines))
            }
            if (finalText.isBlank()) {
                throw IOException("Codex CLI 未返回有效内容。")
            }

            CodexCliTurnResult(
                threadId = resolvedThreadId,
                text = finalText,
            )
        } finally {
            cancellationHandle.dispose()
            runCatching { process.destroy() }
            runCatching { outputFile.delete() }
        }
    }

    private fun buildCommand(
        executable: String,
        modelConfig: ModelConfig,
        projectPath: String?,
        threadId: String?,
        outputFile: File,
        ephemeral: Boolean,
    ): List<String> {
        val command = mutableListOf(executable, "exec")
        if (!threadId.isNullOrBlank()) {
            command += listOf("resume", threadId)
        }

        command += listOf("--json", "--output-last-message", outputFile.absolutePath, "--skip-git-repo-check")
        if (ephemeral) {
            command += "--ephemeral"
        }

        if (threadId.isNullOrBlank()) {
            if (!projectPath.isNullOrBlank()) {
                command += listOf("-C", projectPath)
            }
            command += if (ephemeral) listOf("-s", "read-only") else listOf("--full-auto")
        } else if (!ephemeral) {
            command += "--full-auto"
        }

        modelConfig.model.trim().takeIf { it.isNotBlank() }?.let {
            command += listOf("-m", it)
        }
        command += "-"
        return command
    }

    private fun buildPromptFromRequest(request: ChatRequest): String {
        val blocks = mutableListOf<String>()
        request.messages().forEach { message ->
            renderMessage(message)?.let(blocks::add)
        }

        request.responseFormat()?.let { format ->
            if (format.type() == ResponseFormatType.JSON) {
                val schemaText = format.jsonSchema()?.toString()?.takeIf { it.isNotBlank() }
                blocks += buildString {
                    append("[Output]")
                    append("\nReturn valid JSON only. Do not add markdown fences or extra commentary.")
                    schemaText?.let {
                        append("\nSchema: ")
                        append(it)
                    }
                }
            }
        }

        request.toolSpecifications()?.takeIf { it.isNotEmpty() }?.let { toolSpecs ->
            val names = toolSpecs.mapNotNull { it.name() }.takeIf { it.isNotEmpty() } ?: return@let
            blocks += buildString {
                append("[Tools]")
                append("\nThe original request expected these tools: ")
                append(names.joinToString(", "))
                append("\nThis Codex CLI bridge cannot forward plugin tools for one-shot calls, so answer directly.")
            }
        }

        return blocks.joinToString("\n\n").trim()
    }

    private fun renderMessage(message: ChatMessage): String? {
        return when (message) {
            is SystemMessage -> "[System]\n${message.text()}"
            is UserMessage -> "[User]\n${renderUserMessage(message)}"
            is AiMessage -> message.text()?.takeIf { it.isNotBlank() }?.let { "[Assistant]\n$it" }
            is ToolExecutionResultMessage -> {
                val toolName = message.toolName()?.ifBlank { "tool" } ?: "tool"
                "[Tool:$toolName]\n${message.text()}"
            }
            else -> null
        }
    }

    private fun renderUserMessage(message: UserMessage): String {
        if (message.hasSingleText()) {
            return message.singleText()
        }
        return message.contents().joinToString("\n") { it.toString() }
    }

    private fun parseEvent(line: String): JsonObject? {
        val trimmed = line.trim()
        if (!trimmed.startsWith("{")) return null
        return runCatching { json.parseToJsonElement(trimmed).jsonObject }
            .onFailure { LOG.debug("skip non-event codex line: $trimmed") }
            .getOrNull()
    }

    private fun extractThreadId(event: JsonObject): String? {
        val type = event["type"]?.jsonPrimitive?.contentOrNull ?: return null
        if (type != "thread.started") return null
        return event["thread_id"]?.jsonPrimitive?.contentOrNull
            ?: event["thread"]?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
    }

    private fun extractAssistantText(event: JsonObject): String? {
        val type = event["type"]?.jsonPrimitive?.contentOrNull ?: return null
        if (type != "item.completed") return null
        val item = event["item"]?.jsonObject ?: return null
        if (item["type"]?.jsonPrimitive?.contentOrNull != "agent_message") return null

        item["text"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }?.let { return it }

        val content = item["content"]?.jsonArray ?: return null
        val text = content.mapNotNull { node ->
            val obj = node.jsonObject
            obj["text"]?.jsonPrimitive?.contentOrNull
                ?: obj["content"]?.jsonPrimitive?.contentOrNull
        }.joinToString("\n").trim()
        return text.takeIf { it.isNotBlank() }
    }

    private fun resolveExecutable(): String {
        PathEnvironmentVariableUtil.findExecutableInPathOnAnyOS("codex")?.let { return it.absolutePath }
        fallbackDirectories().map { File(it, "codex") }
            .firstOrNull { it.exists() && it.canExecute() }
            ?.let { return it.absolutePath }
        throw IOException("未找到 codex CLI。请先在系统终端安装 Codex 并执行 codex login。")
    }

    private fun fallbackDirectories(): List<String> {
        val home = System.getProperty("user.home")
        return buildList {
            add("/opt/homebrew/bin")
            add("/usr/local/bin")
            add("/usr/bin")
            add("/usr/local/sbin")
            System.getenv("PNPM_HOME")?.let(::add)
            System.getenv("VOLTA_HOME")?.let { add("$it/bin") }
            home?.let {
                add("$it/.local/bin")
                add("$it/.volta/bin")
            }
        }
    }

    private fun buildFailureMessage(rawLines: List<String>): String {
        val detail = rawLines.asReversed()
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(3)
            .toList()
            .asReversed()
            .joinToString("\n")
            .takeIf { it.isNotBlank() }
        return if (detail == null) {
            "Codex CLI 执行失败。若尚未登录，请先在系统终端执行 codex login。"
        } else {
            "Codex CLI 执行失败。\n$detail\n\n若尚未登录，请先在系统终端执行 codex login。"
        }
    }
}
