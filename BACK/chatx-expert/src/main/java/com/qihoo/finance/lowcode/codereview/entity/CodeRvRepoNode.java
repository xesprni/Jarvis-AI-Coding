package com.qihoo.finance.lowcode.codereview.entity;

import com.qihoo.finance.lowcode.common.entity.PlaceholderNode;
import com.qihoo.finance.lowcode.common.ui.base.FilterableTreeNode;
import com.qihoo.finance.lowcode.common.ui.base.PlaceTextNode;
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
public class CodeRvRepoNode extends FilterableTreeNode implements PlaceTextNode {
    private String depName;
    private String code;
    private String name;
    private String parentCode;
    private CodeRvNodeAttr nodeAttr = new CodeRvNodeAttr();

    public CodeRvRepoNode() {
        add(new PlaceholderNode());
    }

    @Override
    public String toString() {
//        return String.format("%s  (%s)", code, name);
        return name;
    }

    @Override
    public String getDescription() {
        return "  " + depName;
    }
}
