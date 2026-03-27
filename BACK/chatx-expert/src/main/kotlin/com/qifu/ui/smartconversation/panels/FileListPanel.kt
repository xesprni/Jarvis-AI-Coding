package com.qifu.ui.smartconversation.panels

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.qifu.agent.parser.ToolHeader
import com.qifu.utils.extensions.splitLines
import com.qifu.utils.toAbsolutePath
import com.qifu.utils.toRelativePath
import com.qihoo.finance.lowcode.common.util.Icons
import kotlinx.serialization.json.JsonElement
import java.awt.Color
import java.awt.Cursor
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.io.path.Path
import kotlin.io.path.pathString


/**
 * @author weiyichao
 * @date 2025-10-03
 **/
class FileListPanel(private val project: Project) : ToolPanel(project) {

    var resultPanel: JPanel? = null

    fun setToolContent(header: String, basePath: String, items: List<String>, shouldExpand: Boolean = false) {
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

        items.forEach { item ->
            val label = createClickableItemLabel(item, basePath)
            resultPanel!!.add(label)
        }

        if (resultPanel!!.isVisible != shouldExpand) {
            toggleAction()
        }
    }

    /** 创建可点击的文件项标签 */
    private fun createClickableItemLabel(item: String, basePathStr: String): JLabel {
        val isFile = File(basePathStr,item).isFile()

        return JLabel(item).apply {
            border = JBUI.Borders.empty(3, 0, 3, 12) // 使用面板内边距
            foreground = if (isFile) JBColor(Color(74, 74, 74), Color(200, 200, 200))
            else JBColor(Color(90, 90, 90), Color(190, 190, 190)) // 文件稍微亮一些
            font = font.deriveFont(Font.PLAIN, 12f)

            // 只为文件添加点击功能
            if (isFile) {
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

                addMouseListener(object : MouseAdapter() {
                    override fun mouseEntered(e: MouseEvent) {
                        foreground = Color(255, 255, 255) // 悬浮时高亮
                        background = JBColor(Color(170, 170, 170), Color(85, 85, 85))
                        isOpaque = true
                        repaint()
                    }

                    override fun mouseExited(e: MouseEvent) {
                        foreground = JBColor(Color(74, 74, 74), Color(200, 200, 200)) // 恢复正常颜色
                        isOpaque = false
                        repaint()
                    }

                    override fun mouseClicked(e: MouseEvent) {
                        openFileInEditor(item, basePathStr)
                    }
                })
            } else {
                // 目录项只有悬浮效果，无点击功能
                addMouseListener(object : MouseAdapter() {
                    override fun mouseEntered(e: MouseEvent) {
                        foreground = JBColor(Color(85, 85, 85), Color(220, 220, 220)) // 轻微高亮
                        repaint()
                    }

                    override fun mouseExited(e: MouseEvent) {
                        foreground = JBColor(Color(190, 190, 190), Color(90, 90, 90)) // 恢复正常颜色
                        repaint()
                    }
                })

                // 目录项悬浮效果 + 点击聚焦文件树
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                addMouseListener(object : MouseAdapter() {
                    override fun mouseEntered(e: MouseEvent) {
                        foreground = JBColor(Color(85, 85, 85),Color(220, 220, 220))
                        repaint()
                    }

                    override fun mouseExited(e: MouseEvent) {
                        foreground = JBColor(Color(190, 190, 190), Color(90, 90, 90))
                        repaint()
                    }

                    override fun mouseClicked(e: MouseEvent) {
                        focusAndExpandDirectory(item, basePathStr)
                    }
                })


            }
        }
    }

    private fun focusAndExpandDirectory(dirName: String, basePath: String) {
        val project = ProjectManager.getInstance().openProjects.firstOrNull() ?: return
        val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(File("$basePath/$dirName")) ?: return

        ApplicationManager.getApplication().invokeLater {
            ProjectView.getInstance(project).select(null, virtualFile, true)
        }
    }

    override fun setContent(
        filePath: String, content: String, params: Map<String, JsonElement>, toolHeader: ToolHeader, isPartial: Boolean
    ) {
        setTipsContent(toolHeader)
        setTitleIcon(Icons.ExpandAll)
        val displayTitle = getDisplayPath(project, filePath)
        setTitleContent(displayTitle)

        if (content.isNotEmpty()) {
            val lines = content.splitLines()
            val header = lines.firstOrNull() ?: "No results found."
            val files = lines.drop(1)
            val (searchPath, relativePaths) = toRelativePaths(filePath, files)
            setToolContent(header, searchPath, relativePaths)
        }

        contentPanel.revalidate()
        contentPanel.repaint()
    }

    private fun toRelativePaths(searchPath: String, files: List<String>): Pair<String, List<String>> {
        if (files.isEmpty()) return searchPath to files

        var searchPath = toAbsolutePath(searchPath, project.basePath!!)
        if (File(searchPath).isFile) searchPath = Path(searchPath).parent.pathString
        return searchPath to files.map { toRelativePath(it, searchPath) }
    }

}