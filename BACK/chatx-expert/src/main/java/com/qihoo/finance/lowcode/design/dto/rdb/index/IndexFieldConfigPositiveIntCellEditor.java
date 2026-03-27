package com.qihoo.finance.lowcode.design.dto.rdb.index;

import com.qihoo.finance.lowcode.design.dto.RowNumberTableModel;
import com.qihoo.finance.lowcode.design.dto.rdb.PositiveIntCellEditor;
import com.qihoo.finance.lowcode.design.dto.rdb.RdbIndexField;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.util.Optional;

public class IndexFieldConfigPositiveIntCellEditor extends DefaultCellEditor {

    private final JTextField textField;
    private RdbIndexField rdbIndexField;

    public IndexFieldConfigPositiveIntCellEditor() {
        super(new JTextField());
        this.textField = (JTextField) getComponent();
        textField.setHorizontalAlignment(JTextField.CENTER);
        textField.setDocument(new PositiveIntCellEditor.PositiveIntDocument());
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        this.rdbIndexField = ((RowNumberTableModel) table.getModel()).getRowData(row);
        textField.setText(Optional.ofNullable(value).map(Object::toString).orElse(null));
        return textField;
    }

    @Override
    public Object getCellEditorValue() {
        rdbIndexField.setFieldLength(Integer.parseInt(StringUtils.defaultIfEmpty(textField.getText(), "0")));
        return textField.getText();
    }

    public static class PositiveIntDocument extends PlainDocument {
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
