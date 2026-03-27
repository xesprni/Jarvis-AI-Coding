package com.qifu.ui.smartconversation.panels

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.qifu.agent.parser.ToolHeader
import com.qifu.utils.extensions.splitLines
import com.qihoo.finance.lowcode.common.util.Icons
import kotlinx.serialization.json.JsonElement
import java.awt.Color
import java.awt.Cursor
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * LIST_IMPLEMENTATIONS 工具结果展示面板
 * 显示类型的所有继承者/实现者列表，点击可在编辑器中打开对应的类
 */
class ListImplementationsPanel(private val project: Project) : ToolPanel(project) {

    var resultPanel: JPanel? = null

    fun setToolContent(header: String, inheritors: List<String>, shouldExpand: Boolean = false) {
        val toggleAction: () -> Unit = {
            val expanded = !resultPanel!!.isVisible
            resultPanel!!.isVisible = expanded
            this.titleIconButton.icon = if (expanded) Icons.CollapseAll else Icons.ExpandAll
            revalidate()
            repaint()
        }

        resultPanel ?: run {
            resultPanel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                border = JBUI.Borders.emptyTop(8)
                add(JLabel(header).apply {
                    foreground = JBColor(Gray._220, Gray._120)
                })
                isVisible = shouldExpand
            }
            contentPanel.add(resultPanel!!)
            addTitleActionListener { toggleAction() }
        }

        inheritors.forEach { inheritorLine ->
            val label = createClickableInheritorLabel(inheritorLine)
            resultPanel!!.add(label)
        }

        if (resultPanel!!.isVisible != shouldExpand) {
            toggleAction()
        }
    }

    /** 创建可点击的继承者标签 */
    private fun createClickableInheritorLabel(inheritorLine: String): JLabel {
        // 解析格式：com.example.UserServiceImpl (class, project)
        // 提取 FQN（第一个空格之前的部分）
        val fqn = inheritorLine.substringBefore(" ")
        
        return JLabel(inheritorLine).apply {
            border = JBUI.Borders.empty(3, 0, 3, 12)
            foreground = JBColor(Color(74, 74, 74), Color(200, 200, 200))
            font = font.deriveFont(Font.PLAIN, 12f)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

            addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent) {
                    foreground = Color(255, 255, 255) // 悬浮时高亮
                    background = JBColor(Color(170, 170, 170), Color(85, 85, 85))
                    isOpaque = true
                    repaint()
                }

                override fun mouseExited(e: MouseEvent) {
                    foreground = JBColor(Color(74, 74, 74), Color(200, 200, 200))
                    isOpaque = false
                    repaint()
                }

                override fun mouseClicked(e: MouseEvent) {
                    openClassInEditor(fqn)
                }
            })
        }
    }

    /** 通过 FQN 在编辑器中打开类 */
    private fun openClassInEditor(fqn: String) {
        ApplicationManager.getApplication().invokeLater {
            tryOpenFileByFQN(fqn)
        }
    }

    override fun setContent(
        filePath: String,
        content: String,
        params: Map<String, JsonElement>,
        toolHeader: ToolHeader,
        isPartial: Boolean
    ) {
        setTipsContent(toolHeader)
        setTitleIcon(Icons.ExpandAll)
        setTitleContent(filePath)

        if (content.isNotEmpty()) {
            val lines = content.splitLines()
            val header = lines.firstOrNull() ?: "No inheritors found."
            val inheritors = lines.drop(1)
            setToolContent(header, inheritors)
        }

        contentPanel.revalidate()
        contentPanel.repaint()
    }
}
