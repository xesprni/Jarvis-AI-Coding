package com.qihoo.finance.lowcode.apitrack.entity.ai;

import com.qihoo.finance.lowcode.common.entity.PlaceholderNode;
import com.qihoo.finance.lowcode.common.ui.base.FilterableTreeNode;
import com.qihoo.finance.lowcode.common.ui.base.PlaceTextNode;
import lombok.Getter;
import lombok.Setter;

/**
 * yapi project node
 */
@Getter
@Setter
public class AiProjectNode extends FilterableTreeNode implements PlaceTextNode {

    public AiProjectNode() {
        // 这里要给子节点加个占位符，不然没有展开按钮
        add(new PlaceholderNode());
    }

    private Long id;
    private String name;
    private String desc;
    private String groupName;
    private String toolTipText;
    private int addTime;

    @Override
    public String toString() {
        return name;
    }

    @Override
    public String getDescription() {
        return desc;
    }

}
