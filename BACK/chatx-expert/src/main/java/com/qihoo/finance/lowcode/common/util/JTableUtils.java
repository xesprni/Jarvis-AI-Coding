package com.qihoo.finance.lowcode.common.util;

import com.qihoo.finance.lowcode.common.factory.ChatXToolWindowFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.Enumeration;
import java.util.Objects;

/**
 * JTableUtils
 *
 * @author fengjinfu-jk
 * date 2023/11/3
 * @version 1.0.0
 * @apiNote JTableUtils
 */
public class JTableUtils {
    public static final int COLUMN_MIN_WIDTH = 100;
    public static final int COLUMN_MAX_WIDTH = 200;

    public static void setTableHeaderRenderer(JTable table, Icon columnIcon) {

        JTableHeader header = table.getTableHeader();
        // 创建一个自定义的表头渲染器
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                // 调用父类的方法获取默认的表头渲染器组件
                Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // 设置表头文本字体为粗体
                Font font = component.getFont();
                Font boldFont = new Font(font.getFontName(), Font.BOLD, font.getSize());
                component.setFont(boldFont);

                // 图标
                if (component instanceof JLabel) {
                    if (Objects.nonNull(columnIcon)) {
                        ((JLabel) component).setIcon(columnIcon);
                    }
                }

                component.setBackground(ChatXToolWindowFactory.BACKGROUND);
                return component;
            }
        };

        headerRenderer.setHorizontalAlignment(JLabel.CENTER);
        header.setDefaultRenderer(headerRenderer);
    }


    /**
     * 設置table的列寬隨內容調整
     *
     * @param table JTable
     */
    public static void fitTableWidth(JTable table, int maxWidth) {
        int adjust = 50;
        if (autoResize(table, maxWidth, adjust)) return;

        // 设置JTable的列宽随着列表内容的大小进行调整
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JTableHeader header = table.getTableHeader();
        int rowCount = table.getRowCount();
        Enumeration<TableColumn> columns = table.getColumnModel().getColumns();
        while (columns.hasMoreElements()) {
            TableColumn column = columns.nextElement();
            int col = header.getColumnModel().getColumnIndex(column.getIdentifier());
            int width = adjust + (int) table.getTableHeader().getDefaultRenderer().getTableCellRendererComponent(table, column.getIdentifier(), false, false, -1, col).getPreferredSize().getWidth();
            if (width < COLUMN_MIN_WIDTH) {
                for (int row = 0; row < rowCount; row++) {
                    int preferWidth = (int) table.getCellRenderer(row, col).getTableCellRendererComponent(table, table.getValueAt(row, col), false, false, row, col).getPreferredSize().getWidth();

                    width = Math.max(width, preferWidth);
                    width = Math.min(width, COLUMN_MAX_WIDTH);

                    if (width == COLUMN_MAX_WIDTH) break;
                }
            }

            header.setResizingColumn(column);
            column.setWidth(width + table.getIntercellSpacing().width);
        }
    }

    private static int getTableHeaderWidth(JTable table, int adjust) {
        int width = 0;

        Enumeration<TableColumn> columns = table.getColumnModel().getColumns();
        JTableHeader header = table.getTableHeader();
        while (columns.hasMoreElements()) {
            TableColumn column = columns.nextElement();
            int col = header.getColumnModel().getColumnIndex(column.getIdentifier());
            width += adjust + (int) table.getTableHeader().getDefaultRenderer().getTableCellRendererComponent(table, column.getIdentifier(), false, false, -1, col).getPreferredSize().getWidth();
        }

        return width;
    }

    private static boolean autoResize(JTable table, int maxWidth, int adjust) {
        int tableWidth = getTableHeaderWidth(table, adjust);
        if (tableWidth <= maxWidth) {
            // 自适应
            table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
            return true;
        }

        return false;
    }
}
