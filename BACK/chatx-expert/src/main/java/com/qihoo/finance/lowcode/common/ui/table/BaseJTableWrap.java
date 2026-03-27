package com.qihoo.finance.lowcode.common.ui.table;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.table.JBTable;
import com.qihoo.finance.lowcode.common.util.EditorComponentUtils;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.JTableUtils;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * TableSetting
 *
 * @author fengjinfu-jk
 * date 2023/9/4
 * @version 1.0.0
 * @apiNote TableSetting
 */
public interface BaseJTableWrap {

    Map<String, Boolean> booleanStrMap = new HashMap<>() {{
        put("0", false);
        put("1", true);
    }};
    Map<Integer, Boolean> booleanNumMap = new HashMap<>() {{
        put(0, false);
        put(1, true);
    }};

    default boolean isEdit() {
        return false;
    }

    String[] getTableHeaders();

    Object[][] getDefaultTableData();

    default Object[][] getEditTableData() {
        return new Object[0][];
    }

    void addRow(ActionEvent e, JTable table);

    default Object[][] getTableData() {
        return isEdit() ? getEditTableData() : getDefaultTableData();
    }

    void configColumnProperties(JTable table, DefaultTableCellRenderer centerRenderer);

    default DefaultTableModel getTableModel() {
        return new DefaultTableModel(getTableData(), getTableHeaders());
    }

    default JBTable createTable() {
        JBTable table = new JBTable(getTableModel());

        // 设置表头居中对齐
        JTableHeader header = table.getTableHeader();
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        header.setDefaultRenderer(headerRenderer);
        headerRenderer.setHorizontalAlignment(JLabel.CENTER);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        configColumnProperties(table, centerRenderer);

        return table;
    }

    default JBTable createStyleTable(int maxWidth, Icon columnIcon) {
        JBTable table = new JBTable(getTableModel());
        // table风格
        configTableStyle(table, getTableHeaders());
        // 设置表头
        JTableUtils.setTableHeaderRenderer(table, columnIcon);
        // 宽度自适应
        JTableUtils.fitTableWidth(table, maxWidth);

        // columnProperties
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        configColumnProperties(table, centerRenderer);

        return table;
    }

    private void configTableStyle(JTable table, String[] headers) {
        // 设置表格
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.LEFT);
        for (int i = 0; i < headers.length; i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            column.setCellRenderer(centerRenderer);
        }

        // 背景
        table.setBackground(EditorComponentUtils.BACKGROUND);
    }

    class TextRenderer extends DefaultTableCellRenderer {

        public TextRenderer() {
            setHorizontalAlignment(JLabel.CENTER);
        }

        public TextRenderer(int alignment) {
            setHorizontalAlignment(alignment);
        }
    }

    class CheckBoxRenderer extends JCheckBox implements TableCellRenderer {

        public CheckBoxRenderer() {
            setHorizontalAlignment(JLabel.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof String && booleanStrMap.containsKey(value)) {
                setSelected(booleanStrMap.get(value));
            } else if (value instanceof Integer && booleanNumMap.containsKey(value)) {
                setSelected(booleanNumMap.get(value));
            } else {
                setSelected((Boolean) ObjectUtils.defaultIfNull(value, false));
            }

            return this;
        }
    }

    class CheckBoxEditor extends AbstractCellEditor implements TableCellEditor {

        private final JCheckBox checkBox;

        public JCheckBox getCheckBox() {
            return checkBox;
        }

        public CheckBoxEditor() {
            this.checkBox = new JCheckBox();
            checkBox.setHorizontalAlignment(JCheckBox.CENTER);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (value instanceof String && booleanStrMap.containsKey(value)) {
                checkBox.setSelected(booleanStrMap.get(value));
            } else if (value instanceof Integer && booleanNumMap.containsKey(value)) {
                checkBox.setSelected(booleanNumMap.get(value));
            } else if (value instanceof Boolean) {
                checkBox.setSelected((Boolean) value);
            } else {
                checkBox.setSelected(false);
            }

            return checkBox;
        }

        @Override
        public Object getCellEditorValue() {
            return checkBox.isSelected();
        }
    }


    class PositiveIntegerCellEditor extends AbstractCellEditor implements TableCellEditor {

        private final JTextField textField;

        PositiveIntegerCellEditor() {
            this.textField = new JTextField();
            textField.setHorizontalAlignment(JTextField.CENTER);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.textField.setText(Objects.nonNull(value) ? value.toString() : StringUtils.EMPTY);
            return this.textField;
        }

        @Override
        public Object getCellEditorValue() {
            return this.textField.getText();
        }
    }


    class PositiveIntegerDocument extends javax.swing.text.PlainDocument {
        @Override
        public void insertString(int offs, String str, javax.swing.text.AttributeSet a) throws javax.swing.text.BadLocationException {
            if (str == null) {
                return;
            }
            try {
                int value = Integer.parseInt(str);
                if (value >= 0) {
                    super.insertString(offs, str, a);
                }
            } catch (NumberFormatException e) {
                // Ignore non-integer input
            }
        }
    }

    class ComboBoxCellEditor extends AbstractCellEditor implements TableCellEditor {

        private final ComboBox<String> comboBox;

        public ComboBoxCellEditor(ComboBox<String> comboBox) {
            this.comboBox = comboBox;
            this.comboBox.setEditable(false);
            this.comboBox.setRenderer(new DefaultListCellRenderer() {
                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    // 设置选项居中对齐
                    label.setHorizontalAlignment(SwingConstants.CENTER);
                    return label;
                }
            });
        }

        public ComboBoxCellEditor(ComboBox<String> comboBox, int alignment) {
            this.comboBox = comboBox;
            this.comboBox.setEditable(false);
            this.comboBox.setRenderer(new DefaultListCellRenderer() {
                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    // 设置选项居中对齐
                    label.setHorizontalAlignment(alignment);
                    return label;
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.comboBox.setSelectedItem(Objects.nonNull(value) ? value.toString() : StringUtils.EMPTY);
            return comboBox;
        }

        @Override
        public Object getCellEditorValue() {
            ComboBoxEditor editor = comboBox.getEditor();
            Object item = editor.getItem();
            item = Objects.nonNull(item) ? item : comboBox.getSelectedItem();
            return Objects.nonNull(item) ? item.toString() : StringUtils.EMPTY;
        }
    }

    @Getter
    class TextCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JTextField textField;
        private Consumer<String> getCellEditorValue;

        public TextCellEditor() {
            textField = new JTextField();
            textField.setHorizontalAlignment(JTextField.CENTER);
        }

        public TextCellEditor(boolean editable, boolean center) {
            textField = new JTextField();
            textField.setEditable(editable);
            if (center) {
                textField.setHorizontalAlignment(JTextField.CENTER);
            }
        }

        public TextCellEditor(boolean editable, boolean center, Consumer<String> getCellEditorValue) {
            textField = new JTextField();
            textField.setEditable(editable);
            if (center) {
                textField.setHorizontalAlignment(JTextField.CENTER);
            }

            this.getCellEditorValue = getCellEditorValue;
        }

        public TextCellEditor(boolean editable) {
            textField = new JTextField();
            textField.setHorizontalAlignment(JTextField.CENTER);
            textField.setEditable(editable);
        }

        public TextCellEditor(boolean editable, int alignment) {
            textField = new JTextField();
            textField.setHorizontalAlignment(alignment);
            textField.setEditable(editable);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            textField.setText(Objects.nonNull(value) ? value.toString() : StringUtils.EMPTY);
            return textField;
        }

        @Override
        public Object getCellEditorValue() {
            String text = textField.getText();
            if (Objects.nonNull(getCellEditorValue)) {
                getCellEditorValue.accept(text);
            }

            return text;
        }
    }


    class DeleteButtonCellRenderer implements TableCellRenderer {
        private final JButton delButton;

        public DeleteButtonCellRenderer() {
            this.delButton = new JButton();
        }

        public DeleteButtonCellRenderer(JButton delButton) {
            this.delButton = delButton;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            delButton.setIcon(Icons.scaleToWidth(Icons.DELETE, 16));
            delButton.setBorderPainted(false);
            delButton.setContentAreaFilled(false);
            delButton.setPreferredSize(new Dimension(22, 22));
            return delButton;
        }

    }

    class DeleteButtonCellEditor extends DefaultCellEditor {
        public DeleteButtonCellEditor() {
            super(new JTextField());
            this.setClickCountToStart(1);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            int deleteRow = Math.max(row, 0);
            if (table.getRowCount() > deleteRow) {
                model.removeRow(deleteRow);
            }

            return null;
        }

    }

    class JPanelCellEditor extends DefaultCellEditor {
        private final JPanel panel;

        public JPanelCellEditor(JPanel panel) {
            super(new JTextField());
            this.setClickCountToStart(1);

            this.panel = panel;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            return panel;
        }
    }

    class JPanelCellRenderer implements TableCellRenderer {
        private final JPanel panel;

        public JPanelCellRenderer(JPanel panel) {
            this.panel = panel;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return panel;
        }
    }
}
