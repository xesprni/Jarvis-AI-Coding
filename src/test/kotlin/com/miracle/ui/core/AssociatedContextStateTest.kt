package com.miracle.ui.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AssociatedContextStateTest {

    @Test
    fun testAddDeduplicatesFilesAndKeepsClassifiedViews() {
        val state = AssociatedContextState()
        val file = AssociatedContextItem.AssociatedFile("/tmp/project/src/Foo.kt")
        val code = AssociatedContextItem.AssociatedCodeSelection(
            filePath = "/tmp/project/src/Foo.kt",
            startLine = 10,
            endLine = 12,
            fullLineText = "fun foo() {}",
        )

        val first = state.add(file)
        val second = state.add(file)
        state.add(code)

        assertIs<AssociatedContextAddResult.Added>(first)
        assertIs<AssociatedContextAddResult.Existing>(second)
        assertEquals(listOf("/tmp/project/src/Foo.kt"), state.referencedFilePaths())
        assertEquals(listOf(code), state.codeSelections())
        assertEquals(2, state.items().size)
    }

    @Test
    fun testRemoveAndClearUpdateState() {
        val state = AssociatedContextState()
        val file = AssociatedContextItem.AssociatedFile("/tmp/project/src/Foo.kt")
        val code = AssociatedContextItem.AssociatedCodeSelection(
            filePath = "/tmp/project/src/Foo.kt",
            startLine = 1,
            endLine = 2,
            fullLineText = "val x = 1",
        )
        state.add(file)
        state.add(code)

        state.remove(file)
        assertEquals(listOf(code), state.items())

        state.clear()
        assertTrue(state.isEmpty())
    }
}
