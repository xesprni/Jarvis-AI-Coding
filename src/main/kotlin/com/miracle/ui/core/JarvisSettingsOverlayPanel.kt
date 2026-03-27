package com.miracle.ui.core

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
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

class JarvisSettingsOverlayPanel(
    private val project: Project,
    private val parentDisposable: Disposable,
    private val onBack: () -> Unit,
    private val onModelsChanged: () -> Unit,
) : JPanel(BorderLayout()) {
    private val mcpStatusPanel = McpStatusPanel(project, parentDisposable)
    private val componentCache = linkedMapOf<JarvisSettingsSection, JComponent>()

    private val iconLabel = JLabel().apply {
        horizontalAlignment = SwingConstants.CENTER
        verticalAlignment = SwingConstants.CENTER
        preferredSize = JBUI.size(38, 38)
        minimumSize = preferredSize
        maximumSize = preferredSize
        opaque = true
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

    fun showSection(section: JarvisSettingsSection) {
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
            add(createFlatButton("返回聊天") { onBack() }, BorderLayout.EAST)
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

    private fun createFlatButton(text: String, action: () -> Unit): JButton {
        return JButton(text).apply {
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            isFocusPainted = false
            isOpaque = true
            background = PANEL_BACKGROUND
            border = JBUI.Borders.compound(
                JBUI.Borders.customLine(BORDER_COLOR, 1),
                JBUI.Borders.empty(6, 10),
            )
            addActionListener { action() }
        }
    }

    companion object {
        private val PANEL_BACKGROUND = JBColor(Color(247, 248, 250), Color(43, 45, 48))
        private val BORDER_COLOR = JBColor(Color(230, 238, 240), Color(30, 31, 34))
        private val MUTED_FOREGROUND = JBColor(Color(0x6B, 0x75, 0x86), Color(0xA0, 0xA8, 0xB8))
        private val ICON_BACKGROUND = JBColor(Color(252, 253, 255), Color(52, 54, 58))
    }
}
