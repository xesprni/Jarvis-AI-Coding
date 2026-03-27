package com.qihoo.finance.lowcode.editor;

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class TypeOverHandler extends TypedHandlerDelegate {

    private static final Key<Long> TYPE_OVER_STAMP = Key.create("chatx.typeOverStamp");

    public static boolean getPendingTypeOverAndReset(@NotNull Editor editor) {
        Long stamp = TYPE_OVER_STAMP.get((UserDataHolder)editor);
        if (stamp == null) {
            return false;
        }
        TYPE_OVER_STAMP.set((UserDataHolder)editor, null);
        return (stamp == editor.getDocument().getModificationStamp());
    }

    @Override
    public @NotNull Result beforeCharTyped(char c, @NotNull Project project, @NotNull Editor editor
            , @NotNull PsiFile file, @NotNull FileType fileType) {
        boolean validTypeOver = (c == ')' || c == ']' || c == '}' || c == '"' || c == '\'' || c == '>' || c == ';');
        if (validTypeOver && CommandProcessor.getInstance().getCurrentCommand() != null) {
            TYPE_OVER_STAMP.set((UserDataHolder)editor, editor.getDocument().getModificationStamp());
        } else {
            TYPE_OVER_STAMP.set((UserDataHolder)editor, null);
        }
        return Result.CONTINUE;
    }
}
