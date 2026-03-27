package com.miracle.ui.core

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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
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
import javax.swing.JButton
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

    // ── Tool body dispatcher ─────────────────────────────────────────

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
        }
    }

    // ── Tool tips header ─────────────────────────────────────────────

    fun createToolTipsHeader(header: com.miracle.agent.parser.ToolHeader): JComponent {
        val htmlText = """
            <html>
              <body>
                <p style='margin-top: 4px; margin-bottom: 4px; font-weight: bold; margin-left: 4px;'>${header.text}</p>
              </body>
            </html>
        """.trimIndent()
        val textPane = JarvisMarkdownRenderUtil.createHtmlPane(htmlText, false).apply {
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

    fun createSmallSectionLabel(text: String, icon: javax.swing.Icon): JComponent {
        return JBLabel(text, icon, SwingConstants.LEFT).apply {
            font = JBFont.small(); foreground = MUTED_FOREGROUND
            border = JBUI.Borders.emptyBottom(4)
        }
    }

    // ── Tool title resolution ────────────────────────────────────────

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
        val content = prettyToolContent(segment.toolContent)
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = true
            background = TOOL_CONTENT_BACKGROUND
            border = JBUI.Borders.empty(8, 12, 8, 12)
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
            add(renderer.createSyntaxViewer(content, "mcp.json", preferredHeight = 180))
        }
    }

    // ── Ask-user-question viewer ─────────────────────────────────────

    private fun createAskUserQuestionViewer(segment: ToolSegment): JComponent {
        val options = parseQuickOptions(segment)
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = true
            background = TOOL_CONTENT_BACKGROUND
            border = JBUI.Borders.empty(8, 12, 8, 12)
            add(renderer.createMarkdownBlock(segment.toolCommand.ifBlank { segment.toolContent.ifBlank { "Please reply below." } }))
            if (options.isNotEmpty()) {
                add(Box.createVerticalStrut(JBUI.scale(8)))
                add(JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0)).apply {
                    isOpaque = false
                    options.forEach { option ->
                        add(JButton(option).apply {
                            addActionListener { onAskReply?.invoke(option) }
                        })
                    }
                })
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
}
