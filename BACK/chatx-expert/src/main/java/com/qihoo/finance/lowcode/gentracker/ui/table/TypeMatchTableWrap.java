package com.qihoo.finance.lowcode.gentracker.ui.table;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.JBColor;
import com.qihoo.finance.lowcode.common.ui.table.BaseJTableWrap;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.design.constant.FieldTypeMatch;
import com.qihoo.finance.lowcode.design.entity.DatabaseColumnNode;
import com.qihoo.finance.lowcode.gentracker.entity.ColumnInfo;
import com.qihoo.finance.lowcode.gentracker.entity.TableInfo;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * CodeRvCommentTable
 *
 * @author fengjinfu-jk
 * date 2023/11/2
 * @version 1.0.0
 * @apiNote CodeRvCommentTable
 */
public class TypeMatchTableWrap implements BaseJTableWrap {
    private final Project project;
    private final TableInfo dbTable;

    public static final int FIELD_NAME = 0;
    public static final int FIELD_COMMENT = 1;
    public static final int SQL_TYPE = 2;
    public static final int JDBC_TYPE = 3;
    public static final int JAVA_TYPE = 4;

    public TypeMatchTableWrap(Project project, TableInfo dbTable) {
        this.project = project;
        this.dbTable = dbTable;
    }

    @Override
    public String[] getTableHeaders() {
        return new String[]{"字段", "注释", "SQL Server 类型", "JDBC 类型 (java.sql.Types)", "Java 语言类型"};
    }

    @Override
    public Object[][] getDefaultTableData() {
        List<Object[]> columnInfos = new ArrayList<>();
        for (ColumnInfo column : dbTable.getFullColumn()) {
            DatabaseColumnNode columnNode = column.getObj();
            columnInfos.add(new Object[]{columnNode.getFieldName(), columnNode.getFieldComment(), columnNode.getFieldType(), column.getJdbcType(), column.getType()});
        }

        return columnInfos.toArray(Object[][]::new);
    }

    @Override
    public void addRow(ActionEvent e, JTable table) {

    }

    @Override
    public void configColumnProperties(JTable table, DefaultTableCellRenderer centerRenderer) {
        // "字段", "注释", "SQL Server 类型", "JDBC 类型 (java.sql.Types)", "Java 语言类型"
        DefaultTableCellRenderer waringRenderer = new WarningTableCellRenderer();
        table.getColumnModel().getColumn(FIELD_NAME).setCellRenderer(waringRenderer);
        table.getColumnModel().getColumn(FIELD_NAME).setCellEditor(new TextCellEditor(false, JTextField.LEFT));

        table.getColumnModel().getColumn(FIELD_COMMENT).setCellRenderer(waringRenderer);
        table.getColumnModel().getColumn(FIELD_COMMENT).setCellEditor(new TextCellEditor(false, JTextField.LEFT));

        table.getColumnModel().getColumn(SQL_TYPE).setCellRenderer(waringRenderer);
        table.getColumnModel().getColumn(SQL_TYPE).setCellEditor(new TextCellEditor(false, JTextField.LEFT));

        ComboBox<String> jdbcTypeCombobox = new ComboBox<>(FieldTypeMatch.DEFAULT_JDBC_TYPE_LIST);
        table.getColumnModel().getColumn(JDBC_TYPE).setCellEditor(new ComboBoxCellEditor(jdbcTypeCombobox, SwingConstants.LEFT));
        AutoCompleteDecorator.decorate(jdbcTypeCombobox);
        table.getColumnModel().getColumn(JDBC_TYPE).setCellRenderer(new WarningTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Object valueAt = table.getValueAt(row, FIELD_NAME);
                for (ColumnInfo columnInfo : dbTable.getFullColumn()) {
                    if (columnInfo.getObj().getFieldName().equals(valueAt)) {
                        columnInfo.setJdbcType(Objects.nonNull(value) ? value.toString() : FieldTypeMatch.DEFAULT_JDBC_TYPE);
                    }
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });

        ComboBox<String> javaTypeCombobox = new ComboBox<>(FieldTypeMatch.DEFAULT_JAVA_TYPE_LIST);
        AutoCompleteDecorator.decorate(javaTypeCombobox);
        table.getColumnModel().getColumn(JAVA_TYPE).setCellEditor(new ComboBoxCellEditor(javaTypeCombobox, SwingConstants.LEFT));
        table.getColumnModel().getColumn(JAVA_TYPE).setCellRenderer(new WarningTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Object valueAt = table.getValueAt(row, FIELD_NAME);
                for (ColumnInfo columnInfo : dbTable.getFullColumn()) {
                    if (columnInfo.getObj().getFieldName().equals(valueAt)) {
                        String javaType = Objects.nonNull(value) ? value.toString() : FieldTypeMatch.DEFAULT_JAVA_TYPE;
                        columnInfo.setType(javaType);
                        columnInfo.setShortType(ColumnInfo.convertShortType(javaType));
                    }
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });
    }

    static class WarningTableCellRenderer extends DefaultTableCellRenderer {
        public WarningTableCellRenderer() {
            setHorizontalAlignment(JLabel.LEFT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Object typeValue = table.getValueAt(row, JAVA_TYPE);
            if (FieldTypeMatch.WARNING_TYPE.contains(typeValue.toString())) {
                setForeground(Icons.WARNING_COLOR);
            } else {
                setForeground(JBColor.foreground());
            }

            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    //------------------------------------------------------------------------------------------------------------------
}
