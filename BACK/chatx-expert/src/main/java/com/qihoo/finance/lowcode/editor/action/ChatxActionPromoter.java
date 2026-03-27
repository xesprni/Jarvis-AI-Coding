package com.qihoo.finance.lowcode.editor.action;

import com.intellij.openapi.actionSystem.ActionPromoter;
import com.intellij.openapi.actionSystem.ActionWithDelegate;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.qihoo.finance.lowcode.editor.ChatxEditorManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ChatxActionPromoter implements ActionPromoter {

    public List<AnAction> promote(@NotNull List<? extends AnAction> actions, @NotNull DataContext context) {
        if (isDisabledForEditor(CommonDataKeys.EDITOR.getData(context))) {
            return null;
        }
        if (actions.stream().noneMatch(action -> (action instanceof ChatxAction && action instanceof EditorAction))) {
            return null;
        }
        ArrayList<AnAction> result = new ArrayList<>(actions);
        result.sort((a, b) -> {
            boolean aIsChatx = (a instanceof ChatxAction && a instanceof EditorAction);
            boolean bIsChatx = (b instanceof ChatxAction && b instanceof EditorAction);
            if (aIsChatx && bIsChatx) {
                return 0;
            }
            return (isIdeaVimAction(a) || isIdeaVimAction(b)) ? 0 : (aIsChatx ? -1 : (bIsChatx ? 1 : 0));
        });
        return result;
    }

    private boolean isDisabledForEditor(Editor editor) {
        return (editor == null ||
                !ChatxEditorManager.getInstance().isAvailable(editor) || (
                !ChatxApplyInlaysAction.isSupported(editor) && !ChatxDisposeInlaysEditorHandler.isEditorSupported(editor)));
    }

    public static boolean isIdeaVimAction(@NotNull AnAction action) {
        String packagePrefix = "com.maddyhome.idea.vim";
        if (action.getClass().getName().startsWith(packagePrefix))
            return true;
        if (action instanceof ActionWithDelegate) {
            Object delegate = ((ActionWithDelegate)action).getDelegate();
            return delegate.getClass().getName().startsWith(packagePrefix);
        }
        return false;
    }
}
