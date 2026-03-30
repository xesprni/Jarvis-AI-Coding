package com.miracle.ui.core

import com.intellij.util.ui.JBFont
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.miracle.agent.JarvisAsk
import com.miracle.agent.parser.Code
import com.miracle.agent.parser.ErrorSegment
import com.miracle.agent.parser.Segment
import com.miracle.agent.parser.TextSegment
import com.miracle.agent.parser.ToolSegment
import com.miracle.agent.parser.UiToolName
import com.miracle.agent.parser.getToolSegmentHeader
import com.miracle.agent.tool.RequestUserInputOutput
import com.miracle.agent.tool.RequestUserInputQuestion
import com.miracle.utils.UiUtil
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
import java.awt.Dimension
import javax.swing.BoxLayout
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.JPanel
import javax.swing.JTextField
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** 用户对 Ask 请求的决策：批准或拒绝 */
internal enum class AskDecision { APPROVE, REJECT }
/** Ask 面板的交互模式：审批、问答或结构化输入 */
private enum class AskPanelMode { APPROVAL, QUESTION, REQUEST }

/**
 * Panel shown when the agent requests user input / approval.
 *
 * @param onReply called when the user submits a reply.
 *   Receives the current [AskDecision] and the text from the input field.
 */
internal class AskPanel(
    private val onReply: (AskDecision, String) -> Unit,
) : JPanel(BorderLayout()) {

    /** JSON 序列化实例 */
    private val json = Json { ignoreUnknownKeys = true }
    /** 用户输入文本框 */
    val inputField = JBTextField()
    /** 当前选择的决策 */
    var selectedDecision = AskDecision.APPROVE
        private set
    private var requestQuestions: List<RequestUserInputQuestion> = emptyList()
    private val requestAnswerFields = linkedMapOf<String, JBTextField>()

    private val badgeLabel = JBLabel()
    private val questionLabel = JBLabel().apply {
        font = JBFont.label().asBold()
        border = JBUI.Borders.emptyTop(8)
    }
    private val helperLabel = JBLabel().apply {
        font = JBFont.small()
        foreground = ChatTheme.MUTED_FOREGROUND
        border = JBUI.Borders.emptyTop(6)
    }
    private val approveButton = createApproveButton("批准执行", "允许 Agent 继续执行当前操作") {
        selectedDecision = AskDecision.APPROVE
        onReply(selectedDecision, inputField.text.trim())
    }
    private val rejectButton = createRejectButton("拒绝执行", "拒绝当前操作并中止这一步") {
        selectedDecision = AskDecision.REJECT
        onReply(selectedDecision, inputField.text.trim())
    }
    private val replyButton = createActionButton("提交回复", "提交你的回复并继续", primary = true) {
        selectedDecision = AskDecision.APPROVE
        onReply(selectedDecision, inputField.text.trim())
    }
    private val optionsPanel = JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0)).apply {
        isOpaque = false
    }
    private val requestPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = false
    }
    private val headerPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = true
        background = ChatTheme.PLAN_CARD_HEADER_BACKGROUND
        border = JBUI.Borders.empty(12, 12, 10, 12)
        add(badgeLabel)
        add(questionLabel)
        add(helperLabel)
    }
    private val centerPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = true
        background = ChatTheme.PLAN_CARD_BACKGROUND
        border = JBUI.Borders.empty(12, 12, 0, 12)
        add(optionsPanel)
        add(requestPanel)
    }
    private val actionPanel = JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0)).apply {
        isOpaque = false
        add(approveButton)
        add(rejectButton)
        add(replyButton)
    }
    private val inputWrapper = JPanel(BorderLayout()).apply {
        isOpaque = false
        border = JBUI.Borders.empty()
        add(inputField, BorderLayout.CENTER)
        maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
    }

    init {
        isVisible = false
        isOpaque = true
        background = ChatTheme.PLAN_CARD_BACKGROUND
        border = BorderFactory.createCompoundBorder(
            createRoundedBorder(ChatTheme.PLAN_CARD_BORDER_COLOR),
            JBUI.Borders.empty(0),
        )
        styleInputField(inputField)
        add(headerPanel, BorderLayout.NORTH)
        add(centerPanel, BorderLayout.CENTER)
        add(JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = true
            background = ChatTheme.PLAN_CARD_BACKGROUND
            border = JBUI.Borders.empty(10)
            add(inputWrapper)
            add(Box.createVerticalStrut(JBUI.scale(8)))
            add(actionPanel)
        }, BorderLayout.SOUTH)
    }

    /**
     * 绑定 Ask 请求到面板，根据 Ask 类型配置为审批、问答或结构化输入模式。
     *
     * @param ask 要绑定的 Ask 请求
     */
    fun bind(ask: JarvisAsk) {
        isVisible = true
        selectedDecision = AskDecision.APPROVE
        val mode = when {
            isRequestUserInput(ask) -> AskPanelMode.REQUEST
            isAskUserQuestion(ask) -> AskPanelMode.QUESTION
            else -> AskPanelMode.APPROVAL
        }
        configureMode(mode)
        questionLabel.text = wrapHtml(ask.data.joinToString("<br>") { it.content.ifBlank { renderSegmentLabel(it) } })
        UiUtil.clearTextSafely(inputField)
        optionsPanel.removeAll()
        requestPanel.removeAll()
        clearRequestAnswerFields()
        requestQuestions = emptyList()
        requestAnswerFields.clear()

        val isQuestionPrompt = mode == AskPanelMode.QUESTION
        val isRequestPrompt = mode == AskPanelMode.REQUEST
        approveButton.isVisible = mode == AskPanelMode.APPROVAL
        rejectButton.isVisible = mode == AskPanelMode.APPROVAL
        replyButton.isVisible = true
        replyButton.text = when (mode) {
            AskPanelMode.APPROVAL -> "发送反馈"
            AskPanelMode.QUESTION -> "提交回复"
            AskPanelMode.REQUEST -> "提交回答"
        }
        inputWrapper.isVisible = !isRequestPrompt
        inputField.isVisible = !isRequestPrompt
        optionsPanel.isVisible = isQuestionPrompt
        requestPanel.isVisible = isRequestPrompt

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
                optionsPanel.add(
                    createOptionChipButton(option, "使用这个建议回复") {
                        setReplyText(option)
                        onReply(AskDecision.APPROVE, option)
                    }
                )
            }
        } else if (isRequestPrompt) {
            val segment = ask.data.singleOrNull() as? ToolSegment
            requestQuestions = segment?.params?.get("questions")
                ?.let { json.decodeFromString<List<RequestUserInputQuestion>>(it.toString()) }
                .orEmpty()
            requestQuestions.forEachIndexed { index, question ->
                val field = JBTextField().apply {
                    toolTipText = "留空时可以直接点击下面的推荐选项，也可以输入自己的回答。"
                }
                styleInputField(field)
                requestAnswerFields[question.id] = field
                if (index > 0) {
                    requestPanel.add(Box.createVerticalStrut(JBUI.scale(10)))
                }
                requestPanel.add(createQuestionCard(question, field))
            }
        }
        revalidate()
        repaint()
    }

    /**
     * 清空面板状态并隐藏。
     */
    fun clear() {
        isVisible = false
        badgeLabel.text = ""
        questionLabel.text = ""
        helperLabel.text = ""
        UiUtil.clearTextSafely(inputField)
        clearRequestAnswerFields()
        optionsPanel.removeAll()
        requestPanel.removeAll()
        requestQuestions = emptyList()
        requestAnswerFields.clear()
        revalidate()
        repaint()
    }

    /**
     * 返回面板顶部间距，仅在面板可见时返回间距值。
     *
     * @return 间距像素值
     */
    fun spacingTop(): Int = if (isVisible) JBUI.scale(8) else 0

    /**
     * 验证当前回复是否满足 Ask 请求的要求。
     *
     * @param ask 要验证的 Ask 请求
     * @return 错误信息，验证通过时返回 null
     */
    fun validateReply(ask: JarvisAsk): String? {
        return when {
            isRequestUserInput(ask) -> {
                if (requestQuestions.any { requestAnswerFields[it.id]?.text?.trim().isNullOrBlank() }) {
                    "请完整回答所有问题。"
                } else {
                    null
                }
            }
            isAskUserQuestion(ask) && inputField.text.trim().isBlank() -> "请输入回复内容。"
            else -> null
        }
    }

    /**
     * 根据 Ask 请求类型构建回复载荷。
     *
     * @param ask 当前 Ask 请求
     * @return 序列化后的回复字符串
     */
    fun buildReplyPayload(ask: JarvisAsk): String {
        return if (isRequestUserInput(ask)) {
            val answers = requestQuestions.associate { question ->
                question.id to listOf(requestAnswerFields[question.id]?.text?.trim().orEmpty())
            }
            json.encodeToString(RequestUserInputOutput(answers))
        } else {
            inputField.text.trim()
        }
    }

    /**
     * 设置回复文本框的值。
     *
     * @param value 要设置的文本
     */
    internal fun setReplyText(value: String) {
        UiUtil.setTextSafely(inputField, value, moveCaretToEnd = true)
    }

    /**
     * 设置结构化回答中指定问题 ID 的值。
     *
     * @param questionId 问题 ID
     * @param value 要设置的回答文本
     */
    internal fun setStructuredAnswer(questionId: String, value: String) {
        requestAnswerFields[questionId]?.let { UiUtil.setTextSafely(it, value, moveCaretToEnd = true) }
    }

    private fun renderSegmentLabel(segment: Segment): String {
        return when (segment) {
            is ToolSegment -> getToolSegmentHeader(segment).text
            is Code -> segment.codeFilePath ?: segment.codeLanguage
            is TextSegment -> segment.text
            is ErrorSegment -> segment.text
            else -> segment.content
        }
    }

    private fun configureMode(mode: AskPanelMode) {
        val badge = when (mode) {
            AskPanelMode.APPROVAL -> createBadgeLabel(
                text = "Approval Needed",
                background = ChatTheme.ASK_BADGE_APPROVAL_BACKGROUND,
                foreground = ChatTheme.ASK_BADGE_APPROVAL_FOREGROUND,
                borderColor = ChatTheme.ASK_BADGE_APPROVAL_BORDER,
            )
            AskPanelMode.QUESTION -> createBadgeLabel(
                text = "Question",
                background = ChatTheme.ASK_BADGE_QUESTION_BACKGROUND,
                foreground = ChatTheme.ASK_BADGE_QUESTION_FOREGROUND,
                borderColor = ChatTheme.ASK_BADGE_QUESTION_BORDER,
            )
            AskPanelMode.REQUEST -> createBadgeLabel(
                text = "Need Input",
                background = ChatTheme.ASK_BADGE_REQUEST_BACKGROUND,
                foreground = ChatTheme.ASK_BADGE_REQUEST_FOREGROUND,
                borderColor = ChatTheme.ASK_BADGE_REQUEST_BORDER,
            )
        }
        badgeLabel.icon = badge.icon
        badgeLabel.text = badge.text
        badgeLabel.isOpaque = badge.isOpaque
        badgeLabel.background = badge.background
        badgeLabel.foreground = badge.foreground
        badgeLabel.font = badge.font
        badgeLabel.border = badge.border
        helperLabel.text = wrapHtml(
            when (mode) {
                AskPanelMode.APPROVAL -> "Approve to continue, reject to stop here, or send feedback if the model should adjust before acting."
                AskPanelMode.QUESTION -> "Reply directly below, or choose one of the suggested answers to move faster."
                AskPanelMode.REQUEST -> "Answer each item before continuing. You can click a recommended option or type your own response."
            }
        )
    }

    private fun createQuestionCard(question: RequestUserInputQuestion, field: JBTextField): JPanel {
        val title = JBLabel("<html><b>${question.header}</b></html>").apply {
            font = JBFont.label().asBold()
            alignmentX = Component.LEFT_ALIGNMENT
        }
        val prompt = JBLabel(wrapHtml(question.question)).apply {
            font = JBFont.small()
            foreground = ChatTheme.MUTED_FOREGROUND
            border = JBUI.Borders.emptyTop(4)
            alignmentX = Component.LEFT_ALIGNMENT
        }
        val options = JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0)).apply {
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
            border = JBUI.Borders.emptyTop(8)
            question.options.forEach { option ->
                add(createOptionChipButton(option.label, option.description) {
                    UiUtil.setTextSafely(field, option.label, moveCaretToEnd = true)
                })
            }
        }
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = true
            background = ChatTheme.PLAN_CARD_SURFACE_BACKGROUND
            alignmentX = Component.LEFT_ALIGNMENT
            border = BorderFactory.createCompoundBorder(
                createRoundedBorder(ChatTheme.INTERACTION_INPUT_BORDER),
                JBUI.Borders.empty(8, 10),
            )
            add(title)
            add(prompt)
            add(options)
            add(Box.createVerticalStrut(JBUI.scale(6)))
            add(field)
            maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
        }
    }

    private fun styleInputField(field: JTextField) {
        field.font = JBFont.small()
        field.background = ChatTheme.INTERACTION_INPUT_BACKGROUND
        field.border = BorderFactory.createCompoundBorder(
            createRoundedBorder(ChatTheme.INTERACTION_INPUT_BORDER),
            JBUI.Borders.empty(4, 7),
        )
        field.maximumSize = Dimension(Int.MAX_VALUE, field.preferredSize.height)
    }

    private fun clearRequestAnswerFields() {
        requestAnswerFields.values.forEach { UiUtil.clearTextSafely(it) }
    }

    private fun wrapHtml(text: String): String {
        return "<html><body>$text</body></html>"
    }
}

/**
 * Checks whether the given [JarvisAsk] is an ask-user-question prompt.
 */
/**
 * 判断给定的 Ask 是否为用户问答类型。
 */
internal fun isAskUserQuestion(ask: JarvisAsk): Boolean {
    val segment = ask.data.singleOrNull() as? ToolSegment ?: return false
    return segment.name == UiToolName.ASK_USER_QUESTION
}

/**
 * 判断给定的 Ask 是否为结构化用户输入请求类型。
 */
internal fun isRequestUserInput(ask: JarvisAsk): Boolean {
    val segment = ask.data.singleOrNull() as? ToolSegment ?: return false
    return segment.name == UiToolName.REQUEST_USER_INPUT
}
