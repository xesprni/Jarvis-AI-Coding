package com.qihoo.finance.lowcode.editor.completions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ChatxCompletion {

    @NotNull
    List<String> getCompletion();

    @NotNull
    ChatxCompletion asCached();

    boolean isCached();

    @Nullable
    ChatxCompletion withoutPrefix(@NotNull String prefix);

    @NotNull
    ChatxCompletion withCompletion(@NotNull List<String> completion);
}
