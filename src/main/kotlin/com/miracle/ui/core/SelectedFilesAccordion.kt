package com.miracle.ui.core

import com.intellij.icons.AllIcons
import com.intellij.ui.components.ActionLink
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.event.ItemEvent
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JToggleButton
import javax.swing.SwingConstants

/**
 * 可折叠的关联文件列表组件，显示文件链接并支持展开/折叠。
 *
 * @param links 文件链接列表
 */
internal class SelectedFilesAccordion(
    links: List<ActionLink>,
) : JPanel(BorderLayout()) {

    init {
        isOpaque = false
        val contentPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            isVisible = true
            border = JBUI.Borders.empty(4, 0)
            links.forEach { link ->
                add(link)
                add(Box.createVerticalStrut(JBUI.scale(4)))
            }
        }
        add(createToggleButton(contentPanel, links.size), BorderLayout.NORTH)
        add(contentPanel, BorderLayout.CENTER)
    }

    private fun createToggleButton(contentPanel: JComponent, fileCount: Int): JToggleButton {
        return JToggleButton("关联文件 (+$fileCount)", AllIcons.General.ArrowDown).apply {
            isFocusPainted = false
            isContentAreaFilled = false
            border = null
            selectedIcon = AllIcons.General.ArrowUp
            isSelected = true
            horizontalAlignment = SwingConstants.LEFT
            horizontalTextPosition = SwingConstants.RIGHT
            iconTextGap = JBUI.scale(4)
            addItemListener { event ->
                contentPanel.isVisible = event.stateChange == ItemEvent.SELECTED
            }
        }
    }
}
