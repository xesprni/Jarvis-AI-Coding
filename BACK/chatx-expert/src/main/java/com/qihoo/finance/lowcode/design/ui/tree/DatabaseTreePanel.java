package com.qihoo.finance.lowcode.design.ui.tree;

import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.ui.LoadingDecorator;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import com.qihoo.finance.lowcode.common.entity.dto.NodeType;
import com.qihoo.finance.lowcode.common.entity.dto.PermissionTreeDTO;
import com.qihoo.finance.lowcode.common.listener.DefaultTreeExpansionListener;
import com.qihoo.finance.lowcode.common.listener.DefaultTreeWillExpansionListener;
import com.qihoo.finance.lowcode.common.ui.base.TreePanelAdapter;
import com.qihoo.finance.lowcode.common.ui.base.FilterableTreeModel;
import com.qihoo.finance.lowcode.common.ui.base.FilterableTreeNode;
import com.qihoo.finance.lowcode.common.ui.base.SearchTreeCellRenderer;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.design.entity.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;
import java.util.*;

/**
 * @author weiyichao
 * date 2023-07-27
 **/
@Getter
@Slf4j
public abstract class DatabaseTreePanel extends TreePanelAdapter {
    protected JTree tree;
    protected LoadingDecorator loading;
    protected DataContext dataContext;
    private JComponent treeComponent;

    public DatabaseTreePanel(@NotNull Project project) {
        super(project);
        this.dataContext = DataContext.getInstance(project);
    }

    @Override
    public JComponent createPanel() {
        FilterableTreeNode root = new FilterableTreeNode("奇富科技");
        FilterableTreeModel model = new FilterableTreeModel(root);
        model.setRoot(root);
        tree = new Tree(model);
        tree.setOpaque(false);
        DataContext dataContext = DataContext.getInstance(project);
        dataContext.setDbTree(tree);

        // Create the JTree with the root node
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

        // Hide the root node from being displayed
        tree.setRootVisible(false);
        tree.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        tree.setPreferredSize(new Dimension(300, -1));
        tree.setMaximumSize(new Dimension(400, -1));
        tree.setVisibleRowCount(1000);

        // Customize the cell renderer to display only the node names (department, connection string, etc.)
        tree.setCellRenderer(new SearchTreeCellRenderer(tree) {
            @Override
            public void setTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                nodeRenderer(this, (DefaultMutableTreeNode) value);
            }
        });
        tree.setToggleClickCount(0);

        // 为 JTree 添加鼠标右键事件
        tree.addMouseListener(mouseListener(tree));
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // 在这里处理双击事件
                TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                if (Objects.isNull(path)) return;
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    boolean ignore = false;
                    for (Class<? extends DefaultMutableTreeNode> nodeClazz : ignoreExpendNode()) {
                        if (nodeClazz == node.getClass()) {
                            return;
                        }
                    }
                    if (tree.isExpanded(path)) {
                        tree.collapsePath(path);
                    } else {
                        tree.expandPath(path);
                    }
                }
            }
        });

        // add selectEventListener for generateTrack
        treeSelectionListener().forEach(tree::addTreeSelectionListener);
        tree.addTreeExpansionListener(new DefaultTreeExpansionListener(tree));
        tree.addTreeWillExpandListener(new DefaultTreeWillExpansionListener(tree));

        // tree component
//        JComponent component = addLoading(tree);
        JScrollPane scrollPane = new JBScrollPane(tree);
        loading = new LoadingDecorator(scrollPane, this, 0);
        scrollPane.setBorder(null);

        loadDepAndDbTree(tree, true);
        return loading.getComponent();
    }


    public synchronized JComponent getPanel() {
        if (Objects.isNull(treeComponent)) treeComponent = createPanel();

        updateDatabaseContext();
        return treeComponent;
    }

    public abstract List<TreeSelectionListener> treeSelectionListener();

    public List<Class<? extends DefaultMutableTreeNode>> ignoreExpendNode() {
        return Lists.newArrayList(MySQLTableNode.class, MongoCollectionNode.class);
    }

    public abstract MouseListener mouseListener(JTree tree);

    public abstract void nodeRenderer(SearchTreeCellRenderer cellRenderer, DefaultMutableTreeNode node);

    public abstract JBPopupMenu createPopupMenu(JTree tree, DefaultMutableTreeNode node);

    public abstract void loadDepAndDbTree(JTree tree, boolean async);

    public abstract void updateDatabaseContext();

    /**
     * 创建带有自定义边距的 JBMenuItem
     */
    protected JBMenuItem createMenuItem(Action action) {
        JBMenuItem menuItem = new JBMenuItem(action);
        // 设置自定义边距
        menuItem.setMargin(JBUI.insets(0, 10));
        return menuItem;
    }

    public List<DefaultMutableTreeNode> buildNodeTree(List<PermissionTreeDTO> permissions) {
        List<DefaultMutableTreeNode> depNodes = new ArrayList<>();
        for (PermissionTreeDTO permission : permissions) {
            if (NodeType.DATABASE_NODE.getTypeCode().equals(permission.getNodeType())) {
                DatabaseNode node = new DatabaseNode();
                node.setCode(permission.getCode());
                node.setName(permission.getName());
                node.setParentCode(permission.getParentCode());
                node.setNodeAttr(ObjectUtils.defaultIfNull(permission.getNodeAttr(), new HashMap<>()));

                depNodes.add(node);
                continue;
            }

            List<PermissionTreeDTO> children = permission.getChildren();
            DatabaseDepartmentNode node = new DatabaseDepartmentNode();
            node.setCode(permission.getCode());
            node.setName(permission.getName());
            node.setParentCode(permission.getParentCode());
            depNodes.add(node);

            if (CollectionUtils.isNotEmpty(children)) {
                List<DefaultMutableTreeNode> childNodes = buildNodeTree(children);
                childNodes.forEach(node::add);
                for (DefaultMutableTreeNode childNode : childNodes) {
                    if (childNode instanceof DatabaseNode database) {
                        database.setDepName(node.getName());
                    }
                }
            }
        }
        return depNodes;
    }

    public List<DefaultMutableTreeNode> extractDatabaseNode(List<DefaultMutableTreeNode> nodes, String datasourceType) {
        List<DefaultMutableTreeNode> databaseNodes = new ArrayList<>();
        for (DefaultMutableTreeNode dep : nodes) {
            if (dep instanceof DatabaseDepartmentNode) {
                if (dep.getChildCount() > 0) {
                    for (Enumeration<? extends TreeNode> db = dep.children(); db.hasMoreElements(); ) {
                        TreeNode treeNode = db.nextElement();
                        if (treeNode instanceof DatabaseNode || treeNode instanceof MongoDatabaseNode) {
                            databaseNodes.add((DefaultMutableTreeNode) treeNode);
                        }
                    }
                }
            }
        }

        return sortAndCollect(databaseNodes, datasourceType);
    }
}
