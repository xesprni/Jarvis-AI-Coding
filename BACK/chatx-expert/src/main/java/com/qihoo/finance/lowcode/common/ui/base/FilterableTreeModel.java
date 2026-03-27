package com.qihoo.finance.lowcode.common.ui.base;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

/**
 * FilterTreeModel
 *
 * @author fengjinfu-jk
 * date 2023/9/27
 * @version 1.0.0
 * @apiNote FilterTreeModel
 */
public class FilterableTreeModel extends DefaultTreeModel {
    protected boolean filterIsActive;

    public FilterableTreeModel(TreeNode root) {
        this(root, true);
    }

    public FilterableTreeModel(TreeNode root, boolean filterIsActive) {
        this(root, false, filterIsActive);
    }

    public FilterableTreeModel(TreeNode root, boolean asksAllowsChildren,
                               boolean filterIsActive) {
        super(root, asksAllowsChildren);
        this.filterIsActive = filterIsActive;
    }

    public void activateFilter(boolean newValue) {
        filterIsActive = newValue;
    }

    public boolean isActivatedFilter() {
        return filterIsActive;
    }

    public Object getChild(Object parent, int index) {
        if (filterIsActive) {
            if (parent instanceof FilterableTreeNode) {
                return ((FilterableTreeNode) parent).getChildAt(index,
                        filterIsActive);
            }
        }
        return ((TreeNode) parent).getChildAt(index);
    }

    public int getChildCount(Object parent) {
        if (filterIsActive) {
            if (parent instanceof FilterableTreeNode) {
                return ((FilterableTreeNode) parent).getChildCount(filterIsActive);
            }
        }
        return ((TreeNode) parent).getChildCount();
    }
}
