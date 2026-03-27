package com.qihoo.finance.lowcode.design.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.qihoo.finance.lowcode.common.entity.PlaceholderNode;
import com.qihoo.finance.lowcode.common.ui.base.PlaceTextNode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

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
public class MySQLTableNode extends AbstractTableNode implements PlaceTextNode {
    /**
     * 表名前缀
     */
    private String preName;
    /**
     * 注释
     */
    private String tableComment;
    /**
     * 引擎
     */
    private String engine;
    /**
     * 编码
     */
    private String charset;
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
    /**
     * 所有列
     */
    private List<DatabaseIndexNode> indexList;

    public List<DatabaseColumnNode> getTableColumns() {
        List<DatabaseColumnNode> tableColumns = new ArrayList<>();
        Iterator<TreeNode> treeNodeIterator = this.children().asIterator();
        while (treeNodeIterator.hasNext()) {
            TreeNode treeNode = treeNodeIterator.next();
            if (treeNode instanceof DatabaseColumnNode) {
                tableColumns.add((DatabaseColumnNode) treeNode);
            }
        }

        return tableColumns;
    }

    public MySQLTableNode() {
        add(new PlaceholderNode());
    }

    @Override
    public String toString() {
        return getTableName();
    }

    @Override
    public String getDescription() {
        if (StringUtils.isNotEmpty(tableComment)) {
            return String.format("%s", tableComment);
        }
        return null;
    }
}
