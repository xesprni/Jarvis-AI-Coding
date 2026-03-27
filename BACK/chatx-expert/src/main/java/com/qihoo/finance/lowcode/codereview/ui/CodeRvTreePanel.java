package com.qihoo.finance.lowcode.codereview.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LoadingDecorator;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.UIUtil;
import com.qihoo.finance.lowcode.codereview.entity.CodeRvOrgNode;
import com.qihoo.finance.lowcode.codereview.entity.CodeRvRepoNode;
import com.qihoo.finance.lowcode.codereview.entity.CodeRvSprintNode;
import com.qihoo.finance.lowcode.codereview.entity.CodeRvTaskNode;
import com.qihoo.finance.lowcode.codereview.handler.CodeRvMouseAdapter;
import com.qihoo.finance.lowcode.codereview.handler.CodeRvTreeListener;
import com.qihoo.finance.lowcode.codereview.util.CodeRvUtils;
import com.qihoo.finance.lowcode.common.entity.PlaceholderNode;
import com.qihoo.finance.lowcode.common.listener.DefaultTreeExpansionListener;
import com.qihoo.finance.lowcode.common.listener.DefaultTreeWillExpansionListener;
import com.qihoo.finance.lowcode.common.ui.base.FilterableTreeModel;
import com.qihoo.finance.lowcode.common.ui.base.FilterableTreeNode;
import com.qihoo.finance.lowcode.common.ui.base.SearchTreeCellRenderer;
import com.qihoo.finance.lowcode.common.ui.base.TreePanelAdapter;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.JTreeLoadingUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * CodeRvTreePanel
 *
 * @author fengjinfu-jk
 * date 2023/11/2
 * @version 1.0.0
 * @apiNote CodeRvTreePanel
 */
@Slf4j
@Getter
public class CodeRvTreePanel extends TreePanelAdapter {
    private final Project project;
    private LoadingDecorator loading;
    private JTree tree;
    public static final String COLLECT = "CODE_REVIEW";

    public CodeRvTreePanel(@NotNull Project project) {
        super(project);
        this.project = project;
    }

    @Override
    public JComponent createPanel() {
        FilterableTreeNode root = new FilterableTreeNode("奇富科技");
        FilterableTreeModel model = new FilterableTreeModel(root);
        model.setRoot(root);
        tree = new Tree(model);
        DataContext.getInstance(project).setCodeRvTree(tree);

        // Create the JTree with the root node
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

        // Hide the root node from being displayed
        tree.setRootVisible(false);

        // Customize the cell renderer to display only the node names (department, connection string, etc.)
        SearchTreeCellRenderer cellRenderer = new SearchTreeCellRenderer(tree) {
            @Override
            public void setTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                if (node instanceof CodeRvOrgNode) {
                    setIcon(Icons.scaleToWidth(Icons.ORG, 18));
                }
                if (node instanceof CodeRvRepoNode) {
                    setIcon(Icons.scaleToWidth(Icons.GIT_LAB, 20));
                }
                if (node instanceof CodeRvSprintNode) {
                    setIcon(Icons.scaleToWidth(Icons.RELEASE, 16));
                }
                if (node instanceof CodeRvTaskNode) {
                    setIcon(Icons.scaleToWidth(Icons.TASK, 18));
                }
                if (node instanceof PlaceholderNode) {
                    setIcon(Icons.scaleToWidth(Icons.HOLDER, 18));
                }
            }
        };
        tree.setCellRenderer(cellRenderer);
        tree.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        tree.setPreferredSize(new Dimension(300, -1));
        tree.setMaximumSize(new Dimension(400, -1));

        // 为 JTree 添加鼠标右键事件
        tree.addMouseListener(new CodeRvMouseAdapter(project, tree, this));
        // add selectEventListener for generateTrack
        tree.addTreeSelectionListener(new CodeRvTreeListener(project, tree));
        tree.addTreeExpansionListener(new DefaultTreeExpansionListener(tree));
        tree.addTreeWillExpandListener(new DefaultTreeWillExpansionListener(tree));

        // add tree to scrollPane
        JScrollPane scrollPane = new JBScrollPane(tree);
        loading = new LoadingDecorator(scrollPane, this, 0);
        scrollPane.setBorder(null);

        // loadTree
        loadCodeReviewTree(tree);
        log.info("loadCodeReviewTree : {}", System.identityHashCode(tree));
        return loading.getComponent();
    }

    public void loadCodeReviewTree(JTree tree) {
        loadCodeReviewTree(tree, true);
    }

    public void loadCodeReviewTree(JTree tree, boolean async) {
//        showLoading(tree);
        UIUtil.invokeLaterIfNeeded(() -> this.loading.startLoading(false));
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();

        // application
        JTreeLoadingUtils.loading(async, tree, root, () -> {
            List<CodeRvOrgNode> codeRvOrgNodes = CodeRvUtils.queryCodeRvDepartments();
            return extractRepoNodes(codeRvOrgNodes);
        }, nodes -> {
//            closeLoading(tree);
            UIUtil.invokeLaterIfNeeded(this.loading::stopLoading);
            DataContext.getInstance(project).setCodeRvTree(tree);
        });
    }

    private List<? extends DefaultMutableTreeNode> extractRepoNodes(List<CodeRvOrgNode> codeRvOrgNodes) {
        List<DefaultMutableTreeNode> treeNodes = new ArrayList<>();
        for (CodeRvOrgNode codeRvOrgNode : codeRvOrgNodes) {
            if (codeRvOrgNode.getChildCount() > 0) {
                for (Enumeration<? extends TreeNode> e = codeRvOrgNode.children(); e.hasMoreElements(); ) {
                    TreeNode n = e.nextElement();
                    if (n instanceof CodeRvRepoNode codeRvRepoNode) {
                        codeRvRepoNode.setDepName(codeRvOrgNode.getName());
                        treeNodes.add(codeRvRepoNode);
                    }
                }
            }
        }

        return sortAndCollect(treeNodes, COLLECT);
    }
}
