package com.qihoo.finance.lowcode.design.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.qihoo.finance.lowcode.common.ui.base.FilterableTreeNode;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

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
public class AbstractTableNode extends FilterableTreeNode {

    /**
     * 库名
     */
    private String database;

    /**
     * 表名
     */
    private String tableName;
    @JsonFormat(
            locale = "zh",
            timezone = "GMT+8",
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private Date createTime;
    @JsonFormat(
            locale = "zh",
            timezone = "GMT+8",
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private Date updateTime;
}
