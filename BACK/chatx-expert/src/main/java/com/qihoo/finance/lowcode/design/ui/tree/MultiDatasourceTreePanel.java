package com.qihoo.finance.lowcode.design.ui.tree;

import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.util.ui.UIUtil;
import com.qihoo.finance.lowcode.common.configuration.UserInfoPersistentState;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.entity.dto.PermissionTreeDTO;
import com.qihoo.finance.lowcode.common.ui.base.SearchTreeCellRenderer;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.common.util.JTreeLoadingUtils;
import com.qihoo.finance.lowcode.design.entity.DatabaseDepartmentNode;
import com.qihoo.finance.lowcode.design.entity.DatabaseNode;
import com.qihoo.finance.lowcode.design.entity.MongoDatabaseNode;
import com.qihoo.finance.lowcode.design.listener.MongoTreeListener;
import com.qihoo.finance.lowcode.design.listener.MySQLTreeListener;
import com.qihoo.finance.lowcode.design.ui.DatabaseMainPanel;
import com.qihoo.finance.lowcode.design.util.DatabaseDesignUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * MultiDatasourceTreePanel
 *
 * @author fengjinfu-jk
 * date 2024/2/27
 * @version 1.0.0
 * @apiNote MultiDatasourceTreePanel
 */
@Getter
@Slf4j
public class MultiDatasourceTreePanel extends DatabaseTreePanel {
    private final List<DatabaseTreePanel> databaseTrees;
    private final MySQLTreePanel mysql;
    private final MongoTreePanel mongo;

    public MultiDatasourceTreePanel(@NotNull Project project) {
        super(project);

        mysql = project.getService(MySQLTreePanel.class);
        mongo = project.getService(MongoTreePanel.class);
        databaseTrees = new ArrayList<>();
        databaseTrees.add(mysql);
        databaseTrees.add(mongo);

        DatabaseMainPanel databaseMainPanel = project.getService(DatabaseMainPanel.class);
        databaseMainPanel.getDatasource().setVisible(false);
        databaseMainPanel.getDatabaseLabel().setVisible(false);
    }

    @Override
    public List<TreeSelectionListener> treeSelectionListener() {
        return Lists.newArrayList(new MySQLTreeListener(project, tree), new MongoTreeListener(project, tree));
    }

    @Override
    public void nodeRenderer(SearchTreeCellRenderer cellRenderer, DefaultMutableTreeNode node) {
        databaseTrees.forEach(d -> d.nodeRenderer(cellRenderer, node));
    }

    @Override
    public MouseListener mouseListener(JTree tree) {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                for (DatabaseTreePanel databaseTree : databaseTrees) {
                    databaseTree.mouseListener(tree).mouseClicked(e);
                }
            }
        };
    }

    @Override
    public void loadDepAndDbTree(JTree tree, boolean async) {
        log.info("MultiDatasourceTreePanel loadDepAndDbTree : {}", System.identityHashCode(tree));
//        showLoading(tree);
        UIUtil.invokeLaterIfNeeded(() -> this.loading.startLoading(false));

        // root
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        JTreeLoadingUtils.loading(async, tree, root, () -> {
            // department
            List<PermissionTreeDTO> mysqlPermission = DatabaseDesignUtils.queryUserPermissionList(UserInfoPersistentState.getUserInfo().getUserNo(), Constants.DataSource.MySQL);
            List<PermissionTreeDTO> mongoPermission = DatabaseDesignUtils.queryUserPermissionList(UserInfoPersistentState.getUserInfo().getUserNo(), Constants.DataSource.MongoDB);
            // merge tree
            List<DefaultMutableTreeNode> mysqlTree = mysql.buildNodeTree(mysqlPermission);
            List<DefaultMutableTreeNode> mongoTree = mongo.buildNodeTree(mongoPermission);
            return mergeTree(mysqlTree, mongoTree);
        }, nodes -> {
            UIUtil.invokeLaterIfNeeded(this.loading::stopLoading);
            // 加载表并写入上下文
            List<DatabaseNode> databaseNodes = new ArrayList<>();
            List<MongoDatabaseNode> MongoDatabaseNodes = new ArrayList<>();

            for (DefaultMutableTreeNode dep : nodes) {
                if (dep instanceof DatabaseDepartmentNode) {
                    Enumeration<TreeNode> children = dep.children();
                    while (children.hasMoreElements()) {
                        TreeNode treeNode = children.nextElement();
                        if (treeNode instanceof DatabaseNode dbNode) {
                            databaseNodes.add(dbNode);
                        }
                    }
                }
                if (dep instanceof DatabaseDepartmentNode) {
                    Enumeration<TreeNode> children = dep.children();
                    while (children.hasMoreElements()) {
                        TreeNode treeNode = children.nextElement();
                        if (treeNode instanceof MongoDatabaseNode dbNode) {
                            MongoDatabaseNodes.add(dbNode);
                        }
                    }
                }
            }

            DataContext.getInstance(project).setAllMySQLDatabaseList(databaseNodes);
            DataContext.getInstance(project).setAllMongoDatabaseList(MongoDatabaseNodes);
        });
    }

    private List<? extends DefaultMutableTreeNode> mergeTree(List<DefaultMutableTreeNode> mysqlTree, List<DefaultMutableTreeNode> mongoTree) {
        List<DefaultMutableTreeNode> treeNodes = new ArrayList<>(mysqlTree);
        Map<String, DefaultMutableTreeNode> treeNodeMap = treeNodes.stream().collect(Collectors.toMap(DefaultMutableTreeNode::toString, Function.identity()));

        for (DefaultMutableTreeNode node : mongoTree) {
            if (treeNodeMap.containsKey(node.toString())) {
                DefaultMutableTreeNode dep = treeNodeMap.get(node.toString());
                if (node.getChildCount() > 0) {
                    Enumeration<TreeNode> children = node.children();
                    while (children.hasMoreElements()) {
                        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) children.nextElement();
                        dep.add(treeNode);
                    }
                }
            } else {
                treeNodes.add(node);
            }
        }

        return treeNodes;
    }

    @Override
    public void updateDatabaseContext() {
        DataContext.getInstance(project).setDbTree(tree);
    }

    /**
     * 创建自定义的右键菜单
     */
    @Override
    public JBPopupMenu createPopupMenu(JTree tree, DefaultMutableTreeNode node) {
        JBPopupMenu popupMenu = new JBPopupMenu();
        for (DatabaseTreePanel databaseTree : databaseTrees) {
            popupMenu.add(databaseTree.createPopupMenu(tree, node));
        }

        return popupMenu;
    }
}
