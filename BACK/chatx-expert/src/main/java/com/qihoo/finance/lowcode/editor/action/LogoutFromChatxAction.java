package com.qihoo.finance.lowcode.editor.action;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.qihoo.finance.lowcode.common.util.ChatxBundle;
import com.qihoo.finance.lowcode.common.util.NotifyUtils;
import com.qihoo.finance.lowcode.editor.ChatxService;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class LogoutFromChatxAction extends AnAction implements DumbAware, ChatxAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(ChatxService.getInstance().isSignedIn());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ChatxService.getInstance().logout(Objects.requireNonNull(e.getProject()));
        NotifyUtils.notify(ChatxBundle.get("chatx.logout.success.message"), NotificationType.INFORMATION);
    }
}
