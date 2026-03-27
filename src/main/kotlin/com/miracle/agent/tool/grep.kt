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

data class GrepToolOutput(
    val numFiles: Int,
    val filenames: List<String>,
    val mode: String = "files_with_matches",  // 'content' | 'files_with_matches' | 'count'
    val content: String? = null,
    val numLines: Int? = null,
    val numMatches: Int? = null,
    val appliedOffset: Int? = null,
    val appliedLimit: Int? = null,
    val durationMs: Long,
)

/**
 * Grep搜索工具 - 基于JetBrains PSI API的强大搜索功能
 */
object GrepTool : Tool<GrepToolOutput> {

	private val LOG = Logger.getInstance(GrepTool::class.java)
	private const val MAX_RESULT_CHARS = 20_000

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

	override fun getToolSpecification(): ToolSpecification {
		return SPEC
	}

	override fun getExecuteFunc(): KFunction<ToolCallResult<GrepToolOutput>> {
		return ::execute
	}

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

	override suspend fun handlePartialBlock(toolRequestId: String, partialArgs: Map<String, JsonField>, taskState: TaskState, isPartial: Boolean): ToolSegment? {
        if (isPartial) return null

        val pattern = partialArgs["pattern"]!!.value
        val path = partialArgs["path"]?.value ?: taskState.project.basePath!!
        val outputMode = partialArgs["output_mode"]?.value ?: "files_with_matches"
        return renderToolSegment(pattern, path, outputMode, taskState, null)
	}

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

    private fun paginate(items: List<String>, limit: Int?, offset: Int?): List<String> {
        val offset = offset ?: 0
        var items = items
        if (offset > 0) items = items.drop(offset)
        if (limit != null && limit > 0) items = items.take(limit)
        return items
    }

	private fun formatPagination(limit: Int?, offset: Int?): String {
		if (limit == null && offset == null) return ""
		return "limit=$limit, offset=${offset ?: 0}"
	}

	private fun truncateToCharBudget(text: String): String {
		if (text.length <= MAX_RESULT_CHARS) return text
		val head = text.take(MAX_RESULT_CHARS)
		val truncatedLines = text.substring(MAX_RESULT_CHARS).split('\n').size
		return "$head\n\n... [$truncatedLines lines truncated] ..."
	}
}
