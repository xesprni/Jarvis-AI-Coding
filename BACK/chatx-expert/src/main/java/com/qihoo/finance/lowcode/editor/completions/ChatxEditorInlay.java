package com.qihoo.finance.lowcode.editor.completions;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ChatxEditorInlay {

    @NotNull
    ChatxCompletionType getType();

    @NotNull
    List<String> getLines();

    int getEditorOffset();

    default boolean isEmptyCompletion() {
        List<String> completion = getLines();
        return (completion.isEmpty() || (completion.size() == 1 && completion.get(0).isEmpty()));
    }
}
