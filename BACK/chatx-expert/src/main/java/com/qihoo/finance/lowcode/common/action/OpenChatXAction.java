package com.qihoo.finance.lowcode.common.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.factory.ChatXToolWindowFactory;

import java.util.Objects;

public class OpenChatXAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project != null) {
            ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
            ToolWindow toolWindow = toolWindowManager.getToolWindow(Constants.PLUGIN_TOOL_WINDOW_ID);
            if (toolWindow != null) {
                if (ChatXToolWindowFactory.isHidden()) {
                    toolWindow.show(null);
                    ChatXToolWindowFactory.getToolWindow().getContentManager().setSelectedContent(Objects.requireNonNull(toolWindow.getContentManager().getContent(0)));

                }else {
                    ChatXToolWindowFactory.getToolWindow().hide();
                }
            }
        }
    }
}
