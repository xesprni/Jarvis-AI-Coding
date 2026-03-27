package com.qihoo.finance.lowcode.console.mysql.result;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.entity.dto.SQLBatchExecuteResult;
import com.qihoo.finance.lowcode.console.mysql.result.ui.ResultView;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;

/**
 * ExecutionManager
 *
 * @author fengjinfu-jk
 * date 2023/9/12
 * @version 1.0.0
 * @apiNote ExecutionManager
 */
public class ResultManager {
    public static final String TOOL_WINDOW_ID = "SQL Execute Result";
    private static final ResultManager EXECUTE_RESULT_MANAGER = new ResultManager();

    public static ResultManager getInstance() {
        return EXECUTE_RESULT_MANAGER;
    }

    public void showLoadingExecutionConsole(String name) {
        ToolWindow toolWindow = initExecutionConsole();
        loadingResultForm();
        // show
        toolWindow.setAvailable(true, null);
        toolWindow.show(null);
    }

    public void showExecutionConsole(String name, Result<SQLBatchExecuteResult> batchResult) {
        ToolWindow toolWindow = initExecutionConsole();
        refreshResultView(name, batchResult);
        // show
        toolWindow.setAvailable(true, null);
        toolWindow.show(null);
    }

    public void unAvailableExecutionConsole() {
        ToolWindow toolWindow = getExecutionConsoleWindow();
        toolWindow.getContentManager().removeAllContents(false);
        toolWindow.setAvailable(false, null);
    }

    public void hideExecutionConsole() {
        ToolWindow toolWindow = getExecutionConsoleWindow();
        toolWindow.hide();
    }

    public void availableExecutionConsole() {
        ToolWindow toolWindow = getExecutionConsoleWindow();
        toolWindow.setAvailable(true, null);
    }

    private ToolWindow getExecutionConsoleWindow() {
        Project project = ProjectUtils.getCurrProject();
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        return toolWindowManager.getToolWindow(TOOL_WINDOW_ID);
    }

    private ToolWindow initExecutionConsole() {
        ToolWindow toolWindow = getExecutionConsoleWindow();
        ContentManager contentManager = toolWindow.getContentManager();
        if (contentManager.getContents().length == 0) {
            ContentFactory contentFactory = contentManager.getFactory();
            ResultView resultView = ProjectUtils.getCurrProject().getService(ResultView.class);
            Content content = contentFactory.createContent(resultView.getMainComponent(), null, true);
            contentManager.addContent(content);
            toolWindow.setAvailable(true, null);
        }

        return toolWindow;
    }

    private void refreshResultView(String name, Result<SQLBatchExecuteResult> batchResult) {
        ResultView resultView = ProjectUtils.getCurrProject().getService(ResultView.class);
        try {
            resultView.refresh(name, batchResult);
        } finally {
            resultView.closeLoading();
        }
    }

    private void loadingResultForm() {
        ResultView resultView = ProjectUtils.getCurrProject().getService(ResultView.class);
        resultView.loading();
    }
}
