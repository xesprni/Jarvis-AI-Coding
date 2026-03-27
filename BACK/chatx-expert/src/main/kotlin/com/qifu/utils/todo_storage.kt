package com.qifu.utils

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.util.io.createDirectories
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.*


/**
 * Todo 项数据类
 */
@Serializable
data class TodoItem(
    val content: String,
    @Serializable(with = StatusSerializer::class)
    val status: Status,
    val activeForm: String = "",
) {
    enum class Status {
        PENDING, IN_PROGRESS, COMPLETED
    }

    object StatusSerializer : KSerializer<Status> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("TodoItem.Status", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): Status {
            val value = decoder.decodeString()
            return Status.entries
                .firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw SerializationException("Unknown status: $value")
        }

        override fun serialize(encoder: Encoder, value: Status) {
            encoder.encodeString(value.name)
        }
    }
}

/**
 * Todo 存储管理单例对象
 * 负责 Todo 项的内存缓存和文件持久化
 */
object TodoStorage {

    val LOG = thisLogger()
    
    // 内存缓存: key = "convId_agentId", value = List<TodoItem>
    private val todoDict = mutableMapOf<String, List<TodoItem>>()

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
    
    /**
     * 生成缓存 key
     */
    private fun getKey(convId: String, agentId: String): String {
        return "${convId}_${agentId}"
    }
    
    /**
     * 获取文件路径
     */
    private fun getFilePath(convId: String, agentId: String, project: Project): Path {
        val chatPath = Path(getChatDirectory()) / sanitizeFileName(project.name) / convId
        chatPath.createDirectories()
        return chatPath / "todo-agent-${agentId}.json"
    }
    
    /**
     * 设置 Todos（保存到内存和文件）
     * 
     * @param convId 会话 ID
     * @param agentId Agent ID
     * @param todos Todo 列表
     */
    fun setTodos(convId: String, agentId: String, todos: List<TodoItem>, project: Project) {
        val key = getKey(convId, agentId)
        todoDict[key] = todos
        
        val filePath = getFilePath(convId, agentId, project)
        val allCompleted = todos.all { it.status == TodoItem.Status.COMPLETED }
        
        if (allCompleted) {
            // 如果所有 todo 都完成了，删除文件并从内存中清除
            if (filePath.exists()) {
                filePath.deleteIfExists()
            }
            todoDict.remove(key)
        } else {
            // 否则保存到文件
            try {
                val jsonString = json.encodeToString(todos)
                filePath.writeText(jsonString, Charsets.UTF_8)
            } catch (e: Exception) {
                LOG.warn("Error saving todos to file: ${e.message}")
            }
        }
    }
    
    /**
     * 获取 Todos（优先从内存，其次从文件）
     * 
     * @param convId 会话 ID
     * @param agentId Agent ID
     * @return Todo 列表
     */
    fun getTodos(convId: String, agentId: String, project: Project): List<TodoItem> {
        val key = getKey(convId, agentId)
        todoDict[key]?.let { return it }
        
        // 如果内存中没有,尝试从文件中加载
        val filePath = getFilePath(convId, agentId, project)
        if (filePath.exists()) {
            try {
                val jsonString = filePath.readText(Charsets.UTF_8)
                val todos = json.decodeFromString<List<TodoItem>>(jsonString)
                todoDict[key] = todos
                return todos
            } catch (e: Exception) {
                LOG.warn("Warning: Failed to load todos from file ${filePath}: ${e.message}")
                return emptyList()
            }
        }
        
        return emptyList()
    }

    @JvmStatic
    fun clearConversationCache(convId: String) {
        val prefix = "${convId}_"
        val keysToRemove = todoDict.keys.filter { it.startsWith(prefix) }
        keysToRemove.forEach { todoDict.remove(it) }
    }

}
