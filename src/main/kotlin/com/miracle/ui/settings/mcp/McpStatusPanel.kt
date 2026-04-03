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
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.miracle.ui.settings.mcp.components.McpUiComponents
import com.miracle.ui.settings.mcp.viewmodel.McpStatusViewModel
import java.awt.*
import javax.swing.*

/**
 * MCP 服务状态面板 — 使用与 SkillsPanel 一致的卡片列表风格。
 *
 * 每个 MCP 服务器渲染为一张固定高度的卡片，包含：
 * - LetterIcon 头像
 * - 服务器名称（粗体）、scope 标签、连接状态
 * - 工具列表摘要
 * - 启用/禁用 toggle 与卸载按钮
 * - 卡片之间用 JSeparator 分隔
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
        add(createHeader(), BorderLayout.NORTH)

        stackPanel.isOpaque = false

        // Loading card
        stackPanel.add(JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.empty(24)
            add(loadingLabel, BorderLayout.CENTER)
        }, CARD_LOADING)

        // Content card
        stackPanel.add(JBScrollPane(listPanel).apply {
            border = JBUI.Borders.empty()
            viewportBorder = null
            isOpaque = false
            viewport.isOpaque = false
            horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            verticalScrollBar.unitIncrement = JBUI.scale(16)
        }, CARD_CONTENT)

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

    // ─── Header ────────────────────────────────────────────────────

    private fun createHeader(): JComponent {
        val title = JBLabel("MCP", SwingConstants.LEFT).apply {
            font = JBFont.label().asBold().biggerOn(2f)
        }
        val subtitle = JBLabel("MCP 服务状态与工具管理").apply {
            foreground = JBColor(Color(80, 90, 110), Color(170, 180, 195))
            border = JBUI.Borders.emptyTop(4)
            alignmentX = Component.LEFT_ALIGNMENT
            maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
        }

        val addButton = JButton("+ 新增", AllIcons.General.Add).apply {
            toolTipText = "新增 MCP 服务器配置"
            isOpaque = false
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addActionListener { showAddServerDialog() }
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

    // ─── Add server dialog ────────────────────────────────────────

    private fun showAddServerDialog() {
        val dialog = AddMcpServerDialog(project)
        if (dialog.showAndGet()) {
            val (serverName, config, scope) = dialog.getServerConfig()
            viewModel.addServer(serverName, config, scope)
            refresh(showLoading = false)
        }
    }

    // ─── Server list ───────────────────────────────────────────────

    private fun renderServers(servers: List<McpStatusViewModel.ServerInfo>) {
        listPanel.removeAll()
        if (servers.isEmpty()) {
            listPanel.add(createEmptyState())
        } else {
            servers.forEachIndexed { index, server ->
                if (index != 0) {
                    listPanel.add(JSeparator(SwingConstants.HORIZONTAL).apply {
                        maximumSize = Dimension(Int.MAX_VALUE, 10)
                    })
                }
                listPanel.add(createServerCard(server).apply {
                    alignmentX = Component.LEFT_ALIGNMENT
                })
            }
            listPanel.add(Box.createVerticalStrut(JBUI.scale(12)))
        }
        listPanel.revalidate()
        listPanel.repaint()
    }

    // ─── Server card ───────────────────────────────────────────────

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
        val desc = createDescriptionComponent(descText, disabled)

        // Toggle
        val toggle = McpUiComponents.createServerToggle(server.enabled) { enabled ->
            viewModel.setServerEnabled(server.serverName, enabled)
            refresh(showLoading = false)
        }

        // Uninstall button
        val uninstallButton = JButton(AllIcons.Actions.Uninstall).apply {
            toolTipText = "卸载"
            isOpaque = false
            isContentAreaFilled = false
            border = JBUI.Borders.empty()
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addActionListener {
                viewModel.uninstallServer(server.serverName)
                refresh(showLoading = false)
            }
            preferredSize = Dimension(JBUI.scale(28), JBUI.scale(28))
            maximumSize = preferredSize
        }

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
                add(nameLabel)
                add(Box.createVerticalStrut(JBUI.scale(2)))
                add(metaLabel)
            }, BorderLayout.CENTER)

            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                isOpaque = false
                add(uninstallButton)
                add(Box.createHorizontalStrut(JBUI.scale(6)))
                add(toggle)
            }, BorderLayout.EAST)
        }

        val body = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.emptyTop(8)
            add(desc, BorderLayout.CENTER)
        }

        return JPanel(BorderLayout()).apply {
            isOpaque = true
            background = if (disabled) {
                JBColor(Color(0xF7, 0xF8, 0xFA), Color(0x2B, 0x2D, 0x32))
            } else {
                JBColor.PanelBackground
            }
            border = JBUI.Borders.empty(12)
            minimumSize = Dimension(0, JBUI.scale(CARD_HEIGHT))
            preferredSize = Dimension(Int.MAX_VALUE, JBUI.scale(CARD_HEIGHT))
            maximumSize = Dimension(Int.MAX_VALUE, JBUI.scale(CARD_HEIGHT))
            add(header, BorderLayout.NORTH)
            add(body, BorderLayout.CENTER)
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────

    private fun createDescriptionComponent(text: String, disabled: Boolean): JComponent {
        val normalized = text.trim()
        val shortened = if (normalized.length > 260) normalized.take(260) + "..." else normalized
        return JBTextArea(shortened).apply {
            isEditable = false
            isOpaque = false
            lineWrap = true
            wrapStyleWord = true
            font = JBFont.label()
            foreground = if (disabled) JBColor(Color(140, 145, 155), Color(120, 125, 135))
            else JBColor(Color(50, 60, 80), Color(200, 205, 215))
            border = null
            minimumSize = Dimension(0, preferredSize.height)
            maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
        }
    }

    private fun createEmptyState(): JComponent {
        val label = JBLabel("未检测到任何 MCP 服务器。").apply {
            foreground = JBColor(Color(90, 100, 120), Color(150, 160, 175))
        }
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.empty(16)
            add(label, BorderLayout.WEST)
        }
    }

    companion object {
        private const val CARD_HEIGHT = 130
        private const val CARD_LOADING = "loading"
        private const val CARD_CONTENT = "content"
        private const val CARD_MESSAGE = "message"
    }
}
