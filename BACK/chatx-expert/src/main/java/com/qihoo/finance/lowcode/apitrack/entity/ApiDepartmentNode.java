package com.qihoo.finance.lowcode.apitrack.entity;

import com.qihoo.finance.lowcode.common.ui.base.FilterableTreeNode;
import lombok.Getter;
import lombok.Setter;

/**
 * 命名空间信息节点
 *
 * @author fengjinfu-jk
 * date 2023/8/3
 * @version 1.0.0
 * @apiNote DataBaseMutableTreeNode
 */
@Getter
@Setter
public class ApiDepartmentNode extends FilterableTreeNode {
    private String code;
    private String name;
    @Override
    public String toString() {
//        return String.format("%s  (%s)", code, name);
        return name;
    }
}
