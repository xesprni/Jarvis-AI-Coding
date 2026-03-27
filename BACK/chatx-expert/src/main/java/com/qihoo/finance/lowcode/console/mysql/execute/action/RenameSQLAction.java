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
import com.qihoo.finance.lowcode.common.entity.dto.SQLExecuteHistory;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.console.mysql.SQLEditorManager;
import com.qihoo.finance.lowcode.console.mysql.editor.SQLDataEditor;
import com.qihoo.finance.lowcode.console.mysql.execute.ui.dialog.SaveSQLDialog;
import com.qihoo.finance.lowcode.design.entity.DatabaseNode;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;


@Slf4j
public class RenameSQLAction extends AnAction implements Disposable {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        log.info(Constants.Log.USER_ACTION, "用户重命名SQL执行");
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

        save(editor, file, project);
    }

    private void save(Editor editor, VirtualFile file, Project project) {
        if (Objects.nonNull(editor) && Objects.nonNull(file)) {
            if (!SQLEditorManager.isSQLConsole(file)) return;

            // queryDatabase
            DatabaseNode queryDatabase = DataContext.getInstance(project).getConsoleDatabase(file.getName());
            if (Objects.isNull(queryDatabase)) {
                Messages.showMessageDialog("请选择目标数据库", "未指定数据库", Icons.scaleToWidth(Icons.DB, 60));
                return;
            }

            // show sql file name dialog
            SQLExecuteHistory history = file.getUserData(SQLEditorManager.SQL_HISTORY);
            String sql = editor.getDocument().getText().trim();
            new SaveSQLDialog(project, file, queryDatabase, sql, history).show();
        }
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

    @Override
    public void dispose() {

    }
}
