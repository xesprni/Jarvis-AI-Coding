//package com.qifu.agent.tool
//
//import com.intellij.openapi.application.ApplicationManager
//import com.intellij.openapi.command.WriteCommandAction
//import com.intellij.openapi.diagnostic.Logger
//import com.intellij.openapi.fileEditor.FileDocumentManager
//import com.intellij.openapi.vfs.LocalFileSystem
//import com.intellij.psi.PsiDocumentManager
//import com.qifu.agent.TaskState
//import com.qifu.agent.parser.ToolSegment
//import com.qifu.agent.parser.UiToolName
//import com.qifu.utils.*
//import dev.langchain4j.agent.tool.ToolSpecification
//import dev.langchain4j.model.chat.request.json.JsonArraySchema
//import dev.langchain4j.model.chat.request.json.JsonObjectSchema
//import kotlinx.serialization.ExperimentalSerializationApi
//import kotlinx.serialization.Serializable
//import kotlinx.serialization.json.*
//import java.io.File
//import java.nio.charset.Charset
//import kotlin.reflect.KFunction
//
//@OptIn(ExperimentalSerializationApi::class)
//@Serializable
//@JsonIgnoreUnknownKeys
//data class EditOperation(
//    val old_string: String,
//    val new_string: String,
//    val replace_all: Boolean = false
//)
//
//data class AppliedEdit(
//    val editIndex: Int,
//    val success: Boolean,
//    val old_string: String,
//    val new_string: String,
//    val occurrences: Int
//)
//
//data class MultiEditToolOutput(
//    val filePath: String,
//    val wasNewFile: Boolean,
//    val editsApplied: List<AppliedEdit>,
//    val totalEdits: Int,
//    val summary: String,
//    val structuredPatch: List<Hunk>,
//    val originalFile: String,
//    val modifiedFile: String,
//    val operation: String // "create", "update"
//)
//
//data class ContentEditResult(
//    val newContent: String,
//    val occurrences: Int
//)
//
///**
// * 多文件编辑工具 - 在单个文件上执行多个原子编辑操作
// */
//object MultiEditTool: Tool<MultiEditToolOutput> {
//
//    private val LOG = Logger.getInstance(MultiEditTool::class.java)
//    const val N_LINES_SNIPPET = 10
//
//    val SPEC = ToolSpecification.builder()
//        .name("MultiEdit")
//        .description("""A tool for making multiple edits to a single file atomically.
//
//This tool allows you to perform multiple find-and-replace operations efficiently on a single file.
//All edits are applied sequentially in the order provided, and the operation is atomic - either all edits succeed or none are applied.
//
//Usage:
//- Use the Read tool first to understand the file's contents and context
//- All edits must be valid for the operation to succeed
//- Each edit operates on the result of the previous edit
//- For Jupyter notebooks (.ipynb files), use a different tool instead
//- Only use emojis if the user explicitly requests it
//
//CRITICAL REQUIREMENTS:
//- All edits follow the same requirements as the single Edit tool
//- The edits are atomic - either all succeed or none are applied
//- Plan your edits carefully to avoid conflicts between sequential operations
//- Use absolute file paths
//- The tool will fail if old_string doesn't match exactly (including whitespace)
//- The tool will fail if old_string and new_string are the same
//- Since edits are applied in sequence, ensure that earlier edits don't affect the text that later edits are trying to find
//
//When making edits:
//- Ensure all edits result in idiomatic, correct code
//- Do not leave the code in a broken state
//- Always use absolute file paths (starting with /)
//- Only use emojis if the user explicitly requests it. Avoid adding emojis to files unless asked.
//- Use replace_all for replacing and renaming strings across the file. This parameter is useful if you want to rename a variable for instance.
//
//If you want to create a new file, use:
//- A new file path, including dir name if needed
//- First edit: empty old_string and the new file's contents as new_string
//- Subsequent edits: normal edit operations on the created content
//""")
//        .parameters(JsonObjectSchema.builder()
//            .addStringProperty("file_path", "The absolute path to the file to modify")
//            .addProperty("edits", JsonArraySchema.builder()
//                .items(JsonObjectSchema.builder()
//                    .addStringProperty("old_string", "The text to replace")
//                    .addStringProperty("new_string", "The text to replace it with")
//                    .addBooleanProperty("replace_all", "Replace all occurences of old_string (default false)")
//                    .required("old_string", "new_string")
//                    .build())
//                .description("Array of edit operations to perform sequentially on the file")
//                .build())
//            .required("file_path", "edits")
//            .build())
//        .build()
//
//    override fun getToolSpecification(): ToolSpecification {
//        return SPEC
//    }
//
//    override fun getExecuteFunc(): KFunction<ToolCallResult<MultiEditToolOutput>> {
//        return ::execute
//    }
//
//    override fun renderResultForAssistant(output: MultiEditToolOutput): String {
//        val operation = if (output.wasNewFile) "created" else "updated"
//        val snippet = getSnippet(output.originalFile, output.modifiedFile)
//
//        return """The file ${output.filePath} has been $operation with ${output.totalEdits} edits applied. Here's a snippet of the result:
//${snippet}
//
//Summary: ${output.summary}""".trimIndent()
//    }
//    /**
//     * 使用 PSI API 执行多编辑操作
//     * @return 成功返回 MultiEditToolOutput，失败返回 null
//     */
//    private suspend fun executeWithPSI(
//        file_path: String,
//        editOperations: List<EditOperation>,
//        taskState: TaskState
//    ): MultiEditToolOutput? {
//        val project = getCurrentProject()
//        if (project == null || project.isDisposed) {
//            LOG.info("PSI multi_edit: Project is null or disposed, falling back to filesystem")
//            return null
//        }
//
//        return try {
//            val fullFilePath = normalizeFilePath(file_path)
//            val file = File(fullFilePath)
//
//            if (!file.exists()) {
//                if (editOperations.isEmpty() || editOperations[0].old_string.isNotEmpty()) {
//                    // 文件不存在且不是创建操作
//                    LOG.info("PSI multi_edit: File does not exist and not a create operation, falling back to filesystem")
//                    return null
//                }
//            }
//
//            // 先计算编辑结果
//            val result = applyMultiEdit(file_path, editOperations)
//
//            // 使用 PSI API 在 UI 线程中执行写入操作
//            var success = false
//            ApplicationManager.getApplication().invokeAndWait {
//                try {
//                    if (!file.exists()) {
//                        // 创建新文件
//                        val dir = file.parentFile
//                        dir.mkdirs()
//                        file.createNewFile()
//                    }
//
//                    // 刷新文件系统以确保 VirtualFile 是最新的
//                    LocalFileSystem.getInstance().refreshAndFindFileByPath(fullFilePath)?.let { virtualFile ->
//                        val fileDocumentManager = FileDocumentManager.getInstance()
//                        val document = fileDocumentManager.getDocument(virtualFile)
//
//                        if (document == null) {
//                            // 对于二进制文件或无法获取 Document 的文件，使用文件系统方式
//                            LOG.info("PSI multi_edit: Cannot get document for file, falling back")
//                            return@invokeAndWait
//                        }
//
//                        // 对于文本文件，使用 Document API
//                        document.setReadOnly(false)
//                        WriteCommandAction.runWriteCommandAction(project) {
//                            val normalizedText = result.modifiedFile.replace("\r\n", "\n").replace("\r", "\n")
//                            document.setText(normalizedText)
//                        }
//
//                        // 提交文档更改到 PSI
//                        val psiDocumentManager = PsiDocumentManager.getInstance(project)
//                        psiDocumentManager.commitDocument(document)
//
//                        // 保存文档
//                        fileDocumentManager.saveDocument(document)
//
//                        // 刷新文件
//                        virtualFile.refresh(false, false)
//
//                        success = true
//                        LOG.info("PSI multi_edit: Successfully edited file $fullFilePath with ${editOperations.size} edits")
//                    } ?: run {
//                        LOG.info("PSI multi_edit: Could not find VirtualFile for $fullFilePath")
//                    }
//                } catch (e: Exception) {
//                    LOG.info("PSI multi_edit: Error during edit operation: ${e.message}")
//                    e.printStackTrace()
//                }
//            }
//
//            if (!success) {
//                return null
//            }
//
//            // 记录文件编辑
//            taskState.fileFreshnessService?.recordFileEdit(fullFilePath, result.modifiedFile)
//
//            result
//        } catch (e: Exception) {
//            LOG.info("PSI multi_edit failed: ${e.message}, falling back to filesystem")
//            e.printStackTrace()
//            null
//        }
//    }
//
//    /**
//     * 使用文件系统 I/O 执行多编辑操作（兜底逻辑）
//     */
//    private fun executeWithFileSystem(
//        file_path: String,
//        editOperations: List<EditOperation>,
//        taskState: TaskState
//    ): MultiEditToolOutput {
//        LOG.info("Using filesystem multi_edit for $file_path")
//
//        // 应用所有编辑
//        val result = applyMultiEdit(file_path, editOperations)
//
//        // 写入文件
//        val fullFilePath = normalizeFilePath(file_path)
//        val file = File(fullFilePath)
//        val dir = file.parentFile
//        val fileExists = file.exists()
//
//        val encoding = Charsets.UTF_8
//        val endings = if (fileExists) {
//            detectLineEndings(fullFilePath)
//        } else {
//            detectRepoLineEndings(getCurrentProjectRootPath())
//        }
//
//        dir.mkdirs()
//        writeTextContent(fullFilePath, result.modifiedFile, encoding, endings)
//
//        // 记录文件编辑
//        taskState.fileFreshnessService?.recordFileEdit(fullFilePath, result.modifiedFile)
//
//        return result
//    }
//    suspend fun execute(
//        file_path: String,
//        edits: List<EditOperation>,
//        taskState: TaskState
//    ): ToolCallResult<MultiEditToolOutput> {
//        // 添加空编辑列表检查
//        if (edits.isEmpty()) {
//            throw ToolParameterException("Edits list cannot be empty")
//        }
//
//        // 解析编辑操作
//        val editOperations = edits
//
//        // 优先使用 PSI API，失败则使用文件系统 I/O
//        val data = executeWithPSI(file_path, editOperations,taskState)
//            ?: executeWithFileSystem(file_path, editOperations,taskState)
//
//        return ToolCallResult(
//            type = "result",
//            data = data,
//            resultForAssistant = renderResultForAssistant(data)
//        )
//    }
//
//    private fun parseEditOperations(edits: List<Map<String, Any>>): List<EditOperation> {
//        return edits.mapIndexed { index, editMap ->
//            val oldString = editMap["old_string"] as? String
//                ?: throw ToolParameterException("Edit ${index + 1}: missing old_string")
//            val newString = editMap["new_string"] as? String
//                ?: throw ToolParameterException("Edit ${index + 1}: missing new_string")
//            val replaceAll = editMap["replace_all"] as? Boolean ?: false
//
//            EditOperation(oldString, newString, replaceAll)
//        }
//    }
//    override suspend fun validateInput(input: JsonElement, taskState: TaskState) {
//        val jsonObject = input as? JsonObject
//            ?: throw ToolParameterException("Input must be a JSON object")
//
//        val filePath = jsonObject["file_path"]?.jsonPrimitive?.contentOrNull?.let{ normalizeFilePath(it) }
//            ?: throw ToolParameterException("Missing required parameter: file_path")
//        if (!isFilePathInProject(filePath)) throw ToolParameterException("File path must be within the project root directory.")
//
//        val editsElement = jsonObject["edits"]
//            ?: throw ToolParameterException("Missing required parameter: edits")
//
//        val editsArray = if (editsElement is JsonPrimitive) {
//            // 如果是字符串，则解析为 JsonArray
//            Json.decodeFromString<JsonArray>(editsElement.content)
//        } else {
//            // 否则直接当作 JsonArray 使用
//            editsElement.jsonArray
//        }
//
//        if (editsArray.isEmpty()) {
//            throw ToolParameterException("Edits list cannot be empty")
//        }
//
//        val editOperations = editsArray.mapIndexed { index, editElement ->
//            val editObject = editElement as? JsonObject
//                ?: throw ToolParameterException("Edit ${index + 1}: must be a JSON object")
//
//            val oldString = editObject["old_string"]?.jsonPrimitive?.contentOrNull
//                ?: throw ToolParameterException("Edit ${index + 1}: missing old_string")
//
//            val newString = editObject["new_string"]?.jsonPrimitive?.contentOrNull
//                ?: throw ToolParameterException("Edit ${index + 1}: missing new_string")
//
//            val replaceAll = editObject["replace_all"]?.jsonPrimitive?.booleanOrNull ?: false
//
//            EditOperation(oldString, newString, replaceAll)
//
//        }
//        validateInput(filePath, editOperations, taskState.fileFreshnessService!!.getFileReadTimestamps())
//
//    }
//    fun validateInput(
//        filePath: String,
//        editOperations: List<EditOperation>,
//        readFileTimestamps: Map<String, Long>
//    ) {
//        val fullFilePath = normalizeFilePath(filePath)
//        val file = File(fullFilePath)
//
//        // 验证每个编辑操作
//        editOperations.forEachIndexed { index, edit ->
//            if (edit.old_string == edit.new_string) {
//                throw ToolExecutionException("Edit ${index + 1}: old_string and new_string cannot be the same")
//            }
//        }
//
//        // 如果是新文件
//        if (!file.exists()) {
//            // 检查父目录是否存在
//            val parentDir = file.parentFile
//            if (parentDir != null && !parentDir.exists()) {
//                throw ToolExecutionException("Parent directory does not exist: ${parentDir.absolutePath}")
//            }
//
//            // 对于新文件，第一个编辑必须是创建文件（empty old_string）
//            if (editOperations.isEmpty() || editOperations[0].old_string.isNotEmpty()) {
//                throw ToolExecutionException("For new files, the first edit must have an empty old_string to create the file content")
//            }
//            return
//        }
//
//        // 对于现有文件的验证
//        val readTimestamp = readFileTimestamps[fullFilePath]
//            ?: throw ToolExecutionException("File has not been read yet. Read it first before editing it.")
//
//        val lastWriteTime = file.lastModified()
//        if (lastWriteTime > readTimestamp) {
//            throw ToolExecutionException(
//                "File has been modified since read, either by the user or by a linter. Read it again before attempting to edit it."
//            )
//        }
//
//    }
//
//    private fun applyMultiEdit(
//        filePath: String,
//        editOperations: List<EditOperation>
//    ): MultiEditToolOutput {
//        val fullFilePath = normalizeFilePath(filePath)
//        val file = File(fullFilePath)
//        val fileExists = file.exists()
//
//        val originalFile = if (fileExists) {
//            val enc = detectFileEncoding(fullFilePath)
//            file.readText(Charset.forName(enc))
//        } else {
//            ""
//        }
//
//        var currentContent = originalFile
//        val appliedEdits = mutableListOf<AppliedEdit>()
//
//        // 按顺序应用所有编辑
//        editOperations.forEachIndexed { index, edit ->
//            try {
//                val result = applyContentEdit(
//                    currentContent,
//                    edit.old_string,
//                    edit.new_string,
//                    edit.replace_all
//                )
//                currentContent = result.newContent
//
//                appliedEdits.add(
//                    AppliedEdit(
//                        editIndex = index + 1,
//                        success = true,
//                        old_string = edit.old_string.take(100),
//                        new_string = edit.new_string.take(100),
//                        occurrences = result.occurrences
//                    )
//                )
//            } catch (e: Exception) {
//                throw ToolExecutionException("Error in edit ${index + 1}: ${e.message}", e)
//            }
//        }
//
//        val operation = if (fileExists) "update" else "create"
//        val summary = "Successfully applied ${editOperations.size} edits to ${filePath}"
//
//        val structuredPatch = getPatch(
//            filePath = filePath,
//            fileContents = originalFile,
//            oldStr = originalFile,
//            newStr = currentContent
//        )
//
//        return MultiEditToolOutput(
//            filePath = filePath,
//            wasNewFile = !fileExists,
//            editsApplied = appliedEdits,
//            totalEdits = editOperations.size,
//            summary = summary,
//            structuredPatch = structuredPatch,
//            originalFile = originalFile,
//            modifiedFile = currentContent,
//            operation = operation
//        )
//    }
//
//    private fun applyContentEdit(
//        content: String,
//        oldString: String,
//        newString: String,
//        replaceAll: Boolean
//    ): ContentEditResult {
//        if (replaceAll) {
//            val regex = Regex.escape(oldString).toRegex()
//            val matches = regex.findAll(content).count()
//            val newContent = content.replace(regex, newString)
//            return ContentEditResult(newContent, matches)
//        } else {
//            if (content.contains(oldString)) {
//                val newContent = content.replaceFirst(oldString, newString)
//                return ContentEditResult(newContent, 1)
//            } else {
//                throw RuntimeException("String not found: ${oldString.take(50)}...")
//            }
//        }
//    }
//
//    private fun getSnippet(originalFile: String, modifiedFile: String): String {
//        val modifiedLines = modifiedFile.split(Regex("\\r?\\n"))
//        val totalLines = modifiedLines.size
//
//        // 显示文件的前几行和后几行作为片段
//        val startLines = modifiedLines.take(N_LINES_SNIPPET)
//        val endLines = if (totalLines > N_LINES_SNIPPET * 2) {
//            modifiedLines.takeLast(N_LINES_SNIPPET)
//        } else {
//            emptyList()
//        }
//
//        val snippet = if (endLines.isNotEmpty()) {
//            startLines.joinToString("\n") + "\n...[truncated]...\n" + endLines.joinToString("")
//        } else {
//            modifiedLines.joinToString("\n")
//        }
//
//        return addLineNumbers(snippet, 1)
//    }
//
//    override fun handlePartialBlock(toolRequestId: String, partialArgs: Map<String, JsonField>, taskState: TaskState, isPartial: Boolean): ToolSegment? {
//        if (isPartial) return null
//
//        val filePath = partialArgs["file_path"]!!.value
//        val editsStr = partialArgs["edits"]!!.value
//        val edits = Json.decodeFromString<List<EditOperation>>(editsStr)
//
//        // 构造编辑预览信息
//        val replaceAlls = mutableListOf<Boolean>()
//        val editPreview = buildString {
//            edits.forEach { edit ->
//                val oldString = edit.old_string
//                val newString = edit.new_string
//                replaceAlls.add(edit.replace_all)
//                append("------- SEARCH\n$oldString\n=======\n$newString\n+++++++ REPLACE").append("\n")
//            }
//        }
//
//        return ToolSegment(
//            UiToolName.EDITED_EXISTING_FILE, filePath, editPreview.trim(),
//            params = mapOf(
//                "replaceAlls" to Json.encodeToJsonElement(replaceAlls)
//            )
//        )
//    }
//
//
//
//}
