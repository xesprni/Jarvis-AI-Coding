package com.qihoo.finance.lowcode.common.action;


import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.ui.content.Content;
import com.qihoo.finance.lowcode.common.factory.ChatXToolWindowFactory;
import com.qihoo.finance.lowcode.common.util.NotifyUtils;
import com.qihoo.finance.lowcode.smartconversation.panels.SmartToolWindowPanel;
import com.qihoo.finance.lowcode.smartconversation.panels.SmartToolWindowTabPanel;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * @author weiyichao
 * @date 2025-10-22
 **/
public abstract class BaseQuickAskAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // 打开工具窗口
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        ChatXToolWindowFactory.showFirstTab();
        Content content = ChatXToolWindowFactory.getToolWindow().getContentManager().getSelectedContent();
        if (content == null) {
            NotifyUtils.notify("快捷指令前置验证失败", NotificationType.WARNING);
            return;
        }
        if (content.getComponent() instanceof SmartToolWindowPanel smartToolWindowPanel) {
            // 获取选中的代码
            Editor editor = e.getData(CommonDataKeys.EDITOR);
            if (editor == null) {
                return;
            }
            SelectionModel selectionModel = editor.getSelectionModel();
            String selectedText = selectionModel.getSelectedText();
            if (selectedText == null || selectedText.isEmpty()) {
                NotifyUtils.notify(getSelectedTextEmptyTips(), NotificationType.WARNING);
                return;
            }
            String prompt = getPrompt();

            String taskId = UUID.randomUUID().toString().replace("-", "");
            SmartToolWindowTabPanel smartToolWindowTabPanel = new SmartToolWindowTabPanel(project, taskId);
            smartToolWindowPanel.getChatTabbedPane().addNewTab(smartToolWindowTabPanel, taskId, "");
            smartToolWindowPanel.getChatTabbedPane().trySwitchTab(taskId);
            smartToolWindowTabPanel.handleSubmit(prompt);
        }
    }

    abstract String getPrompt();

    abstract String getSelectedTextEmptyTips();


}
