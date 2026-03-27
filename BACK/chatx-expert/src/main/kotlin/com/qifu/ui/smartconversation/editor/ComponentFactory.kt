package com.qifu.ui.smartconversation.editor

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.util.Key
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.intellij.ui.components.ActionLink
import com.intellij.ui.util.preferredHeight
import com.intellij.ui.util.preferredWidth
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import com.qihoo.finance.lowcode.smartconversation.actions.ReplaceCodeInMainEditorAction
import com.qihoo.finance.lowcode.common.util.Icons
import java.awt.Dimension
import javax.swing.SwingConstants

object ComponentFactory {

    val EXPANDED_KEY = Key.create<Boolean>("toolwindow.editor.isExpanded")
    const val MAX_VISIBLE_LINES = 8
    const val MIN_LINES_FOR_EXPAND = 8

    fun createExpandLinkPanel(editor: EditorEx): BorderLayoutPanel {
        return BorderLayoutPanel().apply {
            isOpaque = false
            border = JBUI.Borders.compound(
                JBUI.Borders.customLine(ColorUtil.fromHex("#48494b"), 0, 1, 1, 1),
                JBUI.Borders.empty(4)
            )
            addToCenter(createExpandLink(editor))
            putClientProperty("jarvis.expandedLinkPanel", true)
        }
    }

    fun createEditorActionGroup(editor: Editor): DefaultActionGroup {
        return DefaultActionGroup().apply {
            add(ReplaceCodeInMainEditorAction())
            (editor as? EditorEx)?.contextMenuGroupId?.let { groupId ->
                addAll(ActionManager.getInstance().getAction(groupId))
            }
        }
    }

    // 计算展示高度
    fun updateEditorPreferredSize(editor: EditorEx, expanded: Boolean) {
        editor.component.preferredSize = null   // 重置
        val lineHeight = editor.lineHeight
        val lineCount = editor.document.lineCount
        var desiredHeight = when {
            editor.isOneLineMode -> editor.component.preferredHeight
            lineCount <= MIN_LINES_FOR_EXPAND -> editor.component.preferredHeight
            expanded -> editor.component.preferredHeight
            else -> {
                val verticalInset = editor.component.preferredHeight - editor.contentComponent.preferredHeight
                val visibleLines = lineCount.coerceAtMost(MAX_VISIBLE_LINES)
                (lineHeight * visibleLines).coerceAtLeast(20) + verticalInset
            }
        }
        editor.scrollPane.horizontalScrollBar?.let {
            desiredHeight += it.preferredHeight
        }

        editor.component.preferredSize = Dimension(editor.component.preferredWidth, desiredHeight)
        editor.component.revalidate()
        editor.component.repaint()
    }

    private fun createExpandLink(editor: EditorEx): ActionLink {
        val isExpanded = EXPANDED_KEY.get(editor) ?: false
        val linkText = getLinkText(isExpanded)

        return ActionLink(linkText) { event ->
            val currentState = EXPANDED_KEY.get(editor) ?: false
            val newState = !currentState
            EXPANDED_KEY.set(editor, newState)

            val source = event.source as ActionLink
            source.text = getLinkText(newState)
            source.icon = if (newState) Icons.CollapseAll else Icons.ExpandAll

            updateEditorPreferredSize(editor, newState)
        }.apply {
            icon = if (isExpanded) Icons.CollapseAll else Icons.ExpandAll
            font = JBUI.Fonts.smallFont()
            foreground = JBColor.GRAY
            horizontalAlignment = SwingConstants.CENTER
        }
    }

    private fun getLinkText(expanded: Boolean): String {
        return if (expanded) {
            "Show Less"
        } else {
            "Show More"
        }
    }
}