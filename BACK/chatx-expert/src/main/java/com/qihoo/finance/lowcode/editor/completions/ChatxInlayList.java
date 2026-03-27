package com.qihoo.finance.lowcode.editor.completions;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ChatxInlayList extends Iterable<ChatxEditorInlay> {

    boolean isEmpty();

    @NotNull
    ChatxCompletion getChatxCompletion();

    @NotNull
    TextRange getReplacementRange();

    @NotNull
    String getReplacementText();

    @NotNull
    List<ChatxEditorInlay> getInlays();

    int getOffset();

    void setReplacementText(String text);

    void setReplacementRange(TextRange range);
}
