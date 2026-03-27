package com.miracle.services

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PromptServiceTest {

    @Test
    fun planModeInstructionsShouldUseCodexStylePlanProtocol() {
        val prompt = PromptService.buildPlanModeInstructions()

        assertTrue(prompt.contains("<proposed_plan>"))
        assertTrue(prompt.contains("read-only"))
        assertTrue(prompt.contains("RequestUserInput"))
        assertFalse(prompt.contains("TodoWrite"))
        assertFalse(prompt.contains("ExitPlanMode"))
        assertFalse(prompt.contains("Write tool"))
    }

    @Test
    fun executionModeInstructionsShouldNotContainRuntimePlanSwitching() {
        val prompt = PromptService.buildExecutionModeInstructions()

        assertTrue(prompt.contains("TodoWrite"))
        assertTrue(prompt.contains("AskUserQuestion"))
        assertFalse(prompt.contains("EnterPlanMode"))
        assertFalse(prompt.contains("ExitPlanMode"))
    }
}
