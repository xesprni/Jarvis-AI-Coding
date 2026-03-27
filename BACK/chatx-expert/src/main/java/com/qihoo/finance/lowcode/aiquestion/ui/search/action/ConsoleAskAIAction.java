package com.qihoo.finance.lowcode.aiquestion.ui.search.action;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.content.Content;
import com.qihoo.finance.lowcode.common.factory.ChatXToolWindowFactory;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.NotifyUtils;
import com.qihoo.finance.lowcode.smartconversation.panels.SmartToolWindowPanel;
import com.qihoo.finance.lowcode.smartconversation.panels.SmartToolWindowTabPanel;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * ConsoleAskAIAction
 *
 * @author fengjinfu-jk
 * date 2024/8/7
 * @version 1.0.0
 * @apiNote ConsoleAskAIAction
 */
public class ConsoleAskAIAction extends DumbAwareAction {

    public ConsoleAskAIAction() {
        super("Ask Jarvis", "选择日志内容片段后，Ask Jarvis", Icons.LOGO_ROUND13);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
//        ConsoleView consoleView = e.getData(LangDataKeys.CONSOLE_VIEW);
//        if (Objects.nonNull(consoleView)) {
//            String consoleSelectText = ((ConsoleViewImpl) consoleView).getEditor().getSelectionModel().getSelectedText();
//            e.getPresentation().setEnabled(StringUtils.isNotEmpty(consoleSelectText));
//            return;
//        }

        e.getPresentation().setEnabled(true);
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        final ConsoleView consoleView = e.getData(LangDataKeys.CONSOLE_VIEW);
        Project project = e.getProject();
        if (Objects.isNull(project) || Objects.isNull(consoleView)) return;

        String consoleSelectText = ((ConsoleViewImpl) consoleView).getEditor().getSelectionModel().getSelectedText();
        if (StringUtils.isBlank(consoleSelectText)) {
            NotifyUtils.notify("Jarvis> 请先框选日志片段", NotificationType.WARNING);
            return;
        }
        ChatXToolWindowFactory.showFirstTab();
        Content content = ChatXToolWindowFactory.getToolWindow().getContentManager().getSelectedContent();
        if (content == null) {
            NotifyUtils.notify("快捷指令前置验证失败", NotificationType.WARNING);
            return;
        }
        if (content.getComponent() instanceof SmartToolWindowPanel smartToolWindowPanel) {
            String prompt = String.format("针对以下终端信息，请解释原因并提供修复意见\n```code\n%s\n```", consoleSelectText);
            String taskId = UUID.randomUUID().toString().replace("-", "");
            SmartToolWindowTabPanel smartToolWindowTabPanel = new SmartToolWindowTabPanel(project, taskId);
            smartToolWindowPanel.getChatTabbedPane().addNewTab(smartToolWindowTabPanel, taskId, "");
            smartToolWindowPanel.getChatTabbedPane().trySwitchTab(taskId);
            smartToolWindowTabPanel.handleSubmit(prompt);
        }
    }
}
