package com.qihoo.finance.lowcode.gentracker.entity;

import lombok.Data;

import java.util.List;

/**
 * 列配置分组
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
@Data
public class ColumnConfigGroup implements AbstractGroup<ColumnConfigGroup, ColumnConfig> {
    /**
     * 分组名称
     */
    private String name;
    /**
     * 元素对象
     */
    private List<ColumnConfig> elementList;
}
