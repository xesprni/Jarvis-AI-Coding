package com.miracle.ui.core

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import com.intellij.util.ui.JBUI
import com.miracle.ui.core.ChatTheme.DROPDOWN_BACKGROUND
import com.miracle.ui.core.ChatTheme.DROPDOWN_ROW_HOVER_BACKGROUND
import com.miracle.ui.core.ChatTheme.SELECTOR_HOVER_BACKGROUND
import com.miracle.ui.core.ChatTheme.SELECTOR_PRESSED_BACKGROUND
import com.miracle.ui.core.ChatTheme.SELECTOR_TEXT_FOREGROUND
import com.miracle.ui.smartconversation.settings.configuration.ChatMode
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.Icon
import javax.swing.plaf.basic.BasicComboBoxUI
import com.intellij.util.ui.JBFont
import java.awt.BorderLayout

// ── Data class for model combo box items ─────────────────────────────
internal data class ModelItem(val id: String, val label: String) {
    override fun toString(): String = label
}

// ── Custom renderer for model items ──────────────────────────────────
internal class ModelItemRenderer : javax.swing.DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
        list: JList<*>?,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean,
    ): Component {
        val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
        text = (value as? ModelItem)?.label ?: "未选择模型"
        if (index < 0) {
            background = Color(0, 0, 0, 0)
            foreground = SELECTOR_TEXT_FOREGROUND
            border = JBUI.Borders.empty(2, 0, 2, 0)
            isOpaque = false
        } else {
            background = if (isSelected) DROPDOWN_ROW_HOVER_BACKGROUND else DROPDOWN_BACKGROUND
            foreground = SELECTOR_TEXT_FOREGROUND
            border = JBUI.Borders.empty(6, 10)
            isOpaque = true
        }
        return component
    }
}

// ── Custom renderer for chat mode items ──────────────────────────────
internal class ChatModeRenderer : javax.swing.DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
        list: JList<*>?,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean,
    ): Component {
        val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
        text = (value as? ChatMode)?.displayName ?: ChatMode.AGENT.displayName
        if (index < 0) {
            background = Color(0, 0, 0, 0)
            foreground = SELECTOR_TEXT_FOREGROUND
            border = JBUI.Borders.empty(2, 0, 2, 0)
            isOpaque = false
        } else {
            background = if (isSelected) DROPDOWN_ROW_HOVER_BACKGROUND else DROPDOWN_BACKGROUND
            foreground = SELECTOR_TEXT_FOREGROUND
            border = JBUI.Borders.empty(6, 10)
            isOpaque = true
        }
        return component
    }
}

// ── Custom ComboBox UI that replaces the arrow button ────────────────
internal class SelectorComboBoxUI : BasicComboBoxUI() {
    override fun createArrowButton(): JButton {
        return SelectorArrowButton(AllIcons.General.ArrowDown)
    }

    override fun installDefaults() {
        super.installDefaults()
        comboBox.background = Color(0, 0, 0, 0)
        comboBox.foreground = SELECTOR_TEXT_FOREGROUND
    }

    override fun paintCurrentValueBackground(g: Graphics, bounds: java.awt.Rectangle, hasFocus: Boolean) {
        // Keep the selector transparent. Hover chrome is painted by the wrapper panel.
    }
}

// ── Utility to apply standard styling to a selector combo box ────────
internal fun <T> styleSelectorComboBox(comboBox: JComboBox<T>) {
    comboBox.ui = SelectorComboBoxUI()
    comboBox.isOpaque = false
    comboBox.border = JBUI.Borders.empty(0, 6, 0, 2)
    comboBox.background = Color(0, 0, 0, 0)
    comboBox.putClientProperty("JComboBox.isTableCellEditor", true)
    comboBox.putClientProperty("JComboBox.isBorderlessButton", true)
    comboBox.putClientProperty("JComponent.roundRect", true)
    comboBox.putClientProperty("JComponent.focusWidth", 0)
    comboBox.putClientProperty("JButton.backgroundColor", Color(0, 0, 0, 0))
    comboBox.putClientProperty("ComboBox.padding", JBUI.insets(0))
    comboBox.foreground = SELECTOR_TEXT_FOREGROUND
    comboBox.font = JBFont.label()
    comboBox.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    comboBox.maximumRowCount = 12
}

internal fun wrapSelectorComboBox(comboBox: JComboBox<*>): JPanel = SelectorChromePanel(comboBox)

private class SelectorChromePanel(
    private val comboBox: JComboBox<*>,
) : JPanel(BorderLayout()) {
    private var hovered = false
    private var pressed = false

    init {
        isOpaque = false
        add(comboBox, BorderLayout.CENTER)

        val mouseHandler = object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) {
                hovered = true
                repaint()
            }

            override fun mouseExited(e: MouseEvent) {
                hovered = isEventInside(e)
                if (!hovered) pressed = false
                repaint()
            }

            override fun mousePressed(e: MouseEvent) {
                pressed = true
                hovered = true
                repaint()
            }

            override fun mouseReleased(e: MouseEvent) {
                pressed = false
                hovered = isEventInside(e)
                repaint()
            }
        }

        installMouseTracking(this, mouseHandler)
        comboBox.addPopupMenuListener(object : PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) = repaint()

            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent?) {
                pressed = false
                repaint()
            }

            override fun popupMenuCanceled(e: PopupMenuEvent?) {
                pressed = false
                repaint()
            }
        })
    }

    override fun getMinimumSize(): Dimension = selectorSize(minWidth = JBUI.scale(54))

    override fun getMaximumSize(): Dimension = selectorSize(maxWidth = JBUI.scale(132))

    override fun getPreferredSize(): Dimension = selectorSize()

    override fun paintComponent(g: Graphics) {
        val showChrome = hovered || pressed || comboBox.isPopupVisible
        if (showChrome) {
            val g2 = g.create() as Graphics2D
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                val arc = JBUI.scale(10)
                val background = if (pressed || comboBox.isPopupVisible) SELECTOR_PRESSED_BACKGROUND else SELECTOR_HOVER_BACKGROUND
                g2.color = background
                g2.fillRoundRect(0, 0, width - 1, height - 1, arc, arc)
            } finally {
                g2.dispose()
            }
        }
        super.paintComponent(g)
    }

    private fun installMouseTracking(component: Component, handler: MouseAdapter) {
        component.addMouseListener(handler)
        if (component is java.awt.Container) {
            component.components.forEach { child -> installMouseTracking(child, handler) }
        }
    }

    private fun isEventInside(event: MouseEvent): Boolean {
        return runCatching {
            val point = SwingUtilities.convertPoint(event.component, event.point, this)
            contains(point)
        }.getOrDefault(false)
    }

    private fun selectorSize(minWidth: Int = JBUI.scale(54), maxWidth: Int = JBUI.scale(132)): Dimension {
        val preferred = comboBox.preferredSize
        val width = preferred.width.coerceIn(minWidth, maxWidth)
        return Dimension(width, JBUI.scale(22))
    }
}

private class SelectorArrowButton(
    private val arrowIcon: Icon,
) : JButton() {
    init {
        icon = arrowIcon
        disabledIcon = IconLoader.getDisabledIcon(arrowIcon)
        isOpaque = false
        isContentAreaFilled = false
        isBorderPainted = false
        isFocusPainted = false
        isFocusable = false
        isRolloverEnabled = false
        border = JBUI.Borders.empty()
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        margin = JBUI.insets(0)
        preferredSize = Dimension(JBUI.scale(14), JBUI.scale(20))
        minimumSize = preferredSize
        maximumSize = preferredSize
    }

    override fun paintComponent(g: Graphics) {
        val targetIcon = if (model.isEnabled) icon else disabledIcon
        targetIcon?.paintIcon(
            this,
            g,
            (width - targetIcon.iconWidth) / 2,
            (height - targetIcon.iconHeight) / 2,
        )
    }
}
