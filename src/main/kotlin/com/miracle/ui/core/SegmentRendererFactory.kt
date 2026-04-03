package com.miracle.ui.core

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.miracle.agent.parser.Code
import com.miracle.agent.parser.ErrorSegment
import com.miracle.agent.parser.PlanBlockParser
import com.miracle.agent.parser.ProposedPlanSegment
import com.miracle.agent.parser.SearchReplace
import com.miracle.agent.parser.Segment
import com.miracle.agent.parser.TextSegment
import com.miracle.agent.parser.ToolSegment
import com.miracle.agent.parser.UiToolName
import com.miracle.agent.parser.getToolSegmentHeader
import com.miracle.ui.core.ChatTheme.PANEL_BACKGROUND
import com.miracle.ui.core.ChatTheme.PLAN_BADGE_BACKGROUND
import com.miracle.ui.core.ChatTheme.PLAN_BADGE_BORDER
import com.miracle.ui.core.ChatTheme.PLAN_BADGE_FOREGROUND
import com.miracle.ui.core.ChatTheme.PLAN_CARD_BACKGROUND
import com.miracle.ui.core.ChatTheme.PLAN_CARD_BORDER_COLOR
import com.miracle.ui.core.ChatTheme.PLAN_CARD_HEADER_BACKGROUND
import com.miracle.ui.core.ChatTheme.PLAN_CARD_SURFACE_BACKGROUND
import com.miracle.ui.core.ChatTheme.ROUNDED_BORDER_COLOR
import com.miracle.ui.core.ChatTheme.TOOL_CONTENT_BACKGROUND
import com.miracle.ui.core.ChatTheme.TOOL_TITLE_FOREGROUND
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rtextarea.RTextScrollPane
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.JTextPane

/**
 * Factory that converts [Segment] model objects into Swing [JComponent]s.
 *
 * Core rendering logic (markdown, syntax, collapsible containers) lives here.
 * Tool-specific viewers are delegated to [ToolViewerFactory].
 *
 * @param project    for resolving file paths and opening editors
 * @param scrollManager for viewport-width wrapping and nested scroll bridging
 * @param onAskReply optional callback for ask-user-question quick-option buttons
 */
internal class SegmentRendererFactory(
    private val project: Project,
    private val scrollManager: ChatScrollManager,
    private val onAskReply: ((String) -> Unit)? = null,
    private val onProposedPlanAction: ((ProposedPlanAction, ProposedPlanSegment) -> Unit)? = null,
) {

    private val toolViewers = ToolViewerFactory(project, scrollManager, this, onAskReply)

    // ── Top-level dispatchers ────────────────────────────────────────

    fun renderSegment(segment: Segment): JComponent {
        val content = when (segment) {
            is TextSegment -> createMarkdownBlock(segment.text)
            is ErrorSegment -> createMarkdownBlock(segment.text, error = true)
            is Code -> createCodeBlock(segment)
            is SearchReplace -> createSearchReplaceBlock(segment)
            is ProposedPlanSegment -> createProposedPlanBlock(segment)
            is ToolSegment -> createToolBlock(segment)
            else -> createMarkdownBlock(segment.content)
        }
        return scrollManager.wrapForConversationWidth(content)
    }

    fun normalizeSegmentsForDisplay(segments: List<Segment>): List<Segment> {
        if (segments.isEmpty()) return segments
        return stripPlanTags(mergeAdjacentTextSegments(segments))
    }

    fun renderSegmentsAsText(segments: List<Segment>): String {
        return segments.joinToString("\n\n") { segment ->
            when (segment) {
                is TextSegment -> segment.text
                is ErrorSegment -> segment.text
                is Code -> segment.code
                is SearchReplace -> "SEARCH\n${segment.search}\n\nREPLACE\n${segment.replace}"
                is ProposedPlanSegment -> segment.markdown
                is ToolSegment -> listOf(segment.toolCommand, segment.toolContent)
                    .filter { it.isNotBlank() }.joinToString("\n\n")
                else -> segment.content
            }
        }
    }

    // ── Message shell ────────────────────────────────────────────────

    fun createMessageShell(author: String, icon: javax.swing.Icon, alignRight: Boolean): MessageShell {
        val header = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            if (alignRight) add(Box.createHorizontalGlue())
            add(JBLabel(author, icon, SwingConstants.LEFT).apply {
                font = JBFont.label().asBold()
                foreground = if (alignRight) ChatTheme.USER_FOREGROUND else JBColor.foreground()
            })
            add(Box.createHorizontalStrut(JBUI.scale(8)))
        }
        val body = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            border = JBUI.Borders.emptyTop(6)
        }
        val content = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(header, BorderLayout.NORTH)
            add(body, BorderLayout.CENTER)
        }
        val wrapper = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.emptyBottom(14)
            alignmentX = Component.LEFT_ALIGNMENT
            add(content, if (alignRight) BorderLayout.EAST else BorderLayout.CENTER)
        }
        return MessageShell(wrapper, body, header)
    }

    // ── Text / Markdown ──────────────────────────────────────────────

    fun createTextBlock(text: String, background: Color, foreground: Color, font: Font): JComponent {
        return JBTextArea(text).apply {
            lineWrap = true
            wrapStyleWord = true
            isEditable = false
            border = JBUI.Borders.empty(12)
            isOpaque = true
            this.background = background
            this.foreground = foreground
            this.font = font
            maximumSize = Dimension(JBUI.scale(920), Int.MAX_VALUE)
            alignmentX = Component.LEFT_ALIGNMENT
        }
    }

    fun createHtmlPane(html: String, opaque: Boolean = false): JTextPane {
        return JarvisMarkdownRenderUtil.createHtmlPane(project, html, opaque)
    }

    fun createMarkdownBlock(markdown: String, error: Boolean = false): JComponent {
        val html = if (error) {
            JarvisMarkdownRenderUtil.convertMarkdownToErrorHtml(markdown)
        } else {
            JarvisMarkdownRenderUtil.convertMarkdownToHtml(markdown)
        }
        val pane = createHtmlPane(html, false).apply {
            border = JBUI.Borders.empty(2, 0)
            alignmentX = Component.LEFT_ALIGNMENT
        }
        return if (html.contains("<table")) {
            JBScrollPane(pane).apply {
                border = JBUI.Borders.empty()
                isOpaque = false
                viewport.isOpaque = false
                horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_NEVER
                alignmentX = Component.LEFT_ALIGNMENT
                maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height + JBUI.scale(16))
                scrollManager.installNestedScrollBridge(this, alwaysDelegateVerticalWheel = true)
            }
        } else {
            pane
        }
    }

    // ── Syntax viewer ────────────────────────────────────────────────

    fun createSyntaxViewer(content: String, filePath: String, preferredHeight: Int = 200): JComponent {
        val textArea = RSyntaxTextArea(content).apply {
            isEditable = false
            syntaxEditingStyle = toolViewers.detectSyntax(filePath)
            antiAliasingEnabled = true
            font = Font(Font.MONOSPACED, Font.PLAIN, JBUI.scale(12))
            background = TOOL_CONTENT_BACKGROUND
            currentLineHighlightColor = JBColor(Color(232, 242, 254), Color(50, 50, 50))
            foreground = JBColor(Color(0, 0, 0), Color(220, 220, 220))
            selectionColor = JBColor(Color(173, 214, 255), Color(90, 110, 130))
            caretColor = JBColor(Color.BLACK, Color.WHITE)
            border = null
            lineWrap = false
        }
        return JPanel(BorderLayout()).apply {
            isOpaque = true
            background = TOOL_CONTENT_BACKGROUND
            border = JBUI.Borders.empty(4, 8)
            add(RTextScrollPane(textArea).apply {
                border = null
                viewport.background = TOOL_CONTENT_BACKGROUND
                lineNumbersEnabled = true
                gutter.background = TOOL_CONTENT_BACKGROUND
                gutter.lineNumberColor = JBColor(Color(128, 128, 128), Color(128, 128, 128))
                gutter.borderColor = JBColor(Color(230, 230, 230), Color(60, 60, 60))
                preferredSize = Dimension(JBUI.scale(640), JBUI.scale(preferredHeight))
                minimumSize = Dimension(0, JBUI.scale(100))
                scrollManager.installNestedScrollBridge(this)
            }, BorderLayout.CENTER)
        }
    }

    // ── Collapsible container ────────────────────────────────────────

    fun createInnerToolContainer(title: String, body: JComponent, initiallyExpanded: Boolean = false): JComponent {
        val bodyWrapper = JPanel(BorderLayout()).apply {
            isOpaque = true
            background = TOOL_CONTENT_BACKGROUND
            add(body, BorderLayout.CENTER)
            isVisible = initiallyExpanded
        }
        val chevronLabel = JLabel(
            if (initiallyExpanded) AllIcons.General.ArrowDown else AllIcons.General.ArrowRight
        ).apply {
            border = JBUI.Borders.emptyRight(4)
        }
        val safeTitle = escapeHtml(title.ifBlank { "tool-result" })
        val titleLabel = JBLabel(safeTitle).apply {
            font = Font(Font.MONOSPACED, Font.PLAIN, JBUI.scale(11))
            foreground = TOOL_TITLE_FOREGROUND
        }
        val titleBar = JPanel(BorderLayout()).apply {
            isOpaque = false
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            add(chevronLabel, BorderLayout.WEST)
            add(titleLabel, BorderLayout.CENTER)
            addMouseListener(object : java.awt.event.MouseAdapter() {
                override fun mouseClicked(e: java.awt.event.MouseEvent) {
                    bodyWrapper.isVisible = !bodyWrapper.isVisible
                    chevronLabel.icon = if (bodyWrapper.isVisible)
                        AllIcons.General.ArrowDown else AllIcons.General.ArrowRight
                    val root = SwingUtilities.getAncestorOfClass(JPanel::class.java, e.component)
                    root?.revalidate()
                    root?.repaint()
                }
            })
        }
        return JPanel(BorderLayout()).apply {
            isOpaque = true
            background = PANEL_BACKGROUND
            border = BorderFactory.createCompoundBorder(
                createRoundedBorder(ROUNDED_BORDER_COLOR),
                JBUI.Borders.empty(8, 10, 8, 10),
            )
            alignmentX = Component.LEFT_ALIGNMENT
            add(titleBar, BorderLayout.NORTH)
            add(bodyWrapper, BorderLayout.CENTER)
        }
    }

    // ── Code block ───────────────────────────────────────────────────

    private fun createCodeBlock(segment: Code): JComponent {
        val titleText = buildString {
            append(segment.codeLanguage.ifBlank { "code" })
            segment.codeFilePath?.takeIf { it.isNotBlank() }?.let {
                append(" \u00B7 ")
                append(displayPath(it))
            }
        }
        return createInnerToolContainer(
            title = titleText,
            body = createSyntaxViewer(segment.code, segment.codeFilePath ?: segment.codeLanguage, preferredHeight = 220),
        )
    }

    private fun createProposedPlanBlock(segment: ProposedPlanSegment): JComponent {
        val title = JBLabel("Proposed Plan", AllIcons.Actions.MenuOpen, SwingConstants.LEFT).apply {
            font = JBFont.label().asBold()
            foreground = TOOL_TITLE_FOREGROUND
        }
        val badge = createBadgeLabel(
            text = "Read-only Plan",
            background = PLAN_BADGE_BACKGROUND,
            foreground = PLAN_BADGE_FOREGROUND,
            borderColor = PLAN_BADGE_BORDER,
        )
        val titleRow = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
            add(title)
            add(Box.createHorizontalStrut(JBUI.scale(8)))
            add(badge)
            add(Box.createHorizontalGlue())
        }
        val subtitle = JBLabel(
            "<html><body>Plan mode response. You can execute this plan in Agent mode or continue refining.</body></html>",
        ).apply {
            font = JBFont.small()
            foreground = ChatTheme.MUTED_FOREGROUND
            border = JBUI.Borders.emptyTop(6)
            alignmentX = Component.LEFT_ALIGNMENT
        }
        val header = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = true
            background = PLAN_CARD_HEADER_BACKGROUND
            alignmentX = Component.LEFT_ALIGNMENT
            border = JBUI.Borders.empty(12, 12, 10, 12)
            add(titleRow)
            add(subtitle)
            maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
        }
        val body = createMarkdownBlock(segment.markdown)
        val bodyPanel = JPanel(BorderLayout()).apply {
            isOpaque = true
            background = PLAN_CARD_SURFACE_BACKGROUND
            alignmentX = Component.LEFT_ALIGNMENT
            border = BorderFactory.createCompoundBorder(
                createRoundedBorder(PLAN_CARD_BORDER_COLOR),
                JBUI.Borders.empty(10, 12),
            )
            add(body, BorderLayout.CENTER)
            maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
        }
        val actions = onProposedPlanAction?.let { actionHandler ->
            JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0)).apply {
                isOpaque = false
                alignmentX = Component.LEFT_ALIGNMENT
                border = JBUI.Borders.emptyTop(12)
                add(
                    createActionButton(
                        "\u7EE7\u7EED\u63D0\u95EE",
                        "\u4FDD\u6301\u5728 Plan \u6A21\u5F0F\u4E0B\u7EE7\u7EED\u8BA8\u8BBA\u8FD9\u4E2A\u8BA1\u5212",
                        primary = false,
                    ) {
                        actionHandler(ProposedPlanAction.ASK_FOLLOW_UP, segment)
                    },
                )
                add(
                    createActionButton(
                        "\u5207\u6362\u5230 Agent \u6267\u884C",
                        "\u5207\u6362\u5230 Agent \u6A21\u5F0F\u5E76\u7EE7\u7EED\u6267\u884C\u8FD9\u4E2A\u8BA1\u5212",
                        primary = true,
                    ) {
                        actionHandler(ProposedPlanAction.EXECUTE_IN_AGENT, segment)
                    },
                )
                maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
            }
        }
        val actionHint = onProposedPlanAction?.let {
            JBLabel(
                "<html><body>\u7EE7\u7EED\u63D0\u95EE\u4F1A\u7559\u5728 Plan \u6A21\u5F0F\uFF0C\u4E00\u952E\u6267\u884C\u4F1A\u5207\u5230 Agent \u5E76\u7ACB\u5373\u5F00\u59CB\u3002</body></html>",
            ).apply {
                font = JBFont.small()
                foreground = ChatTheme.MUTED_FOREGROUND
                border = JBUI.Borders.emptyTop(8)
                alignmentX = Component.LEFT_ALIGNMENT
            }
        }
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = true
            background = PLAN_CARD_BACKGROUND
            border = BorderFactory.createCompoundBorder(
                createRoundedBorder(PLAN_CARD_BORDER_COLOR),
                JBUI.Borders.empty(0),
            )
            alignmentX = Component.LEFT_ALIGNMENT
            add(header)
            add(Box.createVerticalStrut(JBUI.scale(10)))
            add(bodyPanel)
            actions?.let { add(it) }
            actionHint?.let { add(it) }
        }
    }

    // ── Tool block ───────────────────────────────────────────────────

    private fun createToolBlock(segment: ToolSegment): JComponent {
        val header = getToolSegmentHeader(segment)
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
            add(toolViewers.createToolTipsHeader(header), BorderLayout.NORTH)
            add(
                createInnerToolContainer(
                    title = toolViewers.resolveToolTitle(segment),
                    body = toolViewers.createToolBody(segment),
                    initiallyExpanded = segment.name == UiToolName.TODO_UPDATE,
                ),
                BorderLayout.CENTER,
            )
        }
    }

    // ── Search/Replace block ─────────────────────────────────────────

    private fun createSearchReplaceBlock(segment: SearchReplace): JComponent {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
            add(toolViewers.createSmallSectionLabel("SEARCH", AllIcons.Actions.Find))
            add(
                createInnerToolContainer(
                    title = buildString {
                        append("search")
                        segment.codeFilePath?.takeIf { it.isNotBlank() }?.let {
                            append(" \u00B7 "); append(displayPath(it))
                        }
                    },
                    body = createSyntaxViewer(segment.search, segment.codeFilePath ?: segment.codeLanguage, preferredHeight = 160),
                ),
            )
            add(Box.createVerticalStrut(JBUI.scale(8)))
            add(toolViewers.createSmallSectionLabel("REPLACE", AllIcons.Actions.MenuOpen))
            add(
                createInnerToolContainer(
                    title = buildString {
                        append("replace")
                        segment.codeFilePath?.takeIf { it.isNotBlank() }?.let {
                            append(" \u00B7 "); append(displayPath(it))
                        }
                    },
                    body = createSyntaxViewer(segment.replace, segment.codeFilePath ?: segment.codeLanguage, preferredHeight = 160),
                ),
            )
        }
    }

    // ── Path utility ─────────────────────────────────────────────────

    /**
     * 移除 TextSegment 中的 <proposed_plan> / </proposed_plan> 标签文本。
     */
    private fun stripPlanTags(segments: List<Segment>): List<Segment> {
        return segments.map { segment ->
            if (segment is TextSegment) {
                val stripped = PlanBlockParser.stripTags(segment.text)
                if (stripped != segment.text) TextSegment(stripped, segment.eventId) else segment
            } else {
                segment
            }
        }
    }

    private fun mergeAdjacentTextSegments(segments: List<Segment>): List<Segment> {
        val merged = mutableListOf<Segment>()
        var pendingText: TextSegment? = null

        fun flushPending() {
            pendingText?.let { merged.add(it) }
            pendingText = null
        }

        segments.forEach { segment ->
            when (segment) {
                is TextSegment -> {
                    pendingText = if (pendingText == null) {
                        segment
                    } else {
                        TextSegment(
                            text = pendingText!!.text + segment.text,
                            eventId = pendingText!!.eventId ?: segment.eventId,
                        )
                    }
                }
                else -> {
                    flushPending()
                    merged.add(segment)
                }
            }
        }
        flushPending()
        return merged
    }

    fun displayPath(path: String): String {
        val basePath = project.basePath ?: return path
        val normalizedBase = basePath.removeSuffix("/")
        return if (path.startsWith(normalizedBase)) {
            path.removePrefix(normalizedBase).removePrefix("/").ifBlank { path }
        } else {
            path
        }
    }
}

internal enum class ProposedPlanAction {
    ASK_FOLLOW_UP,
    EXECUTE_IN_AGENT,
}
