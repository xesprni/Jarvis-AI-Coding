package com.qihoo.finance.lowcode.console.mysql.result.factory;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.console.mysql.result.ResultManager;
import org.jetbrains.annotations.NotNull;

public class ResultToolWindowFactory implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        toolWindow.setTitle(ResultManager.TOOL_WINDOW_ID);
        toolWindow.setStripeTitle(ResultManager.TOOL_WINDOW_ID);
        toolWindow.setIcon(Icons.scaleToWidth(Icons.DB_GEN, 13));
        toolWindow.setToHideOnEmptyContent(true);
        toolWindow.setAutoHide(false);
        toolWindow.setAvailable(false, null);
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return false;
    }
}
