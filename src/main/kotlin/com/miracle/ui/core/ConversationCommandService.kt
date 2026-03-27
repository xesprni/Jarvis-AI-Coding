package com.miracle.ui.core

import com.intellij.openapi.project.Project
import com.miracle.agent.TaskState
import com.miracle.agent.parser.TextSegment
import com.miracle.ui.smartconversation.settings.configuration.ChatMode
import com.miracle.utils.ChatHistoryAssistantMessage
import com.miracle.utils.ChatHistoryUserMessage
import com.miracle.utils.ConversationStore
import com.miracle.utils.JsonLineChatHistory
import com.miracle.utils.JsonLineChatMemory
import com.miracle.utils.TodoStorage
import com.miracle.utils.forceCompact
import com.miracle.utils.getDefaultAgentId
import dev.langchain4j.data.message.AiMessage
import kotlinx.coroutines.runBlocking
import java.io.File

data class CompactResult(
    val summary: String,
)

object ConversationCommandService {

    @JvmStatic
    fun resetConversation(taskId: String, convId: String, modelId: String, project: Project) {
        ConversationStore.deleteConversation(project, convId)

        val agentId = getDefaultAgentId()
        val memoryStore = JsonLineChatMemory(convId, agentId, project)
        val historyStore = JsonLineChatHistory(convId, project)
        clearChatFiles(memoryStore, historyStore)
        TodoStorage.setTodos(convId, agentId, emptyList(), project)

        val taskState = buildTaskState(taskId, convId, modelId, project, memoryStore, historyStore)
        resetSessionCaches(taskState)
        ConversationStore.deleteConversation(project, convId)
    }

    @JvmStatic
    fun compactConversation(taskId: String, convId: String, modelId: String, project: Project): CompactResult {
        val agentId = getDefaultAgentId()
        val memoryStore = JsonLineChatMemory(convId, agentId, project)
        val historyStore = JsonLineChatHistory(convId, project)
        val taskState = buildTaskState(taskId, convId, modelId, project, memoryStore, historyStore)
        val messages = taskState.chatMemory.messages()
        if (messages.isEmpty()) {
            resetSessionCaches(taskState)
            throw IllegalStateException("No conversation context to compact yet.")
        }

        val compactedMessages = try {
            runBlocking {
                forceCompact(modelId, messages, taskState).messages
            }
        } finally {
            taskState.systemReminderService?.resetSession()
        }
        taskState.chatMemory.update(compactedMessages)
        taskState.systemReminderService?.resetSession()

        val summary = compactedMessages.filterIsInstance<AiMessage>().firstOrNull()?.text()?.trim()
            ?: throw IllegalStateException("Compact summary is missing.")
        if (summary.isBlank()) {
            throw IllegalStateException("Compact summary is empty.")
        }

        taskState.chatHistory.add(ChatHistoryUserMessage(text = "/compact"))
        taskState.chatHistory.add(ChatHistoryAssistantMessage(segments = mutableListOf(TextSegment(summary))))
        resetSessionCaches(taskState)
        return CompactResult(summary)
    }

    private fun buildTaskState(
        taskId: String,
        convId: String,
        modelId: String,
        project: Project,
        memoryStore: JsonLineChatMemory,
        historyStore: JsonLineChatHistory,
    ): TaskState {
        return TaskState(
            taskId = taskId,
            convId = convId,
            chatMemory = memoryStore,
            chatHistory = historyStore,
            tools = emptyMap(),
            modelId = modelId,
            chatMode = ChatMode.AGENT,
            agentId = getDefaultAgentId(),
            project = project,
        )
    }

    private fun resetSessionCaches(taskState: TaskState) {
        taskState.systemReminderService?.resetSession()
        taskState.fileFreshnessService?.resetSession()
        runBlocking {
            taskState.shell?.close()
        }
        TaskState.clearCachedServices(taskState.taskId)
    }

    private fun clearChatFiles(memoryStore: JsonLineChatMemory, historyStore: JsonLineChatHistory) {
        runCatching { File(memoryStore.filePath).delete() }
        runCatching { File(historyStore.filePath).delete() }
    }
}
