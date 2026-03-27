package com.qifu.ui.smartconversation.panels

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.comparison.ComparisonManager
import com.intellij.diff.comparison.ComparisonPolicy
import com.intellij.diff.editor.ChainDiffVirtualFile
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.diff.util.DiffUserDataKeys
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.qifu.agent.parser.ToolHeader
import com.qifu.utils.isFilePathInProject
import com.qihoo.finance.lowcode.common.util.Icons
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rtextarea.RTextArea
import org.fife.ui.rtextarea.RTextScrollPane
import java.awt.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.JButton
import javax.swing.JPanel

/**
 * 文件创建操作面板
 * @author weiyichao
 * @date 2025-10-03
 **/
class FileCreatePanel(private val project: Project) : ToolPanel(project) {

    var filePath:String? = null
    var resultPanel: JPanel? = null
    var resultTextArea: RTextArea? = null

    init {
        addTitleClickNavigation()
    }

    /** 添加单个文件的显示组 */
    private fun updateToolContent(fiePath: String, content: String, shouldExpand: Boolean) {
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
            // 文件内容面板 - 模拟代码编辑器样式
            resultPanel = createCodeEditorPanel(content, filePath = fiePath)
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
        this.filePath = filePath
        // 更新描述区域状态
        setTipsContent(toolHeader)
        val shouldExpand = isPartial && content.isNotEmpty()
        setTitleIcon(if (shouldExpand) Icons.CollapseAll else Icons.ExpandAll)
        updateToolContent(filePath, content, shouldExpand)

        val filePathInProject = isFilePathInProject(filePath, project)
        val displayTitle =  filePath.takeIf { !filePathInProject }?:filePath.substringAfterLast("/")
        setTitleContent("📄 $displayTitle")

//        refreshButtonVisible(titleIconButton, null)
        if (!isPartial) {
            val oldString = (params["oldString"] as JsonPrimitive).contentOrNull ?: ""
            val newString = (params["newString"] as JsonPrimitive).contentOrNull ?: ""

            val diffAction: () -> Unit = {
                DiffWindowHolder.showDiffView(project, filePath.substringAfterLast("/") + "-read-only", oldString, newString,null, null, false)
            }
            refreshButtonVisible(titleIconButton, JButton("diff").apply {
                toolTipText = "查看文件差异" // 鼠标悬浮提示文字
                isContentAreaFilled = false   // 去掉按钮背景（可选）
                isBorderPainted = false       // 去掉按钮边框（可选）
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) // 鼠标悬浮变小手（可选）
                icon = Icons.AI_DIFF
                margin = Insets(0, 0, 0, 0)
                isFocusable = false
                border = null
                background = Color(45, 45, 45)
                preferredSize = Dimension(40, 16)
            })
            addDiffActionListener { diffAction() }
        }

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

    /**
     * 为标题添加点击导航功能，点击后打开对应文件
     */
    private fun addTitleClickNavigation() {
        titlePanel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        // 移除之前的监听器（避免重复添加）
        titlePanel.mouseListeners.forEach { titlePanel.removeMouseListener(it) }
        titlePanel.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                filePath ?: return
                ApplicationManager.getApplication().invokeLater {
                    val file = java.io.File(filePath!!)
                    val virtualFile = if (file.exists()) {
                        LocalFileSystem.getInstance().findFileByIoFile(file)
                    } else {
                        // 文件可能刚创建，尝试刷新后查找
                        LocalFileSystem.getInstance().refreshAndFindFileByPath(filePath!!)
                    }
                    if (virtualFile != null) {
                        val descriptor = OpenFileDescriptor(project, virtualFile)
                        descriptor.navigate(true)
                    }
                }
            }
        })
    }
}

object DiffWindowHolder {
    // 文件名 → 对应的 diff 窗口
    private val openDiffs = mutableMapOf<String, DiffSession>()
    // taskId -> 文件名
    private val askFileNameMap = mutableMapOf<String, String>()

    data class DiffSession(
        val oldFile: LightVirtualFile,
        val newFile: LightVirtualFile,
        val disposable: Disposable,
        var isModified: Boolean = false, // 是否被修改
        val absolutePath: String? = null, // 文件绝对路径
        val project: Project
    )

    /**
     * 打开或更新 diff 窗口
     */
    fun showDiffView(
        project: Project, name: String, oldString: String,
        newString: String, taskId: String?, filePath: String?, isInAskFlow: Boolean) {
        val requestKey = "Jarvis Diff: $name"

        val existingSession = openDiffs[requestKey]
        if (existingSession != null) {
            // 🔄 已存在 → 更新文本内容
            updateExistingDiffContent(project, existingSession, oldString, newString)
            return
        }

        val timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
        val fileName = "temp_${timestamp}_$name"

        // 🆕 创建新的 diff 视图
        val diffDisposable = Disposer.newDisposable(requestKey)
        val oldVirtualFile = LightVirtualFile(name, oldString)
        val newVirtualFile = LightVirtualFile(fileName, newString)

        val diffRequest = createDiffRequest(project, newVirtualFile, oldVirtualFile, requestKey, taskId)

        openDiffs[requestKey] = DiffSession(oldVirtualFile, newVirtualFile, diffDisposable, absolutePath = filePath, project = project)

        DiffManager.getInstance().showDiff(project, diffRequest)

        if (isInAskFlow && taskId != null) {
            askFileNameMap[taskId] = requestKey
            // 📝 监听新文件修改
            val document = FileDocumentManager.getInstance().getDocument(newVirtualFile)
            document?.addDocumentListener(object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    openDiffs[requestKey]?.let { session ->
                        session.isModified = (newString != document.text)
                    }
                }
            }, diffDisposable) // 绑定到 disposable,避免内存泄漏
        }

        // 🧹 注册关闭监听，自动清理
        val connection = project.messageBus.connect(diffDisposable)
        connection.subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun fileClosed(source: com.intellij.openapi.fileEditor.FileEditorManager, file: VirtualFile) {
                    if (file is ChainDiffVirtualFile) {
                        file.chain.requests.forEach { chainDiffVirtualFile ->
                            if (chainDiffVirtualFile is SimpleDiffRequestChain.DiffRequestProducerWrapper) {
                                if (requestKey == chainDiffVirtualFile.request.title) {
                                    openDiffs.remove(requestKey)
                                    askFileNameMap.remove(taskId)
                                    Disposer.dispose(diffDisposable)
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    fun createDiffRequest(
        project: Project,
        tempFile: VirtualFile,
        virtualFile: VirtualFile,
        diffTitle: String,
        taskId: String?
    ): SimpleDiffRequest {
        val diffContentFactory = DiffContentFactory.getInstance()
        val leftFileDiffContent = diffContentFactory.create(project, virtualFile).apply {
            putUserData(DiffUserDataKeys.FORCE_READ_ONLY, true)
        }
        // taskId为空的话，为只读
        val rightFileDiffContent = if (taskId == null) {
            diffContentFactory.create(project, tempFile).apply {
                putUserData(DiffUserDataKeys.FORCE_READ_ONLY, true)
            }
        } else {
            diffContentFactory.create(project, tempFile)
        }

        return SimpleDiffRequest(
            diffTitle,
            leftFileDiffContent,
            rightFileDiffContent,
            virtualFile.name,
            "Jarvis suggested code"
        )
    }

    /**
     * 🔁 更新现有 diff 窗口的内容（实时更新）
     */
    private fun updateExistingDiffContent(
        project: Project,
        session: DiffSession,
        newOldText: String,
        newNewText: String
    ) {
        ApplicationManager.getApplication().invokeLater {
            val docManager = FileDocumentManager.getInstance()
            val oldDoc = docManager.getDocument(session.oldFile)
            val newDoc = docManager.getDocument(session.newFile)

            if (oldDoc != null && newDoc != null) {
                ApplicationManager.getApplication().runWriteAction {
                    oldDoc.setText(newOldText)
                    newDoc.setText(newNewText)
                }
            }
        }
    }

    /**
     * 关闭指定的 diff view
     * @param taskId 任务Id
     */
    fun closeDiffView(taskId: String?) {
        if (taskId == null) {
            return
        }
        val requestKey = askFileNameMap[taskId]
        if (requestKey == null) {
            return
        }

        ApplicationManager.getApplication().invokeLater {
            runCatching {
                askFileNameMap.remove(taskId)
                val session = openDiffs.remove(requestKey) ?: return@invokeLater

                // 窗口关闭
                closeDiffWindow(session.project, requestKey)

                // 资源销毁
                Disposer.dispose(session.disposable)
            }
        }
    }

    /**
     * 关闭 diff 窗口
     */
    private fun closeDiffWindow(project: Project, requestKey: String?) {
        val fileEditorManager = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project)

        fileEditorManager.openFiles
            .filterIsInstance<ChainDiffVirtualFile>()
            .firstOrNull { file ->
                file.chain.requests.any { request ->
                    request is SimpleDiffRequestChain.DiffRequestProducerWrapper &&
                    request.request.title == requestKey
                }
            }
            ?.let { fileEditorManager.closeFile(it) }
    }

    /**
     * 是否有进行修改
     */
    fun isModified(askId: String): Boolean {
        // 1. 根据askId获取suggest文件
        val session = openDiffs[askFileNameMap[askId]] ?: return false
        // 判断文件是否有修改
        return session.isModified
    }

    /**
     *  获取用户修改的代码diff
     */
    fun showUserModifications(askId: String, isShowAllDiff: Boolean): String {
        val session = openDiffs[askFileNameMap[askId]] ?: return ""
        val newDoc = FileDocumentManager.getInstance().getDocument(session.newFile) ?: return ""

        var originalSuggested = ""
        // 获取原始建议的代码
        if (isShowAllDiff) {
            originalSuggested = session.oldFile.content.toString()
        } else {
            originalSuggested = session.newFile.content.toString()
        }

        // 获取用户修改后的代码
        val userModified = newDoc.text

        // 使用 IntelliJ 的 Diff 引擎计算差异
        val comparisonManager = ComparisonManager.getInstance()
        val fragments = comparisonManager.compareLines(
            originalSuggested,
            userModified,
            ComparisonPolicy.DEFAULT,
            DumbProgressIndicator.INSTANCE
        )

        // 构建修改摘要
        val contextLines = 4
        val oldLines = originalSuggested.lines()
        val newLines = userModified.lines()
        val modifications = buildString {
            fragments.forEach { fragment ->
                val contextStart1 = maxOf(0, fragment.startLine1 - contextLines)
                val contextEnd1 = minOf(oldLines.size, fragment.endLine1 + contextLines)
                val contextStart2 = maxOf(0, fragment.startLine2 - contextLines)
                val contextEnd2 = minOf(newLines.size, fragment.endLine2 + contextLines)

                // 计算上下文范围
                appendLine("@@ -${contextStart1 + 1},${contextEnd1 - contextStart1} +${contextStart2 + 1},${contextEnd2 - contextStart2} @@")

                oldLines.subList(contextStart1, fragment.startLine1)
                    .forEach { appendLine(" $it") }

                oldLines.subList(fragment.startLine1, fragment.endLine1)
                    .forEach { appendLine("-$it") }

                newLines.subList(fragment.startLine2, fragment.endLine2)
                    .forEach { appendLine("+$it") }

                val postContextEnd = minOf(contextEnd1 - fragment.endLine1, contextEnd2 - fragment.endLine2)
                oldLines.subList(fragment.endLine1, fragment.endLine1 + postContextEnd)
                    .forEach { appendLine(" $it") }
            }
        }
        return modifications
    }

    /**
     * 应用用户修改的代码
     */
    fun applyUserModifications(askId: String) {
        val session = openDiffs[askFileNameMap[askId]] ?: return
        val actualFilePath = session.absolutePath
        ApplicationManager.getApplication().runWriteAction {
            // 获取编辑后的内容
            val newDoc = FileDocumentManager.getInstance().getDocument(session.newFile) ?: return@runWriteAction
            val newContent = newDoc.text

            if (actualFilePath != null) {
                // 找到实际的物理文件
                val actualFile = com.intellij.openapi.vfs.LocalFileSystem.getInstance()
                    .findFileByPath(actualFilePath)

                if (actualFile != null) {
                    FileDocumentManager.getInstance().getDocument(actualFile)?.setText(newContent)
                } else {
                    val file = java.io.File(actualFilePath)
                    file.parentFile?.mkdirs()
                    file.writeText(newContent)
                    LocalFileSystem.getInstance().refreshAndFindFileByPath(actualFilePath)
                }
            }
        }
    }
}

