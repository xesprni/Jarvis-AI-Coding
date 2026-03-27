package com.qihoo.finance.lowcode.design.dto.rdb.index;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.JPanelUtils;
import com.qihoo.finance.lowcode.design.dialog.DatabaseIndexFieldSelectPanel;
import com.qihoo.finance.lowcode.design.dialog.DatabaseIndexPanel;
import com.qihoo.finance.lowcode.design.dialog.DatabasePreviewPanel;
import com.qihoo.finance.lowcode.design.dto.RowNumberTableModel;
import com.qihoo.finance.lowcode.design.dto.rdb.RdbField;
import com.qihoo.finance.lowcode.design.dto.rdb.RdbIndex;
import com.qihoo.finance.lowcode.design.dto.rdb.RdbIndexField;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.List;
import java.util.Optional;

/**
 * 文本框 + "..."按钮，点击按钮后弹框让用户选择索引需要包含的字段
 */
public class MultiSelectIndexFieldCellEditor extends AbstractCellEditor implements TableCellEditor {

    private final MultiSelectField multiSelectField;
    private final JButton button;
    private RdbIndex rdbIndex;
    private final List<RdbField> rdbFields;
    /**
     * 索引字段选择弹框
     */
    private final DatabaseIndexFieldSelectPanel indexFieldSelectPanel;
    private final DatabasePreviewPanel dbPreviewPanel;
    private final DatabaseIndexPanel databaseIndexPanel;


    public MultiSelectIndexFieldCellEditor(List<RdbField> rdbFields, DatabaseIndexFieldSelectPanel indexFieldSelectPanel
            , DatabasePreviewPanel dbPreviewPanel, DatabaseIndexPanel databaseIndexPanel) {
        multiSelectField = new MultiSelectField();
        multiSelectField.getTextField().setEditable(false);

        // 添加按钮来触发对话框
        button = new JButton();
        button.setIcon(Icons.scaleToWidth(Icons.SETTING, 16));
        button.setToolTipText("设置索引字段");
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setPreferredSize(new Dimension(22, 22));
        button.addActionListener(e -> showPopup());

        this.rdbFields = rdbFields;
        this.indexFieldSelectPanel = indexFieldSelectPanel;
        this.dbPreviewPanel = dbPreviewPanel;
        this.databaseIndexPanel = databaseIndexPanel;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        this.rdbIndex = ((RowNumberTableModel) table.getModel()).getRowData(row);
        // 设置编辑器当前选中的值
        JPanel panel = new JPanel(new BorderLayout());
        multiSelectField.setValue(Optional.ofNullable(value).map(Object::toString).orElse(null));
        panel.add(multiSelectField, BorderLayout.CENTER);
        panel.add(button, BorderLayout.EAST);
        return panel; // 返回组合后的编辑器组件
    }

    @Override
    public Object getCellEditorValue() {
        // 返回选中的项，以逗号分隔
        List<RdbIndexField> indexFields = rdbIndex.getRdbIndexFields();
        return DatabaseIndexPanel.getIndexFieldStr(indexFields);
    }

    private void showPopup() {
        new IndexFieldDialog(ProjectUtils.getCurrProject()).show();
    }

    public class IndexFieldDialog extends DialogWrapper {
        private JPanel dialogPanel;

        public IndexFieldDialog(@Nullable Project project) {
            super(project);

            initComponent();
            initEvent();
            initSize();

            init();
            setModal(true);
            setTitle("字段配置");

            setOKButtonText("确定");
            setCancelButtonText("取消");
        }

        private void initComponent() {
            indexFieldSelectPanel.setRdbIndexFields(rdbIndex.getRdbIndexFields());
            indexFieldSelectPanel.setRdbFields(rdbFields);
            // 创建JPanel用于容纳JTable和按钮
            dialogPanel = (JPanel) indexFieldSelectPanel.createPanel();
        }

        private void initEvent() {

        }

        private void initSize() {
            JPanelUtils.setSize(dialogPanel, new Dimension(500, 300));
        }

        @Override
        protected void doOKAction() {
            databaseIndexPanel.updateIndexName(null);
            stopCellEditing();
            super.doOKAction();
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            return dialogPanel;
        }
    }
}
