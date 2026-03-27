package com.qihoo.finance.lowcode.design.dto.rdb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RdbField {
    private String fieldName;
    private String fieldType;
    private Integer fieldLength;
    private Integer fieldPrecision;
    private boolean notNull;
    private boolean pk;
    private boolean autoIncr;
    private boolean unsigned;
    private Object fieldDefault;
    private String fieldComment;
    private String fieldOrder;

    /** 是否是DB中的已有字段 */
    private boolean existsInDb;
    /** DB中字段的备份，用于比对用户具体改了啥 */
    private String dbColumnBackup;
}
