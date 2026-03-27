package com.qifu.ui.settings.mcp

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.qifu.external.McpMarketService
import com.qifu.external.McpMarketServiceTemplate
import com.qifu.ui.settings.mcp.components.BaseCardLayoutPanel
import com.qifu.ui.settings.mcp.components.McpUiComponents
import com.qifu.ui.settings.mcp.viewmodel.McpMarketViewModel
import com.qihoo.finance.lowcode.common.util.Icons
import java.awt.*
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.*
import javax.swing.border.Border
import javax.swing.event.DocumentEvent

/**
 * MCP 市场面板，展示可用的 MCP 服务并支持搜索与快速安装。
 *
 * 该面板采用 CardLayout 实现多状态切换（加载中、内容展示、错误信息），
 * 提供搜索过滤功能和服务卡片列表展示。
 */
@Suppress("DialogTitleCapitalization")
class McpMarketPanel(
    private val project: Project,
    private val installHandler: InstallHandler? = null,
    private val onInstalled: ((String) -> Unit)? = null,
    private val showTitle: Boolean = true,
    private val hideInstalled: Boolean = false,
) : BaseCardLayoutPanel() {

    private val log = Logger.getInstance(McpMarketPanel::class.java)
    private val viewModel = McpMarketViewModel(project, hideInstalled)

    private val searchDefaultBorder: Border? = JBUI.Borders.compound(
        JBUI.Borders.empty(4, 10),
        JBUI.Borders.customLine(JBColor.border(), 1, 1, 1, 1)
    )

    private val searchFocusBorder: Border? = JBUI.Borders.compound(
        JBUI.Borders.empty(4, 10),
        JBUI.Borders.customLine(JBColor(Color(0x47, 0x8B, 0xFF), Color(0x57, 0x6D, 0xC8)), 1, 1, 1, 1)
    )

    private val searchField = JBTextField().apply {
        emptyText.text = "搜索 MCP 服务"
        border = searchDefaultBorder
        maximumSize = Dimension(Int.MAX_VALUE, JBUI.scale(32))
        preferredSize = Dimension(JBUI.scale(240), JBUI.scale(32))
        minimumSize = Dimension(JBUI.scale(160), JBUI.scale(32))
    }

    private val cardsContainer = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = false
    }

    private val scrollPane = JBScrollPane(cardsContainer).apply {
        border = JBUI.Borders.empty()
        viewportBorder = null
        isOpaque = false
        viewport.isOpaque = false
        horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        verticalScrollBar.unitIncrement = JBUI.scale(16)
    }

    init {
        isOpaque = false
        border = JBUI.Borders.emptyTop(8)
        initCardLayout()

        // 设置 ViewModel 状态监听
        viewModel.setStateListener { state ->
            when (state) {
                is McpMarketViewModel.UiState.Loading -> showLoading("正在加载 MCP 市场...")
                is McpMarketViewModel.UiState.Content -> {
                    renderServices(state.services)
                    showContent()
                }
                is McpMarketViewModel.UiState.Error -> showMessage(state.message)
                is McpMarketViewModel.UiState.Empty -> renderEmptyData()
                is McpMarketViewModel.UiState.EmptySearch -> renderEmptySearch(state.keyword)
            }
        }

        // 监听搜索框文本变化
        searchField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                viewModel.search(searchField.text ?: "")
            }
        })

        // 监听搜索框焦点变化
        searchField.addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent?) {
                searchField.border = searchFocusBorder
            }
            override fun focusLost(e: FocusEvent?) {
                searchField.border = searchDefaultBorder
            }
        })
    }

    override fun createContentPanel(): JComponent {
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.emptyTop(4)
            add(createSearchPanel(), BorderLayout.NORTH)
            add(scrollPane, BorderLayout.CENTER)
        }
    }

    override fun wrapCentered(component: JComponent): JPanel {
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.empty(36, 12)
            add(component, BorderLayout.CENTER)
        }
    }

    /**
     * 刷新 MCP 市场数据
     */
    fun refresh() {
        viewModel.refresh()
    }

    private fun createSearchPanel(): JComponent {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            border = JBUI.Borders.emptyBottom(12)

            if (showTitle) {
                add(JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0)).apply {
                    isOpaque = false
                    add(JBLabel("MCP 市场").apply {
                        font = JBFont.label().asBold().biggerOn(2f)
                    })
                })
            }

            add(JPanel(BorderLayout()).apply {
                isOpaque = false
                border = JBUI.Borders.emptyTop(8)
                add(searchField, BorderLayout.CENTER)
            })
        }
    }

    private fun renderServices(services: List<McpMarketViewModel.ServiceDisplayInfo>) {
        cardsContainer.removeAll()

        services.forEachIndexed { index, info ->
            if (index != 0) {
                cardsContainer.add(JSeparator(SwingConstants.HORIZONTAL))
            }
            cardsContainer.add(ServiceCard(info.service, info.isInstalled).apply {
                alignmentX = LEFT_ALIGNMENT
            })
        }

        cardsContainer.add(Box.createVerticalStrut(JBUI.scale(12)))
        cardsContainer.revalidate()
        cardsContainer.repaint()
    }

    private fun renderEmptySearch(keyword: String?) {
        cardsContainer.removeAll()
        cardsContainer.add(Box.createVerticalGlue())
        cardsContainer.add(JBLabel(
            if (keyword.isNullOrEmpty()) "未找到任何 MCP 服务" else "未找到与 \"$keyword\" 匹配的 MCP 服务",
            SwingConstants.CENTER
        ).apply {
            font = JBFont.label().biggerOn(0.5f)
            foreground = JBColor.GRAY
            alignmentX = CENTER_ALIGNMENT
            border = JBUI.Borders.empty(JBUI.scale(12))
        })
        cardsContainer.add(Box.createVerticalGlue())
        cardsContainer.revalidate()
        cardsContainer.repaint()
        showContent()
    }

    private fun renderEmptyData() {
        cardsContainer.removeAll()
        cardsContainer.add(Box.createVerticalGlue())
        cardsContainer.add(JBLabel("市场暂无可用的 MCP 服务", SwingConstants.CENTER).apply {
            font = JBFont.label().biggerOn(0.5f)
            foreground = JBColor.GRAY
            alignmentX = CENTER_ALIGNMENT
            border = JBUI.Borders.empty(JBUI.scale(12))
        })
        cardsContainer.add(Box.createVerticalGlue())
        cardsContainer.revalidate()
        cardsContainer.repaint()
        showContent()
    }

    /**
     * MCP 服务卡片组件
     */
    private inner class ServiceCard(
        private val service: McpMarketService,
        private val isInstalled: Boolean
    ) : JPanel(BorderLayout()) {

        private val fallbackIcon = McpUiComponents.LetterIcon(service.name, JBUI.scale(32))
        private val activeButtonBorderColor = JBColor(Color(0x47, 0x8B, 0xFF), Color(0x57, 0x6D, 0xC8))
        private val disabledButtonBorderColor = JBColor(Color(0xC4, 0xC9, 0xD6), Color(0x4C, 0x4F, 0x58))

        private lateinit var installButton: JButton
        private var isInstalling = false

        init {
            isOpaque = false
            border = JBUI.Borders.empty(16)
            minimumSize = Dimension(0, JBUI.scale(SERVICE_HEIGHT))
            preferredSize = Dimension(Int.MAX_VALUE, JBUI.scale(SERVICE_HEIGHT))
            maximumSize = Dimension(Int.MAX_VALUE, JBUI.scale(SERVICE_HEIGHT))

            add(createHeader(), BorderLayout.NORTH)
            add(createBody(), BorderLayout.CENTER)
        }

        private fun createHeader(): JComponent {
            return JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                isOpaque = false

                add(createIconLabel().also { it.alignmentY = CENTER_ALIGNMENT })
                add(Box.createHorizontalStrut(JBUI.scale(12)))

                add(JBLabel(ellipsize(service.name ?: service.mcpId, 24)).apply {
                    font = JBFont.label().asBold().biggerOn(1.5f)
                    foreground = JBColor.foreground()
                    toolTipText = service.name ?: service.mcpId
                    alignmentY = CENTER_ALIGNMENT
                })

                add(Box.createHorizontalGlue())

                installButton = JButton(if (isInstalled) "已安装" else "安装").apply {
                    val borderColor = if (isInstalled) disabledButtonBorderColor else activeButtonBorderColor
                    isOpaque = false
                    putClientProperty("JButton.buttonType", "roundRect")
                    border = JBUI.Borders.compound(
                        JBUI.Borders.customLine(borderColor, 1),
                        JBUI.Borders.empty(4, 14)
                    )
                    font = JBFont.label().biggerOn(0.5f)
                    cursor = if (isInstalled) Cursor.getDefaultCursor() else Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    isFocusable = false
                    alignmentY = CENTER_ALIGNMENT
                    isEnabled = !isInstalled
                    foreground = if (isInstalled) JBColor.GRAY else JBColor.foreground()
                    if (isEnabled) {
                        addActionListener {
                            if (!isInstalling) {
                                performInstall()
                            }
                        }
                    }
                }
                add(installButton)
            }
        }

        private fun performInstall() {
            isInstalling = true
            installButton.text = "准备中..."
            installButton.isEnabled = false
            installButton.cursor = Cursor.getDefaultCursor()

            viewModel.prepareInstall(
                service = service,
                onSuccess = { request ->
                    val handler = installHandler
                    if (handler != null) {
                        val externalRequest = InstallRequest(
                            service = request.service,
                            serverTemplate = request.serverTemplate,
                            parameterDescriptors = request.parameterDescriptors,
                            onConfirm = { dialogResult ->
                                // 使用 Panel 内部的 executeInstall，确保触发 onInstalled
                                executeInstall(request.serverTemplate, dialogResult)
                            },
                            onCancel = { resetInstallButton() },
                            onError = { handleInstallFailure(it) }
                        )
                        handler.onInstallRequested(externalRequest)
                        return@prepareInstall
                    }

                    val dialog = McpInstallDialog(
                        project,
                        service.name ?: service.mcpId,
                        request.parameterDescriptors
                    )
                    if (!dialog.showAndGet()) {
                        resetInstallButton()
                        return@prepareInstall
                    }
                    val dialogResult = dialog.getResult()
                    if (dialogResult == null) {
                        resetInstallButton()
                        return@prepareInstall
                    }
                    executeInstall(request.serverTemplate, dialogResult)
                },
                onError = { handleInstallFailure(it) }
            )
        }

        private fun executeInstall(
            serverTemplate: McpMarketServiceTemplate,
            dialogResult: McpInstallDialogResult
        ) {
            installButton.text = "安装中..."
            viewModel.executeInstall(
                service = service,
                serverTemplate = serverTemplate,
                dialogResult = dialogResult,
                onSuccess = {
                    isInstalling = false
                    installButton.text = "已安装"
                    installButton.isEnabled = false
                    installButton.cursor = Cursor.getDefaultCursor()
                    onInstalled?.invoke(service.mcpId)
                },
                onError = { handleInstallFailure(it) }
            )
        }

        private fun resetInstallButton() {
            isInstalling = false
            installButton.text = "安装"
            installButton.isEnabled = true
            installButton.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        }

        private fun handleInstallFailure(error: Exception) {
            log.warn("Failed to install MCP service: ${service.mcpId}", error)
            ApplicationManager.getApplication().invokeLater({
                resetInstallButton()
                showMessage("安装失败：${error.message ?: "未知错误"}")
            }, ModalityState.stateForComponent(this@McpMarketPanel))
        }

        private fun createIconLabel(): JLabel {
            val label = JBLabel().apply {
                horizontalAlignment = SwingConstants.CENTER
                verticalAlignment = SwingConstants.CENTER
                icon = fallbackIcon
                preferredSize = Dimension(JBUI.scale(40), JBUI.scale(40))
                minimumSize = Dimension(JBUI.scale(40), JBUI.scale(40))
                maximumSize = Dimension(JBUI.scale(40), JBUI.scale(40))
            }
            service.logoUrl?.takeIf { it.isNotBlank() }?.let { logo ->
                Icons.asyncSetUrlIcon(label, logo, fallbackIcon, JBUI.scale(32))
            }
            return label
        }

        private fun createBody(): JComponent {
            return JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                border = JBUI.Borders.emptyTop(12)

                service.description?.trim()?.takeIf { it.isNotEmpty() }?.let { desc ->
                    add(createDescription(desc))
                    add(Box.createVerticalStrut(JBUI.scale(8)))
                }

                val metaText = buildMetaText()
                if (metaText.isNotEmpty()) {
                    add(JBLabel(metaText).apply {
                        font = JBFont.small()
                        foreground = JBColor.GRAY
                    })
                    add(Box.createVerticalStrut(JBUI.scale(8)))
                }

                val tags = service.tags?.filter { it.isNotBlank() }.orEmpty()
                if (tags.isNotEmpty()) {
                    add(createTagPanel(tags))
                } else {
                    add(Box.createVerticalGlue())
                }
            }
        }

        private fun createDescription(text: String): JComponent {
            val normalized = text.replace("\r\n", "\n").trim()
            val truncated = ellipsize(normalized, 150)
            return JBLabel("<html>${escapeHtml(truncated).replace("\n", "<br/>")}</html>").apply {
                font = JBFont.label()
                foreground = JBColor.DARK_GRAY
                toolTipText = normalized
                alignmentX = LEFT_ALIGNMENT
            }
        }

        private fun createTagPanel(tags: List<String>): JComponent {
            return JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(6), JBUI.scale(6))).apply {
                isOpaque = false
                alignmentX = LEFT_ALIGNMENT
                tags.take(4).forEach { tag ->
                    add(McpUiComponents.createMcpLabel(tag))
                }
            }
        }

        private fun buildMetaText(): String {
            val parts = mutableListOf<String>()
            service.category?.takeIf { it.isNotBlank() }?.let { parts.add("分类：$it") }
            service.orgName?.takeIf { it.isNotBlank() }?.let { parts.add("提供方：$it") }
            service.downloadCount?.takeIf { it > 0 }?.let { parts.add("安装量：$it") }
            if (service.isRecommended == true) {
                parts.add("推荐")
            }
            return parts.joinToString("  |  ")
        }

        private fun ellipsize(value: String, limit: Int): String {
            return StringUtil.shortenTextWithEllipsis(value, limit, 0, true)
        }

        private fun escapeHtml(value: String): String {
            return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
        }
    }

    companion object {
        private const val SERVICE_HEIGHT = 200

        fun createMcpLabel(tag: String): JLabel = McpUiComponents.createMcpLabel(tag)
    }

    fun interface InstallHandler {
        fun onInstallRequested(request: InstallRequest)
    }

    data class InstallRequest(
        val service: McpMarketService,
        val serverTemplate: McpMarketServiceTemplate,
        val parameterDescriptors: List<McpInstallParameterDescriptor>,
        val onConfirm: (McpInstallDialogResult) -> Unit,
        val onCancel: () -> Unit,
        val onError: (Exception) -> Unit
    )
}
