package com.qihoo.finance.lowcode.gentracker.entity;

import lombok.Data;

/**
 * 调试字段实体类
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
@Data
public class DebugField {
    /**
     * 字段名
     */
    private String name;
    /**
     * 字段类型
     */
    private Class<?> type;
    /**
     * 字段值
     */
    private String value;
}
