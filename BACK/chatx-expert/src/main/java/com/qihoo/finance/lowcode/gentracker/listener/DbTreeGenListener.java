package com.qihoo.finance.lowcode.gentracker.listener;

import com.intellij.openapi.project.Project;
import com.qihoo.finance.lowcode.common.listener.TreeSelectionListenerChain;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.design.entity.DatabaseNode;
import com.qihoo.finance.lowcode.design.entity.MongoCollectionNode;
import com.qihoo.finance.lowcode.design.entity.MongoDatabaseNode;
import com.qihoo.finance.lowcode.design.entity.MySQLTableNode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 数据库树节点选择监听器
 *
 * @author fengjinfu-jk
 * date 2023/8/3
 * @version 1.0.0
 * @apiNote DbTreeSelectionListener
 */
public class DbTreeGenListener extends TreeSelectionListenerChain {

    private final JTree tree;

    public DbTreeGenListener(Project project, JTree tree) {
        super(project);
        this.tree = tree;
    }

    @Override
    public void handlerValueChanged(TreeSelectionEvent e) {
        TreePath selectionPath = e.getNewLeadSelectionPath();
        selectionPath = ObjectUtils.defaultIfNull(selectionPath, e.getOldLeadSelectionPath());
        Object lastPathComponent = selectionPath.getLastPathComponent();

        DataContext dataContext = DataContext.getInstance(project);
        if (lastPathComponent instanceof DatabaseNode) {
            DatabaseNode nameSpaceNode = (DatabaseNode) lastPathComponent;
            dataContext.setSelectDatabase(nameSpaceNode);

            dataContext.setSelectDbTable(null);
            dataContext.setDbTableList(new ArrayList<>());
        }
        if (lastPathComponent instanceof MongoDatabaseNode) {
            MongoDatabaseNode nameSpaceNode = (MongoDatabaseNode) lastPathComponent;
            dataContext.setSelectMongoDatabase(nameSpaceNode);
        }

        if (lastPathComponent instanceof MySQLTableNode || lastPathComponent instanceof MongoCollectionNode) {
            DefaultMutableTreeNode table = (DefaultMutableTreeNode) lastPathComponent;
            // select table
            if (lastPathComponent instanceof MySQLTableNode) {
                // MySQL
                dataContext.setSelectDbTable((MySQLTableNode) lastPathComponent);
                // select table list
                TreePath[] selectionPaths = tree.getSelectionPaths();
                if (Objects.nonNull(selectionPaths)) {
                    List<MySQLTableNode> dbTables = new ArrayList<>();
                    dbTables.add((MySQLTableNode) table);

                    if (CollectionUtils.isNotEmpty(dbTables)) {
                        dataContext.setDbTableList(dbTables);
                    }
                }
                // select nameSpaceNode
                TreeNode parent = table.getParent();
                if (Objects.nonNull(parent)) {
                    DatabaseNode nameSpaceNode = (DatabaseNode) parent;
                    dataContext.setSelectDatabase(nameSpaceNode);
                }
            } else {
                // MongoDB
                dataContext.setSelectMongoCollection((MongoCollectionNode) lastPathComponent);
                // select nameSpaceNode
                TreeNode parent = table.getParent();
                if (Objects.nonNull(parent)) {
                    MongoDatabaseNode nameSpaceNode = (MongoDatabaseNode) parent;
                    dataContext.setSelectMongoDatabase(nameSpaceNode);
                }
            }


        }
    }
}
