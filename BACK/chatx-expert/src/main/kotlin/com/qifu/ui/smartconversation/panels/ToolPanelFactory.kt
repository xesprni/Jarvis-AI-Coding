package com.qifu.ui.smartconversation.panels

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import com.qifu.agent.parser.ToolHeader
import com.qifu.agent.parser.ToolSegment
import com.qifu.agent.parser.UiToolName
import com.qifu.utils.normalizeFilePath
import com.qifu.utils.toRelativePath
import com.qihoo.finance.lowcode.common.constants.Constants
import com.qihoo.finance.lowcode.common.util.UIUtil
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.awt.*
import java.io.File
import javax.swing.*
import javax.swing.border.Border
import javax.swing.event.HyperlinkEvent


/**
 * @author weiyichao
 * @date 2025-10-04
 **/


abstract class ToolPanel(
    private val project: Project,
    private val hasShowDiff: Boolean = false
) : BorderLayoutPanel() {
    private val LOG = thisLogger()
    val tipsPanel: JEditorPane
    val iconLabel: JLabel
    val contentPanel: JPanel
    val titlePanel: JEditorPane
    var titleIconButton: JButton
    val titlePanelContainer = JPanel().apply {
        isOpaque = false
        layout = BorderLayout(0, 0)
    }
    val titleButtonPanel = JPanel().apply {
        isOpaque = false
        layout = FlowLayout(FlowLayout.LEFT, 5, 0)
    }
    var diffButton: JButton

    init {
        this.tipsPanel = createTipsPanel()
        this.iconLabel = JLabel()
        this.contentPanel = object : JPanel(), Scrollable {
            override fun getPreferredScrollableViewportSize(): Dimension =
                super.getPreferredSize() ?: Dimension(0, 0)

            override fun getScrollableUnitIncrement(
                visibleRect: Rectangle?,
                orientation: Int,
                direction: Int
            ): Int = 16

            override fun getScrollableBlockIncrement(
                visibleRect: Rectangle?,
                orientation: Int,
                direction: Int
            ): Int = 64

            override fun getScrollableTracksViewportWidth(): Boolean = true

            override fun getScrollableTracksViewportHeight(): Boolean = false
        }
        this.titlePanel = createTitlePanel()
        this.titleIconButton = JButton().apply {
            isContentAreaFilled = false
            isOpaque = false
            isBorderPainted = false
            isFocusPainted = false
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        }
        this.diffButton = JButton()
        setBorder(JBUI.Borders.empty())
        background = JBColor(Color(240, 240, 240), Color(50, 50, 50))
        setOpaque(true)
        createContentPanel()
        onInitialized()
    }

    open fun onInitialized(){

    }

    /** 创建圆角边框 */
    protected fun createRoundedBorder(color: Color): Border {
        val thickness = 1
        val radius = 8
        return object : Border {
            override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
                val g2d = g as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2d.color = color
                g2d.stroke = BasicStroke(thickness.toFloat())
                g2d.drawRoundRect(x, y, width - thickness, height - thickness, radius, radius)
            }

            override fun getBorderInsets(c: Component): Insets {
                return Insets(thickness + 1, thickness + 1, thickness + 1, thickness + 1) // 减少内边距
            }

            override fun isBorderOpaque(): Boolean = false
        }
    }


    /** 创建描述区域 */
    fun createTipsPanel(): JEditorPane {
        val textPane = UIUtil.createTextPane("", false) { event: HyperlinkEvent? ->
            if (FileUtil.exists(event!!.description) && HyperlinkEvent.EventType.ACTIVATED == event.eventType) {
                val file = LocalFileSystem.getInstance().findFileByPath(event.description)
                if (file != null) {
                    FileEditorManager.getInstance(project).openFile(file, true)
                }
            }
            UIUtil.handleHyperlinkClicked(event)
        }
        textPane.setBorder(JBUI.Borders.empty(2, 0))
        return textPane
    }

    fun createTitlePanel(): JEditorPane {
        val textPane = UIUtil.createTextPane("", false)
        textPane.setBorder(JBUI.Borders.empty(0, 0))
        return textPane
    }

    fun createContentPanel() {
        contentPanel.setLayout(BoxLayout(contentPanel, BoxLayout.Y_AXIS))
        contentPanel.setOpaque(true)
        contentPanel.background = JBColor(Color(245, 245, 245), Color(45, 45, 45))

        val scrollPane: JScrollPane = JBScrollPane(contentPanel)
        scrollPane.setBorder(null)
        scrollPane.viewport.isOpaque = false
        scrollPane.background = JBColor(Color(250, 250, 250), Color(35, 35, 35))
        scrollPane.viewport.background = JBColor(Color(250, 250, 250), Color(35, 35, 35))

        // 使用BorderLayout，描述区域在顶部，滚动面板在中心
        val headerPanel = JPanel(BorderLayout())
        headerPanel.add(iconLabel, BorderLayout.WEST)
        headerPanel.add(tipsPanel, BorderLayout.CENTER)
        headerPanel.background = Constants.Color.PANEL_BACKGROUND

        val container = JPanel(BorderLayout())
        titlePanelContainer.removeAll()
        titleButtonPanel.removeAll()
        titlePanelContainer.add(titlePanel, BorderLayout.CENTER)
        titlePanelContainer.add(titleButtonPanel, BorderLayout.EAST)
        if (hasShowDiff) {
            titleButtonPanel.add(diffButton)
        }
        titleButtonPanel.add(titleIconButton)
        container.add(titlePanelContainer, BorderLayout.NORTH)
        container.add(scrollPane, BorderLayout.CENTER)

        val borderColor = JBColor(Color(200, 200, 200), Color(70, 70, 70)) // 边框颜色
        container.border = JBUI.Borders.compound(
            createRoundedBorder(borderColor), // 圆角外边框
            JBUI.Borders.empty(8, 10, 8, 10) // 减少内边距
        )
        container.background = Constants.Color.PANEL_BACKGROUND

        add(headerPanel, BorderLayout.NORTH)
        add(container, BorderLayout.CENTER)
    }


    // 封装动态添加/移除逻辑
    fun refreshButtonVisible(titleButton: JButton?,diffButton: JButton?) {
        titleButtonPanel.removeAll()
        diffButton?.let {
            this.diffButton = it
            titleButtonPanel.add(it)
        }
        titleButton?.let {
            this.titleIconButton = it
            titleButtonPanel.add(it)
        }
        titleButtonPanel.revalidate()
        titleButtonPanel.repaint()
    }


    fun setTipsContent(toolHeader: ToolHeader) {
        this.iconLabel.icon = toolHeader.icon

        val htmlText = """
        <html>
          <body>
            <p style='margin-top: 4px; margin-bottom: 4px; font-weight: bold; margin-left: 4px;'>${toolHeader.text}</p>
          </body>
        </html>
    """.trimIndent()

        this.tipsPanel.text = htmlText
    }

    fun setTitleContent(title: String) {
        val bgColor = JBColor(Color(245, 245, 245), Color(45, 45, 45))
        val fgColor = JBColor(Color(45, 45, 45), Color(245, 245, 245))

        val htmlText = """
        <html>
          <body>
            <p style='margin-top: 0px; margin-bottom: 0px; font-size: 9px; background-color: ${toHex(bgColor)};color: ${toHex(fgColor)};font-family: monospace;'>${title}</p>
          </body>
        </html>
    """.trimIndent()

        this.titlePanel.text = htmlText
    }

    // 转换为css十六进制
    private fun toHex(color: Color): String {
        return String.format("#%02x%02x%02x", color.red, color.green, color.blue)
    }

    fun setTitleIcon(icon: Icon) {
        this.titleIconButton.icon = icon
        this.titleIconButton.margin = JBUI.emptyInsets()
        this.titleIconButton.preferredSize = Dimension(16, 16)
    }

    fun addTitleActionListener(toggleAction: () -> Unit) {
        this.titleIconButton.addActionListener { toggleAction() }
    }

    fun addDiffActionListener(diffAction: () -> Unit) {
        this.diffButton.addActionListener { diffAction() }
    }

    /** 在编辑器中打开文件 */
    fun openFileInEditor(fileName: String, basePath: String) {
        ApplicationManager.getApplication().invokeLater {
            try {
                // 策略：先尝试按文件路径打开，如果文件不存在，再尝试用 FQN 方式打开
                val opened = tryOpenFileByPath(fileName, basePath)
                if (!opened) {
                    // 文件不存在，尝试作为 FQN 打开
                    tryOpenFileByFQN(fileName)
                }
            } catch (e: Exception) {
                LOG.warn("打开文件失败", e)
            }
        }
    }

    /** 尝试通过文件路径打开文件，返回是否成功 */
    private fun tryOpenFileByPath(fileName: String, basePath: String): Boolean {
        // 构建完整文件路径
        val fullPath = buildFilePath(fileName, basePath)
        val file = File(fullPath)

        return if (file.exists() && file.isFile) {
            val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file)
            if (virtualFile != null) {
                FileEditorManager.getInstance(project).openFile(virtualFile, true)
                true
            } else {
                false
            }
        } else {
            false
        }
    }

    /** 尝试通过 FQN 打开文件，返回是否成功 */
    protected fun tryOpenFileByFQN(fqn: String): Boolean {
        val psiClass = JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project))

        return if (psiClass != null) {
            // 通过 navigationElement 获取源码文件而不是反编译的文件
            val psiFile = when (val navElement = psiClass.navigationElement) {
                is PsiClass -> navElement.containingFile
                is PsiFile -> navElement
                else -> psiClass.containingFile
            }
            val virtualFile = psiFile?.virtualFile
            if (virtualFile != null) {
                FileEditorManager.getInstance(project).openFile(virtualFile, true)
                true
            } else {
                false
            }
        } else {
            false
        }
    }

    /** 构建完整文件路径 */
    private fun buildFilePath(fileName: String, basePath: String): String {
        val cleanFileName = fileName.trim().removePrefix("  ") // 移除缩进
        val filePath = if (basePath.isNotEmpty()) {
            "$basePath/$cleanFileName"
        } else {
            cleanFileName
        }
        return normalizeFilePath(filePath)
    }

    /** 获取当前项目 */
    private fun getCurrentProject(): Project? {
        val projects = ProjectManager.getInstance().openProjects
        return if (projects.isNotEmpty()) {
            projects[0] // 返回第一个打开的项目
        } else {
            null
        }
    }

    /** 清空所有结果 */
    open fun clearResults() {
        contentPanel.removeAll()
        tipsPanel.removeAll()
        iconLabel.removeAll()
        contentPanel.revalidate()
        contentPanel.repaint()
    }

    fun getDisplayPath(project: Project, filePath: String): String {
        val basePath = project.basePath ?: return filePath
        val relative = toRelativePath(filePath, basePath)
        return when {
            relative.isEmpty() -> "./"
            else -> relative
        }
    }


    abstract fun setContent(
        filePath: String,
        content: String,
        params: Map<String, JsonElement>,
        toolHeader: ToolHeader,
        isPartial: Boolean
    )
}

class ToolPanelFactory(private val project: Project) {

    private val panelCache = mutableMapOf<String, ToolPanel>()

    fun createPanel(eventId: String, type: UiToolName, segment: ToolSegment): ToolPanel {
        val panel = when (type) {
            UiToolName.NEW_FILE_CREATED -> FileCreatePanel(project)
            UiToolName.LIST_FILES_RECURSIVE -> FileListPanel(project)
            UiToolName.SEARCH_FILES -> {
                val outputMode = segment.params["outputMode"]?.jsonPrimitive?.contentOrNull ?: "content"
                when (outputMode) {
                    "files_with_matches" -> FileListPanel(project)
                    else -> FileCommonPanel(project)
                }
            }
            UiToolName.GLOB_FILES -> FileListPanel(project)
            UiToolName.RESOLVE_CLASS_NAME -> ResolveClassNamePanel(project)
            UiToolName.LIST_IMPLEMENTATIONS -> ListImplementationsPanel(project)
            UiToolName.READ_FILE -> FileReadPanel(project)
            UiToolName.RUN_COMMAND -> FileBashPanel(project)
            UiToolName.EDITED_EXISTING_FILE -> FileCreatePanel(project)
            UiToolName.TODO_UPDATE -> TodoListPanel(project)
            UiToolName.EXCEL_READ -> FileCommonPanel(project)
            UiToolName.ASK_USER_QUESTION -> AskUserQuestionPanel(project)
            UiToolName.MCP_TOOL -> McpToolPanel(project)
            UiToolName.USER_EDIT -> UserEditPanel(project)
            UiToolName.EXIT_PLAN_MODE -> FileCommonPanel(project)
            else -> FileCommonPanel(project)
        }
        panelCache[eventId] = panel // 或 panelCache.put(type, panel)
        return panel
    }

    fun getPanel(eventId: String): ToolPanel? {
        return panelCache[eventId]
    }

    fun removeAllPanel() {
        panelCache.clear()
    }

}
