package com.qihoo.finance.lowcode.common.util;

import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import com.qihoo.finance.lowcode.common.ui.base.FilterableTreeNode;
import com.qihoo.finance.lowcode.common.ui.base.SearchTreeCellRenderer;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.JTextComponent;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
public class JTreeToolbarUtils {
    public static final String INPUT_TIP = "请输入名称搜索...";

    @Data
    public static class SearchPanel {
        JPanel searchPanel;
        JTree tree;
        JTextField searchTxt;
    }

    public static SearchPanel createJTreeSearch(@NotNull Supplier<JTree> treeSupplier) {
        return createJTreeSearch(treeSupplier, null, true);
    }

    public static SearchPanel createJTreeSearch(@NotNull Supplier<JTree> treeSupplier, @NotNull Supplier<JTree> originalTreeSupplier) {
        return createJTreeSearch(treeSupplier, originalTreeSupplier, true);
    }

    public static SearchPanel createJTreeSearch(@NotNull Supplier<JTree> treeSupplier, Supplier<JTree> originalTreeSupplier, boolean emptyBorder) {
        JPanel searchPanel = new JPanel(new BorderLayout());
        JTree tree = treeSupplier.get();
        JTextField searchTxt = new JTextField();
        TreeCellRenderer treeCellRenderer = tree.getCellRenderer();
        if (!(treeCellRenderer instanceof SearchTreeCellRenderer cellRenderer)) {
            throw new RuntimeException("create JTree search panel exception: JTree must use SearchTreeCellRenderer");
        }
        cellRenderer.setSearchKeyTxt(searchTxt);

        SearchPanel searchPanelObj = new SearchPanel();
        searchPanelObj.setSearchPanel(searchPanel);
        searchPanelObj.setTree(tree);
        searchPanelObj.setSearchTxt(searchTxt);

        Border lineBorder = searchTxt.getBorder();
        //去掉文本框的边框线
        searchTxt.setBorder(null);
        //设为透明
        searchTxt.setOpaque(false);
        searchTxt.setBackground(JBColor.background());
        settingInputTips(searchTxt);

        searchTxt.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent event) {
                if (!INPUT_TIP.equals(searchTxt.getText())) {
                    filterTreeNodeVisible(treeSupplier, searchTxt);
                }
                if (StringUtils.isNotEmpty(searchTxt.getText().trim()) && !INPUT_TIP.equals(searchTxt.getText())) {
                    JTree currTree = treeSupplier.get();
                    TreePath pathForRow = currTree.getPathForRow(0);
                    if (Objects.nonNull(pathForRow)) {
                        currTree.scrollRectToVisible(currTree.getPathBounds(pathForRow));
                    }
                }
            }
        });

        // select hit
        JButton searchBtn = new JButton(Icons.scaleToWidth(Icons.SEARCH, 16));
        searchBtn.setToolTipText(INPUT_TIP);
        searchBtn.setBorderPainted(false);
        searchBtn.setContentAreaFilled(false);
        searchBtn.setPreferredSize(new Dimension(22, 22));
//        searchBtn.addActionListener(e -> doSearch(searchPanelObj));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnPanel.setOpaque(false);
        btnPanel.add(searchBtn);

        searchPanel.setLayout(new BorderLayout(2, 0));
        if (!emptyBorder) {
            searchPanel.setBorder(lineBorder);
        }

        searchPanel.add(searchTxt, BorderLayout.CENTER);
        searchPanel.add(btnPanel, BorderLayout.EAST);
        searchPanel.setPreferredSize(new Dimension(-1, 25));
        searchPanel.setOpaque(false);
        return searchPanelObj;
    }

    private static void filterTreeNodeVisible(@NotNull Supplier<JTree> treeSupplier, JTextField searchTxt) {
        JTree tree = treeSupplier.get();
        String key = searchTxt.getText();
        String lowCaseKey = key.toLowerCase();
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        // 备份展开状态
//        Enumeration<TreePath> expandedDescendants = tree.getExpandedDescendants(new TreePath(root));
        List<TreePath> willExpends = new ArrayList<>();
        new SwingWorker<>() {
            @Override
            protected Object doInBackground() {
                // 标记过滤
                Enumeration<TreeNode> nodes = root.children();
                while (nodes.hasMoreElements()) {
                    TreeNode treeNode = nodes.nextElement();
                    if (treeNode instanceof FilterableTreeNode filterNode) {
                        if (JTreeToolbarUtils.INPUT_TIP.equalsIgnoreCase(key) || StringUtils.isEmpty(key)) {
                            filterNode.setVisible(true);
                        } else {
                            String lowCaseNode = StringUtils.defaultString(filterNode.toString()).toLowerCase();
                            filterNode.setVisible(lowCaseNode.contains(lowCaseKey));
                        }
                        recursionExpend(tree, filterNode, lowCaseKey, willExpends);
                    }
                }

                // reload 并恢复展开状态
                return null;
            }

            @Override
            protected void done() {
                UIUtil.invokeLaterIfNeeded(() -> {
                    TreeSelectionListener[] selectionListeners = tree.getTreeSelectionListeners();
                    for (TreeSelectionListener selectionListener : selectionListeners) {
                        tree.removeTreeSelectionListener(selectionListener);
                    }
                    model.reload();
                    for (TreePath treePath : willExpends) {
                        if (!tree.isExpanded(treePath)) {
                            tree.expandPath(treePath);
                        }
                    }
//                    while (expandedDescendants.hasMoreElements()) {
//                        TreePath treePath = expandedDescendants.nextElement();
//                        // 恢复展开
//                        tree.expandPath(treePath);
//                    }

                    for (TreeSelectionListener selectionListener : selectionListeners) {
                        tree.addTreeSelectionListener(selectionListener);
                    }
                });
                super.done();
            }
        }.execute();
    }

    public static void recursionExpend(JTree tree, FilterableTreeNode node, String lowCaseKey, List<TreePath> willExpends) {
        boolean visible = node.isVisible();

        TreePath treePath = new TreePath(node.getPath());
        if (/*tree.isExpanded(treePath) &&*/ node.getChildCount() > 0) {
            for (Enumeration<? extends TreeNode> e = node.children(); e.hasMoreElements(); ) {
                TreeNode treeNode = e.nextElement();
                if (treeNode instanceof FilterableTreeNode filterNode) {
                    if (visible) {
                        // 父级命中, 子级全展示
                        filterNode.setVisible(true);
                    } else if (JTreeToolbarUtils.INPUT_TIP.equalsIgnoreCase(lowCaseKey) || StringUtils.isEmpty(lowCaseKey)) {
                        filterNode.setVisible(true);
                    } else {
                        String lowCaseNode = StringUtils.defaultString(filterNode.toString()).toLowerCase();
                        if (lowCaseNode.contains(lowCaseKey)) {
                            filterNode.setVisible(true);
                            setParentVisible(tree, filterNode);
                            willExpends.add(treePath);
                        } else {
                            filterNode.setVisible(false);
                        }
                    }

                    recursionExpend(tree, filterNode, lowCaseKey, willExpends);
                }
            }
        }
    }

    private static void setParentVisible(JTree tree, FilterableTreeNode filterNode) {
        TreeNode parent = filterNode.getParent();
        if (Objects.nonNull(parent) && parent instanceof FilterableTreeNode parentFilterNode) {
            parentFilterNode.setVisible(true);
            setParentVisible(tree, parentFilterNode);
        }
    }

    private static void doSearch(SearchPanel searchPanelObj) {
        JTree tree = searchPanelObj.getTree();
        if (Objects.isNull(tree)) return;

        TreeCellRenderer treeCellRenderer = tree.getCellRenderer();
        if (!(treeCellRenderer instanceof SearchTreeCellRenderer)) return;

        SearchTreeCellRenderer cellRenderer = (SearchTreeCellRenderer) treeCellRenderer;
        JTextField searchTxt = cellRenderer.getSearchKeyTxt();

        String searchKey = searchTxt.getText().trim();
        clearSearch(tree);

        if (StringUtils.isNotEmpty(searchKey)) {
            search(tree, searchKey);
        }

        tree.repaint();
    }

    private static void search(JTree tree, String searchKey) {
        if (Objects.isNull(tree)) return;
        // 添加选中状态
        addSelectionPathFromSearch(tree, searchKey);
    }

    private static void scrollToHit(JTree tree, JTextField searchTxt) {
        String searchKey = searchTxt.getText().trim();
        if (StringUtils.isEmpty(searchKey)) return;

        TreeModel model = tree.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        Enumeration<TreeNode> nodes = root.depthFirstEnumeration();

        TreePath containsPath = null;
        TreePath startsWithPath = null;
        while (nodes.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
            TreeNode parent = node.getParent();
            if (Objects.isNull(parent)) continue;
            if (!tree.isExpanded(new TreePath(((DefaultMutableTreeNode) parent).getPath()))) continue;

            TreePath path = new TreePath(node.getPath());
            String nodeName = ObjectUtils.defaultIfNull(node.getUserObject(), node).toString();
            nodeName = StringUtils.defaultString(nodeName);
            if (nodeName.startsWith(searchKey)) {
                tree.scrollPathToVisible(path);
                startsWithPath = path;
                break;
            }
            if (nodeName.contains(searchKey)) {
                containsPath = path;
            }
        }

        if (Objects.nonNull(containsPath) && Objects.isNull(startsWithPath)) {
            tree.scrollPathToVisible(containsPath);
        }
    }

    private static void addSelectionPathFromSearch(JTree tree, String searchKey) {
        TreeModel model = tree.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        Enumeration<TreeNode> nodes = root.depthFirstEnumeration();
        while (nodes.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
            TreeNode parent = node.getParent();
            if (Objects.isNull(parent)) continue;

            DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) parent;
            String lowCaseNode = StringUtils.defaultString(node.toString()).toLowerCase();
            String lowCaseKey = searchKey.toLowerCase();
            if (tree.isExpanded(new TreePath(parentNode.getPath())) && lowCaseNode.contains(lowCaseKey)) {
                tree.addSelectionPath(new TreePath(node.getPath()));
            }
        }
    }

    private static void clearSearch(JTree tree) {
        if (Objects.isNull(tree)) return;
        tree.clearSelection();
    }

    public static void settingInputTips(JTextComponent textComponent) {
        if (textComponent.getText().isEmpty()) {
            textComponent.setForeground(JBColor.foreground().darker());
            textComponent.setText(INPUT_TIP);
        }

        textComponent.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (textComponent.getText().equals(INPUT_TIP)) {
                    textComponent.setForeground(JBColor.foreground());
                    textComponent.setText("");
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (textComponent.getText().isEmpty()) {
                    textComponent.setForeground(JBColor.foreground().darker());
                    textComponent.setText(INPUT_TIP);
                }
            }
        });
    }

    @SafeVarargs
    public static JButton createExpandAll(@NotNull Supplier<JTree> treeSupplier, Class<? extends DefaultMutableTreeNode>... ignores) {
        JButton btn = changeExpandStatusBtn(treeSupplier, true, ignores);
        btn.setIcon(Icons.scaleToWidth(Icons.Actions.Expandall, 16));
        btn.setToolTipText("展开所有节点");

        return btn;
    }

    @SafeVarargs
    public static JButton createCollapseAll(@NotNull Supplier<JTree> treeSupplier, Class<? extends DefaultMutableTreeNode>... ignores) {
        JButton btn = changeExpandStatusBtn(treeSupplier, false, ignores);
        btn.setIcon(Icons.scaleToWidth(Icons.Actions.Collapseall, 16));
        btn.setToolTipText("收缩所有节点");

        return btn;
    }

    @SafeVarargs
    private static JButton changeExpandStatusBtn(@NotNull Supplier<JTree> treeSupplier, boolean expend, Class<? extends DefaultMutableTreeNode>... ignores) {
        JButton changeExpendStatus = new JButton();
        changeExpendStatus.setBorderPainted(false);
        changeExpendStatus.setContentAreaFilled(false);
        changeExpendStatus.setPreferredSize(new Dimension(22, 22));
        changeExpendStatus.addActionListener(e -> {
            changeExpandStatus(treeSupplier, expend, ignores);
        });

        return changeExpendStatus;
    }

    @SafeVarargs
    private static void changeExpandStatus(@NotNull Supplier<JTree> treeSupplier, boolean expend, Class<? extends DefaultMutableTreeNode>... ignores) {
        JTree tree = treeSupplier.get();
        // 获取根节点
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();

        // 枚举树中的所有节点
        Enumeration<?> e = root.breadthFirstEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();

            // 忽略节点
            boolean ignoreNode = false;
            for (Class<? extends DefaultMutableTreeNode> ignore : ignores) {
                if (ignore == node.getClass()) {
                    ignoreNode = true;
                    break;
                }
            }

            if (ignoreNode) continue;

            // 展开节点
            if (node.isLeaf()) {
                continue;
            }

            int row = tree.getRowForPath(new TreePath(node.getPath()));
            if (expend) {
                tree.expandRow(row);
            } else {
                tree.collapseRow(row);
            }
        }
    }

    public static JPanel createToolbarPanel() {
        JPanel toolbar = new JPanel(new BorderLayout());
        Border lineBorder = BorderFactory.createLineBorder(JBColor.background().brighter());
        Border searchBorder = BorderFactory.createEmptyBorder(-2, 0, -2, 0);
        toolbar.setBorder(BorderFactory.createCompoundBorder(lineBorder, searchBorder));
        toolbar.setLayout(new BorderLayout(2, 0));
        toolbar.setOpaque(false);

        return toolbar;
    }

    public static JPanel createToolbarSearch(JTextField search, JButton searchBtn) {
        //去掉文本框的边框线
        search.setBorder(null);
        //设为透明
        search.setOpaque(false);
        search.setBackground(JBColor.background());

        searchBtn.setIcon(Icons.scaleToWidth(Icons.SEARCH, 16));
        searchBtn.setToolTipText(INPUT_TIP);
        searchBtn.setBorderPainted(false);
        searchBtn.setContentAreaFilled(false);
        searchBtn.setPreferredSize(new Dimension(20, 20));

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.add(search, BorderLayout.CENTER);
        searchPanel.add(searchBtn, BorderLayout.EAST);
        searchPanel.setPreferredSize(new Dimension(-1, 20));
        searchPanel.setOpaque(false);
        // 合并边框
        searchPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(6, 0, 6, 0),
                BorderFactory.createLineBorder(JBColor.border().brighter(), 2, true))
        );

        return searchPanel;
    }

    public static JButton createToolbarButton(String btn, Icon icon, String toolTipText) {
        JButton button = new JButton();
        if (StringUtils.isNotEmpty(btn)) {
            button.setText(btn);
        }
        if (StringUtils.isNotEmpty(toolTipText)) {
            button.setToolTipText(toolTipText);
        }
        if (Objects.nonNull(icon)) {
            button.setIcon(Icons.scaleToWidth(icon, 16));
        }

        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setPreferredSize(new Dimension(16, 16));

        return button;
    }

    public static JButton createToolbarButton(Icon icon, String toolTipText) {
        return createToolbarButton(null, icon, toolTipText);
    }

    public static JButton createToolbarButton(String btn, String toolTipText) {
        return createToolbarButton(btn, null, toolTipText);
    }

    public static JProgressBar createIndeterminateProgressBar(String tips) {
        JProgressBar progressBar = new JProgressBar();
        progressBar.setStringPainted(true);// 设置显示提示信息
        progressBar.setIndeterminate(true);// 设置采用不确定进度条
        progressBar.setString(tips);// 设置提示信息
        return progressBar;
    }

    public static JProgressBar createIndeterminateProgressBar() {
        JProgressBar progressBar = new JProgressBar();
        progressBar.setStringPainted(true);// 设置显示提示信息
        progressBar.setIndeterminate(true);// 设置采用不确定进度条
        return progressBar;
    }

    private static final Map<JProgressBar, Integer> count = new ConcurrentHashMap<>();

    public static <T> void progressWorker(JProgressBar progressBar, Supplier<T> doInBackground, Consumer<T> done) {
        new SwingWorker<T, T>() {
            @Override
            protected T doInBackground() {
                showProgress(progressBar);
                if (Objects.nonNull(doInBackground)) {
                    try {
                        return doInBackground.get();
                    } catch (Exception e) {
                        // log error
                        log.error("progressWorker exception: {}", e.getMessage(), e);
                    }
                }

                return null;
            }

            @SneakyThrows
            @Override
            protected void done() {
                if (Objects.nonNull(done)) {
                    done.accept(get());
                }
                // 并发时控制由最慢完成的线程进行隐藏进度条
                hiddenProgress(progressBar);
                super.done();
            }
        }.execute();
    }

    private static void showProgress(JProgressBar progressBar) {
        if (count.containsKey(progressBar)) {
            count.put(progressBar, count.get(progressBar) + 1);
        } else {
            count.put(progressBar, 1);
        }

        progressBar.setVisible(true);
    }


    private static void hiddenProgress(JProgressBar progressBar) {
        if (count.containsKey(progressBar)) {
            Integer countProgress = JTreeToolbarUtils.count.get(progressBar);
            if (countProgress > 1) {
                count.put(progressBar, countProgress - 1);
                return;
            }
        }

        count.remove(progressBar);
        progressBar.setVisible(false);
    }

    public static void progressWorker(JProgressBar progressBar, Runnable doInBackground) {
        new SwingWorker<>() {
            @Override
            protected Object doInBackground() {
                showProgress(progressBar);
                doInBackground.run();
                return null;
            }

            @Override
            protected void done() {
                hiddenProgress(progressBar);
                super.done();
            }
        }.execute();
    }

    public static void progressWorker(JProgressBar progressBar, Runnable doInBackground, Runnable done) {
        new SwingWorker<>() {
            @Override
            protected Object doInBackground() {
                showProgress(progressBar);
                doInBackground.run();
                return null;
            }

            @Override
            protected void done() {
                if (Objects.nonNull(done)) {
                    done.run();
                }
                hiddenProgress(progressBar);
                super.done();
            }
        }.execute();
    }
}
