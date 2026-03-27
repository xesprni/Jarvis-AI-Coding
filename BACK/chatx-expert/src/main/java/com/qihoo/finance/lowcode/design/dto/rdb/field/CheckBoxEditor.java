package com.qihoo.finance.lowcode.design.dto.rdb.field;

import com.qihoo.finance.lowcode.design.constant.FieldType;
import com.qihoo.finance.lowcode.design.dialog.DatabasePreviewPanel;
import com.qihoo.finance.lowcode.design.dto.rdb.RdbField;
import com.qihoo.finance.lowcode.design.dto.RowNumberTableModel;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import java.awt.Component;

public class CheckBoxEditor extends DefaultCellEditor {

    private final FieldType fieldType;
    private final DatabasePreviewPanel dbPreviewPanel;
    private final JCheckBox checkBox;
    private RdbField rdbField;

    public CheckBoxEditor(FieldType fieldType, DatabasePreviewPanel dbPreviewPanel) {
        super(new JCheckBox());
        this.checkBox = (JCheckBox)getComponent();
        checkBox.setHorizontalAlignment(JCheckBox.CENTER);
        this.fieldType = fieldType;
        this.dbPreviewPanel = dbPreviewPanel;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        checkBox.setSelected((Boolean) value);
        this.rdbField = ((RowNumberTableModel)table.getModel()).getRowData(row);
        return checkBox;
    }

    @Override
    public Object getCellEditorValue() {
        if (fieldType == FieldType.isPK) {
            boolean value = this.checkBox.isSelected();
            rdbField.setPk(value);
        } else if (fieldType == FieldType.isNotNull) {
            boolean value = this.checkBox.isSelected();
            rdbField.setNotNull(value);
        } else if (fieldType == FieldType.isAutoIncr) {
            boolean value = this.checkBox.isSelected();
            rdbField.setAutoIncr(value);
        } else if (fieldType == FieldType.isUnsigned) {
            boolean value = this.checkBox.isSelected();
            rdbField.setUnsigned(value);
        }
        dbPreviewPanel.updateSqlPreviewText();
        return checkBox.isSelected();
    }
}
