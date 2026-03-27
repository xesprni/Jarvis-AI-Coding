package com.miracle.ui.core

import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.miracle.agent.AgentMessageType
import com.miracle.agent.parser.Segment
import com.miracle.ui.core.ChatTheme.ASSISTANT_BUBBLE
import com.miracle.ui.core.ChatTheme.JARVIS_ICON
import com.miracle.ui.core.ChatTheme.MUTED_FOREGROUND
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Cursor
import java.awt.event.ItemEvent
import javax.swing.Box
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JToggleButton
import javax.swing.SwingConstants

/**
 * A single assistant-side message card that supports partial (streaming) updates.
 */
internal class AssistantMessageCard(
    private val renderer: SegmentRendererFactory,
) {
    private val shell = renderer.createMessageShell("Jarvis", JARVIS_ICON, alignRight = false)
    val root: JComponent = shell.root
    private var lastSegments: List<Segment> = emptyList()
    private var lastType: AgentMessageType = AgentMessageType.TEXT
    private var lastPartial: Boolean = false
    private var thoughtPanel: ThoughtProcessPanel? = null

    fun updateContent(segments: List<Segment>, type: AgentMessageType, partial: Boolean) {
        lastSegments = segments
        lastType = type
        lastPartial = partial
        shell.root.isOpaque = false
        shell.root.border = JBUI.Borders.emptyBottom(12)

        shell.header.removeAll()
        shell.header.add(JBLabel("Jarvis", JARVIS_ICON, SwingConstants.LEFT).apply {
            font = JBFont.label().asBold()
            iconTextGap = JBUI.scale(6)
        })
        shell.header.add(Box.createHorizontalGlue())
        shell.header.add(createHeaderIconButton(AllIcons.Actions.Copy, "复制消息") {
            copyToClipboard(renderer.renderSegmentsAsText(segments))
        })

        shell.body.removeAll()
        if (segments.isEmpty()) {
            shell.body.add(
                renderer.createTextBlock(
                    "等待模型响应...", ASSISTANT_BUBBLE,
                    JBColor.foreground(), JBFont.label(),
                )
            )
        } else if (type == AgentMessageType.REASONING) {
            val tp = thoughtPanel ?: ThoughtProcessPanel().also { thoughtPanel = it }
            val text = segments.joinToString("\n") { it.content }
            tp.updateText(text)
            if (!partial) tp.setFinished()
            shell.body.add(tp)
        } else {
            if (thoughtPanel != null) {
                thoughtPanel!!.setFinished()
                shell.body.add(thoughtPanel!!)
                shell.body.add(Box.createVerticalStrut(JBUI.scale(8)))
            }
            segments.forEachIndexed { index, segment ->
                if (index > 0) shell.body.add(Box.createVerticalStrut(JBUI.scale(8)))
                shell.body.add(renderer.renderSegment(segment))
            }
        }
        shell.root.revalidate()
        shell.root.repaint()
    }

    fun finishPartialIfNeeded() {
        if (lastPartial) {
            updateContent(lastSegments, lastType, false)
        }
    }
}

/**
 * Collapsible panel showing the AI's "thinking" process.
 */
internal class ThoughtProcessPanel : JPanel(BorderLayout()) {
    private var finished: Boolean = false
    private val contentPane = JarvisMarkdownRenderUtil.createHtmlPane("", false).apply {
        foreground = MUTED_FOREGROUND
        border = JBUI.Borders.empty(4, 8)
    }
    private val contentWrapper = JPanel(BorderLayout()).apply {
        isOpaque = false
        add(contentPane, BorderLayout.CENTER)
    }
    private val toggleButton = JToggleButton("Thinking...", AllIcons.General.ArrowDown).apply {
        isSelected = true
        isFocusPainted = false
        isContentAreaFilled = false
        isBorderPainted = false
        selectedIcon = AllIcons.General.ArrowUp
        horizontalAlignment = SwingConstants.LEFT
        horizontalTextPosition = SwingConstants.RIGHT
        iconTextGap = JBUI.scale(4)
        font = JBFont.small()
        foreground = MUTED_FOREGROUND
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        addItemListener { e ->
            contentWrapper.isVisible = e.stateChange == ItemEvent.SELECTED
            this@ThoughtProcessPanel.revalidate()
            this@ThoughtProcessPanel.repaint()
        }
    }

    init {
        isOpaque = false
        alignmentX = Component.LEFT_ALIGNMENT
        add(toggleButton, BorderLayout.NORTH)
        add(contentWrapper, BorderLayout.CENTER)
    }

    fun setFinished() {
        if (finished) return
        finished = true
        toggleButton.text = "Thought Process"
        toggleButton.isSelected = false
    }

    fun updateText(text: String) {
        contentPane.text = JarvisMarkdownRenderUtil.convertMarkdownToHtml(text)
    }
}
