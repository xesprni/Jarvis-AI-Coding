package com.qihoo.finance.lowcode.editor.action;

import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.qihoo.finance.lowcode.editor.ChatxEditorManager;
import com.qihoo.finance.lowcode.editor.InlayDisposeContext;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class ChatxDisposeInlaysEditorHandler extends EditorActionHandler {

    private static final Logger LOG = Logger.getInstance(ChatxDisposeInlaysEditorHandler.class);

    @Nullable
    private final EditorActionHandler baseHandler;

    @Override
    protected boolean isEnabledForCaret(@NotNull Editor editor, @NotNull Caret caret, DataContext dataContext) {
        return isEditorSupported(editor)
                || (this.baseHandler != null && this.baseHandler.isEnabled(editor, caret, dataContext));
    }

    @Override
    public boolean executeInCommand(@NotNull Editor editor, DataContext dataContext) {
        return (this.baseHandler != null && this.baseHandler.executeInCommand(editor, dataContext));
    }

    @Override
    protected void doExecute(@NotNull Editor editor, @Nullable Caret caret, DataContext dataContext) {
        if (isEditorSupported(editor)) {
            // 禁用一段时间补全
            long fetchCompletionAfter = System.currentTimeMillis() + ChatxApplicationSettings.settings()
                    .disableCompletionIntervalSeconds * 1000L;
            ChatxApplicationSettings.settings().fetchCompletionAfter = fetchCompletionAfter;
            LOG.debug("Disable completion until:" + fetchCompletionAfter);
            ChatxEditorManager.getInstance().disposeInlays(editor, InlayDisposeContext.CaretChange);
        }
        if (this.baseHandler != null && this.baseHandler.isEnabled(editor, caret, dataContext))
            this.baseHandler.execute(editor, caret, dataContext);
    }

    static boolean isEditorSupported(@NotNull Editor editor) {
        ChatxEditorManager manager = ChatxEditorManager.getInstance();
        return manager.isAvailable(editor) && manager.hasCompletionInlays(editor)
                && LookupManager.getActiveLookup(editor) == null;
    }
}


