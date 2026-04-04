package com.miracle.ui.settings.agent

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.miracle.agent.mcp.McpClientHub
import com.miracle.agent.mcp.McpConfigManager
import com.miracle.agent.tool.ToolRegistry
import com.miracle.services.AgentService
import com.miracle.ui.settings.mcp.components.McpUiComponents
import com.miracle.ui.settings.components.CardListComponents.createCardDescription
import com.miracle.ui.settings.components.CardListComponents.createCardSectionHeader
import com.miracle.ui.settings.components.CardListComponents.createCardListScrollPane
import com.miracle.ui.settings.components.CardListComponents.createCardHeaderRow
import com.miracle.ui.settings.components.CardListComponents.createCardBody
import com.miracle.ui.settings.components.CardListComponents.createCardShell
import com.miracle.ui.settings.components.CardListComponents.createIconActionButton
import com.miracle.ui.settings.components.CardListComponents.renderCardList
import com.miracle.utils.AgentConfig
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.BoxLayout
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.nio.file.Files
import javax.swing.Box
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

/**
 * 智能体管理面板
 *
 * 提供智能体的列表展示、创建、编辑和删除功能。
 * 使用标签页（Tab）布局，支持同时编辑多个智能体。
 */
@Suppress("DialogTitleCapitalization")
class AgentManagerPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val log = Logger.getInstance(AgentManagerPanel::class.java)
    private val agentService = project.service<AgentService>()

    private val tabs = JBTabbedPane()
    private var newAgentTab: AgentDetailPanel? = null
    private var allAgents: List<AgentConfig> = emptyList()
    private val detailPanels = mutableSetOf<AgentDetailPanel>()

    private val listContent = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = false
        border = JBUI.Borders.emptyTop(8)
    }

    init {
        isOpaque = false
        add(tabs, BorderLayout.CENTER)
        tabs.addTab("智能体列表", buildListPanel())
        refreshAgents()
    }

    fun refresh() {
        refreshAgents()
    }

    fun refreshToolSelections(serverName: String? = null) {
        if (detailPanels.isEmpty()) {
            McpConfigManager.invalidate(project)
            McpConfigManager.loadConfig(project)
            McpClientHub.getInstance(project).ensureInitialized()
            ToolRegistry.getAll()
            return
        }
        detailPanels.forEach { it.refreshToolsAfterMcpInstall(serverName) }
    }

    // ───────── List panel ─────────

    private fun buildListPanel(): JComponent {
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.empty(12)
            add(
                createCardSectionHeader(
                    title = "智能体",
                    subtitle = "管理自定义和内置智能体，支持配置系统提示词和可用工具。",
                    buttonText = "创建智能体",
                    buttonIcon = AllIcons.General.Add,
                ) { openDetailTab(null) },
                BorderLayout.NORTH,
            )
            add(createCardListScrollPane(listContent), BorderLayout.CENTER)
        }
    }

    // ───────── Detail tab management ─────────

    private fun openDetailTab(agent: AgentConfig?) {
        if (agent == null) {
            newAgentTab?.let {
                tabs.selectedComponent = it
                return
            }
        }

        lateinit var detailPanel: AgentDetailPanel
        detailPanel = AgentDetailPanel(
            project = project,
            agent = agent,
            existingAgentsProvider = { allAgents },
            onSaved = {
                if (newAgentTab === detailPanel) newAgentTab = null
                detailPanels.remove(detailPanel)
                tabs.remove(detailPanel)
                refreshAgents()
                tabs.selectedIndex = 0
            },
            onClose = {
                tabs.selectedComponent?.let {
                    val idx = tabs.indexOfComponent(detailPanel)
                    if (idx >= 0) tabs.remove(idx)
                }
                if (newAgentTab === detailPanel) newAgentTab = null
                detailPanels.remove(detailPanel)
                tabs.selectedIndex = 0
            }
        )
        val title = agent?.let { "编辑-${it.agentType}" } ?: "新增智能体"
        tabs.addTab(title, detailPanel)
        val tabIndex = tabs.indexOfComponent(detailPanel)
        tabs.setTabComponentAt(tabIndex, ClosableTab(title) {
            detailPanel.tryClose {
                tabs.remove(detailPanel)
                if (newAgentTab === detailPanel) newAgentTab = null
                tabs.selectedIndex = 0
            }
        })
        tabs.selectedComponent = detailPanel
        if (agent == null) newAgentTab = detailPanel
        detailPanels.add(detailPanel)
    }

    // ───────── Refresh / render ─────────

    private fun refreshAgents() {
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val loader = agentService.agentLoader
                loader.clearCache()
                allAgents = loader.loadAllAgents().allAgents
                ApplicationManager.getApplication().invokeLater { renderList() }
            } catch (e: Exception) {
                log.warn("Failed to refresh agents", e)
            }
        }
    }

    private fun renderList() {
        val cards = allAgents
            .sortedWith(compareBy<AgentConfig> { it.scope == AgentConfig.Scope.BUILT_IN }.thenBy { it.agentType.lowercase() })
            .map { createAgentCard(it) }
        renderCardList(listContent, cards, "暂无智能体。点击「创建智能体」以添加自定义智能体。")
    }

    // ───────── Agent card ─────────

    private fun createAgentCard(agent: AgentConfig): JComponent {
        val isBuiltIn = agent.scope == AgentConfig.Scope.BUILT_IN
        val icon = McpUiComponents.LetterIcon(agent.agentType, JBUI.scale(32))

        val titleLabel = JBLabel(agent.agentType).apply {
            font = JBFont.label().asBold().biggerOn(0.5f)
        }

        val scopeText = when (agent.scope) {
            AgentConfig.Scope.BUILT_IN -> "内置"
            AgentConfig.Scope.USER -> "User"
            AgentConfig.Scope.PROJECT -> "Project"
        }
        val meta = JBLabel(scopeText).apply {
            font = JBFont.small()
            foreground = JBColor(Color(110, 118, 132), Color(150, 160, 175))
        }

        val eastPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            if (!isBuiltIn) {
                add(createIconActionButton(AllIcons.Actions.Edit, "编辑") { openDetailTab(agent) })
                add(Box.createHorizontalStrut(JBUI.scale(6)))
            }
            if (!isBuiltIn && agent.sourcePath != null) {
                add(createIconActionButton(AllIcons.Actions.GC, "删除") { confirmDelete(agent) })
            }
        }

        val header = createCardHeaderRow(icon, titleLabel, meta, eastPanel)
        val body = createCardBody(createCardDescription(agent.whenToUse))
        return createCardShell(header, body, CARD_HEIGHT)
    }

    // ───────── Delete confirmation ─────────

    private fun confirmDelete(agent: AgentConfig) {
        if (agent.sourcePath == null) return
        val res = Messages.showYesNoDialog(
            project,
            "确定删除智能体「${agent.agentType}」吗？",
            "删除智能体",
            Messages.getQuestionIcon(),
        )
        if (res == Messages.YES) {
            try {
                Files.deleteIfExists(agent.sourcePath)
                refreshAgents()
            } catch (e: Exception) {
                log.warn("Failed to delete agent file", e)
                Messages.showWarningDialog(project, "删除失败：${e.message ?: "未知错误"}", "删除智能体")
            }
        }
    }

    // ───────── Closable tab ─────────

    private class ClosableTab(text: String, onClose: () -> Unit) : JPanel() {
        init {
            layout = FlowLayout(FlowLayout.LEFT, JBUI.scale(4), 0)
            isOpaque = false
            add(JBLabel(text))
            add(JButton("×").apply {
                margin = JBUI.emptyInsets()
                border = JBUI.Borders.empty()
                isContentAreaFilled = false
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                addActionListener { onClose() }
            })
        }
    }

    companion object {
        private const val CARD_HEIGHT = 160
    }
}
