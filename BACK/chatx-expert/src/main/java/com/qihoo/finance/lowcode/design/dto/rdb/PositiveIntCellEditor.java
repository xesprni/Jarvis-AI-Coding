package com.qihoo.finance.lowcode.design.dto.rdb;

import javax.swing.DefaultCellEditor;
import javax.swing.JTextField;
import javax.swing.text.PlainDocument;

public class PositiveIntCellEditor extends DefaultCellEditor {
    public PositiveIntCellEditor() {
        super(new JTextField());
        JTextField textField = (JTextField) getComponent();
        textField.setHorizontalAlignment(JTextField.CENTER);
        textField.setDocument(new PositiveIntDocument());
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
