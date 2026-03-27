package com.qifu.ui.smartconversation.panels

import com.qihoo.finance.lowcode.smartconversation.panels.SmartToolWindowScrollablePanel
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SmartToolWindowScrollablePanelTest {

    @Test
    fun testRemoveMessagesFromRemovesTailMessages() {
        val panel = SmartToolWindowScrollablePanel()
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        val id3 = UUID.randomUUID()

        panel.addMessage(id1)
        panel.addMessage(id2)
        panel.addMessage(id3)
        assertEquals(3, panel.componentCount)
        assertFalse(panel.isEmptyMessages())

        panel.removeMessagesFrom(id2)
        assertEquals(1, panel.componentCount)
        assertFalse(panel.isEmptyMessages())

        panel.removeMessagesFrom(id1)
        assertEquals(0, panel.componentCount)
        assertTrue(panel.isEmptyMessages())
    }
}
