package com.qihoo.finance.lowcode.settings.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.qihoo.finance.lowcode.editor.action.ChatxAction;
import com.qihoo.finance.lowcode.settings.ui.CodeCompletionSettingForm;
import org.jetbrains.annotations.NotNull;

public class EditSettingAction extends AnAction implements ChatxAction, DumbAware {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        ShowSettingsUtil.getInstance().showSettingsDialog(project, CodeCompletionSettingForm.NAME);
    }
}
