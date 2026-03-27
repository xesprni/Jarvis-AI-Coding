package com.qihoo.finance.lowcode.editor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.concurrency.annotations.RequiresEdt;
import org.jetbrains.annotations.NotNull;

public interface ChatxEditorManager extends Disposable {

    static ChatxEditorManager getInstance() {
        return ApplicationManager.getApplication().getService(ChatxEditorManager.class);
    }

    @RequiresEdt
    boolean isAvailable(@NotNull Editor editor);

    default boolean hasCompletionInlays(@NotNull Editor editor) {
        if (!isAvailable(editor)) {
            return false;
        }
        return (countCompletionInlays(editor, TextRange.from(0, editor.getDocument().getTextLength())
                , true, true, true, true) > 0);
    }

    @RequiresEdt
    int countCompletionInlays(@NotNull Editor editor, @NotNull TextRange searchRange, boolean inlineInlays
            , boolean afterLineEndInlays, boolean blockInlays, boolean matchInLeadingWhitespace);

    void disposeInlays(@NotNull Editor editor, @NotNull InlayDisposeContext disposeContext);

    default void editorModified(@NotNull Editor editor, @NotNull CompletionRequestType requestType) {
        editorModified(editor, editor.getCaretModel().getOffset(), requestType);
    }

    @RequiresEdt
    void editorModified(@NotNull Editor editor, int offset, @NotNull CompletionRequestType requestType);

    @RequiresEdt
    void cancelCompletionRequests(@NotNull Editor paramEditor);

    @RequiresEdt
    boolean applyCompletion(@NotNull Editor editor, ChatxApplyInlayStrategy applyStrategy);

    /**
     * 当前键入的内容，是否有对应的补全缓存
     * @param editor
     * @param next 下一个敲入的字符
     * @return
     */
    boolean hasTypingAsSuggestedData(Editor editor, char next);
}
