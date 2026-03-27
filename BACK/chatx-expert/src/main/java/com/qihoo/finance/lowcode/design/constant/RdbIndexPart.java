package com.qihoo.finance.lowcode.design.constant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum RdbIndexPart {
    /**
     * 索引名称
     */
    indexName("indexName"),

    /**
     * 索引字段
     */
    indexField("indexField"),

    /**
     * 索引类型
     */
    indexType("indexType"),

    /**
     * 索引方法
     */
    indexMethod("indexMethod"),

    /**
     * 注释
     */
    indexComment("indexComment");

    private final String code;
}
