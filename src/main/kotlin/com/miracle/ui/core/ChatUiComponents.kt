package com.miracle.ui.core

import com.intellij.openapi.util.IconLoader
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.border.AbstractBorder
import javax.swing.border.Border

// ── Shared utility functions used across the chat UI ─────────────────

internal fun escapeHtml(text: String): String {
    return text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}

internal fun createRoundedBorder(color: Color): Border {
    return object : AbstractBorder() {
        override fun paintBorder(c: Component, g: java.awt.Graphics, x: Int, y: Int, width: Int, height: Int) {
            val g2d = g as java.awt.Graphics2D
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
            g2d.color = color
            g2d.stroke = java.awt.BasicStroke(1f)
            g2d.drawRoundRect(x, y, width - 1, height - 1, JBUI.scale(8), JBUI.scale(8))
        }

        override fun getBorderInsets(c: Component): java.awt.Insets {
            return java.awt.Insets(2, 2, 2, 2)
        }

        override fun isBorderOpaque(): Boolean = false
    }
}

internal fun copyToClipboard(text: String) {
    val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
    clipboard.setContents(java.awt.datatransfer.StringSelection(text), null)
}

internal fun formatTimestamp(epochMillis: Long): String {
    return ChatTheme.TIMESTAMP_FORMATTER.format(java.time.Instant.ofEpochMilli(epochMillis))
}

internal fun createIconButton(icon: javax.swing.Icon, tooltip: String): JButton {
    return JButton(icon).apply {
        isOpaque = false
        isContentAreaFilled = false
        isBorderPainted = false
        isFocusPainted = false
        disabledIcon = IconLoader.getDisabledIcon(icon)
        toolTipText = tooltip
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        preferredSize = Dimension(JBUI.scale(24), JBUI.scale(24))
        minimumSize = preferredSize
        maximumSize = preferredSize
    }
}

internal fun createHeaderIconButton(icon: javax.swing.Icon, tooltip: String, action: () -> Unit): JButton {
    return JButton(icon).apply {
        isOpaque = false
        isContentAreaFilled = false
        isBorderPainted = false
        isFocusPainted = false
        toolTipText = tooltip
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        preferredSize = Dimension(JBUI.scale(22), JBUI.scale(22))
        addActionListener { action() }
    }
}

internal fun createHeaderTextButton(text: String, tooltip: String, action: () -> Unit): JButton {
    return JButton(text).apply {
        isOpaque = false
        isContentAreaFilled = false
        isBorderPainted = false
        isFocusPainted = false
        toolTipText = tooltip
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        font = JBFont.small()
        margin = JBUI.insets(0, 0, 0, 0)
        addActionListener { action() }
    }
}

internal fun createBadgeLabel(
    text: String,
    background: Color,
    foreground: Color,
    borderColor: Color,
): JLabel {
    return JLabel(text).apply {
        isOpaque = true
        this.background = background
        this.foreground = foreground
        font = JBFont.small()
        border = javax.swing.BorderFactory.createCompoundBorder(
            createRoundedBorder(borderColor),
            JBUI.Borders.empty(4, 8),
        )
    }
}

internal data class ActionPalette(
    val background: Color,
    val hoverBackground: Color,
    val border: Color,
    val hoverBorder: Color,
    val foreground: Color,
)

internal fun createActionButton(
    text: String,
    tooltip: String,
    primary: Boolean = false,
    action: () -> Unit,
): JButton {
    val palette = if (primary) {
        ActionPalette(
            background = ChatTheme.PLAN_ACTION_PRIMARY_BACKGROUND,
            hoverBackground = ChatTheme.PLAN_ACTION_PRIMARY_HOVER_BACKGROUND,
            border = ChatTheme.PLAN_ACTION_PRIMARY_BORDER,
            hoverBorder = ChatTheme.PLAN_ACTION_PRIMARY_HOVER_BORDER,
            foreground = ChatTheme.PLAN_ACTION_PRIMARY_FOREGROUND,
        )
    } else {
        ActionPalette(
            background = ChatTheme.PLAN_ACTION_SECONDARY_BACKGROUND,
            hoverBackground = ChatTheme.PLAN_ACTION_SECONDARY_HOVER_BACKGROUND,
            border = ChatTheme.PLAN_ACTION_SECONDARY_BORDER,
            hoverBorder = ChatTheme.PLAN_ACTION_SECONDARY_HOVER_BORDER,
            foreground = ChatTheme.PLAN_ACTION_SECONDARY_FOREGROUND,
        )
    }
    return createStyledActionButton(text, tooltip, palette, compact = false, bold = true, action = action)
}

internal fun createOptionChipButton(
    text: String,
    tooltip: String,
    action: () -> Unit,
): JButton {
    val palette = ActionPalette(
        background = ChatTheme.PLAN_ACTION_SECONDARY_BACKGROUND,
        hoverBackground = ChatTheme.PLAN_ACTION_SECONDARY_HOVER_BACKGROUND,
        border = ChatTheme.PLAN_ACTION_SECONDARY_BORDER,
        hoverBorder = ChatTheme.PLAN_ACTION_SECONDARY_HOVER_BORDER,
        foreground = ChatTheme.PLAN_ACTION_SECONDARY_FOREGROUND,
    )
    return createStyledActionButton(text, tooltip, palette, compact = true, bold = false, action = action)
}

internal fun createApproveButton(text: String, tooltip: String, action: () -> Unit): JButton {
    return createStyledActionButton(
        text = text,
        tooltip = tooltip,
        palette = ActionPalette(
            background = ChatTheme.APPROVE_ACTION_BACKGROUND,
            hoverBackground = ChatTheme.APPROVE_ACTION_HOVER_BACKGROUND,
            border = ChatTheme.APPROVE_ACTION_BORDER,
            hoverBorder = ChatTheme.APPROVE_ACTION_HOVER_BORDER,
            foreground = ChatTheme.PLAN_ACTION_PRIMARY_FOREGROUND,
        ),
        compact = false,
        bold = true,
        action = action,
    )
}

internal fun createRejectButton(text: String, tooltip: String, action: () -> Unit): JButton {
    return createStyledActionButton(
        text = text,
        tooltip = tooltip,
        palette = ActionPalette(
            background = ChatTheme.REJECT_ACTION_BACKGROUND,
            hoverBackground = ChatTheme.REJECT_ACTION_HOVER_BACKGROUND,
            border = ChatTheme.REJECT_ACTION_BORDER,
            hoverBorder = ChatTheme.REJECT_ACTION_HOVER_BORDER,
            foreground = ChatTheme.REJECT_ACTION_FOREGROUND,
        ),
        compact = false,
        bold = true,
        action = action,
    )
}

private fun createStyledActionButton(
    text: String,
    tooltip: String,
    palette: ActionPalette,
    compact: Boolean,
    bold: Boolean,
    action: () -> Unit,
): JButton {
    fun JButton.applyPalette(background: Color, borderColor: Color) {
        this.background = background
        border = javax.swing.BorderFactory.createCompoundBorder(
            createRoundedBorder(borderColor),
            JBUI.Borders.empty(if (compact) 5 else 7, if (compact) 10 else 12),
        )
    }

    return JButton(text).apply {
        isOpaque = true
        isContentAreaFilled = true
        isBorderPainted = false
        isFocusPainted = false
        this.foreground = palette.foreground
        toolTipText = tooltip
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        font = if (bold) JBFont.small().asBold() else JBFont.small()
        margin = JBUI.insets(0, 0, 0, 0)
        alignmentY = Component.CENTER_ALIGNMENT
        applyPalette(palette.background, palette.border)
        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) {
                applyPalette(palette.hoverBackground, palette.hoverBorder)
            }

            override fun mouseExited(e: MouseEvent) {
                applyPalette(palette.background, palette.border)
            }
        })
        addActionListener { action() }
    }
}

internal fun createToolbarSeparator(): JPanel {
    return JPanel().apply {
        isOpaque = true
        background = JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground()
        preferredSize = Dimension(1, JBUI.scale(16))
        minimumSize = Dimension(1, JBUI.scale(16))
        maximumSize = Dimension(1, JBUI.scale(16))
    }
}

// ── MessageShell data holder ─────────────────────────────────────────
internal data class MessageShell(
    val root: JPanel,
    val body: JPanel,
    val header: JPanel,
)
