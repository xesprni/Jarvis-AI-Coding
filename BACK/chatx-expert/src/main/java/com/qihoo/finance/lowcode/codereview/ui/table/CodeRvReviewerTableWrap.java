package com.qihoo.finance.lowcode.codereview.ui.table;

import com.qihoo.finance.lowcode.common.ui.table.BaseJTableWrap;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CodeRvCommentTable
 *
 * @author fengjinfu-jk
 * date 2023/11/2
 * @version 1.0.0
 * @apiNote CodeRvCommentTable
 */
public class CodeRvReviewerTableWrap implements BaseJTableWrap {
    private final List<String> reviewers;

    public CodeRvReviewerTableWrap(List<String> reviewers) {
        this.reviewers = reviewers;
    }

    @Override
    public String[] getTableHeaders() {
        return new String[]{"", "备注", "提交人", "时间", "id"};
    }

    @Override
    public Object[][] getDefaultTableData() {
        List<Object[]> mockData = new ArrayList<>();
        int i = 1, k = 1;
        mockData.add(new Object[]{reviewers.contains("" + (i++)), "FEAT(PD-16352): 数据库设计代码生成-支持独立选择是否生成Facade模板 fengjinfu-jk 2023/10/31 21:18", "fengjinfu-jk", "2023/10/30 21:18", k++});
        mockData.add(new Object[]{reviewers.contains("" + (i++)), "FEAT(PD-16352): 数据库设计代码生成-支持独立选择是否生成Facade模板 fengjinfu-jk 2023/10/31 21:18", "fengjinfu-jk", "2023/10/30 21:18", k++});
        mockData.add(new Object[]{reviewers.contains("" + (i++)), "FEAT(PD-16352): 数据库设计代码生成-支持独立选择是否生成Facade模板 fengjinfu-jk 2023/10/31 21:18", "fengjinfu-jk", "2023/10/30 21:18", k++});
        mockData.add(new Object[]{reviewers.contains("" + (i++)), "FEAT(PD-16352): 数据库设计代码生成-支持独立选择是否生成Facade模板 fengjinfu-jk 2023/10/31 21:18", "fengjinfu-jk", "2023/10/30 21:18", k++});
        mockData.add(new Object[]{reviewers.contains("" + (i++)), "FEAT(PD-16352): 数据库设计代码生成-支持独立选择是否生成Facade模板 fengjinfu-jk 2023/10/31 21:18", "fengjinfu-jk", "2023/10/30 21:18", k++});
        mockData.add(new Object[]{reviewers.contains("" + (i++)), "FEAT(PD-16352): 数据库设计代码生成-支持独立选择是否生成Facade模板 fengjinfu-jk 2023/10/31 21:18", "fengjinfu-jk", "2023/10/30 21:18", k++});
        mockData.add(new Object[]{reviewers.contains("" + (i++)), "FEAT(JG-1564): ask ai UI优化", "liuzhenghua-jk", "2023/11/2 13:53", k++});
        mockData.add(new Object[]{reviewers.contains("" + (i++)), "feat(JG-1563) web管理侧屏蔽无用tab", "yangzhihao-jk", "2023/11/2 13:41", k++});
        mockData.add(new Object[]{reviewers.contains("" + (i++)), "FEAT(PD-16352): MultiComboBox支持自定义分隔符&大小自适应", "fengjinfu-jk", "2023/11/2 12:04", k++});
        mockData.add(new Object[]{reviewers.contains("" + (i++)), "FEAT(PD-16352): MultiComboBox支持自定义分隔符&大小自适应", "fengjinfu-jk", "2023/11/2 11:17", k++});
        mockData.add(new Object[]{reviewers.contains("" + (i++)), "FEAT(JG-1564): ask ai UI优化", "liuzhenghua-jk", "2023/11/2 11:46", k++});
        mockData.add(new Object[]{reviewers.contains("" + (i++)), "Merge branch 'feature/lowcode-20230915' into feature/lowcode-20231103", "fengjinfu-jk", "2023/11/1 17:48", k++});
        mockData.add(new Object[]{reviewers.contains("" + (i++)), "FEAT(PD-16352): Jira-commit对接用户活跃任务刷新按钮", "fengjinfu-jk", "2023/11/1 17:45", k++});
        mockData.add(new Object[]{reviewers.contains("" + (i++)), "FEAT(JG-1564): ask ai log", "liuzhenghua-jk", "2023/11/1 17:30", k++});
        mockData.add(new Object[]{reviewers.contains("" + (i++)), "FEAT(JG-1564): ask ai log", "liuzhenghua-jk", "2023/11/1 17:27", k++});
        mockData.add(new Object[]{reviewers.contains("" + (i++)), "feat(JG-1563) lowcode-client 1.0.6-SNAPSHOT", "yangzhihao-jk", "2023/11/1 17:25", k++});
        mockData.add(new Object[]{reviewers.contains("" + (i++)), "fix(JG-1563) lowcode-client BaseEntity去除默认值", "yangzhihao-jk", "2023/11/1 17:17", k++});
        mockData.add(new Object[]{reviewers.contains("" + (i++)), "FEAT(PD-16352): 数据库设计代码生成-支持独立选择是否生成Facade模板 fengjinfu-jk 2023/10/31 21:18", "fengjinfu-jk", "2023/10/30 21:18", k++});
        mockData.add(new Object[]{reviewers.contains("" + (i++)), "FEAT(PD-16352): 数据库设计代码生成-支持独立选择是否生成Facade模板 fengjinfu-jk 2023/10/31 21:18", "fengjinfu-jk", "2023/10/30 21:18", k++});
        mockData.add(new Object[]{reviewers.contains("" + (i++)), "FEAT(PD-16352): 数据库设计代码生成-支持独立选择是否生成Facade模板 fengjinfu-jk 2023/10/31 21:18", "fengjinfu-jk", "2023/10/30 21:18", k++});
        mockData.add(new Object[]{reviewers.contains("" + (i++)), "FEAT(PD-16352): 数据库设计代码生成-支持独立选择是否生成Facade模板 fengjinfu-jk 2023/10/31 21:18", "fengjinfu-jk", "2023/10/30 21:18", k++});
        mockData.add(new Object[]{reviewers.contains("" + (i++)), "FEAT(PD-16352): 数据库设计代码生成-支持独立选择是否生成Facade模板 fengjinfu-jk 2023/10/31 21:18", "fengjinfu-jk", "2023/10/30 21:18", k++});


        return mockData.toArray(Object[][]::new);
    }

    @Override
    public void addRow(ActionEvent e, JTable table) {

    }

    @Override
    public void configColumnProperties(JTable table, DefaultTableCellRenderer centerRenderer) {
        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(JLabel.LEFT);

        table.getColumnModel().getColumn(0).setCellRenderer(new CheckBoxRenderer());
        table.getColumnModel().getColumn(0).setCellEditor(new CheckBoxEditor());
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(0).setMinWidth(50);

        table.getColumnModel().getColumn(1).setCellRenderer(leftRenderer);
        table.getColumnModel().getColumn(1).setCellEditor(new TextCellEditor(false, SwingConstants.LEFT));

        table.getColumnModel().getColumn(2).setCellRenderer(leftRenderer);
        table.getColumnModel().getColumn(2).setCellEditor(new TextCellEditor(false, SwingConstants.LEFT));
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setMaxWidth(120);
        table.getColumnModel().getColumn(2).setMinWidth(120);

        table.getColumnModel().getColumn(3).setCellRenderer(leftRenderer);
        table.getColumnModel().getColumn(3).setCellEditor(new TextCellEditor(false, SwingConstants.LEFT));
        table.getColumnModel().getColumn(3).setPreferredWidth(150);
        table.getColumnModel().getColumn(3).setMaxWidth(150);
        table.getColumnModel().getColumn(3).setMinWidth(150);

        // 隐藏ID列, 隐藏后需通过model获取值
        table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(4).setCellEditor(new TextCellEditor());
        table.removeColumn(table.getColumnModel().getColumn(4));
    }

    //------------------------------------------------------------------------------------------------------------------
    public static Map<String, String> getSelectCommits(JTable table) {
        Map<String, String> selectCommits = new HashMap<>();
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        for (int row = 0; row < model.getRowCount(); row++) {
            if ((Boolean) model.getValueAt(row, 0)) {
                selectCommits.put(model.getValueAt(row, 4).toString(), model.getValueAt(row, 1).toString());
            }
        }

        return selectCommits;
    }
}
