package com.qihoo.finance.lowcode.editor.completions;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Data
@RequiredArgsConstructor
public class DefaultChatxEditorInlay implements ChatxEditorInlay {

    @NotNull
    private final ChatxCompletionType type;
    private final int editorOffset;
    @NotNull
    private final List<String> completionLines;

    @Override
    public @NotNull List<String> getLines() {
        return completionLines;
    }
}
