package com.miracle.ui.settings.autoapprove

import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.WrapLayout
import com.miracle.config.AutoApproveSettings
import com.miracle.ui.core.ChatTagPaintUtil
import com.miracle.ui.core.createRoundedBorder
import com.miracle.ui.settings.mcp.components.McpUiComponents
import com.miracle.utils.UiUtil
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

internal class AutoApproveSettingsPanel : JPanel(BorderLayout()) {
    private var syncing = false

    private val enableAutoApprove = JBCheckBox("启用自动审批")
    private val masterToggle = McpUiComponents.createServerToggle(AutoApproveSettings.state.enabled) { selected ->
        if (enableAutoApprove.isSelected != selected) {
            enableAutoApprove.isSelected = selected
            handleStateChanged()
        }
    }
    private val stateBadge = JLabel()
    private val maxRequestsSpinner = JSpinner(SpinnerNumberModel(40, 1, 9999, 1)).apply {
        preferredSize = Dimension(JBUI.scale(90), preferredSize.height)
    }

    private val readProjectFiles = JBCheckBox("读取项目文件").apply { isEnabled = false }
    private val readAllFiles = JBCheckBox("读取所有文件")
    private val editProjectFiles = JBCheckBox("编辑项目文件")
    private val executeSafeCommands = JBCheckBox("执行终端安全命令")
    private val executeAllCommands = JBCheckBox("执行终端所有命令")
    private val useMcpServer = JBCheckBox("使用 MCP 服务器")
    private val runTask = JBCheckBox("执行 Task 工具")
    private val runSkill = JBCheckBox("使用 Skill 工具")

    private val blacklistInput = JBTextField().apply {
        emptyText.text = "输入命令前缀后点击添加"
    }
    private val addBlacklistButton = JButton("添加")
    private val restoreDefaultsButton = JButton("恢复默认")
    private val blacklistValues = linkedSetOf<String>()
    private val chipsPanel = JPanel(WrapLayout(FlowLayout.LEFT, JBUI.scale(6), JBUI.scale(6))).apply {
        isOpaque = false
        border = JBUI.Borders.emptyTop(8)
    }
    private val emptyBlacklistLabel = JBLabel("暂无黑名单命令").apply {
        font = JBFont.small()
        foreground = MUTED_FOREGROUND
        border = JBUI.Borders.empty(2, 2, 2, 2)
    }

    init {
        isOpaque = false
        border = JBUI.Borders.empty()
        add(
            JBScrollPane(createContent()).apply {
                border = JBUI.Borders.empty()
                isOpaque = false
                viewport.isOpaque = false
                horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                verticalScrollBar.unitIncrement = JBUI.scale(20)
            },
            BorderLayout.CENTER,
        )
        installListeners()
        resetFromState()
    }

    fun applyChanges() {
        persistState()
    }

    fun resetFromState() {
        syncing = true
        try {
            val state = AutoApproveSettings.state
            val actions = state.actions
            enableAutoApprove.isSelected = state.enabled
            masterToggle.isSelected = state.enabled
            readProjectFiles.isSelected = actions.readFiles
            readAllFiles.isSelected = actions.readFilesExternally
            editProjectFiles.isSelected = actions.editFiles
            executeSafeCommands.isSelected = actions.executeSafeCommands
            executeAllCommands.isSelected = actions.executeAllCommands
            useMcpServer.isSelected = actions.useMcp
            runTask.isSelected = actions.runTask
            runSkill.isSelected = actions.runSkill
            maxRequestsSpinner.value = state.maxRequests
            blacklistValues.clear()
            blacklistValues.addAll(state.autoRunCommandsBlacklist)
            UiUtil.clearTextSafely(blacklistInput)
            refreshBlacklistChips()
            updateVisualState()
        } finally {
            syncing = false
        }
    }

    private fun createContent(): JComponent {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            border = JBUI.Borders.empty(14)

            add(createOverviewCard())
            add(Box.createVerticalStrut(JBUI.scale(12)))
            add(createSettingsCard(
                title = "文件与工具",
                description = "控制 Jarvis 自动通过哪些读写与能力调用。",
                rows = listOf(
                    createOptionRow(readProjectFiles, "允许读取工作区内文件，此项保持开启"),
                    createOptionRow(readAllFiles, "允许读取工作区外的文件"),
                    createOptionRow(editProjectFiles, "允许修改工作区内文件"),
                    createOptionRow(useMcpServer, "允许调用已配置的 MCP 工具"),
                    createOptionRow(runTask, "允许创建 Task 子任务"),
                    createOptionRow(runSkill, "允许调用 Skill 工具"),
                ),
            ))
            add(Box.createVerticalStrut(JBUI.scale(12)))
            add(createSettingsCard(
                title = "命令执行",
                description = "控制终端命令的自动执行范围与安全边界。",
                rows = listOf(
                    createOptionRow(executeSafeCommands, "允许执行只读、安全命令"),
                    createOptionRow(executeAllCommands, "允许执行除黑名单外的所有终端命令"),
                ),
            ))
            add(Box.createVerticalStrut(JBUI.scale(12)))
            add(createBlacklistCard())
        }
    }

    private fun createOverviewCard(): JComponent {
        val iconLabel = JLabel(IconUtil.scale(AllIcons.Actions.Checked, null, 1.2f)).apply {
            preferredSize = JBUI.size(40, 40)
            horizontalAlignment = JLabel.CENTER
            verticalAlignment = JLabel.CENTER
            border = JBUI.Borders.empty(4)
        }

        val titlePanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            add(JBLabel("自动审批").apply {
                font = JBFont.label().asBold().biggerOn(1f)
            })
            add(Box.createVerticalStrut(JBUI.scale(2)))
            add(JBLabel("改动会实时保存到本地配置，无需额外确认。").apply {
                font = JBFont.small()
                foreground = MUTED_FOREGROUND
            })
            add(Box.createVerticalStrut(JBUI.scale(8)))
            add(stateBadge)
        }

        val rightPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            add(JPanel(FlowLayout(FlowLayout.RIGHT, JBUI.scale(8), 0)).apply {
                isOpaque = false
                add(JBLabel("启用").apply { font = JBFont.small() })
                add(masterToggle)
            })
            add(Box.createVerticalStrut(JBUI.scale(10)))
            add(JPanel(FlowLayout(FlowLayout.RIGHT, JBUI.scale(8), 0)).apply {
                isOpaque = false
                add(JBLabel("最大自动批准数").apply {
                    font = JBFont.small()
                    foreground = MUTED_FOREGROUND
                })
                add(maxRequestsSpinner)
            })
        }

        return SettingsCardPanel().apply {
            layout = BorderLayout(JBUI.scale(12), 0)
            add(iconLabel, BorderLayout.WEST)
            add(titlePanel, BorderLayout.CENTER)
            add(rightPanel, BorderLayout.EAST)
            add(JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
                isOpaque = false
                border = JBUI.Borders.emptyTop(12)
                add(restoreDefaultsButton)
            }, BorderLayout.SOUTH)
        }
    }

    private fun createSettingsCard(
        title: String,
        description: String,
        rows: List<JComponent>,
    ): JComponent {
        val content = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            rows.forEachIndexed { index, row ->
                add(row)
                if (index != rows.lastIndex) {
                    add(Box.createVerticalStrut(JBUI.scale(2)))
                }
            }
        }

        return SettingsCardPanel().apply {
            layout = BorderLayout()
            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                add(JBLabel(title).apply {
                    font = JBFont.label().asBold()
                })
                add(Box.createVerticalStrut(JBUI.scale(2)))
                add(JBLabel(description).apply {
                    font = JBFont.small()
                    foreground = MUTED_FOREGROUND
                })
            }, BorderLayout.NORTH)
            add(JPanel(BorderLayout()).apply {
                isOpaque = false
                border = JBUI.Borders.emptyTop(10)
                add(content, BorderLayout.CENTER)
            }, BorderLayout.CENTER)
        }
    }

    private fun createBlacklistCard(): JComponent {
        val inputRow = JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0)).apply {
            isOpaque = false
            add(blacklistInput)
            add(addBlacklistButton)
        }

        return SettingsCardPanel().apply {
            layout = BorderLayout()
            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                add(JBLabel("命令黑名单").apply {
                    font = JBFont.label().asBold()
                })
                add(Box.createVerticalStrut(JBUI.scale(2)))
                add(JBLabel("以下前缀命中的命令即使开启“执行终端所有命令”也不会自动执行。").apply {
                    font = JBFont.small()
                    foreground = MUTED_FOREGROUND
                })
            }, BorderLayout.NORTH)
            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                border = JBUI.Borders.emptyTop(10)
                add(inputRow)
                add(chipsPanel)
            }, BorderLayout.CENTER)
        }
    }

    private fun createOptionRow(checkBox: JBCheckBox, description: String): JComponent {
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.empty(8, 0)
            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                add(checkBox)
                add(Box.createVerticalStrut(JBUI.scale(2)))
                add(JBLabel(description).apply {
                    font = JBFont.small()
                    foreground = MUTED_FOREGROUND
                    border = JBUI.Borders.emptyLeft(24)
                })
            }, BorderLayout.CENTER)
        }
    }

    private fun installListeners() {
        enableAutoApprove.addActionListener {
            if (masterToggle.isSelected != enableAutoApprove.isSelected) {
                masterToggle.isSelected = enableAutoApprove.isSelected
            }
            handleStateChanged()
        }
        maxRequestsSpinner.addChangeListener { handleStateChanged() }
        listOf(
            readAllFiles,
            editProjectFiles,
            executeSafeCommands,
            executeAllCommands,
            useMcpServer,
            runTask,
            runSkill,
        ).forEach { checkBox ->
            checkBox.addActionListener { handleStateChanged() }
        }
        addBlacklistButton.addActionListener { addBlacklistValue() }
        blacklistInput.addActionListener { addBlacklistValue() }
        restoreDefaultsButton.addActionListener { restoreDefaults() }
    }

    private fun handleStateChanged() {
        updateVisualState()
        if (!syncing) {
            persistState()
        }
    }

    private fun updateVisualState() {
        val enabled = enableAutoApprove.isSelected
        stateBadge.text = if (enabled) "当前状态：已启用" else "当前状态：已关闭"
        stateBadge.font = JBFont.small()
        stateBadge.foreground = if (enabled) ENABLED_FOREGROUND else MUTED_FOREGROUND

        readAllFiles.isEnabled = enabled
        editProjectFiles.isEnabled = enabled
        executeSafeCommands.isEnabled = enabled
        executeAllCommands.isEnabled = enabled
        useMcpServer.isEnabled = enabled
        runTask.isEnabled = enabled
        runSkill.isEnabled = enabled
        maxRequestsSpinner.isEnabled = enabled

        val blacklistEnabled = enabled && executeAllCommands.isSelected
        blacklistInput.isEnabled = blacklistEnabled
        addBlacklistButton.isEnabled = blacklistEnabled
        chipsPanel.components.forEach { it.isEnabled = blacklistEnabled }
    }

    private fun addBlacklistValue() {
        val value = blacklistInput.text.trim()
        if (value.isBlank()) return
        if (blacklistValues.add(value)) {
            UiUtil.clearTextSafely(blacklistInput)
            refreshBlacklistChips()
            if (!syncing) {
                persistState()
            }
        } else {
            UiUtil.clearTextSafely(blacklistInput)
        }
    }

    private fun refreshBlacklistChips() {
        chipsPanel.removeAll()
        if (blacklistValues.isEmpty()) {
            chipsPanel.add(emptyBlacklistLabel)
        } else {
            blacklistValues.forEach { value ->
                chipsPanel.add(createBlacklistChip(value))
            }
        }
        chipsPanel.revalidate()
        chipsPanel.repaint()
        updateVisualState()
    }

    private fun createBlacklistChip(value: String): JComponent {
        return object : JPanel() {
            init {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                isOpaque = false
                border = JBUI.Borders.empty(2, 8)
                add(JBLabel(value).apply {
                    font = JBFont.small()
                })
                add(Box.createHorizontalStrut(JBUI.scale(2)))
                add(JButton(AllIcons.Actions.Close).apply {
                    isOpaque = false
                    isContentAreaFilled = false
                    isBorderPainted = false
                    isFocusPainted = false
                    border = JBUI.Borders.empty(0, 4)
                    rolloverIcon = AllIcons.Actions.CloseHovered
                    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    toolTipText = "移除"
                    addActionListener {
                        blacklistValues.remove(value)
                        refreshBlacklistChips()
                        if (!syncing) {
                            persistState()
                        }
                    }
                })
            }

            override fun getPreferredSize(): Dimension = super.getPreferredSize().let { Dimension(it.width, JBUI.scale(24)) }

            override fun paintComponent(g: Graphics) {
                ChatTagPaintUtil.drawRoundedBackground(g, this, selected = true)
                super.paintComponent(g)
            }
        }
    }

    private fun restoreDefaults() {
        syncing = true
        try {
            val defaults = AutoApproveSettings.State()
            val actions = defaults.actions
            enableAutoApprove.isSelected = defaults.enabled
            masterToggle.isSelected = defaults.enabled
            readProjectFiles.isSelected = actions.readFiles
            readAllFiles.isSelected = actions.readFilesExternally
            editProjectFiles.isSelected = actions.editFiles
            executeSafeCommands.isSelected = actions.executeSafeCommands
            executeAllCommands.isSelected = actions.executeAllCommands
            useMcpServer.isSelected = actions.useMcp
            runTask.isSelected = actions.runTask
            runSkill.isSelected = actions.runSkill
            maxRequestsSpinner.value = defaults.maxRequests
            blacklistValues.clear()
            blacklistValues.addAll(defaults.autoRunCommandsBlacklist)
            UiUtil.clearTextSafely(blacklistInput)
            refreshBlacklistChips()
            updateVisualState()
        } finally {
            syncing = false
        }
        persistState()
    }

    private fun persistState() {
        val state = AutoApproveSettings.state
        val actions = state.actions
        state.enabled = enableAutoApprove.isSelected
        state.maxRequests = (maxRequestsSpinner.value as Number).toInt()
        actions.readFilesExternally = readAllFiles.isSelected
        actions.editFiles = editProjectFiles.isSelected
        actions.executeSafeCommands = executeSafeCommands.isSelected
        actions.executeAllCommands = executeAllCommands.isSelected
        actions.useMcp = useMcpServer.isSelected
        actions.runTask = runTask.isSelected
        actions.runSkill = runSkill.isSelected
        state.autoRunCommandsBlacklist.clear()
        state.autoRunCommandsBlacklist.addAll(blacklistValues.toList())
    }

    private class SettingsCardPanel : JPanel() {
        init {
            isOpaque = false
            border = BorderFactory.createCompoundBorder(
                createRoundedBorder(CARD_BORDER),
                JBUI.Borders.empty(14),
            )
        }

        override fun paintComponent(g: Graphics) {
            val g2 = g.create() as? Graphics2D ?: return
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.color = CARD_BACKGROUND
                g2.fillRoundRect(0, 0, width - 1, height - 1, JBUI.scale(12), JBUI.scale(12))
            } finally {
                g2.dispose()
            }
            super.paintComponent(g)
        }
    }

    companion object {
        private val MUTED_FOREGROUND = JBColor(Color(110, 118, 132), Color(150, 160, 175))
        private val ENABLED_FOREGROUND = JBColor(Color(0x2E, 0x7D, 0x32), Color(0x81, 0xC7, 0x84))
        private val CARD_BACKGROUND = JBColor(Color(252, 253, 255), Color(52, 54, 58))
        private val CARD_BORDER = JBColor(Color(220, 226, 234), Color(72, 75, 80))
    }
}
