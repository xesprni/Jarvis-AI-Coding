package com.qihoo.finance.lowcode.declarative.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.*;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.entity.dto.SQLBatchExecuteResult;
import com.qihoo.finance.lowcode.common.entity.dto.declarative.DiffDatabase;
import com.qihoo.finance.lowcode.common.entity.dto.declarative.DiffResult;
import com.qihoo.finance.lowcode.common.entity.dto.declarative.DiffTable;
import com.qihoo.finance.lowcode.common.ui.base.FilterableTreeModel;
import com.qihoo.finance.lowcode.common.ui.base.FilterableTreeNode;
import com.qihoo.finance.lowcode.common.ui.base.RoundedLabel;
import com.qihoo.finance.lowcode.common.ui.base.SearchTreeCellRenderer;
import com.qihoo.finance.lowcode.common.util.*;
import com.qihoo.finance.lowcode.console.mysql.result.ui.ResultView;
import com.qihoo.finance.lowcode.declarative.entity.DiffDatabaseNode;
import com.qihoo.finance.lowcode.declarative.entity.DiffTableNode;
import com.qihoo.finance.lowcode.declarative.listener.DiffTreeMouseListener;
import com.qihoo.finance.lowcode.declarative.listener.DiffTreeSelectionListener;
import com.qihoo.finance.lowcode.declarative.util.DeclarativeUtils;
import com.qihoo.finance.lowcode.design.entity.DatabaseNode;
import com.qihoo.finance.lowcode.design.util.DatabaseDesignUtils;
import com.qihoo.finance.lowcode.gentracker.tool.FileUtils;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import com.qihoo.finance.lowcode.gentracker.ui.component.LeftRightComponent;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.qihoo.finance.lowcode.common.constants.Constants.Headers.OPERATE_PATH;

/**
 * DeclarativeDialog
 *
 * @author fengjinfu-jk
 * date 2024/4/25
 * @version 1.0.0
 * @apiNote DeclarativeDialog
 */
public class DeclarativeDialog extends DialogWrapper implements Disposable {
    private final Project project;
    private LoadingDecorator dialogDecorator;
    private JPanel mainPanel;
    private LeftRightComponent contentPanel;
    private JPanel dialogPanel;
    private JPanel leftPanel;
    private JLabel dbLabel;
    private JTabbedPane treeTab;
    private ComboBox<DatabaseNode> relateActualDB;
    private Map<String, String> declaresDBTables;
    private String tableIgnore;
    private final Set<VirtualFile> files;
    public static final String TABLE_IGNORE = ".tableignore";
    public static final String SQL_SUFFIX = ".sql";
    private final DeclareDiffPanel diffPanel;
    private final JProgressBar progress;
    private final JLabel tips;
    private final String declarativePath;
    private DiffDatabaseNode currentDBNode;
    private final Map<String, String> declare_actual_db = new HashMap<>();
    private Tree diffTree;
    private Tree sameTree;
    private Tree ignoreTree;

    private static final DataContext dataContext = DataContext.getInstance(ProjectUtils.getCurrProject());
    private final Map<String, DatabaseNode> databaseMap;
    private static final GridLayout gridLayout2 = new GridLayout(-1, 2);

    public String databaseWithInstance(DatabaseNode databaseNode) {
        return String.format("%s [%s]", databaseNode.getName(), databaseNode.getInstanceName());
    }

    public DeclarativeDialog(@NotNull Project project, Set<VirtualFile> files, String declarativePath) {
        super(project);
        this.project = project;
        this.declarativePath = declarativePath;
        this.files = files;
        this.progress = JPanelUtils.createProgress();
        this.tips = new JLabel("注意：如果文件内容不是最新的, 请关闭文件后重新打开，或者右键文件并选择 Reload from Disk 刷新文件后，再进行声明式SQL变更操作");
        this.tips.setForeground(Icons.WARNING_COLOR);
        this.tips.setIcon(Icons.scaleToWidth(Icons.FTD, 16));
        this.tips.setHorizontalAlignment(JLabel.RIGHT);
        this.diffPanel = new DeclareDiffPanel();
        this.databaseMap = dataContext.getAllMySQLDatabaseList()
                .stream().collect(Collectors.toMap(this::databaseWithInstance, Function.identity(), (v1, v2) -> v1));

        // components
        initComponents();
        // events
        initEvents();
        // init data
        initData("解析MySQL声明式变更文件中...");
        // dialog init setting
        initDialog();
    }

    private Tree initTree() {
        FilterableTreeNode root = new FilterableTreeNode("奇富科技");
        FilterableTreeModel model = new FilterableTreeModel(root);
        model.setRoot(root);
        return new Tree(model);
    }

    private void initDialog() {
        setModal(false);
        setTitle("MySQL声明式变更");
        setOKButtonText("执行");
        setCancelButtonText("关闭");
        init();
        toFront();
    }

    @Override
    protected void doOKAction() {
        execute();
    }

    private void reload() {
        // 获取当前执行的 database.table
        DiffTableNode tableNode = diffPanel.getTableNode();
        new SwingWorker<DiffTable, DiffTable>() {
            @Override
            protected DiffTable doInBackground() {
                // 重新查询此 database.table 与 declarativeSQL 文件中的 diff
                String declareDDL = declaresDBTables.get(tableNode.getDatabaseName());
                return DeclarativeUtils.diffTable(tableNode, declareDDL);
            }

            @SneakyThrows
            @Override
            protected void done() {
                DiffTable table = get();
                // 更新 diffPanel
                tableNode.setDbDDL(table.getDbDDL());
                tableNode.setDeclareDDL(table.getDeclareDDL());
                tableNode.setDiffDDLs(ListUtils.defaultIfNull(table.getDiffDDLs(), new ArrayList<>()));
                tableNode.setDiffType(table.getDiffType());
                stopLoadingAndRepaint();
                reloadTree(diffTree, tableNode);
                NotifyUtils.notifyBalloon(getButtonMap().get(getOKAction()),
                        "执行成功", MessageType.INFO, true);
                super.done();
            }
        }.execute();
    }

    private void reloadTree(Tree tree, DiffTableNode tableNode) {
        Enumeration<TreePath> expandedDescendants = tree.getExpandedDescendants(new TreePath(tree.getModel().getRoot()));
        TreePath selectionPath = tree.getSelectionPath();
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        model.reload();
        // idea version >= 23.3 版本时, 会存在Tree节点自动隐藏的兼容性问题, 后续需要手动进行reload重置树
        // 重新展开节点并选中当前节点，同时滚动条滚动到节点位置
        while (expandedDescendants.hasMoreElements()) {
            TreePath treePath = expandedDescendants.nextElement();
            // 恢复展开
            tree.expandPath(treePath);
        }
        tree.setSelectionPath(selectionPath);
        tree.scrollPathToVisible(selectionPath);
    }

    private void execute() {
        // 获取当前执行的 database.table
        DiffTableNode tableNode = diffPanel.getTableNode();
        DatabaseNode actualDatabaseNode = tableNode.getDatabase().getActualDatabase();
        if (Objects.isNull(actualDatabaseNode)) {
            NotifyUtils.notifyBalloon(getButtonMap().get(getOKAction()),
                    "执行失败, 找不到数据库实例信息", MessageType.ERROR, true);
            return;
        }
        // 组装SQL
        List<String> selectDDLs = diffPanel.getSelectedDDLs();
        String sql = String.join("\n ", selectDDLs);
        // 冻结窗口
        frozenDialog("执行中...");
        new SwingWorker<Result<SQLBatchExecuteResult>, Result<SQLBatchExecuteResult>>() {
            @Override
            protected Result<SQLBatchExecuteResult> doInBackground() {
                Map<String, String> headers = new HashMap<>();
                headers.put(OPERATE_PATH, "declarative.execute");
                return DatabaseDesignUtils.batchExecuteConsoleSQL(headers, actualDatabaseNode, sql);
            }

            @SneakyThrows
            @Override
            protected void done() {
                Result<SQLBatchExecuteResult> result = get();
                if (result.isFail()) {
                    SQLBatchExecuteResult executeResult = new SQLBatchExecuteResult();
                    result.setData(executeResult);
                    executeResult.getResultOverview().add(new SQLBatchExecuteResult.ResultItem(false, sql, result.getErrorMsg()));
                }
                if (result.getData().getResultOverview().stream().allMatch(SQLBatchExecuteResult.ResultItem::isSuccess)) {
                    reload();
                } else {
                    ResultView.showErrMsgDialog(project, result);
                    stopLoadingAndRepaint();
                }

                super.done();
            }
        }.execute();
    }

    private void frozenDialog(String loadingText) {
        UIUtil.invokeLaterIfNeeded(() -> {
            this.setOKActionEnabled(false);
            progress.setVisible(true);
            dialogDecorator.setLoadingText(loadingText);
            dialogDecorator.startLoading(false);
            diffPanel.frozen();
        });
    }

    private void stopLoadingAndRepaint() {
        // 更新 diffPanel
        diffPanel.thaw();
        diffPanel.repaint();
        // 关闭进度条
        progress.setVisible(false);
        dialogDecorator.stopLoading();
        setOKActionEnabled(CollectionUtils.isNotEmpty(diffPanel.getSelectedDDLs()));
    }

    private void reloadData() {
        initData("重新解析MySQL声明式变更文件关联数据库信息中...");
    }

    private void initData(String loadingText) {
        frozenDialog(loadingText);

        initParams();
        new SwingWorker<Result<DiffResult>, Result<DiffResult>>() {
            @Override
            protected Result<DiffResult> doInBackground() {
                if (MapUtils.isEmpty(declare_actual_db)) {
                    declaresDBTables.keySet().forEach(database -> declare_actual_db.put(database, database));
                }
                return DeclarativeUtils.diff(declaresDBTables, declare_actual_db, tableIgnore);
            }

            @SneakyThrows
            @Override
            protected void done() {
                Result<DiffResult> result = get();
                tips.setVisible(true);

                if (result.isFail()) {
                    handlerErrorResult(result);
                    // stop loading
                    UIUtil.invokeLaterIfNeeded(dialogDecorator::stopLoading);
                    super.done();
                    return;
                }

                DiffResult diffResult = result.getData();
                // 左侧树
                declare_actual_db.clear();
                diffTree = loadDatabaseTree(diffResult.getDiffDBs(), diffPanel, relateActualDB);
                sameTree = loadDatabaseTree(diffResult.getSameDBs(), diffPanel, relateActualDB);
                ignoreTree = loadDatabaseTree(diffResult.getIgnoreDBs(), diffPanel, relateActualDB);

                treeTab.removeAll();
                treeTab.add(String.format("有差异(%s)", diffResult.diffDBSize()), new JBScrollPane(diffTree));
                treeTab.add(String.format("无差异(%s)", diffResult.sameDBSize()), new JBScrollPane(sameTree));
                treeTab.add(String.format("忽略(%s)", diffResult.ignoreDBSize()), new JBScrollPane(ignoreTree));
                // 添加差异
                JPanel databaseCombobox = new JPanel(new BorderLayout());
                databaseCombobox.add(dbLabel, BorderLayout.WEST);
                databaseCombobox.add(relateActualDB, BorderLayout.CENTER);

                databaseCombobox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 120));
                JPanel diffTitle = JPanelUtils.gridPanel(gridLayout2, databaseCombobox, new JLabel("     \uD83D\uDCCB声明式文件"));
                diffPanel.setNorthPanel(diffTitle);

                leftPanel.removeAll();
                leftPanel.add(treeTab, BorderLayout.CENTER);

                if (Objects.isNull(contentPanel)) {
                    contentPanel = new LeftRightComponent(leftPanel, diffPanel.getMainPanel(), 0.22F, JBUI.size(1200, 600));
                } else {
                    contentPanel.setLeftPanel(leftPanel);
                    contentPanel.setRightPanel(diffPanel.getMainPanel());
                }
                mainPanel.add(contentPanel.getMainPanel(), BorderLayout.CENTER);
                // stop loading
                UIUtil.invokeLaterIfNeeded(() -> {
                    // init select database
                    this.initSelectDatabase(result);
                    // init select
                    this.selectFirstTablePath(diffTree, true);
                    this.selectFirstTablePath(sameTree, false);
                    this.selectFirstTablePath(ignoreTree, false);
                    stopLoadingAndRepaint();
                });
                super.done();
            }

            private void initSelectDatabase(Result<DiffResult> result) {
                DiffResult data = result.getData();
                if (Objects.isNull(data)) return;

                DiffDatabase database = null;
                List<DiffDatabase> diffDBs = data.getDiffDBs();
                if (CollectionUtils.isNotEmpty(diffDBs)) database = diffDBs.get(0);

                List<DiffDatabase> sameDBs = data.getSameDBs();
                if (CollectionUtils.isNotEmpty(sameDBs) && database == null) database = sameDBs.get(0);

                List<DiffDatabase> ignoreDBs = data.getIgnoreDBs();
                if (CollectionUtils.isNotEmpty(ignoreDBs) && database == null) database = ignoreDBs.get(0);

                if (Objects.isNull(database)) return;
                DatabaseNode databaseNode = databaseMap.get(database.getActual_databaseName());
                if (Objects.isNull(databaseNode)) return;
                relateActualDB.setSelectedItem(databaseNode);
            }

            private void selectFirstTablePath(Tree diffTree, boolean selectFirst) {
                DefaultTreeModel model = (DefaultTreeModel) diffTree.getModel();
                DefaultMutableTreeNode root = ((DefaultMutableTreeNode) model.getRoot());
                this.expendTableNode(diffTree, root, selectFirst);
            }

            private void expendTableNode(Tree tree, DefaultMutableTreeNode node, boolean selectFirst) {
                TreePath treePath = new TreePath(node.getPath());
                if (node instanceof DiffTableNode && selectFirst) {
                    tree.setSelectionPath(treePath);
                } else if (node.getChildCount() > 0) {
                    tree.expandPath(treePath);
                    expendTableNode(tree, (DefaultMutableTreeNode) node.getChildAt(0), selectFirst);
                }
            }
        }.execute();
    }

    private void handlerErrorResult(Result<DiffResult> result) {
        mainPanel.removeAll();
        String errorMsg = result.getErrorMsg();
        JLabel errMsg = new JLabel("文件解析失败:");
        errMsg.setIcon(Icons.scaleToWidth(Icons.SYS_ERROR2, 18));
        errMsg.setHorizontalAlignment(JTextField.CENTER);
        errMsg.setForeground(JBColor.RED);
        errMsg.setBorder(BorderFactory.createEmptyBorder(150, 200, 0, 200));
        mainPanel.add(errMsg, BorderLayout.NORTH);

        JTextArea err = JPanelUtils.tips(JBColor.RED);
        errMsg.setHorizontalAlignment(JTextField.CENTER);
        err.setBorder(BorderFactory.createEmptyBorder(20, 200, 20, 200));
        err.setText(errorMsg);
        mainPanel.add(err, BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private void initParams() {
        this.declaresDBTables = getFiles(files);
//        if (MapUtils.isEmpty(declare_actual_db)) {
//            for (String declaresDB : this.declaresDBTables.keySet()) {
//                this.declare_actual_db.put(declaresDB, declaresDB);
//            }
//        }
        this.tableIgnore = getTableIgnore(files);
    }

    private void initEvents() {
        setOKActionEnabled(false);
        this.diffPanel.setSelectedDDLs(ddlList -> {
            setOKActionEnabled(CollectionUtils.isNotEmpty(ddlList));
        });

        this.relateActualDB.addActionListener(e -> {
            Object selectedItem = this.relateActualDB.getSelectedItem();
            if (Objects.nonNull(selectedItem)) {
                dbLabel.setForeground(null);
            }

            DatabaseNode currentDB = selectedItem == null ? null : (DatabaseNode) selectedItem;
            if (Objects.isNull(currentDB)) return;
            if (currentDB.equals(currentDBNode.getActualDatabase())) return;

            currentDBNode.setActualDatabase(currentDB);
            declare_actual_db.put(currentDBNode.getDatabaseName(), currentDB.getDatabaseWithInstance());
            // reload();
            reloadData();
        });
    }

    private void initComponents() {
        dbLabel = new JLabel("\uD83D\uDD17当前数据库");
        dbLabel.setForeground(JBColor.RED);

        leftPanel = new JPanel(new BorderLayout());
        treeTab = new JTabbedPane();
        relateActualDB = new ComboBox<>();
        relateActualDB.setRenderer(databaseRenderer());
        relateActualDB.setSwingPopup(false);
        databaseMap.keySet().stream().sorted().map(databaseMap::get).forEach(relateActualDB::addItem);

        mainPanel = new JPanel(new BorderLayout());
        dialogDecorator = new LoadingDecorator(mainPanel, this, 0);

        dialogPanel = new JPanel(new BorderLayout());
        dialogPanel.setPreferredSize(JBUI.size(1200, 680));
        dialogPanel.add(dialogDecorator.getComponent(), BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        southPanel.setBorder(BorderFactory.createEmptyBorder(-5, 0, -5, 0));
        southPanel.add(tips);
        southPanel.add(progress);
        tips.setVisible(false);
        progress.setVisible(false);
        dialogPanel.add(southPanel, BorderLayout.SOUTH);
    }

    private ListCellRenderer<? super DatabaseNode> databaseRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component renderer = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                // 自定义选项的外观和布局
                if (value instanceof DatabaseNode databaseNode) {
                    JPanel panel = new JPanel(new GridBagLayout());
                    if (renderer instanceof JLabel) panel.setBorder(((JLabel) renderer).getBorder());
                    panel.setPreferredSize(renderer.getPreferredSize());

                    JLabel commitTypeLabel = new JLabel(databaseNode.getName());
                    commitTypeLabel.setFont(new Font("微软雅黑", Font.BOLD, 12));
//                    RoundedLabel commitTypeLabel = new RoundedLabel(commitType.getType(), JBColor.background().darker(), RoundedLabel.WHITE, 10);

                    JLabel remark = new JLabel(databaseNode.getInstanceName());
                    remark.setHorizontalAlignment(SwingConstants.RIGHT);
                    remark.setForeground(JBColor.GRAY);

                    GridBagConstraints constraints = new GridBagConstraints();
                    constraints.fill = GridBagConstraints.BOTH;
                    constraints.weightx = 1.0;
                    constraints.gridx = 0;
                    constraints.gridwidth = 1;
                    panel.add(commitTypeLabel, constraints);

                    constraints.gridx = 1;
                    constraints.gridwidth = 4;
                    panel.add(remark, constraints);
                    // toolTip
                    commitTypeLabel.setToolTipText(String.format("%s    [%s]", databaseNode.getName(), databaseNode.getInstanceName()));
                    remark.setToolTipText(String.format("%s    [%s]", databaseNode.getName(), databaseNode.getInstanceName()));
                    panel.setToolTipText(String.format("%s    [%s]", databaseNode.getName(), databaseNode.getInstanceName()));

                    if (isSelected) panel.setBackground(RoundedLabel.SELECTED);
                    return panel;
                }

                return renderer;
            }
        };
    }

    private String getTableIgnore(Set<VirtualFile> files) {
        if (CollectionUtils.isEmpty(files)) return StringUtils.EMPTY;

        VirtualFile file = files.iterator().next();
        String path = file.getPath();
        String modulePath = StringUtils.substringBeforeLast(path, declarativePath);
        String tableIgnorePath = modulePath + declarativePath + "/" + TABLE_IGNORE;
        LocalFileSystem fileSystem = LocalFileSystem.getInstance();
        VirtualFile tableIgnoreFile = fileSystem.findFileByPath(tableIgnorePath);
        if (Objects.isNull(tableIgnoreFile)) {
            return StringUtils.EMPTY;
        }

        try {
            return FileUtils.readVirtualFile(tableIgnoreFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> getFiles(Set<VirtualFile> selectFiles) {
        return selectFiles.stream().collect(Collectors.toMap(f -> {
            String name = f.getName();
            return name.endsWith(SQL_SUFFIX) ? name.substring(0, name.length() - SQL_SUFFIX.length()) : name;
        }, f -> {
            try {
                return FileUtils.readVirtualFile(f);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, (a, b) -> a));
    }

    private Tree loadDatabaseTree(List<DiffDatabase> diffDBs, DeclareDiffPanel diffPanel, JComboBox<DatabaseNode> relateActualDB) {
        Tree tree = initTree();
        tree.removeAll();
        tree.setOpaque(false);
        // Create the JTree with the root node
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        // Hide the root node from being displayed
        tree.setRootVisible(false);
        tree.setToggleClickCount(0);
        tree.addMouseListener(new DiffTreeMouseListener(tree));
        tree.addTreeSelectionListener(new DiffTreeSelectionListener(diffPanel, relateActualDB) {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = currentNode(e);
                if (node instanceof DiffDatabaseNode databaseNode) {
                    currentDBNode = databaseNode;
                }
                if (node instanceof DiffTableNode tableNode) {
                    currentDBNode = tableNode.getDatabase();
                }
//                relateActualDB.removeAllItems();
//                relateActualDB.addItem(currentDBNode.getActual_databaseName());
//                relateActualDB.setSelectedItem(currentDBNode.getActual_databaseName());
//                databaseMap.forEach((databaseName, databaseNode) -> {
//                    if (databaseName.contains(currentDBNode.getDatabaseName())
//                            && !databaseName.equals(currentDBNode.getActual_databaseName())) {
//                        relateActualDB.addItem(databaseName);
//                    }
//                });
                super.valueChanged(e);
            }
        });
        tree.setCellRenderer(new SearchTreeCellRenderer(tree) {
            @Override
            public void setTreeCellRendererComponent(JTree tree, Object node, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                if (node instanceof DiffDatabaseNode) {
                    setIcon(Icons.scaleToWidth(AllIcons.Providers.Mysql, 18));
                }
                if (node instanceof DiffTableNode tableNode) {
                    Icon icon = Icons.scaleToWidth(Icons.TABLE2, 16);
                    switch (tableNode.getDiffType()) {
                        case CREATE -> {
                            icon = Icons.scaleToWidth(AllIcons.General.Add, 13);
                            if (!JBColor.isBright()) {
                                setForeground(Icons.GREEN);
                            }
                        }
                        case DROP -> {
                            icon = Icons.scaleToWidth(AllIcons.General.Remove, 13);
                            setForeground(JBColor.GRAY);
                        }
                        case ALTER -> {
                            icon = Icons.scaleToWidth(AllIcons.General.Modified, 13);
                            setForeground(JBColor.BLUE);
                        }
                    }
                    setIcon(icon);
                }
            }

//            @Override
//            protected void paintComponent(Graphics g) {
//                super.paintComponent(g);
//                if (currNode instanceof DiffTableNode tableNode) {
//                    DiffType diffType = tableNode.getDiffType();
//                    Icon icon;
//                    switch (diffType) {
//                        case CREATE -> icon = Icons.scaleToWidth(AllIcons.General.Add, 13);
//                        case DROP -> icon = Icons.scaleToWidth(AllIcons.General.Remove, 13);
//                        case ALTER -> icon = Icons.scaleToWidth(AllIcons.General.Modified, 13);
//                        default -> icon = null;
//                    }
//                    if (Objects.nonNull(icon)) {
//                        int x = getIcon() != null ? getIcon().getIconWidth() + getIconTextGap() : 0;
//                        int y = (getHeight() - icon.getIconHeight()) / 2;
//                        int width = this.getSize().width;
//                        icon.paintIcon(this, g, width - 2 * x, y);
//                    }
//                }
//            }
        });

        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        JTreeLoadingUtils.loading(false, tree, root, () -> {
            List<DiffDatabaseNode> diffDatabaseNodes = new ArrayList<>();
            for (DiffDatabase diffDB : diffDBs) {
                DiffDatabaseNode diffDatabaseNode = new DiffDatabaseNode();
                diffDatabaseNodes.add(diffDatabaseNode);
                diffDatabaseNode.setDatabaseName(diffDB.getDatabaseName());
                diffDatabaseNode.setActualDatabase(databaseMap.get(diffDB.getActual_databaseName()));
                declare_actual_db.put(diffDB.getDatabaseName(), diffDB.getActual_databaseName());
                if (currentDBNode == null) currentDBNode = diffDatabaseNode;
                for (DiffTable table : diffDB.getTables()) {
                    DiffTableNode tableNode = new DiffTableNode();
                    diffDatabaseNode.add(tableNode);
                    tableNode.setDatabase(diffDatabaseNode);
                    tableNode.setDatabaseName(diffDB.getDatabaseName());
                    tableNode.setTableName(table.getTableName());
                    tableNode.setDbDDL(table.getDbDDL());
                    tableNode.setDeclareDDL(table.getDeclareDDL());
                    tableNode.setDiffDDLs(table.getDiffDDLs());
                    tableNode.setDiffType(table.getDiffType());
                }
            }
            return diffDatabaseNodes;
        });

        return tree;
    }

    @Override
    protected @NotNull JComponent createCenterPanel() {
        return dialogPanel;
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
