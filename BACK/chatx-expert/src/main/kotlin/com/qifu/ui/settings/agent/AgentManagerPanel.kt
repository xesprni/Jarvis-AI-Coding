package com.qifu.ui.settings.agent

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
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.qifu.agent.mcp.McpClientHub
import com.qifu.agent.mcp.McpConfigManager
import com.qifu.agent.tool.ToolRegistry
import com.qifu.services.AgentService
import com.qifu.utils.AgentConfig
import com.qihoo.finance.lowcode.common.util.IconUtil
import com.qihoo.finance.lowcode.common.util.Icons
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.nio.file.Files
import javax.swing.*

/**
 * 智能体管理面板
 * 
 * 提供智能体的列表展示、创建、编辑和删除功能。
 * 使用标签页（Tab）布局，支持同时编辑多个智能体。
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

    /**
     * 构建智能体列表面板
     * 包含标题、创建按钮和可滚动的智能体卡片列表
     */
    private fun buildListPanel(): JComponent {
        val title = JBLabel("智能体", IconUtil.getThemeAwareIcon(Icons.AI_AGENT_LIGHT, Icons.AI_AGENT), SwingConstants.LEFT).apply {
            font = JBFont.label().asBold().biggerOn(2f)
        }

        val addButton = createToolbarButton("创建") {
            openDetailTab(null)
        }

        val header = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.empty(12, 12, 8, 12)
            add(title, BorderLayout.WEST)
            add(JPanel(FlowLayout(FlowLayout.RIGHT, JBUI.scale(8), 0)).apply {
                isOpaque = false
                add(addButton)
            }, BorderLayout.EAST)
        }

        listContent.isOpaque = false
        listContent.border = JBUI.Borders.empty(0, 12, 12, 12)
        val scroll = JBScrollPane(listContent).apply {
            border = JBUI.Borders.empty()
            viewportBorder = null
            isOpaque = false
            viewport.isOpaque = false
        }

        return JPanel(BorderLayout()).apply {
            isOpaque = false
            add(header, BorderLayout.NORTH)
            add(scroll, BorderLayout.CENTER)
        }
    }

    /**
     * 创建统一风格的工具栏按钮
     * @param text 按钮文本
     * @param action 点击回调
     */
    private fun createToolbarButton(text: String, action: () -> Unit): JButton {
        return JButton(text).apply {
            putClientProperty("JButton.backgroundColor", JBColor.PanelBackground)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addActionListener { action() }
        }
    }

    /**
     * 打开智能体详情/编辑标签页
     * 
     * @param agent 要编辑的智能体配置，为 null 时表示创建新智能体
     * 
     * 逻辑说明：
     * - 如果是创建新智能体且已存在新建标签页，则直接切换到该标签页
     * - 编辑现有智能体时，每次都创建新的编辑标签页
     * - 标签页支持关闭按钮，关闭时检查未保存更改
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

    /**
     * 异步刷新智能体列表
     * 
     * 在后台线程中重新加载所有智能体配置，
     * 加载完成后在 EDT 线程中更新 UI。
     */
    private fun refreshAgents() {
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val loader = agentService.agentLoader
                // 清除缓存以获取最新配置
                loader.clearCache()
                allAgents = loader.loadAllAgents().allAgents
                // 切换到 UI 线程更新列表
                ApplicationManager.getApplication().invokeLater { renderList() }
            } catch (e: Exception) {
                log.warn("Failed to refresh agents", e)
            }
        }
    }

    /**
     * 渲染智能体列表
     * 
     * 将智能体分为两组显示：
     * 1. 自定义智能体（用户创建，支持编辑和删除）
     * 2. 内置智能体（系统预置，仅供查看）
     */
    private fun renderList() {
        listContent.removeAll()

        // 按来源分组：自定义 vs 内置
        val customAgents = allAgents.filter { it.scope != AgentConfig.Scope.BUILT_IN }
        val builtinAgents = allAgents.filter { it.scope == AgentConfig.Scope.BUILT_IN }

        listContent.add(createSection("自定义智能体", customAgents, isCustom = true))
        listContent.add(Box.createVerticalStrut(JBUI.scale(12)))
        listContent.add(createSection("内置智能体", builtinAgents, isCustom = false))

        listContent.revalidate()
        listContent.repaint()
    }

    private fun createSection(title: String, agents: List<AgentConfig>, isCustom: Boolean): JComponent {
        val section = JPanel()
        section.layout = BoxLayout(section, BoxLayout.Y_AXIS)
        section.isOpaque = false
        section.border = JBUI.Borders.empty(8, 0, 12, 0)

        section.add(JBLabel(title).apply {
            font = JBFont.label().asBold()
            foreground = JBColor(Color(0xC7, 0xD0, 0xD9), Color(0xC7, 0xD0, 0xD9))
            border = JBUI.Borders.empty(0, 4, 8, 0)
        })

        if (agents.isEmpty() && isCustom) {
            section.add(createEmptyCard())
        } else {
            agents.sortedBy { it.agentType }.forEach { agent ->
                section.add(createAgentCard(agent, isCustom))
                section.add(Box.createVerticalStrut(JBUI.scale(8)))
            }
        }

        return section
    }

    private fun createEmptyCard(): JComponent {
        return JPanel(BorderLayout()).apply {
            isOpaque = true
            background = JBColor(Color(0xF5, 0xF6, 0xF7), Color(0x20, 0x21, 0x24))
            border = JBUI.Borders.customLine(JBColor.border(), 1).let { JBUI.Borders.merge(it, JBUI.Borders.empty(12), true) }
            // 与 createAgentCard 保持一致的尺寸设置
            val fixedHeight = JBUI.scale(68)
            minimumSize = Dimension(0, fixedHeight)
            maximumSize = Dimension(Int.MAX_VALUE, fixedHeight)
            alignmentX = LEFT_ALIGNMENT
            add(JBLabel("暂无自定义智能体").apply {
                foreground = JBColor.GRAY
            }, BorderLayout.WEST)
        }
    }

    private fun createAgentCard(agent: AgentConfig, isCustom: Boolean): JComponent {
        val card = JPanel(BorderLayout())
        card.isOpaque = true
        card.background = JBColor(Color(0xF5, 0xF6, 0xF7), Color(0x20, 0x21, 0x24))
        card.border = JBUI.Borders.customLine(JBColor.border(), 1).let { JBUI.Borders.merge(it, JBUI.Borders.empty(12), true) }
        val fixedHeight = JBUI.scale(68)
        card.minimumSize = Dimension(0, fixedHeight)
        card.maximumSize = Dimension(Int.MAX_VALUE, fixedHeight)
        card.alignmentX = LEFT_ALIGNMENT

        val iconLabel = JLabel(IconUtil.getThemeAwareIcon(Icons.AI_AGENT_LIGHT, Icons.AI_AGENT)).apply {
            border = JBUI.Borders.emptyRight(12)
        }

        val nameLabel = JBLabel(agent.agentType).apply {
            font = JBFont.label().asBold().biggerOn(1f)
            foreground = JBColor(Color(0x18, 0x1A, 0x1F), Color(0xF5, 0xF6, 0xF7))
        }
        val descFull = agent.whenToUse
        val descShort = if (descFull.length > 15) descFull.take(15) + "..." else descFull
        val descLabel = JBLabel(descShort).apply {
            foreground = JBColor(Color(0x4A4A4A), Color(0xAEB0B4))
            toolTipText = descFull
        }

        val infoPanel = JPanel()
        infoPanel.layout = BoxLayout(infoPanel, BoxLayout.Y_AXIS)
        infoPanel.isOpaque = false
        infoPanel.add(nameLabel)
        infoPanel.add(Box.createVerticalStrut(JBUI.scale(4)))
        infoPanel.add(descLabel)

        val actionPanel = JPanel(FlowLayout(FlowLayout.RIGHT, JBUI.scale(4), 0)).apply {
            isOpaque = false
            val edit = JButton(AllIcons.Actions.Edit).apply {
                toolTipText = "编辑"
                preferredSize = JBUI.size(26, 26)
                minimumSize = preferredSize
                maximumSize = preferredSize
                isContentAreaFilled = false
                isFocusPainted = false
                isBorderPainted = false
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                addActionListener { openDetailTab(agent) }
            }
            val delete = JButton(AllIcons.Actions.Uninstall).apply {
                toolTipText = "删除"
                preferredSize = JBUI.size(26, 26)
                minimumSize = preferredSize
                maximumSize = preferredSize
                isContentAreaFilled = false
                isFocusPainted = false
                isBorderPainted = false
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                addActionListener {
                    if (agent.sourcePath == null) return@addActionListener
                    val res = Messages.showYesNoDialog(
                        project,
                        "确定删除智能体 ${agent.agentType} 吗？",
                        "删除智能体",
                        null
                    )
                    if (res == Messages.YES) {
                        try {
                            Files.deleteIfExists(agent.sourcePath)
                            refreshAgents()
                        } catch (e: Exception) {
                            log.warn("Failed to delete agent file", e)
                            Messages.showErrorDialog(project, "删除失败：${e.message ?: "未知错误"}", "删除智能体")
                        }
                    }
                }
            }
            edit.isVisible = agent.scope != AgentConfig.Scope.BUILT_IN
            edit.isEnabled = agent.scope != AgentConfig.Scope.BUILT_IN
            delete.isVisible = isCustom && agent.sourcePath != null
            if (edit.isVisible) add(edit)
            add(delete)
        }

        val center = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(iconLabel, BorderLayout.WEST)
            add(infoPanel, BorderLayout.CENTER)
        }

        card.add(center, BorderLayout.CENTER)
        card.add(actionPanel, BorderLayout.EAST)
        return card
    }

    /**
     * 可关闭的标签页头部组件
     */
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
}
