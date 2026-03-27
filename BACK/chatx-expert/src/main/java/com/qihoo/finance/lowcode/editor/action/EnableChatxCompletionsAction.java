package com.qihoo.finance.lowcode.editor.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiFile;
import com.qihoo.finance.lowcode.common.util.ChatxBundle;
import com.qihoo.finance.lowcode.editor.ChatxService;
import com.qihoo.finance.lowcode.editor.statusBar.EditorStatusBarWidget;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;
import com.qihoo.finance.lowcode.settings.ChatxApplicationState;
import org.jetbrains.annotations.NotNull;

public class EnableChatxCompletionsAction extends AnAction implements ChatxAction, DumbAware {

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        // 未登录不启用代码补全
        if (!ChatxService.getInstance().isSignedIn()) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
        boolean currentlyDisabled = !ChatxApplicationSettings.settings().enableCompletions
                || (file != null && !ChatxApplicationSettings.isChatxEnabled(file));
        e.getPresentation().setEnabledAndVisible(currentlyDisabled);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || project.isDisposed()) {
            return;
        }
        ChatxApplicationState settings = ChatxApplicationSettings.settings();
        boolean global = !settings.enableCompletions;
        settings.enableCompletions = true;
        PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
        if (file != null)
            settings.enableLanguage(file.getLanguage());
        StatusBar bar = WindowManager.getInstance().getStatusBar(project);
        if (bar != null)
            bar.setInfo(ChatxBundle.get("action.Chatx.enableChatx.statusEnabled"));
        EditorStatusBarWidget.update(project, " " + ChatxBundle.get("chatx.completion.statusBar.enabled.text"));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
