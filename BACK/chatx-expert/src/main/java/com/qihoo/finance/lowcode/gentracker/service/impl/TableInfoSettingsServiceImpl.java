package com.qihoo.finance.lowcode.gentracker.service.impl;

import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.qihoo.finance.lowcode.design.entity.MySQLTableNode;
import com.qihoo.finance.lowcode.gentracker.dto.TableInfoSettingsDTO;
import com.qihoo.finance.lowcode.gentracker.entity.TableInfo;
import com.qihoo.finance.lowcode.gentracker.service.TableInfoSettingsService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * xml配置映射
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
@State(name = "GenerateTableSetting", storages = @Storage("generate-table-setting.xml"))
public class TableInfoSettingsServiceImpl implements TableInfoSettingsService {

    private TableInfoSettingsDTO tableInfoSettings = new TableInfoSettingsDTO();

    @Nullable
    @Override
    public TableInfoSettingsDTO getState() {
        return tableInfoSettings;
    }

    @Override
    public void loadState(@NotNull TableInfoSettingsDTO state) {
        this.tableInfoSettings = state;
    }

    @Override
    public TableInfo getTableInfo(MySQLTableNode dbTable) {
        return Objects.requireNonNull(getState()).readTableInfo(dbTable);
    }

    @Override
    public TableInfo getMemoryTableInfo(MySQLTableNode dbTable) {
        return Objects.requireNonNull(getState()).readMemoryTableInfo(dbTable);
    }

    /**
     * 保存表信息
     *
     * @param tableInfo 表信息
     */
    @Override
    public void saveTableInfo(TableInfo tableInfo) {
        Objects.requireNonNull(getState()).saveTableInfo(tableInfo);
    }

    /**
     * 重置表信息
     *
     * @param dbTable 数据库表
     */
    @Override
    public void resetTableInfo(MySQLTableNode dbTable) {
        Objects.requireNonNull(getState()).resetTableInfo(dbTable);
    }

    /**
     * 删除表信息
     *
     * @param dbTable 数据库表
     */
    @Override
    public void removeTableInfo(MySQLTableNode dbTable) {
        Objects.requireNonNull(getState()).removeTableInfo(dbTable);
    }
}
