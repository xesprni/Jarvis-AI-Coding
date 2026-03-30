package com.miracle.agent.tool

import com.miracle.agent.TaskState
import com.miracle.agent.parser.Segment
import com.miracle.agent.parser.ToolSegment
import com.miracle.agent.parser.UiToolName
import com.miracle.utils.ChatHistoryAssistantMessage
import com.miracle.utils.ChatHistoryUserMessage
import com.miracle.utils.JsonField
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.model.chat.request.json.JsonArraySchema
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import dev.langchain4j.model.chat.request.json.JsonStringSchema
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.reflect.KFunction

/**
 * 用户输入选项的数据模型
 */
@Serializable
data class RequestUserInputOption(
    val label: String, // 选项标签
    val description: String, // 选项描述
)

/**
 * 用户输入问题的数据模型
 */
@Serializable
data class RequestUserInputQuestion(
    val header: String, // 问题标题（12字符以内）
    val id: String, // 问题标识符（snake_case 格式）
    val question: String, // 问题文本
    val options: List<RequestUserInputOption>, // 选项列表
)

/**
 * 用户输入工具的输出结果
 */
@Serializable
data class RequestUserInputOutput(
    val answers: Map<String, List<String>>, // 问题ID到用户答案的映射
)

/**
 * 请求用户输入工具，适用于 Plan 模式下让用户对实现方案做出具体决策
 */
object RequestUserInputTool : Tool<RequestUserInputOutput> {

    private val json = Json { ignoreUnknownKeys = true }

    val SPEC = ToolSpecification.builder() // 工具规格定义，供模型识别和调用
        .name("RequestUserInput")
        .description(
            """Use this tool when you need the user to make one or more concrete decisions that materially change the implementation plan.

This tool is best suited for plan mode. Ask 1-3 short questions, each with 2-3 meaningful options.
The UI will automatically allow the user to provide a free-form answer instead of choosing one of the listed options.
"""
        )
        .parameters(
            JsonObjectSchema.builder()
                .addProperty(
                    "questions",
                    JsonArraySchema.builder()
                        .items(
                            JsonObjectSchema.builder()
                                .addStringProperty("header", "A short label, 12 characters or fewer.")
                                .addStringProperty("id", "A stable identifier in snake_case.")
                                .addStringProperty("question", "A single concise question.")
                                .addProperty(
                                    "options",
                                    JsonArraySchema.builder()
                                        .items(
                                            JsonObjectSchema.builder()
                                                .addStringProperty("label", "A short user-facing choice label.")
                                                .addStringProperty("description", "One sentence describing the impact of selecting this option.")
                                                .required("label", "description")
                                                .build()
                                        )
                                        .description("Provide 2-3 mutually exclusive options.")
                                        .build()
                                )
                                .required("header", "id", "question", "options")
                                .build()
                        )
                        .description("Ask 1-3 short questions that materially affect the plan.")
                        .build()
                )
                .required("questions")
                .build()
        )
        .build()

    /**
     * 获取工具规格定义
     * @return 工具规格
     */
    override fun getToolSpecification(): ToolSpecification = SPEC

    /**
     * 获取工具执行函数的引用
     * @return 执行函数
     */
    override fun getExecuteFunc(): KFunction<ToolCallResult<RequestUserInputOutput>> = ::execute

    /**
     * 将工具输出结果渲染为给助手的文本
     * @param output 工具输出
     * @return 渲染后的文本
     */
    override fun renderResultForAssistant(output: RequestUserInputOutput): String {
        return json.encodeToString(output)
    }

    /**
     * 执行用户输入请求操作，获取用户的答复
     * @param taskState 当前任务状态
     * @param questions 问题列表
     * @return 工具调用结果
     */
    fun execute(taskState: TaskState, questions: List<RequestUserInputQuestion>): ToolCallResult<RequestUserInputOutput> {
        val responseJson = taskState.askUserResponse ?: """{"answers":{}}"""
        val output = json.decodeFromString<RequestUserInputOutput>(responseJson)
        taskState.waitingAskUserQuestion = false
        taskState.askUserResponse = null

        taskState.historyAiMessage.segments.lastOrNull()
            .takeIf { it is ToolSegment && it.name == UiToolName.REQUEST_USER_INPUT }
            ?.let {
                it as ToolSegment
                (it.params as MutableMap)["answer"] = JsonPrimitive(responseJson)
            }

        if (taskState.historyAiMessage.segments.isNotEmpty()) {
            val segments = mutableListOf<Segment>()
            segments.addAll(taskState.historyAiMessage.segments)
            taskState.chatHistory.add(ChatHistoryAssistantMessage(segments = segments))
        }
        taskState.chatHistory.add(ChatHistoryUserMessage(responseJson))
        taskState.historyAiMessage.segments.clear()

        return ToolCallResult(
            type = "result",
            data = output,
            resultForAssistant = renderResultForAssistant(output)
        )
    }

    /**
     * 校验工具输入参数
     * @param input 工具输入参数
     * @param taskState 当前任务状态
     */
    override suspend fun validateInput(input: JsonElement, taskState: TaskState) {
        val jsonObject = input as? JsonObject ?: throw ToolParameterException("Invalid input format")
        val questionsElement = jsonObject["questions"] ?: throw MissingToolParameterException(getName(), "questions")
        val questions = questionsElement.jsonArray.map { element ->
            json.decodeFromString<RequestUserInputQuestion>(element.toString())
        }

        if (questions.isEmpty() || questions.size > 3) {
            throw ToolParameterException("RequestUserInput requires 1-3 questions.")
        }

        questions.forEach { question ->
            if (question.header.isBlank() || question.header.length > 12) {
                throw ToolParameterException("Question header must be 1-12 characters.")
            }
            if (!question.id.matches(Regex("[a-z][a-z0-9_]*"))) {
                throw ToolParameterException("Question id must be snake_case.")
            }
            if (question.question.isBlank()) {
                throw ToolParameterException("Question text cannot be blank.")
            }
            if (question.options.size !in 2..3) {
                throw ToolParameterException("Each question must include 2-3 options.")
            }
            if (question.options.any { it.label.isBlank() || it.description.isBlank() }) {
                throw ToolParameterException("Each option must include a label and description.")
            }
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

        val rawQuestions = partialArgs["questions"]?.value ?: return null
        val questions = json.decodeFromString<List<RequestUserInputQuestion>>(rawQuestions)
        taskState.waitingAskUserQuestion = true

        val content = buildString {
            appendLine("Please answer the following questions:")
            questions.forEach { question ->
                appendLine()
                appendLine("### ${question.header}")
                appendLine(question.question)
                question.options.forEach { option ->
                    appendLine("- ${option.label}: ${option.description}")
                }
            }
        }.trim()

        return ToolSegment(
            name = UiToolName.REQUEST_USER_INPUT,
            toolCommand = questions.firstOrNull()?.question ?: "Request user input",
            toolContent = content,
            params = mutableMapOf(
                "questions" to json.parseToJsonElement(json.encodeToString(questions)),
                "taskId" to JsonPrimitive(taskState.convId),
                "answer" to JsonPrimitive(""),
                "agent_name" to JsonPrimitive(
                    if (taskState.agentId != "default") taskState.agentId else "Jarvis"
                ),
            )
        )
    }
}
