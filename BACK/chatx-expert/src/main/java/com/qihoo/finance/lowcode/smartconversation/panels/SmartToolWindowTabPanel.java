package com.qihoo.finance.lowcode.smartconversation.panels;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.ui.JBUI;
import com.qifu.agent.TaskState;
import com.qifu.agent.parser.ErrorSegment;
import com.qifu.agent.parser.Segment;
import com.qifu.agent.parser.ToolSegment;
import com.qifu.ui.smartconversation.panels.*;
import com.qifu.ui.smartconversation.psistructure.ClassStructure;
import com.qifu.ui.smartconversation.psistructure.PsiStructureProvider;
import com.qifu.ui.smartconversation.psistructure.PsiStructureRepository;
import com.qifu.ui.smartconversation.psistructure.PsiStructureState;
import com.qifu.ui.smartconversation.settings.service.TaskCompletionParameters;
import com.qifu.ui.smartconversation.sse.AgentCompletionRequestService;
import com.qifu.ui.smartconversation.sse.MessageBuilder;
import com.qifu.ui.smartconversation.settings.configuration.ChatMode;
import com.qifu.ui.smartconversation.textarea.ConversationTagProcessor;
import com.qifu.ui.smartconversation.textarea.header.*;
import com.qifu.ui.utils.CompletionRequestUtil;
import com.qifu.ui.utils.EditorUtil;
import com.qifu.ui.smartconversation.ConversationCommandService;
import com.qifu.ui.smartconversation.CompactResult;
import com.qifu.ui.smartconversation.SlashCommand;
import com.qifu.ui.smartconversation.SlashCommandRegistry;
import com.qifu.utils.ChatHistoryAssistantMessage;
import com.qifu.utils.ChatHistoryMessage;
import com.qifu.utils.ChatHistoryUserMessage;
import com.qifu.utils.CheckpointStorage;
import com.qifu.utils.JsonLineChatHistory;
import com.qifu.utils.TodoStorage;
import com.qifu.utils.coroutines.CoroutineDispatchers;
import com.qifu.utils.file.FileUtil;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.factory.ChatXToolWindowFactory;
import com.qihoo.finance.lowcode.common.util.NotifyUtils;
import com.qihoo.finance.lowcode.common.util.UIUtil;
import com.qihoo.finance.lowcode.smartconversation.actions.CopyAction;
import com.qihoo.finance.lowcode.smartconversation.configuration.SmartCompletionResponseEventListener;
import com.qihoo.finance.lowcode.smartconversation.conversations.Conversation;
import com.qihoo.finance.lowcode.smartconversation.conversations.Message;
import com.qihoo.finance.lowcode.smartconversation.conversations.TaskMessageResponseBody;
import com.qihoo.finance.lowcode.smartconversation.service.ReferencedFile;
import com.qihoo.finance.lowcode.smartconversation.service.TaskCompletionRequestHandler;
import com.qihoo.finance.lowcode.smartconversation.utils.OverlayUtil;
import git4idea.GitCommit;
import kotlin.Unit;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static com.qihoo.finance.lowcode.smartconversation.configuration.JarvisKeys.IMAGE_ATTACHMENT_FILE_PATH;

public class SmartToolWindowTabPanel implements Disposable {

    private static final Logger LOG = Logger.getInstance(SmartToolWindowTabPanel.class);

    private final JPanel rootPanel;

    private final SmartToolWindowScrollablePanel scrollablePanel;

    private final WelcomePanel welcomePanel;

    private final TagManager tagManager;

    @Getter
    private final String taskId;

    private final UserInputPanel userInputPanel;

    private final PsiStructureRepository psiStructureRepository;


    private final Project project;

    private final OperationPanel operationPanel;

    private @Nullable TaskCompletionRequestHandler requestHandler;

    private CardLayout cardLayout;

    private JPanel centerPanel;
    private Boolean isDisposed = false;


    public SmartToolWindowTabPanel(@NotNull Project project, String taskId) {
        this(project, taskId, true);
    }

    public SmartToolWindowTabPanel(@NotNull Project project, String taskId, boolean includeSelectedEditorContext) {
        this.taskId = taskId;
        this.scrollablePanel = new SmartToolWindowScrollablePanel();
        this.welcomePanel = new WelcomePanel(this::handleQuickAsk);
        this.tagManager = new TagManager(this);
        this.project = project;
        this.psiStructureRepository = new PsiStructureRepository(this, project, tagManager, new PsiStructureProvider(), new CoroutineDispatchers());
        this.userInputPanel = new UserInputPanel(project, this, tagManager, taskId, this::handleSubmit, this::handleCancel, includeSelectedEditorContext);
        this.userInputPanel.requestFocus();
        this.operationPanel = new OperationPanel(project);
        this.rootPanel = createRootPanel();
        ApplicationManager.getApplication().invokeLater(this::loadHistoryMessages);
    }

    public void dispose() {
        isDisposed = true;
        handleCancel();
        LOG.info("Disposing BaseChatToolWindowTabPanel component");
    }

    public JComponent getContent() {
        return rootPanel;
    }

    public void requestFocusForTextArea() {
        userInputPanel.requestFocus();
    }


    public List<TagDetails> getSelectedTags() {
        return userInputPanel.getSelectedTags();
    }


    public void addSelection(VirtualFile editorFile, SelectionModel selectionModel) {
        userInputPanel.addSelection(editorFile, selectionModel);
    }

    public void addCommitReferences(List<GitCommit> gitCommits) {
        userInputPanel.addCommitReferences(gitCommits);
    }

    /**
     * 将文本插入到输入框中
     * @param text 要插入的文本
     */
    public void insertTextToPrompt(String text) {
        userInputPanel.insertTextToPrompt(text, true);
    }

    /**
     * 插入代码选择占位符到输入框
     * @param fileName 文件名
     * @param startLine 开始行号
     * @param endLine 结束行号
     * @param selectedCode 选中的代码内容
     */
    public void insertCodeSelectionPlaceholder(String fileName, int startLine, int endLine, String selectedCode) {
        userInputPanel.insertCodeSelectionPlaceholder(fileName, startLine, endLine, selectedCode);
    }

    /**
     * 移除 EditorSelectionTagDetails,但保持编辑器中的选中状态
     */
    public void removeEditorSelectionTag() {
        userInputPanel.removeEditorSelectionTag();
    }

    private TaskCompletionParameters getCallParameters(Message message, Set<ClassStructure> psiStructure) {
        final var selectedTags = tagManager.getTags().stream().filter(TagDetails::getSelected).collect(Collectors.toList());

        List<String> imagePaths = project.getUserData(IMAGE_ATTACHMENT_FILE_PATH);

        var builder = TaskCompletionParameters.builder(taskId, message)
//                .imageDetailsFromPaths(imagePaths, 1024, 0.8f)
                .imageDetailsList(imagePaths)
                .referencedFiles(getReferencedFiles(selectedTags)).psiStructure(psiStructure).chatMode(userInputPanel.getChatMode())
                .history(getHistory(getSelectedTags()))
                .hasCustomInput(true)
                .modelId(userInputPanel.getCurrentModelId());

        findTagOfType(selectedTags, GitCommitTagDetails.class).ifPresent(tag -> builder.gitDiff(tag.getGitCommit().getFullMessage()));

        return builder.build();
    }

    private List<ReferencedFile> getReferencedFiles(List<? extends TagDetails> tags) {
        return tags.stream().map(this::getVirtualFile).filter(Objects::nonNull).distinct().map(ReferencedFile::from).toList();
    }


    private VirtualFile getVirtualFile(TagDetails tag) {
        VirtualFile virtualFile = null;
        if (tag.getSelected()) {
            if (tag instanceof FileTagDetails) {
                virtualFile = ((FileTagDetails) tag).getVirtualFile();
            } else if (tag instanceof FolderTagDetails) {
                virtualFile = ((FolderTagDetails) tag).getFolder();
            }

        }
        return virtualFile;
    }


    private <T extends TagDetails> Optional<T> findTagOfType(List<? extends TagDetails> tags, Class<T> tagClass) {
        return tags.stream().filter(tagClass::isInstance).map(tagClass::cast).findFirst();
    }

    public void displayLandingView() {
        // totalTokensPanel.updateConversationTokens(conversation);
        // scrollablePanel.displayLandingView(getLandingView());
    }

    public void loadHistoryMessages() {
        JsonLineChatHistory historyStore = new JsonLineChatHistory(taskId, project);
        List<ChatHistoryMessage> historyMessages = historyStore.messages();
        if (historyMessages.isEmpty()) {
            return;
        }
        displayConversation(historyMessages);
    }

    public void displayConversation(List<ChatHistoryMessage> historyMessages) {
        if (isDisposed) return;

        this.cardLayout.show(this.centerPanel, "scroll");
        JPanel messagePanel = null;
        for (ChatHistoryMessage historyMessage : historyMessages) {
            if (historyMessage instanceof ChatHistoryUserMessage chatHistoryUserMessage) {
                Message message = new Message();
                message.setPrompt(chatHistoryUserMessage.getText());
                message.setReferencedFilePaths(chatHistoryUserMessage.getReferencedFilePaths());
                message.setImageFilePaths(chatHistoryUserMessage.getImageFilePaths());

                messagePanel = scrollablePanel.addMessage(message.getId());
                messagePanel.add(getUserMessagePanel(message));
            } else if (historyMessage instanceof ChatHistoryAssistantMessage chatHistoryAssistantMessage && messagePanel != null) {
                messagePanel.add(getResponseMessagePanel(UUID.randomUUID().toString(), chatHistoryAssistantMessage.getSegments()));
//                var finalMessagePanel = messagePanel;
//                Alarm alarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, this);
//                alarm.addRequest(() -> {
//                    finalMessagePanel.add(getResponseMessagePanel(UUID.randomUUID().toString(), chatHistoryAssistantMessage.getSegments()));
//                }, 5);

            }
        }
    }

    private List<Conversation> getHistory(List<? extends TagDetails> tags) {
        return tags.stream()
                .map(it -> {
                    if (it instanceof HistoryTagDetails tagDetails) {
                        return ConversationTagProcessor.Companion.getConversation(
                                tagDetails.getConversationId());
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<UUID> getConversationHistoryIds(List<? extends TagDetails> tags) {
        return tags.stream()
                .map(it -> {
                    if (it instanceof HistoryTagDetails tagDetails) {
                        return tagDetails.getConversationId();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private void clearWindow() {
        scrollablePanel.clearAll();
    }


    private UserMessagePanel getUserMessagePanel(Message message) {
        var userMessagePanel = new UserMessagePanel(project, message, this);
        userMessagePanel.addCopyAction(() -> CopyAction.copyToClipboard(message.getPrompt()));
        userMessagePanel.addReloadAction(() -> reloadMessage(
                TaskCompletionParameters.builder(taskId, message)
                        .modelId(userInputPanel.getCurrentModelId())
                        .hasCustomInput(true)
                        .build(),
                userMessagePanel));
        configureCheckpointAction(userMessagePanel, message);
        return userMessagePanel;
    }

    private ResponseMessagePanel getResponseMessagePanel(String eventId, List<Segment> segments) {
        var responseMessagePanel = new ResponseMessagePanel();
        var contentBuilder = new StringBuilder();
        var contentPanel = new TaskMessageResponseBody(project, this);

        for (Segment segment : segments) {
            TaskMessageResponseBody messageResponseBody = new TaskMessageResponseBody(project, this);
            if (segment instanceof ErrorSegment errorSegment) {
                messageResponseBody.withResponse(errorSegment);
            } else if (segment instanceof ToolSegment toolSegment) {
                messageResponseBody.withToolResponse(eventId, toolSegment, false);
            } else {
                messageResponseBody.withResponse(segment);
            }
            contentPanel.contentPanel.add(messageResponseBody);  // 添加到容器面板
            contentBuilder.append(segment.getContent());
        }
        
        // 一次性将容器面板添加到 responseMessagePanel
        responseMessagePanel.addContent(contentPanel);
        responseMessagePanel.addCopyAction(() -> CopyAction.copyToClipboard(contentBuilder.toString()));
        return responseMessagePanel;
    }


    private JPanel createUserPromptPanel() {
        var panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.compound(JBUI.Borders.customLine(Constants.Color.SPLIT_LINE_COLOR, 1, 0, 0, 0), JBUI.Borders.empty(8)));
        panel.add(JBUI.Panels.simplePanel(operationPanel)
                .withBorder(JBUI.Borders.emptyBottom(8))
                .withBackground(Constants.Color.PANEL_BACKGROUND), BorderLayout.NORTH);
        panel.add(userInputPanel, BorderLayout.CENTER);
        panel.setBackground(Constants.Color.PANEL_BACKGROUND);
        return panel;
    }


    private JPanel createRootPanel() {
        var rootPanel = new JPanel(new BorderLayout());
        this.cardLayout = new CardLayout();
        this.centerPanel = new JPanel(cardLayout);
        this.centerPanel.setBackground(Constants.Color.PANEL_BACKGROUND);
        this.centerPanel.add(welcomePanel, "welcome");

        var scrollPane = UIUtil.createScrollPaneWithSmartScroller(scrollablePanel);
        this.centerPanel.add(scrollPane, "scroll");
        this.cardLayout.show(this.centerPanel, "welcome");
        rootPanel.add(this.centerPanel, BorderLayout.CENTER);
        rootPanel.add(createUserPromptPanel(), BorderLayout.SOUTH);
        return rootPanel;
    }

    public void sendMessage(Message message) {
        sendMessage(message, new HashSet<>());
    }

    public void sendMessage(Message message, Set<ClassStructure> psiStructure) {
        var callParameters = getCallParameters(message, psiStructure);
        if (callParameters.getImageDetailsList() != null) {
            JComponent component = ChatXToolWindowFactory.getToolWindow().getContentManager().findContent(ChatXToolWindowFactory.TAB_AGENT_NAME).getComponent();
            if (component instanceof SmartToolWindowPanel smartToolWindowPanel) {
                smartToolWindowPanel.clearImageNotifications(project);
            }
        }

        var userMessagePanel = createUserMessagePanel(message, callParameters);
        var responseMessagePanel = createResponseMessagePanel(callParameters);

        var messagePanel = scrollablePanel.addMessage(message.getId());
        messagePanel.add(userMessagePanel);
        messagePanel.add(responseMessagePanel);
        messagePanel.revalidate();
        messagePanel.repaint();

        call(callParameters, responseMessagePanel, userMessagePanel);
    }


    private ResponseMessagePanel createResponseMessagePanel(TaskCompletionParameters callParameters) {
        var message = callParameters.getMessage();
        var fileContextIncluded = hasReferencedFilePaths(message);

        var panel = new ResponseMessagePanel();
        panel.addCopyAction(() -> CopyAction.copyToClipboard(message.getResponse()));
        panel.addContent(new TaskMessageResponseBody(project, false, message.isWebSearchIncluded(), fileContextIncluded || message.getDocumentationDetails() != null, true, this));
        return panel;
    }


    public void clearAllTags() {
        tagManager.clear();
    }


    public Unit handleSubmit(String text) {
        if (handleSlashCommand(text)) {
            return Unit.INSTANCE;
        }
        this.cardLayout.show(this.centerPanel, "scroll");
        scrollablePanel.scrollToBottom();
        var application = ApplicationManager.getApplication();
        application.executeOnPooledThread(() -> {
            final Set<ClassStructure> psiStructure;
            if (psiStructureRepository.getStructureState().getValue() instanceof PsiStructureState.Content content) {
                psiStructure = content.getElements();
            } else {
                psiStructure = new HashSet<>();
            }

            final var appliedTags = tagManager.getTags().stream().filter(TagDetails::getSelected).collect(Collectors.toList());

            var messageBuilder = new MessageBuilder(project, text).withInlays(appliedTags);

            List<ReferencedFile> referencedFiles = getReferencedFiles(appliedTags);
            if (!referencedFiles.isEmpty()) {
                messageBuilder.withReferencedFiles(referencedFiles);
            }

            List<String> imageFilePaths = project.getUserData(IMAGE_ATTACHMENT_FILE_PATH);
            if (imageFilePaths != null) {
                messageBuilder.withImage(imageFilePaths);
            }
            application.invokeLater(() -> {
                sendMessage(messageBuilder.build(), psiStructure);
                // 自动清除已勾选的文件标签，避免第二次发送时带相同的文件列表
                clearAllTags();
            });
        });
        return Unit.INSTANCE;
    }

    private boolean handleSlashCommand(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        SlashCommand command = SlashCommandRegistry.findCommand(text);
        if (command == null) {
            return false;
        }
        switch (command.getCommand()) {
            case "/clear" -> executeClearCommand();
            case "/compact" -> executeCompactCommand();
            default -> {
                return false;
            }
        }
        return true;
    }

    private void executeClearCommand() {
        if (taskId == null || taskId.isBlank()) {
            appendCommandMessage("/clear failed: no active conversation.");
            return;
        }
        project.getService(AgentCompletionRequestService.class).cancel(taskId);
        handleCancel();
        DiffWindowHolder.INSTANCE.closeDiffView(taskId);
        var modelId = userInputPanel.getCurrentModelId();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                ConversationCommandService.resetConversation(taskId, taskId, modelId, project);
                ApplicationManager.getApplication().invokeLater(() -> {
                    clearImageAttachments();
                    clearAllTags();
                    userInputPanel.clearPrompt();
                    operationPanel.updateVisibility(false);
                    operationPanel.waitUserInput(null);
                    scrollablePanel.clearAll();
                    cardLayout.show(centerPanel, "welcome");
                    // 重置 tab 标题为新的 "Chat X" 格式
                    resetTabTitle();
                });
            } catch (Exception e) {
                var message = e.getMessage() == null ? "unknown error" : e.getMessage();
                ApplicationManager.getApplication().invokeLater(() -> appendCommandMessage("/clear failed: " + message));
            }
        });
    }

    /**
     * 重置当前 tab 的标题为新的 "Chat X" 格式
     */
    private void resetTabTitle() {
        JComponent component = ChatXToolWindowFactory.getToolWindow().getContentManager()
                .findContent(ChatXToolWindowFactory.TAB_AGENT_NAME).getComponent();
        if (component instanceof SmartToolWindowPanel smartToolWindowPanel) {
            smartToolWindowPanel.getChatTabbedPane().resetTabTitle(taskId);
        }
    }

    private void executeCompactCommand() {
        if (taskId == null || taskId.isBlank()) {
            appendCommandMessage("/compact failed: no active conversation.");
            return;
        }
        var modelId = userInputPanel.getCurrentModelId();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                CompactResult result = ConversationCommandService.compactConversation(taskId, taskId, modelId, project);
                ApplicationManager.getApplication().invokeLater(() -> appendCompactSummary(result.getSummary()));
            } catch (Exception e) {
                var message = e.getMessage() == null ? "unknown error" : e.getMessage();
                ApplicationManager.getApplication().invokeLater(() -> appendCommandMessage("/compact failed: " + message));
            }
        });
    }

    private void appendCompactSummary(String summary) {
        cardLayout.show(centerPanel, "scroll");
        var message = new Message();
        message.setPrompt("/compact");
        var messagePanel = scrollablePanel.addMessage(message.getId());
        messagePanel.add(getUserMessagePanel(message));

        var responsePanel = new ResponseMessagePanel();
        var responseBody = new TaskMessageResponseBody(project, this).withResponse(summary);
        responsePanel.addContent(responseBody);
        responsePanel.addCopyAction(() -> CopyAction.copyToClipboard(summary));
        messagePanel.add(responsePanel);
        scrollablePanel.scrollToBottom();
    }

    private void appendCommandMessage(String messageText) {
        cardLayout.show(centerPanel, "scroll");
        var responsePanel = new ResponseMessagePanel();
        var responseBody = new TaskMessageResponseBody(project, this).withResponse(messageText);
        responsePanel.addContent(responseBody);
        responsePanel.addCopyAction(() -> CopyAction.copyToClipboard(messageText));
        var messagePanel = scrollablePanel.addMessage(UUID.randomUUID());
        messagePanel.add(responsePanel);
        scrollablePanel.scrollToBottom();
    }

    private void clearImageAttachments() {
        var toolWindow = ChatXToolWindowFactory.getToolWindow();
        var content = toolWindow.getContentManager().findContent(ChatXToolWindowFactory.TAB_AGENT_NAME);
        if (content != null && content.getComponent() instanceof SmartToolWindowPanel smartToolWindowPanel) {
            smartToolWindowPanel.clearImageNotifications(project);
            return;
        }
        project.putUserData(IMAGE_ATTACHMENT_FILE_PATH, null);
    }

    private Unit handleCancel() {
        if (requestHandler != null) {
            requestHandler.cancel(taskId);
        }
        return Unit.INSTANCE;
    }

    private Unit handleQuickAsk(String text) {
        return handleSubmit(text);
    }

    private boolean hasReferencedFilePaths(Message message) {
        return message.getReferencedFilePaths() != null && !message.getReferencedFilePaths().isEmpty();
    }

    private boolean hasReferencedFilePaths(Conversation conversation) {
        return conversation.getMessages().stream().anyMatch(it -> it.getReferencedFilePaths() != null && !it.getReferencedFilePaths().isEmpty());
    }


    private Runnable buildPlanExecuteAction(TaskCompletionParameters params) {
        return () -> ApplicationManager.getApplication().invokeLater(() -> {
            userInputPanel.setChatMode(ChatMode.Companion.getAGENT());
            userInputPanel.requestFocus();
            userInputPanel.setPromptText("按计划执行");
            userInputPanel.repaint();
        });
    }

    private Runnable buildPlanContinueAction(TaskCompletionParameters params) {
        return () -> ApplicationManager.getApplication().invokeLater(() -> {
            userInputPanel.setChatMode(ChatMode.Companion.getPLAN());
            userInputPanel.requestFocus();
        });
    }


    private void call(TaskCompletionParameters callParameters, ResponseMessagePanel responseMessagePanel, UserMessagePanel userMessagePanel) {
        userInputPanel.setSubmitEnabled(false);
        userMessagePanel.disableActions(List.of("RELOAD", "CHECKPOINT", "DELETE"));
        responseMessagePanel.disableActions(List.of("COPY"));
        requestHandler = new TaskCompletionRequestHandler(project, new SmartCompletionResponseEventListener(project, operationPanel, userMessagePanel, responseMessagePanel, userInputPanel,
                this::buildPlanExecuteAction,
                this::buildPlanContinueAction,
                callParameters) {
            @Override
            public void handleTokensExceededPolicyAccepted() {
                call(callParameters, responseMessagePanel, userMessagePanel);
            }
        });
        ApplicationManager.getApplication().executeOnPooledThread(() -> requestHandler.call(callParameters));
    }

    private UserMessagePanel createUserMessagePanel(Message message, TaskCompletionParameters callParameters) {
        var panel = new UserMessagePanel(project, message, this);
        panel.addCopyAction(() -> CopyAction.copyToClipboard(message.getPrompt()));
        panel.addReloadAction(() -> reloadMessage(callParameters, panel));
        configureCheckpointAction(panel, message);
        return panel;
    }

    private void configureCheckpointAction(UserMessagePanel panel, Message message) {
        final String userMessageId = message.getId().toString();
        panel.addCheckpointAction(
                () -> CheckpointStorage.getChangedFiles(project, taskId, userMessageId),
                () -> restoreFromCheckpoint(message, panel)
        );
        panel.setCheckpointEnabled(CheckpointStorage.hasChangedFiles(project, taskId, userMessageId));
    }

    private void restoreFromCheckpoint(Message message, UserMessagePanel userMessagePanel) {
        final String userMessageId = message.getId().toString();
        final List<String> changedFiles = CheckpointStorage.getChangedFiles(project, taskId, userMessageId);
        handleCancel();
        operationPanel.updateVisibility(false);
        operationPanel.waitUserInput(null);
        userInputPanel.setSubmitEnabled(false);
        userMessagePanel.setCheckpointEnabled(false);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                CheckpointStorage.restoreCheckpointAndContext(project, taskId, userMessageId);
                CheckpointStorage.clearCheckpointsFromMessage(project, taskId, userMessageId);
                TodoStorage.clearConversationCache(taskId);
                TaskState.clearCachedServices(taskId);

                ApplicationManager.getApplication().invokeLater(() -> {
                    reloadFilesFromDisk(changedFiles);
                    userInputPanel.setSubmitEnabled(true);
                    NotifyUtils.notify(
                            project,
                            "Checkpoint restored",
                            "File changes and runtime context were restored. Existing messages stay visible, but won't be used as next-round context.",
                            NotificationType.INFORMATION,
                            null
                    );
                });
            } catch (Exception ex) {
                LOG.warn("Failed to restore checkpoint for message " + userMessageId, ex);
                ApplicationManager.getApplication().invokeLater(() -> {
                    userInputPanel.setSubmitEnabled(true);
                    userMessagePanel.setCheckpointEnabled(CheckpointStorage.hasChangedFiles(project, taskId, userMessageId));
                    var detail = ex.getMessage();
                    if (detail == null || detail.isBlank()) {
                        detail = ExceptionUtil.getThrowableText(ex);
                    }
                    NotifyUtils.notify(project, "Checkpoint restore failed", detail, NotificationType.ERROR, null);
                });
            }
        });
    }

    private void reloadFilesFromDisk(List<String> filePaths) {
        if (filePaths == null || filePaths.isEmpty()) {
            return;
        }
        var localFileSystem = LocalFileSystem.getInstance();
        filePaths.forEach(localFileSystem::refreshAndFindFileByPath);
        var virtualFiles = filePaths.stream()
                .map(FileUtil.INSTANCE::resolveVirtualFile)
                .filter(Objects::nonNull)
                .toArray(VirtualFile[]::new);
        if (virtualFiles.length > 0) {
            FileUtil.INSTANCE.reloadFilesFromDisk(virtualFiles);
        }
    }

    private JComponent getLandingView() {
        return new ChatToolWindowLandingPanel((action, locationOnScreen) -> {
            var editor = EditorUtil.getSelectedEditor(project);
            if (editor == null || !editor.getSelectionModel().hasSelection()) {
                OverlayUtil.showWarningBalloon(editor == null ? "Unable to locate a selected editor" : "Please select a target code before proceeding", locationOnScreen);
                return Unit.INSTANCE;
            }

            var formattedCode = CompletionRequestUtil.formatCode(Objects.requireNonNull(editor.getSelectionModel().getSelectedText()), editor.getVirtualFile().getPath());
            var message = new Message(action.getPrompt().replace("{SELECTION}", formattedCode));
            sendMessage(message);
            return Unit.INSTANCE;
        });
    }


    private void reloadMessage(TaskCompletionParameters prevParameters, UserMessagePanel userMessagePanel) {
        var prevMessage = prevParameters.getMessage();
        ResponseMessagePanel responsePanel = null;
        try {
            responsePanel = scrollablePanel.getResponseMessagePanel(prevMessage.getId());
            ((TaskMessageResponseBody) responsePanel.getContent()).clear();
            scrollablePanel.update();
        } catch (Exception e) {
            throw new RuntimeException("Could not delete the existing message component", e);
        } finally {
            LOG.debug("Reloading message: " + prevMessage.getId());

            if (responsePanel != null) {
                prevMessage.setResponse("");
                // conversationService.saveMessage(conversation, prevMessage);
                 call(prevParameters.toBuilder().retry(true).build(), responsePanel, userMessagePanel);
            }
        }
    }
}
