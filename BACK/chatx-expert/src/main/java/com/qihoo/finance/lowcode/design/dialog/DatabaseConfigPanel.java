package com.qihoo.finance.lowcode.design.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.design.constant.FieldType;
import com.qihoo.finance.lowcode.design.constant.FieldTypeMatch;
import com.qihoo.finance.lowcode.design.dto.RowNumberTableModel;
import com.qihoo.finance.lowcode.design.dto.rdb.RdbField;
import com.qihoo.finance.lowcode.design.dto.rdb.field.*;
import com.qihoo.finance.lowcode.design.entity.DatabaseColumnNode;
import com.qihoo.finance.lowcode.design.listener.DragDropRowTableUI;
import com.qihoo.finance.lowcode.design.util.JTableUtil;
import com.qihoo.finance.lowcode.gentracker.tool.JSON;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.plaf.TableUI;
import javax.swing.table.TableCellRenderer;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author weiyichao
 * @date 2023-07-31
 **/
@Getter
public class DatabaseConfigPanel extends DatabaseTableBaseDialog {

    private List<RdbField> rdbFields;
    private List<RdbField> droppedRdbFields;
    private final DatabasePreviewPanel databasePreviewPanel;

    private final String[] fieldDefaultOptions = {
            Constants.DB_COLUMN.DEFAULT_NULL,
            Constants.DB_COLUMN.EMPTY_STRING,
            Constants.DB_COLUMN.CURRENT_TIMESTAMP,
            Constants.DB_COLUMN.UPDATE_TIMESTAMP
    };

    public final static int COLUMN_NAME_INDEX = 1;
    private final static Set<String> TAIL_COLUMNS = new HashSet<>(Arrays.asList("date_created", "created_by"
            , "date_updated", "updated_by", "deleted_at"));


    public DatabaseConfigPanel(@NotNull Project project) {
        super(project);
        databasePreviewPanel = project.getService(DatabasePreviewPanel.class);
        addEncryptField.setVisible(true);
        addDeletedAt.setVisible(true);
        tipsLabel.setVisible(true);
        tipsLabel2.setVisible(true);
        addDeletedAt.addActionListener(e -> addDeletedAtRow());
        addEncryptField.addActionListener(e -> addEncrypt());
    }

    private void addEncrypt() {
        String encryptField = Messages.showInputDialog(project, "数据库加密字段必须以_encryptx，"
                + "_md5x结尾，Entity字段必须以Encryptx，Md5x结尾\n详见：灵犀文档站点-MSF框架-使用指南-加密脱敏"
                + "-Mysql加密使用手册\n\n" +
                        "请输入原字段名称\n点击确认后将拆成_encryptx字段和_md5x字段\n\n ",
                "添加加密字段", Icons.scaleToWidth(Icons.WARNING, 16));
        if (StringUtils.isBlank(encryptField)) return;

        // encrypt
        RdbField encrypt = RdbField.builder().fieldName(encryptField + Constants.Encrypt.ENCRYPT_FIELD_SUFFIX)
                .fieldType("varchar").fieldLength(64)
                .fieldPrecision(0).pk(false).notNull(true).autoIncr(false).unsigned(false)
                .fieldDefault(Constants.DB_COLUMN.EMPTY_STRING).fieldComment(encryptField + " 密文").build();
        // md5
        RdbField md5 = RdbField.builder().fieldName(encryptField + Constants.Encrypt.MD5X_FIELD_SUFFIX)
                .fieldType("varchar").fieldLength(32)
                .fieldPrecision(0).pk(false).notNull(true).autoIncr(false).unsigned(false)
                .fieldDefault(Constants.DB_COLUMN.EMPTY_STRING).fieldComment(encryptField + " md5值").build();

        addFieldRow(getTable(), md5);
        addFieldRow(getTable(), encrypt);
        databasePreviewPanel.updateSqlPreviewText();
    }

    private void addDeletedAtRow() {
        RowNumberTableModel model = (RowNumberTableModel) getTable().getModel();
        if (model.getRowCount() > 0) {
            String columnName = (String) model.getValueAt(model.getRowCount() - 1, COLUMN_NAME_INDEX);
            if ("deleted_at".equals(columnName)) {
                // 已经有该字段了，不继续添加
                return;
            }
        }

        RdbField rdbField = RdbField.builder().fieldName("deleted_at").fieldType("bigint").fieldLength(20)
                .fieldPrecision(0).pk(false).notNull(true).autoIncr(false).unsigned(false)
                .fieldDefault(Constants.DB_COLUMN.DEFAULT_ZERO).fieldComment("软删除标识").build();
        Object[] deleted_at = new Object[]{rdbField.getFieldName(), rdbField.getFieldType(), rdbField.getFieldLength()
                , rdbField.getFieldPrecision(), rdbField.isNotNull(), rdbField.isPk(), rdbField.isAutoIncr()
                , rdbField.isUnsigned(), rdbField.getFieldDefault(), rdbField.getFieldComment()};
        model.addRow(deleted_at, rdbField);
        databasePreviewPanel.updateSqlPreviewText();
    }


    /**
     * 由于Service类型插件对构造函数的限制，本类自己的成员变量初始化交给init方法完成
     *
     * @param rdbFields
     * @param droppedRdbFields
     */
    public void init(List<RdbField> rdbFields, List<RdbField> droppedRdbFields) {
        this.rdbFields = rdbFields;
        this.droppedRdbFields = droppedRdbFields;
    }

    @Override
    public TableUI getTableUI() {
        DragDropRowTableUI tableUI = (DragDropRowTableUI) super.getTableUI();
        tableUI.setDragDropConfig(((model, fromRow, toRow) -> {
            if (toRow == 0 || fromRow == 0) {
                return false;
            }
            String fromColumnName = (String) model.getValueAt(fromRow, COLUMN_NAME_INDEX);
            String toColumnName = (String) model.getValueAt(toRow, COLUMN_NAME_INDEX);
            if (TAIL_COLUMNS.contains(fromColumnName)) {
                return false;
            } else if (TAIL_COLUMNS.contains(toColumnName)) {
                // 拖拽的列不是tailColumn, 拖拽的目标位置是tailColumn，必须是在tailColumn下方的列才允许拖动
                return fromRow > toRow;
            }
            return true;
        }));
        return tableUI;
    }

    @Override
    protected void addHandler(ActionEvent e, JTable table) {
        addFieldRow(table, initRdbField());
    }

    private void addFieldRow(JTable table, RdbField rdbField) {
        RowNumberTableModel model = (RowNumberTableModel) table.getModel();
        int rowIndex = -1;
        int pos = model.getRowCount() - 1;
        int endIndex = Math.min(model.getRowCount() - TAIL_COLUMNS.size(), 0);

        while (pos >= endIndex) {
            String columnName = (String) model.getValueAt(pos, COLUMN_NAME_INDEX);
            if (TAIL_COLUMNS.contains(columnName)) {
                rowIndex = pos;
            }
            pos--;
        }
        // 添加一行数据
        Object[] row = new Object[]{rdbField.getFieldName(), rdbField.getFieldType(), rdbField.getFieldLength()
                , rdbField.getFieldPrecision(), rdbField.isNotNull(), rdbField.isPk(), rdbField.isAutoIncr()
                , rdbField.isUnsigned(), rdbField.getFieldDefault(), rdbField.getFieldComment()};

        // 如果存在选中行, 则尝试在选中行下插入新行, 否则在非常规字段的最底下插入新行
        int selectedRow = table.getSelectedRow() + 1;
        rowIndex = (selectedRow > 0 && selectedRow < rowIndex) ? selectedRow : rowIndex;
        if (rowIndex >= 0) {
            String fieldOrderStr = "FIRST";
            if (rowIndex > 0) {
                fieldOrderStr = "AFTER " + (String) model.getValueAt(rowIndex - 1, COLUMN_NAME_INDEX);
            }
            rdbField.setFieldOrder(fieldOrderStr);
            model.insertRow(rowIndex, row, rdbField);
        } else {
            model.addRow(row, rdbField);
        }

        databasePreviewPanel.updateSqlPreviewText();
    }

    @Override
    protected Object[][] getEditTableData() {
        List<DatabaseColumnNode> dbTableColumns = DataContext.getInstance(project).getSelectDbTable().getTableColumns();
        Object[][] data = new Object[dbTableColumns.size()][getColumnNames().length];
        for (int i = 0; i < dbTableColumns.size(); i++) {
            DatabaseColumnNode dbColumn = dbTableColumns.get(i);
            String simpleFieldType = StringUtils.defaultString(dbColumn.getFieldType().split("\\(")[0]);
            simpleFieldType = simpleFieldType.replace(" unsigned", "");
            RdbField rdbField = RdbField.builder()
                    .fieldName(dbColumn.getFieldName())
                    .fieldType(simpleFieldType)
                    .fieldLength(dbColumn.getFieldLength())
                    .fieldPrecision(dbColumn.getFieldPrecision())
                    .pk(dbColumn.isPK()).notNull(dbColumn.isNotNull())
                    .autoIncr(dbColumn.isAutoIncr())
                    .unsigned(dbColumn.isUnsigned())
                    .fieldDefault(dbColumn.getFieldDefaults())
                    .fieldComment(dbColumn.getFieldComment())
                    .build();
            if ("".equals(rdbField.getFieldDefault())) {
                rdbField.setFieldDefault(Constants.DB_COLUMN.EMPTY_STRING);
            } else if (null == rdbField.getFieldDefault()) {
                rdbField.setFieldDefault(Constants.DB_COLUMN.DEFAULT_NULL);
            }
            rdbField.setExistsInDb(true);
            rdbField.setDbColumnBackup(JSON.toJson(rdbField));
            data[i] = new Object[]{rdbField.getFieldName(), rdbField.getFieldType(), rdbField.getFieldLength()
                    , rdbField.getFieldPrecision(), rdbField.isNotNull(), rdbField.isPk(), rdbField.isAutoIncr()
                    , rdbField.isUnsigned(), rdbField.getFieldDefault(), rdbField.getFieldComment()};
            rdbFields.add(rdbField);
        }
        return data;
    }

    @Override
    protected List getRowDatas() {
        return rdbFields;
    }

    @Override
    protected Object[][] getDefaultTableData() {
        // inner db column
//        return new String[]{"字段名称", "字段类型", "字段长度", "小数点", "不是NULL", "是否主键", "是否自增", "无符号", "默认值", "字段备注"};
        RdbField idField = RdbField.builder().fieldName("id").fieldType("bigint").fieldLength(20).fieldPrecision(0).pk(true).notNull(true).autoIncr(true).unsigned(true).fieldDefault(Constants.DB_COLUMN.DEFAULT_NULL).fieldComment("主键").build();
        Object[] id = new Object[]{idField.getFieldName(), idField.getFieldType(), idField.getFieldLength(), idField.getFieldPrecision(), idField.isNotNull(), idField.isPk(), idField.isAutoIncr(), idField.isUnsigned(), idField.getFieldDefault(), idField.getFieldComment()};
        rdbFields.add(idField);
        RdbField dataCreated = RdbField.builder().fieldName("date_created").fieldType("datetime").fieldLength(null).fieldPrecision(null).pk(false).notNull(true).autoIncr(false).unsigned(false).fieldDefault(Constants.DB_COLUMN.CURRENT_TIMESTAMP).fieldComment("创建时间").build();
        Object[] date_created = new Object[]{dataCreated.getFieldName(), dataCreated.getFieldType(), dataCreated.getFieldLength(), dataCreated.getFieldPrecision(), dataCreated.isNotNull(), dataCreated.isPk(), dataCreated.isAutoIncr(), dataCreated.isUnsigned(), dataCreated.getFieldDefault(), dataCreated.getFieldComment()};
        rdbFields.add(dataCreated);
        RdbField createdBy = RdbField.builder().fieldName("created_by").fieldType("varchar").fieldLength(32).fieldPrecision(null).pk(false).notNull(true).autoIncr(false).unsigned(false).fieldDefault(Constants.DB_COLUMN.EMPTY_STRING).fieldComment("创建人").build();
        Object[] created_by = new Object[]{createdBy.getFieldName(), createdBy.getFieldType(), createdBy.getFieldLength(), createdBy.getFieldPrecision(), createdBy.isNotNull(), createdBy.isPk(), createdBy.isAutoIncr(), createdBy.isUnsigned(), createdBy.getFieldDefault(), createdBy.getFieldComment()};
        rdbFields.add(createdBy);
        RdbField dateUpdated = RdbField.builder().fieldName("date_updated").fieldType("datetime").fieldLength(null).fieldPrecision(null).pk(false).notNull(true).autoIncr(false).unsigned(false).fieldDefault(Constants.DB_COLUMN.UPDATE_TIMESTAMP).fieldComment("修改时间").build();
        Object[] date_updated = new Object[]{dateUpdated.getFieldName(), dateUpdated.getFieldType(), dateUpdated.getFieldLength(), dateUpdated.getFieldPrecision(), dateUpdated.isNotNull(), dateUpdated.isPk(), dateUpdated.isAutoIncr(), dateUpdated.isUnsigned(), dateUpdated.getFieldDefault(), dateUpdated.getFieldComment()};
        rdbFields.add(dateUpdated);
        RdbField updatedBy = RdbField.builder().fieldName("updated_by").fieldType("varchar").fieldLength(32).fieldPrecision(null).pk(false).notNull(true).autoIncr(false).unsigned(false).fieldDefault(Constants.DB_COLUMN.EMPTY_STRING).fieldComment("修改人").build();
        Object[] updated_by = new Object[]{updatedBy.getFieldName(), updatedBy.getFieldType(), updatedBy.getFieldLength(), updatedBy.getFieldPrecision(), updatedBy.isNotNull(), updatedBy.isPk(), updatedBy.isAutoIncr(), updatedBy.isUnsigned(), updatedBy.getFieldDefault(), updatedBy.getFieldComment()};
        rdbFields.add(updatedBy);

        return new Object[][]{id, date_created, created_by, date_updated, updated_by};
    }

    @Override
    protected String[] getColumnNames() {
        return new String[]{"字段名称", "字段类型", "字段长度", "小数点", "不是NULL", "是否主键", "是否自增", "无符号", "默认值", "字段备注", ""};
    }

    @Override
    protected void configColumnProperties(JTable table, TableCellRenderer centerRenderer) {
        TableCellRenderer leftAlignRender = JTableUtil.getLeftAlignRender();
        table.getColumnModel().getColumn(1).setCellRenderer(leftAlignRender);
        table.getColumnModel().getColumn(1).setCellEditor(new FieldNameCellEditor(FieldType.fieldName
                , databasePreviewPanel));

        // 设置字段类型列为下拉框
        //String[] fieldTypeOptions = {"bigint", "binary", "bit", "blob", "char", "date", "datetime", "decimal", "double", "enum", "float", "geometry", "geometrycollection", "int", "integer", "linestring", "longblob", "longtext", "mediumblob", "mediumint", "mediumtext", "multilinestring", "multipoint", "multipolygon", "numeric", "point", "polygon", "real", "set", "smallint", "tinyblob", "tinyint", "varbinary", "year", "tinytext", "varchar", "text", "timestamp"};
        String[] fieldTypeOptions = FieldTypeMatch.getFieldTypes().toArray(new String[0]);
        JComboBox<String> fieldTypeComboBox = new ComboBox<>(fieldTypeOptions);
        fieldTypeComboBox.setEditable(true);
        AutoCompleteDecorator.decorate(fieldTypeComboBox);
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
//        table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(fieldTypeComboBox));
        table.getColumnModel().getColumn(2).setCellEditor(new ComboBoxCellEditor(fieldTypeComboBox
                , FieldType.fieldType, databasePreviewPanel));

        // 设置字段长度列只允许输入正整数
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(3).setCellEditor(new FieldPositiveIntCellEditor(
                FieldType.fieldLength, databasePreviewPanel));

        table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(4).setCellEditor(new FieldPositiveIntCellEditor(
                FieldType.fieldPrecision, databasePreviewPanel));

        // 设置是否为空为复选框
        table.getColumnModel().getColumn(5).setCellRenderer(new CheckBoxRenderer());
        table.getColumnModel().getColumn(5).setCellEditor(new CheckBoxEditor(FieldType.isNotNull
                , databasePreviewPanel));

        // 设置主键列为复选框
        table.getColumnModel().getColumn(6).setCellRenderer(new CheckBoxRenderer());
        table.getColumnModel().getColumn(6).setCellEditor(new CheckBoxEditor(FieldType.isPK
                , databasePreviewPanel));

        // 设置主键列为复选框
        table.getColumnModel().getColumn(7).setCellRenderer(new CheckBoxRenderer());
        table.getColumnModel().getColumn(7).setCellEditor(new CheckBoxEditor(FieldType.isAutoIncr
                , databasePreviewPanel));

        // 设置主键列为复选框
        table.getColumnModel().getColumn(8).setCellRenderer(new CheckBoxRenderer());
        table.getColumnModel().getColumn(8).setCellEditor(new CheckBoxEditor(FieldType.isUnsigned
                , databasePreviewPanel));

        JComboBox<String> fieldDefaultComboBox = new ComboBox<>(fieldDefaultOptions);
        fieldDefaultComboBox.setEditable(true);
        table.getColumnModel().getColumn(9).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(9).setCellEditor(new ComboBoxCellEditor(fieldDefaultComboBox
                , FieldType.fieldDefaults, databasePreviewPanel));


        table.getColumnModel().getColumn(10).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(10).setCellEditor(new FieldNameCellEditor(FieldType.fieldComment
                , databasePreviewPanel));

        table.getColumnModel().getColumn(11).setCellRenderer(new FieldCopyBtnRenderer());
        table.getColumnModel().getColumn(11).setCellEditor(new FieldCopyBtnCellEditor(databasePreviewPanel));
        table.getColumnModel().getColumn(11).setPreferredWidth(50);
        table.getColumnModel().getColumn(11).setMaxWidth(50);
        table.getColumnModel().getColumn(11).setMinWidth(50);
    }

    @Override
    public void rowOrderChanged(RowNumberTableModel model, int from, int to) {
        // 处理 toField(主动修改顺序的字段)以及它附近的字段order
        RdbField toField = model.getRowData(to);
        if (to == 0) {
            toField.setFieldOrder("FIRST");
            RdbField nextField = model.getRowData(to + 1);
            if (StringUtils.isNotBlank(nextField.getFieldOrder())) {
                nextField.setFieldOrder("AFTER " + toField.getFieldName());
            }
        } else {
            RdbField prevField = model.getRowData(to - 1);
            toField.setFieldOrder("AFTER " + prevField.getFieldName());
        }
        if (to + 1 < model.getRowCount()) {
            RdbField toFieldNext = model.getRowData(to + 1);
            if (StringUtils.isNotBlank(toFieldNext.getFieldOrder())) {
                toFieldNext.setFieldOrder("AFTER " + toField.getFieldName());
            }
        }
        // 处理fromField(被动修改顺序的字段)以及它附近的字段order
        RdbField fromField = model.getRowData(from);
        if (StringUtils.isNotBlank(fromField.getFieldOrder())) {
            if (from == 0) {
                fromField.setFieldOrder("FIRST");
            } else {
                RdbField prevField = model.getRowData(from - 1);
                fromField.setFieldOrder("AFTER " + prevField.getFieldName());
            }
        }
        if (from + 1 < model.getRowCount()) {
            RdbField fromFieldNext = model.getRowData(from + 1);
            if (StringUtils.isNotBlank(fromFieldNext.getFieldOrder())) {
                fromFieldNext.setFieldOrder("AFTER " + fromField.getFieldName());
            }
        }
        databasePreviewPanel.updateSqlPreviewText();
    }

    /**
     * 删除按钮逻辑实现-（操作表格）
     *
     * @param e 参数
     */
    @Override
    protected void deleteHandler(ActionEvent e, JTable table) {
        RowNumberTableModel model = (RowNumberTableModel) table.getModel();
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            //TODO 应该给一个提示先选中一行，再点击删除
            return;
        }
        RdbField rdbField = model.getRowData(selectedRow);
        if (rdbField != null) {
            if (isEdit() && rdbField.isExistsInDb()) {
                droppedRdbFields.add(rdbField);
                // 修改被删除行下一行的排序字段
                if (selectedRow < model.getRowCount() - 1) {
                    RdbField nextRdbField = model.getRowData(selectedRow + 1);
                    if (StringUtils.isNotBlank(nextRdbField.getFieldOrder())) {
                        if (selectedRow >= 1) {
                            String preColumnName = (String) model.getValueAt(selectedRow - 1, COLUMN_NAME_INDEX);
                            nextRdbField.setFieldOrder("AFTER " + preColumnName);
                        } else {
                            nextRdbField.setFieldOrder("FIRST");
                        }
                    }
                }

            }
        }
        model.removeRow(selectedRow);
        databasePreviewPanel.updateSqlPreviewText();
    }

    protected RdbField initRdbField() {
        return RdbField.builder().fieldName(StringUtils.EMPTY).fieldType("varchar").fieldLength(32)
                .fieldPrecision(null).pk(false).notNull(true).fieldDefault(StringUtils.EMPTY).autoIncr(false)
                .unsigned(false).fieldComment(StringUtils.EMPTY).build();
    }

}
