package com.qihoo.finance.lowcode.design.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * 表信息节点
 *
 * @author fengjinfu-jk
 * date 2023/8/3
 * @version 1.0.0
 * @apiNote DataBaseMutableTreeNode
 */

@Getter
@Setter
public class MongoCollectionNode extends AbstractTableNode {
    public MongoCollectionNode() {
    }

    @Override
    public String toString() {
        return getTableName();
    }
}
