package com.qihoo.finance.lowcode.design.dto.rdb.field;

import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.util.Icons;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.Objects;

/**
 * FieldBtnRenderer
 *
 * @author fengjinfu-jk
 * date 2023/10/23
 * @version 1.0.0
 * @apiNote FieldBtnRenderer
 */
public class FieldCopyBtnRenderer implements TableCellRenderer {
    private final JButton copyButton;
    private final JLabel empty;

    public FieldCopyBtnRenderer() {
        this.copyButton = new JButton();
        this.empty = new JLabel();
        copyButton.setToolTipText("字段复制");
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        String fieldName = getColumnVal(table, row, 1, StringUtils.EMPTY);
        if (Constants.DB_COLUMN.BASE_ENTITY.containsKey(fieldName) ||
                Constants.DB_COLUMN.DELETED_AT.containsKey(fieldName)) {
            return empty;
        }

        copyButton.setIcon(Icons.scaleToWidth(Icons.COPY, 16));
        copyButton.setBorderPainted(false);
        copyButton.setContentAreaFilled(false);
        copyButton.setPreferredSize(new Dimension(22, 22));
        return copyButton;
    }

    private <T> T getColumnVal(JTable table, int row, int column, T defaultVal) {
        Object valueAt = table.getValueAt(row, column);
        if (Objects.nonNull(valueAt)) {
            return (T) valueAt;
        }

        return defaultVal;
    }
}
