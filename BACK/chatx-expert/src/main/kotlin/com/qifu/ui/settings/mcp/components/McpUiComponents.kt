package com.qifu.ui.settings.mcp.components

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.ActionListener
import java.awt.geom.RoundRectangle2D
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JToggleButton
import kotlin.math.abs

/**
 * MCP 面板公共 UI 组件工厂
 */
object McpUiComponents {

    /**
     * 创建 MCP 标签组件（用于显示 space/tag 等信息）
     */
    fun createMcpLabel(tag: String): JLabel {
        return JLabel(tag).apply {
            font = JBFont.small()
            foreground = JBColor(Color(0x29, 0x78, 0xF5), Color(0x8B, 0xA2, 0xF6))
            border = JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor(Color(0xCC, 0xD6, 0xF6), Color(0x3A, 0x40, 0x5F)), 1),
                JBUI.Borders.empty(2, 8)
            )
        }
    }

    /**
     * 创建操作按钮（用于卸载等操作）
     */
    fun createActionButton(
        buttonName: String,
        icon: Icon,
        actionListener: ActionListener
    ): JToggleButton {
        val buttonSize = Dimension(JBUI.scale(24), JBUI.scale(24))
        return JToggleButton(icon).apply {
            toolTipText = buttonName
            isFocusable = false
            isOpaque = false
            isBorderPainted = false
            isContentAreaFilled = false
            preferredSize = buttonSize
            minimumSize = buttonSize
            maximumSize = buttonSize
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addActionListener(actionListener)
        }
    }

    /**
     * 创建服务启用切换按钮
     */
    fun createServerToggle(enabled: Boolean, onToggle: (Boolean) -> Unit): JToggleButton {
        val enabledBg = JBColor(Color(0x2ECC71), Color(0x1B8A4C))
        val disabledBg = JBColor(Color(0xC4C9D6), Color(0x4C4F58))
        val knobColor = JBColor.WHITE

        fun tooltipText(selected: Boolean) = if (selected) "点击禁用" else "点击启用"

        val button = object : JToggleButton() {
            override fun getPreferredSize(): Dimension {
                return Dimension(JBUI.scale(36), JBUI.scale(20))
            }

            override fun paintComponent(g: Graphics) {
                val g2 = g.create() as? Graphics2D ?: return
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    val w = width.toFloat()
                    val h = height.toFloat()
                    val arc = h
                    g2.color = if (isSelected) enabledBg else disabledBg
                    g2.fill(RoundRectangle2D.Float(0f, 0f, w, h, arc, arc))

                    val padding = JBUI.scale(2).toFloat()
                    val knobSize = h - padding * 2
                    val knobX = if (isSelected) w - knobSize - padding else padding
                    g2.color = knobColor
                    g2.fillOval(knobX.toInt(), padding.toInt(), knobSize.toInt(), knobSize.toInt())
                } finally {
                    g2.dispose()
                }
            }
        }

        button.isSelected = enabled
        val fixedSize = button.preferredSize
        button.minimumSize = fixedSize
        button.maximumSize = fixedSize
        button.isFocusable = false
        button.isOpaque = false
        button.isBorderPainted = false
        button.isContentAreaFilled = false
        button.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        button.toolTipText = tooltipText(enabled)
        button.accessibleContext.accessibleName = "MCP 服务开关"
        button.addActionListener {
            val selected = button.isSelected
            button.toolTipText = tooltipText(selected)
            button.repaint()
            onToggle(selected)
        }
        return button
    }

    /**
     * 字母图标类 - 当服务图标无法加载时显示的备用图标
     */
    class LetterIcon(name: String?, private val size: Int) : Icon {

        private val displayChar: String =
            name?.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "M"

        private val colorPair = colorForName(name)
        private val textFont = JBFont.label().asBold().biggerOn(2f)

        override fun paintIcon(c: Component?, g: Graphics?, x: Int, y: Int) {
            val g2 = g?.create() as? Graphics2D ?: return
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            // 绘制圆角矩形背景
            val arc = JBUI.scale(8)
            g2.color = colorPair
            g2.fillRoundRect(x, y, size, size, arc, arc)

            // 计算文本居中位置并绘制
            g2.font = textFont
            val metrics = g2.fontMetrics
            val textWidth = metrics.stringWidth(displayChar)
            val textHeight = metrics.ascent
            val textX = x + (size - textWidth) / 2
            val textY = y + (size + textHeight) / 2 - metrics.descent
            g2.color = JBColor.WHITE
            g2.drawString(displayChar, textX, textY)
            g2.dispose()
        }

        override fun getIconWidth(): Int = size
        override fun getIconHeight(): Int = size

        companion object {
            private val palette = listOf(
                JBColor(Color(0xE4, 0xEE, 0xFF), Color(0x2E, 0x38, 0x55)),
                JBColor(Color(0xE6, 0xF6, 0xF0), Color(0x1F, 0x3A, 0x2E)),
                JBColor(Color(0xF4, 0xEE, 0xFF), Color(0x32, 0x24, 0x40)),
                JBColor(Color(0xFF, 0xF1, 0xE6), Color(0x39, 0x2A, 0x1F)),
                JBColor(Color(0xED, 0xF7, 0xFF), Color(0x21, 0x33, 0x47))
            )

            private fun colorForName(name: String?): JBColor {
                val seed = abs((name ?: "").hashCode())
                return palette[seed % palette.size]
            }
        }
    }
}
