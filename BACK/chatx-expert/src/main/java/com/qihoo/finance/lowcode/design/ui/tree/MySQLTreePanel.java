package com.qihoo.finance.lowcode.design.ui.tree;

import com.google.common.collect.Lists;
import com.intellij.icons.AllIcons;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBMenu;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.qihoo.finance.lowcode.common.configuration.UserInfoPersistentState;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.entity.PlaceholderNode;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.entity.dto.PermissionTreeDTO;
import com.qihoo.finance.lowcode.common.entity.dto.SQLBatchExecuteResult;
import com.qihoo.finance.lowcode.common.entity.dto.SQLExecuteHistory;
import com.qihoo.finance.lowcode.common.entity.dto.SQLExecuteHistoryType;
import com.qihoo.finance.lowcode.common.ui.base.SearchTreeCellRenderer;
import com.qihoo.finance.lowcode.common.util.*;
import com.qihoo.finance.lowcode.console.mysql.SQLEditorManager;
import com.qihoo.finance.lowcode.console.mysql.editor.SQLDataEditor;
import com.qihoo.finance.lowcode.console.mysql.editor.SQLFileSystem;
import com.qihoo.finance.lowcode.console.mysql.editor.SQLVirtualFile;
import com.qihoo.finance.lowcode.console.mysql.execute.action.ExecuteSQLAction;
import com.qihoo.finance.lowcode.console.mysql.execute.ui.dialog.SQLHistoryDialog;
import com.qihoo.finance.lowcode.design.action.ExportDDLAction;
import com.qihoo.finance.lowcode.design.constant.DatabaseOperateType;
import com.qihoo.finance.lowcode.design.dialog.DatabaseSQLRecordDialog;
import com.qihoo.finance.lowcode.design.dialog.TableDesignerDialog;
import com.qihoo.finance.lowcode.design.entity.*;
import com.qihoo.finance.lowcode.design.listener.MySQLTreeListener;
import com.qihoo.finance.lowcode.design.util.DatabaseDesignUtils;
import com.qihoo.finance.lowcode.gentracker.actions.GenerateAction;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author weiyichao
 * date 2023-07-27
 **/
@Getter
@Slf4j
public class MySQLTreePanel extends DatabaseTreePanel {
    private final List<String> dateType = Lists.newArrayList("date", "time");
    @Getter
    @Setter
    private Pair<List<JBMenuItem>, List<JBMenuItem>> sqlMenuItems = Pair.of(new ArrayList<>(), new ArrayList<>());

    public MySQLTreePanel(@NotNull Project project) {
        super(project);
    }

    @Override
    public List<TreeSelectionListener> treeSelectionListener() {
        return Lists.newArrayList(new MySQLTreeListener(project, tree));
    }

    @Override
    public void nodeRenderer(SearchTreeCellRenderer cellRenderer, DefaultMutableTreeNode node) {
        if (node instanceof DatabaseDepartmentNode) {
            cellRenderer.setIcon(Icons.scaleToWidth(Icons.ORG, 18));
        }
        if (node instanceof DatabaseNode) {
//            cellRenderer.setIcon(Icons.scaleToWidth(Icons.DB_BLOCK, 18));
            cellRenderer.setIcon(Icons.scaleToWidth(AllIcons.Providers.Mysql, 18));
        }
        if (node instanceof MySQLTableNode) {
            cellRenderer.setIcon(Icons.scaleToWidth(Icons.TABLE2, 16));
        }
        if (node instanceof DatabaseFolderNode) {
            cellRenderer.setIcon(Icons.scaleToWidth(Icons.FOLDER, 15));
        }
        if (node instanceof DatabaseColumnNode) {
            DatabaseColumnNode columnNode = (DatabaseColumnNode) node;
            if (columnNode.isPK()) {
                cellRenderer.setIcon(Icons.scaleToWidth(Icons.PRIMARY_KEY, 15));
            } else if (isDate(columnNode.getFieldType())) {
                cellRenderer.setIcon(Icons.scaleToWidth(Icons.TIMESTAMP, 15));
            } else {
                cellRenderer.setIcon(Icons.scaleToWidth(Icons.COLUMN, 15));
            }
        }
        if (node instanceof PlaceholderNode) {
            cellRenderer.setIcon(Icons.scaleToWidth(Icons.HOLDER, 18));
        }
    }

    private boolean isDate(String filedType) {
        for (String type : dateType) {
            if (filedType.contains(type)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public MouseListener mouseListener(JTree tree) {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = tree.getRowForLocation(e.getX(), e.getY());
                    if (row != -1) {
//                        // fixme: tree.setSelectionRow(row) 会不支持多选
//                        tree.setSelectionRow(row);
                        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                        if (selectedNode != null) {
                            JBPopupMenu popupMenu = createPopupMenu(tree, selectedNode);
                            if (popupMenu.getComponentCount() > 0) {
                                popupMenu.show(tree, e.getX(), e.getY());
                            }
                        }
                    }
                }

                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                    if (Objects.isNull(path)) return;

                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    // 在这里处理双击事件
                    if (node instanceof MySQLTableNode) {
                        // 打开SQL编辑器
                        openSQLConsole(node);
                    }
                }
            }
        };
    }

    private void openSQLConsole(DefaultMutableTreeNode node) {
        SQLEditorManager.openTempSQLConsole(project, (DatabaseNode) node.getParent());
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        FileEditor fileEditor = editorManager.getSelectedEditor();
        if (Objects.isNull(fileEditor) || !(fileEditor instanceof SQLDataEditor)) return;

        SQLDataEditor selectedEditor = (SQLDataEditor) editorManager.getSelectedEditor();
        Editor editor = selectedEditor.getEditor();
        if (Objects.nonNull(editor)) {
            String sql = String.format("select * from %s;", ((MySQLTableNode) node).getTableName());
            ApplicationManager.getApplication().runWriteAction(() -> {
                editor.getDocument().setText(sql);
            });

            ActionManager actionManager = ActionManager.getInstance();
            ExecuteSQLAction action = (ExecuteSQLAction) actionManager.getAction("ChatX.Actions.SQLExecute");
            action.execute(project, sql, dataContext.getSelectDatabase(), fileEditor.getFile());
        }
    }

    @Override
    public void loadDepAndDbTree(JTree tree, boolean async) {
        log.info("MySQLTreePanel loadDepAndDbTree : {}", System.identityHashCode(tree));
        UIUtil.invokeLaterIfNeeded(() -> this.loading.startLoading(false));

        // root
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        JTreeLoadingUtils.loading(async, tree, root, () -> {
            // department
            List<PermissionTreeDTO> permissions = DatabaseDesignUtils.queryUserPermissionList(UserInfoPersistentState.getUserInfo().getUserNo(), Constants.DataSource.MySQL);
            return extractDatabaseNode(buildNodeTree(permissions), Constants.DataSource.MySQL);
        }, nodes -> {
            UIUtil.invokeLaterIfNeeded(this.loading::stopLoading);
            // 加载表并写入上下文
            List<DatabaseNode> databaseNodes = new ArrayList<>();
            for (DefaultMutableTreeNode node : nodes) {
                if (node instanceof DatabaseNode dbNode) {
                    databaseNodes.add(dbNode);
                    // 初始化不再加载表级别
                    if (dbNode.isCollect()) {
                        JTreeLoadingUtils.loading(async, tree, dbNode, () -> {
                            // load database
                            return DatabaseDesignUtils.queryMySQLTableNodes(dbNode);
                        });
                    }
                }
            }

            DataContext.getInstance(project).setAllMySQLDatabaseList(databaseNodes);
            // load history cache
            openSqlExecutes();
        });
    }

    @Override
    public void updateDatabaseContext() {
        DataContext.getInstance(project).setDbTree(tree);
    }

    /**
     * 创建自定义的右键菜单
     */
    @Override
    public JBPopupMenu createPopupMenu(JTree tree, DefaultMutableTreeNode node) {
        JBPopupMenu popupMenu = new JBPopupMenu();
        popupMenu.setBorder(BorderFactory.createLineBorder(JBColor.GRAY));
        popupMenu.getInsets().set(50, 50, 50, 50);

        TreePath[] selectionPaths = tree.getSelectionPaths();
        if (Objects.isNull(selectionPaths)) return popupMenu;

        boolean sameParent = Arrays.stream(selectionPaths).map(TreePath::getParentPath).distinct().count() <= 1;
        boolean isMultiSelect = selectionPaths.length > 1;

        if (node instanceof DatabaseNode) {
            DatabaseNode dbNode = (DatabaseNode) node;

            popupMenu.add(Box.createVerticalStrut(2));
            JBMenu sqlExecute = sqlExecuteMenu(dbNode);
            sqlExecute.setIcon(AllIcons.Providers.Mysql);
            popupMenu.add(sqlExecute);
            popupMenu.add(Box.createVerticalStrut(2));

            // DDL导出
            if (!isMultiSelect) {
                popupMenu.addSeparator();
                popupMenu.add(Box.createVerticalStrut(2));

                JBMenuItem collect = createCollectMenuItem(Constants.DataSource.MySQL, node);
                collect.setIcon(Icons.scaleToWidth(Icons.COLLECT, 14));
                popupMenu.add(collect);

                popupMenu.addSeparator();
                popupMenu.add(Box.createVerticalStrut(2));

                JBMenuItem exportDDL = createMenuItem(new ExportDDLAction("导出DDL"));
                exportDDL.setIcon(Icons.scaleToWidth(Icons.EXPORT_DDL, 16));
                popupMenu.add(exportDDL);
            }

            popupMenu.addSeparator();
            popupMenu.add(Box.createVerticalStrut(2));

            JBMenuItem addTable = createMenuItem("新增表", DatabaseOperateType.CREATE);
            addTable.setIcon(Icons.scaleToWidth(Icons.TABLE_ADD, 16));
            popupMenu.add(addTable);

            popupMenu.add(Box.createVerticalStrut(2));
            popupMenu.addSeparator();
        }
        if (node instanceof MySQLTableNode) {
            MySQLTableNode tableNode = (MySQLTableNode) node;
            TreeNode parent = tableNode.getParent();
            if (Objects.nonNull(parent) && parent instanceof DatabaseNode) {
                popupMenu.add(Box.createVerticalStrut(2));
                JBMenu sqlExecute = sqlExecuteMenu((DatabaseNode) parent);
                sqlExecute.setIcon(AllIcons.Providers.Mysql);
                popupMenu.add(sqlExecute);
            }

            // DDL导出
            if (sameParent) {
                popupMenu.add(Box.createVerticalStrut(6));
                JBMenuItem exportDDL = createMenuItem(new ExportDDLAction("导出DDL"));
                exportDDL.setIcon(Icons.scaleToWidth(Icons.EXPORT_DDL, 16));
                popupMenu.add(exportDDL);
            }

            if (!isMultiSelect) {
                popupMenu.add(Box.createVerticalStrut(6));
                JBMenuItem ddl = openDDL();
                ddl.setIcon(Icons.scaleToWidth(Icons.DDL, 16));
                popupMenu.add(ddl);

                popupMenu.add(Box.createVerticalStrut(6));
                JBMenuItem ddlRecord = openDDLRecord();
                ddlRecord.setIcon(Icons.scaleToWidth(Icons.WATCH, 16));
                popupMenu.add(ddlRecord);
            }

            popupMenu.add(Box.createVerticalStrut(2));
            popupMenu.addSeparator();
            popupMenu.add(Box.createVerticalStrut(2));

            JBMenuItem addTable = createMenuItem("新增表", DatabaseOperateType.CREATE);
            addTable.setIcon(Icons.scaleToWidth(Icons.TABLE_ADD, 16));
            popupMenu.add(addTable);

            if (!isMultiSelect) {
                popupMenu.add(Box.createVerticalStrut(6));
                JBMenuItem copyTable = createMenuItem("复制表", DatabaseOperateType.COPY);
                copyTable.setIcon(Icons.scaleToWidth(Icons.TABLE_ADD, 16));
                popupMenu.add(copyTable);

                popupMenu.add(Box.createVerticalStrut(6));
                JBMenuItem editTable = createMenuItem("编辑表", DatabaseOperateType.EDIT);
                editTable.setIcon(Icons.scaleToWidth(Icons.TABLE_EDIT, 16));
                popupMenu.add(editTable);

                popupMenu.add(Box.createVerticalStrut(6));
                JBMenuItem deleteTable = deleteTableMenu(tableNode);
                deleteTable.setIcon(Icons.scaleToWidth(Icons.TABLE_REMOVE, 16));
                popupMenu.add(deleteTable);

                popupMenu.add(Box.createVerticalStrut(2));
                popupMenu.addSeparator();
                popupMenu.add(Box.createVerticalStrut(2));

                JBMenuItem genCode = createMenuItem(new GenerateAction("生成代码", project));
                genCode.setIcon(Icons.scaleToWidth(Icons.TABLE_GEN_CODE, 15));
                popupMenu.add(genCode);

                popupMenu.add(Box.createVerticalStrut(2));
                popupMenu.addSeparator();
            } else {
                popupMenu.add(Box.createVerticalStrut(2));
                popupMenu.addSeparator();
                popupMenu.add(Box.createVerticalStrut(2));

                JBMenuItem genCode = createMenuItem(new GenerateAction("生成代码 (批量)", project));
                genCode.setIcon(Icons.scaleToWidth(Icons.TABLE_GEN_CODE, 15));
                popupMenu.add(genCode);

                popupMenu.add(Box.createVerticalStrut(2));
                popupMenu.addSeparator();
            }
        }

        if (popupMenu.getComponentCount() > 0) {
            popupMenu.insert(Box.createVerticalStrut(8), 0);
        }

        return popupMenu;
    }

    private JBMenuItem openDDL() {
        JBMenuItem menuItem = new JBMenuItem(new AbstractAction("查看DDL") {
            @Override
            public void actionPerformed(ActionEvent e) {
                MySQLTableNode selectDbTable = DataContext.getInstance(project).getSelectDbTable();
                if (Objects.nonNull(selectDbTable)) {
                    new TableDesignerDialog(project, DatabaseOperateType.EDIT).show(2);
                } else {
                    NotifyUtils.notify("请先选中数据库表, 再点击查看表结构DDL", NotificationType.WARNING);
                }
            }
        });

        // 设置自定义边距
        menuItem.setMargin(JBUI.insets(0, 10));
        return menuItem;
    }

    private JBMenuItem openDDLRecord() {
        JBMenuItem menuItem = new JBMenuItem(new AbstractAction("表变更记录") {
            @Override
            public void actionPerformed(ActionEvent e) {
                new DatabaseSQLRecordDialog(project).show();
            }
        });

        // 设置自定义边距
        menuItem.setMargin(JBUI.insets(0, 10));
        return menuItem;
    }

    private JBMenu sqlExecuteMenu(DatabaseNode dbNode) {
        JBMenu menu = new JBMenu();
        menu.setText("SQL执行");
        menu.add(Box.createVerticalStrut(6));
        // 新建查询
        menu.add(newSqlExecute(dbNode, Icons.scaleToWidth(Icons.ADD_ICON, 16)));
        menu.addSeparator();
        // 历史查询
        Pair<List<JBMenuItem>, List<JBMenuItem>> sqlExecutes = openSqlExecutes();
        for (JBMenuItem sqlExecute : sqlExecutes.getLeft()) {
            menu.add(sqlExecute);
        }
        if (CollectionUtils.isNotEmpty(sqlExecutes.getLeft()) && CollectionUtils.isNotEmpty(sqlExecutes.getRight())) {
            menu.addSeparator();
        }
        for (JBMenuItem sqlExecute : sqlExecutes.getRight()) {
            menu.add(sqlExecute);
        }

        menu.add(Box.createVerticalStrut(2));
        menu.addSeparator();
        menu.add(allSqlExecute());
        menu.add(Box.createVerticalStrut(6));

        return menu;
    }

    private JBMenuItem allSqlExecute() {
        JBMenuItem menuItem = new JBMenuItem(new AbstractAction("SQL执行记录") {
            @Override
            public void actionPerformed(ActionEvent e) {
                // open sql history dialog
                SQLHistoryDialog.showDialog();
            }
        });

        // 设置自定义边距
        menuItem.setMargin(JBUI.insets(0, 10));
        menuItem.setIcon(Icons.scaleToWidth(Icons.EXECUTE_HISTORY, 15));
        return menuItem;
    }

    private JBMenuItem newSqlExecute(DatabaseNode dbNode, Icon icon) {
        JBMenuItem menuItem = new JBMenuItem(new AbstractAction("新建SQL执行") {
            @Override
            public void actionPerformed(ActionEvent e) {
                SQLEditorManager.openTempSQLConsole(project, dbNode);
            }
        });

        // 设置自定义边距
        menuItem.setMargin(JBUI.insets(0, 10));
        menuItem.setIcon(icon);
        return menuItem;
    }

    private Pair<List<JBMenuItem>, List<JBMenuItem>> openSqlExecutes() {
        new SwingWorker<List<SQLExecuteHistory>, List<SQLExecuteHistory>>() {
            @Override
            protected List<SQLExecuteHistory> doInBackground() {
                return DatabaseDesignUtils.getSQLExecuteHistory10();
            }

            @SneakyThrows
            @Override
            protected void done() {
                List<SQLExecuteHistory> histories = get();
                Pair<List<JBMenuItem>, List<JBMenuItem>> items = buildSQLHistoryPopupMenu(histories);
                sqlMenuItems = items;
                super.done();
            }
        }.execute();

        return sqlMenuItems;
    }

    @NotNull
    public Pair<List<JBMenuItem>, List<JBMenuItem>> buildSQLHistoryPopupMenu(List<SQLExecuteHistory> histories) {
        List<JBMenuItem> saveItems = new ArrayList<>();
        List<JBMenuItem> otherItems = new ArrayList<>();
        List<DatabaseNode> databaseNodes = ListUtils.defaultIfNull(
                DataContext.getInstance(project).getAllMySQLDatabaseList(),
                new ArrayList<>()
        );

        // build sql history menu item
        for (SQLExecuteHistory sqlHistory : histories) {
            DatabaseNode databaseNode = databaseNodes.stream().filter(v -> sqlHistory.getInstanceName().equals(v.getInstanceName())
                    && sqlHistory.getDatabaseName().equals(v.getName())).findFirst().orElse(null);
            if (Objects.isNull(databaseNode)) continue;

            String itemName = sqlHistory.getShowSQLConsoleName();
            JBMenuItem menuItem = new JBMenuItem(new AbstractAction(itemName) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    openHistorySQLConsole(databaseNode, sqlHistory);
                }
            });
            menuItem.setMargin(JBUI.insets(0, 10));
            menuItem.setIcon(AllIcons.Providers.Mysql);

            if (SQLExecuteHistoryType.USER_SAVE.getCode().equals(sqlHistory.getHistoryType())) {
                saveItems.add(menuItem);
            } else {
                otherItems.add(menuItem);
            }
        }
        return Pair.of(saveItems, otherItems);
    }

    public void openHistorySQLConsole(DatabaseNode databaseNode, SQLExecuteHistory sqlHistory) {
        if (DataContext.getInstance(project).getConsoleDatabase().containsKey(sqlHistory.getShowSQLConsoleName())) {
            // 尝试定位
            VirtualFile file = Arrays.stream(FileEditorManager.getInstance(project).getOpenFiles())
                    .filter(f -> sqlHistory.getShowSQLConsoleName().equals(f.getName()))
                    .findFirst().orElse(null);
            if (Objects.nonNull(file) && file instanceof SQLVirtualFile virtualFile) {
                SQLFileSystem.getInstance().openEditor(virtualFile);
                return;
            }
        }

        SQLEditorManager.openTempSQLConsole(project, databaseNode, sqlHistory);
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        FileEditor fileEditor = editorManager.getSelectedEditor();
        if (Objects.isNull(fileEditor)) return;

        SQLDataEditor selectedEditor = (SQLDataEditor) editorManager.getSelectedEditor();
        Editor editor = selectedEditor.getEditor();
        if (Objects.nonNull(editor)) {
            String sql = sqlHistory.getSqlContent();
            WriteCommandAction.runWriteCommandAction(project, () -> {
                editor.getDocument().setText(sql);
            });
        }
    }

    private JBMenuItem cleanTable(MySQLTableNode tableNode) {
        JBMenuItem menuItem = new JBMenuItem(new AbstractAction("清除配置") {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = Messages.showIdeaMessageDialog(project, "是否确认清除表配置?\n清除后需重新对没有映射关系的字段进行配置\n", "清除配置", new String[]{"确认", "取消"}, 1, Icons.scaleToWidth(Icons.WARN, 60), null);
                if (index == 0) {
                    CacheManager.refreshTypeMapper();
                }
            }
        });
        // 设置自定义边距
        menuItem.setMargin(JBUI.insets(0, 10));
        return menuItem;
    }

    /**
     * 创建带有自定义边距的 JBMenuItem
     */
    private JBMenuItem createMenuItem(String text, DatabaseOperateType operateType) {
        JBMenuItem menuItem = new JBMenuItem(new AbstractAction(text) {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 自定义菜单项的操作
                TableDesignerDialog dialog = new TableDesignerDialog(project, operateType);
                dialog.show();
            }
        });
        // 设置自定义边距
        menuItem.setMargin(JBUI.insets(0, 10));
        return menuItem;
    }

    /**
     * 创建带有自定义边距的 JBMenuItem
     */
    private JBMenuItem deleteTableMenu(MySQLTableNode node) {
        String text = "删除表";
        JBMenuItem menuItem = new JBMenuItem(new AbstractAction(text) {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 自定义菜单项的操作
                String tips = String.format("您正在删除表 [ %s ] \n\n请输入完整表名确认删除 !", node.getTableName());
                String inputString = Messages.showInputDialog(project, tips, text, Icons.scaleToWidth(Icons.DELETE, 60), null, new InputValidator() {
                    @Override
                    public boolean checkInput(@NlsSafe String inputString) {
                        return node.getTableName().equalsIgnoreCase(inputString);
                    }

                    @Override
                    public boolean canClose(@NlsSafe String inputString) {
                        return true;
                    }
                });

                if (node.getTableName().equalsIgnoreCase(inputString)) {
                    if (node.getParent() instanceof DatabaseNode) {
                        DatabaseNode database = (DatabaseNode) node.getParent();
                        // drop table
//                        String sqlScript = String.format("DROP TABLE %s.%s;", node.getDatabase(), node.getTableName());
                        String sqlScript = String.format("DROP TABLE %s;", node.getTableName());
                        Result<SQLBatchExecuteResult> result = DatabaseDesignUtils.batchExecuteConsoleSQL(database, sqlScript);
                        if (DatabaseDesignUtils.getExecuteResult(result, "保存失败, 脚本执行失败, 请检查脚本:")) {
                            // success
                            DatabaseNode nameSpace = DataContext.getInstance(project).getSelectDatabase();
                            JTree dbTree = DataContext.getInstance(project).getDbTree();
                            CacheManager.refreshTemplate();

                            JTreeLoadingUtils.loading(true, dbTree, nameSpace, () -> {
                                // 重新加载库
                                return DatabaseDesignUtils.queryMySQLTableNodes(nameSpace);
                            });
                        } else {
                            // fail
                            Messages.showMessageDialog("删除失败:\n\n" + result.getErrorMsg(), "删除失败", Icons.scaleToWidth(Icons.FAIL, 60));
                        }
                    }
                }
            }
        });

        // 设置自定义边距
        menuItem.setMargin(JBUI.insets(0, 10));
        return menuItem;
    }
}
