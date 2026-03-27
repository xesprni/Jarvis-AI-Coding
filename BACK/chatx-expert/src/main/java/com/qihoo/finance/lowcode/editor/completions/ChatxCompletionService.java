package com.qihoo.finance.lowcode.editor.completions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import com.intellij.util.concurrency.annotations.RequiresEdt;
import com.qihoo.finance.lowcode.common.constants.CompletionType;
import com.qihoo.finance.lowcode.editor.request.EditorRequest;
import com.qihoo.finance.lowcode.editor.telemetry.LogTelemetries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.Flow;

public interface ChatxCompletionService {

    static ChatxCompletionService getInstance() {
        return ApplicationManager.getApplication().getService(ChatxCompletionService.class);
    }

    boolean isAvailable(@NotNull Editor paramEditor);

    @Nullable
    EditorRequest createRequest(@NotNull Editor editor, int offset, @NotNull CompletionType completionType);

    @RequiresBackgroundThread
    boolean fetchCompletions(@NotNull EditorRequest request, @Nullable Integer maxCompletions, boolean cycling
            , @NotNull Flow.Subscriber<List<ChatxInlayList>> subscriber, Editor editor);

    @RequiresBackgroundThread
    default boolean fetchCompletions(@NotNull EditorRequest request, @Nullable Integer maxCompletions
            , @NotNull Flow.Subscriber<List<ChatxInlayList>> subscriber, Editor editor) {
        return fetchCompletions(request, maxCompletions, false, subscriber, editor);
    }

    @RequiresEdt
    @Nullable
    List<ChatxInlayList> fetchCachedCompletions(@NotNull EditorRequest request);

    void sendShownTelemetry(EditorRequest request, @NotNull ChatxCompletion chatxCompletion, Editor editor);

    LogTelemetries getLogTelemetries();

    void sendAcceptedTelemetry(@NotNull ChatxCompletion chatxCompletion);
    void sendRejectedTelemetry(@NotNull ChatxCompletion chatxCompletion);
}
