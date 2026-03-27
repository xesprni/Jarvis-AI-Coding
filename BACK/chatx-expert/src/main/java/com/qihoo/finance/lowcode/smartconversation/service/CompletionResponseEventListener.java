package com.qihoo.finance.lowcode.smartconversation.service;

import com.qifu.agent.parser.Segment;
import com.qifu.ui.smartconversation.settings.service.TaskCompletionParameters;
import com.qihoo.finance.lowcode.smartconversation.utils.ErrorDetails;

public interface CompletionResponseEventListener {

    default void handleError(ErrorDetails error, Throwable ex) {
    }

    default void handleCompleted(String fullMessage) {
    }

    default void handleCompleted(String fullMessage, TaskCompletionParameters callParameters) {
    }
    default void handleRequestOpen() {
    }

    default void handleMessage(Segment segment) {
    }

    default void handleToolMessage(String eventId, Segment segment, boolean isPartial) {
    }

    default void handlePartialMessage(String taskId, boolean hasAskUserQuestion) {
    }

    default void handlePartialMessageComplete(String taskId, String askId, Boolean hasCustomInput,Boolean pass) {
    }
}
