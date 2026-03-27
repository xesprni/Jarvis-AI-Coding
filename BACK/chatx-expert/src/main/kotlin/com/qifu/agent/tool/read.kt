package com.qifu.agent.tool

import com.intellij.openapi.project.Project
import com.qifu.agent.TaskState
import com.qifu.agent.parser.ToolSegment
import com.qifu.agent.parser.UiToolName
import com.qifu.utils.*
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import dev.langchain4j.model.chat.request.json.JsonRawSchema
import kotlinx.serialization.json.*
import java.io.File
import kotlin.math.roundToInt
import kotlin.reflect.KFunction

data class ReadToolOutput(
    val filePath: String,
    val content: String,
    val startLine: Int,
)

object ReadTool: Tool<ReadToolOutput> {

    val SPEC = ToolSpecification.builder()
        .name("Read")
        .description("""Reads a file from the local filesystem. You can access any file directly by using this tool.
Assume this tool is able to read all files on the machine. If the User provides a path to a file assume that path is valid. It is okay to read a file that does not exist; an error will be returned.

Usage:
- The file_path parameter must be an absolute path, not a relative path
- By default, it reads up to 2000 lines starting from the beginning of the file
- You can optionally specify a line offset and limit (especially handy for long files), but it's recommended to read the whole file by not providing these parameters
- Any lines longer than 2000 characters will be truncated
- Results are returned using cat -n format, with line numbers starting at 1
- This tool can only read files, not directories. To read a directory, use an ls command via the Bash tool.
- This tool can be used to read the content of a '.class' file at a specific path.
- You have the capability to call multiple tools in a single response. It is always better to speculatively read multiple files as a batch that are potentially useful.
- You will regularly be asked to read screenshots. If the user provides a path to a screenshot ALWAYS use this tool to view the file at the path. This tool will work with all temporary file paths like /var/folders/123/abc/T/TemporaryItems/NSIRD_screencaptureui_ZfB1tD/Screenshot.png
- If you read a file that exists but has empty contents you will receive a system reminder warning in place of file contents.
""")
        .parameters(JsonObjectSchema.builder()
            .addStringProperty("file_path", "The absolute path to the file to read")
            .addProperty("offset", JsonRawSchema.from("""{
                "type": ["integer", "null"],
                "description": "The line number to start reading from. Only provide if the file is too large to read at once"
            }"""))
            .addProperty("limit", JsonRawSchema.from("""{
                "type": ["integer", "null"],
                "description": "The number of lines to read. Only provide if the file is too large to read at once"
            }"""))
            .required("file_path", "offset", "limit")
            .build())
        .build()
    // Common image extensions
    val IMAGE_EXTENSIONS = setOf(".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp")
    const val MAX_OUTPUT_SIZE = 0.25 * 1024 * 1024 // 0.25MB in bytes

    override fun renderResultForAssistant(output: ReadToolOutput): String {
        if (output.content.isBlank()) {
            return "<system-reminder>Warning: the file exists but is shorter than the provided offset (1). The file has 1 lines.</system-reminder>"
        }
        var result = addLineNumbers(output.content, output.startLine)
        result = truncateToCharBudget(result)
        return result
    }

    override fun getToolSpecification(): ToolSpecification {
        return SPEC
    }

    override fun getExecuteFunc(): KFunction<ToolCallResult<ReadToolOutput>> {
        return ::execute
    }

    suspend fun execute(taskState: TaskState, file_path: String, offset: Int? = null, limit: Int? = 2000): ToolCallResult<ReadToolOutput> {
        val fullFilePath = resolveFilePath(file_path, taskState.project)
        val ext = getFileExtension(file_path)

        // Record file read for freshness tracking
        taskState.fileFreshnessService!!.recordFileRead(fullFilePath)

        if (ext in IMAGE_EXTENSIONS) {
            throw ToolExecutionException("Read tool does not support image files yet")
        }

        val lineOffset = if (offset == 0) 0 else offset?.minus(1) ?: 0
        val fileContent = PsiFileUtils.getFileContent(taskState.project, fullFilePath) ?: throw ToolExecutionException("File not found: $fullFilePath")
        val content = fileContent.lineSequence().drop(lineOffset).take(limit ?: 2000).joinToString("\n")
        val data = ReadToolOutput(fullFilePath, content, lineOffset + 1)
        val resultForAssistant = renderResultForAssistant(data)
        return ToolCallResult(
            type = "result",
            data = data,
            resultForAssistant = resultForAssistant,
        )
    }

    override suspend fun validateInput(input: JsonElement, taskState: TaskState) {
        (input as JsonObject).let {
            val filePath = it["file_path"]?.jsonPrimitive?.contentOrNull
            val offset = it["offset"]?.jsonPrimitive?.intOrNull
            val limit = it["limit"]?.jsonPrimitive?.intOrNull

            if (filePath == null) {
                throw MissingToolParameterException(getName(), "file_path")
            }
            if (isClassFilePath(filePath)) {
                return
            }

            val fullPath = resolveFilePath(filePath, taskState.project)
            val file = File(fullPath)
            if (!file.exists() || !file.isFile) {
                // Try to find a similar file with a different extension
                val similarFile = findSimilarFile(fullPath)
                var message = "File does not exist."
                // If we found a similar file, suggest it to the assistant
                if (similarFile != null) {
                    message += " Did you mean ${similarFile}?"
                }
                throw ToolParameterException(message)
            }

            val ext = file.extension.lowercase()

            // Skip size check for image files - they have their own size limits
            if (ext !in IMAGE_EXTENSIONS) {
                val fileSize = file.length()
                if (fileSize > MAX_OUTPUT_SIZE && offset == null && limit == null) {
                    throw ToolParameterException(formatFileSizeError(fileSize))
                }
            }
        }
    }

    override suspend fun handlePartialBlock(toolRequestId: String, partialArgs: Map<String, JsonField>, taskState: TaskState, isPartial: Boolean): ToolSegment? {
        if (isPartial) return null

        val offset = partialArgs["offset"]?.value?.toIntOrNull() ?: 0
        val lineOffset = if (offset == 0) 0 else offset.minus(1)
        val limit = partialArgs["limit"]?.value?.toIntOrNull() ?: 2000
        return ToolSegment(
            name = UiToolName.READ_FILE,
            toolCommand = partialArgs["file_path"]!!.value,
            params = mapOf(
                "start" to JsonPrimitive(lineOffset + 1),
                "end" to JsonPrimitive(lineOffset + limit),
                "agent_name" to JsonPrimitive(
                    if (taskState.agentId != "default") taskState.agentId else "Jarvis"
                ),
            )
        )
    }

    private fun formatFileSizeError(sizeInBytes: Number): String {
        return "File content (${(sizeInBytes.toDouble() / 1024).roundToInt()}KB) exceeds maximum allowed size (${(MAX_OUTPUT_SIZE / 1024).roundToInt()}KB). Please use offset and limit parameters to read specific portions of the file, or use the GrepTool to search for specific content."
    }

    private fun resolveFilePath(filePath: String, project: Project): String {
        return if (filePath.contains("://")) {
            toPosixPath(filePath)
        } else {
            normalizeFilePath(filePath, project)
        }
    }

    private fun getFileExtension(filePath: String): String {
        val sanitized = filePath.replace('\\', '/')
        val lastSegment = sanitized.substringAfterLast('/', sanitized)
        return lastSegment.substringAfterLast('.', "").lowercase()
    }

    private fun isClassFilePath(filePath: String): Boolean {
        return filePath.trim().lowercase().endsWith(".class")
    }
}
