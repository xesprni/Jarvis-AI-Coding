package com.qihoo.finance.lowcode.console.mysql.execute.action;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.console.mysql.SQLEditorManager;
import com.qihoo.finance.lowcode.design.entity.DatabaseDepartmentNode;
import com.qihoo.finance.lowcode.design.entity.DatabaseNode;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("all")
public class SelectSchemaAction extends ComboBoxAction implements DumbAware {
    private static final String NAME = "Schema";
    private Project project;

    @Override
    @NotNull
    protected DefaultActionGroup createPopupActionGroup(JComponent component) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        if (Objects.isNull(project)) return actionGroup;

        DataContext dataContext = DataContext.getInstance(project);
        VirtualFile virtualFile = FileEditorManager.getInstance(project).getSelectedEditor().getFile();
        List<DatabaseNode> databaseList = new ArrayList<>();

        String datasourceType = virtualFile.getUserData(SQLEditorManager.SQL_CONSOLE_DATASOURCE);
        boolean mySQLConsole = SQLEditorManager.isMySQLConsole(virtualFile);
        if (mySQLConsole) {
            databaseList = dataContext.getAllMySQLDatabaseList();
        }

        // 设置数据源列表
        for (DatabaseNode databaseNode : databaseList) {
            TreeNode parent = databaseNode.getParent();
            String nodeName = databaseNode.getName();

            if (parent instanceof DatabaseDepartmentNode) {
                nodeName = String.format("[%s] %s", ((DatabaseDepartmentNode) parent).getName(), databaseNode.getName());
            }

            actionGroup.add(new AnAction(nodeName, databaseNode.getCode(), SQLEditorManager.datasourceIcon(datasourceType)) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    VirtualFile file = e.getData(PlatformDataKeys.VIRTUAL_FILE);
                    String fileName = file.getName();
                    dataContext.setConsoleDatabase(fileName, databaseNode);
                }
            });
        }

        return actionGroup;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        project = e.getProject();
        VirtualFile file = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        if (Objects.isNull(file)) return;

        String fileName = file.getName();
        String nodeName = NAME;
        DatabaseNode databaseNode = DataContext.getInstance(project).getConsoleDatabase(fileName);
        if (Objects.nonNull(databaseNode)) {
            TreeNode parent = databaseNode.getParent();
            nodeName = databaseNode.getName();
            if (parent instanceof DatabaseDepartmentNode) {
                nodeName = String.format("[%s] %s", ((DatabaseDepartmentNode) parent).getName(), databaseNode.getName());
            }

            Presentation presentation = e.getPresentation();
            presentation.setText(nodeName, false);
            presentation.setDescription("选择目标数据库");
            presentation.setVisible(true);
            presentation.setEnabled(true);

            String datasourceType = file.getUserData(SQLEditorManager.SQL_CONSOLE_DATASOURCE);
            presentation.setIcon(SQLEditorManager.datasourceIcon(datasourceType));
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
