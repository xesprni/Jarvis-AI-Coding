package com.qihoo.finance.lowcode.apitrack.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LoadingDecorator;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.UIUtil;
import com.qihoo.finance.lowcode.apitrack.entity.ai.AiApiNode;
import com.qihoo.finance.lowcode.apitrack.entity.ai.AiMenuNode;
import com.qihoo.finance.lowcode.apitrack.entity.ai.AiProjectNode;
import com.qihoo.finance.lowcode.apitrack.listener.AiApiTreeMouseListener;
import com.qihoo.finance.lowcode.apitrack.listener.AiApiTreeSelectionListener;
import com.qihoo.finance.lowcode.apitrack.util.ApiDesignUtils;
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

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeSelectionModel;
import java.awt.Dimension;
import java.util.List;

@Getter
public class AiApiTreePanel extends TreePanelAdapter {

    private final String branch;
    private JTree tree;
    private JScrollPane scrollPane;
    private LoadingDecorator loading;
    private boolean loaded = false;

    public final static String BRANCH_MASTER = "master";
    public final static String BRANCH_OTHER = "other";
    public final static String COLLECT_TYPE = "AI_API";

    public AiApiTreePanel(Project project, String branch) {
        super(project);
        this.branch = branch;
    }

    /**
     * Create Tree Panel
     * @return
     */
    public JComponent getComponent() {
        if (loading == null) {
            FilterableTreeNode root = new FilterableTreeNode("奇富科技");
            FilterableTreeModel model = new FilterableTreeModel(root);
            model.setRoot(root);
            tree = new Tree(model);
            tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
            tree.setRootVisible(false);
            tree.setCellRenderer(newTreeCellRenderer());
            tree.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            tree.setPreferredSize(new Dimension(300, -1));
            tree.setMaximumSize(new Dimension(400, -1));
            tree.addMouseListener(new AiApiTreeMouseListener(project, tree, this));
            tree.addTreeSelectionListener(new AiApiTreeSelectionListener(project, tree));
            tree.addTreeExpansionListener(new DefaultTreeExpansionListener(tree));
            tree.addTreeWillExpandListener(new DefaultTreeWillExpansionListener(tree));

            scrollPane = new JBScrollPane(tree);
            scrollPane.setBorder(null);
            loading = new LoadingDecorator(scrollPane, this, 0);
            loading.getComponent().setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        }
        return loading.getComponent();
    }

    public void show() {
        show(true, false);
    }

    /**
     * 展示树的一级结构（yapi项目）
     * @param async
     */
    public void show(boolean async, boolean foreReload) {
        if (loaded && !foreReload) {
            return;
        }
        UIUtil.invokeLaterIfNeeded(() -> this.loading.startLoading(false));
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        JTreeLoadingUtils.loading(async, tree, root, () -> {
            // 调用接口获取 yapi 项目列表
            List<AiProjectNode> nodes = ApiDesignUtils.yapiProjects(branch);
            return sortAndCollect(nodes, COLLECT_TYPE);
        }, nodes -> {
            UIUtil.invokeLaterIfNeeded(this.loading::stopLoading);
            DataContext.getInstance(project).setApiTree(tree);
            loaded = true;
        });
    }

    private TreeCellRenderer newTreeCellRenderer() {
        return new SearchTreeCellRenderer(tree) {
            @Override
            public void setTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                if (node instanceof AiProjectNode aiProjectNode) {
                    setIcon(Icons.scaleToWidth(Icons.API_APPLICATION, 16));
                    setToolTipText(aiProjectNode.getToolTipText());
                } else if (node instanceof AiMenuNode aiMenuNode) {
                    setIcon(Icons.scaleToWidth(Icons.API_FOLDER, 16));
                    setToolTipText(aiMenuNode.getToolTipText());
                } else if (node instanceof AiApiNode aiApiNode) {
                    setIcon(Icons.scaleToWidth(Icons.API, 16));
                    setToolTipText(aiApiNode.getToolTipText());
                } else if (node instanceof PlaceholderNode) {
                    setIcon(Icons.scaleToWidth(Icons.HOLDER, 18));
                }
            }
        };
    }

}
