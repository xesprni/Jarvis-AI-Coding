package com.qihoo.finance.lowcode.codereview.handler;

import com.intellij.openapi.project.Project;
import com.qihoo.finance.lowcode.codereview.entity.CodeRvOrgNode;
import com.qihoo.finance.lowcode.codereview.entity.CodeRvRepoNode;
import com.qihoo.finance.lowcode.codereview.entity.CodeRvSprintNode;
import com.qihoo.finance.lowcode.codereview.entity.CodeRvTaskNode;
import com.qihoo.finance.lowcode.codereview.util.CodeRvUtils;
import com.qihoo.finance.lowcode.common.listener.TreeSelectionListenerChain;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.common.util.JTreeLoadingUtils;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Objects;

/**
 * ApiTreeListener
 *
 * @author fengjinfu-jk
 * date 2023/8/31
 * @version 1.0.0
 * @apiNote ApiTreeListener
 */
public class CodeRvTreeListener extends TreeSelectionListenerChain {
    private final JTree tree;

    public CodeRvTreeListener(Project project, JTree tree) {
        super(project);
        this.tree = tree;
        DataContext.getInstance(project).setCodeRvTree(tree);
    }

    @Override
    public void handlerValueChanged(TreeSelectionEvent e) {
        DataContext dataContext = DataContext.getInstance(project);
        dataContext.setCodeRvTree(tree);

        DefaultMutableTreeNode node = currentNode(e);

        // CodeRvRepoNode
        if (node instanceof CodeRvOrgNode) {
            // application
            dataContext.setSelectOrgNode((CodeRvOrgNode) node);
        }

        // CodeRvRepoNode
        if (node instanceof CodeRvRepoNode) {
            // repo
            dataContext.setSelectCodeRvRepo((CodeRvRepoNode) node);
            JTreeLoadingUtils.loading(true, tree, node, () -> CodeRvUtils.queryCodeRvTaskSprints((CodeRvRepoNode) node));

            // org
            TreeNode parent = node.getParent();
            if (Objects.nonNull(parent) && parent instanceof CodeRvOrgNode orgNode)
                dataContext.setSelectOrgNode(orgNode);
        }

        // CodeRvSprintNode
        if (node instanceof CodeRvSprintNode) {
            // sprint
            dataContext.setSelectCodeRvSprint((CodeRvSprintNode) node);

            // repo
            TreeNode repoNode = node.getParent();
            if (Objects.nonNull(repoNode)) {
                dataContext.setSelectCodeRvRepo((CodeRvRepoNode) repoNode);

                // org
                TreeNode orgNode = repoNode.getParent();
                if (Objects.nonNull(orgNode) && orgNode instanceof CodeRvOrgNode)
                    dataContext.setSelectOrgNode((CodeRvOrgNode) orgNode);
            }
        }

        // CodeRvTaskNode
        if (node instanceof CodeRvTaskNode) {
            // task
            dataContext.setSelectCodeRvTask((CodeRvTaskNode) node);
            TreeNode parent = node.getParent();
            if (Objects.nonNull(parent)) {
                // sprint
                CodeRvSprintNode sprint = (CodeRvSprintNode) parent;
                dataContext.setSelectCodeRvSprint(sprint);

                // repo
                TreeNode repoNode = sprint.getParent();
                if (Objects.nonNull(repoNode)) {
                    dataContext.setSelectCodeRvRepo((CodeRvRepoNode) repoNode);

                    // org
                    TreeNode orgNode = repoNode.getParent();
                    if (Objects.nonNull(orgNode) && orgNode instanceof CodeRvOrgNode)
                        dataContext.setSelectOrgNode((CodeRvOrgNode) orgNode);
                }

            }
        }
    }
}
