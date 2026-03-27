package com.qihoo.finance.lowcode.design.dto.rdb;

import lombok.*;

/**
 * 索引配置窗口，选择索引包含的字段
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RdbIndexField {
    private String fieldName;
    private int fieldLength;
}
