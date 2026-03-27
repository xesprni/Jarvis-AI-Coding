package com.qihoo.finance.lowcode.smartconversation.service;


import com.qihoo.finance.lowcode.smartconversation.utils.ErrorDetails;

/**
 * @author weiyichao
 * @date 2025-09-30
 **/
public interface CompletionEventListener<T> {

    default void onOpen() {
    }

    default void onEvent(String data) {
    }

    default void onThinking(String thinking) {
    }

    default void onMessage(T message, String rawMessage) {
    }

    default void onMessage(T message) {
    }

    default void onToolMessage(String eventId, T message, boolean isPartial) {
    }

    default void onPartialMessage(String taskId, boolean hasAskUserQuestion) {
    }

    default void onPartialMessageComplete(String taskId, String askId, Boolean hasCustomInput, Boolean pass) {
    }

    default void onComplete(StringBuilder messageBuilder) {
    }

    default void onCancelled(StringBuilder messageBuilder) {
    }

    default void onError(ErrorDetails error, Throwable ex) {
    }
}

