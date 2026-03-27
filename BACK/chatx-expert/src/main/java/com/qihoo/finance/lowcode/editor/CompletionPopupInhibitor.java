package com.qihoo.finance.lowcode.editor;

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;
import org.jetbrains.annotations.NotNull;

public class CompletionPopupInhibitor extends TypedHandlerDelegate {

    private static final Logger LOG = Logger.getInstance(CompletionPopupInhibitor.class);

    @Override
    public @NotNull Result checkAutoPopup(char charTyped, @NotNull Project project, @NotNull Editor editor
            , @NotNull PsiFile file) {
        boolean showIdeCompletions = ChatxApplicationSettings.settings().isShowIdeCompletions();
        if (!showIdeCompletions && ChatxEditorManager.getInstance().hasTypingAsSuggestedData(editor, charTyped)) {
            LOG.debug("inhibiting IDE completion popup because typing-as-suggested is available");
            return Result.STOP;
        }
        return super.checkAutoPopup(charTyped, project, editor, file);
    }
}
