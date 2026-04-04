package com.miracle.ui.core

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.JBColor
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.miracle.agent.AgentMessage
import com.miracle.agent.AgentMessageType
import com.miracle.agent.AskResponse
import com.miracle.agent.JarvisAsk
import com.miracle.agent.Task
import com.miracle.agent.TaskState
import com.miracle.agent.parser.ErrorSegment
import com.miracle.agent.parser.ProposedPlanSegment
import com.miracle.agent.parser.TextSegment
import com.miracle.agent.parser.ToolSegment
import com.miracle.agent.parser.UiToolName
import com.miracle.agent.parser.Segment
import com.miracle.agent.tool.ToolRegistry
import com.miracle.config.JarvisCoreSettings
import com.miracle.services.getCustomModels
import com.miracle.services.getSelectedModel
import com.miracle.services.getSelectedModelId
import com.miracle.services.RelatedFilePredictor
import com.miracle.services.setSelectedModel
import com.miracle.ui.core.ChatTheme.CARD_CHAT
import com.miracle.ui.core.ChatTheme.CARD_WELCOME
import com.miracle.ui.core.ChatTheme.CHAT_BACKGROUND
import com.miracle.ui.core.ChatTheme.MUTED_FOREGROUND
import com.miracle.ui.core.ChatTheme.PANEL_BACKGROUND
import com.miracle.ui.core.ChatTheme.SEND_ICON
import com.miracle.ui.core.ChatTheme.SPLIT_LINE_COLOR
import com.miracle.ui.core.ChatTheme.USER_FOREGROUND
import com.miracle.ui.core.ChatTheme.USER_ICON
import com.miracle.ui.core.ChatTheme.USER_MESSAGE_BACKGROUND
import com.miracle.ui.core.composer.ChatComposerField
import com.miracle.ui.core.composer.ChatComposerSupport
import com.miracle.ui.core.composer.SlashCommandRegistry
import com.miracle.ui.smartconversation.settings.configuration.ChatMode
import com.miracle.ui.smartconversation.textarea.lookup.action.ManageCustomModelsDialog
import com.miracle.utils.ChatHistoryAssistantMessage
import com.miracle.utils.ChatHistoryMessage
import com.miracle.utils.ChatHistoryUserMessage
import com.miracle.utils.Conversation
import com.miracle.utils.ConversationStore
import com.miracle.utils.JsonLineChatHistory
import com.miracle.utils.JsonLineChatMemory
import com.miracle.utils.getDefaultAgentId
import com.miracle.utils.toPosixPath
import com.miracle.utils.toRelativePath
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.UserMessage.userMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Component
import java.awt.Dimension
import java.util.UUID
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JViewport

/**
 * 聊天标签页面板，封装了单次会话的完整交互逻辑，包括消息渲染、用户输入、
 * 模型选择、Agent 事件处理、会话历史加载和回滚等功能。
 *
 * @param project 当前项目实例
 * @param tabId 标签页唯一标识
 * @param initialConversationId 初始会话 ID，为空时显示欢迎页
 * @param onTitleChanged 标题变更回调
 * @param onOpenConversation 打开会话回调
 * @param onArchiveConversation 归档会话回调
 */
class JarvisChatTabPanel(
    private val project: Project,
    private val tabId: String,
    initialConversationId: String?,
    private val onTitleChanged: (String) -> Unit,
    private val onOpenConversation: (String, String) -> Unit,
    private val onArchiveConversation: () -> Unit,
) : JPanel(BorderLayout()), Disposable {

    private companion object {
        /** 流式渲染的最小间隔时间（毫秒） */
        const val STREAM_RENDER_INTERVAL_MS = 200L
        /** 回滚操作在 header 中的客户端属性键 */
        const val ROLLBACK_ACTION_KEY = "jarvis.rollback.action"
    }

    /** 输入框状态枚举，表示空闲、运行中或等待用户回复 */
    private enum class ComposerState {
        IDLE,
        RUNNING,
        AWAITING_REPLY,
    }

    // ── Coroutine & task state ───────────────────────────────────────
    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    /** 当前活跃的 Agent 任务 */
    private var activeTask: Task? = null
    /** 当前活跃的协程任务 */
    private var activeJob: Job? = null
    /** 当前会话 ID */
    private var currentConversationId: String? = initialConversationId
    /** 当前待处理的 Ask 请求 */
    private var pendingAsk: JarvisAsk? = null
    /** 已完成的助手消息卡片映射 */
    private val assistantCards = linkedMapOf<String, AssistantMessageCard>()
    /** 正在流式输出的助手消息卡片映射 */
    private val partialCards = linkedMapOf<String, AssistantMessageCard>()
    /** 用户消息的头部面板映射，用于安装回滚操作 */
    private val userMessageHeaders = linkedMapOf<String, JPanel>()

    // ── Extracted collaborators ──────────────────────────────────────
    private val rollbackSupport = RollbackSupport(project)

    // ── UI components ────────────────────────────────────────────────
    private val centerLayout = CardLayout()
    private val centerPanel = JPanel(centerLayout)

    private val messageContainer = MessageColumnPanel().apply {
        isOpaque = false
        border = JBUI.Borders.empty(14)
    }
    private val messageScrollPane = com.intellij.ui.components.JBScrollPane(messageContainer).apply {
        horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        border = JBUI.Borders.empty()
        viewport.background = CHAT_BACKGROUND
        viewport.scrollMode = JViewport.SIMPLE_SCROLL_MODE
        verticalScrollBar.unitIncrement = JBUI.scale(28)
        verticalScrollBar.blockIncrement = JBUI.scale(180)
    }

    private val scrollManager = ChatScrollManager(messageScrollPane, messageContainer)
    private val renderer = SegmentRendererFactory(
        project = project,
        scrollManager = scrollManager,
        onAskReply = { option ->
            askPanel.setReplyText(option)
            handleAskReply()
        },
        onProposedPlanAction = ::handleProposedPlanAction,
    )

    private val currentConversationTitleLabel = JBLabel("\u65B0\u7684\u4F1A\u8BDD").apply {
        font = JBFont.label().asBold().biggerOn(1f)
    }
    private val currentConversationMetaLabel = JBLabel("\u8FD8\u6CA1\u6709\u5F00\u59CB\u5BF9\u8BDD").apply {
        font = JBFont.small()
        foreground = MUTED_FOREGROUND
    }

    private val modelComboBox = JComboBox<ModelItem>()
    private val chatModeComboBox = JComboBox(ChatMode.entries.toTypedArray())
    private val composerField = ChatComposerField(project, ::handlePrimaryAction)
    private val associatedContextState = AssociatedContextState()
    private val associatedContextHeader = AssociatedContextHeaderPanel(
        project = project,
        onAddRequested = ::showAssociatedFilePicker,
        onRemoveRequested = ::removeAssociatedContextItem,
        onPredictedConfirmed = ::confirmPredictedFile,
    )
    private val composerCheckpointRestorePanel = ComposerCheckpointRestorePanel(
        onRestoreAll = { messageId ->
            currentConversationId?.let { conversationId ->
                rollbackSupport.rollback(conversationId, messageId, activeTask, ::stopActiveTask) {
                    if (currentConversationId == conversationId) {
                        loadConversation(conversationId)
                    }
                }
            }
        },
        onRestoreFile = { messageId, filePath ->
            currentConversationId?.let { conversationId ->
                rollbackSupport.restoreFile(conversationId, messageId, filePath, activeTask, ::stopActiveTask) {
                    if (currentConversationId == conversationId) {
                        refreshComposerCheckpointPanel(messageId)
                    }
                }
            }
        },
        onDiffFile = { messageId, filePath ->
            currentConversationId?.let { conversationId ->
                showCheckpointDiff(conversationId, messageId, filePath)
            }
        },
    )
    private val composerHeaderPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = false
        alignmentX = Component.LEFT_ALIGNMENT
        add(associatedContextHeader)
    }
    private val sendButton = createIconButton(SEND_ICON, "\u53D1\u9001")
    private val stopButton = createIconButton(AllIcons.Actions.Suspend, "\u505C\u6B62")
    private val askPanel = AskPanel { decision, text ->
        handleAskReplyFromPanel(decision, text)
    }

    // ── Initialization ───────────────────────────────────────────────

    init {
        isOpaque = true
        background = CHAT_BACKGROUND
        border = JBUI.Borders.empty()
        Disposer.register(project, this)
        composerField.setDisposedWith(this)
        Disposer.register(this, composerField)

        add(createCenterPanel(), BorderLayout.CENTER)
        add(createPromptPanel(), BorderLayout.SOUTH)
        scrollManager.install()

        refreshModels()
        refreshChatMode()
        updateConversationHeader()

        sendButton.addActionListener { handlePrimaryAction() }
        stopButton.addActionListener { stopActiveTask() }
        applyComposerState(ComposerState.IDLE)

        // 监听编辑器文件选择变化，自动触发关联文件预测
        val editorManager = FileEditorManager.getInstance(project)
        editorManager.addFileEditorManagerListener(
            object : FileEditorManagerListener {
                override fun selectionChanged(event: com.intellij.openapi.fileEditor.FileEditorManagerEvent) {
                    triggerFilePrediction()
                    associatedContextState.removeAutoCodeSelection()
                    updateAssociatedContextHeader()
                }

                override fun fileOpened(source: FileEditorManager, file: com.intellij.openapi.vfs.VirtualFile) {
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        source.getSelectedTextEditor()?.let { installSelectionListenerIfNeeded(it) }
                    }
                }
            },
        )
        // 为已打开的文本编辑器安装选区监听
        editorManager.selectedTextEditor?.let { installSelectionListenerFor(it) }
        editorManager.allEditors.forEach { fe ->
            (fe as? com.intellij.openapi.editor.Editor)?.let { installSelectionListenerFor(it) }
        }

        if (initialConversationId.isNullOrBlank()) {
            showWelcome()
        } else {
            loadConversation(initialConversationId)
        }
    }

    // ── Public API ───────────────────────────────────────────────────

    /**
     * 将纯文本追加到输入框中。
     *
     * @param text 要追加的文本
     */
    fun appendToInput(text: String) {
        appendInsertion(ChatComposerInsertion.PlainText(text))
    }

    /**
     * 将插入内容应用到输入框中。
     *
     * @param insertion 要应用的插入内容
     */
    fun appendInsertion(insertion: ChatComposerInsertion) {
        ApplicationManager.getApplication().invokeLater {
            composerField.applyInsertion(insertion)
            requestFocusForInput()
        }
    }

    /**
     * 追加一个关联文件到上下文中。
     *
     * @param path 文件路径
     */
    fun appendAssociatedFile(path: String) {
        if (path.isBlank()) return
        ApplicationManager.getApplication().invokeLater {
            addAssociatedContextItem(AssociatedContextItem.AssociatedFile(path))
            requestFocusForInput()
        }
    }

    /**
     * 追加一个代码选区到关联上下文中。
     *
     * @param selection 代码选区项
     */
    fun appendAssociatedCodeSelection(selection: AssociatedContextItem.AssociatedCodeSelection) {
        ApplicationManager.getApplication().invokeLater {
            addAssociatedContextItem(selection)
            requestFocusForInput()
        }
    }

    /**
     * 请求输入框获得焦点。
     */
    fun requestFocusForInput() {
        ApplicationManager.getApplication().invokeLater {
            composerField.requestComposerFocus()
        }
    }

    /**
     * 刷新模型选择下拉框的选项列表。
     */
    fun refreshModels() {
        val models = getCustomModels()
        val items = models.map { ModelItem(it.id, it.alias) }
        modelComboBox.model = DefaultComboBoxModel(items.toTypedArray())
        val selectedId = getSelectedModel()?.id
        modelComboBox.selectedItem = items.firstOrNull { it.id == selectedId }
        if (selectedId == null && items.isNotEmpty()) {
            modelComboBox.selectedIndex = 0
            (modelComboBox.selectedItem as? ModelItem)?.let { setSelectedModel(it.id) }
        }
        updateConversationHeader()
    }

    /**
     * 显示历史会话弹窗。
     *
     * @param anchor 弹窗锚点组件，默认为当前面板
     */
    fun showHistoryPopup(anchor: Component = this) {
        HistoryPopupBuilder(project).show(
            anchor = anchor,
            currentConversationId = currentConversationId,
            onOpenConversation = { entry ->
                onOpenConversation(entry.id, entry.title)
            },
        )
    }

    override fun dispose() {
        activeJob?.cancel()
        activeJob = null
        activeTask = null
        TaskState.clearCachedServices(tabId)
        uiScope.cancel()
    }

    // ── Center panel (welcome / chat cards) ──────────────────────────

    /**
     * 创建中央面板，包含欢迎页和聊天页的卡片布局。
     */
    private fun createCenterPanel(): JComponent {
        centerPanel.isOpaque = true
        centerPanel.background = PANEL_BACKGROUND
        centerPanel.add(WelcomePanel.create(::startQuickAsk), CARD_WELCOME)
        centerPanel.add(messageScrollPane, CARD_CHAT)
        return centerPanel
    }

    // ── Prompt / composer panel ──────────────────────────────────────

    /**
     * 创建底部输入面板，包括模型选择器、聊天模式选择器和发送/停止按钮。
     */
    private fun createPromptPanel(): JComponent {
        styleSelectorComboBox(modelComboBox)
        modelComboBox.renderer = ModelItemRenderer()
        modelComboBox.addActionListener {
            val item = modelComboBox.selectedItem as? ModelItem ?: return@addActionListener
            setSelectedModel(item.id)
            updateConversationHeader()
        }

        styleSelectorComboBox(chatModeComboBox)
        chatModeComboBox.renderer = ChatModeRenderer()
        chatModeComboBox.addActionListener {
            val mode = chatModeComboBox.selectedItem as? ChatMode ?: return@addActionListener
            JarvisCoreSettings.getInstance().chatMode = mode
            updateConversationHeader()
        }

        return ChatPromptPanel.create(
            inputComponent = composerField,
            headerComponent = composerHeaderPanel,
            checkpointPanel = composerCheckpointRestorePanel,
            askPanel = askPanel,
            modelComboBox = modelComboBox,
            chatModeComboBox = chatModeComboBox,
            sendButton = sendButton,
            stopButton = stopButton,
        )
    }

    // ── User actions ─────────────────────────────────────────────────

    private fun startQuickAsk(prompt: String) {
        composerField.clearComposer()
        clearAssociatedContextItems()
        composerField.applyInsertion(ChatComposerInsertion.PlainText(prompt))
        handlePrimaryAction()
    }

    /**
     * 处理主操作（发送消息），根据当前状态决定是发送新消息还是回复 Ask 请求。
     */
    private fun handlePrimaryAction() {
        if (pendingAsk != null) {
            handleAskReply()
            return
        }

        val text = composerField.expandedText().trim()
        val referencedFilePaths = associatedContextState.referencedFilePaths()
        val codeSelections = associatedContextState.codeSelections()
        if (text.isBlank() && referencedFilePaths.isEmpty() && codeSelections.isEmpty()) return

        val builtInCommand = SlashCommandRegistry.findBuiltInCommand(text)
        if (builtInCommand != null) {
            handleBuiltInCommand(builtInCommand.command)
            return
        }

        val selectedModelId = getSelectedModelId()
        if (selectedModelId.isNullOrBlank()) {
            ManageCustomModelsDialog(project) { refreshModels() }.show()
            return
        }
        val selectedModel = getSelectedModel()
        if (selectedModel == null) {
            refreshModels()
            ManageCustomModelsDialog(project) { refreshModels() }.show()
            return
        }

        showConversation()
        val finalText = ChatComposerSupport.appendAssociatedCodeContext(text, codeSelections)
        val userMessageId = UUID.randomUUID().toString()
        val conversationIdForRequest = currentConversationId ?: UUID.randomUUID().toString()
        if (currentConversationId == null) {
            currentConversationId = conversationIdForRequest
        }
        addUserMessageCard(finalText, userMessageId, referencedFilePaths)
        composerField.clearComposer()
        clearAssociatedContextItems()
        clearAskState()
        clearComposerCheckpointPanel()
        updateConversationHeader()

        val selectedChatMode = JarvisCoreSettings.getInstance().chatMode
        activeJob = uiScope.launch {
            try {
                val task = Task(
                    taskId = tabId,
                    convId = conversationIdForRequest,
                    userMessageId = userMessageId,
                    userInput = finalText.ifBlank { null },
                    modelId = selectedModelId,
                    tools = ToolRegistry.getAll().values.toList(),
                    files = referencedFilePaths.ifEmpty { null },
                    chatMode = selectedChatMode,
                    project = project,
                    updateConvTitle = { _, title ->
                        ApplicationManager.getApplication().invokeLater {
                            currentConversationTitleLabel.text = title
                            onTitleChanged(title)
                        }
                    },
                )
                withContext(Dispatchers.EDT) {
                    activeTask = task
                    currentConversationId = task.convId
                    updateConversationHeader()
                }
                collectTaskEvents(task)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                withContext(Dispatchers.EDT) {
                    showTaskError(e)
                }
            } finally {
                withContext(Dispatchers.EDT) {
                    finalizePartialCards()
                    installRollbackActionIfAvailable(userMessageId)
                    refreshComposerCheckpointPanel(userMessageId)
                    activeJob = null
                    activeTask = null
                    partialCards.clear()
                    updateComposerState()
                    updateConversationHeader()
                }
            }
        }
        updateComposerState()
    }

    /**
     * 处理 Ask 回复，验证输入并提交响应给活跃任务。
     */
    private fun handleAskReply() {
        val ask = pendingAsk ?: return
        askPanel.validateReply(ask)?.let { message ->
            Messages.showInfoMessage(project, message, "Jarvis")
            return
        }
        val isQuestionPrompt = isAskUserQuestion(ask)
        val isRequestPrompt = isRequestUserInput(ask)
        val inputText = askPanel.buildReplyPayload(ask)

        val response = if (isQuestionPrompt || isRequestPrompt) {
            AskResponse(ask.id, AskResponse.ResponseType.MESSAGE, inputText)
        } else {
            when {
                askPanel.selectedDecision == AskDecision.REJECT -> AskResponse(ask.id, AskResponse.ResponseType.NO)
                inputText.isNotBlank() -> AskResponse(ask.id, AskResponse.ResponseType.MESSAGE, inputText)
                else -> AskResponse(ask.id, AskResponse.ResponseType.YES)
            }
        }

        activeTask?.askResponse(response)
        clearAskState()
        updateComposerState()
        requestFocusForInput()
    }

    /** Called by the [AskPanel] callback. */
    private fun handleAskReplyFromPanel(_decision: AskDecision, text: String) {
        if (pendingAsk == null) return
        askPanel.setReplyText(text)
        handleAskReply()
    }

    private fun handleProposedPlanAction(action: ProposedPlanAction, segment: ProposedPlanSegment) {
        if (!ensurePlanActionAvailable()) return
        when (action) {
            ProposedPlanAction.ASK_FOLLOW_UP -> continuePlanDiscussion()
            ProposedPlanAction.EXECUTE_IN_AGENT -> executeProposedPlan(segment)
        }
    }

    // ── Agent event rendering ────────────────────────────────────────

    private fun renderAgentEvent(event: AgentMessage) {
        val wasFollowing = scrollManager.followLatestOutput
        val preservedViewport = scrollManager.captureViewportSnapshot()
        showConversation()
        when (event) {
            is JarvisAsk -> {
                pendingAsk = event
                askPanel.bind(event)
                updateComposerState()
                addOrUpdateAssistantCard(event.id, event.data, event.isPartial, event.type)
            }
            else -> addOrUpdateAssistantCard(resolveMessageKey(event), event.data, event.isPartial, event.type)
        }
        if (!event.isPartial && isFileModifyingEvent(event)) {
            refreshComposerCheckpointPanel(activeTask?.userMessageId)
        }
        if (wasFollowing) {
            scrollManager.scrollToBottom(force = true)
        } else {
            scrollManager.restoreViewportSnapshot(preservedViewport)
        }
    }

    private fun isFileModifyingEvent(event: AgentMessage): Boolean {
        if (event.type != AgentMessageType.TOOL) return false
        return event.data.any { segment ->
            segment is ToolSegment && (segment.name == UiToolName.EDITED_EXISTING_FILE || segment.name == UiToolName.NEW_FILE_CREATED)
        }
    }

    private fun resolveMessageKey(event: AgentMessage): String {
        val stableEventId = event.data.filterIsInstance<TextSegment>().firstOrNull()?.eventId
        return stableEventId ?: event.id
    }

    private fun addAssociatedContextItem(item: AssociatedContextItem) {
        when (val result = associatedContextState.add(item)) {
            AssociatedContextAddResult.Added -> {
                updateAssociatedContextHeader()
                triggerFilePrediction()
            }
            is AssociatedContextAddResult.Existing -> associatedContextHeader.highlightItem(result.key)
        }
    }

    private fun removeAssociatedContextItem(item: AssociatedContextItem) {
        associatedContextState.remove(item)
        updateAssociatedContextHeader()
        triggerFilePrediction()
    }

    private fun clearAssociatedContextItems() {
        associatedContextState.clear()
        updateAssociatedContextHeader()
    }

    private fun updateAssociatedContextHeader() {
        associatedContextHeader.setItems(associatedContextState.allItems())
    }

    private fun showAssociatedFilePicker(anchor: Component) {
        AssociatedFilePickerPopup(project).show(
            anchor = anchor,
            existingPaths = associatedContextState.referencedFilePaths().toSet(),
        ) { file ->
            appendAssociatedFile(file.path)
        }
    }

    /**
     * 确认一个推荐的预测文件，将其从推荐状态变为已选中状态。
     */
    private fun confirmPredictedFile(file: AssociatedContextItem.AssociatedFile) {
        if (associatedContextState.confirmPredicted(file)) {
            updateAssociatedContextHeader()
            triggerFilePrediction()
        }
    }

    /**
     * 基于当前上下文触发文件关联预测，更新推荐文件列表。
     */
    private fun triggerFilePrediction() {
        val currentPaths = associatedContextState.referencedFilePaths().toSet()
            .plus(associatedContextState.predictedFiles().map { it.path }.toSet())
        uiScope.launch {
            val predicted = RelatedFilePredictor.predict(project, currentPaths)
            withContext(Dispatchers.EDT) {
                associatedContextState.setPredictedFiles(predicted)
                updateAssociatedContextHeader()
            }
        }
    }

    // ── Editor selection auto-prediction ────────────────────────────────

    /** 已安装 SelectionListener 的编辑器，避免重复安装 */
    private val installedSelectionListeners = mutableSetOf<com.intellij.openapi.editor.Editor>()

    private fun installSelectionListenerIfNeeded(editor: com.intellij.openapi.editor.Editor) {
        installSelectionListenerFor(editor)
    }

    private fun installSelectionListenerFor(editor: com.intellij.openapi.editor.Editor) {
        if (editor in installedSelectionListeners) return
        installedSelectionListeners.add(editor)
        editor.selectionModel.addSelectionListener(object : com.intellij.openapi.editor.event.SelectionListener {
            override fun selectionChanged(e: com.intellij.openapi.editor.event.SelectionEvent) {
                handleEditorSelectionChanged(e.editor)
            }
        })
    }

    private fun handleEditorSelectionChanged(editor: com.intellij.openapi.editor.Editor) {
        val selectionModel = editor.selectionModel
        if (!selectionModel.hasSelection()) {
            associatedContextState.removeAutoCodeSelection()
            updateAssociatedContextHeader()
            return
        }
        val selectedText = selectionModel.selectedText
        if (selectedText.isNullOrBlank()) {
            associatedContextState.removeAutoCodeSelection()
            updateAssociatedContextHeader()
            return
        }
        val document = editor.document
        val virtualFile = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getFile(document)
        if (virtualFile == null) {
            associatedContextState.removeAutoCodeSelection()
            updateAssociatedContextHeader()
            return
        }
        val startLine = document.getLineNumber(selectionModel.selectionStart)
        val endLine = document.getLineNumber(selectionModel.selectionEnd)
        val startOffset = document.getLineStartOffset(startLine)
        val endOffset = document.getLineEndOffset(endLine)
        val fullLineText = document.getText(com.intellij.openapi.util.TextRange(startOffset, endOffset))

        associatedContextState.setAutoCodeSelection(
            AssociatedContextItem.AssociatedCodeSelection(
                filePath = virtualFile.path,
                startLine = startLine + 1,
                endLine = endLine + 1,
                fullLineText = fullLineText,
            )
        )
        updateAssociatedContextHeader()
    }

    // ── Message cards ────────────────────────────────────────────────

    private fun addUserMessageCard(
        text: String,
        messageId: String? = null,
        referencedFilePaths: List<String> = emptyList(),
    ) {
        val card = renderer.createMessageShell(author = "You", icon = USER_ICON, alignRight = false)
        (card.header.components.firstOrNull() as? JBLabel)?.foreground = USER_FOREGROUND
        card.header.add(Box.createHorizontalGlue())
        maybeInstallRollbackAction(card.header, messageId)
        val copyButton = createHeaderIconButton(AllIcons.Actions.Copy, "\u590D\u5236\u6D88\u606F") {
            copyToClipboard(text)
        }
        card.header.add(copyButton)
        messageId?.let { userMessageHeaders[it] = card.header }
        card.root.isOpaque = true
        card.root.background = USER_MESSAGE_BACKGROUND
        card.root.border = BorderFactory.createCompoundBorder(
            JBUI.Borders.customLine(SPLIT_LINE_COLOR, 1, 0, 1, 0),
            JBUI.Borders.empty(10, 8),
        )
        createReferencedFilesPanel(referencedFilePaths)?.let { card.body.add(it) }
        if (text.isNotBlank()) {
            card.body.add(renderer.createTextBlock(text, USER_MESSAGE_BACKGROUND, JBColor.foreground(), JBFont.label()))
        }
        messageContainer.add(card.root)
        messageContainer.revalidate()
        messageContainer.repaint()
        scrollManager.scrollToBottom(force = true)
    }

    private fun createReferencedFilesPanel(referencedFilePaths: List<String>): JComponent? {
        if (referencedFilePaths.isEmpty()) return null
        val basePath = project.basePath
        val links = referencedFilePaths.distinct().map { path ->
            val relativePath = if (basePath.isNullOrBlank()) path else toRelativePath(path, basePath)
            ActionLink(relativePath) {
                LocalFileSystem.getInstance().findFileByPath(path)?.let { file ->
                    FileEditorManager.getInstance(project).openFile(file, true)
                }
            }.apply {
                icon = LocalFileSystem.getInstance().findFileByPath(path)?.let { file ->
                    if (file.isDirectory) AllIcons.Nodes.Folder else file.fileType.icon
                } ?: AllIcons.FileTypes.Any_type
                toolTipText = path
                border = JBUI.Borders.emptyBottom(2)
                foreground = JBColor.foreground()
            }
        }
        return if (links.isEmpty()) null else SelectedFilesAccordion(links).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            border = JBUI.Borders.emptyBottom(8)
        }
    }

    private fun addOrUpdateAssistantCard(
        key: String,
        segments: List<Segment>,
        partial: Boolean,
        type: AgentMessageType,
    ) {
        val existing = assistantCards[key]
        if (existing != null) {
            val merged = mergeMcpSegmentsIfNeeded(existing.lastSegments, segments)
            existing.updateContent(merged, type, partial)
            messageContainer.revalidate()
            messageContainer.repaint()
            if (partial) {
                partialCards[key] = existing
            } else {
                partialCards.remove(key)
            }
            return
        }

        val card = AssistantMessageCard(renderer)
        card.updateContent(segments, type, partial)
        messageContainer.add(card.root)
        assistantCards[key] = card
        if (partial) partialCards[key] = card
        messageContainer.revalidate()
        messageContainer.repaint()
        if (!partial) partialCards.remove(key)
    }

    /**
     * 当新段包含 MCP_TOOL_RESPONSE 且旧段包含 MCP_TOOL 时，
     * 将响应段追加到旧段列表后（调用与响应在同一卡片内分开展示）。
     */
    private fun mergeMcpSegmentsIfNeeded(oldSegments: List<Segment>, newSegments: List<Segment>): List<Segment> {
        val hasMcpResponse = newSegments.any {
            it is ToolSegment && it.name == UiToolName.MCP_TOOL_RESPONSE
        }
        if (!hasMcpResponse) return newSegments
        return oldSegments + newSegments
    }

    private fun finalizePartialCards() {
        partialCards.values.forEach { it.finishPartialIfNeeded() }
    }

    // ── Conversation lifecycle ───────────────────────────────────────

    private fun loadConversation(convId: String) {
        stopActiveTask()
        if (currentConversationId != null && currentConversationId != convId) {
            TaskState.clearCachedServices(tabId)
        }
        currentConversationId = convId
        clearAskState()
        clearAssociatedContextItems()
        clearComposerCheckpointPanel()
        assistantCards.clear()
        partialCards.clear()
        updateConversationHeader()

        ApplicationManager.getApplication().executeOnPooledThread {
            val history = runCatching { JsonLineChatHistory(convId, project).messages() }.getOrDefault(emptyList())
            ApplicationManager.getApplication().invokeLater {
                renderHistory(history)
            }
        }
    }

    private fun renderHistory(historyMessages: List<ChatHistoryMessage>) {
        messageContainer.removeAll()
        assistantCards.clear()
        partialCards.clear()
        userMessageHeaders.clear()
        val latestUserMessageId = historyMessages.asReversed()
            .filterIsInstance<ChatHistoryUserMessage>()
            .firstOrNull()
            ?.messageId
        if (historyMessages.isEmpty()) {
            clearComposerCheckpointPanel()
            showWelcome()
        } else {
            showConversation()
            historyMessages.forEach { message ->
                when (message) {
                    is ChatHistoryUserMessage -> addUserMessageCard(
                        text = message.text.orEmpty(),
                        messageId = message.messageId,
                        referencedFilePaths = message.referencedFilePaths.orEmpty().filterNotNull(),
                    )
                    is ChatHistoryAssistantMessage -> addOrUpdateAssistantCard(
                        key = "history-${message.hashCode()}",
                        segments = message.segments,
                        partial = false,
                        type = AgentMessageType.TEXT,
                    )
                }
            }
            refreshComposerCheckpointPanel(latestUserMessageId)
        }
        messageContainer.revalidate()
        messageContainer.repaint()
        scrollManager.scrollToBottom(force = true)
        updateComposerState()
    }

    // ── State management helpers ─────────────────────────────────────

    private fun stopActiveTask() {
        activeJob?.cancel()
        activeJob = null
        activeTask = null
        clearAskState()
        updateComposerState()
    }

    private fun clearAskState() {
        pendingAsk = null
        askPanel.clear()
    }

    private fun refreshChatMode() {
        chatModeComboBox.selectedItem = JarvisCoreSettings.getInstance().chatMode
    }

    private fun ensurePlanActionAvailable(): Boolean {
        if (pendingAsk != null) {
            Messages.showInfoMessage(
                project,
                "\u5F53\u524D\u6709\u5F85\u5904\u7406\u7684\u4EA4\u4E92\u8BF7\u6C42\uFF0C\u8BF7\u5148\u5B8C\u6210\u5B83\u3002",
                "Jarvis",
            )
            return false
        }
        if (activeTask != null || activeJob != null) {
            Messages.showInfoMessage(
                project,
                "\u5F53\u524D\u6709\u4EFB\u52A1\u6B63\u5728\u8FD0\u884C\uFF0C\u8BF7\u7B49\u5F85\u5B8C\u6210\u540E\u518D\u7EE7\u7EED\u3002",
                "Jarvis",
            )
            return false
        }
        return true
    }

    private fun continuePlanDiscussion() {
        switchChatMode(ChatMode.PLAN)
        if (composerField.expandedText().trim().isBlank()) {
            composerField.applyInsertion(
                ChatComposerInsertion.PlainText(
                    "\u8BF7\u57FA\u4E8E\u4E0A\u9762\u7684 Proposed Plan \u7EE7\u7EED\u7EC6\u5316\uFF0C\u6211\u7684\u95EE\u9898\u662F\uFF1A"
                )
            )
        }
        requestFocusForInput()
    }

    private fun executeProposedPlan(segment: ProposedPlanSegment) {
        if (!confirmDiscardDraftForPlanExecution()) return
        switchChatMode(ChatMode.AGENT)
        composerField.clearComposer()
        clearAssociatedContextItems()
        composerField.applyInsertion(ChatComposerInsertion.PlainText(buildPlanExecutionPrompt(segment.markdown)))
        handlePrimaryAction()
    }

    private fun confirmDiscardDraftForPlanExecution(): Boolean {
        val hasDraft = composerField.expandedText().trim().isNotBlank()
        val hasReferencedFiles = associatedContextState.referencedFilePaths().isNotEmpty()
        val hasCodeSelections = associatedContextState.codeSelections().isNotEmpty()
        if (!hasDraft && !hasReferencedFiles && !hasCodeSelections) {
            return true
        }
        return Messages.showYesNoDialog(
            project,
            "\u8F93\u5165\u6846\u6216\u9644\u52A0\u4E0A\u4E0B\u6587\u91CC\u8FD8\u6709\u672A\u53D1\u9001\u7684\u5185\u5BB9\u3002\u662F\u5426\u4E22\u5F03\u8FD9\u4E9B\u5185\u5BB9\u5E76\u6309\u5F53\u524D\u8BA1\u5212\u7EE7\u7EED\u6267\u884C\uFF1F",
            "\u6309\u8BA1\u5212\u6267\u884C",
            Messages.getQuestionIcon(),
        ) == Messages.YES
    }

    private fun switchChatMode(mode: ChatMode) {
        JarvisCoreSettings.getInstance().chatMode = mode
        if (chatModeComboBox.selectedItem != mode) {
            chatModeComboBox.selectedItem = mode
        } else {
            updateConversationHeader()
        }
    }

    private fun buildPlanExecutionPrompt(markdown: String): String {
        return buildString {
            append(
                "\u8BF7\u76F4\u63A5\u5F00\u59CB\u6267\u884C\u4E0B\u9762\u8FD9\u4E2A Proposed Plan\u3002\u4E0D\u8981\u91CD\u65B0\u8F93\u51FA\u8BA1\u5212\uFF0C\u800C\u662F\u5148\u8BFB\u53D6\u5FC5\u8981\u6587\u4EF6\uFF0C\u7136\u540E\u6309\u8BA1\u5212\u4FEE\u6539\u4EE3\u7801\u3001\u9A8C\u8BC1\u7ED3\u679C\u5E76\u6C47\u62A5\u3002"
            )
            append("\n\n")
            append("\u5982\u679C\u5728\u6267\u884C\u524D\u53D1\u73B0\u8FD8\u5B58\u5728\u5173\u952E\u672A\u51B3\u95EE\u9898\uFF0C\u53EA\u63D0\u6700\u5C11\u5FC5\u8981\u7684\u95EE\u9898\u3002")
            append("\n\n<proposed_plan>\n")
            append(markdown.trim())
            append("\n</proposed_plan>")
        }
    }

    private fun updateComposerState() {
        val state = when {
            pendingAsk != null -> ComposerState.AWAITING_REPLY
            activeTask != null || activeJob != null -> ComposerState.RUNNING
            else -> ComposerState.IDLE
        }
        applyComposerState(state)
    }

    private fun applyComposerState(state: ComposerState) {
        composerField.isEnabled = true
        when (state) {
            ComposerState.IDLE -> {
                sendButton.isEnabled = true
                sendButton.toolTipText = "\u53D1\u9001"
                stopButton.isEnabled = false
                stopButton.toolTipText = "\u4EC5\u5728\u4EFB\u52A1\u8FD0\u884C\u4E2D\u624D\u53EF\u505C\u6B62"
            }
            ComposerState.RUNNING -> {
                sendButton.isEnabled = false
                sendButton.toolTipText = "\u5F53\u524D\u6B63\u5728\u8FD0\u884C"
                stopButton.isEnabled = true
                stopButton.toolTipText = "\u505C\u6B62\u5F53\u524D\u4EFB\u52A1"
            }
            ComposerState.AWAITING_REPLY -> {
                sendButton.isEnabled = true
                sendButton.toolTipText = "\u63D0\u4EA4\u56DE\u590D"
                stopButton.isEnabled = true
                stopButton.toolTipText = "\u505C\u6B62\u5F53\u524D\u4EFB\u52A1"
            }
        }
    }

    private fun handleBuiltInCommand(command: String) {
        when (command.lowercase()) {
            "/clear" -> executeClearCommand()
            "/new" -> executeNewCommand()
            "/compact" -> executeCompactCommand()
            "/plan" -> executePlanCommand()
        }
    }

    private fun executeClearCommand() {
        val currentModelId = getSelectedModelId().orEmpty()
        activeTask?.let { stopActiveTask() }
        currentConversationId?.let { convId ->
            runCatching { ConversationCommandService.resetConversation(tabId, convId, currentModelId, project) }
        }
        resetConversationUi()
    }

    private fun executeNewCommand() {
        archiveCurrentConversation()
        onArchiveConversation()
    }

    private fun executeCompactCommand() {
        val conversationId = currentConversationId
        if (conversationId.isNullOrBlank()) {
            Messages.showInfoMessage(project, "\u5F53\u524D\u8FD8\u6CA1\u6709\u53EF\u538B\u7F29\u7684\u4F1A\u8BDD\u4E0A\u4E0B\u6587\u3002", "Jarvis")
            return
        }

        val selectedModelId = getSelectedModelId()
        if (selectedModelId.isNullOrBlank()) {
            ManageCustomModelsDialog(project) { refreshModels() }.show()
            return
        }

        sendButton.isEnabled = false
        stopButton.isEnabled = false
        ApplicationManager.getApplication().executeOnPooledThread {
            val result = runCatching {
                ConversationCommandService.compactConversation(tabId, conversationId, selectedModelId, project)
            }
            ApplicationManager.getApplication().invokeLater {
                result.onSuccess {
                    loadConversation(conversationId)
                }.onFailure {
                    Messages.showInfoMessage(
                        project,
                        it.message ?: "\u5F53\u524D\u4F1A\u8BDD\u6682\u65F6\u65E0\u6CD5\u538B\u7F29\u3002",
                        "Jarvis",
                    )
                    updateComposerState()
                }
            }
        }
    }

    private fun executePlanCommand() {
        switchChatMode(ChatMode.PLAN)
        composerField.clearComposer()
        composerField.applyInsertion(
            ChatComposerInsertion.PlainText("\u8BF7\u5E2E\u6211\u89C4\u5212\u4EE5\u4E0B\u4EFB\u52A1\uFF1A")
        )
        requestFocusForInput()
    }

    private fun resetConversationUi() {
        composerField.clearComposer()
        clearAssociatedContextItems()
        TaskState.clearCachedServices(tabId)
        currentConversationId = null
        pendingAsk = null
        clearComposerCheckpointPanel()
        assistantCards.clear()
        partialCards.clear()
        userMessageHeaders.clear()
        messageContainer.removeAll()
        messageContainer.revalidate()
        messageContainer.repaint()
        currentConversationTitleLabel.text = "\u65B0\u7684\u4F1A\u8BDD"
        onTitleChanged("")
        updateConversationHeader()
        showWelcome()
        updateComposerState()
    }

    private fun archiveCurrentConversation() {
        val conversationId = currentConversationId ?: return
        val conversation = ConversationStore.getConversation(project, conversationId) ?: return
        conversation.updatedTime = System.currentTimeMillis()
        ConversationStore.updateConversation(project, conversation)
    }

    private fun updateConversationHeader() {
        val conversation = currentConversationId?.let { ConversationStore.getConversation(project, it) }
        currentConversationTitleLabel.text = conversation?.title?.takeIf { it.isNotBlank() } ?: "\u65B0\u7684\u4F1A\u8BDD"
        val modelLabel = (modelComboBox.selectedItem as? ModelItem)?.label ?: "\u672A\u9009\u62E9\u6A21\u578B"
        val modeLabel = (chatModeComboBox.selectedItem as? ChatMode)?.displayName ?: ChatMode.AGENT.displayName
        val historyLabel = conversation?.let { "\u521B\u5EFA\u4E8E ${formatTimestamp(it.createdTime)}" } ?: "\u672A\u6301\u4E45\u5316"
        currentConversationMetaLabel.text = "$modelLabel \u00B7 $modeLabel \u00B7 $historyLabel"
    }

    private fun showWelcome() {
        centerLayout.show(centerPanel, CARD_WELCOME)
    }

    private fun showConversation() {
        centerLayout.show(centerPanel, CARD_CHAT)
    }

    private suspend fun collectTaskEvents(task: Task) {
        val pendingPartialEvents = linkedMapOf<String, AgentMessage>()
        var lastPartialRenderAt = 0L

        suspend fun renderOnEdt(events: List<AgentMessage>) {
            if (events.isEmpty()) return
            withContext(Dispatchers.EDT) {
                events.forEach(::renderAgentEvent)
            }
        }

        suspend fun flushPendingPartial(force: Boolean = false) {
            if (pendingPartialEvents.isEmpty()) return
            val now = System.currentTimeMillis()
            if (!force && now - lastPartialRenderAt < STREAM_RENDER_INTERVAL_MS) return
            val events = pendingPartialEvents.values.toList()
            pendingPartialEvents.clear()
            lastPartialRenderAt = now
            renderOnEdt(events)
        }

        task.startTaskLoop().collect { event ->
            if (event.isPartial) {
                pendingPartialEvents[partialBufferKey(event)] = event
                flushPendingPartial()
            } else {
                flushPendingPartial(force = true)
                renderOnEdt(listOf(event))
            }
        }
        flushPendingPartial(force = true)
    }

    private fun partialBufferKey(event: AgentMessage): String {
        return when (event) {
            is JarvisAsk -> "ask:${event.id}:${event.type}"
            else -> "message:${resolveMessageKey(event)}:${event.type}"
        }
    }

    private fun refreshComposerCheckpointPanel(messageId: String?) {
        if (messageId.isNullOrBlank() || shouldHideComposerCheckpointPanel()) {
            clearComposerCheckpointPanel()
            return
        }
        val fileChanges = rollbackSupport.getPendingFileChanges(currentConversationId, messageId)
        composerCheckpointRestorePanel.updateContent(messageId, fileChanges)
    }

    private fun clearComposerCheckpointPanel() {
        composerCheckpointRestorePanel.clear()
    }

    private fun showCheckpointDiff(conversationId: String, messageId: String, filePath: String) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val snapshotContent = runCatching {
                com.miracle.utils.CheckpointStorage.getSnapshotContentForFile(project, conversationId, messageId, filePath)
            }.getOrNull() ?: ""
            val currentContent = java.io.File(filePath).let { if (it.exists()) it.readText() else "" }
            val fileName = java.io.File(filePath).name
            val fileType = com.intellij.openapi.fileTypes.FileTypeManager.getInstance().getFileTypeByFileName(fileName)

            ApplicationManager.getApplication().invokeLater {
                val leftContent = com.intellij.diff.DiffContentFactory.getInstance()
                    .create(project, snapshotContent, fileType)
                val rightContent = com.intellij.diff.DiffContentFactory.getInstance()
                    .create(project, currentContent, fileType)
                val request = com.intellij.diff.requests.SimpleDiffRequest(
                    fileName,
                    leftContent,
                    rightContent,
                    "\u539F\u59CB\u7248\u672C",
                    "\u5F53\u524D\u7248\u672C",
                )
                com.intellij.diff.DiffManager.getInstance().showDiff(project, request)
            }
        }
    }

    private fun shouldHideComposerCheckpointPanel(): Boolean {
        return false
    }

    private fun maybeInstallRollbackAction(header: JPanel, messageId: String?) {
        if (messageId.isNullOrBlank()) return
        if (header.getClientProperty(ROLLBACK_ACTION_KEY) == true) return
        if (!rollbackSupport.canRollback(currentConversationId, messageId)) return
        header.putClientProperty(ROLLBACK_ACTION_KEY, true)
        header.add(Box.createHorizontalStrut(JBUI.scale(6)), header.componentCount.coerceAtLeast(1))
        header.add(
            createHeaderTextButton("\u56DE\u9000\u672C\u6B21\u6539\u52A8", "\u56DE\u9000\u672C\u8F6E\u4EA7\u751F\u7684\u6587\u4EF6\u6539\u52A8") {
                rollbackSupport.rollback(currentConversationId, messageId, activeTask, ::stopActiveTask) {
                    loadConversation(currentConversationId!!)
                }
            },
            header.componentCount.coerceAtLeast(1),
        )
    }

    private fun installRollbackActionIfAvailable(messageId: String?) {
        if (messageId.isNullOrBlank()) return
        maybeInstallRollbackAction(userMessageHeaders[messageId] ?: return, messageId)
    }

    private fun showTaskError(error: Throwable) {
        val message = error.message ?: error::class.simpleName ?: "Unknown error"
        addOrUpdateAssistantCard(
            key = "task-error-${System.nanoTime()}",
            segments = listOf(ErrorSegment("Error: $message")),
            partial = false,
            type = AgentMessageType.ERROR,
        )
    }
}
