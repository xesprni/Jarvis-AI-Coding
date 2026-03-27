package com.qihoo.finance.lowcode.design.dto.rdb.index;

import com.qihoo.finance.lowcode.design.constant.RdbIndexPart;
import com.qihoo.finance.lowcode.design.dialog.DatabasePreviewPanel;
import com.qihoo.finance.lowcode.design.dto.rdb.RdbIndex;
import com.qihoo.finance.lowcode.design.dto.RowNumberTableModel;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import java.awt.Component;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class ComboBoxCellEditor extends DefaultCellEditor {

    private final JComboBox<String> comboBox;
    private final RdbIndexPart indexPart;
    private final DatabasePreviewPanel dbPreviewPanel;
    private RdbIndex rdbIndex;
    private Consumer<Object> editorListener;


    @SuppressWarnings("unchecked")
    public ComboBoxCellEditor(JComboBox<String> comboBox, RdbIndexPart indexPart, DatabasePreviewPanel dbPreviewPanel) {
        super(comboBox);
        this.comboBox = (JComboBox<String>)getComponent();
        this.indexPart = indexPart;
        this.dbPreviewPanel = dbPreviewPanel;
    }


    @SuppressWarnings("unchecked")
    public ComboBoxCellEditor(JComboBox<String> comboBox, RdbIndexPart indexPart, DatabasePreviewPanel dbPreviewPanel, Consumer<Object> editorListener) {
        super(comboBox);
        this.comboBox = (JComboBox<String>)getComponent();
        this.indexPart = indexPart;
        this.dbPreviewPanel = dbPreviewPanel;
        this.editorListener = editorListener;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        this.rdbIndex = ((RowNumberTableModel)table.getModel()).getRowData(row);
        this.comboBox.setSelectedItem(Optional.ofNullable(value).map(Object::toString).orElse(null));
        return comboBox;
    }

    @Override
    public Object getCellEditorValue() {
        String value = String.valueOf(this.comboBox.getSelectedItem());
        if (indexPart == RdbIndexPart.indexType) {
            // 从编辑器组件中获取用户输入的文本
            rdbIndex.setIndexType(value);
        } else if (indexPart == RdbIndexPart.indexMethod) {
            rdbIndex.setIndexMethod(value);
        }
        dbPreviewPanel.updateSqlPreviewText();
        Object selectedItem = this.comboBox.getSelectedItem();
        if(Objects.nonNull(editorListener)){
            editorListener.accept(selectedItem);
        }

        return selectedItem;
    }
}
