package com.qihoo.finance.lowcode.design.dialog;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.entity.dto.SQLBatchExecuteResult;
import com.qihoo.finance.lowcode.common.enums.LightVirtualType;
import com.qihoo.finance.lowcode.common.listener.KeyListener;
import com.qihoo.finance.lowcode.common.ui.CustomHeightTabbedPaneUI;
import com.qihoo.finance.lowcode.common.util.*;
import com.qihoo.finance.lowcode.design.constant.DatabaseOperateType;
import com.qihoo.finance.lowcode.design.constant.FieldTypeMatch;
import com.qihoo.finance.lowcode.design.dto.rdb.RdbField;
import com.qihoo.finance.lowcode.design.dto.rdb.RdbFieldTypeConfig;
import com.qihoo.finance.lowcode.design.dto.rdb.RdbIndex;
import com.qihoo.finance.lowcode.design.entity.DatabaseNode;
import com.qihoo.finance.lowcode.design.entity.MySQLTableNode;
import com.qihoo.finance.lowcode.design.util.DatabaseDesignUtils;
import com.qihoo.finance.lowcode.gentracker.actions.GenerateAction;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import com.qihoo.finance.lowcode.gentracker.service.TableInfoSettingsService;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.*;

import static com.qihoo.finance.lowcode.common.constants.Constants.Encrypt.ENCRYPT_FIELD_SUFFIX;
import static com.qihoo.finance.lowcode.common.constants.Constants.Encrypt.MD5X_FIELD_SUFFIX;

/**
 * 表设计主窗口，内部嵌套了：字段属性、索引、SQL预览三个Tab
 *
 * @author weiyichao
 * @date 2023-07-28
 **/
@Slf4j
public class TableDesignerDialog extends DialogWrapper {
    private final Project project;
    private DatabaseOperateType operateType;
    private final DatabaseConfigPanel databaseConfigPanel;
    private final DatabaseIndexPanel databaseIndexPanel;
    private final DatabasePreviewPanel databasePreviewPanel;

    private String tableName;

    private String tableComment;

    private String tableEngine;

    private String tableCharacterSet;

    private JPanel dialogPanel;
    private JTabbedPane tabbedPane;


    public TableDesignerDialog(@NotNull Project project, DatabaseOperateType operateType) {
        super(project);
        this.databasePreviewPanel = project.getService(DatabasePreviewPanel.class);
        this.databaseConfigPanel = project.getService(DatabaseConfigPanel.class);
        this.databaseIndexPanel = project.getService(DatabaseIndexPanel.class);
        this.project = project;
        this.operateType = operateType;

        initData();
        initComponents();
        initSize();

        this.toFront();
        tabbedPane.setUI(new CustomHeightTabbedPaneUI());
        log.info(Constants.Log.USER_ACTION, "用户打开数据库设计界面");
    }

    public void show(int index) {
        this.tabbedPane.setSelectedIndex(index);
        super.show();
    }

    private void initData() {
        if (isEdit()) {
            MySQLTableNode selectDbTable = DataContext.getInstance(project).getSelectDbTable();
            this.tableName = selectDbTable.getTableName();
            this.tableComment = selectDbTable.getTableComment();
            this.tableCharacterSet = StringUtils.defaultString(selectDbTable.getCharset(), "utf8mb4");
            this.tableEngine = StringUtils.defaultString(selectDbTable.getEngine(), "InnoDB");
        } else if (isCopy()) {
            MySQLTableNode selectDbTable = DataContext.getInstance(project).getSelectDbTable();
            this.tableName = selectDbTable.getTableName() + "_copy";
            this.tableComment = selectDbTable.getTableComment();
            this.tableCharacterSet = StringUtils.defaultString(selectDbTable.getCharset(), "utf8mb4");
            this.tableEngine = StringUtils.defaultString(selectDbTable.getEngine(), "InnoDB");
        } else {
            this.tableName = "";
            this.tableComment = "";
            this.tableCharacterSet = "utf8mb4";
            this.tableEngine = "InnoDB";
        }
    }

    private void initSize() {
        JPanelUtils.setSize(dialogPanel, new Dimension(1000, 600));
    }

    public boolean isEdit() {
        return DatabaseOperateType.EDIT == operateType;
    }

    public boolean isCopy() {
        return DatabaseOperateType.COPY == operateType;
    }

    public boolean isCreate() {
        return DatabaseOperateType.CREATE == operateType;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return dialogPanel;
    }

    private Map<Integer, String> sqlPreviewMap;

    public void initComponents() {
        // 全局SQL预览数据集
        sqlPreviewMap = new LinkedHashMap<>();

        // 主面板
        dialogPanel = new JPanel(new BorderLayout());
        dialogPanel.add(createFormPanel(), BorderLayout.NORTH);

        // 创建选项卡页面
        tabbedPane = new JBTabbedPane();

        // 列表信息集合
        List<RdbField> rdbFields = new ArrayList<>();
        List<RdbField> droppedRdbFields = new ArrayList<>();
        List<RdbIndex> rdbIndexes = new ArrayList<>();
        List<RdbIndex> droppedRdbIndexes = new ArrayList<>();

        databaseConfigPanel.init(rdbFields, droppedRdbFields);
        databaseConfigPanel.setOperateType(this.operateType);
        databaseIndexPanel.init(rdbIndexes, droppedRdbIndexes, rdbFields);
        databaseIndexPanel.setOperateType(this.operateType);
        databasePreviewPanel.init(rdbFields, droppedRdbFields, rdbIndexes, droppedRdbIndexes, sqlPreviewMap);
        databasePreviewPanel.setOperateType(this.operateType);

        tabbedPane.addTab("字段属性", databaseConfigPanel.createPanel());
        tabbedPane.addTab("索引", databaseIndexPanel.createPanel());
        tabbedPane.addTab("表结构DDL", this.createDDLPanel());
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(15, 10, 0, 10));
        tabbedPane.setPreferredSize(new Dimension(1000, 430));
        dialogPanel.add(tabbedPane, BorderLayout.CENTER);

        // 创建选项卡页面
        JTabbedPane sqlTab = new JBTabbedPane();
        sqlTab.setUI(new CustomHeightTabbedPaneUI());
        JPanel sqlPreviewPanel = (JPanel) databasePreviewPanel.createPanel();
        sqlTab.add("SQL预览", sqlPreviewPanel);
        sqlTab.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        sqlTab.setPreferredSize(new Dimension(1000, 230));
        dialogPanel.add(sqlTab, BorderLayout.SOUTH);

        init();
        setModal(false);
        setTitle(GlobalDict.TITLE_INFO + (isEdit() ? "-编辑表" : "-新增表"));

        setOKButtonText(isEdit() ? "保存" : "创建");
        setCancelButtonText("取消");
    }

    private Component createDDLPanel() {
        if (!isEdit()) return new JLabel();

        JPanel ddlContent = new JPanel(new BorderLayout());
        Editor editor = EditorComponentUtils.createEditorPanel(project, LightVirtualType.SQL);
        ddlContent.add(editor.getComponent(), BorderLayout.CENTER);

        DataContext dataContext = DataContext.getInstance(project);
        DatabaseNode database = dataContext.getSelectDatabase();
        MySQLTableNode table = dataContext.getSelectDbTable();

        String ddl = DatabaseDesignUtils.queryTableCreateDDL(database, table);
        WriteCommandAction.runWriteCommandAction(project, () -> {
            editor.getDocument().setText(ddl);
            editor.getDocument().setReadOnly(true);
        });

        return ddlContent;
    }

    @Override
    protected void doOKAction() {
        log.info(Constants.Log.USER_ACTION, "用户提交数据库设计");

        // 在此调用后端接口实现保存逻辑
        Editor sqlPreviewEditor = databasePreviewPanel.getSqlPreviewEditor();
        String sqlScriptStr = sqlPreviewEditor.getDocument().getText();

        DataContext dataContext = DataContext.getInstance(project);
        DatabaseNode node = dataContext.getSelectDatabase();
        if (this.validateAndExecuteSQL(node.getDataSourceType(), node, sqlScriptStr)) {
            int generate = Messages.showDialog("保存成功！\n\n请进行代码生成操作", "", new String[]{"生成代码", "否"}, Messages.YES, Icons.scaleToWidth(Icons.SUCCESS, 60));
            if (generate == Messages.NO) {
                super.doOKAction();
                return;
            }

            // 需要重新查询表字段信息以刷新缓存
            TableInfoSettingsService.getInstance().getTableInfo(dataContext.getSelectDbTable(false));
            // 打开代码生成界面
            SwingUtilities.invokeLater(() -> new GenerateAction("生成代码", project, 2).actionPerformed(new ActionEvent(this, 0, null)));
            DataContext.getInstance(project).setMustSyncLoadDbTree(false);
            super.doOKAction();
        }
    }


    @Override
    protected Action @NotNull [] createActions() {
        // 创建自定义按钮的Action
        Action customAction = new AbstractAction("校验") {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 在此调用后端接口实现保存逻辑
                Editor sqlPreviewEditor = databasePreviewPanel.getSqlPreviewEditor();
                String sqlScriptStr = sqlPreviewEditor.getDocument().getText();

                DatabaseNode node = DataContext.getInstance(project).getSelectDatabase();
                validateSQL(e, node.getDataSourceType(), node, sqlScriptStr);
            }
        };

        // 将自定义按钮的Action添加到对话框中
        return new Action[]{customAction, getOKAction(), getCancelAction()};
    }

    private void validateSQL(ActionEvent e, String dataSourceType, DatabaseNode nameSpace, String sqlScriptStr) {
        if (StringUtils.isBlank(sqlScriptStr)) {
            Messages.showMessageDialog("校验成功 ！\n\n表结构未变更, 无需校验", "", Icons.scaleToWidth(Icons.SUCCESS, 60));
            return;
        }

        // 联动属性校验
        if (invalidateFields()) {
            return;
        }

        // 接口校验
        String database = nameSpace.getCode();
        Result<Boolean> checkResult = DatabaseDesignUtils.validateSQL(dataSourceType, database, tableName, sqlScriptStr, nameSpace.getNodeAttr());
        if (checkResult.isFail()) {
            // validate failure
            Messages.showMessageDialog("校验失败, 脚本校验不通过 : \n\n" + checkResult.getErrorMsg(), "", Icons.scaleToWidth(Icons.FAIL, 60));
            return;
        }

        Messages.showMessageDialog("校验通过 ！\n\n请执行保存操作", "", Icons.scaleToWidth(Icons.SUCCESS, 60));
    }

    private boolean validateAndExecuteSQL(String dataSourceType, DatabaseNode nameSpace, String sqlScriptStr) {
        if (StringUtils.isBlank(sqlScriptStr)) {
            Messages.showMessageDialog("保存成功 ！\n\n表结构未变更, 无需保存", "", Icons.scaleToWidth(Icons.SUCCESS, 60));
            return false;
        }

        // 联动属性校验
        if (invalidateFields()) {
            return false;
        }

        String database = nameSpace.getCode();
        Result<Boolean> checkResult = DatabaseDesignUtils.validateSQL(dataSourceType, database, tableName, sqlScriptStr, nameSpace.getNodeAttr());
        if (checkResult.isFail()) {
            // validate failure
            Messages.showMessageDialog("保存失败, 脚本校验不通过 : \n\n" + checkResult.getErrorMsg(), "", Icons.scaleToWidth(Icons.FAIL, 60));
            return false;
        }

        Result<SQLBatchExecuteResult> result = DatabaseDesignUtils.batchExecuteConsoleSQL(nameSpace, sqlScriptStr);
        if (!DatabaseDesignUtils.getExecuteResult(result, "保存失败, 脚本执行失败, 请检查脚本:")) return false;

        // 刷新数据库树并关闭窗口
        DataContext.getInstance(project).setMustSyncLoadDbTree(true);
        refreshDbTree();

        return true;
    }

    private void refreshDbTree() {
        DataContext dataContext = DataContext.getInstance(project);
        DatabaseNode databaseNode = dataContext.getSelectDatabase();
        MySQLTableNode selectDbTable = dataContext.getSelectDbTable();

        JTree dbTree = dataContext.getDbTree();
        CacheManager.refreshTemplate();

        DefaultMutableTreeNode node = isEdit() ? ObjectUtils.defaultIfNull(selectDbTable, databaseNode) : databaseNode;
        boolean expanded = dbTree.isExpanded(new TreePath(node));
        JTreeLoadingUtils.loading(false, dbTree, node, () -> {
//          // 由于表名/注释/字符集等信息也支持更新, 需要进行库级别刷新
            return DatabaseDesignUtils.queryMySQLTableNodes(databaseNode);
        }, nodes -> {
            JTreeLoadingUtils.reloadTree(dbTree, databaseNode, nodes);

            @SuppressWarnings("all") List<MySQLTableNode> tableNodes = (List<MySQLTableNode>) nodes;
            for (MySQLTableNode tableNode : tableNodes) {
                if (tableNode.getTableName().equals(tableName)) {
                    TreePath treePath = new TreePath(tableNode);
                    dbTree.setSelectionPath(treePath);
                    if (expanded) dbTree.expandPath(treePath);
                    break;
                }
            }
        });
    }

    private boolean invalidateFields() {
        java.util.List<String> errMsg = new ArrayList<>();
        if (StringUtils.isEmpty(tableName)) {
            errMsg.add("请填写表名");
        }
        if (StringUtils.isEmpty(tableComment)) {
            errMsg.add("请填写表描述");
        }
        // 字段加密禁止添加明文字段
        List<String> encryptFields = analyzeEncrypt(databaseConfigPanel.getRdbFields());
        for (RdbField rdbField : databaseConfigPanel.getRdbFields()) {
            // 字段加密禁止添加明文字段
            if (encryptFields.contains(rdbField.getFieldName())) {
                String encrypt = rdbField.getFieldName() + ENCRYPT_FIELD_SUFFIX;
                String md5x = rdbField.getFieldName() + MD5X_FIELD_SUFFIX;
                errMsg.add(String.format("%s 和 %s 为加密字段, 不允许存在明文字段  %s, 请删除后重试", encrypt, md5x, rdbField.getFieldName()));
            }

            // fieldEntity.getFieldType()
//            RdbFieldTypeConfig fieldTypeConfig = FieldTypeMatch.FIELD_TYPE_CONFIG.get(rdbField.getFieldType());
//            if (Objects.nonNull(fieldTypeConfig)) {
//                if (fieldTypeConfig.isNeedFieldLength()) {
//                    if (Objects.isNull(rdbField.getFieldLength())) {
//                        errMsg.add(String.format("字段 %s 未指定长度", rdbField.getFieldName()));
//                    }
//                }
//                if (fieldTypeConfig.isNeedFieldPrecision() && Objects.isNull(rdbField.getFieldPrecision())) {
//                    errMsg.add(String.format("字段 %s 未指定小数点位数", rdbField.getFieldName()));
//                }
//            }
            // 非空
            if (rdbField.isPk() && !rdbField.isNotNull()) {
                errMsg.add(String.format("字段 %s 为主键字段, 必须设置为非空字段", rdbField.getFieldName()));
            }
        }
        if (CollectionUtils.isNotEmpty(errMsg)) {
            Messages.showMessageDialog("校验失败, 请检查: \n\n" + String.join("\n\n", errMsg), "", Icons.scaleToWidth(Icons.FAIL, 60));
            return true;
        }

        return false;
    }

    private List<String> analyzeEncrypt(List<RdbField> fullColumn) {
        Map<String, RdbField> encryptColumnMap = new HashMap<>();
        Map<String, RdbField> md5xColumnMap = new HashMap<>();
        for (RdbField columnInfo : fullColumn) {
            String columnName = columnInfo.getFieldName();
            if (columnName.endsWith(ENCRYPT_FIELD_SUFFIX)) {
                encryptColumnMap.put(columnName.substring(0, columnName.lastIndexOf(ENCRYPT_FIELD_SUFFIX)), columnInfo);
            }
            if (columnInfo.getFieldName().endsWith(MD5X_FIELD_SUFFIX)) {
                md5xColumnMap.put(columnName.substring(0, columnName.lastIndexOf(MD5X_FIELD_SUFFIX)), columnInfo);
            }
        }

        List<String> encryptColumns = new ArrayList<>();
        for (String encryptColumnName : encryptColumnMap.keySet()) {
            if (md5xColumnMap.containsKey(encryptColumnName)) {
                encryptColumns.add(encryptColumnName);
            }
        }

        return encryptColumns;
    }

    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        initSqlPreview();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = JBUI.insets(5);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel tableNameLabel = new JLabel("表名:");
        tableNameLabel.setIcon(Icons.scaleToWidth(Icons.TABLE2, 16));
        formPanel.add(tableNameLabel, gbc);
        gbc.gridx++;
        JTextField tableNameTextField = new JTextField(20);
        if (StringUtils.isNotEmpty(tableName)) {
            tableNameTextField.setText(tableName);
        }

        tableNameTextField.getDocument().addDocumentListener(configureDocumentListener(tableNameTextField, SqlType.tableName.code));
        // 添加文本框输入监听器
        tableNameTextField.addKeyListener(KeyListener.inputCheck());
        formPanel.add(tableNameTextField, gbc);

        gbc.gridx++;
        formPanel.add(new JLabel(" 表描述:"), gbc);
        gbc.gridx++;

        JTextField tableCommentTextField = new JTextField(20);
        if (StringUtils.isNotEmpty(tableComment)) {
            tableCommentTextField.setText(tableComment);
        }

        tableCommentTextField.getDocument().addDocumentListener(configureDocumentListener(tableCommentTextField, SqlType.tableComment.code));
        formPanel.add(tableCommentTextField, gbc);


        gbc.gridx++;
        formPanel.add(new JLabel(" 字符集:"), gbc);

        gbc.gridx++;
        String[] collectionTypeOptions = {"utf8mb4", "utf8"};
        JComboBox<String> collectionComboBox = new ComboBox<>(collectionTypeOptions);
        collectionComboBox.setPreferredSize(new Dimension(100, collectionComboBox.getPreferredSize().height));
        if (StringUtils.isNotEmpty(this.tableCharacterSet)) {
            collectionComboBox.setSelectedItem(this.tableCharacterSet);
        }
        collectionComboBox.addActionListener(e -> {
            String collectionType = (String) collectionComboBox.getSelectedItem();
            if (StringUtils.isNotBlank(collectionType)) {
                updateSqlPreview(SqlType.charset.code, collectionType);
            }
        });
        formPanel.add(collectionComboBox, gbc);

        gbc.gridx++;
        JLabel engineLabel = new JLabel("引擎: ");
        engineLabel.setVisible(false);
        formPanel.add(engineLabel, gbc);

        gbc.gridx++;

        // 设置字段类型列为下拉框
        String[] engineTypeOptions = {"InnoDB"};
        JComboBox<String> engineComboBox = new ComboBox<>(engineTypeOptions);
        engineComboBox.setPreferredSize(new Dimension(245, collectionComboBox.getPreferredSize().height));

        engineComboBox.addActionListener(e -> {
            String collectionType = (String) engineComboBox.getSelectedItem();
            if (StringUtils.isNotBlank(collectionType)) {
                updateSqlPreview(SqlType.engine.code, collectionType);
            }
        });
        engineComboBox.setVisible(false);
        formPanel.add(engineComboBox, gbc);

        return formPanel;
    }

    protected DocumentListener configureDocumentListener(JTextField textField, String type) {
        return new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                // 处理文本变化的逻辑，这里简单地打印当前文本内容
                String text = textField.getText();
                updateSqlPreview(type, text);
            }
        };
    }

    protected void initSqlPreview() {
        if (isCopy()) {
            sqlPreviewMap.put(0, String.format("CREATE TABLE `%s`", tableName));
            sqlPreviewMap.put(999, String.format(" ENGINE = `%s` DEFAULT CHARSET = `%s` COMMENT = '%s';", tableEngine, tableCharacterSet, tableComment));
        } else if (isCreate()) {
            sqlPreviewMap.put(0, "CREATE TABLE ``");
            sqlPreviewMap.put(999, " ENGINE = `InnoDB` DEFAULT CHARSET = `utf8mb4` COMMENT = ''; ");
        }
    }

    protected void updateSqlPreview(String type, String textFieldValue) {
        switch (type) {
            case "tableName":
                tableName = textFieldValue;
                updateSqlPreview();
                break;
            case "engine":
                tableEngine = textFieldValue;
                updateSqlPreview();
            case "tableComment":
                tableComment = DatabasePreviewPanel.escapeString(textFieldValue);
                updateSqlPreview();
                break;
            case "charset":
                tableCharacterSet = textFieldValue;
                updateSqlPreview();
                break;
            default:
                break;
        }
    }

    protected void updateSqlPreview() {
        if (isEdit()) {
            StringBuilder sqlPreviewText = new StringBuilder();
            DataContext dataContext = DataContext.getInstance(project);
            buildModifyTableComments(dataContext.getSelectDbTable(), sqlPreviewText);
            buildModifyTableChartSet(dataContext.getSelectDbTable(), sqlPreviewText);
            buildModifyTableName(dataContext.getSelectDbTable(), sqlPreviewText);

            sqlPreviewMap.put(999, sqlPreviewText.toString());
        } else {
            sqlPreviewMap.put(0, String.format("CREATE TABLE `%s`", tableName));
            sqlPreviewMap.put(999, String.format(" ENGINE = `%s` DEFAULT CHARSET = `%s` COMMENT = '%s';", tableEngine, tableCharacterSet, tableComment));
        }
        databasePreviewPanel.updateSqlPreviewText();
    }

    private void buildModifyTableName(MySQLTableNode selectDbTable, StringBuilder sqlPreviewText) {
        // ALTER TABLE YZH_TEST_11 RENAME TO YZH_TEST_12;
        String org = selectDbTable.getTableName();
        if (compareObjModify(org, this.tableName)) {
            sqlPreviewText.append("ALTER TABLE ").append(selectDbTable.getTableName()).append(" RENAME TO ").append(this.tableName).append(";\n");
        }
    }

    private void buildModifyTableChartSet(MySQLTableNode selectDbTable, StringBuilder sqlPreviewText) {
        // ALTER TABLE yzh_test_11 CHARACTER SET = utf8;
        String org = selectDbTable.getCharset();
        if (compareObjModify(org, this.tableCharacterSet)) {
            sqlPreviewText.append("ALTER TABLE ").append(selectDbTable.getTableName()).append(" CHARACTER SET = ").append(this.tableCharacterSet).append(";\n");
        }
    }

    private void buildModifyTableComments(MySQLTableNode selectDbTable, StringBuilder sqlPreviewText) {
        // alter table test1 comment '修改后的表的注释';
        String org = selectDbTable.getTableComment();
        if (compareObjModify(org, this.tableComment)) {
            sqlPreviewText.append("ALTER TABLE ").append(selectDbTable.getTableName()).append(" COMMENT ").append("'").append(this.tableComment).append("';\n");
        }
    }

    private boolean compareObjModify(Object org, Object target) {
        if (Objects.nonNull(org)) {
            return !org.equals(target);
        } else return Objects.nonNull(target);
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    enum SqlType {
        /**
         * 表格名称
         */
        tableName("tableName"), tableComment("tableComment"), engine("engine"), charset("charset");
        private final String code;
    }

}
