package com.miracle.agent.tool

import com.intellij.openapi.diagnostic.thisLogger
import com.miracle.agent.AgentMessageType
import com.miracle.agent.JarvisSay
import com.miracle.agent.TaskState
import com.miracle.agent.parser.ToolSegment
import com.miracle.agent.parser.UiToolName
import com.miracle.utils.JsonField
import com.miracle.utils.SecurityUtil
import com.miracle.utils.getCurrentProjectRootPath
import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*
import kotlin.reflect.KFunction

/**
 * Bash 命令执行工具的输出结果
 */
data class BashToolOutput(
    val stdout: String, // 标准输出内容
    val stdoutLines: Int, // 标准输出行数
    val stderr: String, // 标准错误内容
    val stderrLines: Int, // 标准错误行数
    val interrupted: Boolean, // 命令是否被中断
    val exitCode: Int // 退出码
)

/**
 * Bash 命令执行工具
 */
object BashTool : Tool<BashToolOutput> {

    val LOG = thisLogger()
    const val MAX_OUTPUT_LENGTH = 30000 // 单次输出最大字符数
    const val MAX_RENDERED_LINES = 20 // UI 渲染最大行数
    const val DEFAULT_TIMEOUT = 120000L // 2 minutes
    const val MAX_TIMEOUT = 600000L // 10 minutes
    // 被禁止执行的命令列表
    private val BANNED_COMMANDS = listOf(
        "rm", "rmdir", "del", "format", "fdisk", "mkfs",
        "shutdown", "reboot", "halt", "poweroff",
        "sudo", "su", "passwd", "chpasswd",
        "chmod", "chown", "chgrp",
        "dd", "shred", "wipe"
    )

    val SPEC = ToolSpecification.builder()
        .name("Bash")
        .description("""Executes a given bash command in a persistent shell session with optional timeout, ensuring proper handling and security measures.

IMPORTANT: This tool is for terminal operations like git, npm, docker, etc. DO NOT use it for file operations (reading, writing, editing, searching, finding files) - use the specialized tools for this instead.

Before executing the command, please follow these steps:

1. Directory Verification:
   - If the command will create new directories or files, first use `ls` to verify the parent directory exists and is the correct location
   - For example, before running "mkdir foo/bar", first use `ls foo` to check that "foo" exists and is the intended parent directory

2. Command Execution:
   - Always quote file paths that contain spaces with double quotes (e.g., cd "path with spaces/file.txt")
   - Examples of proper quoting:
     - cd "/Users/name/My Documents" (correct)
     - cd /Users/name/My Documents (incorrect - will fail)
     - python "/path/with spaces/script.py" (correct)
     - python /path/with spaces/script.py (incorrect - will fail)
   - After ensuring proper quoting, execute the command.
   - Capture the output of the command.

Usage notes:
  - The command argument is required.
  - You can specify an optional timeout in milliseconds (up to 600000ms / 10 minutes). If not specified, commands will timeout after 120000ms (2 minutes).
  - It is very helpful if you write a clear, concise description of what this command does in 5-10 words.
  - If the output exceeds 30000 characters, output will be truncated before being returned to you.
  - You can use the `run_in_background` parameter to run the command in the background, which allows you to continue working while the command runs. You can monitor the output using the Bash tool as it becomes available. Never use `run_in_background` to run 'sleep' as it will return immediately. You do not need to use '&' at the end of the command when using this parameter.
  
  - Avoid using Bash with the `find`, `grep`, `cat`, `head`, `tail`, `sed`, `awk`, or `echo` commands, unless explicitly instructed or when these commands are truly necessary for the task. Instead, always prefer using the dedicated tools for these commands:
    - File search: Use Glob (NOT find or ls)
    - Content search: Use Grep (NOT grep or rg)
    - Read files: Use Read (NOT cat/head/tail)
    - Edit files: Use Edit (NOT sed/awk)
    - Write files: Use Write (NOT echo >/cat <<EOF)
    - Communication: Output text directly (NOT echo/printf)
  - When issuing multiple commands:
    - If the commands are independent and can run in parallel, make multiple Bash tool calls in a single message. For example, if you need to run "git status" and "git diff", send a single message with two Bash tool calls in parallel.
    - If the commands depend on each other and must run sequentially, use a single Bash call with '&&' to chain them together (e.g., `git add . && git commit -m "message" && git push`). For instance, if one operation must complete before another starts (like mkdir before cp, Write before Bash for git operations, or git add before git commit), run these operations sequentially instead.
    - Use ';' only when you need to run commands sequentially but don't care if earlier commands fail
    - DO NOT use newlines to separate commands (newlines are ok in quoted strings)
  - Try to maintain your current working directory throughout the session by using absolute paths and avoiding usage of `cd`. You may use `cd` if the User explicitly requests it.
    <good-example>
    pytest /foo/bar/tests
    </good-example>
    <bad-example>
    cd /foo/bar && pytest tests
    </bad-example>""")
        .parameters(JsonObjectSchema.builder()
            .addStringProperty("command", "The command to execute")
            .addIntegerProperty("timeout", "Optional timeout in milliseconds (max 600000)")
            .addStringProperty("description", "Clear, concise description of what this command does in 5-10 words")
            .required("command")
            .build())
        .build()

    /**
     * 获取工具规格定义
     * @return Bash 工具的规格定义
     */
    override fun getToolSpecification(): ToolSpecification {
        return SPEC
    }

    /**
     * 获取工具的执行函数引用
     * @return execute 方法的函数引用
     */
    override fun getExecuteFunc(): KFunction<ToolCallResult<BashToolOutput>> {
        return ::execute
    }

    /**
     * 将工具输出渲染为返回给 AI 的文本
     * @param output Bash 命令执行结果
     * @return 格式化后的结果文本
     */
    override fun renderResultForAssistant(output: BashToolOutput): String {
        val stdout = output.stdout.trim()
        val stderr = output.stderr.trim()
        return buildString {
            if (output.interrupted) {
                append("Command was aborted before completion")
                return@buildString
            }
            append("Command executed.").append("\n")
            if (stdout.isNotEmpty() || stderr.isNotEmpty()) {
                append("Output:\n")
                if (stdout.isNotEmpty()) append(stdout)
                if (stderr.isNotEmpty()) append(stderr)
            }
        }
    }

    /**
     * 执行 Bash 命令
     * @param command 要执行的命令
     * @param timeout 超时时间（毫秒）
     * @param description 命令描述
     * @param taskState 当前任务状态
     * @param toolRequest 工具调用请求
     * @return 工具调用结果
     */
    suspend fun execute(command: String, timeout: Long = DEFAULT_TIMEOUT, description: String = "", taskState: TaskState, toolRequest: ToolExecutionRequest): ToolCallResult<BashToolOutput> {
        // 验证超时时间
        val actualTimeout = when {
            timeout <= 0 -> DEFAULT_TIMEOUT
            timeout > MAX_TIMEOUT -> MAX_TIMEOUT
            else -> timeout
        }

        // 验证命令安全性
        validateCommand(command)

        try {
            val shell = taskState.shell ?: throw taskState.shellCreateException!!
            // 执行并流式输出结果
            val result = shell.exec(command, actualTimeout) { stdout, stderr ->
                // 格式化输出
                val formattedStdout = formatOutput(stdout)
                val formattedStderr = formatOutput(stderr)

                val data = BashToolOutput(
                    stdout = formattedStdout.truncatedContent,
                    stdoutLines = formattedStdout.totalLines,
                    stderr = formattedStderr.truncatedContent,
                    stderrLines = formattedStderr.totalLines,
                    interrupted = false,
                    exitCode = 0
                )
                val segment = ToolSegment(
                    name = UiToolName.COMMAND_OUTPUT,
                    toolCommand = command,
                    toolContent = renderResultForAssistant(data),
                    params = mapOf(
                        "agent_name" to JsonPrimitive(
                            if (taskState.agentId != "default") taskState.agentId else "Jarvis"
                        )
                    )
                )
                taskState.emit!!(JarvisSay(
                    id = toolRequest.id(),
                    type = AgentMessageType.TOOL,
                    data = listOf(segment),
                    isPartial = true
                ))
            }
            
            // 格式化最终输出
            val formattedStdout = formatOutput(result.stdout)
            val formattedStderr = formatOutput(result.stderr)
            
            val data = BashToolOutput(
                stdout = formattedStdout.truncatedContent,
                stdoutLines = formattedStdout.totalLines,
                stderr = formattedStderr.truncatedContent,
                stderrLines = formattedStderr.totalLines,
                interrupted = result.interrupted,
                exitCode = result.code
            )
            val segment = ToolSegment(
                name = UiToolName.COMMAND_OUTPUT,
                toolCommand = command,
                toolContent = renderResultForAssistant(data),
                params = mapOf(
                    "agent_name" to JsonPrimitive(
                        if (taskState.agentId != "default") taskState.agentId else "Jarvis"
                    )
                )
            )
            addBashOutputSegmentToHistory(taskState, segment)
            taskState.emit!!(JarvisSay(
                id = toolRequest.id(),
                type = AgentMessageType.TOOL,
                data = listOf(segment)
            ))
            return ToolCallResult(
                type = "result",
                data = data,
                resultForAssistant = renderResultForAssistant(data)
            )
        } catch (e: Exception) {
            val errorData = BashToolOutput(
                stdout = "",
                stdoutLines = 0,
                stderr = "Command failed: ${e.message}",
                stderrLines = 1,
                interrupted = true,
                exitCode = -1
            )
            val segment = ToolSegment(
                name = UiToolName.COMMAND_OUTPUT,
                toolCommand = command,
                toolContent = renderResultForAssistant(errorData),
                params = mapOf(
                    "agent_name" to JsonPrimitive(
                        if (taskState.agentId != "default") taskState.agentId else "Jarvis"
                    )
                )
            )
            addBashOutputSegmentToHistory(taskState, segment)
            taskState.emit!!(JarvisSay(
                id = toolRequest.id(),
                type = AgentMessageType.TOOL,
                data = listOf(segment)
            ))
            return ToolCallResult(
                type = "result",
                data = errorData,
                resultForAssistant = renderResultForAssistant(errorData)
            )
        } finally {
            if (taskState.shell?.isAlive == true && taskState.shell?.cwd != taskState.project.basePath) {
                try {
                    taskState.shell?.setCwd(getCurrentProjectRootPath())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    LOG.warn("Failed to set cwd to project root", e)
                }
            }
        }
    }

    /**
     * 将 Bash 输出段添加到历史消息中，替换之前的 RUN_COMMAND 段
     */
    private fun addBashOutputSegmentToHistory(taskState: TaskState, segment: ToolSegment) {
        // 移除之前不包含结果的 RUN_COMMAND segment
        taskState.historyAiMessage.segments.lastOrNull()?.takeIf {
            it is ToolSegment && it.name == UiToolName.RUN_COMMAND
        }?.also {
            taskState.historyAiMessage.segments.removeLast()
        }
        // 添加包含结果的 COMMAND_OUTPUT segment
        taskState.historyAiMessage.segments.add(segment)
    }

    /**
     * 校验工具输入参数的合法性
     * @param input 输入的 JSON 参数
     * @param taskState 当前任务状态
     */
    override suspend fun validateInput(input: JsonElement, taskState: TaskState) {
        (input as JsonObject).let {
            val command = it["command"]?.jsonPrimitive?.contentOrNull 
                ?: throw MissingToolParameterException("Bash", "command")
            
            val timeout = it["timeout"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
            
            // 验证超时时间
            if (timeout != null && (timeout <= 0 || timeout > MAX_TIMEOUT)) {
                throw ToolParameterException("Timeout must be between 1 and $MAX_TIMEOUT milliseconds")
            }
            
            // 验证命令
            validateCommand(command)
        }
    }

    /**
     * 处理工具参数流式返回，构建 UI 展示片段
     * @param toolRequestId 工具请求 ID
     * @param partialArgs 已解析的参数字段
     * @param taskState 当前任务状态
     * @param isPartial 是否为部分参数（流式传输中）
     * @return 工具展示片段，流式阶段返回 null
     */
    override suspend fun handlePartialBlock(toolRequestId: String, partialArgs: Map<String, JsonField>, taskState: TaskState, isPartial: Boolean): ToolSegment? {
        if (isPartial) return null
        val command = partialArgs["command"]!!.value
        return ToolSegment(
            name = UiToolName.RUN_COMMAND,
            toolCommand = command,
            params = mapOf(
                "agent_name" to JsonPrimitive(
                    if (taskState.agentId != "default") taskState.agentId else "Jarvis"
                )
            ),
        )
    }

    /**
     * 校验命令安全性，包括空命令检查、禁止命令检查和多行命令检查
     * @param command 待校验的命令
     */
    private fun validateCommand(command: String) {
        val trimmedCommand = command.trim()
        if (trimmedCommand.isEmpty()) {
            throw ToolParameterException("Command cannot be empty")
        }

        // 检查被禁止的命令
        if (SecurityUtil.isBannedCommand(trimmedCommand)) {
            throw ToolExecutionException("Command '$trimmedCommand' is not allowed for security reasons")
        }

        // 检查多行命令（不允许换行符）
        if (trimmedCommand.contains('\n') || trimmedCommand.contains('\r')) {
            throw ToolParameterException("Multi-line commands are not allowed. Use ';' or '&&' to separate commands.")
        }
    }

    /**
     * 格式化命令输出内容，超长时截断中间部分
     * @param content 原始输出内容
     * @return 格式化后的输出
     */
    private fun formatOutput(content: String): FormattedOutput {
        val totalLines = content.split('\n').size
        
        return if (content.length <= MAX_OUTPUT_LENGTH) {
            FormattedOutput(totalLines, content)
        } else {
            val halfLength = MAX_OUTPUT_LENGTH / 2
            val start = content.take(halfLength)
            val end = content.takeLast(halfLength)
            val truncatedLines = content.substring(halfLength, content.length - halfLength).split('\n').size
            val truncated = "$start\n\n... [$truncatedLines lines truncated] ...\n\n$end"
            
            FormattedOutput(totalLines, truncated)
        }
    }

    /**
     * 格式化后的命令输出结果
     */
    private data class FormattedOutput(
        val totalLines: Int,
        val truncatedContent: String
    )
}

