package com.miracle.ui.core

import com.miracle.agent.JarvisAsk
import com.miracle.agent.AgentMessageType
import com.miracle.agent.parser.ToolSegment
import com.miracle.agent.parser.UiToolName
import com.miracle.agent.tool.RequestUserInputOption
import com.miracle.agent.tool.RequestUserInputOutput
import com.miracle.agent.tool.RequestUserInputQuestion
import java.awt.Component
import java.awt.Container
import javax.swing.AbstractButton
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AskPanelTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun requestUserInputShouldRequireAnswersForEveryQuestion() {
        val panel = AskPanel { _, _ -> }
        val ask = buildRequestAsk()

        panel.bind(ask)

        assertNotNull(panel.validateReply(ask))
        panel.setStructuredAnswer("entry_mode", "用户显式切换")
        panel.setStructuredAnswer("compat_strategy", "彻底对齐 Codex")

        val payload = panel.buildReplyPayload(ask)
        val output = json.decodeFromString<RequestUserInputOutput>(payload)

        assertEquals(listOf("用户显式切换"), output.answers["entry_mode"])
        assertEquals(listOf("彻底对齐 Codex"), output.answers["compat_strategy"])
    }

    @Test
    fun requestUserInputPromptShouldBeRecognized() {
        val ask = buildRequestAsk()
        assertTrue(isRequestUserInput(ask))
    }

    @Test
    fun requestUserInputShouldShowStructuredSubmitInsteadOfApprovalButtons() {
        val panel = AskPanel { _, _ -> }
        panel.bind(buildRequestAsk())

        val buttonTexts = collectVisibleButtonTexts(panel)

        assertTrue(buttonTexts.contains("提交回答"))
        assertFalse(buttonTexts.contains("批准执行"))
        assertFalse(buttonTexts.contains("拒绝执行"))
    }

    @Test
    fun approvalPromptShouldShowApproveRejectAndFeedbackActions() {
        val panel = AskPanel { _, _ -> }
        panel.bind(
            JarvisAsk(
                id = "ask-approval",
                type = AgentMessageType.TOOL,
                data = listOf(
                    ToolSegment(
                        name = UiToolName.EDITED_EXISTING_FILE,
                        toolCommand = "/tmp/example.kt",
                        toolContent = "fun answer() = 42",
                    )
                )
            )
        )

        val buttonTexts = collectVisibleButtonTexts(panel)

        assertTrue(buttonTexts.contains("批准执行"))
        assertTrue(buttonTexts.contains("拒绝执行"))
        assertTrue(buttonTexts.contains("发送反馈"))
    }

    private fun buildRequestAsk(): JarvisAsk {
        val questions = listOf(
            RequestUserInputQuestion(
                header = "进入方式",
                id = "entry_mode",
                question = "Plan 模式应该如何进入？",
                options = listOf(
                    RequestUserInputOption("用户显式切换", "只在 UI 中切换"),
                    RequestUserInputOption("双入口并存", "保留两种入口")
                )
            ),
            RequestUserInputQuestion(
                header = "兼容策略",
                id = "compat_strategy",
                question = "兼容策略是什么？",
                options = listOf(
                    RequestUserInputOption("彻底对齐 Codex", "彻底重构"),
                    RequestUserInputOption("兼容迁移", "逐步迁移")
                )
            )
        )
        return JarvisAsk(
            id = "ask-1",
            type = AgentMessageType.TOOL,
            data = listOf(
                ToolSegment(
                    name = UiToolName.REQUEST_USER_INPUT,
                    toolCommand = "Plan 模式应该如何进入？",
                    toolContent = "Please answer the following questions:",
                    params = mutableMapOf(
                        "questions" to Json.parseToJsonElement(Json.encodeToString(questions))
                    )
                )
            )
        )
    }

    private fun collectVisibleButtonTexts(component: Component): List<String> {
        val texts = mutableListOf<String>()
        if (component is AbstractButton && component.isVisible && component.text.isNotBlank()) {
            texts += component.text
        }
        if (component is Container) {
            component.components.forEach { child ->
                texts += collectVisibleButtonTexts(child)
            }
        }
        return texts
    }
}
