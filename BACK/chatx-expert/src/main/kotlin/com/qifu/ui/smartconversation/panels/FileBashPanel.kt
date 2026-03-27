package com.qifu.ui.smartconversation.panels

import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.qifu.agent.parser.ToolHeader
import com.qihoo.finance.lowcode.common.util.Icons
import kotlinx.serialization.json.JsonElement
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import javax.swing.*
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext


/**
 * @author weiyichao
 * @date 2025-10-03
 **/
class FileBashPanel(private val project: Project) : ToolPanel(project) {

    var resultPanel: JPanel? = null
    var resultTextPane: JTextPane? = null

    init {
        refreshButtonVisible(null, null)
    }

    /** 添加单个文件的显示组 */
    private fun updateToolContent(content: String, shouldExpand: Boolean) {
        val toggleAction: () -> Unit = {
            val expanded = !resultPanel!!.isVisible
            resultPanel!!.isVisible = expanded
            this.titleIconButton.icon = if (expanded) Icons.CollapseAll else Icons.ExpandAll
            revalidate()
            repaint()
        }

        resultPanel?.let {
            // 这里不确定command已输出的内容是否会改变，采用全量覆盖内容的方式
            resultTextPane!!.text = content
        } ?: run {
            resultPanel = createTerminalPanel(content, true)
            resultPanel!!.isVisible = shouldExpand
            addTitleActionListener { toggleAction() }
            this.contentPanel.add(resultPanel)
        }
        if (resultPanel!!.isVisible != shouldExpand) {
            toggleAction()
        }
    }


    private fun createTerminalPanel(content: String, isCommand: Boolean = false): JPanel {
        val panel = JPanel(BorderLayout())
        panel.isOpaque = true
        panel.background = JBColor(Color(248, 248, 248), Color(43, 43, 43))
        panel.border = JBUI.Borders.empty(8, 12)

        resultTextPane = createHighlightTextPane(content, "Output")
        val scrollPane = JScrollPane(resultTextPane)
        scrollPane.border = null
        scrollPane.background = JBColor(Color(248, 248, 248), Color(43, 43, 43))
        scrollPane.viewport.background = JBColor(Color(248, 248, 248), Color(43, 43, 43))
        scrollPane.preferredSize = java.awt.Dimension(0, 200)

        panel.add(scrollPane, BorderLayout.CENTER)
        return panel
    }

    fun createHighlightTextPane(content: String, highlightWord: String): JTextPane {
        val textPane = JTextPane()
        textPane.isEditable = false

        val doc = textPane.styledDocument

        // 默认样式
        val defaultStyle = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE)
        val normalAttr = doc.addStyle("normal", defaultStyle)
        StyleConstants.setForeground(normalAttr, JBColor(Color(60, 60, 60), Color.WHITE))

        // 高亮样式
        val highlightAttr = doc.addStyle("highlight", defaultStyle)
        StyleConstants.setForeground(highlightAttr, JBColor(Color(0, 102, 204), Color(255, 215, 0)))
        StyleConstants.setBold(highlightAttr, true)

        // 分段写入文本
        var lastIndex = 0
        val lowerContent = content.lowercase()
        val lowerHighlight = highlightWord.lowercase()

        while (true) {
            val index = lowerContent.indexOf(lowerHighlight, lastIndex)
            if (index == -1) {
                doc.insertString(doc.length, content.substring(lastIndex), normalAttr)
                break
            }
            doc.insertString(doc.length, content.substring(lastIndex, index), normalAttr)
            doc.insertString(doc.length, content.substring(index, index + highlightWord.length), highlightAttr)
            lastIndex = index + highlightWord.length
        }

        textPane.background = JBColor(Color(250, 250, 250), Color(45, 45, 45))
        return textPane
    }

    /** 创建终端风格的单行面板 */
    private fun createTerminalLinePanel(line: String, isCommand: Boolean): JPanel {
        val linePanel = JPanel(BorderLayout())
        linePanel.isOpaque = true
        linePanel.background = JBColor(Color(248, 248, 248), Color(43, 43, 43))
        linePanel.border = JBUI.Borders.empty(1, 0)

        val systemFont = getCodeFont() // 等宽字体

        // 内容
        val codeLabel = JLabel(line.ifEmpty { " " }).apply {
            border = JBUI.Borders.empty(0, 0, 0, 8)
            foreground = JBColor(Color(60, 60, 60), Color(204, 204, 204))
            font = systemFont
        }

        linePanel.add(codeLabel, BorderLayout.CENTER)

        return linePanel
    }

    /** 设置文件创建内容 - 外部调用的主要方法 */
    override fun setContent(
        filePath: String, content: String, params: Map<String, JsonElement>, toolHeader: ToolHeader, isPartial: Boolean
    ) {
        // 更新描述区域状态
        setTipsContent(toolHeader)
        setTitleContent("$ $filePath")
        if (content.isNotEmpty()) {
            val shouldExpand = isPartial && content.isNotEmpty()
            refreshButtonVisible(titleIconButton,null)
            setTitleIcon(if (shouldExpand) Icons.CollapseAll else Icons.ExpandAll)
            updateToolContent(content, shouldExpand)
        }

        contentPanel.revalidate()
        contentPanel.repaint()
    }


    // 工具函数：自动选择支持中文的字体
    private fun getCodeFont(): Font {
        val fallbackFont = Font("Microsoft YaHei", Font.PLAIN, 12) // Windows
        val macFont = Font("PingFang SC", Font.PLAIN, 12) // macOS

        return when {
            System.getProperty("os.name").contains("Mac", ignoreCase = true) -> macFont
            System.getProperty("os.name").contains("Windows", ignoreCase = true) -> fallbackFont
            else -> Font("WenQuanYi Micro Hei", Font.PLAIN, 12) // Linux
        }
    }

}