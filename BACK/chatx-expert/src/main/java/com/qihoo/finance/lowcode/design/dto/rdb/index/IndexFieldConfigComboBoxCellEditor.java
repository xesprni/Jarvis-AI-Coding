package com.qihoo.finance.lowcode.design.dto.rdb.index;

import com.intellij.openapi.ui.ComboBox;
import com.qihoo.finance.lowcode.design.dto.RowNumberTableModel;
import com.qihoo.finance.lowcode.design.dto.rdb.RdbIndexField;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.Component;

public class IndexFieldConfigComboBoxCellEditor extends DefaultCellEditor {

    private final ComboBox<String> comboBox;
    private RdbIndexField rdbIndexField;

    public IndexFieldConfigComboBoxCellEditor(ComboBox<String> comboBox) {
        super(comboBox);
        this.comboBox = comboBox;
        this.setClickCountToStart(1);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        this.rdbIndexField = ((RowNumberTableModel)table.getModel()).getRowData(row);
        this.comboBox.setSelectedItem(value);
        return comboBox;
    }

    @Override
    public Object getCellEditorValue() {
        String value = String.valueOf(this.comboBox.getSelectedItem());
        rdbIndexField.setFieldName(value);
        return value;
    }
}
