package com.qihoo.finance.lowcode.editor.statusBar;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Pair;
import com.qihoo.finance.lowcode.status.ChatxStatus;
import com.qihoo.finance.lowcode.status.ChatxStatusService;
import org.jetbrains.annotations.NotNull;

class StatusItemAction extends AnAction implements DumbAware {
    public void update(@NotNull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        presentation.setEnabled(false);
        Pair<ChatxStatus, String> status = ChatxStatusService.getCurrentStatus();
        presentation.setDisabledIcon(((ChatxStatus)status.first).getIcon());
        presentation.setText("YEEAH!");
    }

    public void actionPerformed(@NotNull AnActionEvent e) {
    }
}