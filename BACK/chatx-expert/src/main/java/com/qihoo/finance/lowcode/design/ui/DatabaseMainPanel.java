package com.qihoo.finance.lowcode.design.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import com.qihoo.finance.lowcode.common.configuration.UserContextPersistent;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.entity.enums.OrderBy;
import com.qihoo.finance.lowcode.common.ui.ToolBarPanel;
import com.qihoo.finance.lowcode.common.ui.base.SearchTreeCellRenderer;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.JTreeToolbarUtils;
import com.qihoo.finance.lowcode.design.dto.SimpleDatasource;
import com.qihoo.finance.lowcode.design.entity.*;
import com.qihoo.finance.lowcode.design.ui.tree.MongoTreePanel;
import com.qihoo.finance.lowcode.design.ui.tree.MySQLTreePanel;
import com.qihoo.finance.lowcode.design.util.DatabaseDesignUtils;
import com.qihoo.finance.lowcode.common.action.UserInfoAction;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.*;

/**
 * DatabaseMainPanel
 *
 * @author fengjinfu-jk
 * @version 1.0
 */
@Getter
public class DatabaseMainPanel extends DatabaseBasePanel {

    private final DatabaseTreePanelFactory treeFactory;
    private final ToolBarPanel toolBarPanel;
    private final Map<OrderBy, JBMenuItem> orderBys = new HashMap<>();
    private JBPopupMenu datasourcePopup;
    private JTreeToolbarUtils.SearchPanel searchPanel;
    private final JButton databaseLabel;
    private final JButton datasource;
    private final JPanel treePanel;
    private final SimpleDatasource simpleDatasource;
    private final DataContext dataContext;
    private final Project project;
    private final JTabbedPane dsTab;
    private final UserInfoAction userInfoAction;


    public DatabaseMainPanel(@NotNull Project project) {
        super(project);
        this.project = project;
        treeFactory = project.getService(DatabaseTreePanelFactory.class);
        toolBarPanel = project.getService(ToolBarPanel.class);

        databaseLabel = new JButton();
        databaseLabel.setBorderPainted(false);
        databaseLabel.setContentAreaFilled(false);
        databaseLabel.setVisible(false);

        simpleDatasource = dataSourceType();
        datasource = JTreeToolbarUtils.createToolbarButton(simpleDatasource.getIcon(), String.format("%s  点击切换其他数据源", simpleDatasource.getDatasource()));
        treePanel = new JPanel(new BorderLayout());
        treePanel.setBorder(null);
        dsTab = new JBTabbedPane();
        JComponent mySQL = treeFactory.switchDatabaseView(Constants.DataSource.MySQL);
        mySQL.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JComponent mongoDB = treeFactory.switchDatabaseView(Constants.DataSource.MongoDB);
        mongoDB.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        dsTab.addTab("MySQL", AllIcons.Providers.Mysql, mySQL);
        dsTab.addTab("MongoDB", AllIcons.Providers.MongoDB, mongoDB);

        dsTab.setTabPlacement(JTabbedPane.BOTTOM);
        dsTab.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        dataContext = DataContext.getInstance(project);
        userInfoAction = project.getService(UserInfoAction.class);
    }

    private void resetCenterPanel() {
        treePanel.removeAll();
        treePanel.revalidate();
        treePanel.add(dsTab, BorderLayout.CENTER);
//        treePanel.add(treeFactory.switchDatabaseView(), BorderLayout.CENTER);
        treePanel.repaint();

        JTree tree = treeFactory.databaseTreePanel().getTree();
        if (Objects.isNull(searchPanel))
            searchPanel = JTreeToolbarUtils.createJTreeSearch(dataContext::getDbTree, () -> {
                JTree originalTree = new JTree();
                treeFactory.databaseTreePanel().loadDepAndDbTree(originalTree, false);
                return originalTree;
            });
        searchPanel.setTree(tree);
        TreeCellRenderer cellRenderer = tree.getCellRenderer();
        if (cellRenderer instanceof SearchTreeCellRenderer) {
            ((SearchTreeCellRenderer) cellRenderer).setSearchKeyTxt(searchPanel.getSearchTxt());
        }
    }

    @Override
    public JComponent createPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.removeAll();
        JPanel toolbar = JTreeToolbarUtils.createToolbarPanel();
        panel.add(toolbar, BorderLayout.NORTH);
        treePanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        panel.add(treePanel, BorderLayout.CENTER);
        resetCenterPanel();
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnPanel.setOpaque(false);

        // 全部展开
        JButton expandAll = JTreeToolbarUtils.createExpandAll(
                dataContext::getDbTree,
                MySQLTableNode.class,
                DatabaseFolderNode.class,
                DatabaseColumnNode.class,
                MongoCollectionNode.class
        );
        expandAll.setToolTipText("展开所有数据库表");
        btnPanel.add(expandAll);

        // 全部收缩
        JButton collapseAll = JTreeToolbarUtils.createCollapseAll(dataContext::getDbTree);
        collapseAll.setToolTipText("收缩所有数据库表");
        btnPanel.add(collapseAll);

        // 排序
        JButton sortBtn = JTreeToolbarUtils.createToolbarButton(Icons.SORT, "排序");
        sortBtn.addActionListener(e -> {
            JTree tree = DataContext.getInstance(project).getDbTree();
            JBPopupMenu sortPopup = createSortPopup(tree);
            int x = toolbar.getWidth() - sortPopup.getWidth();
            int y = toolbar.getY() + toolbar.getHeight() + 8;
            toolbar.setVisible(true);
            try {
                sortPopup.show(toolbar, x, y);
            } catch (Exception ignore) {
                // ignore
            }
        });
        btnPanel.add(sortBtn);

        // 数据源
        datasource.addActionListener(e -> {
            JBPopupMenu popup = getDatasourcePopup(datasource);
            int x = toolbar.getWidth() - popup.getWidth();
            int y = toolbar.getY() + toolbar.getHeight() + 8;
            toolbar.setVisible(true);
            try {
                popup.show(toolbar, x, y);
            } catch (Exception ignore) {
                // ignore
            }
        });
//        btnPanel.add(datasource);

        dsTab.addChangeListener(c -> {
            int selectedIndex = dsTab.getSelectedIndex();
            if (selectedIndex == 0) {
                // MySQL
                mysqlItem().getAction().actionPerformed(null);
                MySQLTreePanel service = project.getService(MySQLTreePanel.class);
                service.updateDatabaseContext();
            } else {
                // MongoDB
                mongoDBItem().getAction().actionPerformed(null);
                MongoTreePanel service = project.getService(MongoTreePanel.class);
                service.updateDatabaseContext();
            }
        });

        // 刷新
        btnPanel.add(userInfoAction.refreshButton());
        toolbar.add(searchPanel.getSearchPanel(), BorderLayout.CENTER);
        toolbar.add(btnPanel, BorderLayout.EAST);

        // 底部tips
        JPanel tipsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        databaseLabel.setText(simpleDatasource.getDatasource());
        databaseLabel.setIcon(simpleDatasource.getIcon());
        databaseLabel.addActionListener(e -> {
            JBPopupMenu popup = getDatasourcePopup(datasource);
            int x = toolbar.getWidth() - popup.getWidth();
            int y = -110;
            tipsPanel.setVisible(true);
            try {
                popup.show(tipsPanel, x, y);
            } catch (Exception ignore) {
                // ignore
            }
        });
        tipsPanel.add(databaseLabel);
        tipsPanel.setOpaque(false);
        panel.add(tipsPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JBPopupMenu getDatasourcePopup(JButton datasourceBtn) {
        if (Objects.isNull(datasourcePopup)) datasourcePopup = createDatasourcePopup(datasourceBtn);
        return datasourcePopup;
    }

    private JBPopupMenu createDatasourcePopup(JButton button) {
        button.setIcon(dataSourceType().getIcon());
        JBPopupMenu popupMenu = new JBPopupMenu();
        popupMenu.setBorder(BorderFactory.createLineBorder(JBColor.GRAY));
        popupMenu.getInsets().set(50, 50, 50, 50);
        popupMenu.add(Box.createVerticalStrut(8));

        popupMenu.add(mysqlItem());
        popupMenu.add(Box.createVerticalStrut(6));
        popupMenu.add(mongoDBItem());
        popupMenu.addSeparator();
        popupMenu.add(oracleDBItem());
        popupMenu.add(Box.createVerticalStrut(6));
        popupMenu.add(postgresqlItem());

        popupMenu.add(Box.createVerticalStrut(2));
        popupMenu.addSeparator();
        return popupMenu;
    }

    public SimpleDatasource dataSourceType() {
        String datasourceType = StringUtils.defaultString(DataContext.getInstance(project).getDatasourceType(), Constants.DataSource.MySQL);
        switch (datasourceType) {
            case Constants.DataSource.MongoDB:
                return new SimpleDatasource("MongoDB", Icons.Providers.MongoDB);
            case Constants.DataSource.Oracle:
                return new SimpleDatasource("Oracle", Icons.Providers.Oracle);
            case Constants.DataSource.Postgresql:
                return new SimpleDatasource("Postgresql", Icons.Providers.Postgresql);
            default:
                return new SimpleDatasource("MySQL", Icons.Providers.Mysql);
        }
    }

    private JBMenuItem postgresqlItem() {
        JBMenuItem menuItem = new JBMenuItem(new AbstractAction("Postgresql") {
            @Override
            public void actionPerformed(ActionEvent e) {
                // todo
            }
        });
        // 设置自定义边距
        menuItem.setMargin(JBUI.insets(0, 10));
        menuItem.setIcon(AllIcons.Providers.Postgresql);
        // 暂不支持
        menuItem.setEnabled(false);
        return menuItem;
    }

    private JBMenuItem oracleDBItem() {
        JBMenuItem menuItem = new JBMenuItem(new AbstractAction("Oracle") {
            @Override
            public void actionPerformed(ActionEvent e) {
                // todo
            }
        });
        // 设置自定义边距
        menuItem.setMargin(JBUI.insets(0, 10));
        menuItem.setIcon(AllIcons.Providers.Oracle);
        // 暂不支持
        menuItem.setEnabled(false);
        return menuItem;
    }

    private JBMenuItem mongoDBItem() {
        JBMenuItem menuItem = new JBMenuItem(new AbstractAction("MongoDB") {
            @Override
            public void actionPerformed(ActionEvent e) {
                DataContext dataContext = DataContext.getInstance(project);
                dataContext.setDatasourceType(Constants.DataSource.MongoDB);

                datasource.setIcon(AllIcons.Providers.MongoDB);
                datasource.setToolTipText("MongoDB 点击切换其他数据源");
                databaseLabel.setIcon(AllIcons.Providers.MongoDB);
                databaseLabel.setText("MongoDB");
                databaseLabel.setToolTipText("MongoDB 点击切换其他数据源");

                resetCenterPanel();
            }
        });
        // 设置自定义边距
        menuItem.setMargin(JBUI.insets(0, 10));
        menuItem.setIcon(AllIcons.Providers.MongoDB);
        // fixme: 暂未可用
//        menuItem.setEnabled(false);
        return menuItem;
    }

    private JBMenuItem mysqlItem() {
        JBMenuItem menuItem = new JBMenuItem(new AbstractAction("MySQL") {
            @Override
            public void actionPerformed(ActionEvent e) {
                DataContext dataContext = DataContext.getInstance(project);
                dataContext.setDatasourceType(Constants.DataSource.MySQL);

                datasource.setIcon(AllIcons.Providers.Mysql);
                datasource.setToolTipText("MySQL 点击切换其他数据源");
                databaseLabel.setIcon(AllIcons.Providers.Mysql);
                databaseLabel.setText("MySQL");
                databaseLabel.setToolTipText("MySQL 点击切换其他数据源");

                resetCenterPanel();
            }
        });
        // 设置自定义边距
        menuItem.setMargin(JBUI.insets(0, 10));
        menuItem.setIcon(AllIcons.Providers.Mysql);
        return menuItem;
    }

    private JBPopupMenu createSortPopup(JTree tree) {
        String dbSortType = StringUtils.defaultString(UserContextPersistent.getUserContext().orderBy, OrderBy.UPDATE_TIME_DESC.name());

        JBPopupMenu popupMenu = new JBPopupMenu();
        popupMenu.setBorder(BorderFactory.createLineBorder(JBColor.GRAY));
        popupMenu.getInsets().set(50, 50, 50, 50);
        popupMenu.add(Box.createVerticalStrut(8));

        OrderBy[] values = OrderBy.values();
        for (int i = 0; i < values.length; i++) {
            OrderBy orderBy = values[i];
            JBMenuItem orderByItem = orderByItem(orderBy, tree);
            popupMenu.add(orderByItem);
            orderBys.put(values[i], orderByItem);

            if (i % 2 != 0 && i < values.length - 1) {
                popupMenu.addSeparator();
            }
            if (orderBy.name().equalsIgnoreCase(dbSortType)) {
                orderByItem.setIcon(Icons.scaleToWidth(Icons.SELECTED, 16));
            } else {
                orderByItem.setIcon(Icons.scaleToWidth(Icons.TRANSPARENT, 16));
            }
        }

        popupMenu.add(Box.createVerticalStrut(2));
        popupMenu.addSeparator();
        return popupMenu;
    }

    private JBMenuItem orderByItem(OrderBy orderBy, JTree tree) {
        JBMenuItem menuItem = new JBMenuItem(new AbstractAction(orderBy.getShowText()) {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Icons
                orderBys.forEach((o, m) -> m.setIcon((orderBy.equals(o)) ? Icons.scaleToWidth(Icons.SELECTED, 16) : Icons.scaleToWidth(Icons.TRANSPARENT, 16)));
                // sortType
                UserContextPersistent.getUserContext().orderBy = orderBy.name();
                // reload table nodes by sortType
                reloadBySort(tree, (TreeNode) tree.getModel().getRoot(), orderBy);
            }
        });
        // 设置自定义边距
        menuItem.setMargin(JBUI.insets(0, 10));
        return menuItem;
    }

    private void reloadBySort(JTree tree, TreeNode node, OrderBy orderBy) {
        for (Enumeration<? extends TreeNode> e = node.children(); e.hasMoreElements(); ) {
            TreeNode n = e.nextElement();
            if (n instanceof SchemaNode && n.getChildCount() > 0) {
                List<AbstractTableNode> tableNodes = new ArrayList<>();
                Enumeration<? extends TreeNode> children = n.children();
                while (children.hasMoreElements()) {
                    TreeNode treeNode = children.nextElement();
                    if (treeNode instanceof AbstractTableNode) {
                        tableNodes.add((AbstractTableNode) treeNode);
                    }
                }
                ((SchemaNode) n).removeAllChildren();
                DatabaseDesignUtils.sortTables(tableNodes, orderBy);
                tableNodes.forEach(((SchemaNode) n)::add);
                ((DefaultTreeModel) tree.getModel()).reload(n);

                continue;
            }

            if (n.getChildCount() > 0) {
                reloadBySort(tree, n, orderBy);
            }
        }
    }
}
