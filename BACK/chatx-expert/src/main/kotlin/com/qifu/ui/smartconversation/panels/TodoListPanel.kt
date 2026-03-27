package com.qifu.ui.smartconversation.panels

import com.intellij.openapi.project.Project
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.qifu.agent.parser.ToolHeader
import com.qifu.utils.TodoItem
import com.qihoo.finance.lowcode.common.util.Icons
import kotlinx.serialization.json.*
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*


/**
 * @author weiyichao
 * @date 2025-10-03
 **/
class TodoListPanel(private val project: Project) : ToolPanel(project) {


    data class TodoGroup(
        val title: String,
        val items: List<TodoItem>
    ) {
        val completedCount: Int get() = items.count { it.status == TodoItem.Status.COMPLETED }
        val totalCount: Int get() = items.size
        val progress: Float get() = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    }

    /** 解析待办事项内容 */
    private fun parseTodoContent(filePath: String, params: Map<String, JsonElement>): List<TodoGroup> {
        val groups = mutableListOf<TodoGroup>()
        val todosValues = params["todos"]
        val executeIndex = (params["curIndex"] as JsonPrimitive).int
        val todoList = Json.decodeFromJsonElement<List<TodoItem>>(todosValues!!)
        if (todoList.isEmpty()) return emptyList()

        val currentGroupTitle = String.format("(%d/%d) %s", executeIndex, todoList.size, filePath)
        groups.add(TodoGroup(currentGroupTitle, todoList))
        return groups
    }

    /** 创建进度条组件 */
    private fun createProgressBar(progress: Float): JPanel {
        return object : JPanel() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val g2d = g as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                val width = width - 8
                val height = 6
                val x = 4 // 与标题对齐
                val y = (getHeight() - height) / 2

                // 背景条
                g2d.color = JBColor(Color(220, 220, 220), Color(60, 60, 60))
                g2d.fillRoundRect(x, y, width, height, height, height)

                // 进度条
                val progressWidth = (width * progress).toInt()
                if (progressWidth > 0) {
                    g2d.color = JBColor(Color(67, 160, 71), Color(76, 175, 80))
                    g2d.fillRoundRect(x, y, progressWidth, height, height, height)
                }
            }

            override fun getPreferredSize(): Dimension {
                return Dimension(200, 16)
            }
        }.apply {
            isOpaque = false
            border = JBUI.Borders.empty(4, 0, 8, 0) // 增加下边距，与任务列表分开
        }
    }

    /** 创建待办事项组面板 */
    private fun createTodoGroupPanel(group: TodoGroup): JPanel {

        setTitleIcon(Icons.CollapseAll)
        setTitleContent(group.title)

        // 进度条
        val progressBar = createProgressBar(group.progress)

        // 任务列表
        val itemsPanel = JPanel()
        itemsPanel.layout = BoxLayout(itemsPanel, BoxLayout.Y_AXIS)
        itemsPanel.isOpaque = false
        itemsPanel.border = JBUI.Borders.empty(4, 0, 0, 0) // 减少上边距，因为进度条已有下边距

        group.items.forEach { item ->
            val itemPanel = createTodoItemPanel(item)
            itemsPanel.add(itemPanel)
        }

        // 默认展开任务列表
        itemsPanel.isVisible = true

        // 折叠/展开逻辑
        val toggleAction: () -> Unit = {
            val expanded = !itemsPanel.isVisible
            itemsPanel.isVisible = expanded
            this.titleIconButton.icon = if (expanded) Icons.CollapseAll else Icons.ExpandAll
            revalidate()
            repaint()
        }

        addTitleActionListener { toggleAction() }


        val centerPanel = JPanel(BorderLayout())
        centerPanel.isOpaque = false
        centerPanel.add(progressBar, BorderLayout.NORTH)
        centerPanel.add(itemsPanel, BorderLayout.CENTER)

        return centerPanel
    }

    /** 创建单个待办事项面板 */
    private fun createTodoItemPanel(item: TodoItem): JPanel {
        val itemPanel = JPanel(BorderLayout())
        itemPanel.isOpaque = true
        itemPanel.maximumSize = Dimension(Integer.MAX_VALUE, 32)
        itemPanel.preferredSize = Dimension(itemPanel.preferredSize.width, 32)

        // 根据状态设置背景色和样式
        when (item.status) {
            TodoItem.Status.COMPLETED -> {
                itemPanel.background = JBColor(Gray._245, Gray._45)
            }

            TodoItem.Status.IN_PROGRESS -> {
                itemPanel.background = JBColor(Gray._250, Gray._55)
                itemPanel.border = JBUI.Borders.customLine(JBColor(Color(76, 175, 80), Color(76, 175, 80)), 0, 3, 0, 0)
            }

            TodoItem.Status.PENDING -> {
                itemPanel.background = JBColor(Gray._245, Gray._45)
            }
        }

        // 状态图标
        val iconLabel = JLabel().apply {
            preferredSize = Dimension(24, 24)
            horizontalAlignment = SwingConstants.CENTER

            when (item.status) {
                TodoItem.Status.COMPLETED -> {
                    text = "✓"
                    foreground = JBColor(Color(67, 160, 71), Color(76, 175, 80))
                    font = font.deriveFont(Font.BOLD, 12f)
                }

                TodoItem.Status.IN_PROGRESS -> {
                    text = "●"
                    foreground = JBColor(Color(67, 160, 71), Color(76, 175, 80))
                    font = font.deriveFont(Font.BOLD, 12f)
                }

                TodoItem.Status.PENDING -> {
                    text = "○"
                    foreground = JBColor(Gray._120, Gray._160)
                    font = font.deriveFont(Font.PLAIN, 12f)
                }
            }
        }

        // 任务文本
        val textLabel = JLabel().apply {
            border = JBUI.Borders.emptyLeft(8)

            when (item.status) {
                TodoItem.Status.COMPLETED -> {
                    // 已完成任务：添加删除线样式
                    text = "<html><s>${item.content}</s></html>"
                    foreground = JBColor(Gray._120, Gray._160)
                    font = font.deriveFont(Font.PLAIN, 12f)
                }

                TodoItem.Status.IN_PROGRESS -> {
                    text = item.activeForm.ifBlank { item.content }
                    foreground = JBColor(Gray._30, Gray._255)
                    font = font.deriveFont(Font.PLAIN, 13f)
                }

                TodoItem.Status.PENDING -> {
                    text = item.content
                    foreground = JBColor(Gray._60, Gray._200)
                    font = font.deriveFont(Font.PLAIN, 12f)
                }
            }
        }

        // 添加悬浮效果
        val hoverColor = when (item.status) {
            TodoItem.Status.IN_PROGRESS -> JBColor(Color(240, 240, 240), Color(65, 65, 65))
            else -> JBColor(Color(235, 235, 235), Color(55, 55, 55))
        }
        val normalColor = itemPanel.background

        itemPanel.addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) {
                itemPanel.background = hoverColor
                itemPanel.repaint()
            }

            override fun mouseExited(e: MouseEvent) {
                itemPanel.background = normalColor
                itemPanel.repaint()
            }
        })

        itemPanel.add(iconLabel, BorderLayout.WEST)
        itemPanel.add(textLabel, BorderLayout.CENTER)
        itemPanel.border = JBUI.Borders.compound(
            itemPanel.border ?: JBUI.Borders.empty(),
            JBUI.Borders.empty(4, 0, 4, 8) // 使用面板内边距，保持右边距
        )

        return itemPanel
    }

    override fun setContent(
        filePath: String,
        content: String,
        params: Map<String, JsonElement>,
        toolHeader: ToolHeader,
        isPartial: Boolean
    ) {
        // 清空现有内容
        contentPanel.removeAll()

        // 解析待办事项内容
        val todoGroups = parseTodoContent(filePath, params)

        // 更新描述区域状态
        setTipsContent(toolHeader)


        // 添加每个待办事项组
        todoGroups.forEachIndexed { index, group ->
            val groupPanel = createTodoGroupPanel(group)
            contentPanel.add(groupPanel)

            // 添加组间间距（除了最后一个）
            if (index < todoGroups.size - 1) {
                contentPanel.add(Box.createVerticalStrut(12))
            }
        }

        // 刷新界面
        contentPanel.revalidate()
        contentPanel.repaint()
    }

}