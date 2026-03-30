package com.miracle.agent.tool

import com.intellij.openapi.diagnostic.Logger
import com.miracle.agent.AgentMessageType
import com.miracle.agent.JarvisSay
import com.miracle.agent.TaskState
import com.miracle.agent.parser.ToolSegment
import com.miracle.agent.parser.UiToolName
import com.miracle.external.RipGrepUtil
import com.miracle.utils.*
import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.model.chat.request.json.JsonEnumSchema
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.*
import java.io.File
import kotlin.reflect.KFunction

/**
 * Grep 工具的输出结果
 */
data class GrepToolOutput(
    val numFiles: Int, // 匹配的文件数量
    val filenames: List<String>, // 匹配的文件路径列表
    val mode: String = "files_with_matches", // 输出模式：'content' | 'files_with_matches' | 'count'
    val content: String? = null, // 内容模式下的搜索结果
    val numLines: Int? = null, // 匹配的行数
    val numMatches: Int? = null, // 匹配的总次数
    val appliedOffset: Int? = null, // 应用的偏移量
    val appliedLimit: Int? = null, // 应用的结果限制数
    val durationMs: Long, // 执行耗时（毫秒）
)

/**
 * Grep搜索工具 - 基于JetBrains PSI API的强大搜索功能
 */
object GrepTool : Tool<GrepToolOutput> {

	private val LOG = Logger.getInstance(GrepTool::class.java)
	private const val MAX_RESULT_CHARS = 20_000 // 返回给 AI 的最大字符数

	val SPEC = ToolSpecification.builder()
		.name("Grep")
		.description("""A powerful search tool built on ripgrep

  Usage:
  - ALWAYS use Grep for search tasks. NEVER invoke `grep` or `rg` as a Bash command. The Grep tool has been optimized for correct permissions and access.
  - Supports full regex syntax (e.g., "log.*Error", "function\s+\w+")
  - Filter files with glob parameter (e.g., "*.js", "**/*.tsx") or type parameter (e.g., "js", "py", "rust")
  - Output modes: "content" shows matching lines, "files_with_matches" shows only file paths (default), "count" shows match counts
  - Use Task tool for open-ended searches requiring multiple rounds
  - Pattern syntax: Uses ripgrep (not grep) - literal braces need escaping (use `interface\{\}` to find `interface{}` in Go code)
  - Multiline matching: By default patterns match within single lines only. For cross-line patterns like `struct \{[\s\S]*?field`, use `multiline: true`""")
		.parameters(JsonObjectSchema.builder()
			.addStringProperty("pattern", "The regular expression pattern to search for in file contents")
			.addStringProperty("path", "File or directory to search in (rg PATH). Defaults to current working directory.")
			.addStringProperty("glob", "Glob pattern to filter files (e.g. \"*.js\", \"*.{ts,tsx}\") - maps to rg --glob")
			.addProperty("output_mode", JsonEnumSchema.builder()
				.enumValues("content", "files_with_matches", "count")
				.description("Output mode: \"content\" shows matching lines (supports -A/-B/-C context, -n line numbers, head_limit), \"files_with_matches\" shows file paths (supports head_limit), \"count\" shows match counts (supports head_limit). Defaults to \"files_with_matches\".")
				.build())
			.addNumberProperty("-B", "Number of lines to show before each match (rg -B). Requires output_mode: \"content\", ignored otherwise.")
			.addNumberProperty("-A", "Number of lines to show after each match (rg -A). Requires output_mode: \"content\", ignored otherwise.")
			.addNumberProperty("-C", "Number of lines to show before and after each match (rg -C). Requires output_mode: \"content\", ignored otherwise.")
			.addBooleanProperty("-n", "Show line numbers in output (rg -n). Requires output_mode: \"content\", ignored otherwise. Defaults to true.")
			.addBooleanProperty("-i", "Case insensitive search")
			.addStringProperty("type", "File type to search (rg --type). Common types: js, py, rust, go, java, etc. More efficient than include for standard file types.")
			.addNumberProperty("head_limit", "Limit output to first N lines/entries, equivalent to \"| head -N\". Works across all output modes: content (limits output lines), files_with_matches (limits file paths), count (limits count entries). Defaults based on \"cap\" experiment value: 0 (unlimited), 20, or 100.")
            .addNumberProperty("offset", "Skip first N lines/entries before applying head_limit, equivalent to \"| tail -n +N | head -N\". Works across all output modes. Defaults to 0.")
			.addBooleanProperty("multiline", "Enable multiline mode where . matches newlines and patterns can span lines (rg -U --multiline-dotall). Default: false.")
			.required("pattern")
			.build())
		.build()

    /**
     * 获取工具规格定义
     * @return Grep 工具的规格定义
     */
	override fun getToolSpecification(): ToolSpecification {
		return SPEC
	}

    /**
     * 获取工具的执行函数引用
     * @return execute 方法的函数引用
     */
	override fun getExecuteFunc(): KFunction<ToolCallResult<GrepToolOutput>> {
		return ::execute
	}

    /**
     * 将工具输出渲染为返回给 AI 的文本
     * @param output Grep 搜索结果
     * @return 格式化后的结果文本
     */
	override fun renderResultForAssistant(output: GrepToolOutput): String {
		val pagination = formatPagination(output.appliedLimit, output.appliedOffset)
		return when (output.mode) {
			"content" -> {
				val base = truncateToCharBudget(output.content?.takeIf { it.isNotBlank() } ?: "No matches found")
				if (pagination.isBlank()) base else "${base}\n\n[Showing results with pagination = ${pagination}]"
			}
			"count" -> {
				val base = truncateToCharBudget(output.content?.takeIf { it.isNotBlank() } ?: "No matches found")
				val numMatches = output.numMatches ?: 0
				val numFiles = output.numFiles
				base +
					"\n\nFound $numMatches total ${if (numMatches == 1) "occurrence" else "occurrences"} across $numFiles ${if (numFiles == 1) "file" else "files"}." +
					(if (pagination.isNotBlank()) " with pagination = $pagination" else "")
			}
			else -> {
				if (output.numFiles == 0) return "No files found"
				val header = "Found ${output.numFiles} file${if (output.numFiles == 1) "" else "s"}${if (pagination.isNotBlank()) " $pagination" else ""}\n${output.filenames.joinToString("\n")}"
				return truncateToCharBudget(header)
			}
		}
	}

    /**
     * 构建 Grep 工具的 UI 展示片段
     * @param pattern 搜索模式
     * @param path 搜索路径
     * @param outputMode 输出模式
     * @param taskState 当前任务状态
     * @param output Grep 工具输出（可为 null 表示搜索未完成）
     * @return 工具展示片段
     */
    fun renderToolSegment(pattern: String, path: String?, outputMode: String, taskState: TaskState, output: GrepToolOutput?): ToolSegment {
        val content = output?.let {
            buildString {
                var numLines = 0
                val result = when (output.mode) {
                    "files_with_matches" -> output.filenames.joinToString("\n")
                    "count" -> {
                        output.filenames.joinToString("\n") {
                            val idx = it.lastIndexOf(':')
                            if (idx > 0) {
                                val relativePath = toRelativePath(it.substring(0, idx), taskState.project.basePath!!)
                                relativePath + it.substring(idx)
                            } else {
                                it
                            }
                        }
                    }
                    else -> {
                        output.filenames.joinToString("\n\n") {
                            val lines = it.split(Regex("\r?\n"))
                            if (lines.isNotEmpty()) {
                                numLines += lines.size - 1
                                val relativePath = toRelativePath(lines[0], taskState.project.basePath!!)
                                listOf(relativePath).plus(lines.drop(1)).joinToString("\n")
                            } else {
                                it
                            }
                        }
                    }
                }

                append("Found ")
                when (output.mode) {
                    "content" -> append("$numLines ${if (numLines == 1) "line" else "lines"}")
                    "count" -> append("${output.numMatches ?: 0} ${if (output.numMatches == 1) "match" else "matches"}")
                    else -> append("${output.numFiles} ${if (output.numFiles == 1) "file" else "files"}")
                }
                append(" in ${output.durationMs}ms\n")
                append(truncateToCharBudget(result))
            }
        }.orEmpty()

        val segment = ToolSegment(
            name = UiToolName.SEARCH_FILES,
            toolCommand = path ?: getCurrentProjectRootPath(),
            toolContent = content,
            params = mapOf(
                "pattern" to JsonPrimitive(pattern),
                "outputMode" to JsonPrimitive(outputMode),
                "agent_name" to JsonPrimitive(
                    if (taskState.agentId != "default") taskState.agentId else "Jarvis"
                )
            ),
        )
        if (content.isNotBlank()) {
            taskState.historyAiMessage.segments.last()
                .let {
                    if ((it is ToolSegment) && (it.name == UiToolName.SEARCH_FILES)) {
                        taskState.historyAiMessage.segments.removeLast()
                    }
                    taskState.historyAiMessage.segments.add(segment)
                }
        }
        return segment
    }

    /**
     * 处理工具参数流式返回，构建 UI 展示片段
     * @param toolRequestId 工具请求 ID
     * @param partialArgs 已解析的参数字段
     * @param taskState 当前任务状态
     * @param isPartial 是否为部分参数（流式传输中）
     * @return 工具展示片段
     */
	override suspend fun handlePartialBlock(toolRequestId: String, partialArgs: Map<String, JsonField>, taskState: TaskState, isPartial: Boolean): ToolSegment? {
        if (isPartial) return null

        val pattern = partialArgs["pattern"]!!.value
        val path = partialArgs["path"]?.value ?: taskState.project.basePath!!
        val outputMode = partialArgs["output_mode"]?.value ?: "files_with_matches"
        return renderToolSegment(pattern, path, outputMode, taskState, null)
	}

    /**
     * 校验工具输入参数的合法性
     * @param input 输入的 JSON 参数
     * @param taskState 当前任务状态
     */
	override suspend fun validateInput(input: JsonElement, taskState: TaskState) {
		val jsonObject = input as? JsonObject ?: throw ToolParameterException("Input must be a JSON object")
		val path = jsonObject["path"]?.jsonPrimitive?.contentOrNull
		if (!path.isNullOrBlank()) {
			val pathFile = File(toAbsolutePath(path, taskState.project.basePath!!))
			if (!pathFile.exists()) {
				throw ToolParameterException("Path does not exist: $path")
			}
		}
	}

    /**
     * 执行 Grep 搜索
     * @param pattern 正则表达式搜索模式
     * @param path 搜索路径
     * @param glob 文件过滤 glob 模式
     * @param output_mode 输出模式
     * @param `-B` 匹配行前显示行数
     * @param `-A` 匹配行后显示行数
     * @param `-C` 匹配行前后显示行数
     * @param `-n` 是否显示行号
     * @param `-i` 是否忽略大小写
     * @param type 文件类型过滤
     * @param offset 结果偏移量
     * @param head_limit 结果数量限制
     * @param multiline 是否启用多行匹配模式
     * @param taskState 当前任务状态
     * @param toolRequest 工具调用请求
     * @return 工具调用结果
     */
	suspend fun execute(
		pattern: String,
		path: String? = null,
		glob: String? = null,
		output_mode: String? = null,
		`-B`: Int? = null,
		`-A`: Int? = null,
		`-C`: Int? = null,
		`-n`: Boolean? = null,
		`-i`: Boolean? = null,
		type: String? = null,
        offset: Int? = null,
		head_limit: Int? = null,
		multiline: Boolean? = null,
        taskState: TaskState,
        toolRequest: ToolExecutionRequest
	): ToolCallResult<GrepToolOutput> {
		val startTime = System.currentTimeMillis()
        val outputMode = output_mode ?: "files_with_matches"
        val lineNumbers = `-n` ?: true
        val caseInsensitive = `-i` ?: false
        val multiline = multiline ?: false
        var lines = RipGrepUtil.grep(
            pattern = pattern, path = path, cwd = taskState.project.basePath!!, glob = glob, type = type,
            outputMode = outputMode, before = `-B`, after = `-A`, context = `-C`, lineNumbers = lineNumbers,
            caseInsensitive = caseInsensitive, multiline = multiline
        ).toList()
        val output = when (outputMode) {
            "content" -> {
                if (!lines.isEmpty()) {
                    lines = lines.joinToString("\n").split("\n\n")
                    if (isFile(path, taskState.project.basePath!!)) {
                        val absPath = toAbsolutePath(path!!, taskState.project.basePath!!)
                        lines = lines.map { "$absPath\n$it" }
                    }
                    lines = paginate(lines, head_limit, offset)
                }
                GrepToolOutput(
                    mode = outputMode,
                    numFiles = 0,
                    filenames = lines,
                    content = lines.joinToString("\n\n"),
                    numLines = lines.size,
                    appliedOffset = offset,
                    appliedLimit = head_limit,
                    durationMs = System.currentTimeMillis() - startTime,
                )
            }
            "count" -> {
                lines = paginate(lines, head_limit, offset)
                if (isFile(path, taskState.project.basePath!!)) {
                    val absPath = toAbsolutePath(path!!, taskState.project.basePath!!)
                    lines = lines.map { "$absPath:$it" }
                }
                var numMatches = 0
                var numFiles = 0
                for (line in lines) {
                    val idx = line.lastIndexOf(':')
                    if (idx > 0) {
                        val count = line.substring(idx + 1).toInt()
                        numMatches += count
                        numFiles++
                    }
                }
                GrepToolOutput(
                    mode = outputMode,
                    numFiles = numFiles,
                    filenames = lines,
                    content = lines.joinToString("\n"),
                    numMatches = numMatches,
                    appliedOffset = offset,
                    appliedLimit = head_limit,
                    durationMs = System.currentTimeMillis() - startTime,
                )
            }
            else -> {
                lines = paginate(lines, head_limit, offset)
                // Convert to absolute paths and sort by file modification time (descending)
                val absolutePaths = lines.map { 
                    toAbsolutePath(it, taskState.project.basePath!!)
                }.sortedByDescending { path ->
                    runCatching {
                        File(path).lastModified()
                    }.getOrElse{ 0 }
                }

                GrepToolOutput(
                    mode = "files_with_matches",
                    numFiles = absolutePaths.size,
                    filenames = absolutePaths,
                    appliedOffset = offset,
                    appliedLimit = head_limit,
                    durationMs = System.currentTimeMillis() - startTime,
                )
            }
        }

        taskState.emit!!(JarvisSay(
            id = toolRequest.id(),
            type = AgentMessageType.TOOL,
            data = listOf(renderToolSegment(pattern, path, outputMode, taskState, output))
        ))
        return ToolCallResult(
            type = "result",
            data = output,
            resultForAssistant = renderResultForAssistant(output)
        )
	}

    /**
     * 对搜索结果进行分页处理
     * @param items 待分页的列表
     * @param limit 每页数量限制
     * @param offset 偏移量
     * @return 分页后的列表
     */
    private fun paginate(items: List<String>, limit: Int?, offset: Int?): List<String> {
        val offset = offset ?: 0
        var items = items
        if (offset > 0) items = items.drop(offset)
        if (limit != null && limit > 0) items = items.take(limit)
        return items
    }

    /**
     * 格式化分页参数为字符串
     * @param limit 数量限制
     * @param offset 偏移量
     * @return 格式化的分页字符串
     */
	private fun formatPagination(limit: Int?, offset: Int?): String {
		if (limit == null && offset == null) return ""
		return "limit=$limit, offset=${offset ?: 0}"
	}

    /**
     * 将文本截断到字符预算内，超长时截断末尾并添加提示
     * @param text 原始文本
     * @return 截断后的文本
     */
	private fun truncateToCharBudget(text: String): String {
		if (text.length <= MAX_RESULT_CHARS) return text
		val head = text.take(MAX_RESULT_CHARS)
		val truncatedLines = text.substring(MAX_RESULT_CHARS).split('\n').size
		return "$head\n\n... [$truncatedLines lines truncated] ..."
	}
}
