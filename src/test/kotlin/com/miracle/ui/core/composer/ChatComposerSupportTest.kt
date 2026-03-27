package com.miracle.ui.core.composer

import com.miracle.ui.core.AssociatedContextItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChatComposerSupportTest {

    @Test
    fun testExpandTextReplacesPlaceholderLabelsWithExpandedContent() {
        val text = "Read  Foo.kt  and  Bar.kt "
        val placeholders = listOf(
            PlaceholderSnapshot(
                startOffset = 5,
                endOffset = 13,
                label = " Foo.kt ",
                content = "`src/Foo.kt`",
            ),
            PlaceholderSnapshot(
                startOffset = 18,
                endOffset = 26,
                label = " Bar.kt ",
                content = "`src/Bar.kt`",
            ),
        )

        val expanded = ChatComposerSupport.expandText(text, placeholders)

        assertEquals("Read `src/Foo.kt` and `src/Bar.kt`", expanded)
    }

    @Test
    fun testFindSearchTextAfterAtStopsAtWhitespace() {
        assertEquals("src", ChatComposerSupport.findSearchTextAfterAt("open @src", 9))
        assertNull(ChatComposerSupport.findSearchTextAfterAt("open @src now", 13))
        assertNull(ChatComposerSupport.findSearchTextAfterAt("open src", 8))
    }

    @Test
    fun testReplaceSlashTokenReplacesCurrentTokenOnly() {
        val (updated, startOffset) = ChatComposerSupport.replaceSlashToken(
            text = "Please use /rev",
            caretOffset = "Please use /rev".length,
            replacement = "/review <target>",
        )

        assertEquals("Please use /review <target>", updated)
        assertEquals(11, startOffset)
    }

    @Test
    fun testFirstPlaceholderSelectionRangeFindsFirstArgument() {
        val range = ChatComposerSupport.firstPlaceholderSelectionRange(
            startOffset = 4,
            invocationText = "/review <target> <mode>",
        )

        assertEquals(TokenRange(13, 19), range)
    }

    @Test
    fun testAppendAssociatedCodeContextAddsFormattedBlocks() {
        val result = ChatComposerSupport.appendAssociatedCodeContext(
            text = "Explain this flow",
            selections = listOf(
                AssociatedContextItem.AssociatedCodeSelection(
                    filePath = "/tmp/demo/Foo.kt",
                    startLine = 12,
                    endLine = 13,
                    fullLineText = "fun foo() {\n  println(\"ok\")\n}",
                )
            ),
        )

        assertEquals(
            """
            Explain this flow

            引用的代码上下文：

            ```kt:/tmp/demo/Foo.kt
                12→fun foo() {
                13→  println("ok")
                14→}
            ```
            """.trimIndent(),
            result,
        )
    }

    @Test
    fun testFindMatchedSlashCommandRangesReturnsOnlyFullCommands() {
        val ranges = ChatComposerSupport.findMatchedSlashCommandRanges(
            text = "/clear now /compact /comp",
            knownCommands = setOf("/clear", "/compact"),
        )

        assertEquals(listOf(TokenRange(0, 6), TokenRange(11, 19)), ranges)
    }

    @Test
    fun testFindSlashArgumentPlaceholderRangesReadsCurrentLineOnly() {
        val ranges = ChatComposerSupport.findSlashArgumentPlaceholderRanges(
            text = "/review <target> <mode>\nnext line",
            slashRanges = listOf(TokenRange(0, 7)),
        )

        assertEquals(listOf(TokenRange(9, 15), TokenRange(18, 22)), ranges)
    }

    @Test
    fun testFindSlashSearchTextReturnsNullForEmptyText() {
        assertNull(ChatComposerSupport.findSlashSearchText("", 0))
        assertNull(ChatComposerSupport.findSlashSearchText("/", 0))
    }
}
