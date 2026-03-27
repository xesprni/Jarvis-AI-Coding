package com.qifu.utils

import com.intellij.openapi.project.Project
import com.intellij.util.io.createDirectories
import com.qifu.agent.parser.Segment
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.ChatMessageDeserializer.messageFromJson
import dev.langchain4j.data.message.ChatMessageSerializer.messageToJson
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*


@Serializable
data class Conversation(
    val id: String = System.currentTimeMillis().toString(),
    var title: String? = null,
    var containsImg: Boolean = false,
    val createdTime: Long = System.currentTimeMillis(),
    val projectPath: String? = null,
)

@Serializable
sealed class ChatHistoryMessage()

@Serializable
data class ChatHistoryUserMessage(
    val text: String?,
    var referencedFilePaths: List<String?>? = null,
    var imageFilePaths: List<String?>? = null
) : ChatHistoryMessage()

@Serializable
data class ChatHistoryAssistantMessage(
    val segments: MutableList<Segment> = mutableListOf()
) : ChatHistoryMessage()


abstract class ChatMessageStore<T> {

    var inited = false
    private val messages: MutableList<T> = mutableListOf()

    private fun initMessages() {
        if (inited) {
            return
        }
        inited = true
        messages.addAll(loadMessages())
    }

    /**
     * 加载消息列表
     */
    protected abstract fun loadMessages(): List<T>

    /**
     * 添加一条消息
     */
    protected abstract fun addInternal(messages: T)

    protected fun addAllInternal(messages: List<T>) {
        messages.forEach {
            addInternal(it)
        }
    }

    /**
     * 覆盖消息列表
     */
    protected abstract fun updateInternal(messages: List<T>)

    fun messages(): List<T> {
        initMessages()
        return messages
    }

    fun add(message: T) {
        initMessages()
        addInternal(message)
        messages.add(message)
    }

    fun addAll(newMessages: List<T>) {
        initMessages()
        addAllInternal(newMessages)
        messages.addAll(newMessages)
    }

    fun update(newMessages: List<T>) {
        initMessages()
        // 找出新旧消息的公共部分长度
        var commonLength = 0
        for (i in 0 until minOf(this.messages.size, newMessages.size)) {
            if (this.messages[i] == newMessages[i]) {
                commonLength++
            } else {
                break
            }
        }

        if (commonLength == this.messages.size) {
            // 只新增的情况，追加差异部分
            val additionalMessages = newMessages.subList(commonLength, newMessages.size)
            addAll(additionalMessages)
        } else {
            // 有修改的情况，替换整个列表
            updateInternal(newMessages)
            this.messages.clear()
            this.messages.addAll(newMessages)
        }
    }

}

class JsonLineChatHistory(val convId: String, val project: Project): ChatMessageStore<ChatHistoryMessage>() {
    
    val filePath: String

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = true
    }
    
    init {
        val path = Path(getChatDirectory()) / sanitizeFileName(project.name) / convId / "chat-history.jsonl"
        path.parent.createDirectories()
        filePath = path.pathString
    }

    override fun loadMessages(): List<ChatHistoryMessage> {
        val file = File(filePath)
        if (!file.exists()) {
            return emptyList()
        }
        
        return file.readLines()
            .filter { it.isNotBlank() }
            .map { line ->
                json.decodeFromString<ChatHistoryMessage>(line)
            }
    }

    override fun addInternal(messages: ChatHistoryMessage) {
        val file = File(filePath)
        val jsonLine = json.encodeToString(messages)
        file.appendText(jsonLine + "\n")
    }

    override fun updateInternal(messages: List<ChatHistoryMessage>) {
        val file = File(filePath)
        val content = messages.joinToString("\n") { message ->
            json.encodeToString(message)
        }
        if (content.isNotEmpty()) {
            file.writeText(content + "\n")
        } else {
            file.writeText("")
        }
    }

}

class JsonLineChatMemory(val convId: String, val agentId: String, val project: Project): ChatMessageStore<ChatMessage>() {

    val filePath: String

    init {
        val path = Path(getChatDirectory())/ sanitizeFileName(project.name) / convId / "chat-memory-$agentId.jsonl"
        path.parent.createDirectories()
        filePath = path.pathString
    }

    override fun loadMessages(): List<ChatMessage> {
        val file = File(filePath)
        if (!file.exists()) {
            return emptyList()
        }
        
        val messages = mutableListOf<ChatMessage>()
        file.readLines()
            .filter { it.isNotBlank() }
            .forEach { line ->
                val parsedMessage = messageFromJson(line)
                messages.add(parsedMessage)
            }
        
        return messages
    }

    override fun addInternal(messages: ChatMessage) {
        val file = File(filePath)
        val jsonLine = messageToJson(messages)
        file.appendText(jsonLine + "\n")
    }

    override fun updateInternal(messages: List<ChatMessage>) {
        val file = File(filePath)
        val content = messages.joinToString("\n") { message ->
            messageToJson(message)
        }
        if (content.isNotEmpty()) {
            file.writeText(content + "\n")
        } else {
            file.writeText("")
        }
    }

}

object ConversationStore {

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    @JvmStatic
    fun getConvBaseDir(project: Project): Path {
        val convDir = Path(getChatDirectory()) / sanitizeFileName(project.name)
        convDir.createDirectories()
        return convDir
    }

    @JvmStatic
    fun getConversations(project: Project): List<Conversation> {
        val convDir = getConvBaseDir(project)
        
        // 读取目录下的所有子文件夹
        return convDir.listDirectoryEntries()
            .filter { it.isDirectory() }
            .mapNotNull { dir ->
                // 读取每个文件夹下的 conversation.json
                val convFile = dir / "conversation.json"
                if (convFile.exists() && convFile.isRegularFile()) {
                    try {
                        val jsonContent = convFile.readText()
                        json.decodeFromString<Conversation>(jsonContent)
                    } catch (e: Exception) {
                        // 如果解析失败，返回 null
                        null
                    }
                } else {
                    dir.toFile().deleteRecursively()
                    null
                }
            }
            .sortedByDescending { it.createdTime } // 按创建时间倒序排列
    }

    @JvmStatic
    fun getConversation(project: Project, convId: String): Conversation? {
        val convFile = getConvBaseDir(project) / convId / "conversation.json"
        
        return if (convFile.exists() && convFile.isRegularFile()) {
            try {
                val jsonContent = convFile.readText()
                json.decodeFromString<Conversation>(jsonContent)
            } catch (e: Exception) {
                // 如果解析失败，返回 null
                null
            }
        } else {
            null
        }
    }
    
    /**
     * 保存会话信息到文件
     */
    @JvmStatic
    fun updateConversation(project: Project, conversation: Conversation) {
        val convDir = getConvBaseDir(project) / conversation.id
        convDir.createDirectories()
        
        val convFile = convDir / "conversation.json"
        val jsonContent = json.encodeToString(conversation)
        convFile.writeText(jsonContent)
    }
    
    /**
     * 删除会话
     */
    @JvmStatic
    fun deleteConversation(project: Project, convId: String) {
        val convDir = Path(getChatDirectory()) / sanitizeFileName(project.name) / convId
        if (convDir.exists()) {
            convDir.toFile().deleteRecursively()
        }
    }

    @JvmStatic
    fun deleteOldVersionsFile() {
        val userPath = Path(getUserConfigDirectory())
        val oldVersionFiles = listOf(
            "intellij-conversations.db",
            "intellij-chat",
        )
        oldVersionFiles.forEach {
            val file = userPath / it
            if (file.exists()) {
                file.toFile().deleteRecursively()
            }
        }
    }
}