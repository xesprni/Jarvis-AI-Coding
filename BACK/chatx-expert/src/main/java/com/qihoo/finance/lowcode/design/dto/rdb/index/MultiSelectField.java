package com.qihoo.finance.lowcode.design.dto.rdb.index;

import lombok.Getter;

import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;

@Getter
public class MultiSelectField extends JPanel {
    private final JTextField textField;

    public MultiSelectField() {
        this.textField = new JTextField();
        this.textField.setEditable(true);
        setLayout(new BorderLayout());
        add(this.textField, BorderLayout.CENTER);
    }

    public void setValue(String values) {
        textField.setText(values);
    }

    public String getValue() {
        return textField.getText();
    }
}
