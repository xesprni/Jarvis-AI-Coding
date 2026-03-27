package com.qihoo.finance.lowcode.design.dto.rdb;

import com.qihoo.finance.lowcode.design.dto.rdb.RdbIndexField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RdbIndex {
    private String indexName;
    private String indexField;
    private String indexType;
    private String indexMethod;
    private String indexComment;
    private boolean dbIndex;
    private String dbIndexBackup;
    /** 解析好的Index涉及的字段，数据用于编辑的弹框 */
    private List<RdbIndexField> rdbIndexFields;
}
