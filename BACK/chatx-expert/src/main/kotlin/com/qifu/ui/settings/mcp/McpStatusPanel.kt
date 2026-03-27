package com.qifu.ui.settings.mcp

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.qifu.ui.settings.mcp.components.BaseCardLayoutPanel
import com.qifu.ui.settings.mcp.components.McpUiComponents
import com.qifu.ui.settings.mcp.viewmodel.McpStatusViewModel
import com.qihoo.finance.lowcode.common.util.Icons
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.RoundRectangle2D
import javax.swing.*

/**
 * 显示 MCP（Model Context Protocol）服务状态以及在这些服务上注册的工具列表的面板。
 *
 * 该面板具有以下功能：
 * - 使用卡片布局（CardLayout）在不同状态（加载、消息、内容）之间切换。
 * - 在后台线程中异步获取 MCP 服务状态。
 * - 为每个检测到的 MCP 服务器显示一个可折叠的部分。
 * - 每个部分显示服务器的名称、启用状态（可切换）和注册的工具列表。
 *
 * @param project 当前的 IntelliJ 项目。
 */
@Suppress("DialogTitleCapitalization")
class McpStatusPanel(private val project: Project) : BaseCardLayoutPanel() {

    private val viewModel = McpStatusViewModel(project)
    private val statusContainer = StatusContentPanel()
    private var suppressLoading = false

    init {
        border = JBUI.Borders.empty(12)
        initCardLayout()
        
        viewModel.setStateListener { state ->
            when (state) {
                is McpStatusViewModel.UiState.Loading -> {
                    if (!suppressLoading) {
                        showLoading("正在加载 MCP 服务信息...")
                    }
                }
                is McpStatusViewModel.UiState.Content -> {
                    statusContainer.updateContent(state.servers, viewModel)
                    showContent()
                    suppressLoading = false
                }
                is McpStatusViewModel.UiState.Error -> {
                    showMessage(state.message)
                    suppressLoading = false
                }
                is McpStatusViewModel.UiState.Empty -> {
                    showMessage("未检测到任何 MCP 服务器。")
                    suppressLoading = false
                }
            }
        }
    }

    override fun createContentPanel(): JComponent {
        return JBScrollPane(statusContainer).apply {
            border = JBUI.Borders.empty()
            viewportBorder = null
            viewport.background = null
            isOpaque = false
        }
    }

    /**
     * 刷新 MCP 服务状态
     */
    fun refresh(showLoading: Boolean = true) {
        suppressLoading = !showLoading
        viewModel.refresh()
    }

    /**
     * 包含所有服务器状态部分的容器面板
     */
    private class StatusContentPanel : JPanel() {
        private var lastSnapshot: List<ServerSnapshot> = emptyList()

        init {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty()
            isOpaque = false
        }

        fun updateContent(servers: List<McpStatusViewModel.ServerInfo>, viewModel: McpStatusViewModel) {
            val snapshot = buildSnapshot(servers)
            if (snapshot == lastSnapshot) {
                return
            }
            lastSnapshot = snapshot
            removeAll()
            
            servers.forEachIndexed { index, serverInfo ->
                if (index > 0) {
                    add(Box.createVerticalStrut(JBUI.scale(12)))
                }
                add(ServerSectionPanel(serverInfo, viewModel) { serverName, enabled ->
                    updateEnabledState(serverName, enabled)
                }.apply {
                    alignmentX = LEFT_ALIGNMENT
                })
            }

            add(Box.createVerticalGlue())
            revalidate()
            repaint()
        }

        private fun updateEnabledState(serverName: String, enabled: Boolean) {
            if (lastSnapshot.isEmpty()) {
                return
            }
            lastSnapshot = lastSnapshot.map { snapshot ->
                if (snapshot.serverName == serverName) {
                    snapshot.copy(enabled = enabled)
                } else {
                    snapshot
                }
            }
        }

        private fun buildSnapshot(servers: List<McpStatusViewModel.ServerInfo>): List<ServerSnapshot> {
            return servers.map { serverInfo ->
                ServerSnapshot(
                    serverName = serverInfo.serverName,
                    displayName = serverInfo.displayName,
                    enabled = serverInfo.enabled,
                    connected = serverInfo.connected,
                    connectionError = serverInfo.connectionError?.trim(),
                    space = serverInfo.space,
                    tools = serverInfo.tools.map { tool ->
                        ToolSnapshot(tool.name, tool.description?.trim())
                    }
                )
            }
        }

        private data class ToolSnapshot(val name: String, val description: String?)

        private data class ServerSnapshot(
            val serverName: String,
            val displayName: String,
            val enabled: Boolean,
            val connected: Boolean,
            val connectionError: String?,
            val space: String?,
            val tools: List<ToolSnapshot>
        )
    }

    /**
     * 显示单个 MCP 服务器信息的面板（可折叠）
     */
    private class ServerSectionPanel(
        private val serverInfo: McpStatusViewModel.ServerInfo,
        private val viewModel: McpStatusViewModel,
        private val onToggleStateChange: (String, Boolean) -> Unit
    ) : JPanel(BorderLayout()) {
        
        private var expanded = false
        private val collapsedHeight = JBUI.scale(44)
        private val expandedHeight = JBUI.scale(180)
        private val expandIcon = JBLabel(AllIcons.General.ArrowRight)
        
        private val contentPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(8, 20, 8, 20)
            isOpaque = false
            alignmentX = LEFT_ALIGNMENT
        }
        
        private val contentScroll = JBScrollPane(contentPanel).apply {
            border = JBUI.Borders.empty()
            viewportBorder = null
            isOpaque = false
            viewport.isOpaque = false
            horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            verticalScrollBar.unitIncrement = JBUI.scale(16)
        }

        init {
            isOpaque = false
            background = JBColor(Color(0xF7F9FC), Color(0x1F2023))
            border = JBUI.Borders.empty(12)
            
            val preferredWidth = JBUI.scale(360)
            minimumSize = Dimension(JBUI.scale(260), collapsedHeight)
            maximumSize = Dimension(Int.MAX_VALUE, expandedHeight + JBUI.scale(60))
            preferredSize = Dimension(preferredWidth, collapsedHeight)

            add(createHeader(), BorderLayout.NORTH)
            add(contentScroll, BorderLayout.CENTER)
            buildContent()
            updateExpandedState()
        }

        private fun createHeader(): JComponent {
            val header = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                isOpaque = false
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            }

            val clickListener = object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    toggleExpanded()
                }
            }

            expandIcon.border = JBUI.Borders.emptyRight(8)
            expandIcon.addMouseListener(clickListener)
            header.add(expandIcon)
            header.add(Box.createHorizontalStrut(JBUI.scale(6)))

            val nameLabel = JBLabel(serverInfo.displayName).apply {
                font = JBFont.label().biggerOn(1f).asBold()
                alignmentX = LEFT_ALIGNMENT
                toolTipText = serverInfo.displayName
            }
            nameLabel.addMouseListener(clickListener)
            header.add(nameLabel)

            header.add(Box.createHorizontalGlue())

            // Space 标签
            serverInfo.space?.takeIf { it.isNotBlank() }?.let { space ->
                header.add(McpUiComponents.createMcpLabel(space))
                header.add(Box.createHorizontalStrut(JBUI.scale(16)))
            }

            header.add(McpUiComponents.createServerToggle(serverInfo.enabled) { enabled ->
                onToggleStateChange(serverInfo.serverName, enabled)
                viewModel.setServerEnabled(serverInfo.serverName, enabled)
            })
            header.add(Box.createHorizontalStrut(JBUI.scale(8)))
            header.add(McpUiComponents.createActionButton("UNINSTALL", Icons.UNINSTALL) {
                viewModel.uninstallServer(serverInfo.serverName)
            })
            header.addMouseListener(clickListener)
            
            return header
        }

        private fun buildContent() {
            contentPanel.removeAll()

            // 连接错误信息
            if (!serverInfo.connectionError.isNullOrBlank() && !serverInfo.connected) {
                contentPanel.add(JBLabel("连接失败：${serverInfo.connectionError.trim()}").apply {
                    foreground = JBColor(Color(0xD93025), Color(0xFF7A7A))
                    font = JBFont.label()
                    alignmentX = LEFT_ALIGNMENT
                    border = JBUI.Borders.emptyBottom(6)
                    toolTipText = serverInfo.connectionError
                })
            }

            // 工具列表
            if (serverInfo.tools.isEmpty()) {
                contentPanel.add(JBLabel("未注册工具").apply {
                    border = JBUI.Borders.empty(2, 0)
                    foreground = JBUI.CurrentTheme.Label.disabledForeground()
                    alignmentX = LEFT_ALIGNMENT
                })
            } else {
                serverInfo.tools.forEachIndexed { index, tool ->
                    contentPanel.add(JBLabel("• ${tool.name}").apply {
                        font = JBFont.label().asBold()
                        border = JBUI.Borders.emptyTop(2)
                        alignmentX = LEFT_ALIGNMENT
                        toolTipText = tool.name
                    })
                    tool.description?.takeIf { it.isNotBlank() }?.let { desc ->
                        contentPanel.add(createDescriptionArea(desc.trim()))
                    }
                    if (index != serverInfo.tools.lastIndex) {
                        contentPanel.add(Box.createVerticalStrut(JBUI.scale(4)))
                    }
                }
            }
            
            contentPanel.revalidate()
            contentPanel.repaint()
        }

        private fun createDescriptionArea(text: String): JComponent {
            val normalized = text.replace("\r\n", "\n").trim()
            val truncated = if (normalized.length > MAX_DESCRIPTION_LENGTH) {
                normalized.take(MAX_DESCRIPTION_LENGTH).trimEnd() + "..."
            } else {
                normalized
            }
            return JBTextArea(truncated).apply {
                isEditable = false
                isFocusable = false
                lineWrap = true
                wrapStyleWord = true
                background = null
                isOpaque = false
                border = JBUI.Borders.empty(0, 16, 2, 0)
                font = JBFont.small()
                foreground = JBColor.GRAY
                alignmentX = LEFT_ALIGNMENT
                toolTipText = normalized
                maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
            }
        }

        private fun toggleExpanded() {
            expanded = !expanded
            updateExpandedState()
        }

        private fun updateExpandedState() {
            contentScroll.isVisible = expanded
            preferredSize = Dimension(preferredSize.width, if (expanded) expandedHeight else collapsedHeight)
            revalidate()
            expandIcon.icon = if (expanded) AllIcons.General.ArrowDown else AllIcons.General.ArrowRight
            repaint()
        }

        override fun paintComponent(g: Graphics?) {
            super.paintComponent(g)
            val g2 = g?.create() as? Graphics2D ?: return
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                val strokeWidth = JBUIScale.scale(1f)
                val arc = JBUIScale.scale(16f)
                val rect = RoundRectangle2D.Float(
                    strokeWidth / 2,
                    strokeWidth / 2,
                    width - strokeWidth,
                    height - strokeWidth,
                    arc,
                    arc
                )
                g2.color = background
                g2.fill(rect)
                g2.paint = GradientPaint(
                    0f, 0f,
                    JBColor(Color(0, 0, 0, 180), Color(255, 255, 255, 180)),
                    width.toFloat(), height.toFloat(),
                    JBColor(Color(0, 0, 0, 0), Color(255, 255, 255, 30))
                )
                g2.stroke = BasicStroke(strokeWidth)
                g2.draw(rect)
            } finally {
                g2.dispose()
            }
        }

        companion object {
            private const val MAX_DESCRIPTION_LENGTH = 220
        }
    }
}
