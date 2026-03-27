package com.qihoo.finance.lowcode.declarative.listener;

import com.qihoo.finance.lowcode.declarative.entity.DiffDatabaseNode;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

/**
 * DiffTreeMouseListener
 *
 * @author fengjinfu-jk
 * date 2024/4/26
 * @version 1.0.0
 * @apiNote DiffTreeMouseListener
 */
public class DiffTreeMouseListener extends MouseAdapter {
    private final JTree tree;

    public DiffTreeMouseListener(JTree tree) {
        this.tree = tree;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // 在这里处理双击事件
        TreePath path = tree.getPathForLocation(e.getX(), e.getY());
        if (Objects.isNull(path)) return;
        if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (node instanceof DiffDatabaseNode) {
                if (tree.isExpanded(path)) {
                    tree.collapsePath(path);
                } else {
                    tree.expandPath(path);
                }
            }
        }
    }
}
