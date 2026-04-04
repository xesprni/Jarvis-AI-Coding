package com.miracle.ui.settings.components

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

/**
 * 卡片列表面板的共享 UI 组件工厂。
 *
 * 提供被 SkillsPanel、McpStatusPanel、AgentManagerPanel 共用的通用方法，
 * 消除跨面板的重复代码。
 */
object CardListComponents {

    // ── Description text ──────────────────────────────────────────────

    /**
     * 创建卡片描述文本组件。
     *
     * 超过 260 字符时截断并添加省略号，支持禁用态灰色显示。
     */
    fun createCardDescription(text: String, disabled: Boolean = false): JComponent {
        val normalized = text.trim()
        val shortened = if (normalized.length > 260) normalized.take(260) + "..." else normalized
        return JBTextArea(shortened).apply {
            isEditable = false
            isOpaque = false
            lineWrap = true
            wrapStyleWord = true
            font = JBFont.label()
            foreground = if (disabled) JBColor(Color(140, 145, 155), Color(120, 125, 135))
            else JBColor(Color(50, 60, 80), Color(200, 205, 215))
            border = null
            minimumSize = Dimension(0, preferredSize.height)
            maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
        }
    }

    // ── Empty state ───────────────────────────────────────────────────

    /**
     * 创建空状态提示。
     */
    fun createEmptyState(message: String): JComponent {
        val label = JBLabel(message).apply {
            foreground = JBColor(Color(90, 100, 120), Color(150, 160, 175))
        }
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.empty(16)
            add(label, BorderLayout.WEST)
        }
    }

    // ── Section header ────────────────────────────────────────────────

    private val MUTED_FOREGROUND = JBColor(Color(0x6B, 0x75, 0x86), Color(0xA0, 0xA8, 0xB8))

    /**
     * 创建面板顶部的标题区域（标题 + 副标题 + 操作按钮）。
     */
    fun createCardSectionHeader(
        title: String,
        subtitle: String,
        buttonText: String,
        buttonIcon: Icon,
        onButtonClick: () -> Unit,
    ): JComponent {
        val titleLabel = JBLabel(title, SwingConstants.LEFT).apply {
            font = JBFont.label().asBold().biggerOn(2f)
        }
        val subtitleLabel = JBLabel(subtitle).apply {
            foreground = MUTED_FOREGROUND
            border = JBUI.Borders.emptyTop(4)
            alignmentX = Component.LEFT_ALIGNMENT
            maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
        }

        val addButton = JButton(buttonText, buttonIcon).apply {
            putClientProperty("JButton.backgroundColor", JBColor.PanelBackground)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addActionListener { onButtonClick() }
            maximumSize = preferredSize
        }

        val topRow = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
            add(titleLabel)
            add(Box.createHorizontalGlue())
            add(addButton)
        }

        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            border = JBUI.Borders.emptyBottom(8)
            add(topRow)
            add(subtitleLabel)
        }
    }

    // ── Scroll pane ───────────────────────────────────────────────────

    /**
     * 创建卡片列表的滚动容器。
     */
    fun createCardListScrollPane(listPanel: JPanel): JComponent {
        return JBScrollPane(listPanel).apply {
            border = JBUI.Borders.empty()
            viewportBorder = null
            isOpaque = false
            viewport.isOpaque = false
            horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            verticalScrollBar.unitIncrement = JBUI.scale(16)
        }
    }

    // ── Icon action button ────────────────────────────────────────────

    /**
     * 创建小型图标操作按钮（编辑、删除、定位等）。
     */
    fun createIconActionButton(
        icon: Icon,
        tooltip: String,
        action: () -> Unit,
    ): JButton {
        return JButton(icon).apply {
            toolTipText = tooltip
            isOpaque = false
            isContentAreaFilled = false
            border = JBUI.Borders.empty()
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addActionListener { action() }
            preferredSize = Dimension(JBUI.scale(28), JBUI.scale(28))
            maximumSize = preferredSize
        }
    }

    // ── Card layout helpers ───────────────────────────────────────────

    /**
     * 创建卡片的图标行区域：左侧图标 + 中间标题/元信息 + 右侧操作区。
     */
    fun createCardHeaderRow(
        icon: Icon,
        title: JComponent,
        meta: JComponent,
        eastPanel: JComponent,
    ): JPanel {
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            add(javax.swing.JLabel(icon).apply {
                preferredSize = Dimension(JBUI.scale(40), JBUI.scale(40))
                horizontalAlignment = SwingConstants.CENTER
                verticalAlignment = SwingConstants.CENTER
            }, BorderLayout.WEST)

            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                add(title)
                add(Box.createVerticalStrut(JBUI.scale(2)))
                add(meta)
            }, BorderLayout.CENTER)

            add(eastPanel, BorderLayout.EAST)
        }
    }

    /**
     * 创建卡片描述区域。
     */
    fun createCardBody(description: JComponent): JPanel {
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.emptyTop(8)
            add(description, BorderLayout.CENTER)
        }
    }

    /**
     * 创建整张卡片的外壳。
     */
    fun createCardShell(
        header: JComponent,
        body: JComponent,
        cardHeight: Int,
        disabled: Boolean = false,
    ): JPanel {
        return JPanel(BorderLayout()).apply {
            isOpaque = true
            background = if (disabled) {
                JBColor(Color(0xF7, 0xF8, 0xFA), Color(0x2B, 0x2D, 0x32))
            } else {
                JBColor.PanelBackground
            }
            border = JBUI.Borders.empty(12)
            minimumSize = Dimension(0, JBUI.scale(cardHeight))
            preferredSize = Dimension(Int.MAX_VALUE, JBUI.scale(cardHeight))
            maximumSize = Dimension(Int.MAX_VALUE, JBUI.scale(cardHeight))
            add(header, BorderLayout.NORTH)
            add(body, BorderLayout.CENTER)
        }
    }

    // ── List rendering ────────────────────────────────────────────────

    /**
     * 渲染卡片列表到指定容器中，卡片之间使用分隔线。
     *
     * 如果列表为空，显示空状态提示。
     */
    fun renderCardList(
        listPanel: JPanel,
        cards: List<JComponent>,
        emptyMessage: String,
    ) {
        listPanel.removeAll()
        if (cards.isEmpty()) {
            listPanel.add(createEmptyState(emptyMessage))
        } else {
            cards.forEachIndexed { index, card ->
                if (index != 0) {
                    listPanel.add(javax.swing.JSeparator(SwingConstants.HORIZONTAL).apply {
                        maximumSize = Dimension(Int.MAX_VALUE, 10)
                    })
                }
                listPanel.add(card.apply { alignmentX = Component.LEFT_ALIGNMENT })
            }
            listPanel.add(Box.createVerticalStrut(JBUI.scale(12)))
        }
        listPanel.revalidate()
        listPanel.repaint()
    }
}
