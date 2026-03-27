package com.qihoo.finance.lowcode.design.entity;

import lombok.Getter;
import lombok.Setter;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Objects;

/**
 * 节点
 *
 * @author fengjinfu-jk
 * date 2023/8/3
 * @version 1.0.0
 * @apiNote DataBaseMutableTreeNode
 */
@Getter
@Setter
public class DatabaseFolderNode extends DefaultMutableTreeNode {
    private String name;

    @Override
    public String toString() {
        return String.format("%s  (%s)", name, Objects.nonNull(children) ? children.size() : 0);
    }
}
