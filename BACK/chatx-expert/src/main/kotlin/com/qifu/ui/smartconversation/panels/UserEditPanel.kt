package com.qifu.ui.smartconversation.panels

import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.qifu.agent.parser.ToolHeader
import com.qihoo.finance.lowcode.common.util.Icons
import kotlinx.serialization.json.JsonElement
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rtextarea.RTextScrollPane
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import javax.swing.JPanel

/**
 * 文件创建操作面板
 * @author weiyichao
 * @date 2025-10-03
 **/
class UserEditPanel(private val project: Project) : ToolPanel(project, true) {

    /** 添加单个文件的显示组 */
    private fun addSingleFileGroup(content: String) {

        // 文件内容面板 - 模拟代码编辑器样式
        val panel = createCodeEditorPanel(content)
        // 默认折叠
        panel.isVisible = false

        // 展开/折叠逻辑
        val toggleAction: () -> Unit = {
            val expanded = !panel.isVisible
            panel.isVisible = expanded
            this.titleIconButton.icon = if (expanded) Icons.CollapseAll else Icons.ExpandAll
            revalidate()
            repaint()
        }

        addTitleActionListener { toggleAction() }
        this.contentPanel.add(panel)
    }

    /** 创建带语法高亮的代码编辑器样式面板 */
    private fun createCodeEditorPanel(content: String, filePath: String = "file.txt"): JPanel {
        val panel = JPanel(BorderLayout())
        panel.isOpaque = true
        panel.background = JBColor(Color(248, 248, 248), Color(43, 43, 43))
        panel.border = JBUI.Borders.empty(4, 8)

        val textArea = RSyntaxTextArea(content).apply {
            isEditable = false
            syntaxEditingStyle = detectSyntax(filePath)
            antiAliasingEnabled = true
            font = getCodeFont()
            background = JBColor(Color(248, 248, 248), Color(43, 43, 43))
            currentLineHighlightColor = JBColor(Color(232, 242, 254), Color(50, 50, 50))
            foreground = JBColor(Color(0, 0, 0), Color(220, 220, 220))
            selectionColor = JBColor(Color(173, 214, 255), Color(90, 110, 130))
            caretColor = Color.WHITE
            border = null
            putClientProperty(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            putClientProperty(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            // 高亮显示
            highlightDiffLines(this, content)
        }


        val scrollPane = RTextScrollPane(textArea).apply {
            border = null
            viewport.background = JBColor(Color(248, 248, 248), Color(43, 43, 43))
            lineNumbersEnabled = true
            gutter.background = JBColor(Color(248, 248, 248), Color(43, 43, 43))
            gutter.lineNumberColor = JBColor(Color(128, 128, 128), Color(128, 128, 128))
            gutter.borderColor = JBColor(Color(230, 230, 230), Color(60, 60, 60))
        }

        panel.add(scrollPane, BorderLayout.CENTER)
        return panel
    }
    
    private fun highlightDiffLines(textArea: RSyntaxTextArea, content: String) {
        val lines = content.lines()
        val addedColor = JBColor(Color(220, 255, 220), Color(34, 73, 34))
        val removedColor = JBColor(Color(255, 220, 220), Color(73, 34, 34))
        
        lines.forEachIndexed { index, line ->
            try {
                when {
                    line.startsWith("+") && !line.startsWith("+++") -> {
                        textArea.addLineHighlight(index, addedColor)
                    }
                    line.startsWith("-") && !line.startsWith("---") -> {
                        textArea.addLineHighlight(index, removedColor)
                    }
                }
            } catch (e: Exception) {
            }
        }
    }


    /** 根据文件扩展名自动选择语法高亮类型 */
    private fun detectSyntax(filePath: String): String {
        return when {
            filePath.endsWith(".java", true) -> SyntaxConstants.SYNTAX_STYLE_JAVA
            filePath.endsWith(".kt", true) -> SyntaxConstants.SYNTAX_STYLE_KOTLIN
            filePath.endsWith(".py", true) -> SyntaxConstants.SYNTAX_STYLE_PYTHON
            filePath.endsWith(".html", true) -> SyntaxConstants.SYNTAX_STYLE_HTML
            filePath.endsWith(".md", true) -> SyntaxConstants.SYNTAX_STYLE_MARKDOWN
            filePath.endsWith(".json", true) -> SyntaxConstants.SYNTAX_STYLE_JSON
            filePath.endsWith(".yml", true) || filePath.endsWith(".yaml", true) -> SyntaxConstants.SYNTAX_STYLE_YAML
            filePath.endsWith(".xml", true) -> SyntaxConstants.SYNTAX_STYLE_XML
            filePath.endsWith(".sql", true) -> SyntaxConstants.SYNTAX_STYLE_SQL
            else -> SyntaxConstants.SYNTAX_STYLE_NONE
        }
    }


    /** 设置文件创建内容 - 外部调用的主要方法 */
    override fun setContent(
        filePath: String,
        content: String,
        params: Map<String, JsonElement>,
        toolHeader: ToolHeader,
        isPartial: Boolean
    ) {
        addSingleFileGroup(content)

        setTitleIcon(Icons.ExpandAll)
        val displayTitle =  filePath
        setTitleContent("\uD83D\uDCDD $displayTitle")
        refreshButtonVisible(titleIconButton, null)



        contentPanel.revalidate()
        contentPanel.repaint()
    }


    // 工具函数：自动选择支持中文的字体
    private fun getCodeFont(): Font {
        val fallbackFont = Font("Microsoft YaHei", Font.PLAIN, 13) // Windows
        val macFont = Font("PingFang SC", Font.PLAIN, 12) // macOS

        return when {
            System.getProperty("os.name").contains("Mac", ignoreCase = true) -> macFont
            System.getProperty("os.name").contains("Windows", ignoreCase = true) -> fallbackFont
            else -> Font("WenQuanYi Micro Hei", Font.PLAIN, 12) // Linux
        }
    }
}

