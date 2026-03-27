package com.miracle.ui.core

import com.intellij.openapi.project.Project
import com.miracle.agent.mcp.McpPromptIntegration
import com.miracle.utils.AutoCompactResult
import com.miracle.utils.ChatHistoryAssistantMessage
import com.miracle.utils.ChatHistoryUserMessage
import com.miracle.utils.JsonLineChatHistory
import com.miracle.utils.JsonLineChatMemory
import com.miracle.utils.getChatDirectory
import com.miracle.utils.getDefaultAgentId
import com.miracle.utils.sanitizeFileName
import com.miracle.agent.parser.TextSegment
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.UserMessage
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.nio.file.Files
import java.util.UUID
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConversationCommandServiceTest {

    private val projectDir = Files.createTempDirectory("conversation-command-project")
    private val projectName = "conversation-command-${UUID.randomUUID()}"
    private val project: Project = mockk(relaxed = true)
    private val convId = "conv-${UUID.randomUUID()}"
    private val taskId = "task-${UUID.randomUUID()}"
    private val modelId = "test-model"
    private val convDir = Path(getChatDirectory()) / sanitizeFileName(projectName) / convId

    init {
        every { project.name } returns projectName
        every { project.basePath } returns projectDir.toString()
    }

    @AfterTest
    fun cleanup() {
        unmockkAll()
        runCatching { convDir.toFile().deleteRecursively() }
        runCatching { projectDir.toFile().deleteRecursively() }
    }

    @Test
    fun testResetConversationDeletesConversationStorage() {
        mockkObject(McpPromptIntegration)
        every { McpPromptIntegration.generateMcpPromptSection(project) } returns ""
        convDir.createDirectories()
        (convDir / "conversation.json").writeText("""{"id":"$convId","title":"demo"}""")
        (convDir / "chat-history.jsonl").writeText("history")
        (convDir / "chat-memory-${getDefaultAgentId()}.jsonl").writeText("memory")
        (convDir / "todo-agent-${getDefaultAgentId()}.json").writeText("todo")

        ConversationCommandService.resetConversation(taskId, convId, modelId, project)

        assertFalse(convDir.exists(), "Conversation directory should be removed after /clear")
    }

    @Test
    fun testCompactConversationUpdatesMemoryAndHistory() {
        mockkObject(McpPromptIntegration)
        every { McpPromptIntegration.generateMcpPromptSection(project) } returns ""
        val memoryStore = JsonLineChatMemory(convId, getDefaultAgentId(), project)
        val historyStore = JsonLineChatHistory(convId, project)
        memoryStore.add(UserMessage.userMessage("hello"))
        historyStore.add(ChatHistoryUserMessage(text = "hello"))

        mockkStatic("com.miracle.utils.Auto_compactKt")
        coEvery { com.miracle.utils.forceCompact(modelId, any(), any()) } returns AutoCompactResult(
            messages = listOf(AiMessage.aiMessage("compact summary")),
            wasCompacted = true,
        )

        val result = ConversationCommandService.compactConversation(taskId, convId, modelId, project)
        val history = JsonLineChatHistory(convId, project).messages()
        val compactedMemory = JsonLineChatMemory(convId, getDefaultAgentId(), project).messages()

        assertEquals("compact summary", result.summary)
        assertEquals(1, compactedMemory.size)
        assertTrue(compactedMemory.first() is AiMessage)
        assertEquals(3, history.size)
        assertEquals("/compact", (history[1] as ChatHistoryUserMessage).text)
        val assistantMessage = history[2] as ChatHistoryAssistantMessage
        assertEquals("compact summary", (assistantMessage.segments.first() as TextSegment).text)
    }
}
