package com.qihoo.finance.lowcode.gentracker.entity;

import lombok.Data;

/**
 * 调试方法实体类
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
@Data
public class DebugMethod {
    /**
     * 方法名
     */
    private String name;
    /**
     * 方法描述
     */
    private String desc;
    /**
     * 执行方法得到的值
     */
    private Object value;
}
