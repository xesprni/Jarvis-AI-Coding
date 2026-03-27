package com.qifu.agent.tool

import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.Logger
import com.qifu.agent.AgentMessageType
import com.qifu.agent.JarvisSay
import com.qifu.agent.TaskState
import com.qifu.agent.parser.ToolSegment
import com.qifu.agent.parser.UiToolName
import com.qifu.ui.smartconversation.panels.DiffWindowHolder
import com.qifu.utils.*
import com.qihoo.finance.lowcode.aiquestion.util.ChatUtil
import com.qihoo.finance.lowcode.common.constants.CompletionType
import com.qihoo.finance.lowcode.common.entity.enums.CompletionStatus
import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import kotlin.reflect.KFunction

data class EditToolOutput(
    val filePath: String,
    val oldString: String,
    val newString: String,
    val originalContent: String,
    val newContent: String,
    val structuredPatch: List<Hunk>,
    val operation: String // "create", "update", "delete"
)

/**
 * 文件编辑工具
 */
object EditTool: Tool<EditToolOutput> {

    const val N_LINES_SNIPPET = 4
    const val MAX_LINES_TO_RENDER_FOR_ASSISTANT = 20
    const val TRUNCATED_MESSAGE = "\n...[truncated]"
    private val LOG = Logger.getInstance(EditTool::class.java)

    val SPEC = ToolSpecification.builder()
        .name("Edit")
        .description("""
Performs exact string replacements in files. 

Usage:
- You must use your `Read` tool at least once in the conversation before editing. This tool will error if you attempt an edit without reading the file. 
- When editing text from Read tool output, ensure you preserve the exact indentation (tabs/spaces) as it appears AFTER the line number prefix. The line number prefix format is: spaces + line number + tab. Everything after that tab is the actual file content to match. Never include any part of the line number prefix in the old_string or new_string.
- ALWAYS prefer editing existing files in the codebase. NEVER write new files unless explicitly required.
- Only use emojis if the user explicitly requests it. Avoid adding emojis to files unless asked.
- The edit will FAIL if `old_string` is not unique in the file. Either provide a larger string with more surrounding context to make it unique or use `replace_all` to change every instance of `old_string`. 
- Use `replace_all` for replacing and renaming strings across the file. This parameter is useful if you want to rename a variable for instance.
""".trimIndent())
        .parameters(JsonObjectSchema.builder()
            .addStringProperty("file_path", "The absolute path to the file to modify")
            .addStringProperty("old_string", "The text to replace")
            .addStringProperty("new_string", "The text to replace it with (must be different from old_string)")
            .addBooleanProperty("replace_all", "Replace all occurences of old_string (default false)")
            .required("file_path", "old_string", "new_string")
            .build())
        .build()

    override fun getToolSpecification(): ToolSpecification {
        return SPEC
    }

    override fun getExecuteFunc(): KFunction<ToolCallResult<EditToolOutput>> {
        return ::execute
    }

    override fun renderResultForAssistant(output: EditToolOutput): String {
        val (snippet, startLine) = if (output.operation == "user_edit") {
            getUserEditSnippet(output.newContent)
        } else {
            getSnippet(output.originalContent, output.oldString, output.newString)
        }
        
        val prefix = if (output.operation == "user_edit") {
            "The file ${output.filePath} has been updated (The user modified some of the content.)."
        } else {
            "The file ${output.filePath} has been updated."
        }
        
        return "$prefix Here's the result of running `cat -n` on a snippet of the edited file:\n${addLineNumbers(snippet, startLine)}"
    }

    suspend fun execute(file_path: String, old_string: String, new_string: String, replace_all: Boolean? = false,
                        taskState: TaskState, toolRequest: ToolExecutionRequest): ToolCallResult<EditToolOutput> {
        val safeReplaceAll = replace_all ?: false
        // 如果用户修改了内容，则提前返回
        taskState.askUserResponse?.let {
            return handleUserModifiedContent(file_path, taskState, toolRequest, old_string, new_string)
        }
        val fullFilePath = normalizeFilePath(file_path, taskState.project)

        // 读取原文的LineEnding，先将原文替换成LF
        val psiFile = PsiFileUtils.findPsiFile(taskState.project, file_path) ?: throw ToolExecutionException("File not found: $fullFilePath")
        var originalContent = PsiFileUtils.getDocument(psiFile)!!.text
        val lineEnding = detectContentLineEnding(originalContent)
        if (lineEnding == LineEndingType.CRLF) originalContent = replaceContentLineEnding(originalContent, LineEndingType.LF)

        // 将replace的内容替换成LF，并执行替换
        val oldString = replaceContentLineEnding(old_string, LineEndingType.LF)
        val newString = replaceContentLineEnding(new_string, LineEndingType.LF)
        var newContent = if (safeReplaceAll) originalContent.replace(oldString, newString)
        else originalContent.replaceFirst(old_string, new_string)

        // 还原 LineEnding 后，更新文件
        if (lineEnding == LineEndingType.CRLF) newContent = replaceContentLineEnding(newContent, LineEndingType.CRLF)
        PsiFileUtils.updatePsiFileContent(psiFile, newContent, flush = true)

        taskState.fileFreshnessService?.recordFileEdit(fullFilePath, newContent)
        val patch = getPatch(fullFilePath, originalContent, old_string, new_string)
        // 用户同意执行更新操作
        ChatUtil.updateCodeCompletionLogStatus(toolRequest.id(), CompletionStatus.ACCEPT)
        val data = EditToolOutput(
            filePath = fullFilePath,
            oldString = old_string,
            newString = new_string,
            originalContent = originalContent,
            newContent = newContent,
            structuredPatch = patch,
            operation = if (safeReplaceAll) "replace_all" else "replace"
        )

        return ToolCallResult(
            type = "result",
            data = data,
            resultForAssistant = renderResultForAssistant(data)
        )
    }
    override suspend fun handlePartialBlock(toolRequestId: String, partialArgs: Map<String, JsonField>, taskState: TaskState, isPartial: Boolean): ToolSegment? {
        if (isPartial) return null

        val filePath = normalizeFilePath(partialArgs["file_path"]!!.value, taskState.project)
        var oldContent = PsiFileUtils.getFileContent(taskState.project, filePath) ?: throw ToolExecutionException("File not found: $filePath")
        oldContent = replaceContentLineEnding(oldContent, LineEndingType.LF)
        val oldString = replaceContentLineEnding(partialArgs["old_string"]!!.value, LineEndingType.LF)
        val newString = replaceContentLineEnding(partialArgs["new_string"]!!.value, LineEndingType.LF)
        val replaceAll = partialArgs["replace_all"]?.value?.toBoolean() ?: false
        val newContent = if (replaceAll) oldContent.replace(oldString, newString) else oldContent.replaceFirst(oldString, newString)

        // 检查是否需要自动显示 diff
        val settings = com.qifu.config.AutoApproveSettings.state
        val shouldShowDiff = !settings.actions.editFiles

        if (shouldShowDiff) {
            // 显示 diff 窗口必须在 EDT 中执行
            val fileName = filePath.substringAfterLast("/")
            withContext(Dispatchers.EDT) {
                DiffWindowHolder.showDiffView(
                    taskState.project,
                    fileName,
                    oldContent,
                    newContent,
                    taskState.convId,
                    filePath,
                    true
                )
            }
        }

        // 添加观测
        ChatUtil.saveCodeCompletionLog(
            taskState.project,
            toolRequestId,
            filePath,
            null,
            CompletionType.ASK_AI,  // 需要确认具体类型
            CompletionStatus.SHOWN,
            newContent,
            null
        )

        return ToolSegment(
            UiToolName.EDITED_EXISTING_FILE,
            filePath,
            "------- SEARCH\n$oldString\n=======\n$newString\n+++++++ REPLACE",
            mapOf(
                "oldString" to JsonPrimitive(oldContent),
                "newString" to JsonPrimitive(newContent),
                "agent_name" to JsonPrimitive(
                    if (taskState.agentId != "default") taskState.agentId else "Jarvis"
                ),
            )
        )
    }

    private fun handleUserModifiedContent(
        filePath: String,
        taskState: TaskState,
        toolRequest: ToolExecutionRequest,
        oldString: String,
        newString: String
    ): ToolCallResult<EditToolOutput> {
        val map: Map<String, String?> = Json.decodeFromString(taskState.askUserResponse?:"")

        // 1. 展示用户修改的代码 给UI展示用的
        val userEditSegment = ToolSegment(
            name = UiToolName.USER_EDIT,
            toolCommand = "user edits",
            toolContent = map["userEdit"] ?:"",
            params = emptyMap()
        )
        taskState.emit!!(JarvisSay(toolRequest.id(), AgentMessageType.TOOL, listOf(userEditSegment)))
        taskState.historyAiMessage.segments.add(userEditSegment)

        // 2. 展示整体修改的代码 给工具(Edit、write)上下文用的
        val editContent = map["allEdit"]!!
        // 用户同意执行更新操作
        ChatUtil.updateCodeCompletionLogStatus(toolRequest.id(), CompletionStatus.ACCEPT)

        val data = EditToolOutput(
            filePath = filePath,
            oldString = oldString,
            newString = newString,
            originalContent = editContent,
            newContent = editContent,
            structuredPatch = emptyList(),
            operation = "user_edit"
        )
        return ToolCallResult(
            type = "result",
            data = data,
            resultForAssistant = renderResultForAssistant(data)
        )
    }

    override suspend fun validateInput(input: JsonElement, taskState: TaskState) {
        input as JsonObject
        // 参数存在性校验
        val filePath = input["file_path"]?.jsonPrimitive?.contentOrNull?.let{ normalizeFilePath(it, taskState.project) }
            ?: throw MissingToolParameterException(getName(), "file_path")
        val oldString = input["old_string"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotEmpty() }?.let{ replaceContentLineEnding(it, LineEndingType.LF) }
            ?: throw MissingToolParameterException(getName(), "old_string")
        val newString = input["new_string"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotEmpty() }?.let { replaceContentLineEnding(it, LineEndingType.LF) }
            ?: throw MissingToolParameterException(getName(), "new_string")
        val replaceAll = input["replace_all"]?.jsonPrimitive?.booleanOrNull ?: false

        if (!isFilePathInProject(filePath, taskState.project)) throw ToolParameterException("File path must be within the project root directory.")

        // 参数逻辑校验
        if (oldString == newString) throw ToolParameterException("No changes to make: old_string and new_string are exactly the same.")

        // 检查是否已读取过文件
        val readFileTimestamps = taskState.fileFreshnessService!!.getFileReadTimestamps()
        readFileTimestamps[filePath]
            ?: throw ToolExecutionException("File has not been read yet. Read it first before writing to it.")

        var fileContent = PsiFileUtils.getFileContent(taskState.project, filePath) ?: throw ToolExecutionException("File not found: $filePath")
        fileContent = replaceContentLineEnding(fileContent, LineEndingType.LF)
        if (!fileContent.contains(oldString)) {
            LOG.warn("String to replace not found in file $filePath: \n$oldString")
            throw ToolExecutionException("String to replace not found in file.")
        }

        // 如果不是replace_all模式，检查old_string是否唯一
        if (!replaceAll) {
            val matches = fileContent.split(oldString).size - 1
            if (matches > 1) {
                throw ToolExecutionException(
                    "Found $matches matches of the string to replace. For safety, this tool only supports replacing exactly one occurrence at a time. Add more lines of context to your edit and try again."
                )
            }
        }
    }


    private fun getSnippet(initialText: String, oldStr: String, newStr: String): Pair<String, Int> {
        val before = if (oldStr.isEmpty()) "" else replaceContentLineEnding( initialText, LineEndingType.LF).split(oldStr)[0]
        val replacementLine = before.split(Regex("\\r?\\n")).size - 1
        val newFileLines = initialText.replace(oldStr, newStr).split(Regex("\\r?\\n"))

        // 计算片段的开始和结束行号
        val startLine = maxOf(0, replacementLine - N_LINES_SNIPPET)
        val endLine = replacementLine + N_LINES_SNIPPET + newStr.split(Regex("\\r?\\n")).size

        // 获取片段，避免添加额外的换行符
        val snippetLines = newFileLines.slice(startLine..minOf(endLine, newFileLines.size - 1))
        val snippet = snippetLines.joinToString("\n")

        return Pair(snippet, startLine + 1)
    }

    /**
     * 解析diff内容，获取修改后的内容
     */
    private fun getUserEditSnippet(editContent: String): Pair<String, Int> {
        val snippet = parseDiffToContent(editContent)
        val diffHeaderRegex = Regex("""@@ -\d+(?:,\d+)? \+(\d+)(?:,\d+)? @@""")
        val match = diffHeaderRegex.find(editContent)
        val startLine = match?.groupValues?.get(1)?.toInt() ?: 1
        return Pair(snippet, startLine)
    }

}

