package com.qifu.agent.parser

import com.intellij.ui.JBColor
import com.qihoo.finance.lowcode.common.util.IconUtil
import com.qihoo.finance.lowcode.common.util.Icons
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import java.awt.Color
import javax.swing.Icon


/**
 * 模型流式返回解析后的片段
 */
@Serializable
sealed class Segment(
    open val content: String = "",
    open val language: String = "",
    open val filePath: String? = null
) {
    @Deprecated("老UI调试用的方法，新UI不应调用此方法（新UI是根据Segment渲染，老UI使用markdown渲染）")
    open fun toMd(): String {
        return content
    }
}

// 纯文本渲染块
@Serializable
data class TextSegment(val text: String, var eventId: String? = null) : Segment(text)

// 错误消息渲染快
@Serializable
data class ErrorSegment(val text: String) : Segment(text)

// 工具调用渲染块
enum class UiToolName {
    EDITED_EXISTING_FILE,
    NEW_FILE_CREATED,
    READ_FILE,
    LIST_FILES_TOP_LEVEL,
    LIST_FILES_RECURSIVE,
    GLOB_FILES,
    SEARCH_FILES,
    RESOLVE_CLASS_NAME,
    LIST_IMPLEMENTATIONS,
    RUN_COMMAND,
    COMMAND_OUTPUT,
    TODO_UPDATE,
    TASK_START,
    TASK_END,
    MCP_TOOL,
    MCP_TOOL_RESPONSE,
    ASK_USER_QUESTION,
    EXCEL_READ,
    USER_EDIT,
    USE_SKILL,
    ENTER_PLAN_MODE,
    EXIT_PLAN_MODE,
}

data class ToolHeader(
    val text: String,
    val icon: Icon? = null
)

val UI_TOOL_HEADERS = mapOf(
    UiToolName.EDITED_EXISTING_FILE to ToolHeader("{{agent_name}} wants to edit this file:", IconUtil.getThemeAwareIcon(Icons.AI_EDIT_LIGHT, Icons.AI_EDIT)),
    UiToolName.NEW_FILE_CREATED to ToolHeader("{{agent_name}} wants to create a new file:", IconUtil.getThemeAwareIcon(Icons.AI_CREATE_LIGHT, Icons.AI_CREATE)),
    UiToolName.READ_FILE to ToolHeader("{{agent_name}} wants to read this file:", IconUtil.getThemeAwareIcon(Icons.AI_READ_LIGHT, Icons.AI_READ)),
    UiToolName.LIST_FILES_TOP_LEVEL to ToolHeader("{{agent_name}} wants to list files in this directory:", IconUtil.getThemeAwareIcon(Icons.AI_FLODER_LIGHT, Icons.AI_FLODER)),
    UiToolName.LIST_FILES_RECURSIVE to ToolHeader(
        "{{agent_name}} wants to recursively view all files in this directory:",
        IconUtil.getThemeAwareIcon(Icons.AI_FLODER_LIGHT, Icons.AI_FLODER)
    ),
    UiToolName.GLOB_FILES to ToolHeader(
        "{{agent_name}} wants to search files in this directory for {{pattern}}:",
        IconUtil.getThemeAwareIcon(Icons.AI_SEARCH_LIGHT, Icons.AI_SEARCH)
    ),
    UiToolName.SEARCH_FILES to ToolHeader("{{agent_name}} wants to search this directory for {{pattern}}:", IconUtil.getThemeAwareIcon(
        Icons.AI_SEARCH_LIGHT, Icons.AI_SEARCH)),
    UiToolName.RESOLVE_CLASS_NAME to ToolHeader("{{agent_name}} wants to resolve class name:", IconUtil.getThemeAwareIcon(Icons.AI_SEARCH_LIGHT, Icons.AI_SEARCH)),
    UiToolName.RUN_COMMAND to ToolHeader("{{agent_name}} wants to run this command:", IconUtil.getThemeAwareIcon(Icons.AI_COMMAND_LIGHT, Icons.AI_COMMAND)),
    UiToolName.COMMAND_OUTPUT to ToolHeader("{{agent_name}} wants to run this command:", IconUtil.getThemeAwareIcon(Icons.AI_COMMAND_LIGHT, Icons.AI_COMMAND)),
    UiToolName.TODO_UPDATE to ToolHeader("{{agent_name}} is updating todos:", IconUtil.getThemeAwareIcon(Icons.AI_TODO_LIGHT, Icons.AI_TODO)),
    UiToolName.TASK_START to ToolHeader("{{agent_name}} wants to run task {{subagent_type}}:", IconUtil.getThemeAwareIcon(Icons.AI_AGENT_LIGHT, Icons.AI_AGENT)),
    UiToolName.TASK_END to ToolHeader("Task {{subagent_type}} done:", IconUtil.getThemeAwareIcon(Icons.AI_AGENT_LIGHT, Icons.AI_AGENT)),
//    UiToolName.MCP_TOOL to ToolHeader("Jarvis wants to use {{tool_name}} from {{server_name}} MCP server:", Icons.AI_EDIT),
    UiToolName.MCP_TOOL_RESPONSE to ToolHeader("MCP tool response from {{server_name}}/{{tool_name}}:", IconUtil.getThemeAwareIcon(Icons.AI_EDIT_LIGHT, Icons.AI_EDIT)),
    UiToolName.EXCEL_READ to ToolHeader("{{agent_name}} wants to read this Excel file", IconUtil.getThemeAwareIcon(Icons.AI_READ_LIGHT, Icons.AI_READ)),
    UiToolName.ASK_USER_QUESTION to ToolHeader("{{agent_name}} has a question for you:", IconUtil.getThemeAwareIcon(Icons.AI_AGENT_LIGHT, Icons.AI_AGENT)),
    UiToolName.USE_SKILL to ToolHeader("{{agent_name}} wants to use skill {{skill_name}}", IconUtil.getThemeAwareIcon(Icons.AI_AGENT_LIGHT, Icons.AI_AGENT)),
    UiToolName.ENTER_PLAN_MODE to ToolHeader("{{agent_name}} wants to enter plan mode:", IconUtil.getThemeAwareIcon(Icons.AI_AGENT_LIGHT, Icons.AI_AGENT)),
    UiToolName.EXIT_PLAN_MODE to ToolHeader("{{agent_name}} has completed planning:", IconUtil.getThemeAwareIcon(Icons.AI_AGENT_LIGHT, Icons.AI_AGENT)),
)

@Serializable
data class ToolSegment(
    val name: UiToolName,
    val toolCommand: String,
    val toolContent: String = "",
    /** 想要替换到 header text 的值必须是 String 类型 */
    val params: Map<String, JsonElement> = emptyMap(),
) : Segment(toolContent, toolCommand) {

    @Deprecated("老UI调试用的方法，新UI不应调用此方法（新UI是根据Segment渲染，老UI使用markdown渲染）")
    override fun toMd(): String {
        return "${getToolSegmentHeader(this).text}\n```$toolCommand\n$toolContent\n```"
    }
}

fun getToolSegmentHeader(toolSegment: ToolSegment): ToolHeader {
    return UI_TOOL_HEADERS[toolSegment.name]?.let {
        var result = it.text
        for ((key, value) in toolSegment.params) {
            value as? JsonPrimitive ?: continue
            result = result.replace("{{$key}}", formatValueStyle(value.content))
        }
        return ToolHeader(result, it.icon)
    } ?: ToolHeader("Jarvis wants to run ${toolSegment.name.name}:", Icons.AI_EDIT)
}

fun formatValueStyle(value: String): String {
    var text = value.replace("<", "&lt;")
    text = text.replace(">", "&gt;")

    val textColor = JBColor(Color(0x8B4513), Color(0xD4AF37))
    val bgColor = JBColor(Color(0xF5F5F5), Color(0x404040))

    // 使用简化的内联样式，去掉不兼容的CSS属性
    return "<span style=\"color: ${toHex(textColor)}; background-color: ${toHex(bgColor)}; padding: 2px 4px; font-family: monospace;\">$text</span>"
//    return "<span style=\"color: #D4AF37; background-color: #404040; padding: 2px 4px; font-family: monospace;\">$text</span>"
}

// 转换为css十六进制
private fun toHex(color: Color): String {
    return String.format("#%02x%02x%02x", color.red, color.green, color.blue)
}

// 代码块渲染块头部 (前端历史会话没处理这里元素，不能存储，否则会多一条空消息)
@Serializable
data class CodeHeader(
    val codeLanguage: String,
    val codeFilePath: String?
) : Segment("", codeLanguage, codeFilePath) {

    override fun toMd(): String {
        return "```$codeLanguage${if (codeFilePath != null) ":$codeFilePath" else ""}"
    }
}

data class CodeHeaderWaiting(val partial: String) : Segment(partial)

@Serializable
data class Code(
    val code: String,
    val codeLanguage: String,
    val codeFilePath: String?
) : Segment(code, codeLanguage, codeFilePath)

// 代码块结束 (前端历史会话没处理这里元素，不能存储，否则会多一条空消息)
@Serializable
data class CodeEnd(val codeContent: String) : Segment(codeContent) {

    override fun toMd(): String {
        return "```"
    }
}

data class SearchWaiting(
    val search: String,
    override val language: String,
    override val filePath: String?
) : Segment(search, language, filePath)

data class ReplaceWaiting(
    val search: String,
    val replace: String,
    override val language: String,
    override val filePath: String?
) : Segment(replace, language, filePath)

@Serializable
data class SearchReplace(
    val search: String,
    val replace: String,
    val codeLanguage: String,
    val codeFilePath: String?
) : Segment(replace, codeLanguage, codeFilePath)
