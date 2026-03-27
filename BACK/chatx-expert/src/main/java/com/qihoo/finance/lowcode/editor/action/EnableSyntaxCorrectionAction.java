package com.qihoo.finance.lowcode.editor.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.qihoo.finance.lowcode.editor.ChatxService;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;
import org.jetbrains.annotations.NotNull;

public class EnableSyntaxCorrectionAction extends AnAction implements ChatxAction, DumbAware {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        boolean enableSyntaxCorrection = ChatxApplicationSettings.settings().enableSyntaxCorrection;
        ChatxApplicationSettings.settings().enableSyntaxCorrection = !enableSyntaxCorrection;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        // 未登录不启用语法纠错
        if (!ChatxService.getInstance().isSignedIn()) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        boolean enableSyntaxCorrection = ChatxApplicationSettings.settings().enableSyntaxCorrection;
        if (enableSyntaxCorrection) {
            e.getPresentation().setText("禁用语法纠错");
        } else {
            e.getPresentation().setText("启用语法纠错");
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
