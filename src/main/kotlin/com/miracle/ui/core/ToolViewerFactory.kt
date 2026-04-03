package com.miracle.ui.core

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.miracle.agent.parser.ToolSegment
import com.miracle.agent.parser.UiToolName
import com.miracle.ui.core.ChatTheme.MUTED_FOREGROUND
import com.miracle.ui.core.ChatTheme.PANEL_BACKGROUND
import com.miracle.ui.core.ChatTheme.PRETTY_JSON
import com.miracle.ui.core.ChatTheme.TOOL_CONTENT_BACKGROUND
import com.miracle.agent.tool.RequestUserInputQuestion
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.io.File
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.SwingConstants

/**
 * Creates Swing viewers for specific [ToolSegment] tool types.
 *
 * This class encapsulates all the "leaf" viewers that [SegmentRendererFactory]
 * delegates to when rendering tool segments, list/status/mcp/ask viewers,
 * terminal output, and related helpers (file resolution, syntax detection, etc.).
 */
internal class ToolViewerFactory(
    private val project: Project,
    private val scrollManager: ChatScrollManager,
    private val renderer: SegmentRendererFactory,
    private val onAskReply: ((String) -> Unit)? = null,
) {

    private val json = Json { ignoreUnknownKeys = true }

    // ── Tool body dispatcher ─────────────────────────────────────────

    /**
     * 根据工具段类型创建对应的工具内容查看器。
     *
     * @param segment 工具段
     * @return 对应的 Swing 查看器组件
     */
    fun createToolBody(segment: ToolSegment): JComponent {
        return when (segment.name) {
            UiToolName.RUN_COMMAND, UiToolName.COMMAND_OUTPUT ->
                createTerminalViewer(segment.toolContent.ifBlank { "Waiting for command output..." })

            UiToolName.NEW_FILE_CREATED, UiToolName.EDITED_EXISTING_FILE,
            UiToolName.READ_FILE, UiToolName.EXCEL_READ,
            UiToolName.USER_EDIT, UiToolName.EXIT_PLAN_MODE ->
                renderer.createSyntaxViewer(segment.toolContent.ifBlank { "" }, segment.toolCommand)

            UiToolName.LIST_FILES_TOP_LEVEL, UiToolName.LIST_FILES_RECURSIVE,
            UiToolName.GLOB_FILES, UiToolName.SEARCH_FILES,
            UiToolName.LIST_IMPLEMENTATIONS, UiToolName.RESOLVE_CLASS_NAME,
            UiToolName.TODO_UPDATE ->
                createListViewer(segment)

            UiToolName.TASK_START, UiToolName.TASK_END,
            UiToolName.USE_SKILL, UiToolName.ENTER_PLAN_MODE ->
                createStatusViewer(segment)

            UiToolName.MCP_TOOL, UiToolName.MCP_TOOL_RESPONSE ->
                createMcpViewer(segment)

            UiToolName.ASK_USER_QUESTION ->
                createAskUserQuestionViewer(segment)

            UiToolName.REQUEST_USER_INPUT ->
                createRequestUserInputViewer(segment)
        }
    }

    // ── Tool tips header ─────────────────────────────────────────────

    /**
     * 创建工具提示头部，包含图标和标题文本。
     *
     * @param header 工具头部信息
     * @return 头部面板组件
     */
    fun createToolTipsHeader(header: com.miracle.agent.parser.ToolHeader): JComponent {
        val htmlText = """
            <html>
              <body>
                <p style='margin-top: 4px; margin-bottom: 4px; font-weight: bold; margin-left: 4px;'>${header.text}</p>
              </body>
            </html>
        """.trimIndent()
        val textPane = renderer.createHtmlPane(htmlText, false).apply {
            border = JBUI.Borders.empty(2, 0)
        }
        return JPanel(BorderLayout()).apply {
            isOpaque = true; background = PANEL_BACKGROUND
            border = JBUI.Borders.emptyBottom(4)
            add(JLabel(header.icon).apply { border = JBUI.Borders.emptyRight(6) }, BorderLayout.WEST)
            add(textPane, BorderLayout.CENTER)
        }
    }

    // ── Small section header ─────────────────────────────────────────

    /**
     * 创建带有图标的小节标题标签。
     *
     * @param text 标题文本
     * @param icon 标题图标
     * @return 标签组件
     */
    fun createSmallSectionLabel(text: String, icon: javax.swing.Icon): JComponent {
        return JBLabel(text, icon, SwingConstants.LEFT).apply {
            font = JBFont.small(); foreground = MUTED_FOREGROUND
            border = JBUI.Borders.emptyBottom(4)
        }
    }

    // ── Tool title resolution ────────────────────────────────────────

    /**
     * 根据工具段类型解析显示标题。
     *
     * @param segment 工具段
     * @return 解析后的标题字符串
     */
    fun resolveToolTitle(segment: ToolSegment): String {
        return when (segment.name) {
            UiToolName.RUN_COMMAND, UiToolName.COMMAND_OUTPUT -> "$ ${segment.toolCommand}"
            UiToolName.NEW_FILE_CREATED, UiToolName.EDITED_EXISTING_FILE,
            UiToolName.READ_FILE, UiToolName.EXCEL_READ,
            UiToolName.USER_EDIT -> renderer.displayPath(segment.toolCommand)
            UiToolName.MCP_TOOL, UiToolName.MCP_TOOL_RESPONSE -> {
                val server = segment.params["server_name"]?.toString()?.trim('"').orEmpty()
                val tool = segment.params["tool_name"]?.toString()?.trim('"').orEmpty()
                listOf(server, tool).filter { it.isNotBlank() }.joinToString("/").ifBlank { "mcp-tool" }
            }
            else -> segment.toolCommand.ifBlank { segment.name.name.lowercase() }
        }
    }

    // ── Syntax detection ─────────────────────────────────────────────

    /**
     * 根据文件扩展名检测语法高亮类型。
     *
     * @param filePath 文件路径
     * @return RSyntaxTextArea 语法常量
     */
    fun detectSyntax(filePath: String): String {
        return when {
            filePath.endsWith(".java", true) -> SyntaxConstants.SYNTAX_STYLE_JAVA
            filePath.endsWith(".kt", true) -> SyntaxConstants.SYNTAX_STYLE_KOTLIN
            filePath.endsWith(".py", true) -> SyntaxConstants.SYNTAX_STYLE_PYTHON
            filePath.endsWith(".html", true) -> SyntaxConstants.SYNTAX_STYLE_HTML
            filePath.endsWith(".md", true) -> SyntaxConstants.SYNTAX_STYLE_MARKDOWN
            filePath.endsWith(".json", true) -> SyntaxConstants.SYNTAX_STYLE_JSON
            filePath.endsWith(".yml", true) || filePath.endsWith(".yaml", true) -> SyntaxConstants.SYNTAX_STYLE_YAML
            filePath.endsWith(".xml", true) -> SyntaxConstants.SYNTAX_STYLE_XML
            filePath.endsWith(".sql", true) -> SyntaxConstants.SYNTAX_STYLE_SQL
            else -> SyntaxConstants.SYNTAX_STYLE_NONE
        }
    }

    // ── Terminal viewer ──────────────────────────────────────────────

    private fun createTerminalViewer(content: String): JComponent {
        val terminalPane = javax.swing.JTextPane().apply {
            isEditable = false
            font = Font(Font.MONOSPACED, Font.PLAIN, JBUI.scale(12))
            text = content
            background = TOOL_CONTENT_BACKGROUND
            foreground = JBColor(Color(60, 60, 60), Color(204, 204, 204))
            border = JBUI.Borders.empty(6, 8)
        }
        return JPanel(BorderLayout()).apply {
            isOpaque = true
            background = TOOL_CONTENT_BACKGROUND
            border = JBUI.Borders.empty(4, 8)
            add(JScrollPane(terminalPane).apply {
                border = null
                background = TOOL_CONTENT_BACKGROUND
                viewport.background = TOOL_CONTENT_BACKGROUND
                preferredSize = Dimension(JBUI.scale(640), JBUI.scale(200))
                minimumSize = Dimension(0, JBUI.scale(96))
                scrollManager.installNestedScrollBridge(this)
            }, BorderLayout.CENTER)
        }
    }

    // ── List viewer ──────────────────────────────────────────────────

    private fun createListViewer(segment: ToolSegment): JComponent {
        val lines = segment.toolContent.lines().map { it.trimEnd() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return renderer.createMarkdownBlock("No results found.")

        val headerText = if (lines.size > 1) lines.first() else null
        val items = if (lines.size > 1) lines.drop(1) else lines
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = true
            background = TOOL_CONTENT_BACKGROUND
            border = JBUI.Borders.empty(8, 12, 8, 12)
            headerText?.let {
                add(JBTextArea(it).apply {
                    isEditable = false; isOpaque = false; lineWrap = true; wrapStyleWord = true
                    foreground = JBColor(Color(120, 120, 120), Color(170, 170, 170))
                    border = JBUI.Borders.emptyBottom(8); font = JBFont.small()
                    alignmentX = Component.LEFT_ALIGNMENT
                })
            }
            items.forEach { item -> add(createListItemLabel(segment, item)) }
        }
    }

    // ── Status viewer ────────────────────────────────────────────────

    private fun createStatusViewer(segment: ToolSegment): JComponent {
        val summary = when (segment.name) {
            UiToolName.TASK_START -> segment.toolCommand.ifBlank { "Task started." }
            UiToolName.TASK_END -> segment.toolCommand.ifBlank { "Task completed." }
            UiToolName.USE_SKILL -> {
                segment.params["skill_name"]?.toString()?.trim('"')
                    ?.let { "Skill: $it" }
                    ?: segment.toolCommand.ifBlank { "Skill in use." }
            }
            UiToolName.ENTER_PLAN_MODE -> segment.toolCommand.ifBlank { "Plan mode requested." }
            else -> segment.toolCommand.ifBlank { "Status updated." }
        }
        val detail = segment.toolContent.ifBlank {
            when (segment.name) {
                UiToolName.TASK_END -> "Task completed."
                UiToolName.ENTER_PLAN_MODE -> "Do you want me to create a plan before implementing this task?"
                else -> ""
            }
        }
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = true
            background = TOOL_CONTENT_BACKGROUND
            border = JBUI.Borders.empty(8, 12, 8, 12)
            add(JBTextArea(summary).apply {
                isEditable = false; isOpaque = false; lineWrap = true; wrapStyleWord = true
                font = JBFont.label().asBold(); foreground = JBColor.foreground()
                alignmentX = Component.LEFT_ALIGNMENT
            })
            if (detail.isNotBlank()) {
                add(Box.createVerticalStrut(JBUI.scale(8)))
                add(renderer.createMarkdownBlock(detail))
            }
        }
    }

    // ── List item label (clickable file link) ────────────────────────

    private fun createListItemLabel(segment: ToolSegment, item: String): JComponent {
        val cleanItem = item.trim()
        val text = JBTextArea(cleanItem).apply {
            isEditable = false; isOpaque = false; lineWrap = true; wrapStyleWord = true
            border = JBUI.Borders.empty(3, 0, 3, 12)
            foreground = JBColor(Color(74, 74, 74), Color(200, 200, 200))
            font = font.deriveFont(Font.PLAIN, 12f)
        }
        val container = JPanel(BorderLayout()).apply {
            isOpaque = false; alignmentX = Component.LEFT_ALIGNMENT
            add(text, BorderLayout.CENTER)
        }

        val resolved = resolveCandidateFile(segment, cleanItem)
        if (resolved != null) {
            val hoverBg = JBColor(Color(170, 170, 170), Color(85, 85, 85))
            container.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            text.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            val listener = object : java.awt.event.MouseAdapter() {
                override fun mouseClicked(e: java.awt.event.MouseEvent) {
                    val vf = LocalFileSystem.getInstance().findFileByIoFile(resolved) ?: return
                    if (!vf.isValid) return
                    FileEditorManager.getInstance(project).openFile(vf, true)
                }
                override fun mouseEntered(e: java.awt.event.MouseEvent) {
                    text.foreground = JBColor(Color.WHITE, Color.WHITE)
                    container.background = hoverBg; container.isOpaque = true
                    text.background = hoverBg; text.isOpaque = true
                }
                override fun mouseExited(e: java.awt.event.MouseEvent) {
                    text.foreground = JBColor(Color(74, 74, 74), Color(200, 200, 200))
                    container.isOpaque = false; text.isOpaque = false
                }
            }
            container.addMouseListener(listener)
            text.addMouseListener(listener)
        }
        return container
    }

    // ── MCP viewer ───────────────────────────────────────────────────

    private fun createMcpViewer(segment: ToolSegment): JComponent {
        val description = segment.params["tool_description"]?.toString()?.trim('"')
        val serverName = segment.params["server_name"]?.toString()?.trim('"')
        val callArguments = segment.params["call_arguments"]
        val inputSchema = segment.params["input_schema"]?.jsonObject
        val content = prettyToolContent(segment.toolContent)
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = true
            background = TOOL_CONTENT_BACKGROUND
            border = JBUI.Borders.empty(8, 12, 8, 12)
            // Tool description
            if (!description.isNullOrBlank()) {
                add(renderer.createMarkdownBlock(description))
                add(Box.createVerticalStrut(JBUI.scale(8)))
            }
            if (!serverName.isNullOrBlank()) {
                add(JBLabel("Server: $serverName").apply {
                    foreground = MUTED_FOREGROUND; font = JBFont.small()
                    border = JBUI.Borders.emptyBottom(6)
                })
            }
            // Parameter schema
            if (inputSchema != null) {
                val properties = inputSchema["properties"]?.jsonObject
                val requiredList = inputSchema["required"]?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                    ?.toSet().orEmpty()
                if (properties != null && properties.isNotEmpty()) {
                    add(createSmallSectionLabel("Parameters", AllIcons.Nodes.Parameter))
                    properties.forEach { (paramName, paramSchema) ->
                        add(createParamRow(paramName, paramSchema, paramName in requiredList))
                    }
                    add(Box.createVerticalStrut(JBUI.scale(8)))
                }
            }
            // Call arguments
            if (callArguments != null) {
                add(createSmallSectionLabel("Arguments", AllIcons.Actions.Edit))
                add(renderer.createSyntaxViewer(
                    prettyJsonElement(callArguments),
                    "mcp.json",
                    preferredHeight = 180
                ))
                add(Box.createVerticalStrut(JBUI.scale(8)))
                add(createSmallSectionLabel("Response", AllIcons.Nodes.Plugin))
            }
            add(renderer.createSyntaxViewer(content, "mcp.json", preferredHeight = 180))
        }
    }

    /**
     * 创建单个参数描述行，展示参数名、类型、必填状态和描述
     */
    private fun createParamRow(name: String, schema: JsonElement, required: Boolean): JComponent {
        val schemaObj = schema.jsonObject
        val type = schemaObj["type"]?.jsonPrimitive?.contentOrNull ?: "any"
        val desc = schemaObj["description"]?.jsonPrimitive?.contentOrNull
        val default = schemaObj["default"]
        val enumValues = schemaObj["enum"]?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            ?.takeIf { it.isNotEmpty() }
        return JPanel(BorderLayout(JBUI.scale(8), 0)).apply {
            isOpaque = false
            border = JBUI.Borders.empty(3, 0)
            // Left: name + type + badges
            val leftPanel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                alignmentY = Component.TOP_ALIGNMENT
                // Line 1: param name
                add(JBLabel(name).apply {
                    font = JBFont.label().asBold()
                    foreground = JBColor.foreground()
                })
                // Line 2: type + required badge
                add(JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(4), 0)).apply {
                    isOpaque = false
                    border = JBUI.Borders.emptyTop(2)
                    add(createBadgeLabel(
                        text = type,
                        background = ChatTheme.PLAN_BADGE_BACKGROUND,
                        foreground = ChatTheme.PLAN_BADGE_FOREGROUND,
                        borderColor = ChatTheme.PLAN_BADGE_BORDER,
                    ))
                    add(createBadgeLabel(
                        text = if (required) "required" else "optional",
                        background = if (required) ChatTheme.ASK_BADGE_APPROVAL_BACKGROUND else ChatTheme.PLAN_BADGE_BACKGROUND,
                        foreground = if (required) ChatTheme.ASK_BADGE_APPROVAL_FOREGROUND else ChatTheme.PLAN_BADGE_FOREGROUND,
                        borderColor = if (required) ChatTheme.ASK_BADGE_APPROVAL_BORDER else ChatTheme.PLAN_BADGE_BORDER,
                    ))
                    if (default != null) {
                        add(createBadgeLabel(
                            text = "default: ${default.jsonPrimitive.contentOrNull ?: default.toString()}",
                            background = ChatTheme.PLAN_BADGE_BACKGROUND,
                            foreground = ChatTheme.MUTED_FOREGROUND,
                            borderColor = ChatTheme.PLAN_BADGE_BORDER,
                        ))
                    }
                })
            }
            add(leftPanel, BorderLayout.CENTER)
            // Right: description
            if (!desc.isNullOrBlank() || !enumValues.isNullOrEmpty()) {
                val rightPanel = JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)
                    isOpaque = false
                    alignmentY = Component.TOP_ALIGNMENT
                    if (!desc.isNullOrBlank()) {
                        add(JBLabel("<html><body style='width:300px'>$desc</body></html>").apply {
                            foreground = MUTED_FOREGROUND
                            font = JBFont.small()
                        })
                    }
                    if (!enumValues.isNullOrEmpty()) {
                        add(JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(4), 0)).apply {
                            isOpaque = false
                            border = JBUI.Borders.emptyTop(2)
                            enumValues.forEach { value ->
                                add(createBadgeLabel(
                                    text = value,
                                    background = ChatTheme.PLAN_BADGE_BACKGROUND,
                                    foreground = ChatTheme.PLAN_BADGE_FOREGROUND,
                                    borderColor = ChatTheme.PLAN_BADGE_BORDER,
                                ))
                            }
                        })
                    }
                }
                add(rightPanel, BorderLayout.EAST)
            }
        }
    }

    // ── Ask-user-question viewer ─────────────────────────────────────

    private fun createAskUserQuestionViewer(segment: ToolSegment): JComponent {
        val options = parseQuickOptions(segment)
        val components = mutableListOf<Component>()
        if (options.isNotEmpty()) {
            components += JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0)).apply {
                isOpaque = false
                options.forEach { option ->
                    add(createOptionChipButton(option, "Use this suggested answer") {
                        onAskReply?.invoke(option)
                    })
                }
            }
        }
        return createInteractionViewer(
            badge = createBadgeLabel(
                text = "Question",
                background = ChatTheme.ASK_BADGE_QUESTION_BACKGROUND,
                foreground = ChatTheme.ASK_BADGE_QUESTION_FOREGROUND,
                borderColor = ChatTheme.ASK_BADGE_QUESTION_BORDER,
            ),
            prompt = segment.toolCommand.ifBlank { segment.toolContent.ifBlank { "Please reply below." } },
            helper = "Reply in the panel below, or choose a suggested answer here.",
            extra = components,
        )
    }

    private fun createRequestUserInputViewer(segment: ToolSegment): JComponent {
        val questions = segment.params["questions"]
            ?.let { json.decodeFromString<List<RequestUserInputQuestion>>(it.toString()) }
            .orEmpty()
        val components = mutableListOf<Component>()
        questions.forEachIndexed { index, question ->
            if (index > 0) {
                components += Box.createVerticalStrut(JBUI.scale(8))
            }
            components += JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = true
                background = ChatTheme.PLAN_CARD_SURFACE_BACKGROUND
                alignmentX = Component.LEFT_ALIGNMENT
                border = BorderFactory.createCompoundBorder(
                    createRoundedBorder(ChatTheme.INTERACTION_INPUT_BORDER),
                    JBUI.Borders.empty(10, 12),
                )
                add(JBLabel("<html><b>${question.header}</b></html>").apply {
                    font = JBFont.label().asBold()
                    alignmentX = Component.LEFT_ALIGNMENT
                })
                add(JBLabel("<html><body>${question.question}</body></html>").apply {
                    font = JBFont.small()
                    foreground = MUTED_FOREGROUND
                    border = JBUI.Borders.emptyTop(4)
                    alignmentX = Component.LEFT_ALIGNMENT
                })
                if (question.options.isNotEmpty()) {
                    add(JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0)).apply {
                        isOpaque = false
                        alignmentX = Component.LEFT_ALIGNMENT
                        border = JBUI.Borders.emptyTop(8)
                        question.options.forEach { option ->
                            add(
                                createBadgeLabel(
                                    text = option.label,
                                    background = ChatTheme.PLAN_BADGE_BACKGROUND,
                                    foreground = ChatTheme.PLAN_BADGE_FOREGROUND,
                                    borderColor = ChatTheme.PLAN_BADGE_BORDER,
                                ).apply { toolTipText = option.description }
                            )
                        }
                    })
                }
            }
        }
        return createInteractionViewer(
            badge = createBadgeLabel(
                text = "Need Input",
                background = ChatTheme.ASK_BADGE_REQUEST_BACKGROUND,
                foreground = ChatTheme.ASK_BADGE_REQUEST_FOREGROUND,
                borderColor = ChatTheme.ASK_BADGE_REQUEST_BORDER,
            ),
            prompt = segment.toolContent.ifBlank { "Waiting for user input..." },
            helper = "Answer each item in the interaction panel below. Recommended options are shown here for reference.",
            extra = components,
        )
    }

    private fun createInteractionViewer(
        badge: JComponent,
        prompt: String,
        helper: String,
        extra: List<Component> = emptyList(),
    ): JComponent {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = true
            background = ChatTheme.PLAN_CARD_SURFACE_BACKGROUND
            border = BorderFactory.createCompoundBorder(
                createRoundedBorder(ChatTheme.PLAN_CARD_BORDER_COLOR),
                JBUI.Borders.empty(10, 12),
            )
            alignmentX = Component.LEFT_ALIGNMENT
            add(badge)
            add(Box.createVerticalStrut(JBUI.scale(8)))
            add(renderer.createMarkdownBlock(prompt))
            add(Box.createVerticalStrut(JBUI.scale(8)))
            add(JBLabel("<html><body>$helper</body></html>").apply {
                font = JBFont.small()
                foreground = MUTED_FOREGROUND
                alignmentX = Component.LEFT_ALIGNMENT
            })
            extra.forEach { component ->
                add(Box.createVerticalStrut(JBUI.scale(8)))
                add(component)
            }
        }
    }

    // ── Utility helpers ──────────────────────────────────────────────

    private fun resolveCandidateFile(segment: ToolSegment, item: String): File? {
        val projectBasePath = project.basePath
        val directFile = File(item)
        if (directFile.exists() && directFile.isFile) return directFile
        val commandBase = segment.toolCommand.takeIf { it.isNotBlank() }?.let(::File)
        if (commandBase != null) {
            val candidate = if (commandBase.isDirectory) File(commandBase, item)
            else File(commandBase.parentFile ?: commandBase, item)
            if (candidate.exists() && candidate.isFile) return candidate
        }
        if (!projectBasePath.isNullOrBlank()) {
            val projectCandidate = File(projectBasePath, item)
            if (projectCandidate.exists() && projectCandidate.isFile) return projectCandidate
        }
        return null
    }

    private fun parseQuickOptions(segment: ToolSegment): List<String> {
        return segment.params["options"]?.toString()
            ?.removePrefix("[")?.removeSuffix("]")
            ?.split(",")
            ?.map { it.trim().trim('"') }
            ?.filter { it.isNotBlank() }
            .orEmpty()
    }

    private fun prettyToolContent(content: String): String {
        return runCatching {
            val element = Json.parseToJsonElement(content)
            PRETTY_JSON.encodeToString(JsonElement.serializer(), element)
        }.getOrDefault(content)
    }

    private fun prettyJsonElement(element: JsonElement): String {
        return PRETTY_JSON.encodeToString(JsonElement.serializer(), element)
    }
}
