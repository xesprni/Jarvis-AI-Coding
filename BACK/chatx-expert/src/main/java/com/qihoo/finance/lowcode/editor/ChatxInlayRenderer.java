package com.qihoo.finance.lowcode.editor;

import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.qihoo.finance.lowcode.editor.completions.ChatxCompletionType;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ChatxInlayRenderer extends EditorCustomElementRenderer {

    @NotNull
    List<String> getContentLines();

    @Nullable
    Inlay<ChatxInlayRenderer> getInlay();

    @NotNull
    ChatxCompletionType getType();
}
