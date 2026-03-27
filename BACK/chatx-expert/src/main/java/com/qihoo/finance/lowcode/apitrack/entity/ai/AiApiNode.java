package com.qihoo.finance.lowcode.apitrack.entity.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.qihoo.finance.lowcode.common.ui.base.FilterableTreeNode;
import com.qihoo.finance.lowcode.common.ui.base.PlaceTextNode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiApiNode extends FilterableTreeNode implements PlaceTextNode {

    private Long id;
    private String catid;
    private String title;
    private String desc;
    private String method;
    @JsonProperty("path")
    private String apiPath;

    @Override
    public String toString() {
        return title;
    }

    public String getToolTipText() {
        return String.format("[%s] %s<br/>%s", method, apiPath, desc);
    }

    @Override
    public String getDescription() {
        return desc;
    }
}
