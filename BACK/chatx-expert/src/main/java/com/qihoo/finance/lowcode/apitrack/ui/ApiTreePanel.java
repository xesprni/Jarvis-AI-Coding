package com.qihoo.finance.lowcode.apitrack.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.*;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.qihoo.finance.lowcode.apitrack.action.ApiDesignAction;
import com.qihoo.finance.lowcode.apitrack.action.ApiGenerateAction;
import com.qihoo.finance.lowcode.apitrack.action.ApiGroupAction;
import com.qihoo.finance.lowcode.apitrack.dialog.ApiDesignDialog;
import com.qihoo.finance.lowcode.apitrack.entity.ApiDepartmentNode;
import com.qihoo.finance.lowcode.apitrack.entity.ApiGroupNode;
import com.qihoo.finance.lowcode.apitrack.entity.ApiNode;
import com.qihoo.finance.lowcode.apitrack.entity.ApplicationNode;
import com.qihoo.finance.lowcode.apitrack.listener.ApiTreeListener;
import com.qihoo.finance.lowcode.apitrack.util.ApiDesignUtils;
import com.qihoo.finance.lowcode.common.constants.OperateType;
import com.qihoo.finance.lowcode.common.entity.PlaceholderNode;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.entity.dto.plugins.PluginConfig;
import com.qihoo.finance.lowcode.common.listener.DefaultTreeExpansionListener;
import com.qihoo.finance.lowcode.common.listener.DefaultTreeWillExpansionListener;
import com.qihoo.finance.lowcode.common.ui.base.FilterableTreeModel;
import com.qihoo.finance.lowcode.common.ui.base.FilterableTreeNode;
import com.qihoo.finance.lowcode.common.ui.base.SearchTreeCellRenderer;
import com.qihoo.finance.lowcode.common.ui.base.TreePanelAdapter;
import com.qihoo.finance.lowcode.common.util.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

/**
 * ApiTreePanel
 *
 * @author fengjinfu-jk
 * date 2023/8/31
 * @version 1.0.0
 * @apiNote ApiTreePanel
 */
@Getter
@Slf4j
public class ApiTreePanel extends TreePanelAdapter {
    private final Project project;
    private LoadingDecorator loading;
    private JTree tree;
    private static final String COLLECT = "API";

    public ApiTreePanel(@NotNull Project project) {
        super(project);
        this.project = project;
    }

    @Override
    public JComponent createPanel() {
        FilterableTreeNode root = new FilterableTreeNode("奇富科技");
        FilterableTreeModel model = new FilterableTreeModel(root);
        model.setRoot(root);
        tree = new Tree(model);
        DataContext.getInstance(project).setApiTree(tree);

        // Create the JTree with the root node
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

        // Hide the root node from being displayed
        tree.setRootVisible(false);

        // Customize the cell renderer to display only the node names (department, connection string, etc.)
        SearchTreeCellRenderer cellRenderer = new SearchTreeCellRenderer(tree) {
            @Override
            public void setTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                if (node instanceof ApiDepartmentNode) {
                    setIcon(Icons.scaleToWidth(Icons.ORG, 18));
                }
                if (node instanceof ApplicationNode) {
                    setIcon(Icons.scaleToWidth(Icons.API_APPLICATION, 16));
                }
                if (node instanceof ApiGroupNode) {
                    setIcon(Icons.scaleToWidth(Icons.API_FOLDER, 16));
                }
                if (node instanceof ApiNode) {
                    setIcon(Icons.scaleToWidth(Icons.API, 16));
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
        tree.addMouseListener(new ApiMouseAdapter(project, tree, this));
        // add selectEventListener for generateTrack
        tree.addTreeSelectionListener(new ApiTreeListener(project, tree));
        tree.addTreeExpansionListener(new DefaultTreeExpansionListener(tree));
        tree.addTreeWillExpandListener(new DefaultTreeWillExpansionListener(tree));

        // add tree to scrollPane
        JScrollPane scrollPane = new JBScrollPane(tree);
        loading = new LoadingDecorator(scrollPane, this, 0);
        scrollPane.setBorder(null);

        // loadTree
        loadApiTree(tree);
        log.info("loadApiTree : {}", System.identityHashCode(tree));
        return loading.getComponent();
    }

    public void loadApiTree(JTree tree) {
        loadApiTree(true);
    }

    public void loadApiTree(boolean async) {
//        showLoading(tree);
        UIUtil.invokeLaterIfNeeded(() -> this.loading.startLoading(false));
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();

        // application
        JTreeLoadingUtils.loading(async, tree, root, () -> {
            List<ApiDepartmentNode> departmentNodes = ApiDesignUtils.apiGroupPrjNodeList();
            return extractApplications(departmentNodes);
        }, nodes -> {
//            closeLoading(tree);
            UIUtil.invokeLaterIfNeeded(this.loading::stopLoading);
            DataContext.getInstance(project).setApiTree(tree);
        });
    }

    private List<DefaultMutableTreeNode> extractApplications(List<ApiDepartmentNode> departmentNodes) {
        List<DefaultMutableTreeNode> treeNodes = new ArrayList<>();
        for (ApiDepartmentNode departmentNode : departmentNodes) {
            if (departmentNode.getChildCount() > 0) {
                for (Enumeration<? extends TreeNode> e = departmentNode.children(); e.hasMoreElements(); ) {
                    TreeNode n = e.nextElement();
                    if (n instanceof ApplicationNode applicationNode) {
                        applicationNode.setDepName(departmentNode.getName());
                        treeNodes.add(applicationNode);
                    }
                }
            }
        }

        return sortAndCollect(treeNodes, COLLECT);
    }

    //------------------------------------------------------------------------------------------------------------------

    static class ApiMouseAdapter extends MouseAdapter {
        private final JTree tree;
        private final Project project;

        private final ApiTreePanel treePanel;

        public ApiMouseAdapter(Project project, JTree tree, ApiTreePanel treePanel) {
            this.project = project;
            this.tree = tree;
            this.treePanel = treePanel;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            TreePath path = tree.getPathForLocation(e.getX(), e.getY());
            if (path == null) {
                return;
            }
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (SwingUtilities.isRightMouseButton(e)) {
                if (node != null) {
                    Pair<Boolean, JBPopupMenu> popupMenuPair = createPopupMenu(node);
                    if (popupMenuPair.getKey()) {
                        popupMenuPair.getValue().show(tree, e.getX(), e.getY());
                    }
                }
            }

            if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                // 在这里处理双击事件
                if (node instanceof ApiNode) {
                    // 展开详情
                    new ApiDesignDialog(project, OperateType.VIEW).show();
                }
            }
        }


        /**
         * 创建自定义的右键菜单
         */
        private Pair<Boolean, JBPopupMenu> createPopupMenu(DefaultMutableTreeNode node) {
            PluginConfig pluginConfig = LowCodeAppUtils.getPluginConfig();

            JBPopupMenu popupMenu = new JBPopupMenu();
            popupMenu.setBorder(BorderFactory.createLineBorder(JBColor.GRAY));
            popupMenu.getInsets().set(50, 50, 50, 50);
            popupMenu.add(Box.createVerticalStrut(8));

            if (node instanceof ApplicationNode) {
                if (!((ApplicationNode) node).isEditable()) {
                    return Pair.of(false, popupMenu);
                }

                JBMenuItem collect = treePanel.createCollectMenuItem(COLLECT, node);
                collect.setIcon(Icons.scaleToWidth(Icons.COLLECT, 14));
                popupMenu.add(collect);

                popupMenu.add(Box.createVerticalStrut(2));
                popupMenu.addSeparator();

                popupMenu.add(Box.createVerticalStrut(2));
                JBMenuItem addGroup = createMenuItem(new ApiGroupAction("新增分类", project, false));
                addGroup.setIcon(Icons.scaleToWidth(Icons.FOLDER_ADD, 16));
                popupMenu.add(addGroup);

                popupMenu.add(Box.createVerticalStrut(2));
                popupMenu.addSeparator();
            }
            if (node instanceof ApiGroupNode) {
                popupMenu.add(Box.createVerticalStrut(2));

                if (((ApiGroupNode) node).isEditable()) {
                    JBMenuItem addGroup = createMenuItem(new ApiGroupAction("新增分类", project, false));
                    addGroup.setIcon(Icons.scaleToWidth(Icons.FOLDER_ADD, 16));
                    popupMenu.add(addGroup);


                    popupMenu.add(Box.createVerticalStrut(6));

                    JBMenuItem editGroup = createMenuItem(new ApiGroupAction("编辑分类", project, true));
                    editGroup.setIcon(Icons.scaleToWidth(Icons.API_FOLDER, 16));
                    popupMenu.add(editGroup);
                    popupMenu.add(Box.createVerticalStrut(6));

                    JBMenuItem deleteGroup = deleteApiGroupMenu((ApiGroupNode) node);
                    deleteGroup.setIcon(Icons.scaleToWidth(Icons.FOLDER_DELETE, 16));
                    popupMenu.add(deleteGroup);


                    popupMenu.add(Box.createVerticalStrut(2));
                    popupMenu.addSeparator();
                    popupMenu.add(Box.createVerticalStrut(2));

                    JBMenuItem addApi = createMenuItem(new ApiDesignAction("新增接口", project, OperateType.CREATE));
                    addApi.setIcon(Icons.scaleToWidth(Icons.API_ADD, 16));
                    popupMenu.add(addApi);

                    popupMenu.add(Box.createVerticalStrut(2));
                    popupMenu.addSeparator();
                    popupMenu.add(Box.createVerticalStrut(2));
                }

                if (!pluginConfig.isDisableApiGenerate()) {
                    JBMenuItem apiGenCode = createMenuItem(new ApiGenerateAction("接口生成代码", project));
                    apiGenCode.setIcon(Icons.scaleToWidth(Icons.TABLE_GEN_CODE, 15));
                    popupMenu.add(apiGenCode);

                    popupMenu.add(Box.createVerticalStrut(2));
                    popupMenu.addSeparator();
                }
            }
            if (node instanceof ApiNode) {
                popupMenu.add(Box.createVerticalStrut(2));
                JBMenuItem apiDetail = createMenuItem(new ApiDesignAction("接口详情", project, OperateType.VIEW));
                apiDetail.setIcon(Icons.scaleToWidth(Icons.API, 16));
                popupMenu.add(apiDetail);

                if (((ApiNode) node).isEditable()) {
                    popupMenu.add(Box.createVerticalStrut(6));

                    JBMenuItem addApi = createMenuItem(new ApiDesignAction("新增接口", project, OperateType.CREATE));
                    addApi.setIcon(Icons.scaleToWidth(Icons.API_ADD, 16));
                    popupMenu.add(addApi);

                    popupMenu.add(Box.createVerticalStrut(6));
                    JBMenuItem copyApi = createMenuItem(new ApiDesignAction("复制接口", project, OperateType.COPY));
                    copyApi.setIcon(Icons.scaleToWidth(Icons.API_ADD, 16));
                    popupMenu.add(copyApi);

                    popupMenu.add(Box.createVerticalStrut(6));
                    JBMenuItem editApi = createMenuItem(new ApiDesignAction("编辑接口", project, OperateType.EDIT));
                    editApi.setIcon(Icons.scaleToWidth(Icons.API_EDIT, 16));
                    popupMenu.add(editApi);

                    popupMenu.add(Box.createVerticalStrut(6));
                    JBMenuItem deleteApiMenu = deleteApiMenu((ApiNode) node);
                    deleteApiMenu.setIcon(Icons.scaleToWidth(Icons.API_DELETE, 16));
                    popupMenu.add(deleteApiMenu);
                }

                popupMenu.add(Box.createVerticalStrut(2));
                popupMenu.addSeparator();
                popupMenu.add(Box.createVerticalStrut(2));

                if (!pluginConfig.isDisableApiGenerate()) {
                    JBMenuItem apiGenCode = createMenuItem(new ApiGenerateAction("接口生成代码", project));
                    apiGenCode.setIcon(Icons.scaleToWidth(Icons.TABLE_GEN_CODE, 15));
                    popupMenu.add(apiGenCode);

                    popupMenu.add(Box.createVerticalStrut(2));
                    popupMenu.addSeparator();
                }
            }

            return Pair.of(true, popupMenu);
        }

        /**
         * 创建带有自定义边距的 JBMenuItem
         */
        private JBMenuItem createMenuItem(Action action) {
            JBMenuItem menuItem = new JBMenuItem(action);
            // 设置自定义边距
            menuItem.setMargin(JBUI.insets(0, 10));
            return menuItem;
        }

        /**
         * 创建带有自定义边距的 JBMenuItem
         */
        private JBMenuItem deleteApiGroupMenu(ApiGroupNode node) {
            String text = "删除分类";
            String sure = "确认删除";
            JBMenuItem menuItem = new JBMenuItem(new AbstractAction(text) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // 自定义菜单项的操作
                    String tips = String.format("您正在删除接口分类 [ %s ] \n\n请输入 “确认删除” 完成删除 !", node.getName());
                    String inputString = Messages.showInputDialog(project, tips, text, Icons.scaleToWidth(Icons.DELETE, 60), null, new InputValidator() {
                        @Override
                        public boolean checkInput(@NlsSafe String inputString) {
                            return sure.equalsIgnoreCase(inputString);
                        }

                        @Override
                        public boolean canClose(@NlsSafe String inputString) {
                            return true;
                        }
                    });

                    if (sure.equalsIgnoreCase(inputString)) {
                        Result<Object> result = ApiDesignUtils.apiCategoryDelete(node);
                        if (result.isFail()) {
                            Messages.showMessageDialog("删除失败:\n\n" + result.getErrorMsg(), "接口分类删除", Icons.scaleToWidth(Icons.FAIL, 60));
                        } else {
                            // 缓存需要刷新
                            CacheManager.refreshInnerCache();

                            ApplicationNode applicationNode = DataContext.getInstance(project).getSelectApplicationNode();
                            JTreeLoadingUtils.loading(true, tree, applicationNode, () -> {
                                // 重新加载库
                                return ApiDesignUtils.apiCategoryList(applicationNode);
                            }, catList -> {
                                ApiTreeListener.setSelectApiGroupNodes(project, applicationNode);
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
        private JBMenuItem deleteApiMenu(ApiNode node) {
            String text = "删除接口";
            String sure = "确认删除";
            JBMenuItem menuItem = new JBMenuItem(new AbstractAction(text) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // 自定义菜单项的操作
                    String tips = String.format("您正在删除接口 [ %s ] \n\n请输入 “确认删除” 完成删除 !", node.getTitle());
                    String inputString = Messages.showInputDialog(project, tips, text, Icons.scaleToWidth(Icons.DELETE, 60), null, new InputValidator() {
                        @Override
                        public boolean checkInput(@NlsSafe String inputString) {
                            return sure.equalsIgnoreCase(inputString);
                        }

                        @Override
                        public boolean canClose(@NlsSafe String inputString) {
                            return true;
                        }
                    });

                    if (sure.equalsIgnoreCase(inputString)) {
                        Result<Object> result = ApiDesignUtils.apiInterfaceDelete(node);
                        if (result.isFail()) {
                            Messages.showMessageDialog("删除失败:\n\n" + result.getErrorMsg(), "接口删除", Icons.scaleToWidth(Icons.FAIL, 60));
                        } else {
                            // 缓存需要刷新
                            CacheManager.refreshInnerCache();
                            ApiGroupNode apiGroupNode = DataContext.getInstance(project).getSelectApiGroupNode();
                            JTreeLoadingUtils.loading(true, tree, apiGroupNode, () -> ApiDesignUtils.apiInterfaceList(apiGroupNode));
                        }
                    }
                }
            });

            // 设置自定义边距
            menuItem.setMargin(JBUI.insets(0, 10));
            return menuItem;
        }
    }

}
