package com.miracle.agent.tool

import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.Logger
import com.miracle.agent.AgentMessageType
import com.miracle.agent.JarvisSay
import com.miracle.agent.TaskState
import com.miracle.agent.parser.ToolSegment
import com.miracle.agent.parser.UiToolName
import com.miracle.ui.smartconversation.panels.DiffWindowHolder
import com.miracle.ui.smartconversation.settings.configuration.ChatMode
import com.miracle.utils.*
import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import org.apache.commons.text.StringEscapeUtils
import java.io.File
import kotlin.reflect.KFunction

data class WriteToolOutput(
    val type: String = "create",
    val filePath: String,
    val content: String,
    val structuredPatch: List<Hunk> = emptyList(),  // diff结构中的变更片段
)

/**
 * 写文件工具
 */
object WriteTool: Tool<WriteToolOutput> {

    private val LOG = Logger.getInstance(WriteTool::class.java)
    const val MAX_LINES_TO_RENDER_FOR_ASSISTANT = 20
    const val TRUNCATED_MESSAGE = "\n...[truncated]"
    val SPEC = ToolSpecification.builder()
        .name("Write")
        .description("""Writes a file to the local filesystem.

Usage:
- This tool will overwrite the existing file if there is one at the provided path.
- If this is an existing file, you MUST use the Read tool first to read the file's contents. This tool will fail if you did not read the file first.
- ALWAYS prefer editing existing files in the codebase. NEVER write new files unless explicitly required.
- NEVER proactively create documentation files (*.md) or README files. Only create documentation files if explicitly requested by the User.
- Only use emojis if the user explicitly requests it. Avoid writing emojis to files unless asked.
- **IMPORTANT: You MUST output the 'file_path' parameter BEFORE the 'content' parameter in the JSON arguments.**
""")
        .parameters(JsonObjectSchema.builder()
            .addStringProperty("file_path", "The absolute path to the file to write (must be absolute, not relative)")
            .addStringProperty("content", "The content to write to the file")
            .required("file_path", "content")
            .build())
        .build()

    override fun getToolSpecification(): ToolSpecification {
        return SPEC
    }

    override fun getExecuteFunc(): KFunction<ToolCallResult<WriteToolOutput>> {
        return ::execute
    }

    override fun renderResultForAssistant(output: WriteToolOutput): String {
        return when(output.type) {
            "create" -> "File created successfully at: ${output.filePath}"
            "update" -> {
                val lines = output.content.split(Regex("\\r?\\n"))
                val displayContent = if (lines.size > MAX_LINES_TO_RENDER_FOR_ASSISTANT) {
                    lines.take(MAX_LINES_TO_RENDER_FOR_ASSISTANT).joinToString("\n") + TRUNCATED_MESSAGE
                } else {
                    output.content
                }
                val numberedContent = addLineNumbers(displayContent, startLine = 1)
                """The file ${output.filePath} has been updated. Here's the result of running `cat -n` on a snippet of the edited file:\n$numberedContent"""
            }
            else -> "Unsupported operation type: ${output.type}"
        }
    }

    suspend fun execute(file_path: String, content: String, taskState: TaskState, toolRequest: ToolExecutionRequest): ToolCallResult<WriteToolOutput> {
        // 如果用户修改了内容，则提前返回
        taskState.askUserResponse?.let {
            return handleUserModifiedContent(file_path, taskState, toolRequest)
        }

        ensurePlanWriteAllowed(file_path, taskState)

        val fullFilePath = normalizeFilePath(file_path, taskState.project)
        // 使用 PSI API 写入文件
        val psiFile = PsiFileUtils.findPsiFile(taskState.project, file_path)
        psiFile?.let {
            PsiFileUtils.updatePsiFileContent(it, content, flush = true)
        } ?: run {
            PsiFileUtils.createPsiFile(taskState.project, fullFilePath, content, flush = true)
        }
        taskState.fileFreshnessService!!.recordFileEdit(fullFilePath, content)
        val data =  WriteToolOutput(
            type = "create",
            filePath = file_path,
            content = content,
        )

        // 用户同意执行，更新补全状态为已应用
        CodeAudit.markAccepted(toolRequest.id())

        return ToolCallResult(
            type = "result",
            data = data,
            resultForAssistant = renderResultForAssistant(data)
        )
    }

    fun handleUserModifiedContent(
        filePath: String,
        taskState: TaskState,
        toolRequest: ToolExecutionRequest
    ): ToolCallResult<WriteToolOutput> {
        val map: Map<String, String?> = Json.decodeFromString(taskState.askUserResponse?:"")

        val userEditSegment = ToolSegment(
            name = UiToolName.USER_EDIT,
            toolCommand = "user edits",
            toolContent = map["userEdit"].orEmpty(),
            params = emptyMap()
        )
        taskState.emit!!(JarvisSay(toolRequest.id(), AgentMessageType.TOOL, listOf(userEditSegment)))
        taskState.historyAiMessage.segments.add(userEditSegment)

        val parsedContent = parseDiffToContent(map["allEdit"]!!)
        CodeAudit.markAccepted(toolRequest.id())

        val data = WriteToolOutput(
            type = "create",
            filePath = filePath,
            content = parsedContent
        )
        return ToolCallResult(
            type = "result",
            data = data,
            resultForAssistant = renderResultForAssistant(data)
        )
    }

    override suspend fun validateInput(input: JsonElement, taskState: TaskState) {
        (input as JsonObject).let {
            val filePath = it["file_path"]?.jsonPrimitive?.contentOrNull ?: return
            ensurePlanWriteAllowed(filePath, taskState)
            val readFileTimestamps = taskState.fileFreshnessService!!.getReadTimestamps()

            val fullFilePath = normalizeFilePath(filePath, taskState.project)
            val file = File(fullFilePath)
            if (!file.exists()) {
                return
            }

            val readTimestamp = readFileTimestamps[fullFilePath]
                ?: throw ToolExecutionException("File has not been read yet. Read it first before writing to it.")

            val lastWriteTime = file.lastModified()
            if (lastWriteTime > readTimestamp) {
                throw ToolExecutionException(
                    message = "File has been modified since read, either by the user or by a linter. Read it again before attempting to write it."
                )
            }
        }
    }

    override suspend fun handlePartialBlock(toolRequestId: String, partialArgs: Map<String, JsonField>, taskState: TaskState, isPartial: Boolean): ToolSegment? {
        var filePath = partialArgs["file_path"]?.takeIf { it.isComplete }?.value ?: return null
        filePath = normalizeFilePath(filePath, taskState.project)
        val content = partialArgs["content"]?.value.let { StringEscapeUtils.unescapeJava(it) }?.let { replaceContentLineEnding(it, LineEndingType.LF) } ?: ""
        val fullPath = normalizeFilePath(filePath, taskState.project)
        val file = File(fullPath)
        var toolUiName = UiToolName.NEW_FILE_CREATED
        var oldString = ""
        if (file.exists()) {
            toolUiName = UiToolName.EDITED_EXISTING_FILE
            oldString = replaceContentLineEnding(file.readText(), LineEndingType.LF)
        }

        // 添加观测
        if (!isPartial) {
            CodeAudit.recordPreview(taskState.project, toolRequestId, filePath, content)

            // 检查是否需要自动显示 diff
            val settings = com.miracle.config.AutoApproveSettings.state
            val shouldShowDiff = !settings.actions.editFiles

            if (shouldShowDiff) {
                // 显示 diff 窗口必须在 EDT 中执行
                val fileName = filePath.substringAfterLast("/")
                withContext(Dispatchers.EDT) {
                    DiffWindowHolder.showDiffView(
                        taskState.project,
                        fileName,
                        oldString,
                        content,
                        taskState.convId,
                        filePath,
                        true
                    )
                }
            }
        }

        return ToolSegment(name = toolUiName, toolCommand = filePath, toolContent = content, params = mapOf(
            "oldString" to JsonPrimitive(oldString),
            "newString" to JsonPrimitive(content),
            "agent_name" to JsonPrimitive(
                if (taskState.agentId != "default") taskState.agentId else "Jarvis"
            ),
        ))
    }


    private fun ensurePlanWriteAllowed(filePath: String, taskState: TaskState) {
        if (taskState.chatMode != ChatMode.PLAN) return
        throw ToolExecutionException("Write is unavailable in Plan mode.")
    }
}
