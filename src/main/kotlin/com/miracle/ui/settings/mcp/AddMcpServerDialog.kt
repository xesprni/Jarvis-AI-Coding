package com.miracle.ui.settings.mcp

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.miracle.agent.mcp.McpCommandLineParser
import com.miracle.agent.mcp.McpConfigManager
import com.miracle.agent.mcp.McpInstallScope
import com.miracle.agent.mcp.McpServerConfig
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.JPasswordField
import javax.swing.ScrollPaneConstants

/**
 * 新增 MCP 服务器配置的 Dialog 弹窗
 *
 * 提供完整表单字段，支持 stdio / sse / streamableHttp 三种传输类型，
 * 根据类型动态显隐 command / args 和 url 字段，提交时进行必填校验和重名校验。
 */
class AddMcpServerDialog(private val project: Project) : DialogWrapper(project) {

    // ─── 表单控件 ──────────────────────────────────────────────

    private val serverNameField = JBTextField().apply {
        preferredSize = Dimension(JBUI.scale(400), preferredSize.height)
        emptyText.text = "例如: my-mcp-server"
    }

    private val typeComboBox = JComboBox(TYPES).apply {
        preferredSize = Dimension(JBUI.scale(200), preferredSize.height)
        addActionListener { updateFieldVisibility() }
    }

    private val commandField = JBTextField().apply {
        preferredSize = Dimension(JBUI.scale(400), preferredSize.height)
        emptyText.text = "例如: npx 或 /absolute/path/to/server"
    }

    private val argsField = JBTextField().apply {
        preferredSize = Dimension(JBUI.scale(400), preferredSize.height)
        emptyText.text = "可选，例如: -y @modelcontextprotocol/server-filesystem /tmp"
    }

    private val urlField = JBTextField().apply {
        preferredSize = Dimension(JBUI.scale(400), preferredSize.height)
        emptyText.text = "例如: http://localhost:3000/sse"
    }

    private val envTextArea = JBTextArea(3, 40).apply {
        emptyText.text = "可选，每行一个 KEY=VALUE"
        lineWrap = true
        wrapStyleWord = true
    }

    private val tokenField = JPasswordField().apply {
        preferredSize = Dimension(JBUI.scale(400), preferredSize.height)
    }

    private val headersTextArea = JBTextArea(3, 40).apply {
        emptyText.text = "可选，每行一个 KEY=VALUE"
        lineWrap = true
        wrapStyleWord = true
    }

    private val timeoutField = JBTextField().apply {
        preferredSize = Dimension(JBUI.scale(200), preferredSize.height)
        emptyText.text = "可选，超时时间（毫秒）"
    }

    private val displayNameField = JBTextField().apply {
        preferredSize = Dimension(JBUI.scale(400), preferredSize.height)
        emptyText.text = "可选，留空则使用服务器名称"
    }

    private val scopeComboBox = JComboBox(McpInstallScope.entries.toTypedArray()).apply {
        preferredSize = Dimension(JBUI.scale(200), preferredSize.height)
        selectedItem = McpInstallScope.PROJECT
    }

    // 用于动态显隐的包装面板
    private val commandRowPanel = createRowPanel("Command *:", commandField)
    private val argsRowPanel = createRowPanel("Args:", argsField)
    private val urlRowPanel = createRowPanel("URL *:", urlField)

    init {
        title = "新增 MCP 配置"
        init()
        updateFieldVisibility()
    }

    // ─── 布局 ──────────────────────────────────────────────

    override fun createCenterPanel(): JComponent {
        val form = JPanel(GridBagLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.emptyTop(8)
        }

        val c = GridBagConstraints().apply {
            insets = JBUI.insets(4)
            anchor = GridBagConstraints.NORTHWEST
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
        }

        var row = 0
        addRow(form, c, row++, "服务器名称 *:", serverNameField)
        addRow(form, c, row++, "传输类型 *:", typeComboBox)
        addRow(form, c, row++, "显示名称:", displayNameField)
        addRow(form, c, row++, "配置范围 *:", scopeComboBox)

        // stdio 相关行（动态显隐）
        addSpanRow(form, c, row++, commandRowPanel)
        addSpanRow(form, c, row++, argsRowPanel)

        // url 行（sse / streamableHttp 时显示）
        addSpanRow(form, c, row++, urlRowPanel)

        addRow(form, c, row++, "环境变量:", wrapScrollPane(envTextArea))
        addRow(form, c, row++, "Token:", tokenField)
        addRow(form, c, row++, "Headers:", wrapScrollPane(headersTextArea))
        addRow(form, c, row++, "超时(ms):", timeoutField)

        return form
    }

    // ─── 辅助布局方法 ──────────────────────────────────────

    private fun createRowPanel(labelText: String, field: JComponent): JPanel {
        return JPanel(GridBagLayout()).apply {
            isOpaque = false
            val lc = GridBagConstraints().apply {
                insets = JBUI.insets(4)
                anchor = GridBagConstraints.WEST
                weightx = 0.0
            }
            add(JBLabel(labelText), lc)

            val fc = GridBagConstraints().apply {
                insets = JBUI.insets(4)
                fill = GridBagConstraints.HORIZONTAL
                weightx = 1.0
                gridx = 1
            }
            add(field, fc)
        }
    }

    private fun addRow(
        panel: JPanel,
        c: GridBagConstraints,
        row: Int,
        labelText: String,
        field: JComponent
    ) {
        val labelConstraints = (c.clone() as GridBagConstraints).apply {
            gridx = 0
            gridy = row
            weightx = 0.0
            anchor = GridBagConstraints.WEST
        }
        panel.add(JBLabel(labelText), labelConstraints)

        val fieldConstraints = (c.clone() as GridBagConstraints).apply {
            gridx = 1
            gridy = row
            weightx = 1.0
        }
        panel.add(field, fieldConstraints)
    }

    private fun addSpanRow(panel: JPanel, c: GridBagConstraints, row: Int, component: JComponent) {
        val constraints = (c.clone() as GridBagConstraints).apply {
            gridx = 0
            gridy = row
            gridwidth = 2
            weightx = 1.0
        }
        panel.add(component, constraints)
    }

    private fun wrapScrollPane(textArea: JBTextArea): JComponent {
        return JBScrollPane(
            textArea,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        ).apply {
            preferredSize = Dimension(JBUI.scale(400), JBUI.scale(60))
            minimumSize = preferredSize
        }
    }

    // ─── 动态显隐 ─────────────────────────────────────────

    private fun updateFieldVisibility() {
        val selectedType = typeComboBox.selectedItem as? String ?: TYPE_STDIO
        val isStdio = selectedType == TYPE_STDIO
        commandRowPanel.isVisible = isStdio
        argsRowPanel.isVisible = isStdio
        urlRowPanel.isVisible = !isStdio
    }

    // ─── 校验 ──────────────────────────────────────────────

    override fun doValidate(): ValidationInfo? {
        val serverName = serverNameField.text.trim()

        if (serverName.isBlank()) {
            return ValidationInfo("请输入服务器名称", serverNameField)
        }
        if (serverName.contains(" ") || serverName.contains("\t")) {
            return ValidationInfo("服务器名称不能包含空格", serverNameField)
        }
        // 检查重名：读取已合并的配置
        val existingConfig = McpConfigManager.getConfig(project)
        if (existingConfig.servers.containsKey(serverName)) {
            return ValidationInfo("服务器 '$serverName' 已存在", serverNameField)
        }

        val selectedType = typeComboBox.selectedItem as? String ?: TYPE_STDIO

        if (selectedType == TYPE_STDIO) {
            if (commandField.text.trim().isBlank()) {
                return ValidationInfo("stdio 类型必须填写 Command", commandField)
            }
        } else {
            if (urlField.text.trim().isBlank()) {
                return ValidationInfo("${selectedType} 类型必须填写 URL", urlField)
            }
        }

        val timeoutText = timeoutField.text.trim()
        if (timeoutText.isNotBlank()) {
            val timeoutValue = timeoutText.toLongOrNull()
            if (timeoutValue == null || timeoutValue <= 0) {
                return ValidationInfo("超时时间必须是大于 0 的整数", timeoutField)
            }
        }

        // 校验 env 格式
        val envErrors = validateKeyValueLines(envTextArea.text)
        if (envErrors != null) {
            return ValidationInfo("环境变量格式错误：$envErrors", envTextArea)
        }

        // 校验 headers 格式
        val headerErrors = validateKeyValueLines(headersTextArea.text)
        if (headerErrors != null) {
            return ValidationInfo("Headers 格式错误：$headerErrors", headersTextArea)
        }

        return null
    }

    // ─── 数据提取 ─────────────────────────────────────────

    /**
     * 构建并返回表单中填写的 MCP 服务器配置
     *
     * @return 服务器名称、配置和范围的 Triple
     */
    fun getServerConfig(): Triple<String, McpServerConfig, McpInstallScope> {
        val serverName = serverNameField.text.trim()
        val selectedType = typeComboBox.selectedItem as? String ?: TYPE_STDIO

        val argsList = if (selectedType == TYPE_STDIO && argsField.text.trim().isNotBlank()) {
            parseArgs(argsField.text.trim())
        } else {
            emptyList()
        }
        val normalizedStdioCommand = if (selectedType == TYPE_STDIO) {
            McpCommandLineParser.normalize(commandField.text.trim(), argsList)
        } else {
            null
        }

        val envMap = parseKeyValueMap(envTextArea.text)
        val headersMap = parseKeyValueMap(headersTextArea.text)
        val timeoutValue = timeoutField.text.trim().toLongOrNull()

        val displayName = displayNameField.text.trim()

        val config = McpServerConfig(
            type = selectedType,
            command = normalizedStdioCommand?.command ?: "",
            args = normalizedStdioCommand?.args ?: emptyList(),
            url = if (selectedType != TYPE_STDIO) urlField.text.trim() else "",
            env = envMap,
            headers = headersMap,
            token = String(tokenField.password).trim(),
            timeout = timeoutValue,
            name = displayName.ifBlank { null },
        )

        val scope = scopeComboBox.selectedItem as? McpInstallScope ?: McpInstallScope.PROJECT
        return Triple(serverName, config, scope)
    }

    // ─── 解析工具 ─────────────────────────────────────────

    /**
     * 解析空格分隔的参数列表，支持引号包裹含空格的参数
     */
    private fun parseArgs(argsText: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var quoteChar = '"'

        for (ch in argsText) {
            when {
                !inQuotes && (ch == '"' || ch == '\'') -> {
                    inQuotes = true
                    quoteChar = ch
                }
                inQuotes && ch == quoteChar -> {
                    inQuotes = false
                }
                !inQuotes && ch.isWhitespace() -> {
                    if (current.isNotEmpty()) {
                        result.add(current.toString())
                        current.clear()
                    }
                }
                else -> current.append(ch)
            }
        }
        if (current.isNotEmpty()) {
            result.add(current.toString())
        }
        return result
    }

    /**
     * 解析 KEY=VALUE 格式的多行文本为 Map
     */
    private fun parseKeyValueMap(text: String): Map<String, String> {
        return text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() && it.contains("=") }
            .associate { line ->
                val idx = line.indexOf('=')
                line.substring(0, idx).trim() to line.substring(idx + 1).trim()
            }
    }

    /**
     * 校验 KEY=VALUE 格式，格式正确返回 null，错误时返回错误信息
     */
    private fun validateKeyValueLines(text: String): String? {
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
        for ((index, line) in lines.withIndex()) {
            if (!line.contains("=")) {
                return "第 ${index + 1} 行缺少 '=' 分隔符: $line"
            }
            val key = line.substringBefore("=").trim()
            if (key.isBlank()) {
                return "第 ${index + 1} 行 key 为空: $line"
            }
        }
        return null
    }

    companion object {
        private const val TYPE_STDIO = "stdio"
        private const val TYPE_SSE = "sse"
        private const val TYPE_STREAMABLE_HTTP = "streamableHttp"
        private val TYPES = arrayOf(TYPE_STDIO, TYPE_SSE, TYPE_STREAMABLE_HTTP)
    }
}
