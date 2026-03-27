package com.qihoo.finance.lowcode.common.listener;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.ExpandVetoException;

/**
 * DefaultTreeExpansionListener
 * 这里要触发selection操作，去调用接口查数据
 * @author fengjinfu-jk
 * date 2023/8/31
 * @version 1.0.0
 * @apiNote DefaultTreeExpansionListener
 */
public class DefaultTreeWillExpansionListener implements TreeWillExpandListener {
    private final JTree tree;

    public DefaultTreeWillExpansionListener(JTree tree) {
        this.tree = tree;
    }

    @Override
    public void treeWillExpand(TreeExpansionEvent event) {
        tree.setSelectionPath(event.getPath());
    }

    @Override
    public void treeWillCollapse(TreeExpansionEvent event) {

    }
}
