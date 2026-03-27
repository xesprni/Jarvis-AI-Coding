package com.qihoo.finance.lowcode.common.util;

import com.intellij.util.ui.UIUtil;
import com.qihoo.finance.lowcode.common.entity.PlaceholderNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import javax.swing.*;
import javax.swing.tree.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * JTreeLoadingUtils
 *
 * @author fengjinfu-jk
 * date 2023/9/1
 * @version 1.0.0
 * @apiNote JTreeLoadingUtils
 */
@Slf4j
public class JTreeLoadingUtils {

    public static void loading(boolean async, boolean autoReload, JTree tree, DefaultMutableTreeNode node, Supplier<List<? extends DefaultMutableTreeNode>> childNodes, Consumer<List<? extends DefaultMutableTreeNode>> done) {
        if (Objects.isNull(tree)) return;

        TreeModel model = tree.getModel();
        if (!(model instanceof DefaultTreeModel)) {
            throw new RuntimeException("jTree model must instanceof DefaultTreeModel");
        }

        // add loading text
        addLoading(node);

        if (async) {
            new SwingWorker<List<? extends DefaultMutableTreeNode>, List<? extends DefaultMutableTreeNode>>() {
                @Override
                protected List<? extends DefaultMutableTreeNode> doInBackground() {
                    try {
                        return childNodes.get();
                    } catch (Throwable e) {
                        return new ArrayList<>();
                    }
                }

                @SneakyThrows
                @Override
                protected void done() {
                    super.done();

                    List<? extends DefaultMutableTreeNode> nodes = get();
                    if (autoReload) {
                        reloadTree(tree, node, nodes);
                    }

                    if (Objects.nonNull(done)) {
                        done.accept(nodes);
                    }
                }
            }.execute();
        } else {
            List<? extends DefaultMutableTreeNode> nodes = childNodes.get();

            if (autoReload) {
                reloadTree(tree, node, nodes);
            }

            if (Objects.nonNull(done)) {
                done.accept(nodes);
            }
        }
    }

    public static <T extends DefaultMutableTreeNode> void loading(boolean async, boolean autoReload, JTree tree, DefaultMutableTreeNode node, Supplier<List<? extends DefaultMutableTreeNode>> childNodes) {
        loading(async, autoReload, tree, node, childNodes, null);
    }

    public static void loading(boolean async, JTree tree, DefaultMutableTreeNode node, Supplier<List<? extends DefaultMutableTreeNode>> childNodes) {
        loading(async, tree, node, childNodes, null);
    }

    public static <T extends DefaultMutableTreeNode> void loading(boolean async, JTree tree, DefaultMutableTreeNode node, Supplier<List<? extends DefaultMutableTreeNode>> childNodes, Consumer<List<? extends DefaultMutableTreeNode>> done) {
        loading(async, true, tree, node, childNodes, done);
    }

    public static <T extends DefaultMutableTreeNode> void reloadTree(JTree tree, DefaultMutableTreeNode node, List<? extends DefaultMutableTreeNode> nodes) {
        TreeModel model = tree.getModel();
        TreePath treePath = new TreePath(node.getPath());
        // 已被删除的节点
        if (!new TreePath(tree.getModel().getRoot()).isDescendant(treePath)) {
            return;
        }

        boolean isLoading = node.getChildCount() > 0 && node.getFirstChild() instanceof PlaceholderNode;
        if (CollectionUtils.isNotEmpty(nodes)) {
            node.removeAllChildren();
            nodes.forEach(node::add);
        } else if (isLoading) {
            PlaceholderNode firstChild = (PlaceholderNode) node.getFirstChild();
            firstChild.setText("暂无数据");
        } else {
            node.removeAllChildren();
            node.add(new PlaceholderNode("暂无数据"));
        }

        UIUtil.invokeLaterIfNeeded(() -> {
            try {
                ((DefaultTreeModel) model).reload(node);
            } catch (Exception e) {
                log.error("ignore reload fail: {}", e.getMessage());
            }
            // 如果需要展开, 则预触发展开事件, 以重新计算JTree滚动条高度
            if (tree.isExpanded(treePath)) {
                tree.fireTreeExpanded(treePath);
            }
        });

//        if (tree.isPathSelected(treePath)) {
//            // 如果已选中, 重新触发选中事件, 保证selectionListener重新触发获取最新数据(主要考虑节点信息被外部更新时, 需保证得到最新的数据)
//            tree.setSelectionPath(treePath);
//        }
    }

    /**
     * 树重置, 会保留展开状态
     *
     * @param tree 当前树
     * @param root 需要重置的节点, 一般为根节点
     */
    private static void resetTree(JTree tree, DefaultMutableTreeNode root) {
        Vector<TreePath> v = new Vector<>();
        analyzeExpandNode(tree, root, v);
        ((DefaultTreeModel) tree.getModel()).reload();

        for (TreePath treePath : v) {
            Object[] objArr = treePath.getPath();
            int len = objArr.length;
            Vector<Object> vec = new Vector<>(Arrays.asList(objArr).subList(0, len));
            expandNode(tree, root, vec);
        }
    }

    private static void analyzeExpandNode(JTree tree, TreeNode node, Vector<TreePath> v) {
        if (node.getChildCount() > 0) {
            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            TreePath treePath = new TreePath(model.getPathToRoot(node));
            if (tree.isExpanded(treePath)) v.add(treePath);
            for (Enumeration<? extends TreeNode> e = node.children(); e.hasMoreElements(); ) {
                TreeNode n = e.nextElement();
                analyzeExpandNode(tree, n, v);
            }
        }
    }

    /**
     * @param myTree   树
     * @param currNode 展开节点的父节点
     * @param vNode    展开节点，路径字符串|路径Node组成的Vector，按从根节点开始，依次添加到Vector
     */
    private static void expandNode(JTree myTree, DefaultMutableTreeNode currNode, Vector<Object> vNode) {
        if (currNode.getParent() == null) {
            vNode.removeElementAt(0);
        }
        if (vNode.isEmpty()) return;

        int childCount = currNode.getChildCount();
        String strNode = vNode.elementAt(0).toString();
        DefaultMutableTreeNode child = null;
        boolean flag = false;
        for (int i = 0; i < childCount; i++) {
            child = (DefaultMutableTreeNode) currNode.getChildAt(i);
            if (strNode.equals(child.toString())) {
                flag = true;
                break;
            }
        }
        if (child != null && flag) {
            vNode.removeElementAt(0);
            if (!vNode.isEmpty()) {
                expandNode(myTree, child, vNode);
            } else {
                myTree.expandPath(new TreePath(child.getPath()));
            }
        }
    }

    private static void addLoading(DefaultMutableTreeNode node) {
        // 层级展开, 数据查询返回前, 展示占位信息
        if (!node.children().hasMoreElements()) {
            node.add(new PlaceholderNode());
        }
    }
}
