package com.qihoo.finance.lowcode.declarative.entity;

import com.qihoo.finance.lowcode.common.entity.dto.declarative.DDLInfo;
import com.qihoo.finance.lowcode.common.entity.dto.declarative.DiffType;
import com.qihoo.finance.lowcode.common.ui.base.FilterableTreeNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * DiffTableNode
 *
 * @author fengjinfu-jk
 * date 2024/4/25
 * @version 1.0.0
 * @apiNote DiffTableNode
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DiffTableNode extends FilterableTreeNode {
    private String databaseName;
    private DiffDatabaseNode database;
    private String tableName;
    private String dbDDL;
    private String declareDDL;
    private DiffType diffType = DiffType.NONE;
    private List<DDLInfo> diffDDLs = new ArrayList<>();

    @Override
    public String toString() {
        return tableName;
    }

    public DiffType getDiffType() {
        if (CollectionUtils.isEmpty(diffDDLs)) return DiffType.NONE;
        return diffType;
    }
}
