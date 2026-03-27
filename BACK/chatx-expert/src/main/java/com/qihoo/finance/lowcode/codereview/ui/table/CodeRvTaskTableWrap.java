package com.qihoo.finance.lowcode.codereview.ui.table;

import com.qihoo.finance.lowcode.common.ui.table.BaseJTableWrap;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * CodeRvCommentTable
 *
 * @author fengjinfu-jk
 * date 2023/11/2
 * @version 1.0.0
 * @apiNote CodeRvCommentTable
 */
public class CodeRvTaskTableWrap implements BaseJTableWrap {
    @Override
    public boolean isEdit() {
        return false;
    }

    @Override
    public String[] getTableHeaders() {
        return new String[]{"发起人", "代码评审人", "发起时间", "任务状态", "关闭人", "关闭时间", "源分支", "源commit", "目标分支", "目标commit"};
    }

    @Override
    public Object[][] getDefaultTableData() {
        List<Object[]> mockData = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            mockData.add(new Object[]{"发起人" + i, "代码评审人" + i, "发起时间" + i, "任务状态" + i, "关闭人" + i, "关闭时间" + i, "源分支" + i, "源commit" + i, "目标分支" + i, "目标commit" + i});
        }

        return mockData.toArray(Object[][]::new);
    }

    @Override
    public Object[][] getEditTableData() {
        return new Object[0][];
    }

    @Override
    public void addRow(ActionEvent e, JTable table) {

    }

    @Override
    public void configColumnProperties(JTable table, DefaultTableCellRenderer centerRenderer) {

    }
}
