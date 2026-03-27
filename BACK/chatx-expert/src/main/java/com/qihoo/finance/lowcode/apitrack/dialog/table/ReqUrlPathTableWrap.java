package com.qihoo.finance.lowcode.apitrack.dialog.table;

import com.qihoo.finance.lowcode.apitrack.dialog.ApiDesignDialog;
import com.qihoo.finance.lowcode.common.entity.dto.yapi.PathParam;
import com.qihoo.finance.lowcode.common.ui.table.BaseJTableWrap;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * TableSetting
 *
 * @author fengjinfu-jk
 * date 2023/9/4
 * @version 1.0.0
 * @apiNote TableSetting
 */
public class ReqUrlPathTableWrap implements BaseJTableWrap {
    private final boolean isEdit;
    private final JPanel dialog;
    private static final int MAX_SHOW_ROW = 3;


    public ReqUrlPathTableWrap(boolean isEdit, JPanel dialog) {
        this.isEdit = isEdit;
        this.dialog = dialog;
    }

    public static List<PathParam> getReqPath(JTable table) {
        // "路由参数名称", "参数示例", "备注"
        List<PathParam> pathParams = new ArrayList<>();
        for (int row = 0; row < table.getRowCount(); row++) {
            PathParam param = new PathParam();
            pathParams.add(param);

            Object name = table.getValueAt(row, 0);
            param.setName(Objects.nonNull(name) ? name.toString() : StringUtils.EMPTY);

            Object example = table.getValueAt(row, 1);
            param.setExample(Objects.nonNull(example) ? example.toString() : StringUtils.EMPTY);

            Object desc = table.getValueAt(row, 2);
            param.setDesc(Objects.nonNull(desc) ? desc.toString() : StringUtils.EMPTY);
        }

        return pathParams;
    }

    @Override
    public boolean isEdit() {
        return isEdit;
    }

    @Override
    public String[] getTableHeaders() {
        return new String[]{"路由参数名称", "参数示例", "备注"};
    }

    @Override
    public Object[][] getDefaultTableData() {
        return new Object[0][];
    }

    @Override
    public Object[][] getEditTableData() {
        return new Object[][]{};
    }

    @Override
    public void addRow(ActionEvent e, JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        // "路由参数名称", "参数示例", "备注"
        model.addRow(new Object[]{"field" + ApiDesignDialog.addFieldCount(), null, null});
    }

    @Override
    public void configColumnProperties(JTable table, DefaultTableCellRenderer centerRenderer) {

        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(0).setCellEditor(new TextCellEditor(false));

        table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(1).setCellEditor(new TextCellEditor());

        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(2).setCellEditor(new TextCellEditor());
    }

    public DocumentListener updateApiPathUrl(JScrollPane pathTableScrollPane, JTable pathTable, JTextField apiPathUrl) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateText();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateText();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateText();
            }

            private void updateText() {
                // 处理文本变化的逻辑
                String urlText = apiPathUrl.getText();
                String[] urls = urlText.split("/");
                int routeRow = 0;
                int column = 0;

                DefaultTableModel model = (DefaultTableModel) pathTable.getModel();
                for (String path : urls) {
                    if (path.startsWith("{") && path.endsWith("}") && path.length() > 2) {
                        String route = path.substring(1, path.length() - 1);

                        if (model.getRowCount() >= routeRow + 1) {
                            Object valueAt = model.getValueAt(routeRow, column);
                            if (!route.equals(valueAt)) {
                                model.setValueAt(route, routeRow, column);
                            }
                        } else {
                            model.addRow(new Object[]{route, "", ""});
                        }
                        routeRow++;
                    }
                }

                if (routeRow < model.getRowCount()) {
                    for (int rowCount = model.getRowCount(); rowCount > routeRow; rowCount--) {
                        model.removeRow(rowCount - 1);
                    }
                }

                // adjustScrollPaneSize
                if (pathTable.getRowCount() == 0) {
                    pathTableScrollPane.setVisible(false);
                } else {
                    int row = Math.min(MAX_SHOW_ROW, pathTable.getRowCount());
                    pathTableScrollPane.setPreferredSize(new Dimension(-1, 30 + (pathTable.getRowHeight() * row)));

                    pathTableScrollPane.setVisible(true);
                }
            }
        };
    }
}
