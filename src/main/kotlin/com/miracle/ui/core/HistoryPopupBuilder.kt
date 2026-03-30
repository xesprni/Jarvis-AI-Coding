package com.miracle.ui.core

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.miracle.ui.core.ChatTheme.DROPDOWN_BACKGROUND
import com.miracle.ui.core.ChatTheme.DROPDOWN_BORDER_COLOR
import com.miracle.ui.core.ChatTheme.DROPDOWN_ROW_BACKGROUND
import com.miracle.ui.core.ChatTheme.DROPDOWN_ROW_BORDER_COLOR
import com.miracle.ui.core.ChatTheme.DROPDOWN_ROW_HOVER_BACKGROUND
import com.miracle.ui.core.ChatTheme.MUTED_FOREGROUND
import com.miracle.ui.core.ChatTheme.SPLIT_LINE_COLOR
import com.miracle.utils.ConversationStore
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane

/**
 * Builds and shows the conversation-history popup.
 */
internal class HistoryPopupBuilder(
    private val project: Project,
) {
    /**
     * 显示历史会话弹窗。
     *
     * @param anchor 弹窗锚点组件
     * @param currentConversationId 当前会话 ID（从列表中排除）
     * @param onOpenConversation 选中会话时的回调
     */
    fun show(
        anchor: Component,
        currentConversationId: String?,
        onOpenConversation: (ConversationHistoryEntry) -> Unit,
    ) {
        val contentPanel = JPanel(BorderLayout()).apply {
            isOpaque = true
            background = DROPDOWN_BACKGROUND
            border = BorderFactory.createCompoundBorder(
                createRoundedBorder(DROPDOWN_BORDER_COLOR),
                JBUI.Borders.empty(5),
            )
        }
        val popupRef = arrayOfNulls<JBPopup>(1)

        fun refreshDropdown() {
            val entries = ConversationHistorySupport.buildEntries(
                conversations = ConversationStore.getConversations(project),
                currentConversationId = currentConversationId,
            )
            contentPanel.removeAll()
            contentPanel.add(
                createHeader(entries) {
                    deleteAllEntries(entries, ::refreshDropdown)
                },
                BorderLayout.NORTH,
            )
            contentPanel.add(
                createBody(
                    entries = entries,
                    openConversation = { entry ->
                        popupRef[0]?.cancel()
                        onOpenConversation(entry)
                    },
                    deleteConversation = { entry ->
                        deleteEntry(entry, ::refreshDropdown)
                    },
                ),
                BorderLayout.CENTER,
            )
            contentPanel.revalidate()
            contentPanel.repaint()
        }

        refreshDropdown()
        popupRef[0] = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(contentPanel, null)
            .setFocusable(true)
            .setRequestFocus(false)
            .setCancelOnClickOutside(true)
            .setCancelOnOtherWindowOpen(true)
            .setMovable(false)
            .setResizable(false)
            .createPopup()
        popupRef[0]?.showUnderneathOf(anchor)
    }

    // ── Header ───────────────────────────────────────────────────────

    private fun createHeader(
        entries: List<ConversationHistoryEntry>,
        deleteAllAction: () -> Unit,
    ): JComponent {
        val titlePanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            add(JBLabel("历史会话").apply { font = JBFont.label().asBold() })
            add(JBLabel("共 ${entries.size} 条").apply {
                font = JBFont.small(); foreground = MUTED_FOREGROUND
            })
        }
        val deleteAllButton = createHeaderTextButton("全部删除", "删除当前项目中的历史会话") {
            deleteAllAction()
        }.apply { isEnabled = entries.isNotEmpty() }

        return JPanel(BorderLayout()).apply {
            isOpaque = false
            border = BorderFactory.createCompoundBorder(
                JBUI.Borders.customLine(SPLIT_LINE_COLOR, 0, 0, 1, 0),
                JBUI.Borders.empty(0, 2, 5, 2),
            )
            add(titlePanel, BorderLayout.WEST)
            add(deleteAllButton, BorderLayout.EAST)
        }
    }

    // ── Body ─────────────────────────────────────────────────────────

    private fun createBody(
        entries: List<ConversationHistoryEntry>,
        openConversation: (ConversationHistoryEntry) -> Unit,
        deleteConversation: (ConversationHistoryEntry) -> Unit,
    ): JComponent {
        if (entries.isEmpty()) {
            return JPanel(BorderLayout()).apply {
                isOpaque = false
                preferredSize = Dimension(JBUI.scale(320), JBUI.scale(112))
                add(JBLabel("当前项目还没有历史会话。", javax.swing.SwingConstants.CENTER).apply {
                    foreground = MUTED_FOREGROUND; font = JBFont.label()
                }, BorderLayout.CENTER)
            }
        }

        val listPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS); isOpaque = false
        }
        entries.forEachIndexed { index, entry ->
            listPanel.add(createRow(entry, openConversation, deleteConversation))
            if (index != entries.lastIndex) listPanel.add(Box.createVerticalStrut(JBUI.scale(2)))
        }
        return JBScrollPane(listPanel).apply {
            isOpaque = false; border = JBUI.Borders.emptyTop(4)
            viewport.isOpaque = false; viewport.background = DROPDOWN_BACKGROUND
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            verticalScrollBar.unitIncrement = JBUI.scale(20)
            preferredSize = Dimension(JBUI.scale(360), JBUI.scale(236))
        }
    }

    // ── Row ──────────────────────────────────────────────────────────

    private fun createRow(
        entry: ConversationHistoryEntry,
        openConversation: (ConversationHistoryEntry) -> Unit,
        deleteConversation: (ConversationHistoryEntry) -> Unit,
    ): JComponent {
        val titleLabel = JBLabel(entry.title).apply { font = JBFont.label().asBold() }
        val timeLabel = JBLabel(formatTimestamp(entry.updatedTime)).apply {
            font = JBFont.small(); foreground = MUTED_FOREGROUND
        }
        val deleteButton = createHeaderIconButton(AllIcons.Actions.Close, "删除会话") {
            deleteConversation(entry)
        }
        val contentPanel = JPanel(BorderLayout(JBUI.scale(8), 0)).apply {
            isOpaque = false
            add(titleLabel, BorderLayout.CENTER)
            add(timeLabel, BorderLayout.EAST)
        }
        val rowPanel = JPanel(BorderLayout(JBUI.scale(8), 0)).apply {
            isOpaque = true; background = DROPDOWN_ROW_BACKGROUND
            border = BorderFactory.createCompoundBorder(
                createRoundedBorder(DROPDOWN_ROW_BORDER_COLOR),
                JBUI.Borders.empty(4, 8),
            )
            alignmentX = Component.LEFT_ALIGNMENT
            maximumSize = Dimension(Int.MAX_VALUE, JBUI.scale(34))
            add(contentPanel, BorderLayout.CENTER)
            add(deleteButton, BorderLayout.EAST)
        }

        val hoverListener = object : java.awt.event.MouseAdapter() {
            override fun mouseEntered(e: java.awt.event.MouseEvent?) {
                rowPanel.background = DROPDOWN_ROW_HOVER_BACKGROUND
            }
            override fun mouseExited(e: java.awt.event.MouseEvent?) {
                rowPanel.background = DROPDOWN_ROW_BACKGROUND
            }
            override fun mouseClicked(e: java.awt.event.MouseEvent?) {
                openConversation(entry)
            }
        }
        rowPanel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        rowPanel.addMouseListener(hoverListener)
        contentPanel.addMouseListener(hoverListener)
        titleLabel.addMouseListener(hoverListener)
        timeLabel.addMouseListener(hoverListener)
        return rowPanel
    }

    // ── Delete helpers ───────────────────────────────────────────────

    private fun deleteEntry(entry: ConversationHistoryEntry, onDeleted: () -> Unit) {
        val result = Messages.showYesNoDialog(
            project, "确定删除历史会话\u201C${entry.title}\u201D吗？",
            "删除历史会话", Messages.getQuestionIcon(),
        )
        if (result != Messages.YES) return
        ConversationStore.deleteConversation(project, entry.id)
        onDeleted()
    }

    private fun deleteAllEntries(entries: List<ConversationHistoryEntry>, onDeleted: () -> Unit) {
        if (entries.isEmpty()) return
        val result = Messages.showYesNoDialog(
            project, "确定删除全部 ${entries.size} 条历史会话吗？当前标签页不会被删除。",
            "删除全部历史会话", Messages.getQuestionIcon(),
        )
        if (result != Messages.YES) return
        entries.forEach { ConversationStore.deleteConversation(project, it.id) }
        onDeleted()
    }
}
