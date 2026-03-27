package com.qihoo.finance.lowcode.console.mysql.result.ui;

import com.google.common.collect.Lists;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.toolbarLayout.ToolbarLayoutStrategy;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LoadingDecorator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.TableSpeedSearch;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.UIUtil;
import com.qihoo.finance.lowcode.common.configuration.UserContextPersistent;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.entity.dto.SQLBatchExecuteResult;
import com.qihoo.finance.lowcode.common.entity.dto.SQLExecuteResult;
import com.qihoo.finance.lowcode.common.entity.enums.SqlOperation;
import com.qihoo.finance.lowcode.common.enums.LightVirtualType;
import com.qihoo.finance.lowcode.common.ui.CustomHeightTabbedPaneUI;
import com.qihoo.finance.lowcode.common.ui.dialog.SimpleDialog;
import com.qihoo.finance.lowcode.common.util.*;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.common.utils.LocalDateUtils;
import com.qihoo.finance.lowcode.console.mongo.view.table.DateTimeSpinner;
import com.qihoo.finance.lowcode.console.mysql.SQLEditorManager;
import com.qihoo.finance.lowcode.console.mysql.editor.SQLDataEditor;
import com.qihoo.finance.lowcode.console.mysql.result.ResultManager;
import com.qihoo.finance.lowcode.console.mysql.result.action.ApplyAction;
import com.qihoo.finance.lowcode.console.mysql.result.action.RefreshAction;
import com.qihoo.finance.lowcode.console.mysql.result.action.UndoAction;
import com.qihoo.finance.lowcode.console.mysql.result.listener.ResultTableMouseListener;
import com.qihoo.finance.lowcode.design.constant.FieldTypeMatch;
import com.qihoo.finance.lowcode.design.entity.DatabaseColumnNode;
import com.qihoo.finance.lowcode.design.entity.DatabaseNode;
import com.qihoo.finance.lowcode.design.util.DatabaseDesignUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.qihoo.finance.lowcode.console.mysql.result.listener.ResultTableMouseListener.generateInsertSQL;

/**
 * ExecutionConsoleForm
 *
 * @author fengjinfu-jk
 * date 2023/9/13
 * @version 1.0.0
 * @apiNote ExecutionConsoleForm
 */
@Getter
public class ResultView implements Disposable {
    public static final long PAGE_SIZE = 100;
    public static final int COLUMN_MIN_WIDTH = 50;
    public static final int COLUMN_MAX_WIDTH = 200;
    private final Project project;
    private final JPanel mainPanel;
    public final LoadingDecorator tabLoading;
    public final JBTabbedPane tabbedPane;
    public final Map<String, JBTabbedPane> tabMap = new HashMap<>();
    protected static final UserContextPersistent userContextPersistent;
    protected static final UserContextPersistent.UserContext userContext;

    static {
        userContextPersistent = ApplicationManager.getApplication().getService(UserContextPersistent.class);
        userContext = userContextPersistent.getState();
    }

    public ResultView(@NotNull Project project) {
        this.project = project;
        this.mainPanel = new JPanel(new BorderLayout());
        this.tabbedPane = new JBTabbedPane();
        this.tabbedPane.setUI(new CustomHeightTabbedPaneUI());
        this.tabLoading = new LoadingDecorator(this.tabbedPane, this, 0);

        mainPanel.add(tabLoading.getComponent(), BorderLayout.CENTER);
    }

    @NotNull
    public JPanel getMainComponent() {
        return this.mainPanel;
    }

    @Override
    public void dispose() {
        Disposer.dispose(this);
    }

    public void loading() {
        UIUtil.invokeLaterIfNeeded(() -> tabLoading.startLoading(false));
    }

    public void closeLoading() {
        UIUtil.invokeLaterIfNeeded(tabLoading::stopLoading);
    }

    public void refresh(String tabName, Result<SQLBatchExecuteResult> batchResult) {
        // show result panel
        DatabaseNode database = DataContext.getInstance(project).getConsoleDatabase(tabName);
        if (Objects.isNull(database)) return;

        if (tabMap.containsKey(tabName)) {
            JBTabbedPane resTab = tabMap.get(tabName);
            resTab.removeAll();
            Map<String, Component> resultComponents = resultComponents(database, batchResult);
            resultComponents.forEach(resTab::add);
            if (resultComponents.size() > 1 && resTab.getTabCount() > 1) resTab.setSelectedIndex(1);

            tabbedPane.setSelectedComponent(resTab);
        } else {
            Map<String, Component> resultComponents = resultComponents(database, batchResult);
            JBTabbedPane resTab = new JBTabbedPane();
            resTab.setUI(new CustomHeightTabbedPaneUI());
            resultComponents.forEach(resTab::add);
            if (resultComponents.size() > 1 && resTab.getTabCount() > 1) resTab.setSelectedIndex(1);

            tabMap.put(tabName, resTab);
            tabbedPane.addTab(tabName, SQLEditorManager.datasourceIcon(getDatasource()), resTab);
            tabbedPane.setSelectedIndex(Math.max(0, tabbedPane.getTabCount() - 1));
        }

        tabbedPane.revalidate();
        tabbedPane.repaint();
    }

    private String getDatasource() {
        VirtualFile file = FileEditorManager.getInstance(project).getSelectedEditor().getFile();
        String datasource = file.getUserData(SQLEditorManager.SQL_CONSOLE_DATASOURCE);
        if (StringUtils.isEmpty(datasource) && Objects.nonNull(userContext)) {
            String datasourceType = DataContext.getInstance(project).getDatasourceType();
            datasource = StringUtils.defaultIfEmpty(datasourceType, Constants.DataSource.MySQL);
        }
        return datasource;
    }

    private Map<String, Component> resultComponents(DatabaseNode database, Result<SQLBatchExecuteResult> executeResult) {
        // 结果展示
        Map<String, Component> resultComponent = new LinkedHashMap<>();

        // 业务失败
        SQLBatchExecuteResult batchResult = executeResult.getData();
        if (!executeResult.isSuccess() || Objects.isNull(batchResult)) {
            resultComponent.put("  信息  ", createExecuteFailView(executeResult));
            return resultComponent;
        }

        // 业务成功
        int count = 1;
        resultComponent.put("  信息  ", createExecuteView(batchResult.getResultOverview()));
        List<SQLExecuteResult> sqlExecuteResults = batchResult.getSqlExecuteResults();
        for (SQLExecuteResult data : sqlExecuteResults) {
            // show sql
            JPanel result = new JPanel(new BorderLayout());
            Component showSql = showExecuteSQL(database, data);
            result.add(showSql, BorderLayout.NORTH);

            if (SqlOperation.QUERY.equals(data.getSqlOperation())) {
                // show component
                JScrollPane tableScroll = new JBScrollPane();
                LoadingDecorator tableLoading = new LoadingDecorator(tableScroll, this, 0);
                tableScroll.putClientProperty(SQLDataEditor.EXECUTE_RESULT, data);
                // bottom
                JPanel bottomToolbar = new JPanel(new BorderLayout());
                // page
                TablePageBtnPanel pageBtnPanel = this.tablePageButton(database, data, tableScroll, tableLoading);
                bottomToolbar.add(pageBtnPanel.getButtonPanel(), BorderLayout.CENTER);
                // table
                JBTable component = createTablePanel(database, data, pageBtnPanel);
                // operateBtnPanel
                bottomToolbar.add(this.operateWestBtnPanel(component, pageBtnPanel), BorderLayout.WEST);
                bottomToolbar.add(this.operateEastBtnPanel(database, component, pageBtnPanel), BorderLayout.EAST);
                result.add(bottomToolbar, BorderLayout.SOUTH);

                // show result
                tableScroll.setViewportView(component);
                result.add(tableLoading.getComponent(), BorderLayout.CENTER);
                resultComponent.put("  结果" + count++ + "  ", result);
            } else {
                // 如果是DDL则额外需要刷新DbTree
                this.refreshDbTreeIfNeed(database, data);
            }
        }

        return resultComponent;
    }

    private JPanel operateWestBtnPanel(JBTable table, TablePageBtnPanel pageBtnPanel) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton updateBtn = new JButton("复制为Update语句");
        updateBtn.addActionListener(e -> {
            SQLExecuteResult data = (SQLExecuteResult) pageBtnPanel.getTableScroll().getClientProperty(SQLDataEditor.EXECUTE_RESULT);
            ResultTableMouseListener.copySQLToClipboard(table, data, false);
        });
        panel.add(updateBtn);
        updateBtn.setEnabled(false);

        JButton insertBtn = new JButton("复制为Insert语句");
        insertBtn.addActionListener(e -> {
            SQLExecuteResult data = (SQLExecuteResult) pageBtnPanel.getTableScroll().getClientProperty(SQLDataEditor.EXECUTE_RESULT);
            ResultTableMouseListener.copySQLToClipboard(table, data, true);
        });
        insertBtn.setEnabled(false);
        panel.add(insertBtn);

        // primaryKeys
        SQLExecuteResult data = (SQLExecuteResult) pageBtnPanel.getTableScroll().getClientProperty(SQLDataEditor.EXECUTE_RESULT);
        List<String> primaryKeys = DatabaseDesignUtils.getPrimaryKeys(data);
        boolean hasPrimaryColumns = hasPrimaryColumns(table, primaryKeys);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (hasPrimaryColumns) {
                updateBtn.setEnabled(table.getSelectedRowCount() > 0);
                insertBtn.setEnabled(table.getSelectedRowCount() > 0);
            }
        });

        return panel;
    }

    private static boolean hasPrimaryColumns(JTable table, List<String> primaryKeys) {
        Enumeration<TableColumn> columns = table.getColumnModel().getColumns();
        while (columns.hasMoreElements()) {
            TableColumn tableColumn = columns.nextElement();
            String columnName = tableColumn.getHeaderValue().toString().trim();
            if (primaryKeys.contains(columnName)) return true;
        }
        return false;
    }

    private JPanel operateEastBtnPanel(DatabaseNode database, JBTable table, TablePageBtnPanel pageBtnPanel) {
        SQLExecuteResult data = (SQLExecuteResult) pageBtnPanel.getTableScroll().getClientProperty(SQLDataEditor.EXECUTE_RESULT);

        // primaryKeys
        List<String> primaryKeys = DatabaseDesignUtils.getPrimaryKeys(data);
        boolean hasPrimaryColumns = hasPrimaryColumns(table, primaryKeys);
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        // action
        DefaultActionGroup group = new DefaultActionGroup("SQLResultGroup", true);
        addAction(group, database, table, pageBtnPanel);
        group.addSeparator();
        ActionToolbar actionToolBar = ActionManager.getInstance().createActionToolbar("SQLResultGroupActions", group, true);
        actionToolBar.setTargetComponent(actionToolBar.getComponent());
        //actionToolBar.setLayoutPolicy(ActionToolbar.AUTO_LAYOUT_POLICY);
        actionToolBar.setLayoutStrategy(ToolbarLayoutStrategy.AUTOLAYOUT_STRATEGY);
        JComponent actionToolBarComponent = actionToolBar.getComponent();
        actionToolBarComponent.setBorder(null);
        actionToolBarComponent.setOpaque(false);
        panel.add(actionToolBarComponent);

        JButton setNull = new JButton("置为Null");
        setNull.addActionListener(e -> {
            setNullAction(table, table.getSelectedRow(), table.getSelectedColumn());
        });
        setNull.setIcon(Icons.scaleToWidth(Icons.EDIT, 13));
        setNull.setEnabled(false);
        panel.add(setNull);

        JButton setEmpty = new JButton("置为空字符串");
        setEmpty.addActionListener(e -> {
            setEmptyAction(table, table.getSelectedRow(), table.getSelectedColumn());
        });
        setEmpty.setIcon(Icons.scaleToWidth(Icons.EDIT, 13));
        setEmpty.setEnabled(false);
        panel.add(setEmpty);

        table.getSelectionModel().addListSelectionListener(x -> {
            if (hasPrimaryColumns) {
                setNull.setEnabled(table.getSelectedRowCount() > 0);
                setEmpty.setEnabled(table.getSelectedRowCount() > 0);
            }
        });

        // table风格
        configTableStyle(table, data.getHeaders(), data);
        return panel;
    }

    private void addAction(DefaultActionGroup group, DatabaseNode database, JBTable table, TablePageBtnPanel pageBtnPanel) {
        group.addAction(new AnAction("添加记录", "添加记录", Icons.scaleToWidth(Icons.ADD_ICON, 16)) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                newAction(table, pageBtnPanel);
            }
        });
        group.addAction(new AnAction("删除记录", "删除记录", Icons.scaleToWidth(Icons.SUB, 16)) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                deleteAction(database, table, pageBtnPanel);
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                super.update(e);
                e.getPresentation().setEnabled(table.getSelectedRowCount() > 0);
            }
        });
        // 应用更改
        group.addAction(new ApplyAction(database, table, pageBtnPanel));
        // 放弃更改
        group.addAction(new UndoAction(table, pageBtnPanel));
        // 刷新
        group.addAction(new RefreshAction(database, table, pageBtnPanel));
    }

    public void setEmptyAction(JBTable table, int row, int column) {
        table.getModel().setValueAt(StringUtils.EMPTY, row, column);
        table.setValueAt(StringUtils.EMPTY, row, column);
    }

    public void setNullAction(JBTable table, int row, int column) {
        table.getModel().setValueAt(null, row, column);
        table.setValueAt(null, row, column);
    }

    public void newAction(JBTable table, TablePageBtnPanel pageBtnPanel) {
        SQLExecuteResult data = (SQLExecuteResult) pageBtnPanel.getTableScroll().getClientProperty(SQLDataEditor.EXECUTE_RESULT);
        // 跳转至最后一页, 添加新行
        List<DatabaseColumnNode> columnInfos = DatabaseDesignUtils.getColumnInfos(data);
        Map<String, DatabaseColumnNode> columnMap = columnInfos.stream().collect(Collectors.toMap(DatabaseColumnNode::getFieldName, Function.identity()));
        pageBtnPanel.getLastPageAction().doAction(newTable -> {
            DefaultTableModel model = (DefaultTableModel) newTable.getModel();
            List<Object> values = new ArrayList<>();
            for (int columnIndex = 0; columnIndex < model.getColumnCount(); columnIndex++) {
                String columnName = model.getColumnName(columnIndex).trim();
                DatabaseColumnNode columnNode = columnMap.get(columnName);
                if (Objects.nonNull(columnNode) && Objects.nonNull(columnNode.getFieldDefaults())) {
                    values.add(convertFieldDefault(columnNode.getFieldDefaults()));
                } else {
                    values.add(null);
                }
            }
            model.addRow(values.toArray());
            // 滚动到底部并选中
            UIUtil.invokeLaterIfNeeded(() -> {
                scrollToBottom(newTable);
            });
        });
    }

    private static void scrollToBottom(JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        int selectRow = model.getRowCount() - 1;
        table.setRowSelectionInterval(selectRow, selectRow);
        Rectangle rect = table.getCellRect(selectRow, 0, true);

        table.repaint();
        table.updateUI();
        table.scrollRectToVisible(rect);
    }

    public void deleteAction(DatabaseNode database, JBTable table, TablePageBtnPanel pageBtnPanel) {
        SQLExecuteResult data = (SQLExecuteResult) pageBtnPanel.getTableScroll().getClientProperty(SQLDataEditor.EXECUTE_RESULT);
        // primaryKeys
        List<String> primaryKeys = DatabaseDesignUtils.getPrimaryKeys(data);
        Pair<List<Map<String, Object>>, List<Integer>> keyValues = getPrimaryKeyValues(data, table, primaryKeys);
        if (confirmDelete(keyValues)) {
            // 删除临时新增行
            List<Integer> deleteRows = keyValues.getRight();
            deleteRows.sort(Comparator.comparingInt(v -> -v));
            for (Integer row : deleteRows) {
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                model.removeRow(row);
            }
            // 数据库物理删除
            List<Map<String, Object>> deletePrimaryKeys = keyValues.getLeft();
            if (CollectionUtils.isNotEmpty(deletePrimaryKeys)) {
                String deleteSQL = generateDeleteSQL(data.getTableName(), deletePrimaryKeys);
                executeSQLAndShowResult(database, pageBtnPanel, deleteSQL);
            }
        }
    }

    private boolean confirmDelete(Pair<List<Map<String, Object>>, List<Integer>> keyValues) {
        List<Map<String, Object>> primaryKeyValues = keyValues.getLeft();
        if (CollectionUtils.isEmpty(primaryKeyValues)) return true;

        String primaryKeyInfo = primaryKeyValues.stream().map(v -> v.keySet().stream().map(k -> k + " : " + v.get(k).toString()).collect(Collectors.joining(", "))).collect(Collectors.joining("\n"));
        int update = Messages.showDialog("确定删除以下数据?\n\n" + primaryKeyInfo, "删除数据", new String[]{"是", "否"}, Messages.NO, Icons.scaleToWidth(Icons.WARN, 50));
        return update == Messages.YES;
    }

    public void executeSQLAndShowResult(DatabaseNode database, TablePageBtnPanel pageBtnPanel, String sql) {
        Result<SQLBatchExecuteResult> result = DatabaseDesignUtils.batchExecuteConsoleSQL(database, sql);
        if (result.isFail()) {
            SQLBatchExecuteResult executeResult = new SQLBatchExecuteResult();
            result.setData(executeResult);
            executeResult.getResultOverview().add(new SQLBatchExecuteResult.ResultItem(false, sql, result.getErrorMsg()));
        }
        boolean success = result.getData().getResultOverview().stream().allMatch(SQLBatchExecuteResult.ResultItem::isSuccess);
        if (success) {
            pageBtnPanel.getReloadAction().actionPerformed(null);
        } else {
            showErrMsgDialog(project, result);
        }
    }

    public static void showErrMsgDialog(Project project, Result<SQLBatchExecuteResult> result) {
        JPanel resultPanel = new JPanel(new BorderLayout());
        JComponent resultDetail = createExecuteView(result.getData().getResultOverview());
        resultDetail.setPreferredSize(new Dimension(650, 300));
        resultPanel.add(resultDetail, BorderLayout.CENTER);
        JTextArea title = JPanelUtils.tips(Icons.WARNING_COLOR);
        title.setText("执行失败, 详情请查看Message");
        title.setBorder(BorderFactory.createEmptyBorder(5, 10, 15, 10));
        resultPanel.add(title, BorderLayout.NORTH);

        SimpleDialog simpleDialog = new SimpleDialog(project, "执行结果", "确定", "关闭", resultPanel);
        simpleDialog.show();
    }

    private String generateDeleteSQL(String tableName, List<Map<String, Object>> primaryKeys) {
        // DELETE FROM table_name WHERE condition;
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> primaryKey : primaryKeys) {
            sb.append("DELETE FROM ").append(tableName).append(" WHERE ");
            for (String primaryColumn : primaryKey.keySet()) {
                sb.append("`").append(primaryColumn).append("` = ").append(primaryKey.get(primaryColumn)).append(";\n");
            }
        }

        return sb.toString();
    }

    private static Pair<List<Map<String, Object>>, List<Integer>> getPrimaryKeyValues(SQLExecuteResult data, JBTable table, List<String> primaryKeys) {
        List<Integer> deleteRowIndexList = new ArrayList<>();
        List<Integer> columnIndexList = new ArrayList<>();
        for (int columnIndex = 0; columnIndex < table.getColumnCount(); columnIndex++) {
            String columnName = table.getColumnModel().getColumn(columnIndex).getHeaderValue().toString().trim();
            if (primaryKeys.contains(columnName)) {
                columnIndexList.add(columnIndex);
            }
        }

        List<Map<String, Object>> rows = data.getRows();
        List<Map<String, Object>> primaryKeyValList = new ArrayList<>();
        for (int selectedRow : table.getSelectedRows()) {
            if (selectedRow >= rows.size()) {
                // 删除手动新增未提交的行
                deleteRowIndexList.add(selectedRow);
                continue;
            }
            Map<String, Object> primaryKeyVal = new HashMap<>();
            for (int columnIndex = 0; columnIndex < columnIndexList.size(); columnIndex++) {
                primaryKeyVal.put(table.getColumnName(columnIndex).trim(), table.getModel().getValueAt(selectedRow, columnIndex));
            }
            primaryKeyValList.add(primaryKeyVal);
        }

        return Pair.of(primaryKeyValList, deleteRowIndexList);
    }

    private Object convertFieldDefault(Object fieldDefaults) {
        if (Objects.isNull(fieldDefaults)) return null;

        if (fieldDefaults.equals(Constants.DB_COLUMN.DEFAULT_ZERO)) {
            return 0;
        }
        if (fieldDefaults.equals(Constants.DB_COLUMN.DEFAULT_NULL)) {
            return "";
        }
        if (fieldDefaults.equals(Constants.DB_COLUMN.EMPTY_STRING)) {
            return "";
        }
        if (fieldDefaults.equals(Constants.DB_COLUMN.CURRENT_TIMESTAMP)) {
            return LocalDateUtils.convertToPatternString(new Date(), LocalDateUtils.FORMAT_DATE_TIME);
        }
        if (fieldDefaults.equals(Constants.DB_COLUMN.UPDATE_TIMESTAMP)) {
            return LocalDateUtils.convertToPatternString(new Date(), LocalDateUtils.FORMAT_DATE_TIME);
        }
        return fieldDefaults;
    }

    public List<String> executeUpdate(JTable table, SQLExecuteResult data) {
        if (Objects.isNull(data)) return new ArrayList<>();

        // insert
        List<String> insertSQLList = new ArrayList<>();
        // 主键信息
        List<String> primaryKeys = DatabaseDesignUtils.getPrimaryKeys(data);
        // 获取被修改的字段信息
        Map<Integer, Map<String, Object>> updateRows = new HashMap<>();
        Map<Integer, Map<String, Object>> updateRowPrimaryKeys = new HashMap<>();
        List<Map<String, Object>> rows = data.getRows();
        for (int rowNum = 0; rowNum < table.getRowCount(); rowNum++) {
            Map<String, Object> updateColumns = new HashMap<>();
            Map<String, Object> rowPrimaryKeys = new HashMap<>();
            if (rowNum >= rows.size()) {
                String insertSQL = generateInsertSQL(table, data.getTableName(), rowNum);
                insertSQLList.add(insertSQL);
                continue;
            }
            // 编辑数据行
            Map<String, Object> row = rows.get(rowNum);
            for (int columnNum = 0; columnNum < table.getColumnCount(); columnNum++) {
                String columnName = table.getColumnModel().getColumn(columnNum).getHeaderValue().toString().trim();
                Object newVal = table.getModel().getValueAt(rowNum, columnNum);
                Object oldVal = row.get(columnName);
                newVal = Objects.nonNull(newVal) ? newVal.toString() : null;
                oldVal = Objects.nonNull(oldVal) ? oldVal.toString() : null;
                if (isDifference(oldVal, newVal)) {
                    updateColumns.put(columnName, newVal);
                }
                if (primaryKeys.contains(columnName)) {
                    rowPrimaryKeys.put(columnName, oldVal);
                }
            }
            if (MapUtils.isNotEmpty(updateColumns)) {
                updateRows.put(rowNum, updateColumns);
            }
            if (MapUtils.isNotEmpty(rowPrimaryKeys)) {
                updateRowPrimaryKeys.put(rowNum, rowPrimaryKeys);
            }
        }

        // 组装更新语句
        List<String> updateSQLList = updateRows.keySet().stream().map(row -> {
            Map<String, Object> updateColumns = updateRows.get(row);
            Map<String, Object> updatePrimaryKeys = updateRowPrimaryKeys.get(row);
            return generateUpdateSQL(data.getTableName(), updatePrimaryKeys, updateColumns);
        }).collect(Collectors.toList());
        // 追加insert
        updateSQLList.addAll(insertSQLList);

        return updateSQLList;
    }

    private static String generateUpdateSQL(String tableName, Map<String, Object> primaryKeys, Map<String, Object> updateColumns) {
        // String updateSQL = "UPDATE `lingxi_server`.`app_base_info` SET `app_code` = 'ags-app', `deleted_at` = 0 WHERE `id` = 25;";
        StringBuilder sb = new StringBuilder();

        StringBuilder columnNameSb = new StringBuilder("UPDATE ");
        columnNameSb.append("`").append(tableName).append("`").append(" SET ");
        int initLength = columnNameSb.length();
        for (String columnName : updateColumns.keySet()) {
            Object valueAt = updateColumns.get(columnName);
            String columnValue = String.valueOf(valueAt);
            boolean bit = Constants.REGEX.BIT.matcher(columnValue).find();
            // split
            if (columnNameSb.length() > initLength) {
                columnNameSb.append(", ");
            }

            // column&value
            columnNameSb.append("`").append(columnName).append("` = ");
            if (valueAt instanceof String && !bit) {
                columnNameSb.append("'").append(columnValue).append("'");
            } else {
                columnNameSb.append(columnValue);
            }
        }

        StringBuilder whereSb = new StringBuilder(" WHERE ");
        for (String columnName : primaryKeys.keySet()) {
            Object valueAt = primaryKeys.get(columnName);
            String columnValue = String.valueOf(valueAt);
            boolean bit = Constants.REGEX.BIT.matcher(columnValue).find();
            // split
            if (whereSb.toString().contains(" = ")) {
                whereSb.append(" AND ");
            }

            whereSb.append("`").append(columnName).append("` = ");
            if (valueAt instanceof String && !bit) {
                whereSb.append("'").append(columnValue).append("'");
            } else {
                whereSb.append(columnValue);
            }
        }

        sb.append(columnNameSb.append(whereSb).append(";\n"));
        return sb.toString();
    }

    private static boolean isDifference(Object val1, Object val2) {
        return !isSame(val1, val2);
    }

    private static boolean isSame(Object val1, Object val2) {
        if (val1 != null) return val1.equals(val2);
        return val2 == null;
    }

    private Component createExecuteFailView(Result<SQLBatchExecuteResult> result) {
        // 业务失败时, 展示错误信息
        String viewText = String.format("execute fail, errorCode: %s, errorMessage: %s", result.getErrorCode(), StringUtils.defaultString(result.getErrorMsg(), "Invalid sql statement"));
        Editor editor = EditorComponentUtils.createEditorPanel(project, LightVirtualType.SQL);
        WriteCommandAction.runWriteCommandAction(project, () -> {
            editor.getDocument().setText(viewText);
            editor.getDocument().setReadOnly(true);
        });

        return editor.getComponent();
    }

    public static JComponent createExecuteView(List<SQLBatchExecuteResult.ResultItem> sqlViews) {
        List<Map<String, Object>> data = new ArrayList<>();
        String columnSQL = "SQL";
        String columnMsg = "Message";
        String result = "Result";
        List<String> headers = Lists.newArrayList(columnSQL, columnMsg, result);
        for (SQLBatchExecuteResult.ResultItem sqlView : sqlViews) {
            Map<String, Object> viewRow = new HashMap<>();
            viewRow.put(columnSQL, sqlView.getSql());
            viewRow.put(columnMsg, sqlView.getMessage());
            viewRow.put(result, sqlView.isSuccess());
            data.add(viewRow);
        }

        DefaultTableModel tableModel = new DefaultTableModel(getTableData(headers, data), getTableHeaders(headers));
        JBTable table = new JBTable(tableModel);
        new TableSpeedSearch(table);
        // 表格样式
        configTableStyle(table, headers);
        // 设置表头
        JTableUtils.setTableHeaderRenderer(table, null);
        // 防止滚动条
        JScrollPane scrollPane = new JBScrollPane();
        scrollPane.setViewportView(table);

        return scrollPane;
    }

    private void refreshDbTreeIfNeed(DatabaseNode database, SQLExecuteResult resultData) {
        // 执行DDL后需刷新对应数据库信息
        if (SqlOperation.DDL.equals(resultData.getSqlOperation())) {
            CacheManager.refreshInnerCache();
            JTreeLoadingUtils.loading(true, DataContext.getInstance(project).getDbTree(), database, () -> DatabaseDesignUtils.queryMySQLTableNodes(database));
        }
    }

    @Getter
    public static class TablePageBtnPanel {
        private final JPanel buttonPanel;
        private final PageActionListener reloadAction;
        private final PageActionListener lastPageAction;
        private final JScrollPane tableScroll;

        public TablePageBtnPanel(JScrollPane tableScroll, JPanel buttonPanel, PageActionListener reloadAction, PageActionListener lastPageAction) {
            this.tableScroll = tableScroll;
            this.buttonPanel = buttonPanel;
            this.reloadAction = reloadAction;
            this.lastPageAction = lastPageAction;
        }
    }

    private TablePageBtnPanel tablePageButton(DatabaseNode database, SQLExecuteResult resultData, JScrollPane tableScrollPane, LoadingDecorator tableLoading) {
        String sql = resultData.getExecuteSql();
        JPanel buttonPanel = new JPanel(new FlowLayout());

        int page = 1;
        long count = resultData.getSelectCount();
        JLabel showPage = new JLabel(String.valueOf(page));
        JLabel showCurrentCount = new JLabel(String.format("当前页 %s 条", resultData.getRows().size()));
        JLabel showCount = new JLabel(String.format("共 %s 条, 每页最多展示 %s 条", count, PAGE_SIZE));

        JButton prePage = new JButton(Icons.scaleToWidth(Icons.PRE_PAGE, 16));
        prePage.setPreferredSize(new Dimension(30, 30));
        prePage.setBorderPainted(false);
        prePage.setContentAreaFilled(false);
        prePage.addActionListener(new PageActionListener(database, tableScrollPane, tableLoading, showPage, showCount, showCurrentCount, sql, PageActionListener.PageOperate.PRE_PAGE));

        JButton nextPage = new JButton(Icons.scaleToWidth(Icons.NEXT_PAGE, 16));
        nextPage.setPreferredSize(new Dimension(30, 30));
        nextPage.setBorderPainted(false);
        nextPage.setContentAreaFilled(false);
        nextPage.addActionListener(new PageActionListener(database, tableScrollPane, tableLoading, showPage, showCount, showCurrentCount, sql, PageActionListener.PageOperate.NEXT_PAGE));

        buttonPanel.add(showCurrentCount, FlowLayout.LEFT);
        buttonPanel.add(prePage);
        buttonPanel.add(showPage);
        buttonPanel.add(nextPage);
        buttonPanel.add(showCount);

        PageActionListener reload = new PageActionListener(database, tableScrollPane, tableLoading, showPage, showCount, showCurrentCount, sql, PageActionListener.PageOperate.RELOAD_PAGE);
        PageActionListener lastPage = new PageActionListener(database, tableScrollPane, tableLoading, showPage, showCount, showCurrentCount, sql, PageActionListener.PageOperate.LAST_PAGE);
        return new TablePageBtnPanel(tableScrollPane, buttonPanel, reload, lastPage);
    }

    private Component showExecuteSQL(DatabaseNode databaseNode, SQLExecuteResult data) {
        Editor editor = EditorComponentUtils.createEditorPanel(project, LightVirtualType.SQL);
        WriteCommandAction.runWriteCommandAction(project, () -> {
            editor.getDocument().setText(String.format(" [%s] %s(%s)> %s\n [%s] completed in %s ms", databaseNode.getDepName(), data.getDatabaseName(), data.getInstanceName(), data.getExecuteSql().trim(), LocalDateUtils.now(), data.getCostMillis()));
            editor.getDocument().setReadOnly(true);
        });

        int scrollHeight = 0;
        if (editor instanceof EditorEx) {
            EditorEx editorEx = (EditorEx) editor;
            editorEx.setVerticalScrollbarVisible(false);
//            scrollHeight = editorEx.getScrollPane().getHorizontalScrollBar().getPreferredSize().height;
        }

        JComponent component = editor.getComponent();
        component.setPreferredSize(new Dimension(-1, scrollHeight + editor.getLineHeight() * 2));
        return component;
    }

    private JBTable createTablePanel(DatabaseNode database, SQLExecuteResult data, TablePageBtnPanel pageBtnPanel) {
        List<String> headers = data.getHeaders();
        List<Map<String, Object>> result = data.getRows();

        DefaultTableModel tableModel = new DefaultTableModel(getTableData(headers, result), getTableHeaders(headers));
        JBTable table = new JBTable(tableModel);
        // 支持快速搜索
        new TableSpeedSearch(table);
        // table风格
        configTableStyle(table, headers);
        // 设置表头
        JTableUtils.setTableHeaderRenderer(table, Icons.Nodes.DataColumn);
        // 宽度自适应
        JTableUtils.fitTableWidth(table, Toolkit.getDefaultToolkit().getScreenSize().width - 150);
        // table事件
        table.addMouseListener(new ResultTableMouseListener(database, table, pageBtnPanel));

        return table;
    }

    private static Object[] getTableHeaders(Collection<String> headers) {
        return headers.stream().map(header -> " " + header + " ").toArray();
    }

    private static Object[][] getTableData(Collection<String> headers, List<Map<String, Object>> result) {
        Object[][] tableData = new Object[result.size()][];
        int rowCount = 0;
        for (Map<String, Object> colum : result) {
            Object[] columData = new Object[colum.values().size()];
            int columnCount = 0;
            for (String header : headers) {
                columData[columnCount] = colum.get(header);
                columnCount++;
            }

            tableData[rowCount] = columData;
            rowCount++;
        }

        return tableData;
    }

    private static void configTableStyle(JTable table, Collection<String> headers) {
        configTableStyle(table, headers, null);
    }

    private static void configTableStyle(JTable table, Collection<String> headers, SQLExecuteResult data) {
        boolean editable = false;
        List<DatabaseColumnNode> columnInfos = new ArrayList<>();
        if (Objects.nonNull(data)) {
            columnInfos = DatabaseDesignUtils.getColumnInfos(data);
            List<String> primaryKeys = columnInfos.stream().filter(DatabaseColumnNode::isPK).map(DatabaseColumnNode::getFieldName).collect(Collectors.toList());
            editable = hasPrimaryColumns(table, primaryKeys);
        }
        // 设置表格
        CellRenderer centerRenderer = new CellRenderer(data);
        centerRenderer.setHorizontalAlignment(JLabel.LEFT);
        Map<String, DatabaseColumnNode> columnMap = columnInfos.stream().collect(Collectors.toMap(DatabaseColumnNode::getFieldName, Function.identity()));
        for (int i = 0; i < headers.size(); i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            column.setCellRenderer(centerRenderer);
            Object headerValue = column.getHeaderValue();
            String columnName = headerValue.toString().trim();

            DatabaseColumnNode columnNode = columnMap.get(columnName);
            if (Objects.nonNull(columnNode) && FieldTypeMatch.DATE_TYPE.contains(columnNode.getFieldType().toLowerCase())) {
                column.setCellEditor(new DateSpinnerCellEditor());
            } else {
                column.setCellEditor(new ColumnEditor(editable));
            }
        }

        // 背景
        UIUtil.invokeLaterIfNeeded(() -> {
            table.setBackground(EditorComponentUtils.BACKGROUND);
        });
    }

    public void removeTab(String tabName) {
        if (tabMap.containsKey(tabName)) {
            Component component = tabMap.get(tabName);
            tabbedPane.remove(component);
            tabMap.remove(tabName);

            if (tabbedPane.getTabCount() == 0) {
                ResultManager.getInstance().unAvailableExecutionConsole();
            }
        }
    }

    // ~ inner class
//------------------------------------------------------------------------------------------------------------------
    static class DateSpinnerCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final DateTimeSpinner spinner;

        public DateSpinnerCellEditor() {
            this.spinner = DateTimeSpinner.create();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (value instanceof Date) {
                spinner.setValue(value);
            } else if (null != value) {
                try {
                    spinner.setValue(LocalDateUtils.formatDateStr(value.toString(), LocalDateUtils.FORMAT_DATE_TIME));
                } catch (Exception ignore) {
                    spinner.setValue(LocalDateUtils.formatDateStr(value.toString(), LocalDateUtils.FORMAT_DATE));
                }
            }
            return spinner;
        }

        @Override
        public Object getCellEditorValue() {
            try {
                ((JSpinner.DateEditor) spinner.getEditor()).commitEdit();
            } catch (ParseException ignore) {
            }
            return LocalDateUtils.convertToPatternString((Date) spinner.getValue(), LocalDateUtils.FORMAT_DATE_TIME);
        }
    }

    static class ColumnEditor extends AbstractCellEditor implements TableCellEditor {
        private final JTextField textField;
        private boolean isNull;

        public ColumnEditor(boolean editable) {
            textField = new JTextField();
            textField.setHorizontalAlignment(JTextField.LEFT);
            textField.setEditable(editable);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            isNull = Objects.isNull(value);
            textField.setText(Objects.nonNull(value) ? value.toString() : null);
            return textField;
        }

        @Override
        public Object getCellEditorValue() {
            String textVal = textField.getText();
            if (StringUtils.isNotEmpty(textVal)) {
                return textVal;
            }
            return isNull ? null : textVal;
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    static class CellRenderer extends DefaultTableCellRenderer {
        private SQLExecuteResult data;

        public CellRenderer(SQLExecuteResult data) {
            this.data = data;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            // update
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (Objects.isNull(value) && component instanceof JLabel nullLabel) {
                nullLabel.setText("NULL");
                nullLabel.setFont(new Font("微软雅黑", Font.ITALIC, 13));
                nullLabel.setForeground(JBColor.GRAY);
                return nullLabel;
            } else if (component instanceof JLabel label) {
                label.setFont(null);
                label.setForeground(JBColor.foreground());
                ((JLabel) component).setToolTipText(value.toString());
                return label;
            }
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    public boolean hadModified(@NotNull JTable table, @NotNull SQLExecuteResult data) {
        // 获取被修改的字段信息
        List<Map<String, Object>> rows = data.getRows();
        for (int rowNum = 0; rowNum < table.getRowCount(); rowNum++) {
            if (rowNum >= rows.size()) {
                return true;
            }
            // 编辑数据行
            Map<String, Object> row = rows.get(rowNum);
            for (int columnNum = 0; columnNum < table.getColumnCount(); columnNum++) {
                String columnName = table.getColumnModel().getColumn(columnNum).getHeaderValue().toString().trim();
                Object newVal = table.getModel().getValueAt(rowNum, columnNum);
                Object oldVal = row.get(columnName);
                newVal = Objects.nonNull(newVal) ? newVal.toString() : null;
                oldVal = Objects.nonNull(oldVal) ? oldVal.toString() : null;
                if (isDifference(oldVal, newVal)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static class PageActionListener implements ActionListener {
        public enum PageOperate {
            NEXT_PAGE, PRE_PAGE, LAST_PAGE, FIRST_PAGE, RELOAD_PAGE
        }

        private final JScrollPane tableScroll;
        private final LoadingDecorator tableLoading;
        private final JLabel showPage;
        private final JLabel showCount;
        private final JLabel showCurrentCount;
        private final String sql;
        private final PageOperate pageOperate;
        private final DatabaseNode database;

        public PageActionListener(DatabaseNode database, JScrollPane tableScroll, LoadingDecorator tableLoading, JLabel showPage, JLabel showCount, JLabel showCurrentCount, String sql, PageOperate pageOperate) {
            this.tableScroll = tableScroll;
            this.tableLoading = tableLoading;
            this.showPage = showPage;
            this.showCount = showCount;
            this.showCurrentCount = showCurrentCount;

            this.sql = sql;
            this.pageOperate = pageOperate;
            this.database = database;
        }

        public void doAction(Consumer<JTable> afterSuccess) {
            Component oldComponent = tableScroll.getViewport().getView();
            // 上次请求未完成
            long oldPage = Long.parseLong(showPage.getText());
            long count = Long.parseLong(showCount.getText().replace("共 ", "").replace(" 条, 每页最多展示 " + PAGE_SIZE + " 条", ""));
            long page = calculatePage(count, oldPage, pageOperate);
            if (page == oldPage && PageOperate.RELOAD_PAGE != pageOperate && oldComponent instanceof JTable) {
                tableScroll.setViewportView(oldComponent);
                if (Objects.nonNull(afterSuccess)) {
                    afterSuccess.accept((JTable) oldComponent);
                }
                return;
            }

            new SwingWorker<SQLExecuteResult, SQLExecuteResult>() {
                @Override
                protected SQLExecuteResult doInBackground() {
                    UIUtil.invokeLaterIfNeeded(() -> tableLoading.startLoading(false));
                    Result<SQLBatchExecuteResult> executeResult = DatabaseDesignUtils.batchExecuteConsoleSQL(database, sql, page, PAGE_SIZE);
                    // update view
                    SQLBatchExecuteResult data = executeResult.getData();
                    if (executeResult.isSuccess() && Objects.nonNull(data)) {
                        if (CollectionUtils.isNotEmpty(data.getSqlExecuteResults())) {
                            return data.getSqlExecuteResults().get(0);
                        }
                    }

                    return null;
                }

                @Override
                protected void done() {
                    try {
                        SQLExecuteResult data = get();
                        if (Objects.isNull(data)) {
                            tableScroll.setViewportView(oldComponent);
                            return;
                        }

                        // update view
                        if (oldComponent instanceof JTable) {
                            tableScroll.putClientProperty(SQLDataEditor.EXECUTE_RESULT, data);
                            JTable table = (JTable) oldComponent;
                            table.editingStopped(null);
                            table.removeAll();
                            DefaultTableModel model = (DefaultTableModel) table.getModel();
                            int rowCount = model.getRowCount();
                            for (int i = 0; i < rowCount; i++) {
                                model.removeRow(0);
                            }
                            for (Map<String, Object> row : data.getRows()) {
                                List<Object> values = new ArrayList<>();
                                for (int columnIndex = 0; columnIndex < model.getColumnCount(); columnIndex++) {
                                    // new rows
                                    values.add(row.get(model.getColumnName(columnIndex).trim()));
                                    TableColumn column = table.getColumnModel().getColumn(columnIndex);
                                    // renderer
                                    TableCellRenderer cellRenderer = column.getCellRenderer();
                                    if (cellRenderer instanceof CellRenderer) {
                                        CellRenderer renderer = (CellRenderer) cellRenderer;
                                        renderer.setData(data);
                                    }
                                }
                                model.addRow(values.toArray());

                            }

                            tableScroll.setViewportView(oldComponent);
                            if (Objects.nonNull(afterSuccess)) {
                                afterSuccess.accept(table);
                            }
                        }

                        // update page
                        showPage.setText(String.valueOf(page));
                        showCurrentCount.setText(String.format("当前页 %s 条", data.getRows().size()));
                        long newCount = data.getSelectCount();
                        showCount.setText(String.format("共 %s 条, 每页最多展示 %s 条", newCount, PAGE_SIZE));
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    } finally {
                        UIUtil.invokeLaterIfNeeded(tableLoading::stopLoading);
                        super.done();
                    }
                }
            }.execute();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            doAction(null);
        }

        private long calculatePage(long count, long oldPage, PageOperate pageOperate) {
            if (Objects.isNull(pageOperate)) return oldPage;

            long countPage = (count % PAGE_SIZE == 0) ? count / PAGE_SIZE : (count / PAGE_SIZE) + 1;
            switch (pageOperate) {
                case NEXT_PAGE:
                    return Math.min(oldPage + 1, countPage);
                case PRE_PAGE:
                    return Math.max(oldPage - 1, 1);
                case LAST_PAGE:
                    return countPage;
                case FIRST_PAGE:
                    return 1;
                case RELOAD_PAGE:
                    return oldPage;
            }
            return oldPage;
        }
    }
}
