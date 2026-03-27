package com.miracle.agent.parser

import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
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
    UiToolName.EDITED_EXISTING_FILE to ToolHeader("{{agent_name}} wants to edit this file:", AllIcons.Actions.Edit),
    UiToolName.NEW_FILE_CREATED to ToolHeader("{{agent_name}} wants to create a new file:", AllIcons.Actions.New),
    UiToolName.READ_FILE to ToolHeader("{{agent_name}} wants to read this file:", AllIcons.Actions.Preview),
    UiToolName.LIST_FILES_TOP_LEVEL to ToolHeader("{{agent_name}} wants to list files in this directory:", AllIcons.Nodes.Folder),
    UiToolName.LIST_FILES_RECURSIVE to ToolHeader(
        "{{agent_name}} wants to recursively view all files in this directory:",
        AllIcons.Nodes.Folder
    ),
    UiToolName.GLOB_FILES to ToolHeader(
        "{{agent_name}} wants to search files in this directory for {{pattern}}:",
        AllIcons.Actions.Find
    ),
    UiToolName.SEARCH_FILES to ToolHeader("{{agent_name}} wants to search this directory for {{pattern}}:", AllIcons.Actions.Find),
    UiToolName.RESOLVE_CLASS_NAME to ToolHeader("{{agent_name}} wants to resolve class name:", AllIcons.Actions.Find),
    UiToolName.RUN_COMMAND to ToolHeader("{{agent_name}} wants to run this command:", AllIcons.RunConfigurations.TestState.Run),
    UiToolName.COMMAND_OUTPUT to ToolHeader("{{agent_name}} wants to run this command:", AllIcons.RunConfigurations.TestState.Run),
    UiToolName.TODO_UPDATE to ToolHeader("{{agent_name}} is updating todos:", AllIcons.General.TodoDefault),
    UiToolName.TASK_START to ToolHeader("{{agent_name}} wants to run task {{subagent_type}}:", AllIcons.Nodes.Plugin),
    UiToolName.TASK_END to ToolHeader("Task {{subagent_type}} done:", AllIcons.Nodes.Plugin),
    UiToolName.MCP_TOOL_RESPONSE to ToolHeader("MCP tool response from {{server_name}}/{{tool_name}}:", AllIcons.Nodes.Plugin),
    UiToolName.EXCEL_READ to ToolHeader("{{agent_name}} wants to read this Excel file", AllIcons.FileTypes.XsdFile),
    UiToolName.ASK_USER_QUESTION to ToolHeader("{{agent_name}} has a question for you:", AllIcons.General.BalloonInformation),
    UiToolName.USE_SKILL to ToolHeader("{{agent_name}} wants to use skill {{skill_name}}", AllIcons.Nodes.Artifact),
    UiToolName.ENTER_PLAN_MODE to ToolHeader("{{agent_name}} wants to enter plan mode:", AllIcons.Actions.MenuOpen),
    UiToolName.EXIT_PLAN_MODE to ToolHeader("{{agent_name}} has completed planning:", AllIcons.Actions.MenuOpen),
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
    } ?: ToolHeader("Jarvis wants to run ${toolSegment.name.name}:", AllIcons.Actions.Edit)
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
