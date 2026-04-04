package com.miracle.ui.settings.mcp

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.miracle.agent.mcp.McpClientHub
import com.miracle.agent.mcp.McpConfigListener
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.miracle.ui.settings.mcp.components.McpUiComponents
import com.miracle.ui.settings.mcp.viewmodel.McpStatusViewModel
import com.miracle.ui.settings.components.CardListComponents.createCardDescription
import com.miracle.ui.settings.components.CardListComponents.createCardSectionHeader
import com.miracle.ui.settings.components.CardListComponents.createCardListScrollPane
import com.miracle.ui.settings.components.CardListComponents.createCardHeaderRow
import com.miracle.ui.settings.components.CardListComponents.createCardBody
import com.miracle.ui.settings.components.CardListComponents.createCardShell
import com.miracle.ui.settings.components.CardListComponents.createIconActionButton
import com.miracle.ui.settings.components.CardListComponents.renderCardList
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.SwingConstants

/**
 * MCP 服务状态面板 — 使用与 SkillsPanel 一致的卡片列表风格。
 */
@Suppress("DialogTitleCapitalization")
class McpStatusPanel(
    private val project: Project,
    parentDisposable: Disposable
) : JPanel(BorderLayout()), Disposable {

    private val viewModel = McpStatusViewModel(project)
    private var suppressLoading = false
    private var hasLoaded = false

    private val listPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = false
        border = JBUI.Borders.emptyTop(8)
    }

    private val loadingLabel = JBLabel("正在加载 MCP 服务信息...").apply {
        font = JBFont.label()
        foreground = JBColor(Color(90, 100, 120), Color(150, 160, 175))
        horizontalAlignment = SwingConstants.CENTER
    }

    private val messageLabel = JBLabel().apply {
        font = JBFont.label()
        foreground = JBColor(Color(90, 100, 120), Color(150, 160, 175))
        horizontalAlignment = SwingConstants.CENTER
    }

    private val cardLayout = CardLayout()
    private val stackPanel = JPanel(cardLayout)

    init {
        Disposer.register(parentDisposable, this)
        isOpaque = false
        border = JBUI.Borders.empty(12)
        add(
            createCardSectionHeader(
                title = "MCP",
                subtitle = "MCP 服务状态与工具管理",
                buttonText = "新增",
                buttonIcon = AllIcons.General.Add,
            ) { showAddServerDialog() },
            BorderLayout.NORTH,
        )

        stackPanel.isOpaque = false

        // Loading card
        stackPanel.add(JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.empty(24)
            add(loadingLabel, BorderLayout.CENTER)
        }, CARD_LOADING)

        // Content card
        stackPanel.add(createCardListScrollPane(listPanel), CARD_CONTENT)

        // Message card
        stackPanel.add(JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.empty(24)
            add(messageLabel, BorderLayout.CENTER)
        }, CARD_MESSAGE)

        add(stackPanel, BorderLayout.CENTER)

        viewModel.setStateListener { state ->
            when (state) {
                is McpStatusViewModel.UiState.Loading -> {
                    if (!suppressLoading) {
                        cardLayout.show(stackPanel, CARD_LOADING)
                    }
                }
                is McpStatusViewModel.UiState.Content -> {
                    renderServers(state.servers)
                    cardLayout.show(stackPanel, CARD_CONTENT)
                    suppressLoading = false
                }
                is McpStatusViewModel.UiState.Error -> {
                    messageLabel.text = state.message
                    cardLayout.show(stackPanel, CARD_MESSAGE)
                    suppressLoading = false
                }
                is McpStatusViewModel.UiState.Empty -> {
                    messageLabel.text = "未检测到任何 MCP 服务器。"
                    cardLayout.show(stackPanel, CARD_MESSAGE)
                    suppressLoading = false
                }
            }
        }

        project.messageBus.connect(this).subscribe(
            McpClientHub.MCP_CONFIG_TOPIC,
            McpConfigListener { updatedProject, refreshStatusPanel, _ ->
                if (updatedProject == project && refreshStatusPanel) {
                    refresh(showLoading = false)
                }
            }
        )
    }

    fun refresh(showLoading: Boolean = true) {
        hasLoaded = true
        suppressLoading = !showLoading
        viewModel.refresh()
    }

    fun refreshIfNeeded() {
        refresh(showLoading = !hasLoaded)
    }

    override fun dispose() = Unit

    private fun showAddServerDialog() {
        val dialog = AddMcpServerDialog(project)
        if (dialog.showAndGet()) {
            val (serverName, config, scope) = dialog.getServerConfig()
            viewModel.addServer(serverName, config, scope)
            refresh(showLoading = false)
        }
    }

    private fun renderServers(servers: List<McpStatusViewModel.ServerInfo>) {
        val cards = servers.map { createServerCard(it) }
        renderCardList(listPanel, cards, "未检测到任何 MCP 服务器。")
    }

    private fun createServerCard(server: McpStatusViewModel.ServerInfo): JComponent {
        val disabled = !server.enabled
        val icon = McpUiComponents.LetterIcon(server.displayName, JBUI.scale(32))

        val nameLabel = JBLabel(server.displayName).apply {
            font = JBFont.label().asBold().biggerOn(0.5f)
            foreground = if (disabled) JBColor.GRAY else JBColor.foreground()
            toolTipText = server.displayName
        }

        // Meta line: scope tag + connection status
        val metaParts = mutableListOf<String>()
        server.space?.takeIf { it.isNotBlank() }?.let { metaParts.add(it) }
        metaParts.add(if (server.connected) "已连接" else "未连接")
        metaParts.add("${server.tools.size} 个工具")
        val metaLabel = JBLabel(metaParts.joinToString(" · ")).apply {
            font = JBFont.small()
            foreground = if (disabled) JBColor.GRAY else JBColor(Color(110, 118, 132), Color(150, 160, 175))
        }

        // Description: tool names or error
        val descText = when {
            !server.connectionError.isNullOrBlank() && !server.connected ->
                "连接失败：${server.connectionError.trim()}"
            server.tools.isNotEmpty() ->
                server.tools.joinToString("、") { it.name }
            else -> "未注册工具"
        }

        // Toggle
        val toggle = McpUiComponents.createServerToggle(server.enabled) { enabled ->
            viewModel.setServerEnabled(server.serverName, enabled)
            refresh(showLoading = false)
        }

        val eastPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            add(createIconActionButton(AllIcons.Actions.Uninstall, "卸载") {
                viewModel.uninstallServer(server.serverName)
                refresh(showLoading = false)
            })
            add(Box.createHorizontalStrut(JBUI.scale(6)))
            add(toggle)
        }

        val header = createCardHeaderRow(icon, nameLabel, metaLabel, eastPanel)
        val body = createCardBody(createCardDescription(descText, disabled))
        return createCardShell(header, body, CARD_HEIGHT, disabled)
    }

    companion object {
        private const val CARD_HEIGHT = 130
        private const val CARD_LOADING = "loading"
        private const val CARD_CONTENT = "content"
        private const val CARD_MESSAGE = "message"
    }
}
