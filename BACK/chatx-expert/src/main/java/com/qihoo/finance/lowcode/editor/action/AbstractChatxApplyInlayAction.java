package com.qihoo.finance.lowcode.editor.action;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.project.DumbAware;
import com.qihoo.finance.lowcode.editor.ChatxApplyInlayStrategy;
import com.qihoo.finance.lowcode.editor.ChatxEditorManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AbstractChatxApplyInlayAction extends EditorAction implements DumbAware, ChatxAction {

    protected AbstractChatxApplyInlayAction(ChatxApplyInlayStrategy strategy) {
        super(new ApplyInlayHandler(strategy));
        setInjectedContext(true);
    }

    private static class ApplyInlayHandler extends EditorActionHandler {
        private final ChatxApplyInlayStrategy strategy;

        public ApplyInlayHandler(ChatxApplyInlayStrategy strategy) {
            this.strategy = strategy;
        }

        protected boolean isEnabledForCaret(@NotNull Editor editor, @NotNull Caret caret, DataContext dataContext) {
            return ChatxApplyInlaysAction.isSupported(editor);
        }

        public boolean executeInCommand(@NotNull Editor editor, DataContext dataContext) {
            return false;
        }

        protected void doExecute(@NotNull Editor editor, @Nullable Caret caret, DataContext dataContext) {
            ChatxEditorManager.getInstance().applyCompletion(editor, this.strategy);
        }
    }
}
