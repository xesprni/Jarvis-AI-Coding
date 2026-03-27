//package com.miracle.utils
//
//import dev.langchain4j.data.message.ChatMessage
//import dev.langchain4j.data.message.SystemMessage
//import dev.langchain4j.data.message.UserMessage
//import dev.langchain4j.data.message.AiMessage
//import dev.langchain4j.data.message.AiMessage.aiMessage
//import dev.langchain4j.data.message.SystemMessage.systemMessage
//import dev.langchain4j.data.message.UserMessage.userMessage
//import org.junit.FixMethodOrder
//import org.junit.runners.MethodSorters
//import kotlin.test.*
//
//@FixMethodOrder(MethodSorters.NAME_ASCENDING)
//class ChatStoreTest {
//
//    // 在每个测试之前清理状态
////    @BeforeTest
//    fun setUp() {
//        // 清理所有会话数据
//        clearAllData()
//    }
//
//    private fun clearAllData() {
//        // 获取所有会话并删除
//        val conversations = ChatStore.getConversations()
//        conversations.forEach { conversation ->
//            ChatStore.deleteConversation(conversation.id)
//            ChatStore.deleteChatMemory(conversation.id)
//            ChatStore.deleteChatHistory(conversation.id)
//        }
//    }
//
////    @Test
//    fun test() {
////        ChatStore.updateConversation(Conversation(title = "LiuZhenghua Conversation", id = "1234"))
//
//        val conversation = ChatStore.getConversation("1234")
//        assertNotNull(conversation, "Conversation should not be null")
//        println(conversation)
//
////        val messages = listOf<ChatMessage>(
////            systemMessage("You are a helpful assistant."),
////            userMessage("Who are you?"),
////            aiMessage("I am LangChain4j.")
////        )
////
////        ChatStore.updateChatMemory("1234", messages)
////        ChatStore.updateChatHistory("1234", messages)
//
//        val chatMemory = ChatStore.getChatMemory("1234")
//        assertNotNull(chatMemory, "Chat memory should not be null")
//        assertEquals(3, chatMemory.size, "Chat memory should have 3 messages")
//        println(chatMemory)
//
//        val chatHistory = ChatStore.getChatHistory("1234")
//        assertNotNull(chatHistory, "Chat history should not be null")
//        assertEquals(3, chatHistory.size, "Chat history should have 3 messages")
//    }
//
//    @Test
//    fun testCreateAndGetConversation() {
//        // 测试创建会话
//        val conversation = Conversation(title = "Test Conversation")
//        val createdConversation = ChatStore.updateConversation(conversation)
//
//        assertNotNull(createdConversation.id, "Conversation ID should not be null")
//        assertEquals("Test Conversation", createdConversation.title, "Conversation title should match")
//        assertTrue(createdConversation.createdTime > 0, "Created time should be set")
//
//        // 测试获取会话
//        val retrievedConversations = ChatStore.getConversations()
//        assertTrue("Retrieved conversations should contains ${createdConversation.id}") {
//            retrievedConversations.any{ it.id == createdConversation.id }
//        }
//    }
//
//    @Test
//    fun testGetConversations() {
//        // 测试获取所有会话
//        val conversations = ChatStore.getConversations()
//        assertTrue(conversations.size >= 2, "Should have at least 2 conversations")
//
//        // 验证会话按创建时间倒序排列
//        assertTrue(conversations[0].createdTime >= conversations[1].createdTime,
//                  "Conversations should be sorted by created time descending")
//    }
//
//    @Test
//    fun testUpdateConversation() {
//        // 创建会话
//        val conversation = ChatStore.updateConversation(Conversation(title = "Original Title"))
//
//        // 更新会话
//        val updatedConversation = conversation.copy(title = "Updated Title")
//        val result = ChatStore.updateConversation(updatedConversation)
//
//        assertEquals("Updated Title", result.title, "Title should be updated")
//
//        // 验证更新后的会话
//        val retrievedConversation = ChatStore.getConversations().firstOrNull { it.id == conversation.id}
//        assertNotNull(retrievedConversation, "Retrieved conversation should not be null")
//        assertEquals("Updated Title", retrievedConversation.title, "Title should be updated in storage")
//    }
//
//    @Test
//    fun testDeleteConversation() {
//        // 创建会话
//        val conversation = ChatStore.updateConversation(Conversation(title = "To Delete"))
//
//        // 验证会话存在
//        val retrievedBefore = ChatStore.getConversations().firstOrNull { it.id == conversation.id}
//        assertNotNull(retrievedBefore, "Conversation should exist before deletion")
//
//        // 删除会话
//        ChatStore.deleteConversation(conversation.id)
//
//        // 验证会话已删除
//        val retrievedAfter = ChatStore.getConversations().firstOrNull { it.id == conversation.id}
//        assertNull(retrievedAfter, "Conversation should be null after deletion")
//    }
//
//    @Test
//    fun testChatMemoryOperations() {
//        val conversationId = "1"
//
//        // 创建测试消息
//        val messages = listOf<ChatMessage>(
//            systemMessage("System message"),
//            userMessage("User message"),
//            aiMessage("AI response")
//        )
//
//        // 测试更新聊天内存
//        ChatStore.updateChatMemory(conversationId, messages)
//
//        // 测试获取聊天内存
//        val retrievedMessages = ChatStore.getChatMemory(conversationId)
//        assertNotNull(retrievedMessages, "Retrieved messages should not be null")
//        assertEquals(3, retrievedMessages.size, "Should have 3 messages")
//
//        // 验证消息类型和内容
//        assertTrue(retrievedMessages[0] is SystemMessage, "First message should be SystemMessage")
//        assertTrue(retrievedMessages[1] is UserMessage, "Second message should be UserMessage")
//        assertTrue(retrievedMessages[2] is AiMessage, "Third message should be AiMessage")
//
//        // 测试删除聊天内存
//        ChatStore.deleteChatMemory(conversationId)
//        val deletedMessages = ChatStore.getChatMemory(conversationId)
//        assertNull(deletedMessages, "Messages should be null after deletion")
//    }
//
//    @Test
//    fun testNonExistentData() {
//        // 测试获取不存在的聊天内存
//        val nonExistentMemory = ChatStore.getChatMemory("999999")
//        assertNull(nonExistentMemory, "Non-existent chat memory should return null")
//
//        // 测试获取不存在的聊天历史
//        val nonExistentHistory = ChatStore.getChatHistory("999999")
//        assertNull(nonExistentHistory, "Non-existent chat history should return null")
//
//        // 测试获取不存在的会话
//        val nonExistentConversation = ChatStore.getConversations().firstOrNull { it.id == "non-existent-id"}
//        assertNull(nonExistentConversation, "Non-existent conversation should return null")
//    }
//}
