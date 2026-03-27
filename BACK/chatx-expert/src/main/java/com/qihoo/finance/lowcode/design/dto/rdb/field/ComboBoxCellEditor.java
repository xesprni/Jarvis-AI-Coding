package com.qihoo.finance.lowcode.design.dto.rdb.field;

import com.qihoo.finance.lowcode.design.constant.FieldType;
import com.qihoo.finance.lowcode.design.dialog.DatabasePreviewPanel;
import com.qihoo.finance.lowcode.design.dto.rdb.RdbField;
import com.qihoo.finance.lowcode.design.dto.RowNumberTableModel;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.Component;
import java.util.Optional;

public class ComboBoxCellEditor extends AbstractCellEditor implements TableCellEditor {

    private final JComboBox<String> comboBox;
    private final FieldType fieldType;
    private final DatabasePreviewPanel dbPreviewPanel;
    private RdbField rdbField;


    public ComboBoxCellEditor(JComboBox<String> comboBox, FieldType fieldType, DatabasePreviewPanel dbPreviewPanel) {
        this.fieldType = fieldType;
        this.comboBox = comboBox;
        this.dbPreviewPanel = dbPreviewPanel;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        this.comboBox.setSelectedItem(Optional.ofNullable(value).map(Object::toString).orElse(null));
        this.rdbField = ((RowNumberTableModel)table.getModel()).getRowData(row);
        return comboBox;
    }

    @Override
    public Object getCellEditorValue() {
        ComboBoxEditor editor = comboBox.getEditor();
        String inputText = Optional.ofNullable(editor.getItem()).map(Object::toString).orElse(null);
        if (inputText != null) {
            if (fieldType == FieldType.fieldType) {
                // 从编辑器组件中获取用户输入的文本
                rdbField.setFieldType(inputText);
            } else if (fieldType == FieldType.fieldDefaults) {
                rdbField.setFieldDefault(inputText);
            }
            dbPreviewPanel.updateSqlPreviewText();

        }
        return inputText;
    }
}