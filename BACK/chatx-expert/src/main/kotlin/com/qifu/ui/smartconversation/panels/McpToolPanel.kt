package com.qifu.ui.smartconversation.panels

import com.intellij.openapi.project.Project
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.qifu.agent.parser.ToolHeader
import com.qihoo.finance.lowcode.common.util.IconUtil
import com.qihoo.finance.lowcode.common.util.Icons
import kotlinx.serialization.json.*
import java.awt.*
import javax.swing.*

/**
 * MCP 工具面板
 *
 * 用于在 AI Agent 聊天界面中展示 MCP (Model Context Protocol) 工具的调用信息,包括:
 * - 工具名称与描述
 * - 工具所属的 MCP 服务器
 * - 工具的参数 Schema
 * - 实际传入的参数内容 (JSON 格式)
 *
 * @param project IntelliJ 项目实例
 */
class McpToolPanel(project: Project) : ToolPanel(project) {

    private val toolNameLabel = JLabel().apply {
        font = (font ?: Font("Dialog", Font.BOLD, 14)).deriveFont(Font.BOLD, 15f)
        foreground = TITLE_TEXT_COLOR
        isOpaque = false
    }

    private val descriptionLabel = JTextArea().apply {
        font = Font("Dialog", Font.PLAIN, 12)
        lineWrap = true
        wrapStyleWord = true
        isEditable = false
        isOpaque = false
        border = null
        foreground = DESCRIPTION_TEXT_COLOR
        background = Color(0, 0, 0, 0)
        toolTipText = null
    }

    private val argumentArea = JTextArea().apply {
        font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        lineWrap = true
        wrapStyleWord = true
        isEditable = false
        background = CODE_BACKGROUND
        foreground = CODE_TEXT_COLOR
        border = JBUI.Borders.empty(10, 12)
        margin = JBInsets(6, 8, 6, 8)
    }

    private val argumentScroll = JBScrollPane(argumentArea).apply {
        border = JBUI.Borders.customLine(BORDER_COLOR, 1)
        horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        background = CODE_BACKGROUND
        alignmentX = Component.LEFT_ALIGNMENT
    }

    private val mainWrapper = JPanel(BorderLayout()).apply {
        isOpaque = true
        background = PANEL_BACKGROUND
        border = JBUI.Borders.empty(12, 18, 18, 18)
    }

    private val prettyJson = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val headerTextPanel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 2)).apply {
        isOpaque = false
    }

    init {
        configureLayout()
        titleIconButton.isVisible = false
        
        // 确保 iconLabel 有足够的边距
        iconLabel.border = JBUI.Borders.empty(4, 4, 4, 8)
    }

    /**
     * 配置面板布局
     *
     * 初始化面板的视觉结构,包括:
     * - 设置 AI Agent 图标
     * - 创建标题行 (工具名称 + 图标)
     * - 创建描述区域
     * - 创建参数展示区域 (带滚动条)
     */
    private fun configureLayout() {
        iconLabel.icon = IconUtil.getThemeAwareIcon(Icons.AI_AGENT_LIGHT, Icons.AI_AGENT)
        contentPanel.removeAll()
        contentPanel.layout = BorderLayout()
        contentPanel.isOpaque = false

        val titleRow = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.emptyBottom(10)
            val icon = JLabel(Icons.AI_COMMAND).apply {
                border = JBUI.Borders.emptyRight(6)
            }
            add(icon, BorderLayout.WEST)
            add(toolNameLabel, BorderLayout.CENTER)
        }

        val descriptionWrapper = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.emptyBottom(10)
            
            val scrollPane = JBScrollPane(descriptionLabel).apply {
                border = null
                isOpaque = false
                viewport.isOpaque = false
                horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
                verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
                maximumSize = Dimension(Int.MAX_VALUE, descriptionLabel.getFontMetrics(descriptionLabel.font).height * 15)
            }
            add(scrollPane, BorderLayout.CENTER)
        }

        val topPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            add(titleRow)
            add(descriptionWrapper)
            add(Box.createVerticalStrut(12))
        }

        mainWrapper.add(topPanel, BorderLayout.NORTH)
        mainWrapper.add(argumentScroll, BorderLayout.CENTER)

        val wrapperPanel = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(mainWrapper, BorderLayout.CENTER)
        }
        contentPanel.add(wrapperPanel, BorderLayout.CENTER)
    }

    /**
     * 清空面板内容
     *
     * 重置所有显示组件的文本内容
     */
    override fun clearResults() {
        toolNameLabel.text = ""
        descriptionLabel.text = ""
        argumentArea.text = ""
    }

    /**
     * 设置面板内容
     *
     * @param filePath 工具名称 (在本面板中用作工具名称显示)
     * @param content 工具调用时传入的参数 JSON 字符串
     * @param params 额外参数,包含:
     *   - server_name: MCP 服务器名称
     *   - remote_server_name: 远程服务器名称 (可选)
     *   - tool_description: 工具描述
     *   - tool_schema: 工具参数的 JSON Schema
     * @param toolHeader 工具头信息 (未使用)
     * @param isPartial 是否为部分更新 (未使用)
     */
    override fun setContent(
        filePath: String,
        content: String,
        params: Map<String, JsonElement>,
        toolHeader: ToolHeader,
        isPartial: Boolean
    ) {
        val serverName = params["server_name"].asTextOrNull().orEmptyIfBlank("未知服务")
        val description = params["tool_description"].asTextOrNull()

        updateHeader(serverName)
        toolNameLabel.text = filePath.ifBlank { "未命名的工具" }
        val descText = description?.takeIf { it.isNotBlank() } ?: ""
        descriptionLabel.text = descText
        
        val actualLines = adjustDescriptionHeight(descText)
        descriptionLabel.toolTipText = if (actualLines > 15) descText else null
        
        val formattedJson = formatJson(content)
        argumentArea.text = formattedJson
        
        adjustArgumentAreaHeight(formattedJson)

        revalidate()
        repaint()
    }
    
    /**
     * 根据内容调整描述区域的高度
     * @return 实际行数
     */
    private fun adjustDescriptionHeight(content: String): Int {
        if (content.isBlank()) {
            descriptionLabel.rows = 0
            return 0
        }
        
        val lineHeight = descriptionLabel.getFontMetrics(descriptionLabel.font).height
        val availableWidth = descriptionLabel.width.takeIf { it > 0 } ?: 500
        
        val lines = content.split("\n").sumOf { line ->
            if (line.isEmpty()) 1
            else {
                val fm = descriptionLabel.getFontMetrics(descriptionLabel.font)
                val lineWidth = fm.stringWidth(line)
                maxOf(1, (lineWidth + availableWidth - 1) / availableWidth)
            }
        }
        
        descriptionLabel.rows = lines.coerceAtMost(15)
        return lines
    }
    
    /**
     * 根据内容调整参数显示区域的高度
     */
    private fun adjustArgumentAreaHeight(content: String) {
        val lines = content.split("\n").size
        val calculatedRows = lines.coerceIn(3, 15)
        argumentArea.rows = calculatedRows
        
        argumentScroll.revalidate()
    }

    /**
     * 更新面板头部信息
     *
     * 显示 "Jarvis want to use a tool from [服务器名称] MCP server" 的提示文本
     *
     * @param serverName 本地 MCP 服务器名称
     * @param remoteServer 远程 MCP 服务器名称,若存在则优先显示
     */
    private fun updateHeader(serverName: String) {
        headerTextPanel.removeAll()
        headerTextPanel.add(JLabel("Jarvis want to use a tool from ").apply {
            font = Font("Dialog", Font.PLAIN, 12)
            foreground = HEADER_TEXT_COLOR
        })
        headerTextPanel.add(JLabel(serverName).apply {
            font = Font("Dialog", Font.PLAIN, 12)
            foreground = SERVER_NAME_COLOR
        })
        headerTextPanel.add(JLabel(" MCP server").apply {
            font = Font("Dialog", Font.PLAIN, 12)
            foreground = HEADER_TEXT_COLOR
        })
        
        tipsPanel.removeAll()
        tipsPanel.layout = BorderLayout()
        tipsPanel.isOpaque = false
        tipsPanel.border = JBUI.Borders.empty(6, 0, 8, 0)
        tipsPanel.preferredSize = Dimension(tipsPanel.preferredSize.width, 30)
        tipsPanel.add(headerTextPanel, BorderLayout.WEST)
        tipsPanel.revalidate()
        tipsPanel.repaint()
    }


    /**
     * 格式化 JSON 字符串
     *
     * 将传入的 JSON 字符串进行格式化 (缩进、换行),使其更易读
     * 如果解析失败则返回原始字符串
     *
     * @param raw 原始 JSON 字符串
     * @return 格式化后的 JSON 字符串,或原始字符串 (解析失败时)
     */
    private fun formatJson(raw: String): String {
        val text = raw.trim()
        if (text.isEmpty()) return "{ }"
        return runCatching {
            val json = Json.parseToJsonElement(text)
            prettyJson.encodeToString(JsonElement.serializer(), json)
        }.getOrElse { text }
    }

    /**
     * 将 JsonElement 安全地转换为字符串
     *
     * @return 如果是 JsonPrimitive 则返回其内容,否则返回 null
     */
    private fun JsonElement?.asTextOrNull(): String? =
        (this as? JsonPrimitive)?.contentOrNull

    /**
     * 将 JsonArray 转换为字符串集合
     *
     * @return 提取数组中所有字符串元素组成的 Set
     */
    private fun JsonArray.toStringSet(): Set<String> =
        mapNotNull { it.jsonPrimitive.contentOrNull }.toSet()

    /**
     * 字符串空白值处理
     *
     * @param default 当字符串为 null 或空白时的默认值
     * @return 原字符串或默认值
     */
    private fun String?.orEmptyIfBlank(default: String): String =
        if (this.isNullOrBlank()) default else this

    /**
     * 扩展组件以填充父容器宽度
     *
     * 设置组件的最大宽度为无限,但限制最大高度
     * 同时设置左对齐
     *
     * @param maxHeight 最大高度,默认为无限
     */
    private fun JComponent.expandToContainer(maxHeight: Int = Int.MAX_VALUE) {
        maximumSize = Dimension(Int.MAX_VALUE, maxHeight.takeIf { it > 0 } ?: Int.MAX_VALUE)
        alignmentX = Component.LEFT_ALIGNMENT
    }

    companion object {
        /**
         * 标题文本颜色
         * 亮色主题: 深黑色，暗色主题: 浅白色
         */
        private val TITLE_TEXT_COLOR = JBColor(
            Color(23, 23, 23),    // Light theme: 接近黑色
            Color(221, 221, 221)  // Dark theme: 浅白色
        )

        /**
         * 描述文本颜色
         * 亮色主题: 中性灰，暗色主题: 浅灰
         */
        private val DESCRIPTION_TEXT_COLOR = JBColor(
            Gray._96,             // Light theme: 中性灰
            Gray._175             // Dark theme: 浅灰
        )

        /**
         * 代码文本颜色
         * 亮色主题: 深灰，暗色主题: 浅白灰
         */
        private val CODE_TEXT_COLOR = JBColor(
            Gray._40,             // Light theme: 深灰色
            Gray._208             // Dark theme: 浅白灰
        )

        /**
         * 面板背景色
         * 使用 IDE 标准面板背景
         */
        private val PANEL_BACKGROUND = JBColor(
            UIUtil.getPanelBackground(),
            UIUtil.getPanelBackground()
        )

        /**
         * 代码背景色
         * 亮色主题: 浅灰白，暗色主题: 较深灰
         */
        private val CODE_BACKGROUND = JBColor(
            Gray._248, // Light theme: 非常浅的灰白色
            Gray._43     // Dark theme: 较深的灰色
        )

        /**
         * 边框颜色
         * 亮色主题: 浅灰，暗色主题: 中灰
         */
        private val BORDER_COLOR = JBColor(
            Gray._220,            // Light theme: 浅灰色边框
            Gray._70              // Dark theme: 中灰色边框
        )

        /**
         * 头部文本颜色
         * 亮色主题: 中灰，暗色主题: 浅灰
         */
        private val HEADER_TEXT_COLOR = JBColor(
            Gray._128,            // Light theme: 中灰色
            Gray._188             // Dark theme: 浅灰色
        )

        /**
         * 服务器名称高亮颜色
         * 亮色主题: 金色，暗色主题: 浅金色
         */
        private val SERVER_NAME_COLOR = JBColor(
            Color(180, 142, 40),  // Light theme: 较深的金色
            Color(220, 184, 80)   // Dark theme: 较浅的金色
        )
    }
}
