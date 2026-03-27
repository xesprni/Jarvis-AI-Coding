package com.qihoo.finance.lowcode.editor.lang.agent;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.qihoo.finance.lowcode.common.configuration.UserInfoPersistentState;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.ui.ToolBarPanel;
import com.qihoo.finance.lowcode.editor.ChatxService;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class AgentChatxService implements ChatxService {

    @Override
    public boolean isSignedIn() {
        UserInfoPersistentState.UserInfo userInfo = ApplicationManager.getApplication()
                .getService(UserInfoPersistentState.class).getState();
        return userInfo != null && StringUtils.isNotEmpty(userInfo.email) && StringUtils.isNotEmpty(userInfo.token);
    }

    @Override
    public void loginInteractive(@NotNull Project project) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow(Constants.PLUGIN_TOOL_WINDOW_ID);
        if (toolWindow != null) {
            toolWindow.show(null);
        }
    }

    @Override
    public void logout(@NotNull Project project) {
        project.getService(ToolBarPanel.class).logout();
    }
}
