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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.treeStructure.treetable.TreeTableTree;
import com.intellij.util.ui.tree.TreeUtil;
import com.mongodb.DBRef;
import com.qihoo.finance.lowcode.console.mongo.logic.Notifier;
import com.qihoo.finance.lowcode.console.mongo.view.JsonTableView;
import com.qihoo.finance.lowcode.console.mongo.view.JsonTreeTableView;
import com.qihoo.finance.lowcode.console.mongo.view.edition.MongoEditionDialog;
import com.qihoo.finance.lowcode.console.mongo.view.model.*;
import com.qihoo.finance.lowcode.console.mongo.view.nodedescriptor.MongoKeyValueDescriptor;
import com.qihoo.finance.lowcode.console.mongo.view.nodedescriptor.MongoNodeDescriptor;
import com.qihoo.finance.lowcode.console.mongo.view.nodedescriptor.MongoResultDescriptor;
import com.qihoo.finance.lowcode.console.mongo.view.nodedescriptor.MongoValueDescriptor;
import lombok.Getter;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.Document;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MongoResultPanel extends JPanel implements Disposable {
    private final Project project;
    private final MongoPanel.MongoDocumentOperations mongoDocumentOperations;
    private final Notifier notifier;
    private JPanel mainPanel;
    private JPanel containerPanel;
    private final JPanel resultTreePanel;

    @Getter
    private JsonTreeTableView resultTreeTableView;
    @Getter
    private JsonTableView resuleTableView;

    private ViewMode currentViewMode = ViewMode.TABLE;
    private MongoPanel.ActionCallback actionCallback;

    private void initForm() {
        mainPanel = new JPanel(new BorderLayout());
        containerPanel = new JPanel();
        mainPanel.add(containerPanel, BorderLayout.CENTER);
    }

    public MongoResultPanel(Project project, MongoPanel.MongoDocumentOperations mongoDocumentOperations, MongoPanel.ActionCallback callback) {
        initForm();
        this.project = project;
        this.mongoDocumentOperations = mongoDocumentOperations;
        this.notifier = Notifier.getInstance(project);
        this.actionCallback = callback;
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        resultTreePanel = new JPanel(new BorderLayout());

//        containerPanel.setLayout(new JBCardLayout());
        containerPanel.setLayout(new BorderLayout());
        containerPanel.add(resultTreePanel, BorderLayout.CENTER);
        Disposer.register(project, this);
    }

    void updateResultView(MongoCollectionResult mongoCollectionResult, Pagination pagination) {
//        if (ViewMode.TREE.equals(currentViewMode)) {
//            updateResultTreeTable(mongoCollectionResult, pagination);
//        } else {
//            updateResultTable(mongoCollectionResult);
//        }
        JsonTableView jsonTableView = updateResultTable(mongoCollectionResult);
        JsonTreeTableView jsonTreeTableView = updateResultTreeTable(mongoCollectionResult, pagination);
        tabDisplayResult(jsonTableView, jsonTreeTableView);
    }

    private JsonTreeTableView updateResultTreeTable(MongoCollectionResult mongoCollectionResult, Pagination pagination) {
        TreeNode treeNode = JsonTreeUtils.buildJsonTree(
                mongoCollectionResult.getCollectionName(),
                extractDocuments(pagination, mongoCollectionResult.getDocuments()),
                pagination.getStartIndex()
        );
        resultTreeTableView = new JsonTreeTableView(treeNode, JsonTreeTableView.COLUMNS_FOR_READING);
        resultTreeTableView.setName("resultTreeTable");
        displayResult(resultTreeTableView);

        return resultTreeTableView;
    }

    private static List<Document> extractDocuments(Pagination pagination, List<Document> documents) {
        return ListUtils.defaultIfNull(documents, new ArrayList<>());
    }

    private JsonTableView updateResultTable(MongoCollectionResult mongoCollectionResult) {
        resuleTableView = new JsonTableView(JsonTableUtils.buildJsonTable(mongoCollectionResult));
        displayResult(resuleTableView);
        return resuleTableView;
    }

    private void tabDisplayResult(JComponent tableView, JComponent jsonView) {
        ViewMode currentView = getCurrentViewMode();

        resultTreePanel.invalidate();
        resultTreePanel.removeAll();
        JTabbedPane viewPanel = new JBTabbedPane();
        viewPanel.addTab("查询结果 Table", AllIcons.Nodes.DataTables, new JBScrollPane(tableView));
        viewPanel.addTab("查询结果 Document", AllIcons.Actions.ShowAsTree, new JBScrollPane(jsonView));
        viewPanel.addChangeListener(changeEvent -> {
            int selectedIndex = viewPanel.getSelectedIndex();
            if (selectedIndex == 0) {
                setCurrentViewMode(ViewMode.TABLE);
            }
            if (selectedIndex == 1) {
                setCurrentViewMode(ViewMode.TREE);
            }
        });
        switch (currentView) {
            case TABLE -> viewPanel.setSelectedIndex(0);
            case TREE -> viewPanel.setSelectedIndex(1);
            default -> viewPanel.setSelectedIndex(0);
        }
        resultTreePanel.add(viewPanel, BorderLayout.CENTER);
        resultTreePanel.validate();
        resultTreePanel.repaint();
    }

    private void displayResult(JComponent tableView) {
        resultTreePanel.invalidate();
        resultTreePanel.removeAll();
        JBScrollPane viewPanel = new JBScrollPane(tableView);
        resultTreePanel.add(viewPanel, BorderLayout.CENTER);
        resultTreePanel.validate();
        resultTreePanel.repaint();
    }

    public void editSelectedMongoDocument(MongoEditionDialog.Operate operate) {
        Document mongoDocument = getSelectedMongoDocument();
        if (mongoDocument == null) {
            return;
        }

        if (operate == MongoEditionDialog.Operate.CLONE) {
            // clone
            Document cloneDocument = Document.parse(mongoDocument.toJson());
            cloneDocument.remove("_id");
            MongoEditionDialog.create(project, mongoDocumentOperations, actionCallback).initDocument(cloneDocument, "新增Document").show();
            return;
        }

        MongoEditionDialog.create(project, mongoDocumentOperations, actionCallback).initDocument(mongoDocument, "编辑Document").show();
    }


    public void addMongoDocument() {
        MongoEditionDialog.create(project, mongoDocumentOperations, actionCallback).initDocument(null, "新增Document").show();
    }

    private Document getSelectedMongoDocument() {
        switch (currentViewMode) {
            case TREE:
                return getSelectedTreeMongoDocument();
            case TABLE:
                return (Document) resuleTableView.getSelectedObject();
        }
        return null;
    }

    private Document getSelectedTreeMongoDocument() {
        TreeTableTree tree = resultTreeTableView.getTree();
        JsonTreeNode treeNode = (JsonTreeNode) tree.getLastSelectedPathComponent();
        if (treeNode == null) {
            return null;
        }

        MongoNodeDescriptor descriptor = treeNode.getDescriptor();
        if (descriptor instanceof MongoKeyValueDescriptor) {
            MongoKeyValueDescriptor keyValueDescriptor = (MongoKeyValueDescriptor) descriptor;
            if (StringUtils.equals(keyValueDescriptor.getKey(), "_id")) {
                return mongoDocumentOperations.getMongoDocument(keyValueDescriptor.getValue());
            }
        } else if (descriptor instanceof MongoValueDescriptor.MongoDocumentValueDescriptor) {
            MongoValueDescriptor.MongoDocumentValueDescriptor documentValueDescriptor = (MongoValueDescriptor.MongoDocumentValueDescriptor) descriptor;
            Object value = documentValueDescriptor.getValue();
            if (value instanceof Document) {
                Document document = (Document) value;
                if (document.containsKey("_id")) {
                    return mongoDocumentOperations.getMongoDocument(document.get("_id"));
                }
            }
        } else {
            // 向上递归获取原始Json对象节点
            TreeNode parent = treeNode.getParent();
            if (parent instanceof JsonTreeNode) {
                JsonTreeNode jsonTreeNode = (JsonTreeNode) parent;
                Object value = jsonTreeNode.getDescriptor().getValue();
                if (value instanceof Document) {
                    Document document = (Document) value;
                    if (document.containsKey("_id")) {
                        return mongoDocumentOperations.getMongoDocument(document.get("_id"));
                    }
                }
            }
        }

        return null;
    }

    private Object getObjectIdDescriptorFromSelectedDocument() {
        switch (currentViewMode) {
            case TREE:
                return getObjectIdFromSelectedTree();
            case TABLE:
                return getObjectIdFromSelectedTable();
        }

        return null;
    }

    private Object getObjectIdFromSelectedTable() {
        if (Objects.isNull(resuleTableView)) return null;

        Object selectedObject = resuleTableView.getSelectedObject();
        if (Objects.nonNull(selectedObject) && selectedObject instanceof Document) {
            return ((Document) selectedObject).get("_id");
        }
        return null;
    }

    @Nullable
    private Object getObjectIdFromSelectedTree() {
        if (resultTreeTableView == null) {
            return null;
        }
        TreeTableTree tree = resultTreeTableView.getTree();
        JsonTreeNode treeNode = (JsonTreeNode) tree.getLastSelectedPathComponent();
        if (treeNode == null) {
            return null;
        }

        MongoNodeDescriptor descriptor = treeNode.getDescriptor();
        if (descriptor instanceof MongoValueDescriptor.MongoDocumentValueDescriptor) {
            JsonTreeNode parent = (JsonTreeNode) treeNode.getParent();
            if (parent.getDescriptor() instanceof MongoResultDescriptor) {
                Object value = descriptor.getValue();
                if (value instanceof Document) {
                    Document document = (Document) value;
                    return document.get("_id");
                }
            }
        }

        if (descriptor instanceof MongoKeyValueDescriptor) {
            MongoKeyValueDescriptor keyValueDescriptor = (MongoKeyValueDescriptor) descriptor;
            if ("_id".equals(keyValueDescriptor.getKey())
                /*&& !(keyValueDescriptor.getValue() instanceof ObjectId)*/) {
                return keyValueDescriptor.getValue();
            }
        }

        return null;
    }


    public boolean isSelectedNodeId() {
        return getObjectIdDescriptorFromSelectedDocument() != null;
    }


    public boolean isSelectedDBRef() {
        if (resultTreeTableView == null) {
            return false;
        }

        TreeTableTree tree = resultTreeTableView.getTree();
        JsonTreeNode treeNode = (JsonTreeNode) tree.getLastSelectedPathComponent();
        if (treeNode == null) {
            return false;
        }

        MongoNodeDescriptor descriptor = treeNode.getDescriptor();
        if (descriptor instanceof MongoKeyValueDescriptor) {
            if (descriptor.getValue() instanceof DBRef) {
                return true;
            } else {
                JsonTreeNode parentNode = (JsonTreeNode) treeNode.getParent();
                return parentNode.getDescriptor().getValue() instanceof DBRef;
            }
        }

        return false;
    }

    void expandAll() {
        TreeUtil.expandAll(resultTreeTableView.getTree());
    }

    void collapseAll() {
        TreeTableTree tree = resultTreeTableView.getTree();
        TreeUtil.collapseAll(tree, 1);
    }

    public String getStringifyResult() {
        switch (this.currentViewMode) {
            case TREE:
                JsonTreeNode rootNode = (JsonTreeNode) resultTreeTableView.getTree().getModel().getRoot();
                return stringifyResult(rootNode);
            case TABLE:
                return getJsonTableSelectedValues();
        }

        return null;
    }

    public JsonTreeNode getSelectedNode() {
        return (JsonTreeNode) resultTreeTableView.getTree().getLastSelectedPathComponent();
    }

    public String getSelectedNodeStringifyValue() {
        switch (this.currentViewMode) {
            case TREE:
                return getJsonTreeSelectedValue();
            case TABLE:
                return getJsonTableSelectedValue();
        }
        return null;
    }

    private String getJsonTableSelectedValues() {
        List<?> selectedObjects = resuleTableView.getSelectedObjects();
        return "[" + selectedObjects.stream().map(obj -> ((Document) obj).toJson()).collect(Collectors.joining(",\n")) + "]";
    }

    @Nullable
    private String getJsonTableSelectedValue() {
        int selectedRow = resuleTableView.getSelectedRow();
        if (selectedRow < 0) return null;

        Object valueAt = resuleTableView.getValueAt(selectedRow, Math.max(resuleTableView.getSelectedColumn(), 0));
        return Objects.nonNull(valueAt) ? valueAt.toString() : null;
    }

    @Nullable
    private String getJsonTreeSelectedValue() {
        // key : column==0, value : column==1
        JsonTreeNode lastSelectedResultNode = getSelectedNode();
        if (lastSelectedResultNode == null) {
            return null;
        }
        int column = resultTreeTableView.getTree().getTreeTable().getSelectedColumn();
        // copy key
        if (0 == column) {
            return lastSelectedResultNode.getDescriptor().getKey();
        }
        // copy value
        MongoNodeDescriptor userObject = lastSelectedResultNode.getDescriptor();
        return userObject.pretty();
    }

    public DBRef getSelectedDBRef() {
        TreeTableTree tree = resultTreeTableView.getTree();
        JsonTreeNode treeNode = (JsonTreeNode) tree.getLastSelectedPathComponent();

        MongoNodeDescriptor descriptor = treeNode.getDescriptor();
        DBRef selectedDBRef = null;
        if (descriptor instanceof MongoKeyValueDescriptor) {
            if (descriptor.getValue() instanceof DBRef) {
                selectedDBRef = (DBRef) descriptor.getValue();
            } else {
                JsonTreeNode parentNode = (JsonTreeNode) treeNode.getParent();
                MongoNodeDescriptor parentDescriptor = parentNode.getDescriptor();
                if (parentDescriptor.getValue() instanceof DBRef) {
                    selectedDBRef = (DBRef) parentDescriptor.getValue();
                }
            }
        }

        return selectedDBRef;
    }


    private String stringifyResult(DefaultMutableTreeNode selectedResultNode) {
        return IntStream.range(0, selectedResultNode.getChildCount()).mapToObj(i -> getDescriptor(i, selectedResultNode).pretty()).collect(Collectors.joining(",", "[", "]"));
    }

    private static MongoNodeDescriptor getDescriptor(int i, DefaultMutableTreeNode parentNode) {
        DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) parentNode.getChildAt(i);
        return (MongoNodeDescriptor) childNode.getUserObject();
    }

    @Override
    public void dispose() {
        resultTreeTableView = null;
    }

    void setCurrentViewMode(ViewMode viewMode) {
        this.currentViewMode = viewMode;
    }

    ViewMode getCurrentViewMode() {
        return currentViewMode;
    }

    public Document getReferencedDocument(DBRef selectedDBRef) {
        return mongoDocumentOperations.getReferenceDocument(selectedDBRef.getCollectionName(), selectedDBRef.getDatabaseName(), selectedDBRef.getId());
    }

    public boolean deleteSelectedMongoDocument() {
        Object _id = getObjectIdDescriptorFromSelectedDocument();
        if (_id == null) {
            return false;
        }

        if (mongoDocumentOperations.deleteMongoDocument(_id)) {
            notifier.notifyInfo("Document with _id=" + _id.toString() + " deleted.");
            return true;
        }

        return false;
    }

    public enum ViewMode {
        TREE, TABLE
    }

}
