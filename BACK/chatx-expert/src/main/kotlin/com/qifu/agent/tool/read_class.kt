package com.qifu.agent.tool

import com.intellij.openapi.application.readAction
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.qifu.agent.TaskState
import com.qifu.agent.parser.ToolSegment
import com.qifu.agent.parser.UiToolName
import com.qifu.utils.JsonField
import com.qifu.utils.addLineNumbers
import com.qifu.utils.truncateToCharBudget
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import dev.langchain4j.model.chat.request.json.JsonRawSchema
import kotlinx.serialization.json.JsonPrimitive
import kotlin.reflect.KFunction

data class ReadClassToolOutput(
    val fqn: String,
    val content: String,
    val startLine: Int,
)

object ReadClassTool: Tool<ReadClassToolOutput> {

    val SPEC = ToolSpecification.builder()
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

    override fun getToolSpecification(): ToolSpecification {
        return SPEC
    }

    override fun getExecuteFunc(): KFunction<ToolCallResult<ReadClassToolOutput>> {
        return ::execute
    }

    override fun renderResultForAssistant(output: ReadClassToolOutput): String {
        var result = addLineNumbers(output.content, output.startLine)
        result = truncateToCharBudget(result)
        return result
    }

    suspend fun execute(taskState: TaskState, fqn: String, offset: Int? = null, limit: Int? = 2000): ToolCallResult<ReadClassToolOutput> {
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
        val resultForAssistant = renderResultForAssistant(data)
        return ToolCallResult("result", data, resultForAssistant)
    }

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
}