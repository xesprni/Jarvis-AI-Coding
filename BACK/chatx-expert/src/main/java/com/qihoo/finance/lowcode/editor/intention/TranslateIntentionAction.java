package com.qihoo.finance.lowcode.editor.intention;

import com.intellij.codeInsight.intention.FileModifier;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.java.PsiIdentifierImpl;
import com.intellij.util.IncorrectOperationException;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * TranslateAction
 *
 * @author fengjinfu-jk
 * date 2024/7/23
 * @version 1.0.0
 * @apiNote TranslateAction
 */
public class TranslateIntentionAction extends PsiElementBaseIntentionAction {

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        TranslateAction.showLookup(editor, element);
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        String selectedText = editor.getSelectionModel().getSelectedText();
        return StringUtils.isNotBlank(selectedText) || element instanceof PsiIdentifierImpl;
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return getText();
    }

    @NotNull
    @Override
    public String getText() {
        return String.format("「Jarvis」命名优化            %s", SystemInfo.isMac ? "Control + Meta + O" : "Ctrl + Shift + X");
    }

    @Override
    public @Nullable FileModifier getFileModifierForPreview(@NotNull PsiFile target) {
        // 禁用预览功能，因为该意图需要执行后台任务和显示 Lookup，这些操作在预览模式下不被允许
        return null;
    }

    @Override
    public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        // 显式禁用预览，返回空预览
        return IntentionPreviewInfo.EMPTY;
    }
}
