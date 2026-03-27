package com.qihoo.finance.lowcode.smartconversation.configuration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.qifu.agent.AgentMessageType;
import com.qifu.agent.parser.Segment;
import com.qifu.ui.smartconversation.panels.ResponseMessagePanel;
import com.qifu.ui.smartconversation.panels.UserInputPanel;
import com.qifu.ui.smartconversation.panels.UserMessagePanel;
import com.qifu.ui.smartconversation.settings.service.TaskCompletionParameters;
import com.qifu.ui.smartconversation.sse.CompletionProgressNotifier;
import com.qifu.utils.CheckpointStorage;
import com.qihoo.finance.lowcode.smartconversation.conversations.TaskMessageResponseBody;
import com.qihoo.finance.lowcode.smartconversation.panels.OperationPanel;
import com.qihoo.finance.lowcode.smartconversation.service.CompletionResponseEventListener;
import com.qihoo.finance.lowcode.smartconversation.utils.ErrorDetails;
import com.qifu.ui.smartconversation.settings.configuration.ChatMode;

import java.util.function.Function;


public abstract class SmartCompletionResponseEventListener implements CompletionResponseEventListener {

    private static final Logger LOG = Logger.getInstance(SmartCompletionResponseEventListener.class);

    private final Project project;
    private final ResponseMessagePanel responsePanel;
    private final UserMessagePanel userMessagePanel;
    private final TaskMessageResponseBody responseContainer;
    private final UserInputPanel textArea;
    private final OperationPanel operationPanel;
    private final Function<TaskCompletionParameters, Runnable> planExecuteHandler;
    private final Function<TaskCompletionParameters, Runnable> planContinueHandler;
    private final TaskCompletionParameters requestParameters;

    public SmartCompletionResponseEventListener(Project project,
                                               OperationPanel operationPanel,
                                               UserMessagePanel userMessagePanel,
                                               ResponseMessagePanel responsePanel,
                                               UserInputPanel textArea,
                                               Function<TaskCompletionParameters, Runnable> planExecuteHandler,
                                               Function<TaskCompletionParameters, Runnable> planContinueHandler,
                                               TaskCompletionParameters requestParameters) {
        this.project = project;
        this.operationPanel = operationPanel;
        this.userMessagePanel = userMessagePanel;
        this.responsePanel = responsePanel;
        this.responseContainer = (TaskMessageResponseBody) responsePanel.getContent();
        this.textArea = textArea;
        this.planExecuteHandler = planExecuteHandler;
        this.planContinueHandler = planContinueHandler;
        this.requestParameters = requestParameters;
    }

    public abstract void handleTokensExceededPolicyAccepted();

    @Override
    public void handleRequestOpen() {
        LOG.info("Task 链接已建立....");
    }

    @Override
    public void handleMessage(Segment segment) {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                responseContainer.withResponse(segment);
            } catch (Exception e) {
                responseContainer.displayError("Something went wrong.");
                throw new RuntimeException("Error while updating the content", e);
            }
        });
    }

    @Override
    public void handleToolMessage(String eventId, Segment segment, boolean isPartial) {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                responseContainer.withToolResponse(eventId, segment, isPartial);
            } catch (Exception e) {
                responseContainer.displayError("Something went wrong.");
                throw new RuntimeException("Error while updating the content", e);
            }
        });
    }

    @Override
    public void handlePartialMessage(String taskId, boolean hasAskUserQuestion) {
        this.stopStreaming(responseContainer, false);
        this.operationPanel.updateVisibility(!hasAskUserQuestion);
        this.operationPanel.waitUserInput(taskId);
    }

    @Override
    public void handlePartialMessageComplete(String taskId, String askId, Boolean hasCustomInput, Boolean pass) {
        this.operationPanel.updateVisibility(false);
        this.operationPanel.waitUserInput(null);

        if (!hasCustomInput) {
            if (!pass) {
                responseContainer.toolPanelRemoveAll();
                if (AgentMessageType.AUTO_APPROVAL_MAX_REQ_REACHED.name().equals(askId) || AgentMessageType.API_REQ_FAILED.name().equals(askId)) {
                    return;
                }
            }
            textArea.setSubmitEnabled(false);
            userMessagePanel.enableAllActions(false);
            responsePanel.enableAllActions(false);
            responseContainer.startLoading();
            CompletionProgressNotifier.update(project, true);
        }
    }

    @Override
    public void handleError(ErrorDetails error, Throwable ex) {
        ApplicationManager.getApplication().invokeLater(() -> {
            boolean hasFileChanges = hasCheckpointChanges(requestParameters);
            try {
                if ("insufficient_quota".equals(error.getCode())) {
                    responseContainer.displayQuotaExceeded();
                } else {
                    responseContainer.displayError(error.getMessage());
                }
            } finally {
                stopStreaming(responseContainer, hasFileChanges);
            }
        });
    }


    @Override
    public void handleCompleted(String fullMessage, TaskCompletionParameters callParameters) {
        ApplicationManager.getApplication().invokeLater(() -> {
            boolean hasFileChanges = hasCheckpointChanges(callParameters);
            try {
                responsePanel.enableAllActions(true);
                if (!fullMessage.isEmpty()) {
                    responseContainer.withResponse(fullMessage);
                }
                if (ChatMode.Companion.getPLAN().equals(callParameters.getChatMode()) && planExecuteHandler != null && planContinueHandler != null) {
                    Runnable execute = planExecuteHandler.apply(callParameters);
                    Runnable continuePlan = planContinueHandler.apply(callParameters);
                    responseContainer.showPlanActions(execute, continuePlan);
                }
            } finally {
                stopStreaming(responseContainer, hasFileChanges);
            }
        });
    }

    private void stopStreaming(TaskMessageResponseBody responseContainer, boolean checkpointEnabled) {
        textArea.setSubmitEnabled(true);
        userMessagePanel.enableAllActions(true);
        userMessagePanel.setCheckpointEnabled(checkpointEnabled);
        responsePanel.enableAllActions(true);
        responseContainer.stopLoading();
        responseContainer.hideCaret();
        // 流式渲染结束后，重新处理表格，为其添加横向滚动条
        responseContainer.rebuildTablesWithScrollPane();
        CompletionProgressNotifier.update(project, false);
    }

    private boolean hasCheckpointChanges(TaskCompletionParameters params) {
        try {
            return CheckpointStorage.hasChangedFiles(project, params.getTaskId(), params.getMessage().getId().toString());
        } catch (Exception e) {
            LOG.warn("Failed to read checkpoint status", e);
            return false;
        }
    }
}
