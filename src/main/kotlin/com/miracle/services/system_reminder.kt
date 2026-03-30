package com.miracle.services

import com.intellij.openapi.project.Project
import com.miracle.ui.smartconversation.settings.configuration.ChatMode
import com.miracle.utils.ChatMessageStore
import com.miracle.utils.TodoItem
import com.miracle.utils.TodoStorage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.Content
import dev.langchain4j.data.message.TextContent
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.data.message.UserMessage.userMessage
import dev.langchain4j.memory.ChatMemory
import kotlin.collections.isNotEmpty
import kotlin.collections.joinToString

/**
 * 系统提醒消息数据类，用于在对话中注入系统级别的提示信息
 *
 * @param role 消息角色，通常为 "system"
 * @param content 消息内容
 * @param isMeta 是否为元信息消息
 * @param timestamp 消息时间戳
 * @param type 提醒类型
 * @param priority 优先级，取值为 'low'、'medium' 或 'high'
 * @param category 分类，取值为 'task'、'security'、'performance' 或 'general'
 */
data class ReminderMessage(
    val role: String = "system",
    val content: String,
    val isMeta: Boolean,
    val timestamp: Long,
    val type: String,
    val priority: String,           // 'low' | 'medium' | 'high'
    val category: String,           // 'task' | 'security' | 'performance' | 'general'
)


/**
 * 提醒事件枚举，定义系统中所有可触发的提醒事件类型
 */
enum class ReminderEvent(val value: String) {
    TODO_CHANGED("todo:changed"),
    TODO_FILE_CHANGED("todo:file_changed"),
    REMINDER_INJECTED("reminder:inject"),
    AGENT_MENTIONED("agent:mentioned"),
    FILE_MENTIONED("file:mentioned"),
    ASK_MODEL_MENTIONED("ask-model:mentioned"),
    ;

    /** 会话启动时的上下文信息 */
    data class SessionStartupContext(
        var agentId: String? = null,
        var messages: Int? = null,
        val timestamp: Long,
        var context: Map<String, String>? = null,
    )

    /** TODO 变更事件的上下文 */
    data class TodoChangedContext(
        val agentId: String
    )

    /** TODO 文件外部变更事件的上下文 */
    data class TodoFileChangedContext(
        var agentId: String? = null,
        val filePath: String,
        val reminder: String?,
        val timestamp: Long,
    )

    /** 外部注入提醒事件的上下文 */
    data class ReminderInjectedContext(
        val reminder: String,
        val agentId: String,
        val type: String,
        val timestamp: Long,
    )

    /** Agent 被提及事件的上下文 */
    data class AgentMentionedContext(
        val agentType: String,
        val originalMention: String,
        val timestamp: Long,
    )

    /** 文件被提及事件的上下文 */
    data class FileMentionedContext(
        val filePath: String,
        val originalMention: String,
        val timestamp: Long,
    )

    /** 文件读取事件的上下文 */
    data class FileReadContext(
        val filePath: String,
        val timestamp: Long,
        var size: Long,
        var modified: Long,
    )

    /** 文件编辑事件的上下文 */
    data class FileEditedContext(
        val filePath: String,
        val timestamp: Long,
        var contentLength: Int,
        var source: String,
    )

    /** 指定模型被提及事件的上下文 */
    data class AskModelMentionedContext(
        val modelName: String,
        val originalMention: String,
        val timestamp: Long,
    )
}


/**
 * System Reminder服务
 */
class SystemReminderService(private val convId: String, private val agentId: String, private val project: Project) {

    /** 提醒消息缓存 */
    private val reminderCache: MutableMap<String, ReminderMessage> = HashMap()
    /** 已发送过的提醒键集合，用于去重 */
    private val remindersSent: MutableSet<String> = mutableSetOf()
    /** 事件分发器，存储事件名到回调列表的映射 */
    private val eventDispatcher: MutableMap<String, MutableList<(Any) -> Unit>> = HashMap()
    /** 当前聊天模式 */
    var chatMode: ChatMode = ChatMode.AGENT

    companion object {
        const val TODO_CHANGED_KEY = "todo_changed"
        const val TODO_EMPTY_KEY = "todo_empty"
        const val PROJECT_INSTRUCTIONS_KEY = "project_instructions"
        const val PLAN_MODE_KEY = "plan_mode"
    }

    init {
        setupEventDispatcher()
    }

    /**
     * 生成当前会话的提醒消息列表，最多返回 5 条
     *
     * @return 提醒消息列表
     */
    fun generateReminders(): List<ReminderMessage> {
        val reminders = mutableListOf<ReminderMessage>()

        // Use lazy evaluation for performance with agent context
        val reminderGenerators: List<() -> List<ReminderMessage>?> = listOf(
            { dispatchTodoEvent()?.let { listOf(it) } },
            { getMentionReminders() },
            { getPlanModeReminder() }
        )

        for (generator in reminderGenerators) {
            if (reminders.size >= 5) break
            val result = generator()
            if (!result.isNullOrEmpty()) {
                reminders.addAll(result)
            }
        }
        return reminders
    }

    /**
     * 派发 TODO 相关的提醒事件，返回缓存中的或新生成的 TODO 提醒
     *
     * @return TODO 相关的提醒消息，无需提醒时返回 null
     */
    private fun dispatchTodoEvent(): ReminderMessage? {
        if (TODO_CHANGED_KEY in reminderCache) {
            remindersSent.add(TODO_CHANGED_KEY)
            val reminder = reminderCache[TODO_CHANGED_KEY]!!
            reminderCache.remove(TODO_CHANGED_KEY)
            return reminder
        }

        val todos = TodoStorage.getTodos(convId, agentId, project)
        if (todos.isEmpty() && !remindersSent.contains(TODO_EMPTY_KEY)) {
            remindersSent.add(TODO_EMPTY_KEY)
            return createReminderMessage(
                "todo", "task", "medium",
                "This is a reminder that your todo list is currently empty. DO NOT mention this to the user explicitly because they are already aware. If you are working on tasks that would benefit from a todo list please use the TodoWrite tool to create one. If not, please feel free to ignore. Again do not mention this message to the user.",
            )
        }

        if (todos.isNotEmpty() && !remindersSent.contains(TODO_CHANGED_KEY)) {
            remindersSent.add(TODO_CHANGED_KEY)
            val todoContent = todos.joinToString(",\n", "[\n", "\n]") { todo ->
                val contentPreview = if (todo.content.length > 100) todo.content.substring(0, 100) + "..." else todo.content
                val activeFormPreview = if (todo.activeForm.length > 100) todo.activeForm.substring(0, 100) + "..." else todo.activeForm
                """
                {
                  "content": "$contentPreview",
                  "status": "${todo.status}",
                  "activeForm": "$activeFormPreview"
                }
                """.trimIndent()
            }

            val reminder = createReminderMessage(
                "todo", "task", "medium",
                "Your todo list has changed. DO NOT mention this explicitly to the user. Here are the latest contents of your todo list:\n\n$todoContent. Continue on with the tasks at hand if applicable.",
            )
            return reminder
        }

        return null
    }

    /**
     * 获取缓存中的提及提醒消息，仅返回 5 秒内的未过期提醒
     *
     * @return 有效的提及提醒消息列表
     */
    private fun getMentionReminders(): List<ReminderMessage> {
        val currentTime = System.currentTimeMillis()
        val freshnessWindow = 5000L
        val reminders = mutableListOf<ReminderMessage>()
        val expiredKeys = mutableListOf<String>()

        // Single pass through cache for both collection and cleanup identification
        for ((key, reminder) in reminderCache) {
            if (isMentionReminder(reminder)) {
                val age = currentTime - reminder.timestamp
                if (age <= freshnessWindow) {
                    reminders.add(reminder)
                } else {
                    expiredKeys.add(key)
                }
            }
        }

        // Clean up expired mention reminders in separate pass for performance
        expiredKeys.forEach { reminderCache.remove(it) }
        return reminders
    }

    /**
     * 获取规划模式的提醒消息，仅在首次进入规划模式时生成
     *
     * @return 规划模式提醒消息列表，已提醒过时返回 null
     */
    private fun getPlanModeReminder(): List<ReminderMessage>? {
        if (chatMode != ChatMode.PLAN) return null
        if (!remindersSent.contains(PLAN_MODE_KEY)) {
            remindersSent.add(PLAN_MODE_KEY)
            val reminder = createReminderMessage(
                type = "plan_mode",
                category = "task",
                priority = "high",
                content = """Plan mode is active. Stay read-only and do not execute implementation work.

Use only the read-only planning tools available in this mode. Explore the codebase first, then ask focused follow-up questions only when they materially change the implementation plan.

When the spec is decision-complete, include exactly one <proposed_plan>...</proposed_plan> block in the assistant response. If you need a short handoff or clarification, put it after the closing tag in plain text. Do not write plan files, do not call plan-mode transition tools, and do not output multiple proposed plans in the same turn.""",
            )
            return listOf(reminder)
        }

        return null
    }

    /**
     * 判断提醒消息是否为提及类型（agent/file/ask_model）
     *
     * @param reminder 待判断的提醒消息
     * @return 是否为提及类型的提醒
     */
    private fun isMentionReminder(reminder: ReminderMessage): Boolean {
        val mentionTypes = setOf("agent_mention", "file_mention", "ask_model_mention")
        return mentionTypes.contains(reminder.type)
    }

    /**
     * 创建提醒消息，统一包装为 system-reminder 格式
     *
     * @param type 提醒类型
     * @param category 提醒分类
     * @param priority 优先级
     * @param content 提醒内容
     * @param timestamp 时间戳，默认为当前时间
     * @return 构建好的提醒消息
     */
    private fun createReminderMessage(type: String, category: String, priority: String, content: String, timestamp: Long = System.currentTimeMillis()): ReminderMessage {
        return ReminderMessage(
            role = "system",
            content = "<system-reminder>\n$content\n</system-reminder>",
            isMeta = true,
            timestamp = timestamp,
            type = type,
            priority = priority,
            category = category
        )
    }

    /**
     * 统一的提及提醒创建方法，内置去重逻辑
     *
     * @param type 提醒类型
     * @param key 去重键，已存在时跳过创建
     * @param category 提醒分类
     * @param priority 优先级
     * @param content 提醒内容
     * @param timestamp 时间戳
     */
    private fun createMentionReminder(type: String, key: String, category: String, priority: String, content: String, timestamp: Long) {
        if (!remindersSent.add(key)) return
        val reminder = createReminderMessage(
            type = type,
            category = category,
            priority = priority,
            content = content,
            timestamp = timestamp
        )
        reminderCache[key] = reminder
    }

    /**
     * 重置会话状态，清空提醒缓存和已发送记录
     */
    fun resetSession() {
        reminderCache.clear()
        remindersSent.clear()
    }

    /**
     * 注册事件监听器
     *
     * @param event 事件名称
     * @param callback 事件回调函数
     */
    fun addEventListener(event: String, callback: (Any) -> Unit) {
        eventDispatcher.computeIfAbsent(event) { mutableListOf() }.add(callback)
    }

    /**
     * 触发指定事件，通知所有注册的监听器
     *
     * @param event 事件名称
     * @param context 事件上下文数据
     */
    fun emitEvent(event: String, context: Any) {
        eventDispatcher[event]?.forEach {
            try {
                it(context)
            } catch (e: Exception) {
                println("Error in event listener for $event: $e")
            }
        }
    }

    /**
     * 初始化事件分发器，注册各类事件的监听器
     */
    private fun setupEventDispatcher() {
        // Todo change events
        addEventListener(ReminderEvent.TODO_CHANGED.value) { context ->
            remindersSent.remove(TODO_CHANGED_KEY)
            reminderCache.remove(TODO_CHANGED_KEY)
        }
        // Todo file changed externally
        addEventListener(ReminderEvent.TODO_FILE_CHANGED.value) { context ->
            remindersSent.remove(TODO_CHANGED_KEY)
            reminderCache.remove(TODO_CHANGED_KEY)
        }
        // Unified mention event handlers
        addEventListener(ReminderEvent.AGENT_MENTIONED.value) { context ->
            context as ReminderEvent.AgentMentionedContext
            createMentionReminder(
                type = "agent_mention",
                key = "agent_mention_${context.agentType}_${context.timestamp}",
                category = "task",
                priority = "high",
                content = "The user mentioned @${context.originalMention}. You MUST use the Task tool with subagent_type=\"${context.agentType}\" to delegate this task to the specified agent. Provide a detailed, self-contained task description that fully captures the user's intent for the ${context.agentType} agent to execute.",
                timestamp = context.timestamp
            )
        }
        addEventListener(ReminderEvent.FILE_MENTIONED.value) { context ->
            context as ReminderEvent.FileMentionedContext
            createMentionReminder(
                type = "file_mention",
                key = "file_mention_${context.filePath}_${context.timestamp}",
                category = "general",
                priority = "high",
                content = "The user mentioned @${context.originalMention}. You MUST read the entire content of the file at path: ${context.filePath} using the Read tool to understand the full context before proceeding with the user's request.",
                timestamp = context.timestamp
            )
        }
        addEventListener(ReminderEvent.ASK_MODEL_MENTIONED.value) { context ->
            context as ReminderEvent.AskModelMentionedContext
            createMentionReminder(
                type = "ask_model_mention",
                key = "ask_model_mention_${context.modelName}_${context.timestamp}",
                category = "task",
                priority = "high",
                content = "The user mentioned @${context.modelName}. You MUST use the AskExpertModelTool to consult this specific model for expert opinions and analysis. Provide the user's question or context clearly to get the most relevant response from ${context.modelName}.",
                timestamp = context.timestamp
            )
        }
    }

    /**
     * 将系统提醒注入到聊天消息中，包括提醒内容和项目规则文件
     *
     * @param chatMemory 聊天消息存储，用于读写对话历史
     */
    fun injectSystemReminder(chatMemory: ChatMessageStore<ChatMessage>) {
        // 1. 获取 System Reminder
        val contents = mutableListOf<Content>()
        val reminders = generateReminders().map {
            TextContent(it.content)
        }
        contents.addAll(reminders)

        // 2. 读取项目规则文件
        if (!remindersSent.contains("PROJECT_INSTRUCTIONS")) {
            remindersSent.add("PROJECT_INSTRUCTIONS")
            val projectInstruction = getProjectInstruction()
            if (!projectInstruction.isNullOrBlank()) contents.add(TextContent(projectInstruction))
        }

        // 3. 注入到UserMessage中
        injectContentToUserMessage(contents, chatMemory)
    }

    /**
     * 将内容注入到 UserMessage 中，放在已有消息内容之前
     *
     * @param contents 要注入的内容列表
     * @param chatMemory 聊天消息存储，用于读写对话历史
     */
    private fun injectContentToUserMessage(contents: List<Content>, chatMemory: ChatMessageStore<ChatMessage>) {
        if (contents.isEmpty()) return

        var messages = chatMemory.messages()
        var lastUserMessage = messages.lastOrNull().takeIf { it is UserMessage } as UserMessage?
        if (lastUserMessage == null) {
            lastUserMessage = userMessage(contents)
            chatMemory.add(lastUserMessage)
        } else {
            messages = messages.toMutableList()
            messages.removeLast()
            lastUserMessage = userMessage(contents + lastUserMessage.contents())
            messages.add(lastUserMessage)
            chatMemory.update(messages)
        }
    }

}
