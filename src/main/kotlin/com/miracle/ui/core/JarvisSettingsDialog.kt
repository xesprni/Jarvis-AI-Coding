package com.miracle.ui.core

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBUI
import com.miracle.ui.settings.agent.AgentManagerPanel
import com.miracle.ui.settings.autoapprove.AutoApproveSettingsPanel
import com.miracle.ui.settings.mcp.McpStatusPanel
import com.miracle.ui.settings.mcp.components.McpConfigFooterPanel
import com.miracle.ui.settings.models.ModelsListPanel
import com.miracle.ui.settings.rules.RulesManagerPanel
import com.miracle.ui.settings.skills.SkillsPanel
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel

class JarvisSettingsDialog(
    private val project: Project,
    initialTab: SettingsTab = SettingsTab.MODELS,
) : DialogWrapper(project) {
    private val mcpStatusPanel by lazy(LazyThreadSafetyMode.NONE) { McpStatusPanel(project, disposable) }
    private val mcpTab by lazy(LazyThreadSafetyMode.NONE) { createMcpPanel(project) }
    private val autoApprovePanel = AutoApproveSettingsPanel()

    private val tabs = JBTabbedPane().apply {
        addTab(SettingsTab.MODELS.title, ModelsListPanel(project) {
            project.getService(JarvisToolWindowService::class.java).panel?.refreshModels()
        })
        addTab(SettingsTab.MCP.title, mcpTab)
        addTab(SettingsTab.AGENT.title, AgentManagerPanel(project))
        addTab(SettingsTab.SKILLS.title, SkillsPanel(project, disposable))
        addTab(SettingsTab.RULES.title, RulesManagerPanel(project))
        addTab(SettingsTab.AUTO_APPROVE.title, autoApprovePanel)
        addChangeListener { refreshMcpIfSelected() }
    }

    init {
        title = "Jarvis 设置"
        init()
        tabs.selectedIndex = tabs.indexOfTab(initialTab.title).coerceAtLeast(0)
    }

    override fun createCenterPanel(): JComponent {
        return JPanel(BorderLayout()).apply {
            preferredSize = Dimension(JBUI.scale(980), JBUI.scale(680))
            add(tabs, BorderLayout.CENTER)
        }
    }

    override fun doOKAction() {
        runCatching {
            autoApprovePanel.applyChanges()
        }.onFailure {
            Messages.showErrorDialog(project, it.message ?: "保存自动审批设置失败", "Jarvis 设置")
            return
        }
        super.doOKAction()
    }

    private fun createMcpPanel(project: Project): JComponent {
        return JPanel(BorderLayout()).apply {
            add(mcpStatusPanel, BorderLayout.CENTER)
            add(McpConfigFooterPanel(project), BorderLayout.SOUTH)
        }
    }

    private fun refreshMcpIfSelected() {
        if (tabs.selectedComponent === mcpTab) {
            mcpStatusPanel.refreshIfNeeded()
        }
    }

    enum class SettingsTab(val title: String) {
        MODELS("模型"),
        MCP("MCP"),
        AGENT("智能体"),
        SKILLS("Skills"),
        RULES("Rules"),
        AUTO_APPROVE("自动审批"),
    }
}
