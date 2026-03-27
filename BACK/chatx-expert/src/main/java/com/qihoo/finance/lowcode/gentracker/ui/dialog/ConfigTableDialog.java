package com.qihoo.finance.lowcode.gentracker.ui.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBoxTableRenderer;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.BooleanTableCellEditor;
import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.qihoo.finance.lowcode.design.constant.FieldTypeMatch;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import com.qihoo.finance.lowcode.gentracker.entity.ColumnConfig;
import com.qihoo.finance.lowcode.gentracker.entity.TableInfo;
import com.qihoo.finance.lowcode.gentracker.factory.CellEditorFactory;
import com.qihoo.finance.lowcode.gentracker.service.TableInfoSettingsService;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.gentracker.tool.CurrGroupUtils;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import com.qihoo.finance.lowcode.gentracker.tool.StringUtils;
import com.qihoo.finance.lowcode.gentracker.ui.base.ConfigTableModel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * 表配置窗口
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
public class ConfigTableDialog extends DialogWrapper {

    /**
     * 主面板
     */
    private final JPanel mainPanel;
    /**
     * 表信息对象
     */
    private TableInfo tableInfo;
    private final Project project;


    public ConfigTableDialog(Project project) {
        super(project);
        this.project = project;
        this.mainPanel = new JPanel(new BorderLayout());
        this.initPanel();
    }

    private void initPanel() {
        init();
        this.tableInfo = TableInfoSettingsService.getInstance().getTableInfo(DataContext.getInstance(project).getSelectDbTable());
        setTitle("[" + this.tableInfo.getObj().getTableName() + "]已有表字段信息不允许修改, 可以新增自定义字段, 配置不会对实际数据库中的表结构产生影响");
        ConfigTableModel model = new ConfigTableModel(this.tableInfo);
        JBTable table = new JBTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        int totalWidth = 0;

        // 配置列编辑器
        TableColumn nameColumn = table.getColumn("name");
        nameColumn.setCellEditor(CellEditorFactory.createTextFieldEditor());
        nameColumn.setPreferredWidth(150);
        totalWidth += 150;
        TableColumn typeColumn = table.getColumn("type");
        typeColumn.setCellRenderer(new ComboBoxTableRenderer<>(FieldTypeMatch.DEFAULT_JAVA_TYPE_LIST));
        typeColumn.setCellEditor(CellEditorFactory.createComboBoxEditor(true, FieldTypeMatch.DEFAULT_JAVA_TYPE_LIST));
        typeColumn.setPreferredWidth(200);
        totalWidth += 200;
        TableColumn commentColumn = table.getColumn("comment");
        commentColumn.setCellEditor(CellEditorFactory.createTextFieldEditor());
        commentColumn.setMinWidth(200);
        totalWidth += 200;
        // 其他附加列
        for (ColumnConfig columnConfig : CurrGroupUtils.getCurrColumnConfigGroup().getElementList()) {
            TableColumn column = table.getColumn(columnConfig.getTitle());
            switch (columnConfig.getType()) {
                case TEXT:
                    column.setCellEditor(CellEditorFactory.createTextFieldEditor());
                    column.setMinWidth(120);
                    totalWidth += 120;
                    break;
                case SELECT:
                    if (StringUtils.isEmpty(columnConfig.getSelectValue())) {
                        column.setCellEditor(CellEditorFactory.createTextFieldEditor());
                    } else {
                        String[] split = columnConfig.getSelectValue().split(",");
                        ArrayList<String> list = new ArrayList<>(Arrays.asList(split));
                        // 添加一个空值作为默认值
                        list.add(0, "");
                        split = list.toArray(new String[0]);
                        column.setCellRenderer(new ComboBoxTableRenderer<>(split));
                        column.setCellEditor(CellEditorFactory.createComboBoxEditor(false, split));
                    }
                    column.setMinWidth(100);
                    totalWidth += 100;
                    break;
                case BOOLEAN:
                    column.setCellRenderer(new BooleanTableCellRenderer());
                    column.setCellEditor(new BooleanTableCellEditor());
                    column.setMinWidth(60);
                    totalWidth += 60;
                    break;
                default:
                    break;
            }
        }

        final ToolbarDecorator decorator = ToolbarDecorator.createDecorator(table);
        this.mainPanel.add(decorator.createPanel(), BorderLayout.CENTER);
        this.mainPanel.setMinimumSize(new Dimension(totalWidth, Math.max(300, totalWidth / 3)));
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return this.mainPanel;
    }

    @Override
    protected void doOKAction() {
        // 保存信息
        TableInfoSettingsService.getInstance().saveTableInfo(tableInfo);
        super.doOKAction();
    }

}
