package com.qihoo.finance.lowcode.editor.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.qihoo.finance.lowcode.editor.ChatxService;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class LoginToChatxAction extends AnAction implements DumbAware, ChatxAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled((e.getProject() != null));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ChatxService.getInstance().loginInteractive(Objects.requireNonNull(e.getProject()));
    }
}
