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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.util.Alarm;
import com.intellij.util.textCompletion.TextFieldWithCompletion;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UIUtil;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONParseException;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.console.mongo.model.MongoQueryOptions;
import com.qihoo.finance.lowcode.console.mongo.view.action.result.OperatorCompletionAction;
import com.qihoo.finance.lowcode.console.mongo.view.editor.MongoCompletionProvider;
import com.qihoo.finance.lowcode.console.mongo.view.editor.MongoVirtualFile;
import com.qihoo.finance.lowcode.gentracker.ui.base.EditorSettingsInit;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class QueryPanel extends JPanel implements Disposable {
    private static final Font COURIER_FONT = new Font("Courier", Font.PLAIN, UIUtil.getLabelFont().getSize());
    private static final String FILTER_PANEL = "FilterPanel";
    private static final String AGGREGATION_PANEL = "AggregationPanel";
    private final Alarm myUpdateAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
    private final Project project;
    private final MongoVirtualFile virtualFile;
    private final CardLayout queryCardLayout;
    private final JPanel queryContainerPanel = new JPanel();
    private final OperatorPanel filterPanel;
    private final OperatorPanel aggregationPanel;
    private final JBTabbedPane tabbedPane;

    public QueryPanel(Project project, MongoVirtualFile virtualFile) {
        this.project = project;
        this.virtualFile = virtualFile;
        setLayout(new BorderLayout());
        JPanel mainPanel = new JPanel();
        add(mainPanel, BorderLayout.CENTER);
        queryCardLayout = new CardLayout();

        tabbedPane = new JBTabbedPane();
        filterPanel = createFilterPanel(tabbedPane);
        JBScrollPane filterScrollPane = new JBScrollPane(filterPanel);
        tabbedPane.addTab("Filter查询", Icons.General.Filter, filterScrollPane);

        aggregationPanel = createAggregationPanel(tabbedPane);
        tabbedPane.addTab("Aggregation查询", Icons.AGGREGATION, aggregationPanel);
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
    }

    private OperatorPanel createAggregationPanel(JBTabbedPane tabbedPane) {
        return new AggregatorPanel(tabbedPane);
    }

    private OperatorPanel createFilterPanel(JBTabbedPane tabbedPane) {
        return new FilterPanel(tabbedPane);
    }

    public void requestFocusOnEditor() {// Code from requestFocus of EditorImpl
        final IdeFocusManager focusManager = IdeFocusManager.getInstance(this.project);
        JComponent editorContentComponent = getCurrentOperatorPanel().getRequestFocusComponent();
        if (focusManager.getFocusOwner() != editorContentComponent) {
            focusManager.requestFocus(editorContentComponent, true);
        }
    }

    private OperatorPanel getCurrentOperatorPanel() {
        boolean isAggregation = 1 == this.tabbedPane.getSelectedIndex();
        return isAggregation ? aggregationPanel : filterPanel;
    }

    public MongoQueryOptions getQueryOptions(String rowLimit) {
        return getCurrentOperatorPanel().buildQueryOptions(rowLimit);
    }

    @Override
    public void dispose() {
        myUpdateAlarm.cancelAllRequests();
        filterPanel.dispose();
        aggregationPanel.dispose();
    }

    public void toggleToAggregation() {
        queryCardLayout.show(queryContainerPanel, AGGREGATION_PANEL);
    }

    public void toggleToFind() {
        queryCardLayout.show(queryContainerPanel, FILTER_PANEL);
    }

    public void validateQuery() {
        getCurrentOperatorPanel().validateQuery();
    }

    private class AggregatorPanel extends OperatorPanel {

        private final Editor editor;
        private final OperatorCompletionAction operatorCompletionAction;
        private final JBTabbedPane tabbedPane;

        private AggregatorPanel(JBTabbedPane tabbedPane) {
            this.tabbedPane = tabbedPane;
            setLayout(new BorderLayout());
            this.editor = createEditor("{\n" +
                    "$sort: { \"_id\": 1}\n" +
                    "}\n");
            add(this.editor.getComponent(), BorderLayout.CENTER);
            this.operatorCompletionAction = new OperatorCompletionAction(project, editor);
        }

        @Override
        @SuppressWarnings("all")
        public void validateQuery() {
            try {
                String query = getQuery();
                if (StringUtils.isEmpty(query)) {
                    return;
                }
                JSON.parse(query);
            } catch (JSONParseException | NumberFormatException ex) {
                notifyOnErrorForOperator(editor.getComponent(), ex);
            }
        }

        private String getQuery() {
            return String.format("[%s]", StringUtils.trim(this.editor.getDocument().getText()));
        }

        @Override
        @SuppressWarnings("all")
        public MongoQueryOptions buildQueryOptions(String rowLimit) {
            MongoQueryOptions mongoQueryOptions = new MongoQueryOptions();
            try {
                mongoQueryOptions.setOperations(getQuery());
            } catch (JSONParseException ex) {
                notifyOnErrorForOperator(editor.getComponent(), ex);
            }

            if (StringUtils.isNotBlank(rowLimit)) {
                mongoQueryOptions.setResultLimit(Integer.parseInt(rowLimit));
            }

            mongoQueryOptions.setAggregate(1 == this.tabbedPane.getSelectedIndex());
            return mongoQueryOptions;
        }

        @Override
        public JComponent getRequestFocusComponent() {
            return this.editor.getContentComponent();
        }

        @Override
        public void dispose() {
            operatorCompletionAction.dispose();
            EditorFactory.getInstance().releaseEditor(this.editor);
        }
    }

    private class FilterPanel extends OperatorPanel {
        private final Editor selectEditor;
        private final OperatorCompletionAction operatorCompletionAction;
        private final Editor projectionEditor;
        private final Editor sortEditor;
        private final JBTabbedPane tabbedPane;

        private FilterPanel(JBTabbedPane tabbedPane) {
            this.tabbedPane = tabbedPane;

            this.selectEditor = createEditor("{\"key1\" : \"value\", \"key2\" : {$gt : 0}}");
            this.projectionEditor = createEditor("{\"key1\" : \"\", \"key2\" : \"\", \"key3\" : \"\"}");
            this.sortEditor = createEditor("{\"_id\" : 1}");
            this.selectEditor.getDocument().addDocumentListener(new ResizeDocumentListener((EditorEx) this.selectEditor));
            this.projectionEditor.getDocument().addDocumentListener(new ResizeDocumentListener((EditorEx) this.projectionEditor));
            this.sortEditor.getDocument().addDocumentListener(new ResizeDocumentListener((EditorEx) this.sortEditor));

            setLayout(new BorderLayout());
            JPanel formPanel = FormBuilder.createFormBuilder()
                    .addLabeledComponent("Filter(条件过滤)", this.selectEditor.getComponent())
                    .addLabeledComponent("Projection(字段投影)", this.projectionEditor.getComponent())
                    .addLabeledComponent("Sort(字段排序)", this.sortEditor.getComponent())
                    .getPanel();
            formPanel.setBorder(BorderFactory.createEmptyBorder(-5, 15, -5, 20));
            add(formPanel, BorderLayout.CENTER);

            this.operatorCompletionAction = new OperatorCompletionAction(project, selectEditor);
        }

        @Override
        public JComponent getRequestFocusComponent() {
            return this.selectEditor.getContentComponent();
        }

        @Override
        public void validateQuery() {
            validateEditorQuery(selectEditor);
            validateEditorQuery(projectionEditor);
            validateEditorQuery(sortEditor);
        }

        @Override
        public MongoQueryOptions buildQueryOptions(String rowLimit) {
            MongoQueryOptions mongoQueryOptions = new MongoQueryOptions();
            try {
                mongoQueryOptions.setFilter(getQueryFrom(selectEditor));
                mongoQueryOptions.setProjection(getQueryFrom(projectionEditor));
                mongoQueryOptions.setSort(getQueryFrom(sortEditor));
            } catch (Exception ex) {
                notifyOnErrorForOperator(selectEditor.getComponent(), ex);
            }

            if (StringUtils.isNotBlank(rowLimit)) {
                mongoQueryOptions.setResultLimit(Integer.parseInt(rowLimit));
            } else {
                mongoQueryOptions.setResultLimit(MongoQueryOptions.NO_LIMIT);
            }

            mongoQueryOptions.setAggregate(1 == this.tabbedPane.getSelectedIndex());
            return mongoQueryOptions;
        }

        @Override
        public void dispose() {
            operatorCompletionAction.dispose();
            EditorFactory.getInstance().releaseEditor(this.selectEditor);
            EditorFactory.getInstance().releaseEditor(this.projectionEditor);
            EditorFactory.getInstance().releaseEditor(this.sortEditor);
        }

        @SuppressWarnings("all")
        private void validateEditorQuery(Editor editor) {
            try {
                String query = getQueryFrom(editor);
                if (StringUtils.isEmpty(query)) {
                    return;
                }
                JSON.parse(query);
            } catch (Exception ex) {
                notifyOnErrorForOperator(editor.getComponent(), ex);
            }
        }

        private String getQueryFrom(Editor editor) {
            return StringUtils.trim(editor.getDocument().getText());
        }

        private JPanel createSubOperatorPanel(String title, Editor subOperatorEditor) {
            JPanel selectPanel = new JPanel();
            selectPanel.setLayout(new BorderLayout());
            NonOpaquePanel headPanel = new NonOpaquePanel();
            JLabel operatorLabel = new JLabel(title);
            headPanel.add(operatorLabel, BorderLayout.WEST);
            selectPanel.add(headPanel, BorderLayout.NORTH);
            selectPanel.add(subOperatorEditor.getComponent(), BorderLayout.CENTER);

            return selectPanel;
        }
    }

    private abstract class OperatorPanel extends JPanel implements Disposable {

        protected abstract JComponent getRequestFocusComponent();

        protected abstract void validateQuery();

        protected abstract MongoQueryOptions buildQueryOptions(String rowLimit);

        @SuppressWarnings("all")
        void notifyOnErrorForOperator(final JComponent component, Exception ex) {
            String message;
            if (ex instanceof JSONParseException) {
                message = StringUtils.removeStart(ex.getMessage(), "\n");
            } else {
                message = String.format("%s: %s", ex.getClass().getSimpleName(), ex.getMessage());
            }

            final NonOpaquePanel nonOpaquePanel = new NonOpaquePanel();
            JTextPane textPane = Messages.configureMessagePaneUi(new JTextPane(), message);
            textPane.setFont(COURIER_FONT);
            textPane.setBackground(MessageType.ERROR.getPopupBackground());
            nonOpaquePanel.add(textPane, BorderLayout.CENTER);
            nonOpaquePanel.add(new JLabel(MessageType.ERROR.getDefaultIcon()), BorderLayout.WEST);

            UIUtil.invokeLaterIfNeeded(() ->
                    JBPopupFactory.getInstance().createBalloonBuilder(nonOpaquePanel)
                            .setFillColor(MessageType.ERROR.getPopupBackground())
                            .createBalloon()
                            .show(new RelativePoint(component, new Point(0, 0)), Balloon.Position.above)
            );
        }

        Editor createEditor(String placeholder) {
            TextFieldWithCompletion completion = new TextFieldWithCompletion(project,
                    new MongoCompletionProvider(virtualFile),
                    "", false, true, true, true, false);

            EditorFactory editorFactory = EditorFactory.getInstance();
            Editor editor = editorFactory.createEditor(completion.getDocument(), project);
            EditorSettingsInit.fillEditorSettings(editor.getSettings());
            EditorEx editorEx = (EditorEx) editor;
            editorEx.setPlaceholder(placeholder);
            EditorSettingsInit.attachHighlighter(editorEx, "JSON");

            return editor;
        }
    }

    @RequiredArgsConstructor
    private static class ResizeDocumentListener implements DocumentListener {
        @NotNull
        private final EditorEx editor;
        private int lastLineCount = 0;

        @Override
        public void documentChanged(@NotNull DocumentEvent event) {
            Document document = event.getDocument();
            int newLineCount = document.getLineCount();
            if (lastLineCount != newLineCount) {
                lastLineCount = newLineCount;
                editor.getComponent().revalidate();
                editor.getComponent().repaint();
            }
        }
    }
}
