//package com.miracle.agent.tool
//
//
//import com.intellij.openapi.diagnostic.Logger
//import com.miracle.agent.TaskState
//import com.miracle.agent.parser.ToolSegment
//import com.miracle.agent.parser.UiToolName
//import com.miracle.utils.JsonField
//import com.miracle.utils.normalizeFilePath
//import dev.langchain4j.agent.tool.ToolSpecification
//import dev.langchain4j.model.chat.request.json.JsonObjectSchema
//import kotlinx.serialization.json.JsonElement
//import kotlinx.serialization.json.JsonObject
//import kotlinx.serialization.json.jsonPrimitive
//import java.io.File
//import kotlin.reflect.KFunction
//
//data class LsToolOutput(
//    val type: String = "list",
//    val path: String,
//    val files: List<String>,
//    val isDirectory: Boolean = true
//)
//
///**
// * 列出目录内容工具
// */
//object LsTool: Tool<LsToolOutput> {
//
//    private val LOG = Logger.getInstance(LsTool::class.java)
//    const val MAX_FILES = 200
//    const val MAX_LINES_TO_RENDER_FOR_ASSISTANT = 200
//    const val TRUNCATED_MESSAGE = "\n...[truncated - showing first $MAX_FILES files]"
//
//    val SPEC = ToolSpecification.builder()
//        .name("Ls")
//        .description("""Lists files and directories in a given path.
//            |
//            |Usage:
//            |- The path parameter must be an absolute path, not a relative path.
//            |- You can optionally provide an array of glob patterns to ignore with the ignore parameter.
//            |- You should generally prefer the Glob and Grep tools, if you know which directories to search.
//            |- Hidden files (starting with '.') are excluded from results.
//            |- Results are limited to $MAX_FILES files to prevent overwhelming output.
//        """.trimMargin())
//        .parameters(JsonObjectSchema.builder()
//            .addStringProperty("path", "The absolute path to the directory to list")
//            .required("path")
//            .build())
//        .build()
//
//    override fun getToolSpecification(): ToolSpecification {
//        return SPEC
//    }
//
//    override fun getExecuteFunc(): KFunction<ToolCallResult<LsToolOutput>> {
//        return ::execute
//    }
//
//    override fun renderResultForAssistant(output: LsToolOutput): String {
//        val displayFiles = if (output.files.size > MAX_LINES_TO_RENDER_FOR_ASSISTANT) {
//            output.files.take(MAX_LINES_TO_RENDER_FOR_ASSISTANT) + listOf(TRUNCATED_MESSAGE)
//        } else {
//            output.files
//        }
//
//        val fileTree = createFileTree(displayFiles, output.path)
//        return if (output.isDirectory) {
//            "Contents of directory ${output.path}:\n$fileTree"
//        } else {
//            "Not a directory: ${output.path}"
//        }
//    }
//
//    override suspend fun validateInput(input: JsonElement, taskState: TaskState) {
//        (input as JsonObject).let {
//            val path = it["path"]?.jsonPrimitive?.contentOrNull ?: throw MissingToolParameterException(getName(), "path")
//
//            val file = File(path)
//            if (!file.exists()) {
//                throw ToolParameterException("Path does not exist: $path")
//            }
//        }
//    }
//
//    override suspend fun handlePartialBlock(toolRequestId: String, partialArgs: Map<String, JsonField>, taskState: TaskState, isPartial: Boolean): ToolSegment? {
//        if (isPartial) return null
//
//        val path = normalizeFilePath(partialArgs["path"]!!.value)
//        val result = execute(path)
//        // 修改为返回完整的文件路径列表
//        val fullPathList = result.data.files.joinToString("\n") { file ->
//            if (file.endsWith("/")) {
//                // 目录
//                "$path/$file".replace("\\", "/").trimEnd('/')
//            } else {
//                // 文件
//                "$path/$file".replace("\\", "/")
//            }
//        }
//        return ToolSegment(UiToolName.LIST_FILES_RECURSIVE, path, fullPathList)
//    }
//
//    fun execute(path: String): ToolCallResult<LsToolOutput> {
//        val file = File(path)
//
//        if (!file.exists()) {
//            throw ToolExecutionException("Path does not exist: $path")
//        }
//
//        if (!file.isDirectory) {
//            return ToolCallResult(
//                type = "result",
//                data = LsToolOutput(
//                    type = "list",
//                    path = path,
//                    files = emptyList(),
//                    isDirectory = false
//                ),
//                resultForAssistant = "Not a directory: $path"
//            )
//        }
//
//        val files = listDirectory(file)
//
//        val data = LsToolOutput(
//            type = "list",
//            path = path,
//            files = files,
//            isDirectory = true
//        )
//
//        return ToolCallResult(
//            type = "result",
//            data = data,
//            resultForAssistant = renderResultForAssistant(data)
//        )
//    }
//
//    /**
//     * 列出目录中的文件和子目录
//     */
//    private fun listDirectory(dir: File): List<String> {
//        val results = mutableListOf<String>()
//        val queue = mutableListOf(dir)
//
//        while (queue.isNotEmpty() && results.size <= MAX_FILES) {
//            val currentDir = queue.removeAt(0)
//
//            try {
//                val children = currentDir.listFiles() ?: continue
//
//                for (child in children) {
//                    // 跳过隐藏文件
//                    if (child.name.startsWith(".")) {
//                        continue
//                    }
//
//                    val relativePath = if (dir == currentDir) {
//                        child.name
//                    } else {
//                        // 使用正斜杠作为路径分隔符，确保跨平台一致性
//                        child.path.substring(dir.path.length + 1).replace("\\", "/")
//                    }
//
//                    if (child.isDirectory) {
//                        results.add("$relativePath/")
//                        queue.add(child)
//                    } else {
//                        results.add(relativePath)
//                    }
//
//                    if (results.size >= MAX_FILES) {
//                        break
//                    }
//                }
//            } catch (e: Exception) {
//                throw ToolExecutionException("Error listing directory: $dir", e)
//            }
//        }
//
//        return results.sorted()
//    }
//
//    /**
//     * 创建文件树结构的字符串表示
//     */
//    private fun createFileTree(files: List<String>, rootPath: String): String {
//        val builder = StringBuilder()
//        builder.append("- $rootPath/\n")
//
//        // 构建目录树结构
//        val pathTree = mutableMapOf<String, MutableSet<String>>()
//
//        // 将所有文件路径解析到树结构中
//        for (file in files.sorted()) {
//            // 统一处理路径分隔符，将Windows的反斜杠替换为正斜杠
//            val normalizedFile = file.replace("\\", "/")
//            val parts = normalizedFile.split("/").filter { it.isNotEmpty() }
//
//            if (parts.isEmpty()) continue
//
//            if (parts.size == 1) {
//                // 根目录下的文件
//                val item = parts[0]
//                if (!normalizedFile.endsWith("/")) {
//                    pathTree[""] = pathTree.getOrDefault("", mutableSetOf()).apply { add(item) }
//                } else {
//                    pathTree[""] = pathTree.getOrDefault("", mutableSetOf()).apply { add("$item/") }
//                }
//            } else {
//                // 构建路径树
//                var currentPath = ""
//                for (i in 0 until parts.size) {
//                    val part = parts[i]
//                    val isLast = i == parts.size - 1
//
//                    val parentPath = currentPath
//                    currentPath = if (currentPath.isEmpty()) "$part/" else "$currentPath$part/"
//
//                    // 将当前路径添加到父路径的子节点中
//                    if (isLast) {
//                        if (normalizedFile.endsWith("/")) {
//                            // 如果是目录
//                            pathTree[parentPath] = pathTree.getOrDefault(parentPath, mutableSetOf()).apply { add("$part/") }
//                        } else {
//                            // 如果是文件
//                            pathTree[parentPath] = pathTree.getOrDefault(parentPath, mutableSetOf()).apply { add(part) }
//                        }
//                    } else {
//                        // 中间目录
//                        pathTree[parentPath] = pathTree.getOrDefault(parentPath, mutableSetOf()).apply { add("$part/") }
//                    }
//                }
//            }
//        }
//
//        // 递归构建树形输出
//        buildTreeOutput(builder, pathTree, "", 1)
//
//        return builder.toString()
//    }
//
//    /**
//     * 递归构建树形输出
//     */
//    private fun buildTreeOutput(builder: StringBuilder, pathTree: Map<String, Set<String>>, currentPath: String, depth: Int) {
//        val indent = "  ".repeat(depth)
//
//        // 获取当前路径下的所有项目
//        val items = pathTree[currentPath] ?: return
//
//        // 先添加所有目录
//        val dirs = items.filter { it.endsWith("/") }.sorted()
//        for (dir in dirs) {
//            builder.append("$indent- $dir\n")
//            val nextPath = if (currentPath.isEmpty()) dir else "$currentPath$dir"
//            buildTreeOutput(builder, pathTree, nextPath, depth + 1)
//        }
//
//        // 再添加所有文件
//        val files = items.filter { !it.endsWith("/") }.sorted()
//        for (file in files) {
//            builder.append("$indent- $file\n")
//        }
//    }
//}
//
