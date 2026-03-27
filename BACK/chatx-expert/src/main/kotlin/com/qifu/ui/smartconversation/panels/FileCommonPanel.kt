package com.qifu.ui.smartconversation.panels

import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.qifu.agent.parser.ToolHeader
import com.qifu.utils.getUserConfigDirectory
import com.qihoo.finance.lowcode.common.util.Icons
import kotlinx.serialization.json.JsonElement
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rtextarea.RTextScrollPane
import java.awt.*
import javax.swing.JPanel

/**
 * 文件通用面板
 * @author weiyichao
 * @date 2025-10-03
 **/
class FileCommonPanel(private val project: Project) : ToolPanel(project) {

    var resultPanel: JPanel? = null
    var resultTextArea: RSyntaxTextArea? = null

    /** 添加单个文件的显示组 */
    private fun updateToolContent(content: String, shouldExpand: Boolean = false) {

        val toggleAction: () -> Unit = {
            val expanded = !resultPanel!!.isVisible
            resultPanel!!.isVisible = expanded
            this.titleIconButton.icon = if (expanded) Icons.CollapseAll else Icons.ExpandAll
            revalidate()
            repaint()
        }

        resultPanel?.let {
            resultTextArea!!.append(content.drop(resultTextArea!!.text?.length ?: 0))
        } ?: run {
            resultPanel = createCodeEditorPanel(content)
            resultPanel!!.isVisible = shouldExpand
            addTitleActionListener { toggleAction() }
            this.contentPanel.add(resultPanel)
        }
        if (resultPanel!!.isVisible != shouldExpand) {
            toggleAction()
        }
    }

    /** 创建带语法高亮的代码编辑器样式面板 */
    private fun createCodeEditorPanel(content: String, filePath: String = "file.txt"): JPanel {
        val panel = JPanel(BorderLayout())
        panel.isOpaque = true
        panel.background = JBColor(Color(248, 248, 248), Color(43, 43, 43))
        panel.border = JBUI.Borders.empty(4, 8)

        // 创建 RSyntaxTextArea
        resultTextArea = RSyntaxTextArea(content).apply {
            isEditable = false
            syntaxEditingStyle = detectSyntax(filePath)
            antiAliasingEnabled = true
            font = getCodeFont()
            background = JBColor(Color(248, 248, 248), Color(43, 43, 43))
            currentLineHighlightColor = JBColor(Color(232, 242, 254), Color(50, 50, 50))
            foreground = JBColor(Color(0, 0, 0), Color(220, 220, 220))
            selectionColor = JBColor(Color(173, 214, 255), Color(90, 110, 130))
            caretColor = JBColor(Color.BLACK, Color.WHITE)
            border = null
            putClientProperty(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            putClientProperty(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        }

        val scrollPane = RTextScrollPane(resultTextArea).apply {
            border = null
            viewport.background = JBColor(Color(248, 248, 248), Color(43, 43, 43))
            lineNumbersEnabled = true
            gutter.background = JBColor(Color(248, 248, 248), Color(43, 43, 43))
            gutter.lineNumberColor = JBColor(Color(128, 128, 128), Color(128, 128, 128))
            gutter.borderColor = JBColor(Color(230, 230, 230), Color(60, 60, 60))
            preferredSize = Dimension(0, 200)
        }

        panel.add(scrollPane, BorderLayout.CENTER)
        return panel
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
        setTipsContent(toolHeader)
        setTitleIcon(Icons.ExpandAll)
        val displayTitle = if (filePath.contains(getUserConfigDirectory())) {
            filePath
        } else {
            getDisplayPath(project, filePath)
        }
        setTitleContent("📄 $displayTitle")
        updateToolContent(content)

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
