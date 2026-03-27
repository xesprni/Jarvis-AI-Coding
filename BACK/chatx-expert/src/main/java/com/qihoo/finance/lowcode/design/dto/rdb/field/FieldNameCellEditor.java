package com.qihoo.finance.lowcode.design.dto.rdb.field;

import com.qihoo.finance.lowcode.common.listener.KeyListener;
import com.qihoo.finance.lowcode.design.constant.FieldType;
import com.qihoo.finance.lowcode.design.dialog.DatabasePreviewPanel;
import com.qihoo.finance.lowcode.design.dto.RowNumberTableModel;
import com.qihoo.finance.lowcode.design.dto.rdb.RdbField;
import org.apache.commons.lang3.StringUtils;

import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.Component;
import java.util.Optional;

public class FieldNameCellEditor extends DefaultCellEditor {
    private final JTextField textField;
    private final FieldType fieldType;
    private final DatabasePreviewPanel dbPreviewPanel;
    private RdbField rdbField;
    private JTable table;

    public FieldNameCellEditor(FieldType fieldType, DatabasePreviewPanel databasePreviewPanel) {
        super(new JTextField());
        textField = (JTextField) getComponent();
        textField.setHorizontalAlignment(JTextField.CENTER);
        this.fieldType = fieldType;
        if (FieldType.fieldName == fieldType) {
            textField.addKeyListener(KeyListener.inputCheck());
        }
        this.dbPreviewPanel = databasePreviewPanel;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        textField.setText(Optional.ofNullable(value).map(String::valueOf).orElse(null));
        this.table = table;
        this.rdbField = ((RowNumberTableModel) table.getModel()).getRowData(row);
        return textField;
    }

    @Override
    public Object getCellEditorValue() {
        if (fieldType == FieldType.fieldName) {
            rdbField.setFieldName(textField.getText());
            RowNumberTableModel model = (RowNumberTableModel) table.getModel();
            int editingRow = table.getEditingRow();
            if (editingRow + 1 < model.getRowCount()) {
                RdbField nextRdbField = model.getRowData(editingRow + 1);
                if (StringUtils.isNotBlank(nextRdbField.getFieldOrder())) {
                    nextRdbField.setFieldOrder("AFTER " + rdbField.getFieldName());
                }
            }
        } else if (fieldType == FieldType.fieldLength) {
            String length = textField.getText();
            if (StringUtils.isNotBlank(length)) {
                rdbField.setFieldLength(Integer.valueOf(textField.getText()));
            }
        } else if (fieldType == FieldType.fieldPrecision) {
            String fieldPrecision = textField.getText();
            if (StringUtils.isNotBlank(fieldPrecision)) {
                rdbField.setFieldPrecision(Integer.valueOf(textField.getText()));
            }
        } else if (fieldType == FieldType.fieldComment) {
            rdbField.setFieldComment(textField.getText());
        }
        dbPreviewPanel.updateSqlPreviewText();
        return textField.getText();
    }
}
