package com.qihoo.finance.lowcode.apitrack.listener;

import com.intellij.openapi.project.Project;
import com.qihoo.finance.lowcode.apitrack.entity.ApiGroupNode;
import com.qihoo.finance.lowcode.apitrack.entity.ApiNode;
import com.qihoo.finance.lowcode.apitrack.entity.ApplicationNode;
import com.qihoo.finance.lowcode.apitrack.util.ApiDesignUtils;
import com.qihoo.finance.lowcode.common.listener.TreeSelectionListenerChain;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.common.util.JTreeLoadingUtils;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

/**
 * ApiTreeListener
 *
 * @author fengjinfu-jk
 * date 2023/8/31
 * @version 1.0.0
 * @apiNote ApiTreeListener
 */
public class ApiTreeListener extends TreeSelectionListenerChain {
    private final JTree tree;

    public ApiTreeListener(Project project, JTree tree) {
        super(project);
        this.tree = tree;
        DataContext.getInstance(project).setApiTree(tree);
    }

    @Override
    public void handlerValueChanged(TreeSelectionEvent e) {
        DataContext dataContext = DataContext.getInstance(project);
        dataContext.setApiTree(tree);

        DefaultMutableTreeNode node = currentNode(e);

        // application层级展开, 查询api目录
        if (node instanceof ApplicationNode) {
            // application
            JTreeLoadingUtils.loading(true, tree, node, () -> ApiDesignUtils.apiCategoryList((ApplicationNode) node), catList -> {
                setSelectApiGroupNodes(project, (ApplicationNode) node);
            });

            // application
            dataContext.setSelectApplicationNode((ApplicationNode) node);
        }

        // api目录展开, 查询api
        if (node instanceof ApiGroupNode) {
            JTreeLoadingUtils.loading(true, tree, node, () -> ApiDesignUtils.apiInterfaceList((ApiGroupNode) node));

            // apiGroup
            dataContext.setSelectApiGroupNode((ApiGroupNode) node);
            // application
            TreeNode parent = node.getParent();
            if (Objects.nonNull(parent) && parent instanceof ApplicationNode) {
                ApplicationNode applicationNode = (ApplicationNode) parent;
                dataContext.setSelectApplicationNode(applicationNode);

                // setSelectApiGroupNodes
                setSelectApiGroupNodes(project, applicationNode);
            }
        }

        // api层
        if (node instanceof ApiNode) {
            // api
            dataContext.setSelectApiNode((ApiNode) node);

            // apiGroup
            TreeNode apiGroup = node.getParent();
            if (Objects.nonNull(apiGroup) && apiGroup instanceof ApiGroupNode) {
                dataContext.setSelectApiGroupNode((ApiGroupNode) apiGroup);

                // application
                TreeNode applicationNode = apiGroup.getParent();
                if (Objects.nonNull(applicationNode) && applicationNode instanceof ApplicationNode) {
                    dataContext.setSelectApplicationNode((ApplicationNode) applicationNode);

                    // setSelectApiGroupNodes
                    setSelectApiGroupNodes(project, (ApplicationNode) applicationNode);
                }
            }
        }
    }

    public static void setSelectApiGroupNodes(Project project, ApplicationNode applicationNode) {
        List<ApiGroupNode> selectApiGroupNodes = DataContext.getInstance(project).getSelectApiGroupNodes();
        selectApiGroupNodes.clear();

        for (Enumeration<? extends TreeNode> children = applicationNode.children(); children.hasMoreElements(); ) {
            TreeNode treeNode = children.nextElement();
            if (treeNode instanceof ApiGroupNode) {
                selectApiGroupNodes.add((ApiGroupNode) treeNode);
            }
        }
    }
}
