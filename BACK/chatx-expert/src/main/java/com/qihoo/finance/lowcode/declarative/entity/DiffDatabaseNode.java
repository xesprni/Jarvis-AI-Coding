package com.qihoo.finance.lowcode.declarative.entity;

import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.qihoo.finance.lowcode.common.ui.base.FilterableTreeNode;
import com.qihoo.finance.lowcode.common.ui.base.PlaceTextNode;
import com.qihoo.finance.lowcode.design.entity.DatabaseNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

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
public class DiffDatabaseNode extends FilterableTreeNode implements PlaceTextNode {
    private String databaseName;
    private DatabaseNode actualDatabase;

    @Override
    public String toString() {
        int childCount = getChildCount();
        if (childCount == 0) {
            return databaseName;
        }
        return databaseName + " (" + childCount + ")";
    }

    @Override
    public String getDescription() {
        if (Objects.isNull(actualDatabase)) return "请选择数据库";
        return "\uD83D\uDD17" + String.format("%s [%s]", actualDatabase.getName(), actualDatabase.getInstanceName());
    }


    SimpleTextAttributes RED = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.RED);
    SimpleTextAttributes GRAY = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.GRAY);

    @Override
    public SimpleTextAttributes getAttributes() {
        if (Objects.isNull(actualDatabase)) return RED;
        String db = actualDatabase.getName();
        if (StringUtils.isNotEmpty(databaseName) && !databaseName.equals(db)) {
            return RED;
        }
        return GRAY;
    }
}
