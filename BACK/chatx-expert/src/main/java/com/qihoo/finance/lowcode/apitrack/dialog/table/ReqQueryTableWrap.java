package com.qihoo.finance.lowcode.apitrack.dialog.table;

import com.qihoo.finance.lowcode.apitrack.dialog.ApiDesignDialog;
import com.qihoo.finance.lowcode.common.entity.dto.yapi.QueryParam;
import com.qihoo.finance.lowcode.common.ui.table.BaseJTableWrap;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
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
public class ReqQueryTableWrap implements BaseJTableWrap {
    private final boolean isEdit;

    public ReqQueryTableWrap(boolean isEdit) {
        this.isEdit = isEdit;
    }

    public static List<QueryParam> getReqQuery(JTable table) {
        // "参数名称", "必须", "参数示例", "备注", "操作"
        List<QueryParam> queryParams = new ArrayList<>();
        for (int row = 0; row < table.getRowCount(); row++) {
            QueryParam param = new QueryParam();
            queryParams.add(param);

            Object name = table.getValueAt(row, 0);
            param.setName(Objects.nonNull(name) ? name.toString() : StringUtils.EMPTY);

            Object required = table.getValueAt(row, 1);
            String requiredStr = Objects.nonNull(required) ? required.toString() : StringUtils.EMPTY;
            if ("true".equalsIgnoreCase(requiredStr)) requiredStr = QueryParam.REQUIRED_TRUE;
            if ("false".equalsIgnoreCase(requiredStr)) requiredStr = QueryParam.REQUIRED_FALSE;
            param.setRequired(requiredStr);

            Object example = table.getValueAt(row, 2);
            param.setExample(Objects.nonNull(example) ? example.toString() : StringUtils.EMPTY);

            Object desc = table.getValueAt(row, 3);
            param.setDesc(Objects.nonNull(desc) ? desc.toString() : StringUtils.EMPTY);
        }

        return queryParams;
    }

    @Override
    public boolean isEdit() {
        return isEdit;
    }

    @Override
    public String[] getTableHeaders() {
        return new String[]{"参数名称", "必须", "参数示例", "备注", "操作"};
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
        // "参数名称", "必须", "参数示例", "备注", "操作"
        model.addRow(new Object[]{"field" + ApiDesignDialog.addFieldCount(), QueryParam.REQUIRED_FALSE, null, null});
    }

    @Override
    public void configColumnProperties(JTable table, DefaultTableCellRenderer centerRenderer) {

        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(0).setCellEditor(new TextCellEditor());

        table.getColumnModel().getColumn(1).setCellRenderer(new CheckBoxRenderer());
        table.getColumnModel().getColumn(1).setCellEditor(new CheckBoxEditor());

        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(2).setCellEditor(new TextCellEditor());

        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(3).setCellEditor(new TextCellEditor());

        table.getColumnModel().getColumn(4).setCellRenderer(new DeleteButtonCellRenderer());
        table.getColumnModel().getColumn(4).setCellEditor(new DeleteButtonCellEditor());
    }
}
