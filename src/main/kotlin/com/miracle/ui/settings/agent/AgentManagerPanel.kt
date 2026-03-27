package com.miracle.ui.settings.agent

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.miracle.agent.mcp.McpClientHub
import com.miracle.agent.mcp.McpConfigManager
import com.miracle.agent.tool.ToolRegistry
import com.miracle.services.AgentService
import com.miracle.ui.settings.mcp.components.McpUiComponents
import com.miracle.utils.AgentConfig
import java.awt.*
import java.nio.file.Files
import javax.swing.*

/**
 * 智能体管理面板
 *
 * 提供智能体的列表展示、创建、编辑和删除功能。
 * 使用标签页（Tab）布局，支持同时编辑多个智能体。
 * 采用与 SkillsPanel 一致的卡片列表风格。
 *
 * @param project 当前 IntelliJ 项目实例
 */
@Suppress("DialogTitleCapitalization")
class AgentManagerPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val log = Logger.getInstance(AgentManagerPanel::class.java)
    private val agentService = project.service<AgentService>()

    /** 标签页容器，用于切换智能体列表和编辑面板 */
    private val tabs = JBTabbedPane()

    /** 当前正在创建的新智能体标签页引用，防止重复创建 */
    private var newAgentTab: AgentDetailPanel? = null

    /** 缓存的所有智能体配置列表 */
    private var allAgents: List<AgentConfig> = emptyList()
    private val detailPanels = mutableSetOf<AgentDetailPanel>()

    /** 智能体列表内容容器 */
    private val listContent = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = false
        border = JBUI.Borders.emptyTop(8)
    }
    private val listPanel = buildListPanel()

    init {
        isOpaque = false
        add(tabs, BorderLayout.CENTER)
        tabs.addTab("智能体列表", listPanel)
        refreshAgents()
    }

    /**
     * 刷新智能体列表
     * 对外暴露的刷新方法，供外部调用
     */
    fun refresh() {
        refreshAgents()
    }

    fun refreshToolSelections(serverName: String? = null) {
        if (detailPanels.isEmpty()) {
            // 触发 ToolRegistry 更新 MCP 工具缓存
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
            add(createHeader(), BorderLayout.NORTH)
            add(createListContainer(), BorderLayout.CENTER)
        }
    }

    private fun createHeader(): JComponent {
        val title = JBLabel("智能体", SwingConstants.LEFT).apply {
            font = JBFont.label().asBold().biggerOn(2f)
        }
        val subtitle = JBLabel("管理自定义和内置智能体，支持配置系统提示词和可用工具。").apply {
            foreground = MUTED_FOREGROUND
            border = JBUI.Borders.emptyTop(4)
            alignmentX = Component.LEFT_ALIGNMENT
            maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
        }

        val addButton = JButton("创建智能体", AllIcons.General.Add).apply {
            putClientProperty("JButton.backgroundColor", JBColor.PanelBackground)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addActionListener { openDetailTab(null) }
            maximumSize = preferredSize
        }

        val topRow = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
            add(title)
            add(Box.createHorizontalGlue())
            add(addButton)
        }

        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            border = JBUI.Borders.emptyBottom(8)
            add(topRow)
            add(subtitle)
        }
    }

    private fun createListContainer(): JComponent {
        return JBScrollPane(listContent).apply {
            border = JBUI.Borders.empty()
            viewportBorder = null
            isOpaque = false
            viewport.isOpaque = false
            horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            verticalScrollBar.unitIncrement = JBUI.scale(16)
        }
    }

    // ───────── Detail tab management ─────────

    /**
     * 打开智能体详情/编辑标签页
     *
     * @param agent 要编辑的智能体配置，为 null 时表示创建新智能体
     */
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
        listContent.removeAll()
        if (allAgents.isEmpty()) {
            listContent.add(createEmptyState())
        } else {
            allAgents.sortedWith(compareBy<AgentConfig> { it.scope == AgentConfig.Scope.BUILT_IN }.thenBy { it.agentType.lowercase() })
                .forEachIndexed { index, agent ->
                    if (index != 0) {
                        listContent.add(JSeparator(SwingConstants.HORIZONTAL).apply {
                            maximumSize = Dimension(Int.MAX_VALUE, 10)
                        })
                    }
                    listContent.add(createAgentCard(agent).apply { alignmentX = Component.LEFT_ALIGNMENT })
                }
            listContent.add(Box.createVerticalStrut(JBUI.scale(12)))
        }
        listContent.revalidate()
        listContent.repaint()
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

        // 描述文本，自动换行
        val descNormalized = agent.whenToUse.trim()
        val descShortened = if (descNormalized.length > 260) descNormalized.take(260) + "..." else descNormalized
        val desc = JBTextArea(descShortened).apply {
            isEditable = false
            isOpaque = false
            lineWrap = true
            wrapStyleWord = true
            font = JBFont.label()
            foreground = JBColor(Color(50, 60, 80), Color(200, 205, 215))
            border = null
            minimumSize = Dimension(0, preferredSize.height)
            maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
        }

        // 操作按钮
        val buttonsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
        }

        if (!isBuiltIn) {
            val editButton = JButton(AllIcons.Actions.Edit).apply {
                toolTipText = "编辑"
                isOpaque = false
                isContentAreaFilled = false
                border = JBUI.Borders.empty()
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                preferredSize = Dimension(JBUI.scale(28), JBUI.scale(28))
                maximumSize = preferredSize
                addActionListener { openDetailTab(agent) }
            }
            buttonsPanel.add(editButton)
            buttonsPanel.add(Box.createHorizontalStrut(JBUI.scale(6)))
        }

        if (!isBuiltIn && agent.sourcePath != null) {
            val deleteButton = JButton(AllIcons.Actions.GC).apply {
                toolTipText = "删除"
                isOpaque = false
                isContentAreaFilled = false
                border = JBUI.Borders.empty()
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                preferredSize = Dimension(JBUI.scale(28), JBUI.scale(28))
                maximumSize = preferredSize
                addActionListener { confirmDelete(agent) }
            }
            buttonsPanel.add(deleteButton)
        }

        // ── header row ──

        val header = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(JLabel(icon).apply {
                preferredSize = Dimension(JBUI.scale(40), JBUI.scale(40))
                horizontalAlignment = SwingConstants.CENTER
                verticalAlignment = SwingConstants.CENTER
            }, BorderLayout.WEST)

            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                add(titleLabel)
                add(Box.createVerticalStrut(JBUI.scale(2)))
                add(meta)
            }, BorderLayout.CENTER)

            add(buttonsPanel, BorderLayout.EAST)
        }

        val body = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.emptyTop(8)
            add(desc, BorderLayout.CENTER)
        }

        return JPanel(BorderLayout()).apply {
            isOpaque = true
            background = JBColor.PanelBackground
            border = JBUI.Borders.empty(12)
            minimumSize = Dimension(0, JBUI.scale(CARD_HEIGHT))
            preferredSize = Dimension(Int.MAX_VALUE, JBUI.scale(CARD_HEIGHT))
            maximumSize = Dimension(Int.MAX_VALUE, JBUI.scale(CARD_HEIGHT))
            add(header, BorderLayout.NORTH)
            add(body, BorderLayout.CENTER)
        }
    }

    // ───────── Empty state ─────────

    private fun createEmptyState(): JComponent {
        val label = JBLabel("暂无智能体。点击「创建智能体」以添加自定义智能体。").apply {
            foreground = JBColor(Color(90, 100, 120), Color(150, 160, 175))
        }
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.empty(16)
            add(label, BorderLayout.WEST)
        }
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
        private val MUTED_FOREGROUND = JBColor(Color(0x6B, 0x75, 0x86), Color(0xA0, 0xA8, 0xB8))
    }
}
