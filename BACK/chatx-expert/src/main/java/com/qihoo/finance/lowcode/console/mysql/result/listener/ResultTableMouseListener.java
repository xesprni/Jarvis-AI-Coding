package com.qihoo.finance.lowcode.console.mysql.result.listener;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.ui.JBColor;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.entity.dto.SQLExecuteResult;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.NotifyUtils;
import com.qihoo.finance.lowcode.console.mysql.editor.SQLDataEditor;
import com.qihoo.finance.lowcode.console.mysql.result.ui.ResultView;
import com.qihoo.finance.lowcode.design.entity.DatabaseNode;
import com.qihoo.finance.lowcode.design.util.DatabaseDesignUtils;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * MouseAction
 *
 * @author fengjinfu-jk
 * date 2023/10/25
 * @version 1.0.0
 * @apiNote MouseAction
 */
public class ResultTableMouseListener extends MouseAdapter {
    private final JBTable table;
    private final DatabaseNode database;
    private final ResultView.TablePageBtnPanel pageBtnPanel;
    // 主键信息
    private final ResultView resultView;

    public ResultTableMouseListener(DatabaseNode database, JBTable table, ResultView.TablePageBtnPanel pageBtnPanel) {
        this.database = database;
        this.table = table;
        this.pageBtnPanel = pageBtnPanel;
        this.resultView = ProjectUtils.getCurrProject().getService(ResultView.class);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        super.mouseClicked(e);
        // 左键两次或右键
        boolean selectRow = table.getSelectedRow() >= 0;
        boolean click = SwingUtilities.isRightMouseButton(e);
        if (selectRow && click) {
            // 复制为insert语句或update语句
            JBPopupMenu popupMenu = createPopupMenu(e, table);
            popupMenu.show(table, e.getX(), e.getY());
        }
    }

    private JBPopupMenu createPopupMenu(MouseEvent event, JBTable table) {
        JBPopupMenu popupMenu = new JBPopupMenu();
        popupMenu.setBorder(BorderFactory.createLineBorder(JBColor.GRAY));
        popupMenu.getInsets().set(50, 50, 50, 50);
        popupMenu.add(Box.createVerticalStrut(2));

        JBMenuItem newRow = createMenuItem(new AbstractAction("新增") {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 主键信息
                resultView.newAction(table, pageBtnPanel);
            }
        });
        newRow.setIcon(Icons.scaleToWidth(Icons.ADD_ICON, 16));
        popupMenu.add(newRow);
        popupMenu.add(Box.createVerticalStrut(2));

        JBMenuItem delete = createMenuItem(new AbstractAction("删除") {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 主键信息
                resultView.deleteAction(database, table, pageBtnPanel);
            }
        });
        delete.setIcon(Icons.scaleToWidth(Icons.DELETE, 16));
        popupMenu.add(delete);
        popupMenu.add(Box.createVerticalStrut(2));
        popupMenu.addSeparator();
        popupMenu.add(Box.createVerticalStrut(2));

        JBMenuItem setNull = createMenuItem(new AbstractAction("置为Null") {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 主键信息
                int row = table.rowAtPoint(event.getPoint());
                int column = table.columnAtPoint(event.getPoint());
                resultView.setNullAction(table, row, column);
            }
        });
        setNull.setIcon(Icons.scaleToWidth(Icons.EDIT, 13));
        popupMenu.add(setNull);
        popupMenu.add(Box.createVerticalStrut(2));

        JBMenuItem setEmpty = createMenuItem(new AbstractAction("置为空字符串") {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 主键信息
                int row = table.rowAtPoint(event.getPoint());
                int column = table.columnAtPoint(event.getPoint());
                resultView.setEmptyAction(table, row, column);
            }
        });
        setEmpty.setIcon(Icons.scaleToWidth(Icons.EDIT, 13));
        popupMenu.add(setEmpty);
        popupMenu.add(Box.createVerticalStrut(2));
        popupMenu.addSeparator();

        popupMenu.add(Box.createVerticalStrut(2));
        JBMenuItem copyToInsert = createMenuItem("复制为Insert语句", table, true);
        copyToInsert.setIcon(Icons.scaleToWidth(Icons.COPY_DOC, 16));
        popupMenu.add(copyToInsert);
        popupMenu.add(Box.createVerticalStrut(2));
        JBMenuItem copyToUpdate = createMenuItem("复制为Update语句", table, false);
        copyToUpdate.setIcon(Icons.scaleToWidth(Icons.COPY_DOC, 16));
        popupMenu.add(copyToUpdate);

        popupMenu.add(Box.createVerticalStrut(2));
        popupMenu.addSeparator();

        return popupMenu;
    }

    private JBMenuItem createMenuItem(String text, JBTable table, boolean insert) {
                /*
                INSERT INTO `lingxi_server`.`app_base_info` (`id`, `app_code`) VALUES (25, 'ags-app');
                UPDATE `lingxi_server`.`app_base_info` SET `app_code` = 'ags-app' WHERE `id` = 25;
                 */
        JBMenuItem menuItem = new JBMenuItem(new AbstractAction(text) {
            @Override
            public void actionPerformed(ActionEvent e) {
                SQLExecuteResult data = (SQLExecuteResult) pageBtnPanel.getTableScroll().getClientProperty(SQLDataEditor.EXECUTE_RESULT);
                copySQLToClipboard(table, data, insert);
            }
        });
        // 设置自定义边距
        menuItem.setMargin(JBUI.insets(0, 10));
        return menuItem;
    }

    private JBMenuItem createMenuItem(AbstractAction action) {
        JBMenuItem menuItem = new JBMenuItem(action);
        // 设置自定义边距
        menuItem.setMargin(JBUI.insets(0, 10));
        return menuItem;
    }

    public static void copySQLToClipboard(JBTable table, SQLExecuteResult data, boolean insert) {
        // 表信息
        String tableName = StringUtils.defaultString(data.getTableName());
        // 主键信息
        List<String> primaryKeys = DatabaseDesignUtils.getPrimaryKeys(data);

        // 自定义菜单项的操作
        Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        String sql = insert ? generateInsertSQL(table, tableName) : generateUpdateSQL(table, tableName, primaryKeys);
        if (StringUtils.isNotEmpty(sql)) {
            StringSelection selection = new StringSelection(sql);
            systemClipboard.setContents(selection, null);

            NotifyUtils.notify("SQL语句已复制到粘贴板", NotificationType.INFORMATION);
        }
    }

    private static String generateInsertSQL(JTable table, String tableName) {
        // String insertSQL = "INSERT INTO `lingxi_server`.`app_base_info` (`id`, `app_code`) VALUES (25, 'ags-app');";
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length == 0) {
            NotifyUtils.notify("请先选中数据行", NotificationType.WARNING);
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();
        for (int selectedRow : selectedRows) {
            sb.append(generateInsertSQL(table, tableName, selectedRow));
        }

        return sb.toString();
    }

    public static String generateInsertSQL(JTable table, String tableName, int selectedRow) {
        TableColumnModel columnModel = table.getColumnModel();
        StringBuilder columnNameSb = new StringBuilder("INSERT INTO ");
        columnNameSb.append("`").append(tableName).append("`").append(" (");
        StringBuilder columnValueSb = new StringBuilder(" VALUES (");

        for (int column = 0; column < table.getColumnCount(); column++) {
            Object valueAt = table.getValueAt(selectedRow, column);
            String columnName = columnModel.getColumn(column).getHeaderValue().toString().trim();
            String columnValue = String.valueOf(valueAt);
            boolean bit = Constants.REGEX.BIT.matcher(columnValue).find();

            // column&value
            columnNameSb.append("`").append(columnName).append("`");
            if (valueAt instanceof String && !bit) {
                columnValueSb.append("'").append(columnValue).append("'");
            } else {
                columnValueSb.append(columnValue);
            }
            // split
            if (column < table.getColumnCount() - 1) {
                columnNameSb.append(", ");
                columnValueSb.append(", ");
            }
        }

        return columnNameSb.append(")").append(columnValueSb).append(");\n").toString();
    }

    private static String generateUpdateSQL(JBTable table, String tableName, List<String> primaryKeys) {
        // String updateSQL = "UPDATE `lingxi_server`.`app_base_info` SET `app_code` = 'ags-app', `deleted_at` = 0 WHERE `id` = 25;";
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length == 0) {
            NotifyUtils.notify("请先选中数据行后再进行复制操作", NotificationType.WARNING);
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();
        for (int selectedRow : selectedRows) {
            TableColumnModel columnModel = table.getColumnModel();
            StringBuilder columnNameSb = new StringBuilder("UPDATE ");
            columnNameSb.append("`").append(tableName).append("`").append(" SET ");
            StringBuilder whereSb = new StringBuilder(" WHERE ");
            int initLength = columnNameSb.length();
            for (int column = 0; column < table.getColumnCount(); column++) {
                Object valueAt = table.getValueAt(selectedRow, column);
                String columnName = columnModel.getColumn(column).getHeaderValue().toString().trim();
                String columnValue = String.valueOf(valueAt);
                boolean bit = Constants.REGEX.BIT.matcher(columnValue).find();

                if (primaryKeys.contains(columnName)) {
                    // split
                    if (whereSb.toString().contains(" = ")) {
                        whereSb.append(" AND ");
                    }

                    whereSb.append("`").append(columnName).append("` = ");
                    if (valueAt instanceof String && !bit) {
                        whereSb.append("'").append(columnValue).append("'");
                    } else {
                        whereSb.append(columnValue);
                    }

                    continue;
                }

                // split
                if (columnNameSb.length() > initLength) {
                    columnNameSb.append(", ");
                }

                // column&value
                columnNameSb.append("`").append(columnName).append("` = ");
                if (valueAt instanceof String && !bit) {
                    columnNameSb.append("'").append(columnValue).append("'");
                } else {
                    columnNameSb.append(columnValue);
                }
            }

            sb.append(columnNameSb.append(whereSb).append(";\n"));
        }

        return sb.toString();
    }
}
