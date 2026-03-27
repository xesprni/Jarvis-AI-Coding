package com.qihoo.finance.lowcode.common.ui.base;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Enumeration;

/**
 * DefaultFilterTreeNode
 *
 * @author fengjinfu-jk
 * date 2023/9/27
 * @version 1.0.0
 * @apiNote DefaultFilterTreeNode
 */
public class FilterableTreeNode extends DefaultMutableTreeNode {
    protected boolean isVisible;
    /** 是否收藏（置顶） */
    protected boolean collect;

    public FilterableTreeNode() {
        this(null);
    }

    public FilterableTreeNode(Object userObject) {
        this(userObject, true, true);
    }

    public FilterableTreeNode(Object userObject, boolean allowsChildren,
                              boolean isVisible) {
        super(userObject, allowsChildren);
        this.isVisible = isVisible;
    }

    public TreeNode getChildAt(int index, boolean filterIsActive) {
        if (!filterIsActive) {
            return super.getChildAt(index);
        }
        if (children == null) {
            throw new ArrayIndexOutOfBoundsException("node has no children");
        }

        int realIndex = -1;
        int visibleIndex = -1;
        Enumeration e = children.elements();
        while (e.hasMoreElements()) {
            Object n = e.nextElement();
            if (n instanceof FilterableTreeNode node) {
                if (node.isVisible()) {
                    visibleIndex++;
                }
            } else {
                visibleIndex++;
            }
            realIndex++;
            if (visibleIndex == index) {
                return (TreeNode) children.elementAt(realIndex);
            }
        }

//        throw new ArrayIndexOutOfBoundsException("index unmatched");
        return (TreeNode) children.elementAt(index);
    }

    public int getChildCount(boolean filterIsActive) {
        if (!filterIsActive) {
            return super.getChildCount();
        }
        if (children == null) {
            return 0;
        }

        int count = 0;
        Enumeration e = children.elements();
        while (e.hasMoreElements()) {
            Object n = e.nextElement();
            if (n instanceof FilterableTreeNode node) {
                if (node.isVisible()) {
                    count++;
                }
            } else {
                count++;
            }
        }

        return count;
    }

    public void setVisible(boolean visible) {
        this.isVisible = visible;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public boolean isCollect() {
        return collect;
    }

    public void setCollect(boolean collect) {
        this.collect = collect;
    }
}
