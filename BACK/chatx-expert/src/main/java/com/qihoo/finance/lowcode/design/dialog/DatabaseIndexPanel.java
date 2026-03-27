package com.qihoo.finance.lowcode.design.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.design.constant.RdbIndexPart;
import com.qihoo.finance.lowcode.design.dto.RowNumberTableModel;
import com.qihoo.finance.lowcode.design.dto.rdb.RdbField;
import com.qihoo.finance.lowcode.design.dto.rdb.RdbIndex;
import com.qihoo.finance.lowcode.design.dto.rdb.RdbIndexField;
import com.qihoo.finance.lowcode.design.dto.rdb.index.ComboBoxCellEditor;
import com.qihoo.finance.lowcode.design.dto.rdb.index.IndexTextCellEditor;
import com.qihoo.finance.lowcode.design.dto.rdb.index.MultiSelectIndexFieldCellEditor;
import com.qihoo.finance.lowcode.design.entity.DatabaseColumnNode;
import com.qihoo.finance.lowcode.design.entity.DatabaseIndexNode;
import com.qihoo.finance.lowcode.design.util.JTableUtil;
import com.qihoo.finance.lowcode.gentracker.tool.JSON;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author weiyichao
 * @date 2023-07-31
 **/
@SuppressWarnings("rawtype")
public class DatabaseIndexPanel extends DatabaseTableBaseDialog {

    private final Project project;
    private List<RdbIndex> rdbIndexes;
    private List<RdbIndex> droppedRdbIndexes;
    private List<RdbField> rdbFields;

    private final DatabaseIndexFieldSelectPanel indexFieldSelectPanel;
    private final DatabasePreviewPanel databasePreviewPanel;

    public DatabaseIndexPanel(@NotNull Project project) {
        super(project);
        this.project = project;
        this.indexFieldSelectPanel = project.getService(DatabaseIndexFieldSelectPanel.class);
        this.databasePreviewPanel = project.getService(DatabasePreviewPanel.class);
    }

    /**
     * 由于Service类型插件限制，构造函数只能含有project参数，类自身需要的成员初始化由此方法负责
     *
     * @param rdbIndexes
     * @param droppedRdbIndexes
     * @param rdbFields
     */
    public void init(List<RdbIndex> rdbIndexes, List<RdbIndex> droppedRdbIndexes, List<RdbField> rdbFields) {
        this.rdbIndexes = rdbIndexes;
        this.droppedRdbIndexes = droppedRdbIndexes;
        this.rdbFields = rdbFields;
    }

    @Override
    protected void addHandler(ActionEvent e, JTable table) {
        RowNumberTableModel model = (RowNumberTableModel) table.getModel();
        // 添加一行数据
        RdbIndex rdbIndex = RdbIndex.builder().indexName(StringUtils.EMPTY).indexField(StringUtils.EMPTY)
                .indexType("NORMAL").indexMethod("BTREE").indexComment(StringUtils.EMPTY)
                .rdbIndexFields(new ArrayList<>()).build();
        model.addRow(new Object[]{rdbIndex.getIndexType(), rdbIndex.getIndexField(), rdbIndex.getIndexName(), rdbIndex.getIndexMethod(), rdbIndex.getIndexComment()}, rdbIndex);
    }

    /**
     * 删除按钮逻辑实现-（操作表格）
     *
     * @param e 参数
     */
    @Override
    protected void deleteHandler(ActionEvent e, JTable table) {
        RowNumberTableModel model = (RowNumberTableModel) table.getModel();
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            //TODO 应该给一个提示先选中一行，再点击删除
            return;
        }
        RdbIndex rdbIndex = model.getRowData(selectedRow);
        if (rdbIndex != null) {
            if (isEdit() && rdbIndex.isDbIndex()) {
                droppedRdbIndexes.add(rdbIndex);
            }
        }
        model.removeRow(selectedRow);
        databasePreviewPanel.updateSqlPreviewText();
    }

    @Override
    protected List<RdbIndex> getRowDatas() {
        return rdbIndexes;
    }

    @Override
    protected Object[][] getEditTableData() {
        List<DatabaseIndexNode> indexList = ObjectUtils.defaultIfNull(DataContext.getInstance(project).getSelectDbTable().getIndexList(), new ArrayList<>());
        List<DatabaseIndexNode> divIndexList = indexList.stream().filter(i -> !"PRIMARY".equals(i.getIndexName())).collect(Collectors.toList());
        List<DatabaseColumnNode> tableColumns = ObjectUtils.defaultIfNull(DataContext.getInstance(project).getSelectDbTable().getTableColumns(), new ArrayList<>());
        // fieldLength可能为0
        Map<String, Optional<Integer>> fieldLengthMap = tableColumns.stream().collect(Collectors.toMap(DatabaseColumnNode::getFieldName, c -> Optional.ofNullable(c.getFieldLength())));
        Object[][] data = new Object[divIndexList.size()][getColumnNames().length];
        for (int i = 0; i < divIndexList.size(); i++) {
            DatabaseIndexNode indexNode = divIndexList.get(i);
            if ("PRIMARY".equals(indexNode.getIndexName())) {
                continue;
            }
            RdbIndex rdbIndex = RdbIndex.builder()
                    .indexName(indexNode.getIndexName())
                    .indexType(indexNode.getIndexDesc())
                    .indexMethod(indexNode.getIndexType()).indexComment(indexNode.getIndexComment()).build();
            rdbIndex.setDbIndex(true);

            // indexFieldModelMap 从row=0开始
            List<RdbIndexField> rdbIndexFields = Arrays.stream(indexNode.getColumnSetStr().split(","))
                    .filter(fieldLengthMap::containsKey).map(field -> {
//                        Optional<Integer> optionalLength = fieldLengthMap.get(field);
//                        return RdbIndexField.builder().fieldName(field).fieldLength(optionalLength.orElse(0)).build();
                        return RdbIndexField.builder().fieldName(field).build();
                    }).collect(Collectors.toList());
            rdbIndex.setRdbIndexFields(rdbIndexFields);
            rdbIndex.setIndexField(getIndexFieldStr(rdbIndexFields));

            // setDbIndexBackup 必须放在最后
            rdbIndex.setDbIndexBackup(JSON.toJson(rdbIndex));
            rdbIndexes.add(rdbIndex);
            data[i] = new Object[]{rdbIndex.getIndexType(), rdbIndex.getIndexField(), rdbIndex.getIndexName(), rdbIndex.getIndexMethod(), rdbIndex.getIndexComment()};
        }
        return data;
    }

    @Override
    protected String[] getColumnNames() {
        return new String[]{"索引类型", "索引栏位", "索引名称", "索引方法", "注释"};
    }

    @Override
    protected void configColumnProperties(JTable table, TableCellRenderer centerRenderer) {
        TableCellRenderer leftAlignRender = JTableUtil.getLeftAlignRender();

        // 设置字段类型列为下拉框
        String[] indexTypeOptions = {"", "NORMAL", "UNIQUE", "FULLTEXT", "SPATIAL"};
        JComboBox<String> indexTypeComboBox = new ComboBox<>(indexTypeOptions);
        table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        ComboBoxCellEditor indexTypeComboBoxCellEditor = new ComboBoxCellEditor(indexTypeComboBox
                , RdbIndexPart.indexType, databasePreviewPanel, indexType -> updateIndexName((String) indexType));
        table.getColumnModel().getColumn(1).setCellEditor(indexTypeComboBoxCellEditor);

        // 索引栏位
        table.getColumnModel().getColumn(2).setCellRenderer(leftAlignRender);
        table.getColumnModel().getColumn(2).setCellEditor(new MultiSelectIndexFieldCellEditor(rdbFields
                , indexFieldSelectPanel, databasePreviewPanel, this));

        table.getColumnModel().getColumn(3).setCellRenderer(leftAlignRender);
        table.getColumnModel().getColumn(3).setCellEditor(new IndexTextCellEditor(RdbIndexPart.indexName
                , databasePreviewPanel));

        String[] indexMethodOptions = {"", "BTREE", "HASH"};
        JComboBox<String> indexMethodComboBox = new ComboBox<>(indexMethodOptions);
        table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(4).setCellEditor(new ComboBoxCellEditor(indexMethodComboBox
                , RdbIndexPart.indexMethod, databasePreviewPanel));

        table.getColumnModel().getColumn(5).setCellRenderer(leftAlignRender);
        table.getColumnModel().getColumn(5).setCellEditor(new IndexTextCellEditor(RdbIndexPart.indexComment
                , databasePreviewPanel));
    }

    @Override
    public void rowOrderChanged(RowNumberTableModel model, int from, int to) {

    }

    public void updateIndexName(String indexType) {
        // 索引类型
        JTable table = this.getTable();
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        int selectedRow = table.getSelectedRow();
        RdbIndex rdbIndex = ((RowNumberTableModel) table.getModel()).getRowData(selectedRow);
        // 索引字段
        List<RdbIndexField> indexFields = rdbIndex.getRdbIndexFields();
        List<String> indexFieldNames = ListUtils.defaultIfNull(indexFields, new ArrayList<>()).stream().map(RdbIndexField::getFieldName).collect(Collectors.toList());
        Object valueAt = model.getValueAt(selectedRow, 1);
        indexType = StringUtils.isNotEmpty(indexType) ? indexType : Objects.nonNull(valueAt) ? valueAt.toString() : StringUtils.EMPTY;
        if (CollectionUtils.isNotEmpty(indexFieldNames) && StringUtils.isNotEmpty(indexType)) {
            String indexName = indexNamePrefix(indexType) + "_" + String.join("_", indexFieldNames);
            rdbIndex.setIndexField(getIndexFieldStr(indexFields));
            rdbIndex.setIndexName(indexName);
            model.setValueAt(indexName, selectedRow, 3);

            databasePreviewPanel.updateSqlPreviewText();
        }
    }

    public static String getIndexFieldStr(List<RdbIndexField> indexFields) {
        if (indexFields != null) {
            StringBuilder builder = new StringBuilder();
            for (RdbIndexField indexField : indexFields) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                if (indexField.getFieldLength() > 0) {
                    builder.append(String.format("`%s`(%d)", indexField.getFieldName(), indexField.getFieldLength()));
                } else {
                    builder.append(String.format("`%s`", indexField.getFieldName()));
                }
            }
            return builder.toString();
        }

        return StringUtils.EMPTY;
    }

    private String indexNamePrefix(String indexType) {
        switch (indexType) {
            case "NORMAL":
                return "idx";
            case "UNIQUE":
                return "udx";
            case "FULLTEXT":
                return "ft";
            case "SPATIAL":
                return "spatial";
        }

        return null;
    }

}
