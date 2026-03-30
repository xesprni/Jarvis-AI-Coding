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

/**
 * 将文本中的 HTML 特殊字符转义为对应的实体，防止在 HTML 渲染时被解析。
 *
 * @param text 原始文本
 * @return 转义后的安全文本
 */
internal fun escapeHtml(text: String): String {
    return text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}

/**
 * 创建一个圆角边框，使用指定颜色进行绘制。
 *
 * @param color 边框颜色
 * @return 圆角边框实例
 */
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

/**
 * 将文本复制到系统剪贴板。
 *
 * @param text 要复制的文本内容
 */
internal fun copyToClipboard(text: String) {
    val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
    clipboard.setContents(java.awt.datatransfer.StringSelection(text), null)
}

/**
 * 将 Unix 时间戳（毫秒）格式化为可读的时间字符串。
 *
 * @param epochMillis 毫秒级 Unix 时间戳
 * @return 格式化后的时间字符串
 */
internal fun formatTimestamp(epochMillis: Long): String {
    return ChatTheme.TIMESTAMP_FORMATTER.format(java.time.Instant.ofEpochMilli(epochMillis))
}

/**
 * 创建一个仅包含图标的按钮，无边框、无背景填充，适用于工具栏图标操作。
 *
 * @param icon 按钮图标
 * @param tooltip 鼠标悬停提示文本
 * @return 配置好的图标按钮
 */
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

/**
 * 创建一个用于顶部工具栏的图标按钮，点击时执行指定操作。
 *
 * @param icon 按钮图标
 * @param tooltip 鼠标悬停提示文本
 * @param action 点击回调
 * @return 配置好的工具栏图标按钮
 */
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

/**
 * 创建一个纯文本的顶部工具栏按钮，点击时执行指定操作。
 *
 * @param text 按钮文本
 * @param tooltip 鼠标悬停提示文本
 * @param action 点击回调
 * @return 配置好的文本按钮
 */
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

/**
 * 创建一个带有圆角边框的徽章标签，常用于显示状态标签。
 *
 * @param text 标签文本
 * @param background 背景色
 * @param foreground 前景色
 * @param borderColor 边框颜色
 * @return 配置好的标签组件
 */
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

/**
 * 操作按钮的配色方案，包含默认态和悬停态的背景色与边框色。
 */
internal data class ActionPalette(
    val background: Color,
    val hoverBackground: Color,
    val border: Color,
    val hoverBorder: Color,
    val foreground: Color,
)

/**
 * 创建一个操作按钮，支持主要（primary）和次要两种样式。
 *
 * @param text 按钮文本
 * @param tooltip 鼠标悬停提示文本
 * @param primary 是否使用主要样式，默认 false
 * @param action 点击回调
 * @return 配置好的操作按钮
 */
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

/**
 * 创建一个选项芯片按钮，使用次要样式，常用于快捷回复选项。
 *
 * @param text 按钮文本
 * @param tooltip 鼠标悬停提示文本
 * @param verticalPadding 垂直内边距，默认 4
 * @param horizontalPadding 水平内边距，默认 8
 * @param action 点击回调
 * @return 配置好的芯片按钮
 */
internal fun createOptionChipButton(
    text: String,
    tooltip: String,
    verticalPadding: Int = 4,
    horizontalPadding: Int = 8,
    action: () -> Unit,
): JButton {
    val palette = ActionPalette(
        background = ChatTheme.PLAN_ACTION_SECONDARY_BACKGROUND,
        hoverBackground = ChatTheme.PLAN_ACTION_SECONDARY_HOVER_BACKGROUND,
        border = ChatTheme.PLAN_ACTION_SECONDARY_BORDER,
        hoverBorder = ChatTheme.PLAN_ACTION_SECONDARY_HOVER_BORDER,
        foreground = ChatTheme.PLAN_ACTION_SECONDARY_FOREGROUND,
    )
    return createStyledActionButton(
        text = text,
        tooltip = tooltip,
        palette = palette,
        compact = true,
        bold = false,
        verticalPadding = verticalPadding,
        horizontalPadding = horizontalPadding,
        action = action,
    )
}

/**
 * 创建一个批准操作按钮，使用绿色主题配色。
 *
 * @param text 按钮文本
 * @param tooltip 鼠标悬停提示文本
 * @param action 点击回调
 * @return 配置好的批准按钮
 */
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

/**
 * 创建一个拒绝操作按钮，使用红色主题配色。
 *
 * @param text 按钮文本
 * @param tooltip 鼠标悬停提示文本
 * @param action 点击回调
 * @return 配置好的拒绝按钮
 */
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

/**
 * 根据配色方案创建带样式的操作按钮，支持悬停变色效果。
 *
 * @param text 按钮文本
 * @param tooltip 鼠标悬停提示文本
 * @param palette 配色方案
 * @param compact 是否使用紧凑模式
 * @param bold 是否使用粗体
 * @param verticalPadding 垂直内边距
 * @param horizontalPadding 水平内边距
 * @param action 点击回调
 * @return 配置好的按钮
 */
private fun createStyledActionButton(
    text: String,
    tooltip: String,
    palette: ActionPalette,
    compact: Boolean,
    bold: Boolean,
    verticalPadding: Int = if (compact) 4 else 5,
    horizontalPadding: Int = if (compact) 8 else 10,
    action: () -> Unit,
): JButton {
    fun JButton.applyPalette(background: Color, borderColor: Color) {
        this.background = background
        border = javax.swing.BorderFactory.createCompoundBorder(
            createRoundedBorder(borderColor),
            JBUI.Borders.empty(verticalPadding, horizontalPadding),
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

/**
 * 创建一个垂直方向的工具栏分隔线。
 *
 * @return 分隔线面板
 */
internal fun createToolbarSeparator(): JPanel {
    return JPanel().apply {
        isOpaque = true
        background = JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground()
        preferredSize = Dimension(1, JBUI.scale(16))
        minimumSize = Dimension(1, JBUI.scale(16))
        maximumSize = Dimension(1, JBUI.scale(16))
    }
}

/**
 * 消息卡片的外壳容器，包含根面板、内容区和头部面板。
 *
 * @property root 根面板
 * @property body 消息内容区面板
 * @property header 消息头部面板（包含作者信息、操作按钮等）
 */
// ── MessageShell data holder ─────────────────────────────────────────
internal data class MessageShell(
    val root: JPanel,
    val body: JPanel,
    val header: JPanel,
)
