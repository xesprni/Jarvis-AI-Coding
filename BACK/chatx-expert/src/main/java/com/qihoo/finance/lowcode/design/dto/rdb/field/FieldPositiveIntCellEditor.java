package com.qihoo.finance.lowcode.design.dto.rdb.field;

import com.qihoo.finance.lowcode.design.constant.FieldType;
import com.qihoo.finance.lowcode.design.dialog.DatabasePreviewPanel;
import com.qihoo.finance.lowcode.design.dto.RowNumberTableModel;
import com.qihoo.finance.lowcode.design.dto.rdb.RdbField;
import com.qihoo.finance.lowcode.design.dto.rdb.PositiveIntCellEditor;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.Component;
import java.util.Optional;

public class FieldPositiveIntCellEditor extends PositiveIntCellEditor {

    private final JTextField textField;
    private final FieldType fieldType;
    private final DatabasePreviewPanel dbPreviewPanel;
    private RdbField rdbField;


    public FieldPositiveIntCellEditor(FieldType fieldType, DatabasePreviewPanel dbPreviewPanel) {
        super();
        this.fieldType = fieldType;
        this.textField = (JTextField)getComponent();
        this.dbPreviewPanel = dbPreviewPanel;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        this.textField.setText(Optional.ofNullable(value).map(String::valueOf).orElse(null));
        this.rdbField = ((RowNumberTableModel)table.getModel()).getRowData(row);
        return super.getTableCellEditorComponent(table, value, isSelected, row, column);
    }

    @Override
    public Object getCellEditorValue() {
        if (fieldType == FieldType.fieldLength) {
            String value = this.textField.getText();
            if (StringUtils.isNotBlank(value)) {
                rdbField.setFieldLength(Integer.parseInt(value));
            }
        } else if (fieldType == FieldType.fieldPrecision) {
            String value = this.textField.getText();
            if (StringUtils.isNotBlank(value)) {
                rdbField.setFieldPrecision(Integer.parseInt(value));
            }
        }
        dbPreviewPanel.updateSqlPreviewText();
        return this.textField.getText();
    }

    static class PositiveIntegerDocument extends javax.swing.text.PlainDocument {
        @Override
        public void insertString(int offs, String str, javax.swing.text.AttributeSet a)
                throws javax.swing.text.BadLocationException {
            if (str == null) {
                return;
            }
            try {
                int value = Integer.parseInt(str);
                if (value >= 0) {
                    super.insertString(offs, str, a);
                }
            } catch (NumberFormatException e) {
                // Ignore non-integer input
            }
        }
    }
}
