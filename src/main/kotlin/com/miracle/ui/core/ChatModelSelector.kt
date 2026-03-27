package com.miracle.ui.core

import com.intellij.icons.AllIcons
import com.intellij.util.ui.JBUI
import com.miracle.ui.core.ChatTheme.DROPDOWN_BACKGROUND
import com.miracle.ui.core.ChatTheme.DROPDOWN_ROW_HOVER_BACKGROUND
import com.miracle.ui.core.ChatTheme.SELECTOR_TEXT_FOREGROUND
import com.miracle.ui.smartconversation.settings.configuration.ChatMode
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JList
import javax.swing.plaf.basic.BasicComboBoxUI
import com.intellij.util.ui.JBFont

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
        return JButton(AllIcons.General.ArrowDown).apply {
            isOpaque = false
            isContentAreaFilled = false
            border = JBUI.Borders.empty(0, 0, 0, 4)
            isBorderPainted = false
            isFocusPainted = false
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            margin = JBUI.insets(0)
            preferredSize = Dimension(JBUI.scale(18), JBUI.scale(22))
            minimumSize = preferredSize
            maximumSize = preferredSize
        }
    }

    override fun installDefaults() {
        super.installDefaults()
        comboBox.background = Color(0, 0, 0, 0)
        comboBox.foreground = SELECTOR_TEXT_FOREGROUND
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
    val h = JBUI.scale(26)
    val maxW = JBUI.scale(154)
    comboBox.maximumSize = Dimension(maxW, h)
    comboBox.minimumSize = Dimension(JBUI.scale(72), h)
    comboBox.preferredSize = Dimension(comboBox.preferredSize.width.coerceIn(JBUI.scale(72), maxW), h)
}
