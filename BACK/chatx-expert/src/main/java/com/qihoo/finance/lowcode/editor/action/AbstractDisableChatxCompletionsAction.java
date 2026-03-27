package com.qihoo.finance.lowcode.editor.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.qihoo.finance.lowcode.editor.ChatxEditorManager;
import com.qihoo.finance.lowcode.editor.InlayDisposeContext;
import com.qihoo.finance.lowcode.editor.statusBar.EditorStatusBarWidget;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class AbstractDisableChatxCompletionsAction extends AnAction implements ChatxAction, DumbAware {

    private final boolean forCurrentFile;

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || (!ChatxApplicationSettings.settings().enabledCompletionDisableButton && !forCurrentFile)) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
        boolean enabledGlobally = (ChatxApplicationSettings.settings()).enableCompletions;
        boolean enabledForFile = (file != null && ChatxApplicationSettings.settings().isEnabled(file.getLanguage()));
        e.getPresentation().setEnabledAndVisible((enabledGlobally && enabledForFile));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || project.isDisposed())
            return;
        PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
        boolean global = (!this.forCurrentFile || file == null);
        if (global) {
            (ChatxApplicationSettings.settings()).enableCompletions = false;
        } else {
            ChatxApplicationSettings.settings().disableLanguage(file.getLanguage());
        }
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor != null) {
            ChatxEditorManager.getInstance().disposeInlays(editor, InlayDisposeContext.SettingsChange);
        }
//        StatusBar bar = WindowManager.getInstance().getStatusBar(project);
//        if (bar != null) {
//            bar.setInfo(ChatxBundle.get("action.Chatx.disableChatx.statusEnabled"));
//        }
        EditorStatusBarWidget.update(project);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
