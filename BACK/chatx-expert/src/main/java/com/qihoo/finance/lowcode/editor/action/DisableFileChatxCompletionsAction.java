package com.qihoo.finance.lowcode.editor.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.psi.PsiFile;
import com.qihoo.finance.lowcode.common.util.ChatxBundle;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;
import org.jetbrains.annotations.NotNull;

public class DisableFileChatxCompletionsAction extends AbstractDisableChatxCompletionsAction {

    public DisableFileChatxCompletionsAction() {
        super(true);
    }

    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        if (e.getPresentation().isEnabledAndVisible()) {
            PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
            if (file == null || !ChatxApplicationSettings.settings().disableCompletionAllowLanguageSet.contains(file.getLanguage().getDisplayName())) {
                e.getPresentation().setEnabledAndVisible(false);
                return;
            }
            String language = file.getLanguage().getDisplayName();
            e.getPresentation().setText(ChatxBundle.get("action.Chatx.disableChatxLanguage.text", language));
        }
    }
}
