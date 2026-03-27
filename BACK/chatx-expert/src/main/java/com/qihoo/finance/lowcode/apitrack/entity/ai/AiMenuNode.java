package com.qihoo.finance.lowcode.apitrack.entity.ai;

import com.qihoo.finance.lowcode.common.ui.base.FilterableTreeNode;
import com.qihoo.finance.lowcode.common.ui.base.PlaceTextNode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AiMenuNode extends FilterableTreeNode implements PlaceTextNode {

    private Long id;
    private String name;
    private String desc;
    private String projectId;

    private List<AiApiNode> list;

    @Override
    public String toString() {
        return name;
    }

    public String getToolTipText() {
        return desc;
    }

    @Override
    public String getDescription() {
        return desc;
    }
}
