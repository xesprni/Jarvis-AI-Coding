package com.qihoo.finance.lowcode.gentracker.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.qihoo.finance.lowcode.design.entity.DatabaseColumnNode;
import lombok.Data;

import java.util.Map;

/**
 * 列信息
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
@Data
public class ColumnInfo {
    /**
     * 原始对象
     */
    @JsonIgnore
    private DatabaseColumnNode obj;
    /**
     * 名称
     */
    private String name;
    /**
     * 注释
     */
    private String comment;
    /**
     * 全类型
     */
    private String type;
    /**
     * 短类型
     */
    private String shortType;
    /**
     * jdbc类型
     */
    private String jdbcType;
    /**
     * 标记是否为自定义附加列
     */
    private Boolean custom;
    /**
     * 扩展数据
     */
    private Map<String, Object> ext;

    public static String convertShortType(String type) {
        return type.substring(type.lastIndexOf(".") + 1);
    }
}
