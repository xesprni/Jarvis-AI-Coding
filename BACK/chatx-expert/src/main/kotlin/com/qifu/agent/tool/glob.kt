package com.qifu.agent.tool

import com.qifu.agent.AgentMessageType
import com.qifu.agent.JarvisSay
import com.qifu.agent.TaskState
import com.qifu.agent.parser.ToolSegment
import com.qifu.agent.parser.UiToolName
import com.qifu.external.RipGrepUtil
import com.qifu.utils.JsonField
import com.qifu.utils.getCurrentProjectRootPath
import com.qifu.utils.normalizeFilePath
import com.qifu.utils.toPosixPath
import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.nio.file.FileSystems
import kotlin.reflect.KFunction

data class GlobToolOutput(
    val filenames: List<String>,
    val numFiles: Int,
    val truncated: Boolean,
    val durationMs: Long
)

/**
 * 文件模式匹配工具
 * 用于根据glob模式快速查找匹配的文件
 * 基于 JetBrains PSI API 实现
 */
object GlobTool: Tool<GlobToolOutput> {

    const val DEFAULT_LIMIT = 100

    val SPEC = ToolSpecification.builder()
        .name("Glob")
        .description("""
- Fast file pattern matching tool that works with any codebase size
- Supports glob patterns like "**/*.js" or "src/**/*.ts"
- Supports FIND and SEARCH CLASS within the project dependency libraries
- Returns matching file paths sorted by modification time
- Use this tool when you need to find files by name patterns
- When you are doing an open ended search that may require multiple rounds of globbing and grepping, use the Agent tool instead
- You have the capability to call multiple tools in a single response. It is always better to speculatively perform multiple searches as a batch that are potentially useful.
        """.trimMargin())
        .parameters(JsonObjectSchema.builder()
            .addStringProperty("pattern", "The glob pattern to match files against")
            .addStringProperty("path", "The directory to search in. If not specified, the current working directory will be used. IMPORTANT: Omit this field to use the default directory. DO NOT enter \"undefined\" or \"null\" - simply omit it for the default behavior. Must be a valid directory path if provided.")
            .required("pattern")
            .build())
        .build()

    override fun getToolSpecification(): ToolSpecification {
        return SPEC
    }

    override fun getExecuteFunc(): KFunction<ToolCallResult<GlobToolOutput>> {
        return ::execute
    }

    override suspend fun handlePartialBlock(toolRequestId: String, partialArgs: Map<String, JsonField>, taskState: TaskState, isPartial: Boolean): ToolSegment? {
        if (isPartial) return null

        val pattern = partialArgs["pattern"]!!.value
        val path = partialArgs["path"]?.value ?: getCurrentProjectRootPath()
        return renderToolSegment(pattern, path, taskState, null)
    }

    override fun renderResultForAssistant(output: GlobToolOutput): String {
        var result = if (output.filenames.isEmpty()) {
            "No files found"
        } else {
            output.filenames.joinToString("\n")
        }

        // 只有在结果被截断时才添加截断消息
        if (output.truncated) {
            result += "\n(Results are truncated. Consider using a more specific path or pattern.)"
        }

        return result
    }

    fun renderToolSegment(pattern: String, path: String?, taskState: TaskState, output: GlobToolOutput?): ToolSegment {
        // 构建搜索信息展示
        val content = output?.let {
            buildString {
                append("Found ${output.numFiles} ${if(output.numFiles == 0 || output.numFiles > 1) "files" else "file"} in ${output.durationMs}ms\n")
                append(output.filenames.joinToString("\n"))
                if (output.truncated) {
                    append("\n(Results are truncated. Consider using a more specific path or pattern.)")
                }
            }
        }.orEmpty()

        val segment = ToolSegment(
            name = UiToolName.GLOB_FILES,
            toolCommand = path ?: getCurrentProjectRootPath(),
            toolContent = content,
            params = mapOf(
                "pattern" to JsonPrimitive(pattern),
                "agent_name" to JsonPrimitive(
                    if (taskState.agentId != "default") taskState.agentId else "Jarvis"
                )
            ),
        )
        if (content.isNotBlank()) {
            taskState.historyAiMessage.segments.last()
                .let {
                    if ((it is ToolSegment) && (it.name == UiToolName.GLOB_FILES)) {
                        taskState.historyAiMessage.segments.removeLast()
                    }
                    taskState.historyAiMessage.segments.add(segment)
                }
        }
        return segment
    }

    override suspend fun validateInput(input: JsonElement, taskState: TaskState) {
        (input as JsonObject).let {
            val pattern = it["pattern"]?.jsonPrimitive?.contentOrNull
            val path = it["path"]?.jsonPrimitive?.contentOrNull
            
            if (pattern == null || pattern.isBlank()) {
                throw MissingToolParameterException(getName(), "pattern")
            }
            
            // 如果提供了 path 参数，验证其是否存在且为目录
            if (path != null) {
                val normalizedPath = normalizeFilePath(path, taskState.project)
                val file = File(normalizedPath)
                if (!file.exists()) {
                    throw ToolParameterException("Path does not exist: $path")
                }
                if (!file.isDirectory) {
                    throw ToolParameterException("Path is not a directory: $path")
                }
            }
        }
    }

    suspend fun execute(pattern: String, path: String? = null, taskState: TaskState, toolRequest: ToolExecutionRequest): ToolCallResult<GlobToolOutput> {
        val start = System.currentTimeMillis()
        val cwd = toPosixPath(getCurrentProjectRootPath())

        val files = RipGrepUtil.glob(pattern, path, cwd).toList()
        val numFiles = files.size
        val truncated = numFiles > DEFAULT_LIMIT
        val filenames = files.take(DEFAULT_LIMIT).map {
            normalizeFilePath(it, taskState.project)
        }.toList()
        val durationMs = System.currentTimeMillis() - start
        val output = GlobToolOutput(
            filenames = filenames,
            numFiles = numFiles,
            truncated = truncated,
            durationMs = durationMs
        )

        taskState.emit!!(JarvisSay(
            id = toolRequest.id(),
            type = AgentMessageType.TOOL,
            data = listOf(renderToolSegment(pattern, path, taskState, output))
        ))
        return ToolCallResult(
            type = "result",
            data = output,
            resultForAssistant = renderResultForAssistant(output)
        )
    }

    private data class GlobResult(
        val files: List<String> = emptyList(),
        val truncated: Boolean = false
    )

}