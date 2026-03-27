package com.qifu.ui.smartconversation.panels

import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint

import com.intellij.ui.components.JBLabel
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.qifu.services.AgentService
import com.qifu.config.AgentSettings
import com.qifu.ui.settings.skills.SkillDialogActions
import com.qifu.ui.smartconversation.SearchReplaceToggleAction
import com.qifu.ui.smartconversation.SkillsSelectionComponent
import com.qifu.ui.smartconversation.settings.configuration.ChatMode
import com.qifu.ui.smartconversation.textarea.PromptTextField
import com.qifu.ui.smartconversation.textarea.header.*
import com.qifu.ui.smartconversation.textarea.lookup.LookupActionItem
import com.qifu.ui.smartconversation.textarea.lookup.action.ModelComboBoxAction
import com.qifu.ui.smartconversation.sse.AgentCompletionRequestService
import com.qifu.ui.utils.EditorUtil
import com.qifu.utils.SkillConfig
import com.qifu.utils.coroutines.DisposableCoroutineScope
import com.qifu.utils.image.ImageUtil
import com.qihoo.finance.lowcode.common.constants.Constants
import com.qihoo.finance.lowcode.common.util.Icons
import com.qihoo.finance.lowcode.common.util.NotifyUtils
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings
import com.qihoo.finance.lowcode.smartconversation.actions.IconActionButton
import com.qihoo.finance.lowcode.smartconversation.configuration.JarvisKeys.IMAGE_ATTACHMENT_FILE_PATH
import git4idea.GitCommit
import java.awt.*
import java.awt.geom.Area
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.*
import kotlin.jvm.JvmOverloads

class UserInputPanel @JvmOverloads constructor(
    private val project: Project,
    parentDisposable: Disposable,
    private val tagManager: TagManager,
    private val taskId: String,
    private val onSubmit: (String) -> Unit,
    private val onStop: () -> Unit,
    private val includeSelectedEditorContent: Boolean = true
) : JPanel(BorderLayout()) {

    companion object {
        private const val CORNER_RADIUS = 16
    }

    private var chatMode: ChatMode = ChatMode.AGENT
    private val disposableCoroutineScope = DisposableCoroutineScope()
    private val promptTextField =
        PromptTextField(
            project,
            tagManager,
            imageActionSupported(),
            taskId,
            ::updateUserTokens,
            ::handleBackSpace,
            ::handleLookupAdded,
            ::handleSubmit,
            ::handleImagePasted
        )
    private val searchReplaceToggle = SearchReplaceToggleAction(this)
    private val userInputHeaderPanel =
        UserInputHeaderPanel(
            project,
            tagManager,
            promptTextField
        )
    private val submitButton = IconActionButton(
        object : AnAction(
            "Send Message",
            "Send message",
            IconUtil.scale(Icons.SEND, null, 0.85f)
        ) {
            override fun actionPerformed(e: AnActionEvent) {
                handleSubmit(promptTextField.getExpandedText())
            }
        },
        "SUBMIT"
    )
    private val stopButton = IconActionButton(
        object : AnAction(
            "Stop",
            "Stop Completion",
            AllIcons.Actions.Suspend
        ) {
            override fun actionPerformed(e: AnActionEvent) {
                onStop()
            }
        },
        "STOP"
    ).apply { isEnabled = false }

    private val modelComboBox = ModelComboBoxAction(
        this.project,
        ::handleModelChange,
    )


    val text: String
        get() = promptTextField.text

    fun getChatMode(): ChatMode = chatMode

    fun setChatMode(mode: ChatMode) {
        chatMode = mode
        searchReplaceToggle.refreshPresentation()
    }

    fun setPromptText(text: String) {
        promptTextField.text = text
        focusOnPromptEnd()
    }

    fun clearPrompt() {
        promptTextField.clear()
    }

    fun getCurrentModelId(): String = modelComboBox.getCurrentModelId()

    init {
        setupDisposables(parentDisposable)
        setupLayout()
        // 移除自动添加选中内容的逻辑
        // if (includeSelectedEditorContent) {
        //     addSelectedEditorContent()
        // }
    }

    private fun setupDisposables(parentDisposable: Disposable) {
        Disposer.register(parentDisposable, disposableCoroutineScope)
        Disposer.register(parentDisposable, promptTextField)
    }

    private fun setupLayout() {
//        background = service<EditorColorsManager>().globalScheme.defaultBackground
        background = JBColor.lazy {
            service<EditorColorsManager>().globalScheme.defaultBackground
        }
        add(userInputHeaderPanel, BorderLayout.NORTH)
        add(promptTextField, BorderLayout.CENTER)
        add(createFooterPanel(), BorderLayout.SOUTH)
    }

    private fun addSelectedEditorContent() {
        EditorUtil.getSelectedEditor(project)?.let { editor ->
            if (EditorUtil.hasSelection(editor)) {
                tagManager.addTag(
                    EditorSelectionTagDetails(
                        FileDocumentManager.getInstance().getFile(editor.document)!!,
                        editor.selectionModel
                    )
                )
            }
        }
    }

    fun getSelectedTags(): List<TagDetails> {
        return userInputHeaderPanel.getSelectedTags()
    }

    fun setSubmitEnabled(enabled: Boolean) {
        submitButton.isEnabled = enabled
        stopButton.isEnabled = !enabled
    }

    fun addSelection(editorFile: VirtualFile, selectionModel: SelectionModel) {
        addTag(SelectionTagDetails(editorFile, selectionModel))
        promptTextField.requestFocusInWindow()
        selectionModel.removeSelection()
    }

    fun addCommitReferences(gitCommits: List<GitCommit>) {
        runInEdt {
            setCommitPromptIfEmpty(gitCommits)
            addCommitTags(gitCommits)
            focusOnPromptEnd()
        }
    }

    private fun setCommitPromptIfEmpty(gitCommits: List<GitCommit>) {
        if (promptTextField.text.isEmpty()) {
            promptTextField.text = buildCommitPrompt(gitCommits)
        }
    }

    private fun buildCommitPrompt(gitCommits: List<GitCommit>): String {
        return if (gitCommits.size == 1) {
            "Explain the commit `${gitCommits[0].id.toShortString()}`"
        } else {
            "Explain the commits ${gitCommits.joinToString(", ") { "`${it.id.toShortString()}`" }}"
        }
    }

    private fun addCommitTags(gitCommits: List<GitCommit>) {
        gitCommits.forEach { addTag(GitCommitTagDetails(it)) }
    }

    private fun focusOnPromptEnd() {
        promptTextField.requestFocusInWindow()
        promptTextField.editor?.caretModel?.moveToOffset(promptTextField.text.length)
    }

    fun addTag(tagDetails: TagDetails) {
        userInputHeaderPanel.addTag(tagDetails)
        removeTrailingAtSymbol()
    }

    private fun removeTrailingAtSymbol() {
        val text = promptTextField.text
        if (text.endsWith('@')) {
            promptTextField.text = text.dropLast(1)
        }
    }

    fun includeFiles(referencedFiles: MutableList<VirtualFile>) {
        referencedFiles.forEach { userInputHeaderPanel.addTag(FileTagDetails(it)) }
    }

    fun imageActionSupported(): Boolean {
        return isImageActionSupported()
    }

    /**
     * 将文本插入到输入框中
     * @param text 要插入的文本
     * @param focus 是否聚焦到输入框
     */
    fun insertTextToPrompt(text: String, focus: Boolean = true) {
        invokeLater {
            promptTextField.setTextAndFocus(text)
            if (focus) {
                promptTextField.requestFocusInWindow()
            }
        }
    }

    /**
     * 插入代码选择占位符到输入框
     * @param fileName 文件名
     * @param startLine 开始行号
     * @param endLine 结束行号
     * @param selectedCode 选中的代码内容
     */
    fun insertCodeSelectionPlaceholder(fileName: String, startLine: Int, endLine: Int, selectedCode: String) {
        invokeLater {
            promptTextField.insertCodeSelectionPlaceholder(fileName, startLine, endLine, selectedCode)
            promptTextField.requestFocusInWindow()
        }
    }

    /**
     * 插入文件路径占位符到输入框
     * @param fileName 文件名（显示用）
     * @param filePath 文件绝对路径
     */
    fun insertFilePathPlaceholder(fileName: String, filePath: String) {
        invokeLater {
            promptTextField.insertFilePathPlaceholder(fileName, filePath)
            promptTextField.requestFocusInWindow()
        }
    }

    /**
     * 插入文件夹路径占位符到输入框
     * @param folderName 文件夹名（显示用）
     * @param folderPath 文件夹绝对路径
     */
    fun insertFolderPathPlaceholder(folderName: String, folderPath: String) {
        invokeLater {
            promptTextField.insertFolderPathPlaceholder(folderName, folderPath)
            promptTextField.requestFocusInWindow()
        }
    }

    /**
     * 移除 EditorSelectionTagDetails,但保持编辑器中的选中状态
     */
    fun removeEditorSelectionTag() {
        // 查找所有 EditorSelectionTagDetails
        val editorSelectionTags = tagManager.getTags().filterIsInstance<EditorSelectionTagDetails>()
        // 移除所有找到的 EditorSelectionTagDetails
        editorSelectionTags.forEach { tag ->
            tagManager.remove(tag)
        }
    }

    override fun requestFocus() {
        invokeLater {
            promptTextField.requestFocusInWindow()
        }
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        try {
            setupGraphics(g2)
            drawRoundedBackground(g2)
            super.paintComponent(g2)
        } finally {
            g2.dispose()
        }
    }

    private fun setupGraphics(g2: Graphics2D) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    }

    private fun drawRoundedBackground(g2: Graphics2D) {
        val area = createRoundedArea()
        g2.clip = area
        g2.color = background
        g2.fill(area)
    }

    private fun createRoundedArea(): Area {
        val bounds = Rectangle2D.Float(0f, 0f, width.toFloat(), height.toFloat())
        val roundedRect = RoundRectangle2D.Float(
            0f, 0f, width.toFloat(), height.toFloat(),
            CORNER_RADIUS.toFloat(), CORNER_RADIUS.toFloat()
        )
        val area = Area(bounds)
        area.intersect(Area(roundedRect))
        return area
    }

    override fun paintBorder(g: Graphics) {
        val g2 = g.create() as Graphics2D
        try {
            setupGraphics(g2)
            drawRoundedBorder(g2)
        } finally {
            g2.dispose()
        }
    }

    private fun drawRoundedBorder(g2: Graphics2D) {
        val gradient = GradientPaint(
            0f, 0f, Color(64, 128, 255),
            width.toFloat(), height.toFloat(), Color(32, 96, 255)
        )
        g2.paint = gradient
        g2.stroke = BasicStroke(2.5F)
        g2.drawRoundRect(0, 0, width - 1, height - 1, CORNER_RADIUS, CORNER_RADIUS)
    }

    override fun getInsets(): Insets = JBUI.insets(4)

    private fun handleSubmit(text: String) {
        if (text.isNotEmpty() && submitButton.isEnabled) {
            onSubmit(text)
            promptTextField.clear()
        }
    }

    private fun handleModelChange(modelId: String, supportImage: Boolean) {
        promptTextField.updateModelSupportImages(supportImage)
        project.service<AgentCompletionRequestService>().switchModel(taskId, modelId)
    }

    private fun handleImagePasted(image: Image) {
        if (!isImageActionSupported()) {
            NotifyUtils.notify("当前模型不支持多模态识别", NotificationType.WARNING)
            return
        }

        val imagePath = ImageUtil.saveTmpImage(image, taskId, project)
        val imagePaths = project.getUserData(IMAGE_ATTACHMENT_FILE_PATH)?.toMutableList() ?: mutableListOf()
        imagePaths.add(imagePath)
        project.putUserData(IMAGE_ATTACHMENT_FILE_PATH, imagePaths)
        tagManager.addTag(ImageTagDetails(imagePath))
    }

    private fun updateUserTokens(text: String) {
        // totalTokensPanel.updateUserPromptTokens(text)
    }

    private fun handleBackSpace() {
        // 取消空文本时，删除tag的逻辑
//        if (text.isEmpty()) {
//            userInputHeaderPanel.getLastTag()?.let { tagManager.remove(it) }
//        }
    }

    private fun handleLookupAdded(item: LookupActionItem) {
        item.execute(project, this)
    }

    private fun createToolbarSeparator(): JPanel {
        return JPanel().apply {
            isOpaque = true
            background = JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground()
            preferredSize = Dimension(1, 16)
            minimumSize = Dimension(1, 16)
            maximumSize = Dimension(1, 16)
        }
    }


    private fun createFooterPanel(): JPanel {
        // TODO: 初始化模型需要取会话存储的数据
        val searchReplaceToggleComponent =
            searchReplaceToggle.createCustomComponent(ActionPlaces.UNKNOWN)
        val skillsToggle = SkillsSelectionComponent(project).getComponent()

        // 左侧工具栏面板（可压缩）
        val leftToolbarPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            add(modelComboBox.createCustomComponent(ActionPlaces.UNKNOWN))
            add(Box.createHorizontalStrut(JBUI.scale(4)))
            add(createToolbarSeparator())
            add(Box.createHorizontalStrut(JBUI.scale(4)))
            add(searchReplaceToggleComponent)
            add(Box.createHorizontalStrut(JBUI.scale(4)))
            add(createToolbarSeparator())
            add(Box.createHorizontalStrut(JBUI.scale(4)))
            add(skillsToggle)
        }

        // 右侧按钮面板（固定尺寸，不压缩）
        val rightButtonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            add(submitButton)
            add(Box.createHorizontalStrut(JBUI.scale(4)))
            add(stopButton)
            // 设置固定尺寸，防止被压缩
            val buttonWidth = submitButton.preferredSize.width + stopButton.preferredSize.width + JBUI.scale(8)
            val buttonHeight = maxOf(submitButton.preferredSize.height, stopButton.preferredSize.height)
            minimumSize = Dimension(buttonWidth, buttonHeight)
            preferredSize = Dimension(buttonWidth, buttonHeight)
        }

        // 主面板使用 BorderLayout，确保右侧按钮始终可见
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            // 左侧工具栏放在 CENTER，会被压缩
            add(leftToolbarPanel, BorderLayout.CENTER)
            // 右侧按钮放在 EAST，始终保持固定尺寸
            add(rightButtonPanel, BorderLayout.EAST)
        }
    }

    private fun isImageActionSupported(): Boolean {
        return ChatxApplicationSettings.settings().modelSupportImage ?: false
    }
}
