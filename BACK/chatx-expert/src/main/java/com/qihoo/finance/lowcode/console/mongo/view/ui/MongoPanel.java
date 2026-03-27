/*
 * Copyright (c) 2018 David Boissier.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qihoo.finance.lowcode.console.mongo.view.ui;

import com.intellij.ide.CommonActionsManager;
import com.intellij.ide.TreeExpander;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.actionSystem.toolbarLayout.ToolbarLayoutStrategy;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LoadingDecorator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.JBColor;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.NumberDocument;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.util.ui.UIUtil;
import com.mongodb.DBRef;
import com.qihoo.finance.lowcode.common.ui.base.VFlowLayout;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.console.mongo.logic.Notifier;
import com.qihoo.finance.lowcode.console.mongo.model.MongoQueryOptions;
import com.qihoo.finance.lowcode.console.mongo.utils.MongoTemplateUtils;
import com.qihoo.finance.lowcode.console.mongo.view.JsonTableView;
import com.qihoo.finance.lowcode.console.mongo.view.JsonTreeTableView;
import com.qihoo.finance.lowcode.console.mongo.view.PaginationPopupComponent;
import com.qihoo.finance.lowcode.console.mongo.view.action.pagination.PaginationAction;
import com.qihoo.finance.lowcode.console.mongo.view.action.result.*;
import com.qihoo.finance.lowcode.console.mongo.view.editor.MongoVirtualFile;
import com.qihoo.finance.lowcode.console.mongo.view.model.JsonTableUtils;
import com.qihoo.finance.lowcode.console.mongo.view.model.MongoCollectionResult;
import com.qihoo.finance.lowcode.console.mongo.view.model.Pagination;
import com.qihoo.finance.lowcode.console.mongo.view.model.navigation.Navigation;
import com.qihoo.finance.lowcode.console.mysql.result.ResultManager;
import com.qihoo.finance.lowcode.design.entity.MongoCollectionNode;
import com.qihoo.finance.lowcode.design.entity.MongoDatabaseNode;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Objects;

public class MongoPanel extends JPanel implements Disposable {

    private final Project project;
    private final LoadingDecorator loadingDecorator;
    private JPanel rootPanel;
    private JBSplitter splitter;
    private JPanel toolBar;
    private JPanel errorPanel;
    private JPanel paginationPanel;

    private final JTextField rowLimitField = new JTextField();
    private final JBLabel rowCountLabel = new JBLabel();
    private final JBLabel pageNumberLabel = new JBLabel();

    @Getter
    private final MongoResultPanel resultPanel;
    private final QueryPanel queryPanel;
    private MongoCollectionResult currentResults;

    private final Pagination pagination;
    private final ActionCallback actionCallback;
    private final Navigation navigation;
    private final MongoVirtualFile mongoVirtualFile;

    private void initForm() {
        rootPanel = new JPanel();
        rootPanel.setBorder(BorderFactory.createLineBorder(JBColor.border()));
        rootPanel.setLayout(new BorderLayout());

        splitter = new JBSplitter();
        toolBar = new JPanel();
        errorPanel = new JPanel();
        paginationPanel = new JPanel();

        rootPanel.add(toolBar, BorderLayout.NORTH);
        rootPanel.add(splitter, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new VFlowLayout(VFlowLayout.TOP));
        bottomPanel.add(paginationPanel);
        bottomPanel.add(errorPanel);
        rootPanel.add(bottomPanel, BorderLayout.SOUTH);
    }

    public MongoPanel(Project project, MongoVirtualFile mongoVirtualFile) {
        initForm();

        this.project = project;
        this.pagination = new Pagination();
        this.mongoVirtualFile = mongoVirtualFile;
        this.navigation = mongoVirtualFile.getNavigation();
        this.currentResults = new MongoCollectionResult();

        Notifier notifier = Notifier.getInstance(project);
        actionCallback = new ActionCallback() {
            public void onOperationSuccess(String shortMessage, String detailedMessage) {
                notifier.notifyInfo(detailedMessage);
                executeQuery(false, navigation.getCurrentWayPoint());
            }

            @Override
            public void onOperationFailure(Exception exception) {
                notifier.notifyError(exception.getMessage());
            }
        };

        errorPanel.setLayout(new BorderLayout());
        queryPanel = new QueryPanel(project, mongoVirtualFile);
        queryPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, JBColor.border()));
        JBScrollPane scrollPane = new JBScrollPane(queryPanel);
        scrollPane.setMinimumSize(queryPanel.getPreferredSize());
        splitter.setFirstComponent(scrollPane);

        resultPanel = createResultPanel(project, mongoVirtualFile);
        loadingDecorator = new LoadingDecorator(resultPanel, this, 0);
        splitter.setProportion(0.1f);
        splitter.setOrientation(true);
        splitter.setSecondComponent(loadingDecorator.getComponent());

        setLayout(new BorderLayout());
        add(rootPanel);

        initToolBar();
        initPaginationPanel();

        pagination.addSetPageListener(() -> {
            MongoQueryOptions queryOptions = navigation.getCurrentWayPoint().getQueryOptions();
            queryOptions.setPage(pagination.getPageNumber());
            queryOptions.setPageSize(pagination.getNbDocumentsPerPage());
            showResults(false);
        });
        pagination.addSetPageListener(() -> {
            pagination.setTotalDocuments(currentResults.getCountDocuments());
//                    if (NbPerPage.ALL.equals(pagination.getNbPerPage())) {
//                        pageNumberLabel.setVisible(false);
//                    } else {
            pageNumberLabel.setText(String.format("Page %d/%d", pagination.getPageNumber(), pagination.getTotalPageNumber()));
            pageNumberLabel.setVisible(true);
//                    }
        });
    }

    private MongoResultPanel createResultPanel(Project project, MongoVirtualFile objectFile) {
        return new MongoResultPanel(project, new MongoDocumentOperations() {
            @Override
            public Document getMongoDocument(Object _id) {
                Navigation navigation = objectFile.getNavigation();
                MongoCollectionNode collection = navigation.getCurrentWayPoint().getCollection();
                return MongoTemplateUtils.findDocument((MongoDatabaseNode) collection.getParent(), collection.getTableName(), _id);
            }

            @Override
            public boolean updateMongoDocument(Document mongoDocument) {
                Navigation navigation = objectFile.getNavigation();
                MongoCollectionNode collection = navigation.getCurrentWayPoint().getCollection();
                return MongoTemplateUtils.updateDocument((MongoDatabaseNode) collection.getParent(), collection.getTableName(), mongoDocument);
            }

            @Override
            public Document getReferenceDocument(String collection, String database, Object _id) {
                return null;
            }

            @Override
            public boolean deleteMongoDocument(Object objectId) {
                Navigation navigation = objectFile.getNavigation();
                MongoCollectionNode collection = navigation.getCurrentWayPoint().getCollection();
                return MongoTemplateUtils.deleteDocument((MongoDatabaseNode) collection.getParent(), collection.getTableName(), objectId);
            }
        }, actionCallback);
    }

    private void initToolBar() {
        toolBar.setLayout(new BorderLayout());

//        JPanel rowLimitPanel = createRowLimitPanel();
//        toolBar.add(rowLimitPanel, BorderLayout.WEST);

        JComponent actionToolBarComponent = createResultActionsComponent();
        toolBar.add(actionToolBarComponent, BorderLayout.CENTER);

//        JComponent viewToolbarComponent = createSelectViewActionsComponent();
//        toolBar.add(viewToolbarComponent, BorderLayout.EAST);
    }

    private void initPaginationPanel() {
        paginationPanel.setLayout(new BorderLayout());

        JComponent actionToolbarComponent = createPaginationActionsComponent();
        JPanel btnPanel = new JPanel();
        btnPanel.setBorder(BorderFactory.createEmptyBorder(-5, 0, -5, 0));
        btnPanel.add(actionToolbarComponent);
        paginationPanel.add(btnPanel, BorderLayout.CENTER);

        JPanel panel = new JPanel();
        panel.add(pageNumberLabel);
        panel.add(com.intellij.ui.GuiUtils.createVerticalStrut());
        panel.add(rowCountLabel);

        paginationPanel.setBorder(BorderFactory.createEmptyBorder(-3, 0, -3, 0));
        paginationPanel.add(panel, BorderLayout.EAST);
    }

    @NotNull
    private JPanel createRowLimitPanel() {
        rowLimitField.setText(Integer.toString(/*configuration.getDefaultRowLimit()*/100));
        rowLimitField.setColumns(5);
        rowLimitField.setDocument(new NumberDocument());
        rowLimitField.setText(Integer.toString(/*configuration.getDefaultRowLimit()*/100));

        JPanel rowLimitPanel = new NonOpaquePanel();
        rowLimitPanel.add(new JLabel("Row limit:"), BorderLayout.WEST);
        rowLimitPanel.add(rowLimitField, BorderLayout.CENTER);
        rowLimitPanel.add(Box.createHorizontalStrut(5), BorderLayout.EAST);
        return rowLimitPanel;
    }

    @NotNull
    private JComponent createResultActionsComponent() {
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        DefaultActionGroup actionResultGroup = new DefaultActionGroup("MongoResultGroup", true);
        actionResultGroup.add(new ExecuteQuery(this));
//        actionResultGroup.add(new OpenFindAction(this));
//        actionResultGroup.add(new EnableAggregateAction(queryPanel));
        actionResultGroup.addSeparator();
        actionResultGroup.add(new AddMongoDocumentAction(resultPanel));
        actionResultGroup.add(new EditMongoDocumentAction(resultPanel));
        actionResultGroup.add(new DeleteMongoDocumentAction(resultPanel, actionCallback));
        actionResultGroup.add(new CopyAllAction(resultPanel));
        actionResultGroup.addSeparator();
//        actionResultGroup.add(new NavigateBackwardAction(this));
        // 展开收缩
        addBasicTreeActions(actionResultGroup);
//        actionResultGroup.add(new CloseFindEditorAction(this));

        ActionToolbar actionToolBar = ActionManager.getInstance().createActionToolbar("MongoResultGroupActions", actionResultGroup, true);
        actionToolBar.setTargetComponent(actionToolBar.getComponent());

        // actionToolBar.setLayoutPolicy(ActionToolbar.AUTO_LAYOUT_POLICY);
        actionToolBar.setLayoutStrategy(ToolbarLayoutStrategy.AUTOLAYOUT_STRATEGY);
        JComponent actionToolBarComponent = actionToolBar.getComponent();
        actionToolBarComponent.setBorder(null);
        actionToolBarComponent.setOpaque(false);

        MongoCollectionNode collection = mongoVirtualFile.getMongoCollection();
        JLabel sqlLabel = new JLabel(String.format("%s  [%s]", GlobalDict.PLUGIN_NAME, collection.getTableName()));
        sqlLabel.setIcon(Icons.scaleToWidth(Icons.LOGO_ROUND, 16));
        actionsPanel.add(sqlLabel);
        actionsPanel.add(actionToolBarComponent);

        return actionsPanel;
    }

    @NotNull
    private JComponent createPaginationActionsComponent() {
        DefaultActionGroup actionResultGroup = new DefaultActionGroup("MongoPaginationGroup", false);
        actionResultGroup.add(new ChangeNbPerPageActionComponent(() -> new PaginationPopupComponent(pagination).initUi()));
        actionResultGroup.add(new PaginationAction.Previous(pagination));
        actionResultGroup.add(new PaginationAction.Next(pagination));

        ActionToolbar actionToolBar = ActionManager.getInstance().createActionToolbar("MongoPaginationGroupActions", actionResultGroup, true);
        actionToolBar.setTargetComponent(actionToolBar.getComponent());

        // actionToolBar.setLayoutPolicy(ActionToolbar.AUTO_LAYOUT_POLICY);
        actionToolBar.setLayoutStrategy(ToolbarLayoutStrategy.AUTOLAYOUT_STRATEGY);
        JComponent actionToolBarComponent = actionToolBar.getComponent();
        actionToolBarComponent.setBorder(null);
        actionToolBarComponent.setOpaque(false);
        return actionToolBarComponent;
    }

    @NotNull
    private JComponent createSelectViewActionsComponent() {
        DefaultActionGroup viewSelectGroup = new DefaultActionGroup("MongoViewSelectGroup", false);
//        viewSelectGroup.add(new ViewAsTreeAction(this));
//        viewSelectGroup.add(new ViewAsTableAction(this));

        ActionToolbar viewToolbar = ActionManager.getInstance().createActionToolbar("MongoViewSelectedActions", viewSelectGroup, true);
        viewToolbar.setTargetComponent(viewToolbar.getComponent());

        // viewToolbar.setLayoutPolicy(ActionToolbar.AUTO_LAYOUT_POLICY);

        viewToolbar.setLayoutStrategy(ToolbarLayoutStrategy.AUTOLAYOUT_STRATEGY);
        JComponent viewToolbarComponent = viewToolbar.getComponent();
        viewToolbarComponent.setBorder(null);
        viewToolbarComponent.setOpaque(false);
        return viewToolbarComponent;
    }

    private void addBasicTreeActions(DefaultActionGroup actionResultGroup) {
        final TreeExpander treeExpander = new TreeExpander() {
            @Override
            public void expandAll() {
                resultPanel.expandAll();
            }

            @Override
            public boolean canExpand() {
                return true;
            }

            @Override
            public void collapseAll() {
                resultPanel.collapseAll();
            }

            @Override
            public boolean canCollapse() {
                return true;
            }
        };

        CommonActionsManager actionsManager = CommonActionsManager.getInstance();
        final AnAction expandAllAction = actionsManager.createExpandAllAction(treeExpander, resultPanel);
        final AnAction collapseAllAction = actionsManager.createCollapseAllAction(treeExpander, resultPanel);

        Disposer.register(this, () -> {
            collapseAllAction.unregisterCustomShortcutSet(resultPanel);
            expandAllAction.unregisterCustomShortcutSet(resultPanel);
        });

        actionResultGroup.addSeparator();
//        actionResultGroup.add(new ViewAsTreeAction(this));
//        actionResultGroup.add(new ViewAsTableAction(this));
        actionResultGroup.addSeparator();
        actionResultGroup.add(expandAllAction);
        actionResultGroup.add(collapseAllAction);
    }

    public Navigation.WayPoint getCurrentWayPoint() {
        return navigation.getCurrentWayPoint();
    }

    public void showResults() {
        showResults(false);
    }

    private void showResults(boolean cached) {
        executeQuery(cached, navigation.getCurrentWayPoint());
    }

    public void executeQuery() {
        MongoQueryOptions queryOptions = queryPanel.getQueryOptions(rowLimitField.getText());
        Navigation.WayPoint currentWayPoint = navigation.getCurrentWayPoint();
        currentWayPoint.setQueryOptions(queryOptions);
        pagination.setPageNumber(1);
        executeQuery(false, currentWayPoint);
    }

    private void executeQuery(final boolean useCachedResults, final Navigation.WayPoint wayPoint) {
        errorPanel.setVisible(false);
        validateQuery();
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Get documents from " + wayPoint.getLabel(), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    UIUtil.invokeLaterIfNeeded(() -> loadingDecorator.startLoading(false));

                    MongoQueryOptions queryOptions = wayPoint.getQueryOptions();
                    if (!useCachedResults) {
                        MongoCollectionNode collection = mongoVirtualFile.getMongoCollection();
                        currentResults = MongoTemplateUtils.findDocuments((MongoDatabaseNode) collection.getParent(), collection.getTableName(), queryOptions);
                        if (CollectionUtils.isNotEmpty(currentResults.getDocuments())) {
                            List<String> fields = JsonTableUtils.extractAllColumnNames(currentResults.getDocuments().get(0));
                            mongoVirtualFile.putUserData(MongoVirtualFile.COLLECTION_FIELD_LIST, fields);
                        }
                    }

                    UIUtil.invokeLaterIfNeeded(() -> {
                        pagination.setTotalDocuments(currentResults.getCountDocuments());
                        resultPanel.updateResultView(currentResults, pagination);
                        pageNumberLabel.setText(String.format("Page %d/%d", pagination.getPageNumber(), pagination.getTotalPageNumber()));
                        rowCountLabel.setText(String.format("%s documents", currentResults.getCountDocuments()));
                        initActions(resultPanel);
                    });
                } catch (final Exception ex) {
                    UIUtil.invokeLaterIfNeeded(() -> {
                        errorPanel.invalidate();
                        errorPanel.removeAll();
//                        errorPanel.add(new ErrorPanel(ex), BorderLayout.CENTER);
                        errorPanel.validate();
                        errorPanel.setVisible(true);
                    });
                } finally {
                    UIUtil.invokeLaterIfNeeded(loadingDecorator::stopLoading);
                }

            }
        });

        // 尝试关闭非MongoDB的toolwindows
        ResultManager.getInstance().hideExecutionConsole();
    }

    private void initActions(MongoResultPanel resultPanel) {
        DefaultActionGroup actionPopupGroup = new DefaultActionGroup("MongoResultPopupGroup", true);
        if (ApplicationManager.getApplication() != null) {
            actionPopupGroup.add(new AddMongoDocumentAction(resultPanel));
            actionPopupGroup.add(new CloneMongoDocumentAction(resultPanel));
            actionPopupGroup.addSeparator();
            actionPopupGroup.add(new EditMongoDocumentAction(resultPanel));
            actionPopupGroup.add(new DeleteMongoDocumentAction(resultPanel, actionCallback));
            actionPopupGroup.addSeparator();
            actionPopupGroup.add(new CopyNodeAction(resultPanel));
//            actionPopupGroup.add(new GoToMongoDocumentAction(this));
        }

        JsonTreeTableView resultTreeTableView = resultPanel.getResultTreeTableView();
        if (Objects.nonNull(resultTreeTableView)) {
            PopupHandler.installPopupMenu(resultTreeTableView, actionPopupGroup, "POPUP");
        }

        JsonTableView tableView = resultPanel.getResuleTableView();
        if (Objects.nonNull(tableView)) {
            PopupHandler.installPopupMenu(tableView, actionPopupGroup, "POPUP");
        }
    }

    private void validateQuery() {
        queryPanel.validateQuery();
    }

    @Override
    public void dispose() {
        resultPanel.dispose();
    }

    public void openFindEditor() {
        splitter.setFirstComponent(queryPanel);
        UIUtil.invokeLaterIfNeeded(this::focusOnEditor);
    }

    public void closeFindEditor() {
        splitter.setFirstComponent(null);
    }

    public void focusOnEditor() {
        queryPanel.requestFocusOnEditor();
    }

    public boolean isFindEditorOpened() {
        return splitter.getFirstComponent() == queryPanel;
    }

    public void setViewMode(MongoResultPanel.ViewMode viewMode) {
        if (resultPanel.getCurrentViewMode().equals(viewMode)) {
            return;
        }
        this.resultPanel.setCurrentViewMode(viewMode);
        executeQuery(true, navigation.getCurrentWayPoint());
    }

    public void navigateBackward() {
//        navigation.moveBackward();
//        executeQuery(false, navigation.getCurrentWayPoint());
    }

    public boolean hasNavigationHistory() {
//        return navigation.getWayPoints().size() > 1;
        return true;
    }

    public void goToReferencedDocument() {
        DBRef selectedDBRef = resultPanel.getSelectedDBRef();
        Document referencedDocument = resultPanel.getReferencedDocument(selectedDBRef);
        if (referencedDocument == null) {
            Messages.showErrorDialog(this, "Referenced document was not found");
            return;
        }

        executeQuery(false, navigation.getCurrentWayPoint());
    }

    private static class ChangeNbPerPageActionComponent extends DumbAwareAction implements CustomComponentAction {

        @NotNull
        private final Computable<JComponent> myComponentCreator;

        ChangeNbPerPageActionComponent(@NotNull Computable<JComponent> componentCreator) {
            myComponentCreator = componentCreator;
        }

        @Override
        public @NotNull JComponent createCustomComponent(@NotNull Presentation presentation, @NotNull String place) {
            return myComponentCreator.compute();
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
        }
    }


    public interface MongoDocumentOperations {
        Document getMongoDocument(Object _id);

        boolean deleteMongoDocument(Object mongoDocument);

        boolean updateMongoDocument(Document mongoDocument);

        Document getReferenceDocument(String collection, String database, Object _id);
    }

    public interface ActionCallback {

        void onOperationSuccess(String label, String message);

        void onOperationFailure(Exception exception);
    }
}
