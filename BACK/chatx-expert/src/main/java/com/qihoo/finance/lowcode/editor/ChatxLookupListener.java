package com.qihoo.finance.lowcode.editor;

import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupManagerListener;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;
import org.jetbrains.annotations.Nullable;

/**
 * Idea 内置的代码提示弹框监听器
 */
public class ChatxLookupListener implements LookupManagerListener {

    private final static Logger LOG = Logger.getInstance(ChatxLookupListener.class);

    @Override
    public void activeLookupChanged(@Nullable Lookup oldLookup, @Nullable Lookup newLookup) {
        LOG.debug("activeLookupChanged: oldLookup={}, newLookup={}", oldLookup, newLookup);
        Lookup validLookup = (newLookup != null) ? newLookup : oldLookup;
        PsiFile psiFile = (validLookup != null) ? ReadAction.compute(validLookup::getPsiFile) : null;
        // 如果没启用代码补全，不做任何处理
        if (psiFile != null && !ChatxApplicationSettings.isChatxEnabled(psiFile)) {
            return;
        }
        ChatxEditorManager editorManager = ChatxEditorManager.getInstance();
        if (oldLookup != null && newLookup == null) {
            // 之前展示了系统弹框，但是现在系统弹框关闭了，需要调用Chatx的补全功能
            PsiFile file = ReadAction.compute(oldLookup::getPsiFile);
            if (file != null) {
                Editor editor = ReadAction.compute(oldLookup::getEditor);
                if (ChatxEditorUtil.isSelectedEditor(editor) && editorManager.isAvailable(editor) &&
                        !editor.getDocument().isInBulkUpdate())
                    editorManager.editorModified(editor, CompletionRequestType.Forced);
            }
        } else if (newLookup != null && oldLookup == null && !ChatxApplicationSettings.settings().isShowIdeCompletions()) {
            // 之前没有展示过系统弹框，现在展示系统弹框了，需要销毁补全(否则按Tab键使用的是系统的补全，造成干扰)
            Editor editor = ReadAction.compute(newLookup::getEditor);
            if (editorManager.isAvailable(editor)) {
                editorManager.cancelCompletionRequests(editor);
                editorManager.disposeInlays(editor, InlayDisposeContext.IdeCompletion);
            }
        }
    }
}
