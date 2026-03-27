package com.qihoo.finance.lowcode.design.dto.rdb.index;

import com.qihoo.finance.lowcode.design.constant.RdbIndexPart;
import com.qihoo.finance.lowcode.design.dialog.DatabasePreviewPanel;
import com.qihoo.finance.lowcode.design.dto.rdb.RdbIndex;
import com.qihoo.finance.lowcode.design.dto.RowNumberTableModel;

import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.Component;
import java.util.Optional;

public class IndexTextCellEditor extends DefaultCellEditor {

    private final JTextField textField;
    private final RdbIndexPart rdbIndexPart;
    private RdbIndex rdbIndex;
    private final DatabasePreviewPanel dbPreviewPanel;

    public IndexTextCellEditor(RdbIndexPart rdbIndexPart, DatabasePreviewPanel dbPreviewPanel) {
        super(new JTextField());
        textField = (JTextField)getComponent();
        textField.setHorizontalAlignment(JTextField.CENTER);
        this.rdbIndexPart = rdbIndexPart;
        this.dbPreviewPanel = dbPreviewPanel;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        this.rdbIndex = ((RowNumberTableModel)table.getModel()).getRowData(row);
        textField.setText(Optional.ofNullable(value).map(Object::toString).orElse(null));
        return textField;
    }

    @Override
    public Object getCellEditorValue() {
        if (rdbIndexPart == RdbIndexPart.indexName) {
            rdbIndex.setIndexName(textField.getText());
        } else if (rdbIndexPart == RdbIndexPart.indexComment) {
            rdbIndex.setIndexComment(textField.getText());
        }
        dbPreviewPanel.updateSqlPreviewText();
        return textField.getText();
    }
}
