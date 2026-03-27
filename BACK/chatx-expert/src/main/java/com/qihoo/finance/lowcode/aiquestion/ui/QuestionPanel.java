package com.qihoo.finance.lowcode.aiquestion.ui;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollBar;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.util.concurrency.annotations.RequiresEdt;
import com.intellij.util.ui.UIUtil;
import com.qifu.controller.TaskController;
import com.qihoo.finance.lowcode.aiquestion.dto.GitIndex;
import com.qihoo.finance.lowcode.aiquestion.ui.factory.InputPanelFactory;
import com.qihoo.finance.lowcode.aiquestion.ui.factory.MessageRowFactory;
import com.qihoo.finance.lowcode.aiquestion.ui.worker.ChatSwingWorker;
import com.qihoo.finance.lowcode.aiquestion.util.GitIndexUtils;
import com.qihoo.finance.lowcode.common.action.GenerateUnitTestAction;
import com.qihoo.finance.lowcode.common.constants.QuestionType;
import com.qihoo.finance.lowcode.common.enums.GitIndexStatus;
import com.qihoo.finance.lowcode.common.util.EditorComponentUtils;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.NotifyUtils;
import com.qihoo.finance.lowcode.common.util.UserUtils;
import com.qihoo.finance.lowcode.design.ui.DatabaseBasePanel;
import com.qihoo.finance.lowcode.gentracker.tool.StringUtils;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.Objects;
import java.util.Optional;


public class QuestionPanel extends DatabaseBasePanel {

    static final String NAME = "askAiPanel";

    @Getter
    private final NonOpaquePanel panel;
    @Getter
    private AskAiDecorator askAiDecorator;
    @Getter
    private JBScrollPane viewScrollPane;
    @Getter
    private JPanel viewPanel;
    @Getter
    private final InputPanelFactory inputPanelFactory;
    private MessageRowFactory messageRowFactory;
    @Getter
    private ChatSwingWorker chatSwingWorker;

    private TaskController taskController;
    @Getter
    private boolean autoScrollBottom = true;

    public QuestionPanel(@NotNull Project project) {
        super(project);
        panel = new NonOpaquePanel(new BorderLayout());
        panel.setName(NAME);
        panel.setOpaque(false);
        inputPanelFactory = new InputPanelFactory(project);
        // 初始化
        initAiQuestionEvent();
    }

    public void scrollBottom(boolean force) {
        if (autoScrollBottom || force) {
            JScrollBar verticalScrollBar = viewScrollPane.getVerticalScrollBar();
            if (verticalScrollBar != null && verticalScrollBar.getValue() < verticalScrollBar.getMaximum()) {
                SwingUtilities.invokeLater(() -> verticalScrollBar.setValue(verticalScrollBar.getMaximum()));
            }
        }
    }

    private void initAiQuestionEvent() {
        // Icon
        DefaultActionGroup actionGroup = (DefaultActionGroup) ActionManager.getInstance().getAction("ChatX.ActionGroup.AiQuestion");
        if (Objects.nonNull(actionGroup)) {
            actionGroup.getTemplatePresentation().setIcon(Icons.scaleToWidth(Icons.LOGO_ROUND, 16));
        }

        // projectView fixme: 会覆盖原有拖拽事件, 禁用
//        ProjectView projectView = ProjectView.getInstance(project);
//        AbstractProjectViewPane projectViewPane = projectView.getCurrentProjectViewPane();
//        if (Objects.nonNull(projectViewPane)) {
//            UIUtil.forEachComponentInHierarchy(projectViewPane.getTree(), inputPanelFactory::supportDragAndDropTips);
//        }
    }

    @Override
    public JComponent createPanel() {
        // 重画
        repaintPanel();

        return panel;
    }

    @RequiresEdt
    public void repaintPanel() {
        panel.removeAll();
        panel.setBorder(null);
        JComponent viewTextComponent = createViewComponent();
        // decorator
        askAiDecorator = new AskAiDecorator(viewTextComponent, this, 0);
        panel.add(askAiDecorator.getComponent(), BorderLayout.CENTER);
        askAiDecorator.showMainPage();

        JPanel inputPanel = inputPanelFactory.create();
        panel.add(inputPanel, BorderLayout.SOUTH);

        inputPanelFactory.flushAssistant(inputPanelFactory.getAssistant(), "", true);
        UIUtil.forEachComponentInHierarchy(panel, inputPanelFactory::supportDragAndDrop);
        panel.repaint();
    }

    private JComponent createViewComponent() {
        viewPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        viewPanel.setBackground(JBColor.background());
        viewPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
        viewPanel.setAutoscrolls(false);
        viewScrollPane = new JBScrollPane(viewPanel);
        viewScrollPane.setBorder(null);
        viewScrollPane.setVerticalScrollBarPolicy(JBScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        viewScrollPane.setHorizontalScrollBarPolicy(JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        viewScrollPane.setAutoscrolls(false);
        viewScrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            private int previousValue;

            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                int currentValue = e.getValue();
                if (currentValue < previousValue) {
                    autoScrollBottom = false;
                } else {
                    Adjustable adjustable = e.getAdjustable();
                    if (adjustable instanceof JBScrollBar scrollBar) {
                        int extent = scrollBar.getModel().getExtent();
                        if (scrollBar.getModel().getMaximum() - (extent + currentValue) <= 100) {
                            autoScrollBottom = true;
                        }
                    }
                }
                previousValue = currentValue;
            }
        });
        viewPanel.setSize(new Dimension(viewScrollPane.getWidth(), Short.MAX_VALUE));
        messageRowFactory = new MessageRowFactory();
        return viewScrollPane;
    }

    public void renderMsg(String username, String msg) {
        JPanel msgRow = messageRowFactory.create(username, msg);
        if (viewPanel.getComponentCount() > 0) {
            // RoundPanel bug, 暂时无法通过设置border控制间距, 添加empty label来达成效果
            JLabel empty = new JLabel();
            empty.setPreferredSize(new Dimension(-1, 8));
            viewPanel.add(empty);
        }
        viewPanel.add(msgRow);

        JLabel empty = new JLabel();
        empty.setPreferredSize(new Dimension(-1, 2));
        viewPanel.add(empty);
        viewPanel.revalidate();
        viewPanel.repaint();

        // question定位到最上方
        // scroll
//        viewScrollPane.revalidate();
//        viewScrollPane.repaint();
        JScrollBar verticalScrollBar = viewScrollPane.getVerticalScrollBar();
        verticalScrollBar.setMaximum(10000);
        verticalScrollBar.setValue(10000);
    }

    public void askAi(String assistant, QuestionType questionType, String question) {
        askAi(assistant, questionType, question, null);
    }

    public void askAi(String assistant, QuestionType questionType, String question, String stackTrace) {
        if (!checkActiveGitIndex()) {
            inputPanelFactory.askAiDone();
            return;
        }

        // stop decorator
        if (Objects.nonNull(askAiDecorator)) askAiDecorator.hidden();
        inputPanelFactory.startProgress();

        // render question
        String displayedQuestion = question;
        if (questionType == QuestionType.COMMENT_CODE) {
            displayedQuestion = ChatxApplicationSettings.settings().commentCodePrefix + question;
        } else if (questionType == QuestionType.EXPLAIN_CODE) {
            displayedQuestion = ChatxApplicationSettings.settings().explainCodePrefix + question;
        } else if (questionType == QuestionType.OPTIMIZE_CODE) {
            displayedQuestion = ChatxApplicationSettings.settings().optimizeCodePrefix + question;
        } else if (questionType == QuestionType.UNIT_TEST) {
            displayedQuestion = ChatxApplicationSettings.settings().unitTestPrefix + question;
        } else if (questionType == QuestionType.OPTIMIZATION_NAMING) {
            displayedQuestion = ChatxApplicationSettings.settings().translatePrefix + question;
        }
        if (question.startsWith(ChatxApplicationSettings.settings().commentCodePrefix)) {
            question = question.substring(ChatxApplicationSettings.settings().commentCodePrefix.length());
            questionType = QuestionType.COMMENT_CODE;
        } else if (question.startsWith(ChatxApplicationSettings.settings().explainCodePrefix)) {
            question = question.substring(ChatxApplicationSettings.settings().explainCodePrefix.length());
            questionType = QuestionType.EXPLAIN_CODE;
        } else if (question.startsWith(ChatxApplicationSettings.settings().optimizeCodePrefix)) {
            question = question.substring(ChatxApplicationSettings.settings().optimizeCodePrefix.length());
            questionType = QuestionType.OPTIMIZE_CODE;
        } else if (question.startsWith(ChatxApplicationSettings.settings().unitTestPrefix)) {
            question = GenerateUnitTestAction.preparePrompt(question);
            questionType = QuestionType.UNIT_TEST;
        } else if (question.startsWith(ChatxApplicationSettings.settings().translatePrefix)) {
            question = question.substring(ChatxApplicationSettings.settings().translatePrefix.length());
            questionType = QuestionType.OPTIMIZATION_NAMING;
        }
        final String username = Optional.ofNullable(UserUtils.getUserInfo().getUserNo()).orElse("You");
        renderMsg(username, displayedQuestion);
//        chatSwingWorker = new ChatSwingWorker(project, this, assistant, questionType, question, stackTrace);
//        chatSwingWorker.execute();

        chatSwingWorker = new ChatSwingWorker(project, this, assistant, questionType, question, stackTrace);
        if (taskController == null) {
            taskController = new TaskController(chatSwingWorker);
        } else {
            taskController.setChatSwingWorker(chatSwingWorker);
        }
        taskController.chat(question);
        autoScrollBottom = true;
    }

    private boolean checkActiveGitIndex() {
        String gitToggleValue = inputPanelFactory.getGitToggleValue();
        if (StringUtils.isEmpty(gitToggleValue) || !"Y".equals(gitToggleValue)) return true;

        GitIndex gitIndex = GitIndexUtils.gitIndexStatus(project);
        if (GitIndexStatus.done.name().equals(gitIndex.getStatus()) || GitIndexStatus.reindex.name().equals(gitIndex.getStatus())) {
            return true;
        }
        JPanel inputPanel = inputPanelFactory.getComponent();
        NotifyUtils.notifyBalloon(inputPanelFactory.getGitInfo(), "分支索引未完成, 请稍后再使用仓库问答",
                Icons.BUILD_GIT_INDEX, inputPanel.getForeground(), EditorComponentUtils.BACKGROUND);
        return false;
    }

    public void askAiDone() {
        if (Objects.nonNull(inputPanelFactory)) inputPanelFactory.askAiDone();
    }
}
