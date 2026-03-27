package com.qifu.ui.settings.mcp.components

import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Color
import javax.swing.JButton

/**
 * 美化的返回按钮组件
 *
 * 带有圆角背景和悬停效果的返回按钮
 */
class StyledBackButton(onClick: () -> Unit) : JButton(AllIcons.Actions.Back) {

    init {
        isOpaque = false
        isContentAreaFilled = false
        isBorderPainted = false
        isFocusable = false
        isRolloverEnabled = true
        border = JBUI.Borders.empty()
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        toolTipText = "返回"

        val size = JBUI.scale(28)
        preferredSize = Dimension(size, size)
        minimumSize = preferredSize
        maximumSize = preferredSize
        iconTextGap = 0

        addActionListener { onClick() }
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val arc = JBUI.scale(12)

        val backgroundColor = when {
            model.isPressed -> PRESSED_BG
            model.isRollover -> HOVER_BG
            else -> DEFAULT_BG
        }

        g2.color = backgroundColor
        g2.fillRoundRect(0, 0, width - 1, height - 1, arc, arc)

        if (model.isRollover) {
            g2.color = BORDER_COLOR
            g2.drawRoundRect(0, 0, width - 1, height - 1, arc, arc)
        }

        g2.dispose()
        foreground = FOREGROUND_COLOR
        super.paintComponent(g)
    }

    companion object {
        private val DEFAULT_BG = JBColor(Color(0xF3, 0xF4, 0xF6), Color(0x2C, 0x2F, 0x34))
        private val HOVER_BG = JBColor(Color(0xE6, 0xE8, 0xEE), Color(0x37, 0x3D, 0x45))
        private val PRESSED_BG = JBColor(Color(0xD8, 0xDA, 0xDF), Color(0x30, 0x35, 0x3D))
        private val BORDER_COLOR = JBColor(Color(0xCF, 0xD3, 0xDA), Color(0x4B, 0x54, 0x5E))
        private val FOREGROUND_COLOR = JBColor(Color(0x16, 0x24, 0x3A), Color(0xE4, 0xE6, 0xEA))
    }
}
