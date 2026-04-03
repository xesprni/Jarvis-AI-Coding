package com.miracle.ui.core

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.WrapLayout
import com.miracle.utils.toRelativePath
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.Timer

internal class AssociatedContextHeaderPanel(
    private val project: Project,
    private val onAddRequested: (Component) -> Unit,
    private val onRemoveRequested: (AssociatedContextItem) -> Unit,
    private val onPredictedConfirmed: (AssociatedContextItem.AssociatedFile) -> Unit = {},
) : JPanel(BorderLayout()) {

    private val itemsPanel = JPanel(WrapLayout(FlowLayout.LEFT, JBUI.scale(4), JBUI.scale(4))).apply {
        isOpaque = false
        border = JBUI.Borders.empty(4, 8, 0, 8)
    }
    private val emptyText = JBLabel("暂无关联文件/代码").apply {
        foreground = JBUI.CurrentTheme.Label.disabledForeground()
        font = JBUI.Fonts.miniFont()
    }
    private val addButton = AddButton { onAddRequested(it) }
    private val chipComponents = linkedMapOf<String, JComponent>()

    init {
        isOpaque = false
        add(itemsPanel, BorderLayout.CENTER)
        setItems(emptyList())
    }

    fun setItems(items: List<AssociatedContextItem>) {
        itemsPanel.removeAll()
        chipComponents.clear()
        itemsPanel.add(addButton)
        if (items.isEmpty()) {
            itemsPanel.add(emptyText)
        } else {
            items.forEach { item ->
                val chip = createChip(item)
                chipComponents[item.key] = chip
                itemsPanel.add(chip)
            }
        }
        revalidate()
        repaint()
    }

    fun highlightItem(key: String) {
        val component = chipComponents[key] ?: return
        component.scrollRectToVisible(component.bounds)
        Timer(1200) { _: ActionEvent ->
            if (component is AssociatedTagChip) {
                component.setHighlighted(false)
            }
        }.apply {
            isRepeats = false
            start()
        }
        if (component is AssociatedTagChip) {
            component.setHighlighted(true)
        }
    }

    private fun createChip(item: AssociatedContextItem): JComponent {
        val isSuggested = item is AssociatedContextItem.AssociatedFile && item.suggested
        return if (isSuggested) {
            SuggestedTagChip(
                text = resolveChipText(item),
                icon = resolveChipIcon(item),
                tooltip = resolveTooltip(item),
                sourceLabel = (item as AssociatedContextItem.AssociatedFile).sourceLabel,
                onConfirm = { onPredictedConfirmed(item as AssociatedContextItem.AssociatedFile) },
            )
        } else {
            AssociatedTagChip(
                text = resolveChipText(item),
                icon = resolveChipIcon(item),
                tooltip = resolveTooltip(item),
                onRemove = { onRemoveRequested(item) },
            )
        }
    }

    private fun resolveChipText(item: AssociatedContextItem): String {
        return when (item) {
            is AssociatedContextItem.AssociatedFile -> item.displayName
            is AssociatedContextItem.AssociatedCodeSelection -> item.displayName
        }
    }

    private fun resolveTooltip(item: AssociatedContextItem): String {
        val basePath = project.basePath
        return when (item) {
            is AssociatedContextItem.AssociatedFile -> {
                val relativePath = if (basePath.isNullOrBlank()) item.path else toRelativePath(item.path, basePath)
                if (item.suggested) {
                    val suffix = item.sourceLabel?.let { " ($it)" }.orEmpty()
                    "$relativePath$suffix\n点击添加到上下文"
                } else {
                    relativePath
                }
            }
            is AssociatedContextItem.AssociatedCodeSelection -> {
                val relativePath = if (basePath.isNullOrBlank()) item.filePath else toRelativePath(item.filePath, basePath)
                "<html>$relativePath<br/>Lines ${item.startLine}-${item.endLine}</html>"
            }
        }
    }

    private fun resolveChipIcon(item: AssociatedContextItem): javax.swing.Icon {
        return when (item) {
            is AssociatedContextItem.AssociatedFile -> {
                IconUtil.scale(
                    FileTypeManager.getInstance().getFileTypeByFileName(File(item.path).name).icon
                    ?: AllIcons.FileTypes.Any_type
                    , null, 0.65f)
            }
            is AssociatedContextItem.AssociatedCodeSelection -> {
                IconUtil.scale(
                    FileTypeManager.getInstance().getFileTypeByFileName(File(item.filePath).name).icon
                    ?: AllIcons.FileTypes.Text
                    , null, 0.65f)
            }
        }
    }

    private fun String?.orEmpty(): String = this ?: ""

    private class AddButton(
        private val onAdd: (Component) -> Unit,
    ) : JButton() {
        init {
            cursor = Cursor(Cursor.HAND_CURSOR)
            preferredSize = JBUI.size(16, 16)
            minimumSize = preferredSize
            maximumSize = preferredSize
            isContentAreaFilled = false
            isOpaque = false
            isBorderPainted = false
            isFocusPainted = false
            border = null
            toolTipText = "添加关联文件"
            icon = IconUtil.scale(AllIcons.General.InlineAdd, null, 0.75f)
            rolloverIcon = IconUtil.scale(AllIcons.General.InlineAddHover, null, 0.75f)
            pressedIcon = rolloverIcon
            addActionListener { onAdd(this) }
        }

        override fun paintComponent(g: Graphics) {
            ChatTagPaintUtil.drawRoundedBackground(g, this, true)
            super.paintComponent(g)
        }
    }

    /**
     * 已选中文件的标签 chip，带删除按钮
     */
    private class AssociatedTagChip(
        text: String,
        icon: javax.swing.Icon?,
        tooltip: String,
        onRemove: () -> Unit,
    ) : JPanel() {
        private var highlighted = false

        init {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            cursor = Cursor(Cursor.HAND_CURSOR)
            border = JBUI.Borders.empty(0, 5)
            toolTipText = tooltip

            add(JBLabel(text).apply {
                this.icon = icon
                font = JBUI.Fonts.miniFont()
                iconTextGap = JBUI.scale(4)
            })
            add(Box.createHorizontalStrut(JBUI.scale(2)))
            add(object : JButton(AllIcons.Actions.Close) {
                init {
                    val iconSize = JBUI.size(AllIcons.Actions.Close.iconWidth, AllIcons.Actions.Close.iconHeight)
                    preferredSize = iconSize
                    minimumSize = iconSize
                    maximumSize = iconSize
                    border = BorderFactory.createEmptyBorder(0, 4, 0, 4)
                    isContentAreaFilled = false
                    isOpaque = false
                    isBorderPainted = false
                    isFocusPainted = false
                    toolTipText = "移除关联"
                    rolloverIcon = AllIcons.Actions.CloseHovered
                    addActionListener { onRemove() }
                }
            })
        }

        fun setHighlighted(value: Boolean) {
            highlighted = value
            repaint()
        }

        override fun getPreferredSize() = super.getPreferredSize().let { java.awt.Dimension(it.width, 16) }
        override fun getMinimumSize() = preferredSize
        override fun getMaximumSize() = preferredSize

        override fun paintComponent(g: Graphics) {
            ChatTagPaintUtil.drawRoundedBackground(g, this, selected = !highlighted)
            super.paintComponent(g)
        }
    }

    /**
     * 推荐文件的标签 chip，置灰显示，点击后添加到上下文。
     * 使用虚线边框和半透明背景，无删除按钮，显示来源标签。
     */
    private class SuggestedTagChip(
        text: String,
        icon: javax.swing.Icon?,
        tooltip: String,
        sourceLabel: String?,
        onConfirm: () -> Unit,
    ) : JPanel() {

        init {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            cursor = Cursor(Cursor.HAND_CURSOR)
            border = JBUI.Borders.empty(0, 5)
            toolTipText = tooltip

            add(JBLabel(text).apply {
                this.icon = icon
                font = JBUI.Fonts.miniFont()
                iconTextGap = JBUI.scale(4)
                foreground = JBUI.CurrentTheme.Label.disabledForeground()
            })
            if (!sourceLabel.isNullOrBlank()) {
                add(JBLabel(sourceLabel).apply {
                    font = JBUI.Fonts.miniFont()
                    foreground = JBUI.CurrentTheme.Label.disabledForeground()
                    border = JBUI.Borders.emptyLeft(4)
                })
            }
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    onConfirm()
                }
            })
        }

        override fun getPreferredSize() = super.getPreferredSize().let { java.awt.Dimension(it.width, 16) }
        override fun getMinimumSize() = preferredSize
        override fun getMaximumSize() = preferredSize

        override fun paintComponent(g: Graphics) {
            // 使用虚线边框 + 半透明效果表示推荐状态
            val g2 = g.create() as Graphics2D
            try {
                ChatTagPaintUtil.drawRoundedBackground(g2, this, selected = false)
            } finally {
                g2.dispose()
            }
            super.paintComponent(g)
        }
    }
}
