package com.qihoo.finance.lowcode.design.util;

import com.intellij.ui.table.JBTable;
import com.qihoo.finance.lowcode.design.dto.RowNumberTableModel;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

public class JTableUtil {

    public static void initStyle(JTable table) {
        TableModel model = table.getModel();
        table.getTableHeader().setDefaultRenderer(getCenterAlignRender());
        if (model instanceof RowNumberTableModel) {
            table.getColumnModel().getColumn(0).setMaxWidth(50);
            table.getColumnModel().getColumn(0).setCellRenderer(getRightAlignRender());
//            table.getColumnModel().getColumn(0).setHeaderRenderer(getHeaderRightAlignRender(table));
            table.getColumnModel().getColumn(1).setCellRenderer(getLeftAlignRender());
        }
    }

    public static TableCellRenderer getHeaderRightAlignRender(JTable table) {
        DefaultTableCellRenderer headerRenderer = (DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer();
        // TODO 获取到的header是单实例，会影响其他列
//        headerRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        return headerRenderer;
    }

    public static TableCellRenderer getRightAlignRender() {
        DefaultTableCellRenderer rightAlignRender = new DefaultTableCellRenderer();
        rightAlignRender.setHorizontalAlignment(SwingConstants.RIGHT);
        return rightAlignRender;
    }

    public static TableCellRenderer getCenterAlignRender() {
        DefaultTableCellRenderer centerAlignRender = new DefaultTableCellRenderer();
        centerAlignRender.setHorizontalAlignment(SwingConstants.CENTER);
        return centerAlignRender;
    }

    public static TableCellRenderer getLeftAlignRender() {
        DefaultTableCellRenderer leftAlignRender = new DefaultTableCellRenderer();
        leftAlignRender.setHorizontalAlignment(SwingConstants.LEFT);
        return leftAlignRender;
    }

}
