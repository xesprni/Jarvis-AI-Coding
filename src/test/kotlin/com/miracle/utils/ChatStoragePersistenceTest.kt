package com.miracle.utils

import com.intellij.openapi.project.Project
import io.mockk.every
import io.mockk.mockk
import java.nio.file.Files
import java.util.UUID
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.pathString
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChatStoragePersistenceTest {

    private val projectDir = Files.createTempDirectory("chat-storage-project")
    private val projectName = "chat-storage-${UUID.randomUUID()}"
    private val project: Project = mockk(relaxed = true)
    private val projectChatDir = Path(getChatDirectory()) / sanitizeFileName(projectName)

    init {
        every { project.name } returns projectName
        every { project.basePath } returns projectDir.toString()
    }

    @AfterTest
    fun cleanup() {
        runCatching { projectChatDir.toFile().deleteRecursively() }
        runCatching { projectDir.toFile().deleteRecursively() }
    }

    @Test
    fun testConversationStoreSortsByUpdatedTimeAndReadsLegacyConversation() {
        val legacyId = "legacy-${UUID.randomUUID()}"
        val recentId = "recent-${UUID.randomUUID()}"
        val convBaseDir = ConversationStore.getConvBaseDir(project)

        val legacyDir = convBaseDir / legacyId
        legacyDir.createDirectories()
        (legacyDir / "conversation.json").writeText(
            """
            {"id":"$legacyId","title":"Legacy","containsImg":false,"createdTime":1000,"projectPath":"${projectDir.pathString}"}
            """.trimIndent()
        )

        ConversationStore.updateConversation(
            project,
            Conversation(
                id = recentId,
                title = "Recent",
                createdTime = 2000,
                updatedTime = 9000,
                projectPath = projectDir.pathString,
            )
        )

        val conversations = ConversationStore.getConversations(project)
        assertEquals(listOf(recentId, legacyId), conversations.map { it.id })
        assertEquals(1000, ConversationStore.getConversation(project, legacyId)?.updatedTime)
    }

    @Test
    fun testJsonLineChatHistoryPersistsMessageIdAndLoadsLegacyUserMessage() {
        val convId = "conv-${UUID.randomUUID()}"
        val store = JsonLineChatHistory(convId, project)

        store.add(ChatHistoryUserMessage(text = "legacy-message"))
        store.add(ChatHistoryUserMessage(text = "message-with-id", messageId = "msg-1"))
        store.add(ChatHistoryAssistantMessage())

        val loadedMessages = JsonLineChatHistory(convId, project).messages()
        val userMessages = loadedMessages.filterIsInstance<ChatHistoryUserMessage>()

        assertEquals(2, userMessages.size)
        assertNull(userMessages[0].messageId)
        assertEquals("msg-1", userMessages[1].messageId)
        assertTrue(loadedMessages.any { it is ChatHistoryAssistantMessage })
    }
}
