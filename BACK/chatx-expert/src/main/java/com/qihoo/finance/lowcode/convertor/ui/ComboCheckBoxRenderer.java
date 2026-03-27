package com.qihoo.finance.lowcode.convertor.ui;

import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import java.awt.Component;
import java.io.Serializable;

public class ComboCheckBoxRenderer extends JCheckBox implements ListCellRenderer, Serializable {

    public ComboCheckBoxRenderer() {
        super();
        setOpaque(true);
        setBorder(null);
    }

    public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {
        setComponentOrientation(list.getComponentOrientation());
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        if (value instanceof ComboCheckBoxEntry) {
            ComboCheckBoxEntry item = (ComboCheckBoxEntry) value;
            setSelected(item.getChecked());
            setToolTipText(item.getValue());
            setText(item.getValue());
        } else {
            setText((value == null) ? "" : value.toString());
        }
        setEnabled(list.isEnabled());
        setFont(list.getFont());
        return this;
    }
}
