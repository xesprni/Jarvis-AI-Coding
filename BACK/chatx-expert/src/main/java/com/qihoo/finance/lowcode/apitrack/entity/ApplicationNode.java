package com.qihoo.finance.lowcode.apitrack.entity;

import com.qihoo.finance.lowcode.common.entity.PlaceholderNode;
import com.qihoo.finance.lowcode.common.ui.base.FilterableTreeNode;
import com.qihoo.finance.lowcode.common.ui.base.PlaceTextNode;
import lombok.Getter;
import lombok.Setter;

/**
 * 加载中
 *
 * @author fengjinfu-jk
 * date 2023/8/3
 * @version 1.0.0
 * @apiNote LoadingNode
 */
@Getter
@Setter
public class ApplicationNode extends FilterableTreeNode implements PlaceTextNode {
    private String depName;
    private String name;
    private String code;
    private String appCode;

    private Long projectId;

    private String projectName;

    private String token;

    private boolean editable;

    public ApplicationNode() {
        add(new PlaceholderNode());
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", appCode, projectName);
    }

    @Override
    public String getDescription() {
        if (!editable) {
            return String.format(" %s「只读」", depName);
        }
        return "  " + depName;
    }
}
