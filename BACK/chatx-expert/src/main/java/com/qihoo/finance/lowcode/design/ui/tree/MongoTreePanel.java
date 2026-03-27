package com.qihoo.finance.lowcode.design.ui.tree;

import com.google.common.collect.Lists;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.qihoo.finance.lowcode.common.configuration.UserInfoPersistentState;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.entity.PlaceholderNode;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.entity.dto.NodeType;
import com.qihoo.finance.lowcode.common.entity.dto.PermissionTreeDTO;
import com.qihoo.finance.lowcode.common.entity.dto.plugins.PluginConfig;
import com.qihoo.finance.lowcode.common.ui.base.SearchTreeCellRenderer;
import com.qihoo.finance.lowcode.common.util.*;
import com.qihoo.finance.lowcode.console.mongo.MongoEditorManager;
import com.qihoo.finance.lowcode.design.entity.DatabaseDepartmentNode;
import com.qihoo.finance.lowcode.design.entity.MongoCollectionNode;
import com.qihoo.finance.lowcode.design.entity.MongoDatabaseNode;
import com.qihoo.finance.lowcode.design.listener.MongoTreeListener;
import com.qihoo.finance.lowcode.design.util.DatabaseDesignUtils;
import com.qihoo.finance.lowcode.gentracker.actions.GenerateAction;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * @author weiyichao
 * date 2023-07-27
 **/
@Getter
@Slf4j
public class MongoTreePanel extends DatabaseTreePanel {
    private MongoEditorManager mongoEditorManager;

    public MongoTreePanel(@NotNull Project project) {
        super(project);
        mongoEditorManager = project.getService(MongoEditorManager.class);
    }

    @Override
    public List<TreeSelectionListener> treeSelectionListener() {
        return Lists.newArrayList(new MongoTreeListener(project, tree));
    }

    public List<DefaultMutableTreeNode> buildNodeTree(List<PermissionTreeDTO> permissions) {
        List<DefaultMutableTreeNode> depNodes = new ArrayList<>();
        for (PermissionTreeDTO permission : permissions) {
            if (NodeType.DATABASE_NODE.getTypeCode().equals(permission.getNodeType())) {
                MongoDatabaseNode node = new MongoDatabaseNode();
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
                    if (childNode instanceof MongoDatabaseNode database) {
                        database.setDepName(node.getName());
                    }
                }
            }
        }
        return depNodes;
    }

    @Override
    public void nodeRenderer(SearchTreeCellRenderer cellRenderer, DefaultMutableTreeNode node) {
        if (node instanceof DatabaseDepartmentNode) {
            cellRenderer.setIcon(Icons.scaleToWidth(Icons.ORG, 18));
        }
        if (node instanceof MongoDatabaseNode) {
//            cellRenderer.setIcon(Icons.scaleToWidth(Icons.DB_BLOCK, 18));
            cellRenderer.setIcon(Icons.scaleToWidth(AllIcons.Providers.MongoDB, 18));
        }
        if (node instanceof MongoCollectionNode) {
            cellRenderer.setIcon(Icons.scaleToWidth(Icons.TABLE2, 16));
        }
        if (node instanceof PlaceholderNode) {
            cellRenderer.setIcon(Icons.scaleToWidth(Icons.HOLDER, 18));
        }
    }

    @Override
    public MouseListener mouseListener(JTree tree) {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = tree.getRowForLocation(e.getX(), e.getY());
                    if (row != -1) {
//                        // fixme: tree.setSelectionRow(row) 会不支持多选
//                        tree.setSelectionRow(row);
                        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                        if (selectedNode != null) {
                            JBPopupMenu popupMenu = createPopupMenu(tree, selectedNode);
                            if (popupMenu.getComponentCount() > 0) {
                                popupMenu.show(tree, e.getX(), e.getY());
                            }
                        }
                    }
                }

                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    // 在这里处理双击事件
                    TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                    if (Objects.isNull(path)) return;

                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    if (node instanceof MongoCollectionNode) {
                        queryCollection((MongoCollectionNode) node);
                    }
                }
            }
        };
    }

    private void queryCollection(MongoCollectionNode node) {
        mongoEditorManager.openMongoEditor(node);
    }

    @Override
    public void loadDepAndDbTree(JTree tree, boolean async) {
        log.info("MongoTreePanel loadDepAndDbTree : {}", System.identityHashCode(tree));
//        showLoading(tree);
        UIUtil.invokeLaterIfNeeded(() -> this.loading.startLoading(false));

        // root
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        JTreeLoadingUtils.loading(async, tree, root, () -> {
            // department
            List<PermissionTreeDTO> permissions = DatabaseDesignUtils.queryUserPermissionList(UserInfoPersistentState.getUserInfo().getUserNo(), Constants.DataSource.MongoDB);
            return extractDatabaseNode(buildNodeTree(permissions), Constants.DataSource.MongoDB);
        }, nodes -> {
            UIUtil.invokeLaterIfNeeded(this.loading::stopLoading);
            // 加载表并写入上下文
            List<MongoDatabaseNode> mongoDatabaseNodes = new ArrayList<>();
            for (DefaultMutableTreeNode dep : nodes) {
                if (dep instanceof MongoDatabaseNode dbNode) {
                    mongoDatabaseNodes.add(dbNode);
                    if (dbNode.isCollect()) {
                        JTreeLoadingUtils.loading(async, tree, dbNode, () -> DatabaseDesignUtils.queryMongoCollectionNodes(dbNode));
                    }
                }
            }

            DataContext.getInstance(project).setAllMongoDatabaseList(mongoDatabaseNodes);
        });
    }

    private List<DefaultMutableTreeNode> sortAndCollect(List<DefaultMutableTreeNode> databaseNodes) {
        return null;
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
        PluginConfig pluginConfig = LowCodeAppUtils.getPluginConfig();
        TreePath[] selectionPaths = tree.getSelectionPaths();
        boolean isMultiSelect = selectionPaths != null && selectionPaths.length > 1;

        JBPopupMenu popupMenu = new JBPopupMenu();
        popupMenu.setBorder(BorderFactory.createLineBorder(JBColor.GRAY));
        popupMenu.getInsets().set(50, 50, 50, 50);

        if (node instanceof MongoDatabaseNode) {
            JBMenuItem collect = createCollectMenuItem(Constants.DataSource.MongoDB, node);
            collect.setIcon(Icons.scaleToWidth(Icons.COLLECT, 14));
            popupMenu.add(collect);

            popupMenu.addSeparator();
            popupMenu.add(Box.createVerticalStrut(2));

            JBMenuItem addTable = createMenuItem("新建collection", (MongoDatabaseNode) node);
            addTable.setIcon(Icons.scaleToWidth(Icons.TABLE_ADD, 16));
            popupMenu.add(addTable);
            popupMenu.addSeparator();
        }
        if (node instanceof MongoCollectionNode) {
            MongoCollectionNode collectionNode = (MongoCollectionNode) node;
            TreeNode parent = collectionNode.getParent();
            if (Objects.nonNull(parent) && parent instanceof MongoDatabaseNode) {
                popupMenu.add(Box.createVerticalStrut(2));
                JBMenuItem sqlExecute = newScriptExecute((MongoDatabaseNode) parent);
                sqlExecute.setIcon(AllIcons.Providers.MongoDB);
                popupMenu.add(sqlExecute);

                popupMenu.addSeparator();
                popupMenu.add(Box.createVerticalStrut(2));

                JBMenuItem addTable = createMenuItem("新建collection", (MongoDatabaseNode) parent);
                addTable.setIcon(Icons.scaleToWidth(Icons.TABLE_ADD, 16));
                popupMenu.add(addTable);
            }

            if (!isMultiSelect) {
                popupMenu.add(Box.createVerticalStrut(6));
                JBMenuItem deleteTable = deleteTableMenu(collectionNode);
                deleteTable.setIcon(Icons.scaleToWidth(Icons.TABLE_REMOVE, 16));
                popupMenu.add(deleteTable);

                popupMenu.addSeparator();
                popupMenu.add(Box.createVerticalStrut(2));

                if (!pluginConfig.isDisableDbGenerate()) {
                    JBMenuItem genCode = createMenuItem(new GenerateAction("生成代码", project));
                    genCode.setIcon(Icons.scaleToWidth(Icons.TABLE_GEN_CODE, 15));
                    popupMenu.add(genCode);

                    popupMenu.add(Box.createVerticalStrut(2));
                    popupMenu.addSeparator();
                }
            } else if (!pluginConfig.isDisableDbGenerate()) {
                popupMenu.addSeparator();
                popupMenu.add(Box.createVerticalStrut(2));

                JBMenuItem genCode = createMenuItem(new GenerateAction("生成代码 (批量)", project));
                genCode.setIcon(Icons.scaleToWidth(Icons.TABLE_GEN_CODE, 15));
                popupMenu.add(genCode);

                popupMenu.addSeparator();
            }

        }

        if (popupMenu.getComponentCount() > 0) {
            popupMenu.insert(Box.createVerticalStrut(8), 0);
        }

        return popupMenu;
    }

    private JBMenuItem newScriptExecute(MongoDatabaseNode dbNode) {
        JBMenuItem menuItem = new JBMenuItem(new AbstractAction("查询数据") {
            @Override
            public void actionPerformed(ActionEvent e) {
                MongoCollectionNode collectionNode = dataContext.getSelectMongoCollection();
                if (Objects.nonNull(collectionNode)) {
                    queryCollection(collectionNode);
                }
            }
        });

        // 设置自定义边距
        menuItem.setMargin(JBUI.insets(0, 10));
        return menuItem;
    }

    /**
     * 创建带有自定义边距的 JBMenuItem
     */
    private JBMenuItem createMenuItem(String text, MongoDatabaseNode node) {
        JBMenuItem menuItem = new JBMenuItem(new AbstractAction(text) {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 自定义菜单项的操作
                String collection = Messages.showInputDialog("请输入collection名称", text, Icons.scaleToWidth(Icons.TABLE_ADD, 60));
                if (StringUtils.isNotEmpty(collection)) {
                    // db.createCollection(name, options)
                    String mongoSQL = String.format("db.createCollection(%s, {})", collection);
                    // execute create collection mongoSQL
                    log.info("execute create collection mongoSQL: {}", mongoSQL);
                    if (DatabaseDesignUtils.createMongoCollection(node, collection)) {
                        CacheManager.refreshTemplate();
                        JTreeLoadingUtils.loading(false, tree, node, () -> {
                            // 重新加载库并加载代码生成
                            return DatabaseDesignUtils.queryMongoCollectionNodes(node);
                        }, nodes -> {
                            for (DefaultMutableTreeNode node : nodes) {
                                MongoCollectionNode collectionNode = (MongoCollectionNode) node;
                                if (collection.equals(collectionNode.getTableName())) {
                                    SwingUtilities.invokeLater(() -> {
                                        tree.setSelectionPath(new TreePath(collectionNode));
                                        new GenerateAction("生成代码", project, 2)
                                                .actionPerformed(new ActionEvent(this, 0, null));
                                    });
                                }
                            }
                        });
                    }
                }
            }
        });
        // 设置自定义边距
        menuItem.setMargin(JBUI.insets(0, 10));
        return menuItem;
    }

    /**
     * 创建带有自定义边距的 JBMenuItem
     */
    private JBMenuItem deleteTableMenu(MongoCollectionNode node) {
        String text = "删除collection";
        JBMenuItem menuItem = new JBMenuItem(new AbstractAction(text) {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 自定义菜单项的操作
                String tips = String.format("您正在删除collection [ %s ] \n\n请输入完整collection名称确认删除 !", node.getTableName());
                String inputString = Messages.showInputDialog(project, tips, text, Icons.scaleToWidth(Icons.DELETE, 60), null, new InputValidator() {
                    @Override
                    public boolean checkInput(@NlsSafe String inputString) {
                        return node.getTableName().equalsIgnoreCase(inputString);
                    }

                    @Override
                    public boolean canClose(@NlsSafe String inputString) {
                        return true;
                    }
                });

                if (node.getTableName().equalsIgnoreCase(inputString)) {
                    if (node.getParent() instanceof MongoDatabaseNode) {
                        MongoDatabaseNode database = (MongoDatabaseNode) node.getParent();
                        String dropCollection = String.format("db.getCollection('%s').drop();", node.getTableName());
                        // execute create collection mongoSQL
                        log.info("execute drop collection mongoSQL: {}", dropCollection);
                        Result<Boolean> result = DatabaseDesignUtils.dropMongoCollection(database, dropCollection);
                        if (result.isFail()) {
                            Messages.showMessageDialog("\n" + result.getErrorMsg(), "删除失败", Icons.scaleToWidth(Icons.FAIL, 60));
                        } else {
                            CacheManager.refreshTemplate();
                            JTreeLoadingUtils.loading(false, tree, database, () -> {
                                // 重新加载库并加载代码生成
                                return DatabaseDesignUtils.queryMongoCollectionNodes(database);
                            });
                        }
                    }
                }
            }
        });

        // 设置自定义边距
        menuItem.setMargin(JBUI.insets(0, 10));
        return menuItem;
    }
}
