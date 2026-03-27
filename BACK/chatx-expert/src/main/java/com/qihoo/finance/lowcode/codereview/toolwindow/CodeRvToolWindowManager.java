package com.qihoo.finance.lowcode.codereview.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.qihoo.finance.lowcode.codereview.ui.CodeRvTaskPanel;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;

/**
 * CodeRvToolWindowFactory
 *
 * @author fengjinfu-jk
 * date 2023/11/2
 * @version 1.0.0
 * @apiNote CodeRvToolWindowFactory
 */
public class CodeRvToolWindowManager {
    public static final String TOOL_WINDOW_ID = "Code Review";
    private static final CodeRvToolWindowManager manager = new CodeRvToolWindowManager();

    public static CodeRvToolWindowManager getInstance() {
        return manager;
    }

    public void showCodeRvToolWindow() {
        ToolWindow toolWindow = initCodeRvTaskPanel();
        toolWindow.show(null);
    }

    private ToolWindow initCodeRvTaskPanel() {
        ToolWindow toolWindow = getCodeRvTaskPanelWindow();
        ContentManager contentManager = toolWindow.getContentManager();

        ContentFactory contentFactory = contentManager.getFactory();
        CodeRvTaskPanel taskPanel = ProjectUtils.getCurrProject().getService(CodeRvTaskPanel.class);
        Content content = contentFactory.createContent(taskPanel.createPanel(), null, true);
        contentManager.removeAllContents(true);
        contentManager.addContent(content);

        toolWindow.setAvailable(true, null);
        return toolWindow;
    }

    private ToolWindow getCodeRvTaskPanelWindow() {
        Project project = ProjectUtils.getCurrProject();
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        return toolWindowManager.getToolWindow(TOOL_WINDOW_ID);
    }
}
