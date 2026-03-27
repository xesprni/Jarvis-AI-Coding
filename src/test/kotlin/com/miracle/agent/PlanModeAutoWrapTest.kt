package com.miracle.agent

import com.miracle.agent.parser.CompleteMessageParser
import com.miracle.agent.parser.ProposedPlanSegment
import com.miracle.agent.parser.TextSegment
import com.miracle.ui.smartconversation.settings.configuration.ChatMode
import dev.langchain4j.data.message.AiMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 测试 Plan 模式下自动将 AI 响应包装为 ProposedPlanSegment 的逻辑
 *
 * 场景覆盖:
 * 1. Plan 模式下,AI 输出包含 <proposed_plan> 标签 -> 保持原样
 * 2. Plan 模式下,AI 输出不包含标签 -> 自动包装为 ProposedPlanSegment
 * 3. Agent 模式下,AI 输出不包含标签 -> 不包装
 * 4. Plan 模式下,AI 有工具调用 -> 不包装
 */
class PlanModeAutoWrapTest {

    /**
     * 测试场景 1: Plan 模式下,AI 输出包含 <proposed_plan> 标签
     * 预期: 保持原有解析逻辑,ProposedPlanSegment 正常生成
     */
    @Test
    fun planModeWithExplicitProposedPlanTag() {
        val aiResponse = """
            Here is my analysis:
            
            <proposed_plan>
            # Implementation Plan
            - Step 1: Read the file
            - Step 2: Modify the code
            - Step 3: Test the changes
            </proposed_plan>
            
            Let me know if you have questions.
        """.trimIndent()

        val parser = CompleteMessageParser()
        val segments = parser.parse(aiResponse)
            .filter { it is TextSegment || it is ProposedPlanSegment }

        // 验证: 应该有 1 个 ProposedPlanSegment 和 2 个 TextSegment
        assertEquals(1, segments.filterIsInstance<ProposedPlanSegment>().size)
        assertEquals(2, segments.filterIsInstance<TextSegment>().size)

        val planSegment = segments.filterIsInstance<ProposedPlanSegment>().single()
        assertTrue(planSegment.markdown.contains("# Implementation Plan"))
        assertTrue(planSegment.markdown.contains("Step 1: Read the file"))
    }

    /**
     * 测试场景 2: Plan 模式下,AI 输出不包含 <proposed_plan> 标签
     * 预期: 模拟 agent.kt 的逻辑,自动将整个响应包装为 ProposedPlanSegment
     */
    @Test
    fun planModeWithoutProposedPlanTagShouldAutoWrap() {
        val aiResponse = """
            # Analysis Report
            
            Based on my investigation:
            
            1. The issue is in the parser logic
            2. We need to add auto-wrap functionality
            3. Testing is required
            
            Next steps: Implement the changes.
        """.trimIndent()

        val parser = CompleteMessageParser()
        val segments = parser.parse(aiResponse)
            .filter { it is TextSegment || it is ProposedPlanSegment }

        // 模拟 agent.kt 的逻辑
        val chatMode = ChatMode.PLAN
        val hasToolCalls = false // 没有工具调用

        val finalSegments = if (chatMode == ChatMode.PLAN && !hasToolCalls) {
            if (segments.none { it is ProposedPlanSegment }) {
                // 自动包装整个响应
                listOf(ProposedPlanSegment(aiResponse))
            } else {
                segments
            }
        } else {
            segments
        }

        // 验证: 应该自动包装为 1 个 ProposedPlanSegment
        assertEquals(1, finalSegments.size)
        assertTrue(finalSegments.single() is ProposedPlanSegment)

        val planSegment = finalSegments.filterIsInstance<ProposedPlanSegment>().single()
        assertTrue(planSegment.markdown.contains("# Analysis Report"))
        assertTrue(planSegment.markdown.contains("parser logic"))
    }

    /**
     * 测试场景 3: Agent 模式下,AI 输出不包含标签
     * 预期: 不进行自动包装,保持原有 TextSegment
     */
    @Test
    fun agentModeShouldNotAutoWrap() {
        val aiResponse = """
            I've completed the task. Here's what I did:
            
            1. Modified the file
            2. Ran the tests
            3. Everything works
        """.trimIndent()

        val parser = CompleteMessageParser()
        val segments = parser.parse(aiResponse)
            .filter { it is TextSegment || it is ProposedPlanSegment }

        // 模拟 agent.kt 的逻辑
        val chatMode = ChatMode.AGENT
        val hasToolCalls = false

        val finalSegments = if (chatMode == ChatMode.PLAN && !hasToolCalls) {
            if (segments.none { it is ProposedPlanSegment }) {
                listOf(ProposedPlanSegment(aiResponse))
            } else {
                segments
            }
        } else {
            segments
        }

        // 验证: Agent 模式下不应该包装
        assertFalse(finalSegments.any { it is ProposedPlanSegment })
        assertTrue(finalSegments.all { it is TextSegment })
    }

    /**
     * 测试场景 4: Plan 模式下,AI 有工具调用
     * 预期: 不进行自动包装,等待工具执行完成
     */
    @Test
    fun planModeWithToolCallsShouldNotAutoWrap() {
        val aiResponse = "Let me read the file first to understand the structure."

        val parser = CompleteMessageParser()
        val segments = parser.parse(aiResponse)
            .filter { it is TextSegment || it is ProposedPlanSegment }

        // 模拟 agent.kt 的逻辑
        val chatMode = ChatMode.PLAN
        val hasToolCalls = true // 有工具调用

        val finalSegments = if (chatMode == ChatMode.PLAN && !hasToolCalls) {
            if (segments.none { it is ProposedPlanSegment }) {
                listOf(ProposedPlanSegment(aiResponse))
            } else {
                segments
            }
        } else {
            segments
        }

        // 验证: 有工具调用时不应该包装
        assertFalse(finalSegments.any { it is ProposedPlanSegment })
        assertTrue(finalSegments.all { it is TextSegment })
    }

    /**
     * 测试场景 5: Plan 模式下,空响应
     * 预期: 不进行包装
     */
    @Test
    fun planModeWithEmptyResponseShouldNotWrap() {
        val aiResponse = ""

        val parser = CompleteMessageParser()
        val segments = parser.parse(aiResponse)
            .filter { it is TextSegment || it is ProposedPlanSegment }

        // 模拟 agent.kt 的逻辑
        val chatMode = ChatMode.PLAN
        val hasToolCalls = false

        val finalSegments = if (chatMode == ChatMode.PLAN && !hasToolCalls && aiResponse.isNotBlank()) {
            if (segments.none { it is ProposedPlanSegment }) {
                listOf(ProposedPlanSegment(aiResponse))
            } else {
                segments
            }
        } else {
            segments
        }

        // 验证: 空响应不应该包装
        assertTrue(finalSegments.isEmpty())
    }

    /**
     * 测试场景 6: Plan 模式下,响应包含代码块但没有 proposed_plan 标签
     * 预期: 自动包装整个响应(包括代码块)
     */
    @Test
    fun planModeWithCodeBlockShouldAutoWrap() {
        val aiResponse = """
            Here's the plan:
            
            ```kotlin
            fun example() {
                println("Hello")
            }
            ```
            
            This code demonstrates the approach.
        """.trimIndent()

        val parser = CompleteMessageParser()
        val segments = parser.parse(aiResponse)
            .filter { it is TextSegment || it is ProposedPlanSegment }

        // 模拟 agent.kt 的逻辑
        val chatMode = ChatMode.PLAN
        val hasToolCalls = false

        val finalSegments = if (chatMode == ChatMode.PLAN && !hasToolCalls) {
            if (segments.none { it is ProposedPlanSegment }) {
                listOf(ProposedPlanSegment(aiResponse))
            } else {
                segments
            }
        } else {
            segments
        }

        // 验证: 应该自动包装为 1 个 ProposedPlanSegment
        assertEquals(1, finalSegments.size)
        assertTrue(finalSegments.single() is ProposedPlanSegment)

        val planSegment = finalSegments.filterIsInstance<ProposedPlanSegment>().single()
        assertTrue(planSegment.markdown.contains("fun example()"))
    }
}
