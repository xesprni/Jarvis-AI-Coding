package com.miracle.agent.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProposedPlanParserTest {

    @Test
    fun completeMessageParserShouldNotExtractProposedPlanBlock() {
        val parser = CompleteMessageParser()
        val input = """
            Intro text
            <proposed_plan>
            # Title
            - item
            </proposed_plan>
            Outro text
        """.trimIndent()

        val segments = parser.parse(input)

        // Parser no longer expands <proposed_plan> tags; they stay as text
        assertFalse(segments.any { it is ProposedPlanSegment })
        assertTrue(segments.filterIsInstance<TextSegment>().any { it.text.contains("Intro text") })
        assertTrue(segments.filterIsInstance<TextSegment>().any { it.text.contains("Outro text") })
    }

    @Test
    fun completeMessageParserShouldIgnorePlanTagInsideCodeFence() {
        val parser = CompleteMessageParser()
        val input = """
            ```md
            <proposed_plan>
            # not a plan
            </proposed_plan>
            ```
        """.trimIndent()

        val segments = parser.parse(input)

        assertFalse(segments.any { it is ProposedPlanSegment })
        assertTrue(segments.filterIsInstance<Code>().single().code.contains("<proposed_plan>"))
    }

    @Test
    fun sseParserShouldNotExtractProposedPlanFromTags() {
        val parser = SseMessageParser()
        val chunks = listOf(
            "Before\n<proposed_",
            "plan>\n# My Plan\n",
            "- first\n</proposed_plan>\nAfter"
        )

        var latestSegments: List<Segment> = emptyList()
        chunks.forEach { chunk ->
            latestSegments = parser.parse(chunk)
        }

        // Parser no longer expands <proposed_plan> tags
        assertFalse(latestSegments.any { it is ProposedPlanSegment })
        assertTrue(latestSegments.filterIsInstance<TextSegment>().any { it.text.contains("After") })
    }

    @Test
    fun planBlockParserShouldKeepIncompleteTagAsText() {
        val input = "prefix <proposed_plan>\n# draft"
        val segments = PlanBlockParser.splitText("prefix <proposed_plan>\n# draft")

        assertEquals(input, segments.joinToString("") { it.content })
        assertTrue(segments.all { it is TextSegment })
    }
}
