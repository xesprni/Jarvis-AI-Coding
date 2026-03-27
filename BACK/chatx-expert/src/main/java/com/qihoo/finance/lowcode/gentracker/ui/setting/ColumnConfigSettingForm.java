package com.qihoo.finance.lowcode.gentracker.ui.setting;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.ComboBoxTableRenderer;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import com.qihoo.finance.lowcode.gentracker.config.SettingsSettingStorage;
import com.qihoo.finance.lowcode.gentracker.entity.ColumnConfig;
import com.qihoo.finance.lowcode.gentracker.entity.ColumnConfigGroup;
import com.qihoo.finance.lowcode.gentracker.enums.ColumnConfigType;
import com.qihoo.finance.lowcode.gentracker.factory.CellEditorFactory;
import com.qihoo.finance.lowcode.gentracker.tool.CloneUtils;
import com.qihoo.finance.lowcode.gentracker.ui.component.GroupNameComponent;
import com.qihoo.finance.lowcode.gentracker.ui.component.TableComponent;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * 表字段设计界面
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
@Slf4j
public class ColumnConfigSettingForm implements Configurable, BaseSettings {
    private final JPanel mainPanel;
    /**
     * 列配置
     */
    private Map<String, ColumnConfigGroup> columnConfigGroupMap;
    /**
     * 当前分组名
     */
    private ColumnConfigGroup currColumnConfigGroup;
    /**
     * 表格组件
     */
    private TableComponent<ColumnConfig> tableComponent;
    /**
     * 分组操作组件
     */
    private GroupNameComponent<ColumnConfig, ColumnConfigGroup> groupNameComponent;

    public ColumnConfigSettingForm() {
        this.mainPanel = new JPanel(new BorderLayout());
    }

    private void initTable() {
        // 第一列，类型
        String[] columnConfigTypeNames = Stream.of(ColumnConfigType.values()).map(ColumnConfigType::name).toArray(String[]::new);
        TableCellEditor typeEditor = CellEditorFactory.createComboBoxEditor(false, columnConfigTypeNames);
        TableCellRenderer typeRenderer = new ComboBoxTableRenderer<>(columnConfigTypeNames);
        TableComponent.Column<ColumnConfig> typeColumn = new TableComponent.Column<>("type", item -> item.getType().name(), (entity, val) -> entity.setType(ColumnConfigType.valueOf(val)), typeEditor, typeRenderer);
        // 第二列标题
        TableCellEditor titleEditor = CellEditorFactory.createTextFieldEditor();
        TableComponent.Column<ColumnConfig> titleColumn = new TableComponent.Column<>("title", ColumnConfig::getTitle, ColumnConfig::setTitle, titleEditor, null);
        // 第三列选项
        TableCellEditor selectValueEditor = CellEditorFactory.createTextFieldEditor();
        TableComponent.Column<ColumnConfig> selectValueColumn = new TableComponent.Column<>("selectValue", ColumnConfig::getSelectValue, ColumnConfig::setSelectValue, selectValueEditor, null);
        List<TableComponent.Column<ColumnConfig>> columns = Arrays.asList(typeColumn, titleColumn, selectValueColumn);

        // 表格初始化
        this.tableComponent = new TableComponent<>(columns, this.currColumnConfigGroup.getElementList(), ColumnConfig.class);
        this.mainPanel.add(this.tableComponent.createPanel(), BorderLayout.CENTER);
    }

    private void initGroupName() {

        // 切换分组操作
        Consumer<ColumnConfigGroup> switchGroupOperator = typeColumnConfigGroupMap -> {
            this.currColumnConfigGroup = typeColumnConfigGroupMap;
            refreshUiVal();
        };

        this.groupNameComponent = new GroupNameComponent<>(switchGroupOperator, this.columnConfigGroupMap);
        this.mainPanel.add(groupNameComponent.getPanel(), BorderLayout.NORTH);
    }

    private void initPanel() {
        this.loadSettingsStore(getSettingsStorage());
        // 初始化表格
        this.initTable();
        this.initGroupName();
    }

    @Override
    public String getDisplayName() {
        return "Column Config";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return getDisplayName();
    }

    @Override
    public @Nullable JComponent createComponent() {
        try {
            this.initPanel();
        } catch (Exception e) {
            // ignore
            log.error("ignore: {}", e.getMessage());
        }
        return mainPanel;
    }

    @Override
    public boolean isModified() {
        return !this.columnConfigGroupMap.equals(getSettingsStorage().getColumnConfigGroupMap())
                || !getSettingsStorage().getCurrColumnConfigGroupName().equals(this.currColumnConfigGroup.getName());
    }

    @Override
    public void apply() {
        getSettingsStorage().setColumnConfigGroupMap(this.columnConfigGroupMap);
        getSettingsStorage().setCurrColumnConfigGroupName(this.currColumnConfigGroup.getName());
        // 保存包后重新加载配置
        this.loadSettingsStore(getSettingsStorage());
    }

    /**
     * 加载配置信息
     *
     * @param settingsStorage 配置信息
     */
    @Override
    public void loadSettingsStore(SettingsSettingStorage settingsStorage) {
        // 复制配置，防止篡改
        this.columnConfigGroupMap = CloneUtils.cloneByJson(settingsStorage.getColumnConfigGroupMap(), new TypeReference<Map<String, ColumnConfigGroup>>() {
        });
        this.currColumnConfigGroup = this.columnConfigGroupMap.get(settingsStorage.getCurrColumnConfigGroupName());
        if (this.currColumnConfigGroup == null) {
            this.currColumnConfigGroup = this.columnConfigGroupMap.get(GlobalDict.DEFAULT_GROUP_NAME);
        }
        this.refreshUiVal();
    }

    private void refreshUiVal() {
        if (this.tableComponent != null) {
            this.tableComponent.setDataList(this.currColumnConfigGroup.getElementList());
        }
        if (this.groupNameComponent != null) {
            this.groupNameComponent.setGroupMap(this.columnConfigGroupMap);
            this.groupNameComponent.setCurrGroupName(this.currColumnConfigGroup.getName());
        }
    }
}
