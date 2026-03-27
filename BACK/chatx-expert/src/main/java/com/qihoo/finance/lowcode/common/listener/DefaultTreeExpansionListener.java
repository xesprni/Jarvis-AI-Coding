package com.qihoo.finance.lowcode.common.listener;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import java.awt.*;

/**
 * DefaultTreeExpansionListener
 *
 * @author fengjinfu-jk
 * date 2023/8/31
 * @version 1.0.0
 * @apiNote DefaultTreeExpansionListener
 */
public class DefaultTreeExpansionListener implements TreeExpansionListener {
    private final JTree tree;

    public DefaultTreeExpansionListener(JTree tree) {
        this.tree = tree;
    }

    @Override
    public void treeExpanded(TreeExpansionEvent event) {
        // 当节点展开时，重新计算 JScrollPane 的大小
        adjustScrollPaneSize(event, tree);
    }

    @Override
    public void treeCollapsed(TreeExpansionEvent event) {
        // 当节点折叠时，重新计算 JScrollPane 的大小
        adjustScrollPaneSize(event, tree);
    }

    private void adjustScrollPaneSize(TreeExpansionEvent event, JTree tree) {
        Dimension preferredSize = new Dimension();
        preferredSize.height = 100 + (tree.getRowCount() * tree.getRowHeight());
        tree.setPreferredSize(preferredSize);
    }
}
