package com.qihoo.finance.lowcode.declarative.listener;

import com.intellij.openapi.externalSystem.util.Order;
import com.qihoo.finance.lowcode.declarative.entity.DiffDatabaseNode;
import com.qihoo.finance.lowcode.declarative.entity.DiffTableNode;
import com.qihoo.finance.lowcode.declarative.ui.DeclareDiffPanel;
import com.qihoo.finance.lowcode.design.entity.DatabaseNode;
import org.apache.commons.lang3.ObjectUtils;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

/**
 * DiffTreeSelectionListener
 *
 * @author fengjinfu-jk
 * date 2023/8/7
 * @version 1.0.0
 * @apiNote DiffTreeSelectionListener
 */
@Order(value = Integer.MAX_VALUE)
public class DiffTreeSelectionListener implements TreeSelectionListener {
    private final DeclareDiffPanel diffPanel;
    private final JComboBox<DatabaseNode> relateActualDB;

    public DiffTreeSelectionListener(DeclareDiffPanel diffPanel, JComboBox<DatabaseNode> relateActualDB) {
        this.diffPanel = diffPanel;
        this.relateActualDB = relateActualDB;
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = currentNode(e);
        if (node instanceof DiffDatabaseNode databaseNode) {
            relateActualDB.setSelectedItem(databaseNode.getActualDatabase());
        }
        if (node instanceof DiffTableNode tableNode) {
            // 更新右侧diff窗口
            relateActualDB.setSelectedItem(tableNode.getDatabase().getActualDatabase());
            diffPanel.setTableNode(tableNode);
            diffPanel.repaint();
        }
    }

    protected DefaultMutableTreeNode currentNode(TreeSelectionEvent e) {
        TreePath selectionPath = e.getNewLeadSelectionPath();
        selectionPath = ObjectUtils.defaultIfNull(selectionPath, e.getOldLeadSelectionPath());
        Object component = selectionPath.getLastPathComponent();
        return (DefaultMutableTreeNode) component;
    }
    //------------------------------------------------------------------------------------------------------------------
}
