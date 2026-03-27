package com.qihoo.finance.lowcode.common.ui.base;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.util.ui.JBUI;
import com.qihoo.finance.lowcode.common.configuration.UserContextPersistent;
import com.qihoo.finance.lowcode.design.ui.DatabaseBasePanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * BasePanel
 *
 * @author fengjinfu-jk
 * date 2023/8/18
 * @version 1.0.0
 * @apiNote MainPanel
 */
public abstract class TreePanelAdapter extends DatabaseBasePanel {
    public TreePanelAdapter(@NotNull Project project) {
        super(project);
    }

    @Override
    public Component createPanel() {
        return null;
    }

    public abstract JTree getTree();


    protected java.util.List<DefaultMutableTreeNode> sortAndCollect(java.util.List<? extends DefaultMutableTreeNode> nodes, String datasourceType) {
        java.util.List<String> collects = UserContextPersistent.getUserContext().getCollects().getOrDefault(datasourceType, new ArrayList<>());
        // 处理收藏/置顶后排序
        Map<String, DefaultMutableTreeNode> collectList = new HashMap<>();
        for (DefaultMutableTreeNode node : nodes) {
            if (node instanceof FilterableTreeNode filterNode) {
                if (collects.contains(node.toString())) {
                    filterNode.setCollect(true);
                    collectList.put(node.toString(), node);
                }
            }
        }
        // 收藏的
        Collection<DefaultMutableTreeNode> collectValues = collectList.values();
        java.util.List<DefaultMutableTreeNode> collectDbs = collectValues.stream().sorted(Comparator.comparing(DefaultMutableTreeNode::toString)).collect(Collectors.toList());
        // 其他的
        Collection<? extends DefaultMutableTreeNode> databases = nodes.stream().collect(Collectors.toMap(DefaultMutableTreeNode::toString, Function.identity(), (o, n) -> n)).values();
        java.util.List<? extends DefaultMutableTreeNode> others = databases.stream().filter(node -> !collects.contains(node.toString()))
                .sorted(Comparator.comparing(DefaultMutableTreeNode::toString)).toList();
        // 汇总
        collectDbs.addAll(others);
        return collectDbs;
    }

    /**
     * 创建带有自定义边距的 JBMenuItem
     */
    public JBMenuItem createCollectMenuItem(String key, DefaultMutableTreeNode node) {
        String text = hadCollect(node) ? "取消 收藏/置顶" : "收藏/置顶";
        JBMenuItem menuItem = new JBMenuItem(new AbstractAction(text) {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean hadCollect = hadCollect(node);
                // 自定义菜单项的操作
                Map<String, java.util.List<String>> collects = UserContextPersistent.getUserContext().collects;
                java.util.List<String> values = collects.getOrDefault(key, new ArrayList<>());
                if (!hadCollect) {
                    values.add(node.toString());
                } else {
                    values.remove(node.toString());
                }

                UserContextPersistent.getUserContext().collects.put(key, values);
                UserContextPersistent.getInstance().loadState(UserContextPersistent.getUserContext());
                if (node instanceof FilterableTreeNode filterNode) {
                    filterNode.setCollect(!hadCollect);
                }

                resort(node);
            }

            private void resort(DefaultMutableTreeNode node) {
                java.util.List<DefaultMutableTreeNode> nodes = new ArrayList<>();
                TreeNode root = node.getParent();
                for (Enumeration<? extends TreeNode> children = root.children(); children.hasMoreElements(); ) {
                    TreeNode treeNode = children.nextElement();
                    nodes.add((DefaultMutableTreeNode) treeNode);
                }

                List<DefaultMutableTreeNode> sortNodes = sortAndCollect(nodes, key);
                DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) root;
                rootNode.removeAllChildren();
                sortNodes.forEach(rootNode::add);
                // reload
                DefaultTreeModel model = (DefaultTreeModel) getTree().getModel();
                model.reload(rootNode);
            }
        });
        // 设置自定义边距
        menuItem.setMargin(JBUI.insets(0, 10));
        return menuItem;
    }

    private boolean hadCollect(DefaultMutableTreeNode node) {
        if (node instanceof FilterableTreeNode filterNode) {
            return (filterNode.isCollect());
        }

        return false;
    }
}
