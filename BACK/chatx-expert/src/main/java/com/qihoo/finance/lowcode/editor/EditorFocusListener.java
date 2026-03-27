package com.qihoo.finance.lowcode.editor;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * 用户切换编辑器的文件后，销毁掉旧编辑器文件的补全
 */
@RequiredArgsConstructor
public class EditorFocusListener implements FileEditorManagerListener {

    private final Project project;

    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        VirtualFile oldFile = event.getOldFile();
        if (oldFile == null || !oldFile.isValid())
            return;
        PsiFile psiFile = PsiManager.getInstance(this.project).findFile(oldFile);
        if (psiFile == null || !psiFile.isValid())
            return;
        FileEditor oldEditor = event.getOldEditor();
        if (oldEditor instanceof TextEditor) {
            Editor editor = ((TextEditor)oldEditor).getEditor();
            ChatxEditorManager.getInstance().disposeInlays(editor, InlayDisposeContext.UserAction);
        }
    }
}
