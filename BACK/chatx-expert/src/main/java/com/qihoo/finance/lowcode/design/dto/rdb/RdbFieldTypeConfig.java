package com.qihoo.finance.lowcode.design.dto.rdb;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder
public class RdbFieldTypeConfig {

    private String fieldTypeName;
    private boolean needFieldLength;
    private boolean needFieldPrecision;
    private String defaultJdbcType;
    private String defaultJavaType;

}
