package com.qihoo.finance.lowcode.codereview.toolwindow.factory;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.qihoo.finance.lowcode.codereview.toolwindow.CodeRvToolWindowManager;
import com.qihoo.finance.lowcode.common.util.Icons;
import org.jetbrains.annotations.NotNull;

/**
 * CodeRvToolWindowFactory
 *
 * @author fengjinfu-jk
 * date 2023/11/2
 * @version 1.0.0
 * @apiNote CodeRvToolWindowFactory
 */
public class CodeRvToolWindowFactory implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        toolWindow.setTitle(CodeRvToolWindowManager.TOOL_WINDOW_ID);
        toolWindow.setStripeTitle(CodeRvToolWindowManager.TOOL_WINDOW_ID);
        toolWindow.setIcon(Icons.scaleToWidth(Icons.GIT_LAB, 16));
        toolWindow.setToHideOnEmptyContent(true);
        toolWindow.setAutoHide(false);
        toolWindow.setAvailable(false, null);
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return false;
    }
}
