package com.qihoo.finance.lowcode.design.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.ui.base.RoundedLabel;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.design.dto.RowNumberTableModel;
import com.qihoo.finance.lowcode.design.dto.rdb.RdbField;
import com.qihoo.finance.lowcode.design.listener.DragDropRowTableUI;
import com.qihoo.finance.lowcode.design.ui.DatabaseBasePanel;
import com.qihoo.finance.lowcode.design.util.JTableUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.plaf.TableUI;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * @author weiyichao
 * @date 2023-07-31
 **/
@SuppressWarnings("rawtypes")
public abstract class DatabaseTableBaseDialog extends DatabaseBasePanel {

    protected JButton deleteButton;
    protected JButton addDeletedAt = new JButton("+ 添加软删除");
    protected JButton addEncryptField = new JButton("+ 添加加密字段");
    protected RoundedLabel tipsLabel = new RoundedLabel("可拖动行改变字段顺序", RoundedLabel.GREEN, RoundedLabel.WHITE, 10);
    protected RoundedLabel tipsLabel2 = new RoundedLabel("MSF加密字段必须以_encryptx，_md5x结尾", RoundedLabel.BLUE, RoundedLabel.WHITE, 10);

    private JTable table;

    public DatabaseTableBaseDialog(@NotNull Project project) {
        super(project);
        addDeletedAt.setVisible(false);
        addEncryptField.setVisible(false);
        tipsLabel.setVisible(false);
        tipsLabel2.setVisible(false);
    }


    public JPanel getOperationButton() {
        // 创建新增按钮
        JButton addButton = new JButton("+ 新增");
        // 创建删除按钮
        deleteButton = new JButton("- 删除");
        deleteButton.setPreferredSize(addButton.getPreferredSize());
        deleteButton.setVisible(true);
        // 设置删除按钮的点击事件
        deleteButton.addActionListener(e -> deleteHandler(e, table));
        // 设置新增按钮的点击事件
        addButton.addActionListener(e -> addHandler(e, table));

        // 创建按钮面板并添加按钮
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 2));
        buttonPanel.add(addButton);
        buttonPanel.add(addEncryptField);
        buttonPanel.add(addDeletedAt);
        buttonPanel.add(deleteButton);
        buttonPanel.add(tipsLabel);
        buttonPanel.add(tipsLabel2);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 5));
        return buttonPanel;
    }

    public JTable getTable() {
        return table;
    }

    /**
     * 删除按钮逻辑实现-（操作表格）
     *
     * @param e 参数
     */
    protected void deleteHandler(ActionEvent e, JTable table) {
        int selectedRow = table.getSelectedRow();
        RowNumberTableModel model = (RowNumberTableModel) table.getModel();
        if (selectedRow > -1) {
            ((DefaultTableModel) table.getModel()).removeRow(selectedRow);
        }
    }

    /**
     * 新增按钮逻辑实现-（操作表格）
     *
     * @param e 参数
     */
    protected abstract void addHandler(ActionEvent e, JTable table);

    protected abstract List getRowDatas();

    protected abstract String[] getColumnNames();

    protected Object[][] getDefaultTableData() {
        return new Object[][]{};
    }

    protected Object[][] getEditTableData() {
        return new Object[][]{};
    }

    protected abstract void configColumnProperties(JTable table, TableCellRenderer centerRenderer);

    @Override
    public Component createPanel() {
        String[] columnNames = getColumnNames();
        Object[][] data = (isEdit() || isCopy()) ? getEditTableData() : getDefaultTableData();
        List rowDatas = getRowDatas();
        RowNumberTableModel tableModel = new RowNumberTableModel(data, columnNames, rowDatas);
        tableModel.setRowOrderChangeListener(this::rowOrderChanged);
        table = new JBTable(tableModel);
        // 禁止拖到列
        table.getTableHeader().setReorderingAllowed(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setUI(getTableUI());
        // 设置表头居中对齐
        JTableUtil.initStyle(table);
        configColumnProperties(table, JTableUtil.getCenterAlignRender());
        // 创建JPanel用于容纳JTable和按钮
        JPanel panel = new JPanel(new BorderLayout());
        // 创建按钮面板并添加按钮
        JPanel buttonPanel = getOperationButton();
        panel.add(buttonPanel, BorderLayout.NORTH);
        table.setBorder(null);
        JScrollPane scrollPane = new JBScrollPane(table);
        scrollPane.setBorder(null);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    public abstract void rowOrderChanged(RowNumberTableModel model, int from, int to);

    public TableUI getTableUI() {
        return new DragDropRowTableUI();
    }

}
