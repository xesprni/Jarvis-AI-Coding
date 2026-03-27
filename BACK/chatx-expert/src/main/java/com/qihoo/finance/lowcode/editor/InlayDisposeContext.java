package com.qihoo.finance.lowcode.editor;

public enum InlayDisposeContext {

    UserAction, IdeCompletion, CaretChange, SelectionChange, SettingsChange, Cycling, TypingAsSuggested, Typing, Applied;

    public boolean isResetLastRequest() {
        return (this == SettingsChange || this == Applied);
    }

    public boolean isSendRejectedTelemetry() {
        return (this == UserAction);
    }
}
