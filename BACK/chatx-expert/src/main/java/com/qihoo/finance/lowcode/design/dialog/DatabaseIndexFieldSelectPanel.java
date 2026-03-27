package com.qihoo.finance.lowcode.design.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.qihoo.finance.lowcode.design.dto.RowNumberTableModel;
import com.qihoo.finance.lowcode.design.dto.rdb.RdbField;
import com.qihoo.finance.lowcode.design.dto.rdb.RdbIndexField;
import com.qihoo.finance.lowcode.design.dto.rdb.index.IndexFieldConfigComboBoxCellEditor;
import com.qihoo.finance.lowcode.design.dto.rdb.index.IndexFieldConfigPositiveIntCellEditor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author weiyichao
 * @date 2023-07-31
 **/
@Getter
@Setter
@SuppressWarnings("rawtypes")
public class DatabaseIndexFieldSelectPanel extends DatabaseTableBaseDialog {

    private List<RdbIndexField> rdbIndexFields;
    private List<String> rdbFieldNames;
    private List<RdbField> rdbFields;

    public void setRdbFields(List<RdbField> rdbFields) {
        this.rdbFields = rdbFields;
        this.rdbFieldNames = rdbFields.stream().map(RdbField::getFieldName).filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }

    public DatabaseIndexFieldSelectPanel(@NotNull Project project) {
        super(project);
    }


    @Override
    protected void addHandler(ActionEvent e, JTable table) {
        // 添加一行数据
        RowNumberTableModel model = (RowNumberTableModel) table.getModel();
        RdbIndexField rdbIndexField = RdbIndexField.builder().fieldName(StringUtils.EMPTY).fieldLength(0).build();
        model.addRow(new Object[]{rdbIndexField.getFieldName(), String.valueOf(rdbIndexField.getFieldLength())}
                , rdbIndexField);
    }

    @Override
    protected List getRowDatas() {
        return rdbIndexFields;
    }

    @Override
    protected Object[][] getDefaultTableData() {
        Object[][] data;
        if (CollectionUtils.isNotEmpty(rdbIndexFields)) {
            data = new Object[rdbIndexFields.size()][2];
            for (int i = 0; i < rdbIndexFields.size(); i++) {
                Object[] row = new Object[]{rdbIndexFields.get(i).getFieldName()
                        , String.valueOf(rdbIndexFields.get(i).getFieldLength())};
                data[i] = row;
            }
        } else {
            data = new Object[][]{};
        }
        return data;
    }

    @Override
    protected String[] getColumnNames() {
        return new String[]{"字段", "键长度"};
    }

    @Override
    protected void configColumnProperties(JTable table, TableCellRenderer centerRenderer) {
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
//        table.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(new JTextField()));

        // 设置字段类型列为下拉框
        //String[] fieldTypeOptions = {"normal", "unique"};
        String[] rdbFieldArr = rdbFieldNames.toArray(new String[0]);
        ComboBox<String> fieldNameComboBox = new ComboBox<>(rdbFieldArr);

        table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(1).setCellEditor(new IndexFieldConfigComboBoxCellEditor(fieldNameComboBox));

        // 设置字段长度列只允许输入正整数
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(2).setCellEditor(new IndexFieldConfigPositiveIntCellEditor());
    }

    @Override
    public void rowOrderChanged(RowNumberTableModel model, int from, int to) {

    }
}
