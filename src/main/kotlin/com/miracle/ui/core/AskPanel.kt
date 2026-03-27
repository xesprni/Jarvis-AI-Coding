package com.miracle.ui.core

import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.miracle.agent.JarvisAsk
import com.miracle.agent.parser.Code
import com.miracle.agent.parser.ErrorSegment
import com.miracle.agent.parser.Segment
import com.miracle.agent.parser.TextSegment
import com.miracle.agent.parser.ToolSegment
import com.miracle.agent.parser.UiToolName
import com.miracle.agent.parser.getToolSegmentHeader
import com.miracle.ui.core.ChatTheme.CHAT_BACKGROUND
import com.miracle.ui.core.ChatTheme.SPLIT_LINE_COLOR
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTextField

internal enum class AskDecision { APPROVE, REJECT }

/**
 * Panel shown when the agent requests user input / approval.
 *
 * @param onReply called when the user submits a reply.
 *   Receives the current [AskDecision] and the text from the input field.
 */
internal class AskPanel(
    private val onReply: (AskDecision, String) -> Unit,
) : JPanel(BorderLayout()) {

    val inputField = JTextField()
    var selectedDecision = AskDecision.APPROVE
        private set

    private val questionLabel = JBLabel().apply {
        border = JBUI.Borders.emptyBottom(6)
    }
    private val approveButton = JButton("批准").apply {
        addActionListener {
            selectedDecision = AskDecision.APPROVE
            onReply(selectedDecision, inputField.text.trim())
        }
    }
    private val rejectButton = JButton("拒绝").apply {
        addActionListener {
            selectedDecision = AskDecision.REJECT
            onReply(selectedDecision, inputField.text.trim())
        }
    }
    private val replyButton = JButton("回复").apply {
        addActionListener {
            selectedDecision = AskDecision.APPROVE
            onReply(selectedDecision, inputField.text.trim())
        }
    }
    private val optionsPanel = JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0)).apply {
        isOpaque = false
    }

    init {
        isVisible = false
        background = CHAT_BACKGROUND
        border = BorderFactory.createCompoundBorder(
            JBUI.Borders.customLine(SPLIT_LINE_COLOR, 1),
            JBUI.Borders.empty(10),
        )
        add(questionLabel, BorderLayout.NORTH)
        add(optionsPanel, BorderLayout.CENTER)
        add(JPanel(BorderLayout(JBUI.scale(8), 0)).apply {
            isOpaque = false
            add(inputField, BorderLayout.CENTER)
            add(JPanel(FlowLayout(FlowLayout.RIGHT, JBUI.scale(6), 0)).apply {
                isOpaque = false
                add(approveButton)
                add(rejectButton)
                add(replyButton)
            }, BorderLayout.EAST)
        }, BorderLayout.SOUTH)
    }

    fun bind(ask: JarvisAsk) {
        isVisible = true
        selectedDecision = AskDecision.APPROVE
        questionLabel.text = "<html>${ask.data.joinToString("<br>") { it.content.ifBlank { renderSegmentLabel(it) } }}</html>"
        inputField.text = ""
        optionsPanel.removeAll()

        val isQuestionPrompt = isAskUserQuestion(ask)
        approveButton.isVisible = !isQuestionPrompt
        rejectButton.isVisible = !isQuestionPrompt
        replyButton.text = if (isQuestionPrompt) "提交回复" else "发送反馈"

        if (isQuestionPrompt) {
            val segment = ask.data.singleOrNull() as? ToolSegment
            val options = segment?.params?.get("options")?.toString()
                ?.removePrefix("[")
                ?.removeSuffix("]")
                ?.split(",")
                ?.map { it.trim().trim('"') }
                ?.filter { it.isNotBlank() }
                .orEmpty()
            options.forEach { option ->
                optionsPanel.add(JButton(option).apply {
                    addActionListener {
                        inputField.text = option
                        onReply(AskDecision.APPROVE, option)
                    }
                })
            }
        }
        revalidate()
        repaint()
    }

    fun clear() {
        isVisible = false
        questionLabel.text = ""
        inputField.text = ""
        optionsPanel.removeAll()
        revalidate()
        repaint()
    }

    fun spacingTop(): Int = if (isVisible) JBUI.scale(8) else 0

    private fun renderSegmentLabel(segment: Segment): String {
        return when (segment) {
            is ToolSegment -> getToolSegmentHeader(segment).text
            is Code -> segment.codeFilePath ?: segment.codeLanguage
            is TextSegment -> segment.text
            is ErrorSegment -> segment.text
            else -> segment.content
        }
    }
}

/**
 * Checks whether the given [JarvisAsk] is an ask-user-question prompt.
 */
internal fun isAskUserQuestion(ask: JarvisAsk): Boolean {
    val segment = ask.data.singleOrNull() as? ToolSegment ?: return false
    return segment.name == UiToolName.ASK_USER_QUESTION
}
