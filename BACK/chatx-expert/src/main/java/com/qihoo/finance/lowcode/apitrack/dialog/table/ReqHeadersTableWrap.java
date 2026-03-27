package com.qihoo.finance.lowcode.apitrack.dialog.table;

import com.qihoo.finance.lowcode.apitrack.dialog.ApiDesignDialog;
import com.qihoo.finance.lowcode.common.entity.dto.yapi.HeaderParam;
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
public class ReqHeadersTableWrap implements BaseJTableWrap {
    private final boolean isEdit;

    public ReqHeadersTableWrap(boolean isEdit) {
        this.isEdit = isEdit;
    }

    public static List<HeaderParam> getReqHeaders(JTable table) {
        // "参数名称", "参数值", "参数示例", "备注", "操作"
        List<HeaderParam> headerParams = new ArrayList<>();
        for (int row = 0; row < table.getRowCount(); row++) {
            HeaderParam param = new HeaderParam();
            headerParams.add(param);

            Object name = table.getValueAt(row, 0);
            param.setName(Objects.nonNull(name) ? name.toString() : StringUtils.EMPTY);

            Object value = table.getValueAt(row, 1);
            param.setValue(Objects.nonNull(value) ? value.toString() : StringUtils.EMPTY);

            Object example = table.getValueAt(row, 2);
            param.setExample(Objects.nonNull(example) ? example.toString() : StringUtils.EMPTY);

            Object desc = table.getValueAt(row, 3);
            param.setDesc(Objects.nonNull(desc) ? desc.toString() : StringUtils.EMPTY);
        }

        return headerParams;
    }

    @Override
    public boolean isEdit() {
        return isEdit;
    }

    @Override
    public String[] getTableHeaders() {
        return new String[]{"参数名称", "参数值", "参数示例", "备注", "操作"};
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
        // "参数名称", "参数值", "参数示例", "备注", "操作"
        model.addRow(new Object[]{"field" + ApiDesignDialog.addFieldCount(), null, null, null});
    }

    @Override
    public void configColumnProperties(JTable table, DefaultTableCellRenderer centerRenderer) {

        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(0).setCellEditor(new TextCellEditor());

        table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(1).setCellEditor(new TextCellEditor());

        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(2).setCellEditor(new TextCellEditor());

        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(3).setCellEditor(new TextCellEditor());

        table.getColumnModel().getColumn(4).setCellRenderer(new DeleteButtonCellRenderer());
        table.getColumnModel().getColumn(4).setCellEditor(new DeleteButtonCellEditor());
    }
}
