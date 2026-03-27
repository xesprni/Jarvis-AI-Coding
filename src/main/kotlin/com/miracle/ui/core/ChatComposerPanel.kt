package com.miracle.ui.core

import com.intellij.util.ui.JBUI
import com.miracle.ui.core.ChatTheme.INPUT_BACKGROUND
import com.miracle.ui.core.ChatTheme.PRIMARY_BORDER_START
import com.miracle.ui.core.ChatTheme.PRIMARY_BORDER_END
import java.awt.BorderLayout
import java.awt.GradientPaint
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.BasicStroke
import javax.swing.JPanel

/**
 * A [JPanel] with a rounded gradient border used as the input composer area.
 */
internal class RoundedComposerPanel : JPanel(BorderLayout()) {
    private val radius = JBUI.scale(16)
    private val borderStroke = 2.0f

    init {
        isOpaque = false
        border = JBUI.Borders.empty(3)
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.color = INPUT_BACKGROUND
            g2.fillRoundRect(0, 0, width - 1, height - 1, radius, radius)
        } finally {
            g2.dispose()
        }
        super.paintComponent(g)
    }

    override fun paintBorder(g: Graphics) {
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.paint = GradientPaint(
                0f, 0f, PRIMARY_BORDER_START,
                width.toFloat(), height.toFloat(), PRIMARY_BORDER_END,
            )
            val inset = borderStroke / 2f
            g2.stroke = BasicStroke(borderStroke)
            g2.drawRoundRect(
                inset.toInt(), inset.toInt(),
                width - 1 - (inset * 2).toInt(), height - 1 - (inset * 2).toInt(),
                radius, radius,
            )
        } finally {
            g2.dispose()
        }
    }
}
