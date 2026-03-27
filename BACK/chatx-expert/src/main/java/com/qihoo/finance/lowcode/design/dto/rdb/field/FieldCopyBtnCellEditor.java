package com.qihoo.finance.lowcode.design.dto.rdb.field;

import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.design.dialog.DatabaseConfigPanel;
import com.qihoo.finance.lowcode.design.dialog.DatabasePreviewPanel;
import com.qihoo.finance.lowcode.design.dto.RowNumberTableModel;
import com.qihoo.finance.lowcode.design.dto.rdb.RdbField;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * FieldBtnCellEditor
 *
 * @author fengjinfu-jk
 * date 2023/10/23
 * @version 1.0.0
 * @apiNote FieldBtnCellEditor
 */
public class FieldCopyBtnCellEditor extends DefaultCellEditor {
    private final DatabasePreviewPanel dbPreviewPanel;

    public FieldCopyBtnCellEditor(DatabasePreviewPanel dbPreviewPanel) {
        super(new JTextField());
        this.setClickCountToStart(1);

        this.dbPreviewPanel = dbPreviewPanel;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        String fieldName = String.valueOf(getColumnVal(table, row, 1, StringUtils.EMPTY));
        if (Constants.DB_COLUMN.BASE_ENTITY.containsKey(fieldName) ||
                Constants.DB_COLUMN.DELETED_AT.containsKey(fieldName)) {
            return null;
        }

        Object fieldPrecision = getColumnVal(table, row, 4, null);

        RowNumberTableModel model = (RowNumberTableModel) table.getModel();
        RdbField rdbField = RdbField.builder()
                .fieldName(getColumnVal(table, row, 1, StringUtils.EMPTY) + "_copy")
                .fieldType(String.valueOf(getColumnVal(table, row, 2, "varchar")))
                .fieldLength(Integer.parseInt(getColumnVal(table, row, 3, 0).toString()))
                .fieldPrecision(Objects.nonNull(fieldPrecision) ? Integer.parseInt(fieldPrecision.toString()) : null)
                .notNull(Boolean.parseBoolean(getColumnVal(table, row, 5, false).toString()))
                .pk(Boolean.parseBoolean(getColumnVal(table, row, 6, false).toString()))
                .autoIncr(Boolean.parseBoolean(getColumnVal(table, row, 7, false).toString()))
                .unsigned(Boolean.parseBoolean(getColumnVal(table, row, 8, false).toString()))
                .fieldDefault(getColumnVal(table, row, 9, StringUtils.EMPTY))
                .fieldComment(String.valueOf(getColumnVal(table, row, 10, StringUtils.EMPTY)))
                .build();

        Object[] rowData = new Object[]{rdbField.getFieldName(), rdbField.getFieldType(), rdbField.getFieldLength()
                , rdbField.getFieldPrecision(), rdbField.isNotNull(), rdbField.isPk(), rdbField.isAutoIncr()
                , rdbField.isUnsigned(), rdbField.getFieldDefault(), rdbField.getFieldComment()};

        String fieldOrderStr = "FIRST";
        if (row > 0) {
            fieldOrderStr = "AFTER " + (String) model.getValueAt(row, DatabaseConfigPanel.COLUMN_NAME_INDEX);
        }
        rdbField.setFieldOrder(fieldOrderStr);

        model.insertRow(row + 1, rowData, rdbField);
        dbPreviewPanel.updateSqlPreviewText();

        return null;
    }

    private Object getColumnVal(JTable table, int row, int column, Object defaultVal) {
        Object valueAt = table.getValueAt(row, column);
        if (Objects.nonNull(valueAt)) {
            return valueAt;
        }

        return defaultVal;
    }
}
