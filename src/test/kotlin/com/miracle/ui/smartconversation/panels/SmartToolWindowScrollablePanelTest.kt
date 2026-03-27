package com.miracle.ui.smartconversation.panels

import com.miracle.ui.core.MessageColumnPanel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SmartToolWindowScrollablePanelTest {

    @Test
    fun testMessageColumnPanelTracksViewportWidthButNotHeight() {
        val panel = MessageColumnPanel()

        assertTrue(panel.getScrollableTracksViewportWidth())
        assertFalse(panel.getScrollableTracksViewportHeight())
        assertEquals(28, panel.getScrollableUnitIncrement(null, 0, 1))
    }
}
