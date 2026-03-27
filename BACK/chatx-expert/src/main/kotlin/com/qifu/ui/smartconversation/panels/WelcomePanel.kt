package com.qifu.ui.smartconversation.panels

import com.intellij.ui.JBColor
import com.qihoo.finance.lowcode.common.util.Icons
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Font
import javax.swing.*

/**
 * @author weiyichao
 * @date 2025-10-14
 **/
class WelcomePanel(
    private val onQuickAsk: (String) -> Unit
) : JPanel(BorderLayout()) {

    private val rootPanel: JPanel

    init {
        isOpaque = false
        rootPanel = createRootPanel()
    }

    fun getContent(): JPanel = rootPanel

    private fun createRootPanel(): JPanel {
        val centerPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
        }

        val logo = JLabel().apply {
            horizontalAlignment = SwingConstants.CENTER
            horizontalTextPosition = JLabel.LEFT
            icon = Icons.scaleToWidth(Icons.LOGO_ROUND, 120F)
            alignmentX = CENTER_ALIGNMENT
        }

        val title = JLabel("与 Jarvis Act AI 协作").apply {
            font = font.deriveFont(Font.BOLD, 18f)
            foreground = JBColor.foreground()
            alignmentX = CENTER_ALIGNMENT
            border = BorderFactory.createEmptyBorder(12, 0, 24, 0)
        }

        centerPanel.add(Box.createVerticalGlue())
        centerPanel.add(logo)
        centerPanel.add(Box.createRigidArea(Dimension(0, 16)))
        centerPanel.add(title)

        val actions = arrayOf(
            "帮我分析下项目架构",
            "帮我优化下代码性能",
            "项目的核心代码是哪一块？"
        )

        actions.forEach { action ->
            val btn = JButton(action).apply {
                alignmentX = CENTER_ALIGNMENT
                maximumSize = Dimension(280, 40)
                val originalBorder = border
                val paddingBorder = BorderFactory.createEmptyBorder(5, 0, 5, 0)
                border = BorderFactory.createCompoundBorder(originalBorder, paddingBorder)
                isOpaque = false
                isContentAreaFilled = false
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                addActionListener {
                    onQuickAsk(action)
                }
            }
            centerPanel.add(btn)
            centerPanel.add(Box.createRigidArea(Dimension(0, 8)))
        }

        centerPanel.add(Box.createVerticalGlue())
        add(centerPanel, BorderLayout.CENTER)
        return centerPanel
    }
}