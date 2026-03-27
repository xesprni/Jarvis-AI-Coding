package com.miracle.ui.core

import com.intellij.openapi.util.IconLoader
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.miracle.ui.core.ChatTheme.MUTED_FOREGROUND
import com.miracle.ui.core.ChatTheme.PANEL_BACKGROUND
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

/**
 * Builds the welcome panel shown when no conversation is active.
 *
 * The panel displays a logo, a title/subtitle, and a list of quick-start
 * prompt buttons that trigger [onQuickAsk].
 */
internal object WelcomePanel {

    fun create(onQuickAsk: (String) -> Unit): JComponent {
        val centerContent = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
        }

        val scaledSize = JBUI.scale(80)
        val logo = JBLabel(IconLoader.getIcon("/img/inner/logo_round32.svg", JarvisChatTabPanel::class.java).let { icon ->
            object : Icon {
                override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
                    val g2 = g.create() as Graphics2D
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    g2.translate(x, y)
                    val sx = scaledSize.toDouble() / icon.iconWidth
                    val sy = scaledSize.toDouble() / icon.iconHeight
                    g2.scale(sx, sy)
                    icon.paintIcon(c, g2, 0, 0)
                    g2.dispose()
                }
                override fun getIconWidth(): Int = scaledSize
                override fun getIconHeight(): Int = scaledSize
            }
        }).apply {
            alignmentX = Component.CENTER_ALIGNMENT
            horizontalAlignment = SwingConstants.CENTER
        }
        val title = JBLabel("\u4E0E Jarvis AI \u534F\u4F5C").apply {
            font = font.deriveFont(Font.BOLD, JBUI.scale(18).toFloat())
            alignmentX = Component.CENTER_ALIGNMENT
        }
        val subtitle = JBLabel("\u76F4\u63A5\u5F00\u59CB\u672C\u5730\u804A\u5929\uFF0C\u6216\u4ECE\u4E0B\u9762\u7684\u5FEB\u6377\u95EE\u9898\u8FDB\u5165\u3002").apply {
            font = JBFont.small()
            foreground = MUTED_FOREGROUND
            alignmentX = Component.CENTER_ALIGNMENT
        }

        centerContent.add(Box.createVerticalGlue())
        centerContent.add(logo)
        centerContent.add(Box.createVerticalStrut(JBUI.scale(16)))
        centerContent.add(title)
        centerContent.add(Box.createVerticalStrut(JBUI.scale(6)))
        centerContent.add(subtitle)
        centerContent.add(Box.createVerticalStrut(JBUI.scale(22)))

        listOf(
            "\u5E2E\u6211\u5206\u6790\u4E0B\u9879\u76EE\u67B6\u6784",
            "\u5E2E\u6211\u4F18\u5316\u4E0B\u4EE3\u7801\u6027\u80FD",
            "\u9879\u76EE\u7684\u6838\u5FC3\u4EE3\u7801\u662F\u54EA\u4E00\u5757\uFF1F",
        ).forEach { prompt ->
            centerContent.add(JButton(prompt).apply {
                alignmentX = Component.CENTER_ALIGNMENT
                maximumSize = Dimension(JBUI.scale(320), JBUI.scale(38))
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                isContentAreaFilled = false
                addActionListener { onQuickAsk(prompt) }
            })
            centerContent.add(Box.createVerticalStrut(JBUI.scale(8)))
        }

        centerContent.add(Box.createVerticalGlue())

        return JPanel(BorderLayout()).apply {
            isOpaque = true
            background = PANEL_BACKGROUND
            add(centerContent, BorderLayout.CENTER)
        }
    }
}
