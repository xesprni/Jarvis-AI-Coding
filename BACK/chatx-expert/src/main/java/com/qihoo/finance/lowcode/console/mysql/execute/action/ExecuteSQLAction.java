package com.qihoo.finance.lowcode.console.mysql.execute.action;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.entity.dto.SQLBatchExecuteResult;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.console.mysql.SQLEditorManager;
import com.qihoo.finance.lowcode.console.mysql.editor.SQLDataEditor;
import com.qihoo.finance.lowcode.console.mysql.result.ResultManager;
import com.qihoo.finance.lowcode.console.mysql.result.ui.ResultView;
import com.qihoo.finance.lowcode.design.entity.DatabaseNode;
import com.qihoo.finance.lowcode.design.util.DatabaseDesignUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


@Slf4j
public class ExecuteSQLAction extends AnAction implements Disposable {
    private final ResultManager resultManager = ResultManager.getInstance();

    public ExecuteSQLAction() {
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        log.info(Constants.Log.USER_ACTION, "用户提交SQL执行");
        Project project = e.getProject();
        if (Objects.isNull(project)) return;

        VirtualFile file = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        if (Objects.isNull(editor)) {
            FileEditor selectedEditor = FileEditorManager.getInstance(project).getSelectedEditor();
            if (Objects.nonNull(selectedEditor) && selectedEditor instanceof SQLDataEditor) {
                editor = ((SQLDataEditor) selectedEditor).getEditor();
            }
        }

        if (Objects.isNull(file)) return;
        if (Objects.isNull(editor)) return;

        execute(editor, file, project);
    }

    private void execute(Editor editor, VirtualFile file, Project project) {
        if (Objects.nonNull(editor) && Objects.nonNull(file)) {
            if (!SQLEditorManager.isSQLConsole(file)) return;

            // queryDatabase
            DatabaseNode queryDatabase = DataContext.getInstance(project).getConsoleDatabase(file.getName());
            if (Objects.isNull(queryDatabase)) {
                Messages.showMessageDialog("请选择目标数据库", "未指定数据库", Icons.scaleToWidth(Icons.DB, 60));
                return;
            }

            // 优先支持选中执行
            String selectedText = StringUtils.defaultString(editor.getSelectionModel().getSelectedText(), editor.getDocument().getText());
            execute(project, selectedText, queryDatabase, file);
        }
    }

    public void execute(Project project, String selectedText, DatabaseNode queryDatabase, VirtualFile file) {
        // loading
        if (StringUtils.isNotEmpty(clearComment(selectedText))) {
            showLoadingExecutionConsole(file);
        }

        // execute in background
        new SwingWorker<>() {
            private Result<SQLBatchExecuteResult> executeResult;

            @Override
            protected Object doInBackground() {
                // 清除注释及空行
                String sqlContent = clearComment(selectedText);
                if (StringUtils.isNotEmpty(sqlContent)) {
                    // verify datasourceType
                    String datasourceType = file.getUserData(SQLEditorManager.SQL_CONSOLE_DATASOURCE);
                    if (StringUtils.isNotEmpty(datasourceType)) queryDatabase.setDataSourceType(datasourceType);
                    // batch execute SQL
                    executeResult = DatabaseDesignUtils.batchExecuteConsoleSQL(queryDatabase, sqlContent, 1, ResultView.PAGE_SIZE);
                    // show result
                    if (executeResult.isSuccess() && Objects.nonNull(executeResult.getData())) {
                        // 结果集追加字段提示补全
                        SQLBatchExecuteResult data = executeResult.getData();
                        List<String> columns = data.getSqlExecuteResults().stream().flatMap(r -> r.getHeaders().stream()).collect(Collectors.toList());
                        Map<String, List<String>> consoleTableColumns = DataContext.getInstance(project).getConsoleTableColumns();
                        consoleTableColumns.put(file.getName(), columns);
                    }
                }

                return null;
            }

            @Override
            protected void done() {
                showExecutionConsole(file, executeResult);
                super.done();
            }
        }.execute();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        VirtualFile file = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        if (Objects.nonNull(file)) {
            Presentation presentation = e.getPresentation();
            presentation.setVisible(SQLEditorManager.isSQLConsole(file));
        }

        super.update(e);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    private void showExecutionConsole(VirtualFile file, Result<SQLBatchExecuteResult> batchResult) {
        if (Objects.nonNull(file) && Objects.nonNull(batchResult)) {
            resultManager.showExecutionConsole(file.getName(), batchResult);
        }
    }

    private void showLoadingExecutionConsole(VirtualFile file) {
        resultManager.showLoadingExecutionConsole(file.getName());
    }

    @Override
    public void dispose() {

    }

    public static String clearComment(String sqlContent) {
        String[] sqlLines = sqlContent.split("\n");
        return Arrays.stream(sqlLines).filter(sqlLine -> {
            // -- 和 # 开头的为注释行
            sqlLine = sqlLine.trim();
            if (sqlLine.startsWith("--") || sqlLine.startsWith("#")) return false;
            // 空行
            String trim = sqlLine.replaceAll("\n", "").trim();
            return !StringUtils.isEmpty(trim);
        }).collect(Collectors.joining(" "));
    }
}
