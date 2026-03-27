package com.qihoo.finance.lowcode.editor.completions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.ThreadSafe;
import java.util.List;

@ThreadSafe
public interface CompletionCache {
    boolean isLatestPrefix(@NotNull String prefix);

    @Nullable
    List<ChatxCompletion> get(@NotNull String prompt, boolean isMultiline);

    @Nullable
    List<ChatxCompletion> getLatest(@NotNull String prefix, int tabWidth);

    void add(@NotNull String prefix, @NotNull String prompt, boolean isMultiline, @NotNull ChatxCompletion item);

    void updateLatest(@NotNull String prefix, @NotNull String prompt, boolean isMultiline);

    void clear();
}
