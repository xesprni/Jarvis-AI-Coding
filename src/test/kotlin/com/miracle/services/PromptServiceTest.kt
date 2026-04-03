package com.miracle.services

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PromptServiceTest {

    @Test
    fun planModeInstructionsShouldUsePlanProtocol() {
        val prompt = PromptService.buildPlanModeInstructions()

        assertTrue(prompt.contains("<proposed_plan>"))
        assertTrue(prompt.contains("read-only"))
        assertTrue(prompt.contains("RequestUserInput"))
        assertTrue(prompt.contains("after the closing </proposed_plan> tag"))
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

    @Test
    fun commonPromptShouldRequireMarkdownFileLinksForCodeReferences() {
        val prompt = PromptService.buildCommonSystemPrompt()

        assertTrue(prompt.contains("Markdown file links"))
        assertTrue(prompt.contains("jarvis-file://"))
        assertFalse(prompt.contains("`file_path:line_number`"))
    }
}
