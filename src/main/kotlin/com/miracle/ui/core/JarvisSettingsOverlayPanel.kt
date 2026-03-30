package com.miracle.ui.core

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.miracle.ui.settings.agent.AgentManagerPanel
import com.miracle.ui.settings.autoapprove.AutoApproveSettingsPanel
import com.miracle.ui.settings.mcp.McpStatusPanel
import com.miracle.ui.settings.mcp.components.McpConfigFooterPanel
import com.miracle.ui.settings.models.ModelsListPanel
import com.miracle.ui.settings.rules.RulesManagerPanel
import com.miracle.ui.settings.skills.SkillsPanel
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

/**
 * Jarvis 设置覆盖面板，在工具窗口中以覆盖层的形式展示各设置分区。
 *
 * @param project 当前项目实例
 * @param parentDisposable 父级可释放资源
 * @param onBack 返回回调
 * @param onModelsChanged 模型列表变更回调
 */
class JarvisSettingsOverlayPanel(
    private val project: Project,
    private val parentDisposable: Disposable,
    private val onBack: () -> Unit,
    private val onModelsChanged: () -> Unit,
) : JPanel(BorderLayout()) {
    /** MCP 状态面板，懒加载 */
    private val mcpStatusPanel = McpStatusPanel(project, parentDisposable)
    /** 设置分区组件缓存，避免重复创建 */
    private val componentCache = linkedMapOf<JarvisSettingsSection, JComponent>()

    private val iconLabel = JLabel().apply {
        horizontalAlignment = SwingConstants.CENTER
        verticalAlignment = SwingConstants.CENTER
        preferredSize = JBUI.size(38, 38)
        minimumSize = preferredSize
        maximumSize = preferredSize
        isOpaque = true
        background = ICON_BACKGROUND
        border = BorderFactory.createCompoundBorder(
            JBUI.Borders.customLine(BORDER_COLOR, 1),
            JBUI.Borders.empty(6),
        )
    }
    private val titleLabel = JBLabel().apply {
        font = JBFont.label().asBold().biggerOn(2f)
    }
    private val descriptionLabel = JBLabel().apply {
        font = JBFont.small()
        foreground = MUTED_FOREGROUND
    }
    private val contentHost = JPanel(BorderLayout()).apply {
        isOpaque = true
        background = PANEL_BACKGROUND
        border = JBUI.Borders.empty(14)
    }

    init {
        isOpaque = true
        background = PANEL_BACKGROUND
        border = JBUI.Borders.empty()
        add(createHeader(), BorderLayout.NORTH)
        add(contentHost, BorderLayout.CENTER)
    }

    /**
     * 显示指定设置分区的内容。
     *
     * @param section 要显示的设置分区
     */
    internal fun showSection(section: JarvisSettingsSection) {
        titleLabel.text = section.title
        descriptionLabel.text = section.description
        iconLabel.icon = section.icon

        val component = componentCache.getOrPut(section) { createSectionComponent(section) }
        contentHost.removeAll()
        contentHost.add(component, BorderLayout.CENTER)
        contentHost.revalidate()
        contentHost.repaint()

        if (section == JarvisSettingsSection.MCP) {
            mcpStatusPanel.refreshIfNeeded()
        }
    }

    private fun createHeader(): JComponent {
        val titlePanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            add(iconLabel)
            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                border = JBUI.Borders.emptyLeft(10)
                add(titleLabel)
                add(descriptionLabel)
            })
        }

        return JPanel(BorderLayout()).apply {
            isOpaque = true
            background = PANEL_BACKGROUND
            border = JBUI.Borders.compound(
                JBUI.Borders.customLine(BORDER_COLOR, 0, 0, 1, 0),
                JBUI.Borders.empty(12, 14),
            )
            add(titlePanel, BorderLayout.WEST)
            add(createBackButton { onBack() }, BorderLayout.EAST)
        }
    }

    private fun createSectionComponent(section: JarvisSettingsSection): JComponent {
        return when (section) {
            JarvisSettingsSection.MODELS -> ModelsListPanel(project) { onModelsChanged() }
            JarvisSettingsSection.MCP -> JPanel(BorderLayout()).apply {
                isOpaque = true
                background = PANEL_BACKGROUND
                add(mcpStatusPanel, BorderLayout.CENTER)
                add(McpConfigFooterPanel(project), BorderLayout.SOUTH)
            }
            JarvisSettingsSection.AGENT -> AgentManagerPanel(project)
            JarvisSettingsSection.SKILLS -> SkillsPanel(project, parentDisposable)
            JarvisSettingsSection.RULES -> RulesManagerPanel(project)
            JarvisSettingsSection.AUTO_APPROVE -> AutoApproveSettingsPanel()
        }
    }

    private fun createBackButton(action: () -> Unit): JButton {
        return object : JButton("返回", AllIcons.Actions.Back) {
            init {
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                isFocusPainted = false
                isFocusable = false
                isOpaque = false
                isContentAreaFilled = false
                isBorderPainted = false
                isRolloverEnabled = true
                font = JBFont.small()
                foreground = BUTTON_FOREGROUND
                iconTextGap = JBUI.scale(3)
                border = JBUI.Borders.empty(0, 9)
                preferredSize = Dimension(JBUI.scale(68), JBUI.scale(24))
                minimumSize = preferredSize
                maximumSize = preferredSize
                toolTipText = "返回聊天"
                addActionListener { action() }
            }

            override fun paintComponent(g: Graphics) {
                val g2 = g.create() as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                val arc = JBUI.scale(18)
                g2.color = when {
                    model.isPressed -> BUTTON_PRESSED_BACKGROUND
                    model.isRollover -> BUTTON_HOVER_BACKGROUND
                    else -> BUTTON_BACKGROUND
                }
                g2.fillRoundRect(0, 0, width - 1, height - 1, arc, arc)

                if (model.isRollover || model.isPressed) {
                    g2.color = if (model.isPressed) BUTTON_PRESSED_BORDER else BUTTON_HOVER_BORDER
                    g2.drawRoundRect(0, 0, width - 1, height - 1, arc, arc)
                }
                g2.dispose()

                foreground = when {
                    model.isPressed -> BUTTON_FOREGROUND
                    model.isRollover -> BUTTON_FOREGROUND
                    else -> BUTTON_MUTED_FOREGROUND
                }
                super.paintComponent(g)
            }
        }
    }

    companion object {
        private val PANEL_BACKGROUND = JBColor(Color(247, 248, 250), Color(43, 45, 48))
        private val BORDER_COLOR = JBColor(Color(230, 238, 240), Color(30, 31, 34))
        private val MUTED_FOREGROUND = JBColor(Color(0x6B, 0x75, 0x86), Color(0xA0, 0xA8, 0xB8))
        private val ICON_BACKGROUND = JBColor(Color(252, 253, 255), Color(52, 54, 58))
        private val BUTTON_BACKGROUND = JBColor(Color(244, 247, 250), Color(49, 52, 57))
        private val BUTTON_HOVER_BACKGROUND = JBColor(Color(237, 242, 247), Color(56, 61, 67))
        private val BUTTON_PRESSED_BACKGROUND = JBColor(Color(230, 236, 243), Color(63, 68, 75))
        private val BUTTON_HOVER_BORDER = JBColor(Color(208, 219, 229), Color(88, 95, 105))
        private val BUTTON_PRESSED_BORDER = JBColor(Color(191, 205, 219), Color(104, 112, 123))
        private val BUTTON_FOREGROUND = JBColor(Color(0x1F, 0x29, 0x37), Color(0xE6, 0xE9, 0xEE))
        private val BUTTON_MUTED_FOREGROUND = JBColor(Color(0x5B, 0x67, 0x76), Color(0xB8, 0xC0, 0xCC))
    }
}
