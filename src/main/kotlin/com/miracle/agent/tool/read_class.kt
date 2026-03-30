package com.miracle.agent.tool

import com.intellij.openapi.application.readAction
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.miracle.agent.AgentMessageType
import com.miracle.agent.JarvisSay
import com.miracle.agent.TaskState
import com.miracle.agent.parser.ToolSegment
import com.miracle.agent.parser.UiToolName
import com.miracle.utils.JsonField
import com.miracle.utils.addLineNumbers
import com.miracle.utils.truncateToCharBudget
import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import dev.langchain4j.model.chat.request.json.JsonRawSchema
import kotlinx.serialization.json.JsonPrimitive
import kotlin.reflect.KFunction

/**
 * ReadClass 工具的输出结果
 */
data class ReadClassToolOutput(
    val fqn: String, // 类的完全限定名
    val content: String, // 类文件内容
    val startLine: Int, // 起始行号（1-based）
)

/**
 * Java 类源码读取工具，通过完全限定名（FQN）读取类的源码定义
 */
object ReadClassTool: Tool<ReadClassToolOutput> {

    val SPEC = ToolSpecification.builder() // 工具规格定义，供模型识别和调用
        .name("ReadClass")
        .description("""Read the source definition of a Java class or interface by its fully qualified name (`FQN`), similar to reading a file from the workspace.
The returned text contains the class definition, including imports, fields, methods, and implementations if available.

Usage:
- By default, it reads up to 2000 lines starting from the beginning of the class
- You can optionally specify a line offset and limit (especially handy for long class), but it's recommended to read the whole class by not providing these parameters
- Any lines longer than 500 characters will be truncated
- Results are returned using cat -n format, with line numbers starting at 1
""")
        .parameters(JsonObjectSchema.builder()
            .addStringProperty("fqn", "The fully qualified name (FQN) of the Java class or interface to read, for example 'com.example.MyService'.")
            .addProperty("offset", JsonRawSchema.from("""{
                "type": ["integer", "null"],
                "minimum": 1,
                "description": "The line number to start reading from. Only provide if the file is too large to read at once"
            }"""))
            .addProperty("limit", JsonRawSchema.from("""{
                "type": ["integer", "null"],
                "minimum": 1,
                "description": "The number of lines to read. Only provide if the file is too large to read at once"
            }"""))
            .required("fqn")
            .build())
        .build()

    /**
     * 获取工具规格定义
     * @return 工具规格
     */
    override fun getToolSpecification(): ToolSpecification {
        return SPEC
    }

    /**
     * 获取工具执行函数的引用
     * @return 执行函数
     */
    override fun getExecuteFunc(): KFunction<ToolCallResult<ReadClassToolOutput>> {
        return ::execute
    }

    /**
     * 将工具输出结果渲染为给助手的文本
     * @param output 工具输出
     * @return 渲染后的文本
     */
    override fun renderResultForAssistant(output: ReadClassToolOutput): String {
        var result = addLineNumbers(output.content, output.startLine)
        result = truncateToCharBudget(result)
        return result
    }

    /**
     * 执行类源码读取操作
     * @param taskState 当前任务状态
     * @param fqn 类的完全限定名
     * @param offset 起始行号（1-based），默认从开头读取
     * @param limit 读取的最大行数
     * @param toolRequest 工具调用请求
     * @return 工具调用结果
     */
    suspend fun execute(
        taskState: TaskState,
        fqn: String,
        offset: Int? = null,
        limit: Int? = 2000,
        toolRequest: ToolExecutionRequest,
    ): ToolCallResult<ReadClassToolOutput> {
        val offset = (offset ?: 1).minus(1)
        val limit = limit ?: 2000

        // Resolve FQN to PsiClass using JavaPsiFacade
        val fileContent = readAction {
            val psiFacade = JavaPsiFacade.getInstance(taskState.project)
            val psiClass = psiFacade.findClass(fqn, GlobalSearchScope.allScope(taskState.project))
                ?: throw ToolExecutionException("Class '$fqn' not found in project")

            val file = when (val navElement = psiClass.navigationElement) {
                is PsiClass -> navElement.containingFile
                is PsiFile -> navElement
                else -> psiClass.containingFile
            } ?: throw ToolExecutionException("Unable to read content for class '$fqn'")
            file.text
        }
        val content = fileContent.lineSequence().drop(offset).take(limit).joinToString("\n")
        val data = ReadClassToolOutput(fqn, content, offset + 1)
        taskState.emit!!(JarvisSay(
            id = toolRequest.id(),
            type = AgentMessageType.TOOL,
            data = listOf(renderToolSegment(taskState, fqn, data, limit)),
        ))
        val resultForAssistant = renderResultForAssistant(data)
        return ToolCallResult("result", data, resultForAssistant)
    }

    /**
     * 工具结果添加后的回调，更新 UI 展示片段的内容
     * @param data 工具输出数据
     * @param taskState 当前任务状态
     */
    override suspend fun afterAddToolResult(data: Any?, taskState: TaskState) {
        val output = data as? ReadClassToolOutput ?: return
        val segments = taskState.historyAiMessage.segments
        val lastIndex = segments.indexOfLast { it is ToolSegment && it.name == UiToolName.READ_FILE }
        if (lastIndex >= 0) {
            val old = segments[lastIndex] as ToolSegment
            segments[lastIndex] = old.copy(toolContent = output.content.ifBlank { renderResultForAssistant(output) })
        }
    }

    /**
     * 处理流式参数块，构建 UI 展示片段
     * @param toolRequestId 工具请求ID
     * @param partialArgs 部分参数
     * @param taskState 当前任务状态
     * @param isPartial 是否为部分参数
     * @return UI 展示片段
     */
    override suspend fun handlePartialBlock(
        toolRequestId: String,
        partialArgs: Map<String, JsonField>,
        taskState: TaskState,
        isPartial: Boolean
    ): ToolSegment? {
        if (isPartial) return null

        val offset = partialArgs["offset"]?.value?.toIntOrNull() ?: 0
        val lineOffset = if (offset == 0) 0 else offset.minus(1)
        val limit = partialArgs["limit"]?.value?.toIntOrNull() ?: 2000

        return ToolSegment(
            name = UiToolName.READ_FILE,
            toolCommand = partialArgs["fqn"]!!.value,
            params = mapOf(
                "start" to JsonPrimitive(lineOffset + 1),
                "end" to JsonPrimitive(lineOffset + limit),
                "agent_name" to JsonPrimitive(
                    if (taskState.agentId != "default") taskState.agentId else "Jarvis"
                ),
            )
        )
    }

    /**
     * 构建 ReadClass 工具的 UI 展示片段
     * @param taskState 当前任务状态
     * @param fqn 类的完全限定名
     * @param output 读取工具输出
     * @param limit 行数限制
     * @return 工具展示片段
     */
    private fun renderToolSegment(
        taskState: TaskState,
        fqn: String,
        output: ReadClassToolOutput,
        limit: Int,
    ): ToolSegment {
        val lineCount = output.content.lineSequence().count().coerceAtLeast(1)
        return ToolSegment(
            name = UiToolName.READ_FILE,
            toolCommand = fqn,
            toolContent = output.content.ifBlank { renderResultForAssistant(output) },
            params = mapOf(
                "start" to JsonPrimitive(output.startLine),
                "end" to JsonPrimitive(output.startLine + minOf(lineCount, limit) - 1),
                "agent_name" to JsonPrimitive(
                    if (taskState.agentId != "default") taskState.agentId else "Jarvis"
                ),
            )
        )
    }
}
