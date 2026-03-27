package com.qihoo.finance.lowcode.design.listener;

import com.intellij.openapi.project.Project;
import com.qihoo.finance.lowcode.common.listener.TreeSelectionListenerChain;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.common.util.JTreeLoadingUtils;
import com.qihoo.finance.lowcode.design.entity.DatabaseNode;
import com.qihoo.finance.lowcode.design.entity.MongoDatabaseNode;
import com.qihoo.finance.lowcode.design.util.DatabaseDesignUtils;
import com.qihoo.finance.lowcode.gentracker.listener.DbTreeGenListener;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * 数据库树监听
 *
 * @author fengjinfu-jk
 * date 2023/8/7
 * @version 1.0.0
 * @apiNote DbTreeListener
 */
public class MongoTreeListener extends TreeSelectionListenerChain {

    private final JTree tree;

    public MongoTreeListener(Project project, JTree tree) {
        super(project);
        this.tree = tree;
        DataContext.getInstance(project).setDbTree(tree);
    }

    @Override
    public TreeSelectionListener nextListener() {
        return new DbTreeGenListener(project, tree);
    }

    @Override
    public void handlerValueChanged(TreeSelectionEvent e) {
        DataContext dataContext = DataContext.getInstance(project);
        dataContext.setDbTree(tree);

        boolean async = !dataContext.isMustSyncLoadDbTree();
        DefaultMutableTreeNode node = currentNode(e);

        // 数据库层级展开, 查询表信息
        if (node instanceof MongoDatabaseNode) {
            JTreeLoadingUtils.loading(async, tree, node, () -> DatabaseDesignUtils.queryMongoCollectionNodes((MongoDatabaseNode) node));
        }
    }

    //------------------------------------------------------------------------------------------------------------------
}
