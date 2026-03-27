package com.miracle.ui.settings.agent

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.miracle.agent.mcp.McpClientHub
import com.miracle.agent.mcp.McpConfigManager
import com.miracle.agent.tool.ToolRegistry
import com.miracle.utils.AgentConfig
import com.miracle.utils.getProjectConfigDirectory
import com.miracle.utils.getUserConfigDirectory
import kotlinx.coroutines.runBlocking
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.FlowLayout
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.*

/**
 * 智能体详情/编辑面板
 *
 * 提供智能体的创建和编辑功能，包括：
 * - 基本信息编辑（标识、使用场景、系统提示词）
 * - 工具权限配置
 * - 保存范围选择（全局/项目级别）
 * - AI 智能生成功能
 *
 * @param project 当前项目
 * @param agent 要编辑的智能体，为 null 时表示新建
 * @param existingAgentsProvider 获取已存在智能体列表的函数，用于名称冲突检测
 * @param onSaved 保存成功后的回调
 * @param onClose 关闭面板的回调
 */
@Suppress("DialogTitleCapitalization")
class AgentDetailPanel(
    private val project: Project,
    private val agent: AgentConfig?,
    private val existingAgentsProvider: () -> List<AgentConfig>,
    private val onSaved: () -> Unit,
    private val onClose: () -> Unit
) : JPanel(BorderLayout()) {

    private val log = Logger.getInstance(AgentDetailPanel::class.java)

    // ========== 表单控件 ==========
    /** 智能体标识输入框 */
    private val nameField = JBTextField()
    /** 使用场景描述输入框 */
    private val descArea = JBTextArea(3, 20)
    /** 系统提示词输入框 */
    private val promptArea = JBTextArea(10, 20)

    // ========== 保存范围选择 ==========
    private val scopeUserRadio = JRadioButton("全局 (~/.jarvis/agents)")
    private val scopeProjectRadio = JRadioButton("项目 (.jarvis/agents)", true)
    private val scopeGroup = ButtonGroup()

    // ========== 工具权限选择 ==========
    /** 全选工具复选框 */
    private val selectAllCheck = JCheckBox("全部工具 (*)", true)
    /** 各个工具的复选框列表 */
    private val toolChecks = mutableListOf<JCheckBox>()
    private val toolListPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = false
    }

    /** 状态提示标签，显示保存结果等信息 */
    private val statusLabel = JBLabel().apply {
        font = JBFont.small()
        foreground = JBColor.GRAY
    }

    /** 原始表单状态，用于检测是否有未保存的更改 */
    private var originalState: AgentFormState

    init {
        isOpaque = false
        border = JBUI.Borders.empty(12)
        scopeGroup.add(scopeUserRadio)
        scopeGroup.add(scopeProjectRadio)

        val content = JPanel()
        content.layout = BoxLayout(content, BoxLayout.Y_AXIS)
        content.isOpaque = false
        descArea.lineWrap = true
        descArea.wrapStyleWord = true
        promptArea.lineWrap = true
        promptArea.wrapStyleWord = true

        content.add(createTopActions())
        content.add(labeled("名称", nameField))
        content.add(labeled("描述(何时使用)", JBScrollPane(descArea).apply {
            preferredSize = JBUI.size(200, 80)
            border = JBUI.Borders.customLine(JBColor.border(), 1)
        }))
        content.add(labeled("生效范围", createScopeRadios()))
        content.add(labeled("提示词", JBScrollPane(promptArea).apply {
            preferredSize = JBUI.size(200, 180)
            border = JBUI.Borders.customLine(JBColor.border(), 1)
        }))
        content.add(createToolsSection())
        content.add(Box.createVerticalStrut(JBUI.scale(8)))
        content.add(createFooter())
        content.add(Box.createVerticalStrut(JBUI.scale(4)))
        content.add(statusLabel)

        add(content, BorderLayout.CENTER)

        if (agent != null) {
            loadAgent(agent)
        } else {
            setToolsSelection(listOf("*"))
            setScopeSelection(SaveScope.PROJECT)
        }
        originalState = currentState()
        setEditable(agent?.scope != AgentConfig.Scope.BUILT_IN)
    }

    /**
     * 尝试关闭面板
     * 如果有未保存的更改，弹出确认对话框
     *
     * @param onClosed 确认关闭后执行的回调
     */
    fun tryClose(onClosed: () -> Unit) {
        if (!isDirty()) {
            onClosed()
            return
        }
        val res = Messages.showYesNoDialog(
            project,
            "有未保存的更改，确定关闭吗？",
            "关闭标签",
            "关闭",
            "取消",
            null
        )
        if (res == Messages.YES) onClosed()
    }

    fun refreshToolsAfterMcpInstall(serverName: String?) {
        ApplicationManager.getApplication().executeOnPooledThread {
            waitForMcpConnection(serverName)
            refreshToolsFromRegistry(preserveSelection = false, forceSelectAll = true)
        }
    }

    // ========== 私有方法 ==========

    private fun labeled(label: String, component: JComponent): JComponent {
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.emptyBottom(10)
            add(JBLabel(label).apply { border = JBUI.Borders.emptyBottom(4) }, BorderLayout.NORTH)
            add(component, BorderLayout.CENTER)
        }
    }

    private fun createScopeRadios(): JComponent {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            add(scopeProjectRadio)
            add(Box.createHorizontalStrut(JBUI.scale(12)))
            add(scopeUserRadio)
        }
    }

    private fun createToolsSection(): JComponent {
        val header = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.emptyBottom(4)
            add(JBLabel("工具权限"), BorderLayout.WEST)
            add(createToolbarButton("刷新 MCP 工具") {
                refreshToolsFromRegistry(preserveSelection = true, forceSelectAll = false)
            }, BorderLayout.EAST)
        }
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.emptyBottom(10)
            add(header, BorderLayout.NORTH)
            add(createToolsSelector(), BorderLayout.CENTER)
        }
    }

    private fun createToolsSelector(): JComponent {
        populateToolChecks()

        selectAllCheck.addActionListener {
            val selected = selectAllCheck.isSelected
            toolChecks.forEach { it.isSelected = selected }
        }

        val scroll = JBScrollPane(toolListPanel).apply {
            border = JBUI.Borders.customLine(JBColor.border(), 1)
            viewportBorder = null
            preferredSize = JBUI.size(240, 160)
        }

        return JPanel(BorderLayout()).apply {
            isOpaque = false
            add(selectAllCheck, BorderLayout.NORTH)
            add(scroll, BorderLayout.CENTER)
        }
    }

    private fun populateToolChecks(preservedSelection: Set<String>? = null) {
        val keepSelected = preservedSelection ?: currentSelectedToolNames()
        toolChecks.clear()
        toolListPanel.removeAll()
        ToolRegistry.getAll().keys.sorted().forEach { toolName ->
            val check = JCheckBox(toolName, selectAllCheck.isSelected || keepSelected.contains(toolName))
            check.addActionListener {
                if (!check.isSelected) {
                    selectAllCheck.isSelected = false
                } else if (toolChecks.all { it.isSelected }) {
                    selectAllCheck.isSelected = true
                }
            }
            toolChecks += check
            toolListPanel.add(check)
        }
        if (selectAllCheck.isSelected) {
            toolChecks.forEach { it.isSelected = true }
        }
        selectAllCheck.isSelected = toolChecks.all { it.isSelected }
        toolListPanel.revalidate()
        toolListPanel.repaint()
    }

    private fun createTopActions(): JComponent =
        JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0)).apply {
            isOpaque = false
            border = JBUI.Borders.emptyBottom(8)
            add(createToolbarButton("智能生成") { openSmartGenerationDialog() })
        }

    private fun createFooter(): JComponent {
        return JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0)).apply {
            isOpaque = false
            add(createToolbarButton("保存") { saveAgent() })
        }
    }

    private fun createToolbarButton(text: String, action: () -> Unit): JButton {
        return JButton(text).apply {
            putClientProperty("JButton.backgroundColor", JBColor.PanelBackground)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addActionListener { action() }
        }
    }

    private fun loadAgent(agent: AgentConfig) {
        nameField.text = agent.agentType
        descArea.text = agent.whenToUse
        when (val tools = agent.tools) {
            is String -> setToolsSelection(if (tools.trim() == "*") listOf("*") else listOf(tools))
            is List<*> -> setToolsSelection(tools.filterIsInstance<String>())
            else -> setToolsSelection(emptyList())
        }
        promptArea.text = agent.systemPrompt
        setScopeSelection(
            when (agent.scope) {
                AgentConfig.Scope.PROJECT -> SaveScope.PROJECT
                else -> SaveScope.USER
            }
        )
        if (agent.scope == AgentConfig.Scope.BUILT_IN) {
            statusLabel.foreground = JBColor.GRAY
            statusLabel.text = "内置智能体仅供查看"
        }
    }

    private fun setEditable(enabled: Boolean) {
        nameField.isEditable = enabled
        descArea.isEditable = enabled
        promptArea.isEditable = enabled
        scopeUserRadio.isEnabled = enabled
        scopeProjectRadio.isEnabled = enabled
        selectAllCheck.isEnabled = enabled
        toolChecks.forEach { it.isEnabled = enabled }
    }

    private fun setToolsSelection(tools: List<String>) {
        if (tools.any { it == "*" }) {
            selectAllCheck.isSelected = true
            toolChecks.forEach { it.isSelected = true }
            return
        }
        val wanted = tools.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        toolChecks.forEach { check ->
            check.isSelected = wanted.isEmpty() || wanted.contains(check.text)
        }
        selectAllCheck.isSelected = toolChecks.all { it.isSelected }
    }

    private fun selectedToolsForSave(): Any {
        val selectedNames = toolChecks.filter { it.isSelected }.map { it.text }
        return if (selectAllCheck.isSelected || selectedNames.size == toolChecks.size || selectedNames.isEmpty()) {
            "*"
        } else {
            selectedNames
        }
    }


    private fun currentSelectedToolNames(): Set<String> =
        toolChecks.filter { it.isSelected }.map { it.text }.toSet()

    private fun setScopeSelection(scope: SaveScope) {
        when (scope) {
            SaveScope.USER -> scopeUserRadio.isSelected = true
            SaveScope.PROJECT -> scopeProjectRadio.isSelected = true
        }
    }

    private fun getSelectedScope(): SaveScope =
        if (scopeProjectRadio.isSelected) SaveScope.PROJECT else SaveScope.USER

    @Suppress("SameParameterValue")
    private fun refreshToolsFromRegistry(
        preserveSelection: Boolean = true,
        forceSelectAll: Boolean = false
    ) {
        val preservedSelection = if (preserveSelection) currentSelectedToolNames() else emptySet()
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                McpConfigManager.invalidate(project)
                McpConfigManager.loadConfig(project)
                McpClientHub.getInstance(project).ensureInitialized()
                McpClientHub.instantiate()
            } catch (e: Exception) {
                log.warn("Failed to refresh tools after MCP install", e)
            } finally {
                ApplicationManager.getApplication().invokeLater {
                    if (forceSelectAll) {
                        selectAllCheck.isSelected = true
                    }
                    populateToolChecks(preservedSelection)
                    if (forceSelectAll) {
                        selectAllCheck.isSelected = true
                        toolChecks.forEach { it.isSelected = true }
                    }
                }
            }
        }
    }

    private fun waitForMcpConnection(serverName: String?) {
        if (serverName.isNullOrBlank()) return
        val hub = McpClientHub.getInstance(project)
        hub.ensureInitialized()
        repeat(5) {
            try {
                val tools = runBlocking { hub.getServerTools(serverName) }
                if (tools.isNotEmpty()) {
                    return
                }
            } catch (_: Exception) {
                // ignore and retry
            }
            try {
                Thread.sleep(400)
            } catch (_: InterruptedException) {
                return
            }
        }
    }

    private fun openSmartGenerationDialog() {
        val inputArea = JBTextArea(6, 48).apply {
            lineWrap = true
            wrapStyleWord = true
            border = JBUI.Borders.customLine(JBColor.border(), 1)
        }
        val panel = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.empty(4)
            add(JBLabel("输入你想要的智能体用途和风格："), BorderLayout.NORTH)
            add(JBScrollPane(inputArea).apply { border = JBUI.Borders.empty() }, BorderLayout.CENTER)
        }

        val dialog = DialogBuilder(project)
        dialog.setTitle("智能生成智能体")
        dialog.setCenterPanel(panel)
        dialog.removeAllActions()
        dialog.addOkAction()
        dialog.addCancelAction()

        if (dialog.showAndGet()) {
            val requirement = inputArea.text.trim()
            if (requirement.isEmpty()) {
                statusLabel.foreground = JBColor.RED
                statusLabel.text = "请输入智能体需求后再生成。"
            } else {
                generateAgent(requirement)
            }
        }
    }

    /**
     * 使用 AI 智能生成智能体配置
     *
     * 流程：
     * 1. 在后台线程调用 LLM 接口
     * 2. 解析 LLM 返回的 JSON 格式配置
     * 3. 将生成的配置填充到表单中
     *
     * @param requirement 用户输入的智能体需求描述
     */
    private fun generateAgent(requirement: String) {
        statusLabel.foreground = JBColor.GRAY
        statusLabel.text = "正在智能生成智能体..."
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val generated = AgentAIGenerator.generateBlocking(requirement)

                // 在 UI 线程中填充表单
                ApplicationManager.getApplication().invokeLater {
                    nameField.text = generated.name
                    descArea.text = generated.description
                    setToolsSelection(generated.tools)
                    promptArea.text = generated.systemPrompt
                    setScopeSelection(SaveScope.PROJECT)
                    statusLabel.foreground = JBColor(0x1B8A4C, 0x6FCF97)
                    statusLabel.text = "已生成新的智能体草稿"
                }
            } catch (e: Exception) {
                log.warn("Agent generation failed", e)
                ApplicationManager.getApplication().invokeLater {
                    statusLabel.foreground = JBColor.RED
                    statusLabel.text = "生成失败：${e.message ?: "未知错误"}"
                }
            }
        }
    }

    /**
     * 保存智能体配置到文件
     *
     * 保存流程：
     * 1. 表单验证（标识、使用场景、系统提示词必填，标识不能重复）
     * 2. 根据选择的范围确定保存目录（全局 ~/.jarvis/agents 或项目 .jarvis/agents）
     * 3. 生成 Markdown 格式的配置文件
     * 4. 如果是编辑且更换了保存范围，删除原文件
     */
    private fun saveAgent() {
        val name = nameField.text.trim()
        val description = descArea.text.trim()
        val systemPrompt = promptArea.text.trim()

        // ========== 表单验证 ==========
        if (name.isEmpty()) {
            Messages.showErrorDialog(project, "请填写智能体标识。", "无法保存智能体")
            return
        }
        // 检查名称是否与其他智能体冲突（排除当前正在编辑的智能体）
        val existing = existingAgentsProvider().filter {
            it.agentType.equals(name, ignoreCase = true) &&
                (agent?.sourcePath == null || agent.sourcePath != it.sourcePath)
        }
        if (existing.isNotEmpty()) {
            Messages.showErrorDialog(project, "标识已存在，请更换名称。", "无法保存智能体")
            return
        }
        if (description.isEmpty()) {
            Messages.showErrorDialog(project, "请填写使用场景。", "无法保存智能体")
            return
        }
        if (systemPrompt.isEmpty()) {
            Messages.showErrorDialog(project, "请填写系统提示词。", "无法保存智能体")
            return
        }

        // ========== 确定保存路径 ==========
        val scope = getSelectedScope()
        val targetDir = getTargetDirectory(scope)
        try {
            Files.createDirectories(targetDir)
            val slug = slugify(name)
            val existingPath = agent?.sourcePath

            // 如果是编辑且范围未变，复用原路径；否则生成新路径（避免文件名冲突）
            val targetPath = when {
                agent != null && existingPath != null && scope.matches(agent.scope) -> existingPath
                else -> {
                    var target = targetDir.resolve("$slug.md")
                    var index = 1
                    while (Files.exists(target)) {
                        target = targetDir.resolve("$slug-$index.md")
                        index++
                    }
                    target
                }
            }

            // ========== 写入文件 ==========
            val content = buildAgentFile(
                name = name,
                description = description,
                tools = selectedToolsForSave(),
                systemPrompt = systemPrompt
            )
            Files.writeString(targetPath, content)

            // 如果更换了范围，删除旧文件
            if (agent != null && existingPath != null && targetPath != existingPath) {
                Files.deleteIfExists(existingPath)
            }

            statusLabel.foreground = JBColor(0x1B8A4C, 0x6FCF97)
            statusLabel.text = "已保存到 ${targetPath.toAbsolutePath()}"
            originalState = currentState()
            onSaved()
        } catch (e: Exception) {
            log.warn("Failed to save agent", e)
            statusLabel.foreground = JBColor.RED
            statusLabel.text = "保存失败：${e.message ?: "未知错误"}"
        }
    }

    /**
     * 构建智能体配置文件内容
     *
     * 文件格式为 Markdown + YAML Front Matter：
     * ```
     * ---
     * name: "智能体名称"
     * description: "使用场景描述"
     * tools: "*" 或 ["tool1", "tool2"]
     * ---
     * 系统提示词内容...
     * ```
     */
    private fun buildAgentFile(
        name: String,
        description: String,
        tools: Any,
        systemPrompt: String
    ): String {
        // 转义特殊字符
        val safeDesc = description.replace("\n", "\\n").replace("\"", "\\\"")
        val safeName = name.replace("\"", "\\\"")

        val builder = StringBuilder()
        // YAML Front Matter 开始
        builder.appendLine("---")
        builder.appendLine("name: \"$safeName\"")
        builder.appendLine("description: \"$safeDesc\"")
        when (tools) {
            is String -> builder.appendLine("tools: \"$tools\"")
            is List<*> -> {
                builder.appendLine("tools:")
                tools.filterIsInstance<String>().forEach { tool ->
                    builder.appendLine("  - \"${tool.replace("\"", "\\\"")}\"")
                }
            }
        }
        builder.appendLine("---")
        // 系统提示词作为 Markdown 正文
        builder.appendLine(systemPrompt.trim())
        return builder.toString()
    }

    /**
     * 将名称转换为 URL 友好的 slug 格式
     * 用于生成文件名
     */
    private fun slugify(input: String): String {
        return input.lowercase()
            .replace("[^a-z0-9-_]".toRegex(), "-")
            .replace("-+".toRegex(), "-")
            .trim('-')
            .ifEmpty { "agent" }
    }

    /**
     * 根据保存范围获取目标目录
     * - USER: ~/.jarvis/agents
     * - PROJECT: {projectRoot}/.jarvis/agents
     */
    private fun getTargetDirectory(scope: SaveScope): Path {
        return when (scope) {
            SaveScope.USER -> Paths.get(getUserConfigDirectory(), "agents")
            SaveScope.PROJECT -> Paths.get(getProjectConfigDirectory(project), "agents")
        }
    }

    private fun isDirty(): Boolean = originalState != currentState()

    private fun currentState(): AgentFormState = AgentFormState(
        nameField.text.trim(),
        descArea.text.trim(),
        promptArea.text.trim(),
        getSelectedScope(),
        selectedToolsForSave()
    )
}
