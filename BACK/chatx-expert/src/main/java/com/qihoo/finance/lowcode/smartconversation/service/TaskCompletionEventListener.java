package com.qihoo.finance.lowcode.smartconversation.service;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.qifu.agent.parser.Segment;
import com.qifu.ui.smartconversation.settings.service.TaskCompletionParameters;
import com.qifu.ui.smartconversation.sse.CompletionProgressNotifier;
import com.qihoo.finance.lowcode.smartconversation.utils.ErrorDetails;

public class TaskCompletionEventListener implements CompletionEventListener<Segment> {

    private static final Logger LOG = Logger.getInstance(TaskCompletionEventListener.class);

    private final Project project;
    private final TaskCompletionParameters callParameters;
    private final CompletionResponseEventListener eventListener;
    private final StringBuilder messageBuilder = new StringBuilder();

    public TaskCompletionEventListener(
            Project project,
            TaskCompletionParameters callParameters,
            CompletionResponseEventListener eventListener) {
        this.project = project;
        this.callParameters = callParameters;
        this.eventListener = eventListener;
    }

    @Override
    public void onOpen() {
        eventListener.handleRequestOpen();
    }

    @Override
    public void onMessage(Segment message) {
        messageBuilder.append(message);
        callParameters.getMessage().setResponse(messageBuilder.toString());
        eventListener.handleMessage(message);
    }

    @Override
    public void onToolMessage(String eventId, Segment message, boolean isPartial) {
        messageBuilder.append(message);
        callParameters.getMessage().setResponse(messageBuilder.toString());
        eventListener.handleToolMessage(eventId, message, isPartial);
    }

    @Override
    public void onComplete(StringBuilder messageBuilder) {
        handleCompleted(messageBuilder);
    }

    @Override
    public void onCancelled(StringBuilder messageBuilder) {
        handleCompleted(messageBuilder);
    }

    @Override
    public void onError(ErrorDetails error, Throwable ex) {
        try {
            eventListener.handleError(error, ex);
        } finally {
            sendError(error, ex);
        }
    }

    @Override
    public void onPartialMessage(String taskId, boolean hasAskUserQuestion) {
        eventListener.handlePartialMessage(taskId, hasAskUserQuestion);
    }

    @Override
    public void onPartialMessageComplete(String taskId, String askId, Boolean hasCustomInput, Boolean pass) {
        eventListener.handlePartialMessageComplete(taskId, askId, hasCustomInput, pass);
    }

    private void handleCompleted(StringBuilder messageBuilder) {
        CompletionProgressNotifier.Companion.update(project, false);
        eventListener.handleCompleted(messageBuilder.toString(), callParameters);
    }

    private void sendError(ErrorDetails error, Throwable ex) {

    }
}
