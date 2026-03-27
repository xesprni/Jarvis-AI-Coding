package com.qifu.ui.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.qifu.agent.mcp.McpClientHub
import com.qifu.agent.mcp.McpConfigListener
import com.qifu.ui.settings.agent.AgentManagerPanel
import com.qifu.ui.settings.mcp.McpMarketPanel
import com.qifu.ui.settings.mcp.McpStatusPanel
import com.qifu.ui.settings.mcp.components.McpConfigFooterPanel
import com.qifu.ui.settings.mcp.components.StyledBackButton
import com.qifu.ui.settings.skills.SkillsPanel
import java.awt.*
import javax.swing.*

/**
 * 插件设置的覆盖层面板
 *
 * 提供多标签页管理的设置界面，包括：
 * - MCP 服务状态管理标签页
 * - MCP 市场标签页
 * - 返回按钮用于退出设置界面
 * - 半透明背景遮罩效果
 *
 * 使用 CardLayout 在不同标签页内容之间切换
 */
class SettingsOverlayPanel(
    private val project: Project,
    private val parentDisposable: Disposable,
    private val onBack: () -> Unit
) : JPanel(GridBagLayout()) {

    /**
     * 标签页定义数据类
     */
    private data class TabDefinition(val id: String, val title: String, val component: JComponent)

    /** MCP 服务状态面板 */
    private val mcpStatusPanel = McpStatusPanel(project)

    /** MCP 市场面板 */
    private val mcpMarketPanel = McpMarketPanel(
        project,
        onInstalled = { serverName -> agentManagerPanel.refreshToolSelections(serverName) }
    )

    /** Agent 管理面板 */
    private val agentManagerPanel = AgentManagerPanel(project)

    /** 技能面板 */
    private val skillsPanel = SkillsPanel(project).apply {
        Disposer.register(parentDisposable, this)
    }

    /** 标签页定义列表，包含所有可用的标签页 */
    private val tabDefinitions = listOf(
        // MCP 服务管理标签页
        TabDefinition(
            TAB_MCP,
            "MCP 服务",
            JPanel(BorderLayout()).apply {
                isOpaque = false
                border = JBUI.Borders.emptyTop(8)
                add(mcpStatusPanel, BorderLayout.CENTER)
                add(McpConfigFooterPanel(project), BorderLayout.SOUTH)
            }
        ),
        // MCP 市场标签页
        TabDefinition(
            TAB_MCP_MARKET,
            "MCP 市场",
            mcpMarketPanel
        ),
        // Agent 管理标签页
        TabDefinition(
            TAB_AGENT,
            "智能体",
            agentManagerPanel
        ),
        // Skills 管理标签页
        TabDefinition(
            TAB_SKILLS,
            "Skills",
            skillsPanel
        )
    )

    private val tabDefinitionsById = tabDefinitions.associateBy { it.id }

    /** CardLayout 布局管理器，用于切换标签页内容 */
    private val tabContentLayout = CardLayout()

    /** 标签页内容容器面板 */
    private val tabContentPanel = JPanel(tabContentLayout).apply {
        isOpaque = false
        border = JBUI.Borders.emptyTop(12)
    }

    /** 当前选中的标签页 ID */
    private var currentTabId: String = TAB_MCP

    private val titleLabel = JLabel().apply {
        font = JBFont.label().asBold().biggerOn(2f)
        foreground = JBColor(Color(0x18, 0x22, 0x34), Color(0xE8, 0xEA, 0xEC))
    }
    private val tabLabels = mapOf(
        TAB_MCP to createTabLabel("MCP 设置", TAB_MCP),
        TAB_MCP_MARKET to createTabLabel("MCP 市场", TAB_MCP_MARKET)
    )
    private val tabSwitcher = createTabSwitcher()

    /**
     * 初始化面板
     *
     * 初始化流程：
     * 1. 订阅 MCP 配置变更事件，自动刷新 MCP 标签页
     * 2. 将所有标签页内容添加到 CardLayout 容器
     * 3. 创建主内容面板，包含头部和标签页内容
     * 4. 使用 GridBagLayout 居中显示内容面板
     * 5. 默认选中 MCP 标签页
     */
    init {
        isOpaque = false
        border = JBUI.Borders.empty()

        // 订阅 MCP 配置变更事件
        project.messageBus.connect(parentDisposable).subscribe(
            McpClientHub.MCP_CONFIG_TOPIC,
            McpConfigListener { targetProject, refreshStatusPanel, refreshMarketPanel ->
                if (targetProject != project) {
                    return@McpConfigListener
                }
                ApplicationManager.getApplication().invokeLater {
                    if (refreshStatusPanel) {
                        mcpStatusPanel.refresh(showLoading = false)
                    }
                    if (refreshMarketPanel) {
                        mcpMarketPanel.refresh()
                    }
                }
            }
        )

        // 将所有标签页内容添加到容器
        tabDefinitions.forEach { definition ->
            tabContentPanel.add(definition.component, definition.id)
        }

        // 创建主内容面板
        val contentPanel = JPanel(BorderLayout()).apply {
            background = JBColor.PanelBackground
            border = JBUI.Borders.empty(16, 20, 20, 20)
            add(createHeader(), BorderLayout.NORTH)
            add(tabContentPanel, BorderLayout.CENTER)
        }

        // 使用 GridBagLayout 居中显示
        val constraints = GridBagConstraints().apply {
            fill = GridBagConstraints.BOTH
            weightx = 1.0
            weighty = 1.0
            gridx = 0
            gridy = 0
        }
        add(contentPanel, constraints)

        // 默认选中 MCP 标签页
        selectTab(TAB_MCP, triggerRefresh = false)
    }

    /**
     * 创建头部组件
     *
     * 包含导航栏（返回按钮和标题）
     */
    private fun createHeader(): JComponent {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            border = JBUI.Borders.emptyBottom(12)
            add(createNavigationRow())
            add(Box.createHorizontalStrut(JBUI.scale(8)))
            add(titleLabel)
            add(Box.createHorizontalGlue())
            add(tabSwitcher)
        }
    }

    /**
     * 创建导航栏
     *
     * 包含：返回按钮
     */
    private fun createNavigationRow(): JComponent {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            add(StyledBackButton { onBack() })
        }
    }

    private fun createTabSwitcher(): JComponent {
        return JPanel(GridLayout(1, tabLabels.size, JBUI.scale(4), 0)).apply {
            isOpaque = false
            border = JBUI.Borders.emptyLeft(12)
            tabLabels.values.forEach { add(it) }
            isVisible = false
        }
    }

    private fun createTabLabel(text: String, tabId: String): JLabel {
        return JLabel(text, SwingConstants.CENTER).apply {
            isOpaque = true
            border = JBUI.Borders.empty(6, 12)
            font = JBFont.label().asBold()
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addMouseListener(object : java.awt.event.MouseAdapter() {
                override fun mouseClicked(e: java.awt.event.MouseEvent?) {
                    if (currentTabId != tabId) {
                        selectTab(tabId)
                    }
                }
            })
        }
    }

    /**
     * 刷新指定标签页的内容
     */
    fun refresh(tabId: String = currentTabId, silent: Boolean = false) {
        when (tabId) {
            TAB_MCP -> mcpStatusPanel.refresh(showLoading = !silent)
            TAB_MCP_MARKET -> mcpMarketPanel.refresh()
            TAB_AGENT -> agentManagerPanel.refresh()
            TAB_SKILLS -> skillsPanel.refresh()
        }
    }


    /**
     * 选中并切换到指定标签页
     */
    fun selectTab(tabId: String, triggerRefresh: Boolean = true) {
        // 检查标签页是否存在
        val definition = tabDefinitionsById[tabId] ?: return
        currentTabId = definition.id
        updateTitle(definition.title)
        updateTabSwitcher(definition.id)
        tabContentLayout.show(tabContentPanel, definition.id)
        if (triggerRefresh) refresh(definition.id)
    }

    private fun updateTabSwitcher(tabId: String) {
        val showSwitcher = tabId == TAB_MCP || tabId == TAB_MCP_MARKET
        tabSwitcher.isVisible = showSwitcher
        tabLabels.forEach { (id, label) ->
            val selected = id == tabId
            label.background = if (selected) JBColor(Color(0xE6, 0xF0, 0xFF), Color(0x2A, 0x35, 0x4A)) else JBColor.PanelBackground
            label.foreground = if (selected) JBColor(Color(0x1A, 0x4B, 0x9E), Color(0x9AB6FF)) else JBColor.foreground()
        }
        tabSwitcher.revalidate()
        tabSwitcher.repaint()
    }

    private fun updateTitle(title: String) {
        titleLabel.text = title
    }

    /**
     * 绘制组件背景
     *
     * 绘制半透明的背景遮罩层，营造覆盖层效果
     */
    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        // 绘制半透明背景（亮色模式：浅灰色半透明，暗色模式：深色半透明）
        g2.color = JBColor(Color(224, 224, 220, 100),Color(15, 18, 22, 160))
        g2.fillRect(0, 0, width, height)
        g2.dispose()
        super.paintComponent(g)
    }

    companion object {
        /** MCP 标签页 ID */
        const val TAB_MCP = "MCP"

        /** MCP 市场标签页 ID */
        const val TAB_MCP_MARKET = "MCP_MARKET"

        /** Agent 管理标签页 ID */
        const val TAB_AGENT = "AGENT"

        const val TAB_SKILLS = "SKILLS"
    }
}
