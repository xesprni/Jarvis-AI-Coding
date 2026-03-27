package com.qihoo.finance.lowcode.design.dto.rdb.field;

import org.apache.commons.lang3.ObjectUtils;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;

public class CheckBoxRenderer extends JCheckBox implements TableCellRenderer {
    public CheckBoxRenderer() {
        setHorizontalAlignment(JLabel.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus
            , int row, int column) {
        setSelected((Boolean) ObjectUtils.defaultIfNull(value, false));
        return this;
    }
}
