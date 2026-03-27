package com.qifu.ui.smartconversation.panels

import com.intellij.icons.AllIcons
import com.intellij.icons.AllIcons.Actions
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.JBColor
import com.intellij.ui.RoundedIcon
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.util.application
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import com.qifu.ui.smartconversation.textarea.ConversationTagProcessor
import com.qifu.ui.smartconversation.textarea.ConversationTagProcessor.Companion.formatConversation
import com.qifu.ui.smartconversation.textarea.lookup.action.HistoryActionItem
import com.qifu.ui.utils.MarkdownUtil
import com.qifu.utils.toRelativePath
import com.qihoo.finance.lowcode.common.configuration.UserInfoPersistentState
import com.qihoo.finance.lowcode.common.constants.Constants
import com.qihoo.finance.lowcode.common.util.Icons
import com.qihoo.finance.lowcode.smartconversation.actions.IconActionButton
import com.qihoo.finance.lowcode.smartconversation.conversations.Message
import com.qihoo.finance.lowcode.smartconversation.conversations.TaskMessageResponseBody
import com.qihoo.finance.lowcode.smartconversation.panels.ImageAccordion
import com.qihoo.finance.lowcode.smartconversation.panels.SelectedFilesAccordion
import com.qihoo.finance.lowcode.smartconversation.settings.GeneralSettings
import com.qihoo.finance.lowcode.smartconversation.settings.SmartToolWindowContentManager
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.function.Supplier
import javax.swing.*

class UserMessagePanel(
    private val project: Project,
    private val message: Message,
    private val parentDisposable: Disposable
) : BaseMessagePanel() {

    private data class CheckpointFileItem(
        val displayPath: String,
        val absolutePath: String
    )

    init {
        border = JBUI.Borders.customLine(JBColor.border(), 1, 0, 1, 0)
//        background = ColorUtil.brighter(getBackground(), 2)
        background = Constants.Color.USER_MESSAGE_BACKGROUND

        setupAdditionalContext()
        setupResponseBody()
    }

    private fun getUserIcon(): Icon {
        try {
            val avatarBase64 = GeneralSettings.getCurrentState().avatarBase64
            return if (avatarBase64.isNullOrEmpty()) {
                Icons.User
            } else {
                val originalIcon = ImageIcon(Base64.getDecoder().decode(avatarBase64))
                val resizedImage = originalIcon.image.getScaledInstance(
                    24,
                    24,
                    Image.SCALE_SMOOTH
                )
                RoundedIcon(resizedImage, 1.0)
            }
        } catch (ex: Exception) {
            return Icons.User
        }
    }

    override fun createDisplayNameLabel(): JBLabel {
        return JBLabel(
            UserInfoPersistentState.getUserInfo().getNickName(),
            getUserIcon(),
            SwingConstants.LEADING
        )
            .setAllowAutoWrapping(true)
            .withFont(JBFont.label().asBold())
            .apply {
                iconTextGap = 6
            }
    }

    fun addReloadAction(onReload: Runnable) {
        addIconActionButton(
            IconActionButton(
                object : AnAction(
                    "Reload Message",
                    "Reload message",
                    Actions.Refresh
                ) {
                    override fun actionPerformed(e: AnActionEvent) {
                        onReload.run()
                    }
                },
                "RELOAD"
            )
        )
    }

    fun addCheckpointAction(changedFilesProvider: Supplier<List<String>>, onContinue: Runnable) {
        addIconActionButton(
            IconActionButton(
                object : AnAction(
                    "Revert",
                    "Restore file changes and rollback conversation context",
                    Icons.ROLLBACK
                ) {
                    override fun actionPerformed(e: AnActionEvent) {
                        val changedFiles = changedFilesProvider.get()
                        if (changedFiles.isEmpty()) {
                            setCheckpointEnabled(false)
                            return
                        }
                        showCheckpointPopup(changedFiles, e, onContinue)
                    }
                },
                "CHECKPOINT"
            )
        )
        setCheckpointEnabled(false)
    }

    fun setCheckpointEnabled(enabled: Boolean) {
        setActionEnabled("CHECKPOINT", enabled)
    }

    private fun showCheckpointPopup(changedFiles: List<String>, event: AnActionEvent, onContinue: Runnable) {
        val basePath = project.basePath
        val items = changedFiles.map { absPath ->
            val display = if (!basePath.isNullOrBlank()) toRelativePath(absPath, basePath) else absPath
            CheckpointFileItem(display, absPath)
        }
        val model = DefaultListModel<CheckpointFileItem>().apply {
            items.forEach { addElement(it) }
        }

        val list = JBList(model).apply {
            visibleRowCount = 8
            selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
            fixedCellHeight = JBUI.scale(44)
            border = JBUI.Borders.empty(4)
            background = JBColor(0xFAFAFA, 0x2E2F31)
            cellRenderer = object : DefaultListCellRenderer() {
                override fun getListCellRendererComponent(
                    list: JList<*>,
                    value: Any?,
                    index: Int,
                    isSelected: Boolean,
                    cellHasFocus: Boolean
                ): Component {
                    val item = value as? CheckpointFileItem
                    val comp = super.getListCellRendererComponent(
                        list,
                        item?.displayPath ?: value,
                        index,
                        isSelected,
                        cellHasFocus
                    )
                    if (comp is JLabel && item != null) {
                        val escapedDisplay = StringUtil.escapeXmlEntities(item.displayPath)
                        val escapedAbsolute = StringUtil.escapeXmlEntities(item.absolutePath)
                        val extName = item.displayPath.substringAfterLast("/")
                        comp.icon = FileTypeManager.getInstance().getFileTypeByFileName(extName).icon
                        comp.border = JBUI.Borders.empty(4, 8)
                        comp.toolTipText = item.absolutePath
                        comp.text = "<html><div><b>$escapedDisplay</b></div><div style='color:#8A8F98;'>$escapedAbsolute</div></html>"
                    }
                    return comp
                }
            }
        }

        val titlePanel = BorderLayoutPanel().apply {
            isOpaque = false
            border = JBUI.Borders.empty(0, 0, 8, 0)
            addToLeft(
                JBLabel("Restore checkpoint").apply {
                    font = JBFont.label().asBold()
                }
            )
            addToRight(
                JBLabel("${items.size} file${if (items.size > 1) "s" else ""}").apply {
                    font = JBFont.small()
                    foreground = JBColor.GRAY
                }
            )
        }
        val description = JBLabel(
            "Only files and runtime context will be restored; message bubbles remain visible."
        ).apply {
            font = JBFont.small()
            foreground = JBColor.GRAY
            border = JBUI.Borders.emptyBottom(8)
        }

        val scrollPane = ScrollPaneFactory.createScrollPane(list).apply {
            preferredSize = Dimension(JBUI.scale(560), JBUI.scale(220))
            border = JBUI.Borders.customLine(JBColor.border(), 1)
        }

        val headerPanel = BorderLayoutPanel().apply {
            isOpaque = false
            addToTop(titlePanel)
            addToCenter(description)
        }

        val contentPanel = JPanel(BorderLayout()).apply {
            isOpaque = true
            border = JBUI.Borders.empty(12)
            add(headerPanel, BorderLayout.NORTH)
            add(scrollPane, BorderLayout.CENTER)
        }

        val footerPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 8, 0)).apply {
            isOpaque = false
            border = JBUI.Borders.emptyTop(10)
        }
        contentPanel.add(footerPanel, BorderLayout.SOUTH)

        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(contentPanel, list)
            .setFocusable(true)
            .setRequestFocus(true)
            .setCancelOnClickOutside(true)
            .setResizable(true)
            .setMovable(false)
            .createPopup()

        val cancelButton = JButton("Cancel").apply {
            addActionListener { popup.cancel() }
        }
        val continueButton = JButton("Continue").apply {
            isDefaultCapable = true
            addActionListener {
                popup.cancel()
                onContinue.run()
            }
        }
        footerPanel.add(cancelButton)
        footerPanel.add(continueButton)

        val anchor = event.inputEvent?.component as? JComponent
        if (anchor != null) {
            popup.show(RelativePoint.getSouthWestOf(anchor))
        } else {
            popup.showCenteredInCurrentWindow(project)
        }
    }

    private fun setupAdditionalContext() {
        val additionalContextPanel = getAdditionalContextPanel(project, message)
        if (additionalContextPanel != null) {
            body.addToTop(additionalContextPanel)
        }
    }

    private fun setupResponseBody() {
        addContent(
            TaskMessageResponseBody(project, true, false, false, false, parentDisposable)
                .withResponse(message.prompt)
        )
    }

    private fun getAdditionalContextPanel(project: Project, message: Message): JPanel? {
        val imageFilePaths = message.imageFilePaths ?: emptyList()
        val referencedFilePaths = message.referencedFilePaths ?: emptyList()
        if (imageFilePaths.isEmpty() && referencedFilePaths.isEmpty()) {
            return null
        }

        return BorderLayoutPanel().apply {
            isOpaque = false

            val additionalContextPanel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
            }
            addToTop(additionalContextPanel)

            message.conversationsHistoryIds?.let { ids ->
                additionalContextPanel.add(JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)
                    isOpaque = false
                    ids.forEach {
                        ConversationTagProcessor.getConversation(it)?.let { conversation ->
                            val title = HistoryActionItem.getConversationTitle(conversation)
                            val titleLink = ActionLink(title) {
                                project.service<SmartToolWindowContentManager>()
                                    .displayConversation(conversation)
                            }.apply {
                                icon = AllIcons.General.Balloon
                                toolTipText =
                                    MarkdownUtil.convertMdToHtml(
                                        formatConversation(conversation)
                                    )
                            }
                            add(BorderLayoutPanel().addToLeft(titleLink).andTransparent())
                        }
                    }
                })
            }

            if (referencedFilePaths.isNotEmpty()) {
                application.executeOnPooledThread {
                    val links = referencedFilePaths
                        .mapNotNull {
                            LocalFileSystem.getInstance().findFileByPath(it)
                        }
                        .map {
                            val actionLink = ActionLink(
                                Paths.get(it.path).fileName.toString(),
                                ActionListener { _: ActionEvent ->
                                    FileEditorManager.getInstance(project)
                                        .openFile(Objects.requireNonNull(it), true)
                                })
                            actionLink.icon =
                                if (it.isDirectory) AllIcons.Nodes.Folder else it.fileType.icon
                            actionLink
                        }
                        .toList()
                    runInEdt {
                        additionalContextPanel.add(SelectedFilesAccordion(links))
                    }
                }
            }

            imageFilePaths.forEach { imageFilePath ->
                if (imageFilePath.isNotEmpty()) {
                    try {
                        val path = Paths.get(imageFilePath)
                        additionalContextPanel.add(
                            ImageAccordion(path.fileName.toString(), Files.readAllBytes(path))
                        )
                    } catch (e: IOException) {
                        additionalContextPanel.add(
                            JBLabel(
                                "<html><small>Unable to load image $imageFilePath</small></html>",
                                AllIcons.General.Error,
                                SwingConstants.LEFT
                            )
                        )
                    }
                }
            }
        }
    }

    private fun createAdditionalContextPanel(title: String, component: JComponent): JPanel {
        return BorderLayoutPanel().apply {
            isOpaque = false
            addToTop(BorderLayoutPanel().apply {
                isOpaque = false
                border = JBUI.Borders.empty(8, 0)
                addToLeft(JBLabel(title).withFont(JBUI.Fonts.miniFont()))
            })
            addToCenter(BorderLayoutPanel().apply {
                isOpaque = false
                addToLeft(component)
            })
        }
    }
}
