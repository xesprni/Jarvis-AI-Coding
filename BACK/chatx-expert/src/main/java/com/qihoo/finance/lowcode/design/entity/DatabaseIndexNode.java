package com.qihoo.finance.lowcode.design.entity;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * 表字段节点
 *
 * @author fengjinfu-jk
 * date 2023/8/3
 * @version 1.0.0
 * @apiNote DataBaseMutableTreeNode
 */
@Getter
@Setter
public class DatabaseIndexNode extends DefaultMutableTreeNode {
    private String indexName;
    private String tableSchema;
    private String tableName;
    private Long nonUnique;
    private String indexField;
    private String columnSetStr;
    private String indexType;
    private String indexDesc;
    private String indexComment;
}
