package com.qihoo.finance.lowcode.convertor.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.qihoo.finance.lowcode.convertor.dialog.GenConvertClassDiaglog;
import org.jetbrains.annotations.NotNull;

public class GenConvertClassAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (!(psiFile instanceof PsiJavaFile) || e.getProject() == null) {
            Messages.showMessageDialog("请在一个Java文件中操作", "提示", Messages.getInformationIcon());
            return;
        }
        new GenConvertClassDiaglog(e.getProject(), (PsiJavaFile)psiFile, editor).show();
    }
}
