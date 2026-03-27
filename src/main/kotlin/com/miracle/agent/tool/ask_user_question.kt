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
import kotlinx.serialization.json.*
import kotlin.reflect.KFunction

object AskUserQuestionTool: Tool<String> {

    val SPEC = ToolSpecification.builder()
        .name("AskUserQuestion")
        .description("""Use this tool when you need to ask the user questions during execution. This allows you to:
1. Gather user preferences or requirements
2. Clarify ambiguous instructions
3. Get decisions on implementation choices as you work
4. Offer choices to the user about what direction to take.
""")
        .parameters(JsonObjectSchema.builder()
            .addStringProperty("question", "The complete question to ask the user. Should be clear, specific, and end with a question mark. Example: \"Which library should we use for date formatting?\" If multiSelect is true, phrase it accordingly, e.g. \"Which features do you want to enable?\"")
            .addProperty("options",
                JsonArraySchema.builder()
                .items(
                    JsonStringSchema.builder()
                    .description("Each option should be a string describing a possible answer.")
                    .build()
                )
                .description("An array of 2-5 options for the user to choose from. You may not always need to provide options, but it may be helpful in many cases where it can save the user from having to type out a response manually. There should be no 'Other' option, that will be provided automatically.")
                .build()
            )
            .required("question")
            .build())
        .build()


    override fun renderResultForAssistant(output: String): String {
        return output
    }

    override fun getToolSpecification(): ToolSpecification {
        return SPEC
    }

    override fun getExecuteFunc(): KFunction<ToolCallResult<String>> {
        return ::execute
    }

    fun execute(taskState: TaskState, question: String, options: List<String> = emptyList()): ToolCallResult<String> {
        val userAnswer = taskState.askUserResponse!!
        val answer = taskState.askUserResponse?.let { "<answer>$it</answer>" } ?: "用户未答复内容"
        taskState.waitingAskUserQuestion = false
        taskState.askUserResponse = null

        // 更新ToolSegment的answer
        taskState.historyAiMessage.segments.lastOrNull()
            .takeIf { it is ToolSegment && it.name == UiToolName.ASK_USER_QUESTION }?.let {
                it as ToolSegment
                (it.params as MutableMap)["answer"] = JsonPrimitive(userAnswer)
            }

        // AiMessage 加入到history中，清空segment
        if (taskState.historyAiMessage.segments.isNotEmpty()) {
            val segments = mutableListOf<Segment>()
            segments.addAll(taskState.historyAiMessage.segments)
            val historyAiMessageCopy = ChatHistoryAssistantMessage(segments = segments)
            taskState.chatHistory.add(historyAiMessageCopy)
        }
        // 用户本次答复放到UserMessage
        taskState.chatHistory.add(ChatHistoryUserMessage(userAnswer))
        taskState.historyAiMessage.segments.clear()

        val resultForAssistant = renderResultForAssistant(answer)
        return ToolCallResult(
            type = "result",
            data = answer,
            resultForAssistant = resultForAssistant,
        )
    }

    override suspend fun validateInput(input: JsonElement, taskState: TaskState) {
        (input as JsonObject).let {
            it["question"]?.jsonPrimitive?.contentOrNull ?: throw MissingToolParameterException(getName(), "question")
//            val options = it["options"]?.let {str -> Json.decodeFromJsonElement<List<String>>(str) } ?: emptyList()
        }
    }

    override suspend fun handlePartialBlock(toolRequestId: String, partialArgs: Map<String, JsonField>, taskState: TaskState, isPartial: Boolean): ToolSegment? {
        if (isPartial) return null

        taskState.waitingAskUserQuestion = true
        val question = partialArgs["question"]?.value!!
        val options = partialArgs["options"]?.value?.let {Json.decodeFromString<List<String>>(it) } ?: emptyList()
        return ToolSegment(
            name = UiToolName.ASK_USER_QUESTION,
            toolCommand = question,
            toolContent = options.joinToString("\n") { "- $it" },
            params = mutableMapOf(
                "options" to Json.encodeToJsonElement(options),
                "taskId" to JsonPrimitive(taskState.convId),  // 给UI端要传主agent的taskId
                "answer" to JsonPrimitive(""),
                "agent_name" to JsonPrimitive(
                    if (taskState.agentId != "default") taskState.agentId else "Jarvis"
                ),
            )
        )
    }

}
