package com.qihoo.finance.lowcode.design.constant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum FieldType {
    /**
     * 字段名称
     */
    fieldName("fieldName"),

    /**
     * 字段类型
     */
    fieldType("fieldType"),

    /**
     * 字段长度
     */
    fieldLength("fieldLength"),

    /**
     * 字段精度
     */
    fieldPrecision("fieldPrecision"),

    /**
     * 是否为空
     */
    isNotNull("isNotNull"),


    /**
     * 是否自增
     */
    isAutoIncr("isAutoIncr"),


    /**
     * 是否无符号
     */
    isUnsigned("isUnsigned"),


    /**
     * 是否主键
     */
    isPK("isPK"),

    /**
     * 默认值
     */
    fieldDefaults("fieldDefaults"),


    /**
     * 字段注释
     */
    fieldComment("fieldComment"),

    fieldNo("fieldNo"),
    ;

    private final String code;
}