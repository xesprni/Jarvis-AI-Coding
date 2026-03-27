package com.miracle.ui.core

import com.intellij.icons.AllIcons
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.miracle.utils.CheckpointFileChangeSummary
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel

internal class ComposerCheckpointRestorePanel(
    private val onRestoreAll: (messageId: String) -> Unit,
    private val onRestoreFile: (messageId: String, filePath: String) -> Unit,
    private val onDiffFile: (messageId: String, filePath: String) -> Unit,
) : JPanel(BorderLayout()) {

    private var currentMessageId: String? = null
    private var currentFiles: List<CheckpointFileChangeSummary> = emptyList()
    private var expanded: Boolean = false

    private val summaryLabel = JBLabel().apply {
        font = JBFont.small()
    }
    private val toggleLabel = JBLabel()
    private val restoreAllButton = createOptionChipButton(
        text = "\u5168\u90E8\u8FD8\u539F",
        tooltip = "\u5C06\u672C\u8F6E\u6240\u6709\u6587\u4EF6\u8FD8\u539F\u5230\u4FEE\u6539\u524D",
    ) {
        currentMessageId?.let(onRestoreAll)
    }.apply {
        alignmentY = Component.CENTER_ALIGNMENT
        applySmallChipBorder()
    }

    private val detailsPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = false
        border = JBUI.Borders.emptyBottom(2)
        isVisible = false
        alignmentX = Component.LEFT_ALIGNMENT
    }

    private val summaryContent = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        isOpaque = false
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        add(toggleLabel)
        add(Box.createHorizontalStrut(JBUI.scale(4)))
        add(summaryLabel)
    }

    private val summaryBar = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        isOpaque = false
        add(summaryContent)
        add(Box.createHorizontalGlue())
        add(restoreAllButton)
    }

    init {
        isOpaque = true
        background = ChatTheme.PLAN_CARD_SURFACE_BACKGROUND
        border = BorderFactory.createCompoundBorder(
            createRoundedBorder(ChatTheme.PLAN_ACTION_SECONDARY_BORDER),
            JBUI.Borders.empty(2, 6),
        )
        alignmentX = Component.LEFT_ALIGNMENT
        add(detailsPanel, BorderLayout.CENTER)
        add(summaryBar, BorderLayout.SOUTH)
        isVisible = false
        installToggleBehavior(summaryContent)
        installToggleBehavior(summaryLabel)
        installToggleBehavior(toggleLabel)
        refreshUi()
    }

    fun updateContent(messageId: String?, files: List<CheckpointFileChangeSummary>) {
        val isSameMessage = currentMessageId == messageId
        currentMessageId = messageId
        currentFiles = files

        if (files.isEmpty()) {
            expanded = false
        } else if (!isSameMessage) {
            expanded = false
        }

        rebuildDetails()
        refreshUi()
    }

    fun clear() {
        updateContent(null, emptyList())
    }

    private fun rebuildDetails() {
        detailsPanel.removeAll()
        currentFiles.forEachIndexed { index, summary ->
            detailsPanel.add(createRow(summary))
            if (index < currentFiles.lastIndex) {
                detailsPanel.add(Box.createVerticalStrut(JBUI.scale(3)))
            }
        }
    }

    private fun createRow(summary: CheckpointFileChangeSummary): JPanel {
        val pathLabel = JBLabel(summary.relativePath).apply {
            font = JBFont.small()
            toolTipText = summary.absolutePath
        }
        val statText = buildString {
            if (summary.isNewFile) {
                append("\u65B0\u5EFA \u00B7 ")
            }
            append("+${summary.addedLines}/-${summary.deletedLines}")
        }
        val statLabel = JBLabel(statText).apply {
            font = JBFont.small()
            foreground = ChatTheme.MUTED_FOREGROUND
            border = JBUI.Borders.emptyLeft(8)
            minimumSize = preferredSize
        }
        val restoreButton: JButton = createOptionChipButton(
            text = "\u8FD8\u539F",
            tooltip = "\u4EC5\u6062\u590D\u8FD9\u4E2A\u6587\u4EF6",
        ) {
            currentMessageId?.let { onRestoreFile(it, summary.absolutePath) }
        }.apply { applySmallChipBorder() }

        val diffButton: JButton = createOptionChipButton(
            text = "Diff",
            tooltip = "\u67E5\u770B\u6B64\u6587\u4EF6\u7684\u5DEE\u5F02",
        ) {
            currentMessageId?.let { onDiffFile(it, summary.absolutePath) }
        }.apply { applySmallChipBorder() }

        val infoPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            add(pathLabel)
            add(statLabel)
        }

        val buttonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            add(diffButton)
            add(Box.createHorizontalStrut(JBUI.scale(4)))
            add(restoreButton)
        }

        return JPanel(BorderLayout()).apply {
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
            add(infoPanel, BorderLayout.CENTER)
            add(buttonPanel, BorderLayout.EAST)
        }
    }

    private fun toggleExpanded() {
        if (currentFiles.isEmpty()) {
            return
        }
        expanded = !expanded
        refreshUi()
    }

    private fun installToggleBehavior(component: Component) {
        component.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        component.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                toggleExpanded()
            }
        })
    }

    private fun refreshUi() {
        val fileCount = currentFiles.size
        val totalLines = currentFiles.sumOf { it.totalChangedLines }
        summaryLabel.text = "\u672C\u8F6E\u6539\u52A8 $totalLines \u884C \u00B7 $fileCount \u4E2A\u6587\u4EF6"
        toggleLabel.icon = if (expanded) AllIcons.General.ArrowDown else AllIcons.General.ArrowUp
        detailsPanel.isVisible = expanded && currentFiles.isNotEmpty()
        restoreAllButton.isVisible = currentFiles.isNotEmpty()
        isVisible = currentFiles.isNotEmpty()
        revalidate()
        repaint()
    }

    private fun JButton.applySmallChipBorder() {
        border = BorderFactory.createCompoundBorder(
            createRoundedBorder(ChatTheme.PLAN_ACTION_SECONDARY_BORDER),
            JBUI.Borders.empty(2, 6),
        )
    }
}
