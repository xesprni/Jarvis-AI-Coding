package com.qihoo.finance.lowcode.gentracker.dto;

import com.qihoo.finance.lowcode.design.entity.DatabaseColumnNode;
import com.qihoo.finance.lowcode.design.entity.MySQLTableNode;
import com.qihoo.finance.lowcode.gentracker.entity.ColumnInfo;
import com.qihoo.finance.lowcode.gentracker.entity.TableInfo;
import com.qihoo.finance.lowcode.gentracker.tool.CollectionUtil;
import com.qihoo.finance.lowcode.gentracker.tool.JSON;
import com.qihoo.finance.lowcode.gentracker.tool.NameUtils;
import com.qihoo.finance.lowcode.gentracker.tool.StringUtils;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 表格信息传输对象
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
@Data
@NoArgsConstructor
public class TableInfoDTO {

    public TableInfoDTO(TableInfoDTO dto, MySQLTableNode dbTable) {
        this(dbTable);
        merge(dto, this);
    }

    private TableInfoDTO(MySQLTableNode dbTable) {
        this.name = NameUtils.getInstance().getClassName(dbTable.getTableName());
        this.preName = dbTable.getPreName();
        this.comment = dbTable.getTableComment();
        this.templateGroupName = "";
        this.savePackageName = "";
        this.savePath = "";
        this.saveModelName = "";
        this.fullColumn = new ArrayList<>();
        // 处理所有列
        for (DatabaseColumnNode column : dbTable.getTableColumns()) {
            this.fullColumn.add(new ColumnInfoDTO(column));
        }
    }

    private static void merge(TableInfoDTO oldData, TableInfoDTO newData) {
        if (oldData == null || CollectionUtil.isEmpty(oldData.getFullColumn())) {
            return;
        }
        if (!StringUtils.isEmpty(oldData.getPreName())) {
            newData.preName = oldData.getPreName();
        }
        if (!StringUtils.isEmpty(oldData.getTemplateGroupName())) {
            newData.templateGroupName = oldData.getTemplateGroupName();
        }
        if (!StringUtils.isEmpty(oldData.getSavePackageName())) {
            newData.savePackageName = oldData.getSavePackageName();
        }
        if (!StringUtils.isEmpty(oldData.getSavePath())) {
            newData.savePath = oldData.getSavePath();
        }
        if (!StringUtils.isEmpty(oldData.getSaveModelName())) {
            newData.saveModelName = oldData.getSaveModelName();
        }
        List<String> allNewColumnNames = newData.getFullColumn().stream().map(ColumnInfoDTO::getName).collect(Collectors.toList());
        // 列出旧的顺序，并删除已经不存在的列，不包括自定义列
        List<ColumnInfoDTO> oldSequenceColumn = oldData.getFullColumn().stream().filter(item -> allNewColumnNames.contains(item.getName()) || Boolean.TRUE.equals(item.getCustom())).collect(Collectors.toList());
        // 尽可能的保留原始顺序(把自定义列按原始位置插入)
        Map<String, String> nameMap = new HashMap<>(oldSequenceColumn.size());
        for (int i = 0; i < oldSequenceColumn.size(); i++) {
            ColumnInfoDTO columnInfo = oldSequenceColumn.get(i);
            if (columnInfo.getCustom()) {
                // 如果原本是自定义列，现在变成了数据库列，则忽略调原本的自定义列
                if (allNewColumnNames.contains(columnInfo.getName())) {
                    continue;
                }
                // 获取当前自定义列的前一个名称
                String beforeName = "";
                if (i > 0) {
                    beforeName = oldSequenceColumn.get(i - 1).getName();
                }
                nameMap.put(beforeName, columnInfo.getName());
            }
        }
        // 将自定义列按顺序插入到新表中
        nameMap.forEach((k, v) -> {
            if (StringUtils.isEmpty(k)) {
                allNewColumnNames.add(0, v);
            } else {
                for (int i = 0; i < allNewColumnNames.size(); i++) {
                    if (allNewColumnNames.get(i).equals(k)) {
                        allNewColumnNames.add(i + 1, v);
                        return;
                    }
                }
            }
        });
        // 按顺序依次重写数据
        Map<String, ColumnInfoDTO> oldColumnMap = oldData.getFullColumn().stream().collect(Collectors.toMap(ColumnInfoDTO::getName, v -> v));
        Map<String, ColumnInfoDTO> newColumnMap = newData.getFullColumn().stream().collect(Collectors.toMap(ColumnInfoDTO::getName, v -> v));
        List<ColumnInfoDTO> tmpList = new ArrayList<>();
        for (String name : allNewColumnNames) {
            ColumnInfoDTO newColumnInfo = newColumnMap.get(name);
            if (newColumnInfo == null) {
                newColumnInfo = oldColumnMap.get(name);
                if (newColumnInfo == null) {
                    throw new NullPointerException("找不到列信息");
                }
                tmpList.add(newColumnInfo);
                continue;
            }
            ColumnInfoDTO oldColumnInfo = oldColumnMap.get(name);
            if (oldColumnInfo == null) {
                tmpList.add(newColumnInfo);
                continue;
            }
            // 需要进行合并操作
            newColumnInfo.setExt(oldColumnInfo.getExt());
            if (StringUtils.isEmpty(newColumnInfo.getComment())) {
                newColumnInfo.setComment(oldColumnInfo.getComment());
            }
            tmpList.add(newColumnInfo);
        }
        // list数据替换
        newData.getFullColumn().clear();
        newData.getFullColumn().addAll(tmpList);

        newData.setEntityColumn(oldData.getEntityColumn());
        newData.setBusinessColumn(oldData.getBusinessColumn());
    }


    /**
     * 表名（首字母大写）
     */
    private String name;
    /**
     * 表名前缀
     */
    private String preName;
    /**
     * 注释
     */
    private String comment;
    /**
     * 模板组名称
     */
    private String templateGroupName;
    /**
     * 所有列
     */
    private List<ColumnInfoDTO> fullColumn;
    /**
     * 所有列(排除extends Entity)
     */
    private List<ColumnInfoDTO> entityColumn;
    /**
     * 业务主键字段
     */
    private List<ColumnInfoDTO> businessColumn;
    /**
     * 保存的包名称
     */
    private String savePackageName;
    /**
     * 保存路径
     */
    private String savePath;
    /**
     * 保存的model名称
     */
    private String saveModelName;

    @SuppressWarnings("unchecked")
    public TableInfo toTableInfo(MySQLTableNode dbTable) {
        TableInfo tableInfo = simpleTableInfo();
        tableInfo.setObj(dbTable);

        // 列
        Map<String, DatabaseColumnNode> nameToObj = dbTable.getTableColumns().stream().collect(Collectors.toMap(column -> NameUtils.getInstance().getJavaName(column.getFieldName()), Function.identity()));

        for (ColumnInfoDTO dto : this.getFullColumn()) {
            ColumnInfo columnInfo = convertColumn(dto, nameToObj.get(dto.getName()));
            tableInfo.getFullColumn().add(columnInfo);
            if (columnInfo.getObj() != null && dto.getIdentity()) {
                tableInfo.getPkColumn().add(columnInfo);
            } else {
                tableInfo.getOtherColumn().add(columnInfo);
            }
        }

        if (CollectionUtils.isNotEmpty(this.getEntityColumn())) {
            for (ColumnInfoDTO dto : this.getEntityColumn()) {
                ColumnInfo columnInfo = convertColumn(dto, nameToObj.get(dto.getName()));
                tableInfo.getEntityColumn().add(columnInfo);
            }
        }

        if (CollectionUtils.isNotEmpty(this.getBusinessColumn())) {
            for (ColumnInfoDTO dto : this.getBusinessColumn()) {
                ColumnInfo columnInfo = convertColumn(dto, nameToObj.get(dto.getName()));
                tableInfo.getBusinessColumn().add(columnInfo);
            }
        }

        return tableInfo;
    }

    private static ColumnInfo convertColumn(ColumnInfoDTO dto, DatabaseColumnNode obj) {
        ColumnInfo columnInfo = new ColumnInfo();
        columnInfo.setObj(obj);
        columnInfo.setName(dto.getName());
        columnInfo.setType(dto.getType());
        // 最后一节为短类型
        String[] split = dto.getType().split("\\.");
        columnInfo.setShortType(split[split.length - 1]);
        columnInfo.setJdbcType(dto.getJdbcType());
        columnInfo.setComment(dto.getComment());
        columnInfo.setCustom(dto.getCustom());
        columnInfo.setExt(StringUtils.isEmpty(dto.getExt()) ? new HashMap<>() : JSON.parse(dto.getExt(), HashMap.class));

        return columnInfo;
    }

    public static TableInfoDTO valueOf(TableInfo tableInfo) {
        TableInfoDTO dto = new TableInfoDTO();
        dto.setName(tableInfo.getName());
        dto.setTemplateGroupName(tableInfo.getTemplateGroupName());
        dto.setSavePath(tableInfo.getSavePath());
        dto.setPreName(tableInfo.getPreName());
        dto.setComment(tableInfo.getComment());
        dto.setSavePackageName(tableInfo.getSavePackageName());
        dto.setSaveModelName(tableInfo.getSaveModelName());
        dto.setFullColumn(new ArrayList<>());
        dto.setEntityColumn(new ArrayList<>());
        dto.setBusinessColumn(new ArrayList<>());

        // 处理列
        if (CollectionUtils.isNotEmpty(tableInfo.getFullColumn())) {
            for (ColumnInfo columnInfo : tableInfo.getFullColumn()) {
                dto.getFullColumn().add(covertColumnDTO(columnInfo));
            }
        }

        if (CollectionUtils.isNotEmpty(tableInfo.getEntityColumn())) {
            for (ColumnInfo columnInfo : tableInfo.getEntityColumn()) {
                dto.getEntityColumn().add(covertColumnDTO(columnInfo));
            }
        }

        if (CollectionUtils.isNotEmpty(tableInfo.getBusinessColumn())) {
            for (ColumnInfo columnInfo : tableInfo.getBusinessColumn()) {
                dto.getBusinessColumn().add(covertColumnDTO(columnInfo));
            }
        }

        return dto;
    }

    private static ColumnInfoDTO covertColumnDTO(ColumnInfo columnInfo) {
        ColumnInfoDTO columnInfoDTO = new ColumnInfoDTO();
        columnInfoDTO.setName(columnInfo.getName());
        columnInfoDTO.setType(columnInfo.getType());
        columnInfoDTO.setJdbcType(columnInfo.getJdbcType());
        columnInfoDTO.setExt(JSON.toJson(columnInfo.getExt()));
        columnInfoDTO.setCustom(columnInfo.getCustom());
        columnInfoDTO.setComment(columnInfo.getComment());

        return columnInfoDTO;
    }

    private TableInfo simpleTableInfo() {
        TableInfo tableInfo = new TableInfo();
        tableInfo.setName(this.getName());
        tableInfo.setPreName(this.getPreName());
        tableInfo.setTemplateGroupName(this.getTemplateGroupName());
        tableInfo.setSavePackageName(this.getSavePackageName());
        tableInfo.setSavePath(this.getSavePath());
        tableInfo.setComment(this.getComment());
        tableInfo.setSaveModelName(this.getSaveModelName());

        tableInfo.setFullColumn(new ArrayList<>());
        tableInfo.setPkColumn(new ArrayList<>());
        tableInfo.setOtherColumn(new ArrayList<>());
        tableInfo.setEntityColumn(new ArrayList<>());
        tableInfo.setBusinessColumn(new ArrayList<>());

        return tableInfo;
    }
}
