package com.qihoo.finance.lowcode.apitrack.entity;

import com.qihoo.finance.lowcode.common.entity.PlaceholderNode;
import com.qihoo.finance.lowcode.common.ui.base.FilterableTreeNode;
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
public class ApiGroupNode extends FilterableTreeNode {
    private ApplicationNode applicationNode;
    private String name;
    private String code;
    private String appCode;

    private String id;
    private String className;
    private Long projectId;
    private String projectToken;

    private String autoGenPackage;
    private String extensionPackage;
    private String classDesc;

    private boolean editable;

    public ApiGroupNode() {
        add(new PlaceholderNode());
    }

    @Override
    public String toString() {
        return name;
    }
}
