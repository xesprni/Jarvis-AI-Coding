package com.qihoo.finance.lowcode.gentracker.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.qihoo.finance.lowcode.design.entity.MySQLTableNode;
import com.qihoo.finance.lowcode.gentracker.tool.NameUtils;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 表信息
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
@Data
public class TableInfo {
    /**
     * 原始对象
     */
    @JsonIgnore
    private MySQLTableNode obj;

    /**
     * 原始对象（从实体生成）
     * <p>
     * Note: 实际类型是com.intellij.psi.PsiClass，为了避免velocity反射出现ClassNotFound，写为Object类型
     */
    @JsonIgnore
    private Object psiClassObj;

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
    private List<ColumnInfo> fullColumn;
    /**
     * 所有列(排除extends Entity)
     */
    private List<ColumnInfo> entityColumn;
    /**
     * 业务主键字段
     */
    private List<ColumnInfo> businessColumn;
    /**
     * 主键列
     */
    private List<ColumnInfo> pkColumn;
    /**
     * 其他列
     */
    private List<ColumnInfo> otherColumn;
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

    public String getBusinessColumnStr() {
        if (CollectionUtils.isNotEmpty(businessColumn)) {
            return businessColumn.stream().map(ColumnInfo::getName)
                    .map(fieldName -> NameUtils.getInstance().firstUpperCase(fieldName))
                    .collect(Collectors.joining("And"));
        }

        return "";
    }
}
