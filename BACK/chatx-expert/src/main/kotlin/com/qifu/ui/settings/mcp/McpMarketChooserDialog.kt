package com.qifu.ui.settings.mcp

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.qifu.agent.mcp.McpInstallScope
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*

/**
 * MCP 市场选择对话框
 * 
 * 提供 MCP 服务的浏览和安装功能：
 * - 显示可用的 MCP 服务列表
 * - 支持安装参数配置
 * - 安装完成后刷新工具列表
 * 
 * @param project 当前项目
 * @param onToolsRefresh 安装完成后刷新工具的回调，参数为服务名称
 */
@Suppress("DialogTitleCapitalization")
class McpMarketChooserDialog(
    private val project: Project,
    private val onToolsRefresh: (serverName: String?) -> Unit
) : DialogWrapper(project, true) {

    private val cardLayout = CardLayout()
    private val cards = JPanel(cardLayout).apply { isOpaque = false }
    private val installContainer = JPanel(BorderLayout()).apply { isOpaque = false }
    private val headerLabel = JBLabel("MCP 市场").apply {
        font = JBFont.label().asBold().biggerOn(1f)
    }
    private val backButton = createToolbarButton("返回") { showMarket() }.apply {
        isVisible = false
    }
    private val marketPanel = McpMarketPanel(
        project,
        installHandler = { request -> showInstallForm(request) },
        onInstalled = { serverName ->
            onToolsRefresh(serverName)
            close(OK_EXIT_CODE)
        },
        showTitle = false,
        hideInstalled = false
    )
    private var pendingCancel: (() -> Unit)? = null

    init {
        title = "从 MCP 市场添加"
        init()
        marketPanel.refresh()
    }

    override fun createCenterPanel(): JComponent {
        val header = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.empty(4, 0, 8, 0)
            add(JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0)).apply {
                isOpaque = false
                add(backButton)
                add(headerLabel)
            }, BorderLayout.WEST)
        }
        cards.add(JPanel(BorderLayout()).apply {
            isOpaque = false
            add(marketPanel, BorderLayout.CENTER)
        }, CARD_MARKET)
        cards.add(installContainer, CARD_INSTALL)
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.empty(8, 12, 12, 12)
            preferredSize = JBUI.size(640, 500)
            add(header, BorderLayout.NORTH)
            add(cards, BorderLayout.CENTER)
        }
    }

    override fun createActions(): Array<Action> = arrayOf(cancelAction)

    override fun doCancelAction() {
        pendingCancel?.invoke()
        pendingCancel = null
        super.doCancelAction()
    }

    private fun showMarket() {
        pendingCancel?.invoke()
        pendingCancel = null
        headerLabel.text = "MCP 市场"
        backButton.isVisible = false
        cardLayout.show(cards, CARD_MARKET)
    }

    private fun showInstallForm(request: McpMarketPanel.InstallRequest) {
        pendingCancel?.invoke()
        pendingCancel = request.onCancel
        headerLabel.text = "安装 ${request.service.name ?: request.service.mcpId}"
        backButton.isVisible = true
        installContainer.removeAll()
        installContainer.add(buildInstallForm(request), BorderLayout.CENTER)
        installContainer.revalidate()
        installContainer.repaint()
        cardLayout.show(cards, CARD_INSTALL)
    }

    private fun buildInstallForm(request: McpMarketPanel.InstallRequest): JComponent {
        val scopeButtons = McpInstallScope.entries.associateWith { scope ->
            JRadioButton(scope.displayName).apply { isSelected = scope == McpInstallScope.GLOBAL }
        }
        ButtonGroup().apply {
            scopeButtons.values.forEach { add(it) }
        }
        val parameterFields = request.parameterDescriptors.map { descriptor ->
            descriptor to JBTextField().apply {
                emptyText.text = descriptor.description ?: "请输入 ${descriptor.name}"
                maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
            }
        }
        lateinit var installButton: JButton
        installButton = createToolbarButton("安装") {
            val selectedScope = scopeButtons.entries.firstOrNull { it.value.isSelected }?.key
                ?: McpInstallScope.GLOBAL
            val params = buildMap {
                parameterFields.forEach { (descriptor, field) ->
                    put(descriptor.name, field.text.trim())
                }
            }
            installButton.isEnabled = false
            request.onConfirm(McpInstallDialogResult(selectedScope, params))
        }

        val form = JPanel()
        form.layout = BoxLayout(form, BoxLayout.Y_AXIS)
        form.isOpaque = false
        form.border = JBUI.Borders.empty(4, 8, 8, 8)
        form.alignmentX = Component.LEFT_ALIGNMENT
        form.maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
        val descText = (request.service.description ?: "填写安装信息").trim()
        val truncatedDesc = if (descText.length > 220) descText.take(220).trimEnd() + "..." else descText
        val wrappedDesc = "<html><div style='width:${JBUI.scale(420)}px;'>${truncatedDesc.replace("\n", "<br/>")}</div></html>"
        form.add(JBLabel(wrappedDesc).apply {
            font = JBFont.small()
            foreground = JBColor.GRAY
            border = JBUI.Borders.emptyBottom(8)
            toolTipText = descText
            alignmentX = Component.LEFT_ALIGNMENT
            maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
        })
        form.add(JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0)).apply {
            isOpaque = false
            add(JBLabel("安装范围："))
            scopeButtons.values.forEach { add(it) }
            maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
        })
        if (parameterFields.isNotEmpty()) {
            form.add(Box.createVerticalStrut(JBUI.scale(8)))
            parameterFields.forEach { (descriptor, field) ->
                form.add(JPanel(BorderLayout()).apply {
                    isOpaque = false
                    border = JBUI.Borders.emptyBottom(6)
                    add(JBLabel("${descriptor.name}："), BorderLayout.NORTH)
                    add(field, BorderLayout.CENTER)
                    maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
                })
            }
        } else {
            form.add(Box.createVerticalStrut(JBUI.scale(8)))
            form.add(JBLabel("该服务无需额外参数").apply {
                foreground = JBColor.GRAY
                font = JBFont.small()
                alignmentX = Component.LEFT_ALIGNMENT
                maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
            })
        }
        form.add(Box.createVerticalStrut(JBUI.scale(12)))
        form.add(JPanel(FlowLayout(FlowLayout.RIGHT, JBUI.scale(8), 0)).apply {
            isOpaque = false
            add(installButton)
            maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
        })

        return JBScrollPane(form).apply {
            border = JBUI.Borders.empty()
            viewportBorder = null
            isOpaque = false
            viewport.isOpaque = false
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        }
    }

    private fun createToolbarButton(text: String, action: () -> Unit): JButton {
        return JButton(text).apply {
            putClientProperty("JButton.backgroundColor", JBColor.PanelBackground)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addActionListener { action() }
        }
    }

    companion object {
        private const val CARD_MARKET = "market"
        private const val CARD_INSTALL = "install"
    }
}
