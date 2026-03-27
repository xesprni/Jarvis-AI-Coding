package com.qihoo.finance.lowcode.gentracker.dto;

import com.intellij.openapi.ui.Messages;
import com.qihoo.finance.lowcode.design.entity.DatabaseNode;
import com.qihoo.finance.lowcode.design.entity.MySQLTableNode;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import com.qihoo.finance.lowcode.gentracker.entity.TableInfo;
import lombok.Data;

import javax.swing.tree.TreeNode;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * 表格信息设置传输对象
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
@Data
public class TableInfoSettingsDTO {
    private Map<String, TableInfoDTO> tableInfoMap;

    public TableInfoSettingsDTO() {
        this.tableInfoMap = new TreeMap<>();
    }

    private String generateKey(MySQLTableNode dbTable) {
        // 递归添加3层名称作为key，第一层为表名、第二层为名命空间名称、第三层为数据库名
        StringBuilder builder = new StringBuilder(dbTable.getTableName());
        TreeNode parent = dbTable.getParent();

        while (Objects.nonNull(parent) && parent instanceof DatabaseNode) {
            DatabaseNode node = (DatabaseNode) parent;
            Object name = node.getName();

            // 添加分割符
            builder.insert(0, ".");
            builder.insert(0, name);
            parent = parent.getParent();
        }

        return builder.toString();
    }

    /**
     * 读表信息
     *
     * @param dbTable 数据库表
     * @return {@link TableInfo}
     */
    @SuppressWarnings("Duplicates")
    public TableInfo readTableInfo(MySQLTableNode dbTable) {
        String key = generateKey(dbTable);
        TableInfoDTO dto = this.tableInfoMap.get(key);
        // 表可能新增了字段，需要重新合并保存
        dto = new TableInfoDTO(dto, dbTable);
        this.tableInfoMap.put(key, dto);
        return dto.toTableInfo(dbTable);
    }

    /**
     * 读内存表信息
     *
     * @param dbTable 数据库表
     * @return {@link TableInfo}
     */
    @SuppressWarnings("Duplicates")
    public TableInfo readMemoryTableInfo(MySQLTableNode dbTable) {
        String key = generateKey(dbTable);
        TableInfoDTO dto = this.tableInfoMap.get(key);
        // 表可能新增了字段，需要重新合并保存
        this.tableInfoMap.put(key, dto);
        return dto.toTableInfo(dbTable);
    }

    /**
     * 保存表信息
     *
     * @param tableInfo 表信息
     */
    public void saveTableInfo(TableInfo tableInfo) {
        if (tableInfo == null) {
            return;
        }
        MySQLTableNode dbTable = tableInfo.getObj();
        String key;
        if (dbTable != null) {
            key = generateKey(dbTable);
        } else {
            Messages.showInfoMessage(tableInfo.getName() + "表配置信息保存失败", GlobalDict.TITLE_INFO);
            return;
        }
        this.tableInfoMap.put(key, TableInfoDTO.valueOf(tableInfo));
    }

    /**
     * 重置表信息
     *
     * @param dbTable 数据库表
     */
    public void resetTableInfo(MySQLTableNode dbTable) {
        String key = generateKey(dbTable);
        this.tableInfoMap.put(key, new TableInfoDTO(null, dbTable));
    }

    /**
     * 删除表信息
     *
     * @param dbTable 数据库表
     */
    public void removeTableInfo(MySQLTableNode dbTable) {
        String key = generateKey(dbTable);
        this.tableInfoMap.remove(key);
    }
}
