package com.qifu.ui.smartconversation.panels

import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import com.qifu.agent.parser.ToolHeader
import com.qihoo.finance.lowcode.common.factory.ChatXToolWindowFactory
import com.qihoo.finance.lowcode.common.util.NotifyUtils
import com.qihoo.finance.lowcode.common.util.UIUtil
import com.qihoo.finance.lowcode.smartconversation.panels.SmartToolWindowPanel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import java.awt.*
import javax.swing.*


/**
 * @author weiyichao
 * @date 2025-11-07
 **/
class AskUserQuestionPanel(private val project: Project) : ToolPanel(project) {
    var messagePanel: JEditorPane? = null
    var buttonPanel: JPanel? = null
    var mainPanel: JPanel? = null
    private var headerPanel: JPanel? = null
    private var headerLabel: JLabel? = null

    override fun onInitialized() {
        removeAll()
        setBorder(JBUI.Borders.empty())
        setOpaque(true)
        this.messagePanel = createMessagePanel()
        this.buttonPanel = createButtonPanel()
        this.headerPanel = createHeaderPanel()
        val contentPanel = JPanel(BorderLayout()).apply {
            add(messagePanel!!, BorderLayout.NORTH)
            add(buttonPanel!!, BorderLayout.CENTER)
        }
        this.mainPanel = JPanel(BorderLayout())
        mainPanel!!.layout = BorderLayout()
        mainPanel!!.add(headerPanel!!, BorderLayout.NORTH)
        mainPanel!!.add(contentPanel, BorderLayout.CENTER)
        // 布局添加
        add(mainPanel!!, BorderLayout.CENTER)
    }

    fun createMessagePanel(): JEditorPane {
        val textPane = UIUtil.createTextPane("", false)
        textPane.setBorder(JBUI.Borders.empty(2, 0))
        return textPane
    }

    fun createButtonPanel(): JPanel {
        val panel = JPanel(WrapLayout(FlowLayout.LEFT, 8, 4))
        panel.isOpaque = true
        panel.background = Color(45, 45, 45)
        panel.border = JBUI.Borders.empty(2, 0)
        return panel
    }

    private fun createHeaderPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.isOpaque = true
        panel.border = JBUI.Borders.empty(6, 8, 4, 8)
        headerLabel = JLabel().apply {
            font = font.deriveFont(Font.BOLD)
        }
        panel.add(headerLabel, BorderLayout.WEST)
        return panel
    }

    private fun setHeader(toolHeader: ToolHeader) {
        headerLabel?.apply {
            text = "<html>${toolHeader.text}</html>"
            icon = toolHeader.icon
        }
        headerPanel?.revalidate()
        headerPanel?.repaint()
    }

    /** 设置消息内容 **/
    fun setMessage(html: String) {
        val htmlText = """
       <html>
          <body>
            <p style='margin-top: 4px; margin-bottom: 4px; font-weight: bold; margin-left: 4px;'>${html}</p>
          </body>
        </html>
    """.trimIndent()

        messagePanel!!.text = htmlText
    }

    /** 动态添加按钮 **/
    fun addButton(label: String, answer: String, onClick: () -> Unit) {
        val button = JButton(label)
        if ("" == answer){
            button.isEnabled = true
        }
        else if (label.equals(answer, ignoreCase = true)){
            button.apply {
                isEnabled = false
                icon = AllIcons.General.InspectionsOK
                font = font.deriveFont(Font.BOLD)
            }
        }
        else{
            button.isEnabled = false
        }

        button.addActionListener { onClick() }
        buttonPanel!!.add(button)
    }

    /** 清空所有结果 */
    override fun clearResults() {
        buttonPanel?.removeAll()
        messagePanel?.text = ""
        mainPanel!!.revalidate()
        mainPanel!!.repaint()
    }

    override fun setContent(
        filePath: String,
        content: String,
        params: Map<String, JsonElement>,
        toolHeader: ToolHeader,
        isPartial: Boolean
    ) {
        setHeader(toolHeader)
        setMessage(filePath)

        val options = params["options"]
        val taskId = (params["taskId"] as JsonPrimitive).content
        val answer = (params["answer"] as JsonPrimitive).content
        val quickMessages = Json.decodeFromJsonElement<List<String>>(options!!)
        if (quickMessages.isNotEmpty()) {
            quickMessages.forEach {
                addButton(it, answer) {
                    onSuggestMessageClick(it, taskId)
                }
            }
        } else {
            mainPanel?.remove(buttonPanel)
        }
        SwingUtilities.invokeLater {
            mainPanel!!.revalidate()
            mainPanel!!.repaint()
        }
    }


    fun onSuggestMessageClick(message: String, taskId: String) {
        val buttons = buttonPanel?.components?.filterIsInstance<JButton>() ?: return
        buttons.forEach { btn ->
            btn.isEnabled = false
            btn.font = btn.font.deriveFont(Font.PLAIN)
        }
        val clickedButton = buttons.find { it.text == message }
        clickedButton?.apply {
            isEnabled = false
            font = font.deriveFont(Font.BOLD)
            icon = AllIcons.General.InspectionsOK
        }

        val content = ChatXToolWindowFactory.getToolWindow().contentManager.selectedContent
        if (content == null) {
            NotifyUtils.notify("流程异常", NotificationType.WARNING)
            return
        }

        if (content.component is SmartToolWindowPanel) {
            val tabbedPane = (content.component as SmartToolWindowPanel).chatTabbedPane
            val selectedTab = tabbedPane.getTab(taskId)
            selectedTab?.handleSubmit(message)
        }
    }

    class WrapLayout(align: Int = LEFT, hgap: Int = 5, vgap: Int = 5) : FlowLayout(align, hgap, vgap) {
        override fun preferredLayoutSize(target: Container): Dimension = layoutSize(target, true)
        override fun minimumLayoutSize(target: Container): Dimension = layoutSize(target, false)

        private fun layoutSize(target: Container, preferred: Boolean): Dimension {
            val insets = target.insets
            val parent = target.parent
            val availableWidth = when {
                target.width > 0 -> target.width
                parent != null && parent.width > 0 -> parent.width
                else -> 600 // 兜底初始宽度
            }
            val maxWidth = availableWidth - (insets.left + insets.right + hgap * 2)

            var width = 0
            var height = insets.top + vgap
            var rowWidth = 0
            var rowHeight = 0

            val components = target.components
            for (component in components) {
                if (!component.isVisible) continue
                val size = if (preferred) component.preferredSize else component.minimumSize

                if (rowWidth + size.width > maxWidth) {
                    height += rowHeight + vgap
                    width = maxOf(width, rowWidth)
                    rowWidth = 0
                    rowHeight = 0
                }

                rowWidth += size.width + hgap
                rowHeight = maxOf(rowHeight, size.height)
            }

            height += rowHeight + vgap + insets.bottom
            width = maxOf(width, rowWidth) + insets.left + insets.right
            return Dimension(width, height)
        }
    }
}
