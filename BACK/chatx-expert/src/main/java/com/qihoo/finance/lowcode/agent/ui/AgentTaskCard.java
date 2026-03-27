package com.qihoo.finance.lowcode.agent.ui;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import com.qihoo.finance.lowcode.agent.util.AgentTaskUtils;
import com.qihoo.finance.lowcode.common.constants.AgentTaskStatus;
import com.qihoo.finance.lowcode.common.entity.agent.AgentTaskDetail;
import com.qihoo.finance.lowcode.common.entity.agent.CallbackAction;
import com.qihoo.finance.lowcode.common.ui.base.RoundedPanel;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.JPanelUtils;
import com.qihoo.finance.lowcode.common.util.NotifyUtils;
import com.qihoo.finance.lowcode.common.util.RestTemplateUtil;
import com.qihoo.finance.lowcode.common.utils.LocalDateUtils;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * TaskStatusCard
 *
 * @author fengjinfu-jk
 * date 2024/3/27
 * @version 1.0.0
 * @apiNote TaskStatusCard
 */
public class AgentTaskCard {
    private static final Color background = JBColor.background().darker();
    private static final GridLayout gridLayout2 = new GridLayout(-1, 2);
    private static final GridLayout gridLayout3 = new GridLayout(-1, 3);

    public static JComponent createAgentTaskCard(AgentTaskDetail task, Runnable closeCardAction) {
        // title
        JPanel title = getTitle(task, closeCardAction);
        // 简要信息
        JPanel statusPanel = getStatusPanel(task);
        // 进度
        JProgressBar progressBar = getProgressBar(task);
        // action
        JComponent actionsPanel = getActions(task, progressBar);
        // errMsg
        JComponent errMsgPanel = getErrMsgPanel(task);
        // card
        return buildAgentTaskCard(title, statusPanel, actionsPanel, errMsgPanel);
    }

    private static JComponent getErrMsgPanel(AgentTaskDetail task) {
        if (AgentTaskStatus.FAILED == task.getStatus() && StringUtils.isNotEmpty(task.getErrMsg())) {
            JTextArea errLabel = new JTextArea(task.getErrMsg());
            errLabel.setForeground(JBColor.RED);
            errLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            errLabel.setBackground(background);
            errLabel.setLineWrap(true);
            errLabel.setWrapStyleWord(true);
            errLabel.setEditable(false);
            return errLabel;
        }
        return null;
    }

    @NotNull
    private static JPanel buildAgentTaskCard(JComponent title, JComponent statusPanel, JComponent actionsPanel, JComponent errMsgPanel) {
        JPanel agentTaskCard = new RoundedPanel(background, 20);
        agentTaskCard.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP));

        title.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 0));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(0, 13, 0, 0));
        agentTaskCard.add(title);
        agentTaskCard.add(statusPanel);
        if (Objects.nonNull(actionsPanel)) agentTaskCard.add(actionsPanel);
        if (Objects.isNull(actionsPanel) && Objects.isNull(errMsgPanel)) {
            statusPanel.setBorder(BorderFactory.createEmptyBorder(0, 13, 6, 0));
            return agentTaskCard;
        }

        if (Objects.nonNull(errMsgPanel)) {
            actionsPanel.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 0));
            errMsgPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 0));
            agentTaskCard.add(errMsgPanel);
        } else {
            actionsPanel.setBorder(BorderFactory.createEmptyBorder(2, 10, 5, 0));
        }

        return agentTaskCard;
    }

    private static JComponent getActions(AgentTaskDetail task, JProgressBar progressBar) {
        if (task.getStatus() != AgentTaskStatus.RUNNING) {
            return null;
        }
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        switch (task.getStatus()) {
            case RUNNING -> {
                List<JButton> actionBtnList = new ArrayList<>();
                for (CallbackAction callbackAction : task.getCallbackActions()) {
                    JButton action = new JButton(callbackAction.getActionDesc());
                    action.setForeground(JBColor.BLUE);
                    action.setPreferredSize(new Dimension(action.getPreferredSize().width - 20, action.getPreferredSize().height));
                    action.setBorderPainted(false);
                    action.setContentAreaFilled(false);
                    action.addActionListener(e -> {
                        // call actionUrl
                        // 异步调用任务
                        String message = String.format("执行【%s】中...", callbackAction.getActionDesc());
                        // 提示
                        UIUtil.invokeLaterIfNeeded(() -> NotifyUtils.notifyBalloon(action, message, MessageType.INFO, true));
                        ProgressManager.getInstance().run(new Task.Backgroundable(ProjectUtils.getCurrProject(), message) {
                            @SneakyThrows
                            @Override
                            public void run(@NotNull ProgressIndicator indicator) {
                                try {
                                    RestTemplateUtil.post(callbackAction.getActionUrl(), callbackAction.getActionBody(), callbackAction.getActionHeaders());
                                    String successMsg = String.format("执行【%s】成功", callbackAction.getActionDesc());
                                    UIUtil.invokeLaterIfNeeded(() -> NotifyUtils.notifyBalloon(action, successMsg, MessageType.INFO, true));
                                } catch (Exception e) {
                                    // 消息提示
                                    String errMsg = String.format("执行【%s】失败, 请联系管理员, 错误信息：\n%s", callbackAction.getActionDesc(), e.getMessage());
                                    UIUtil.invokeLaterIfNeeded(() -> NotifyUtils.notifyBalloon(action, errMsg, MessageType.ERROR, true));
                                }
                            }
                        });
                    });
                    action.setPreferredSize(new Dimension(-1, 20));
                    actionBtnList.add(action);
                }

                actions = JPanelUtils.gridPanel(gridLayout3, actionBtnList.toArray(JButton[]::new));
            }
            case SUCCESS, FAILED, CANCEL -> {
                // ignore
            }
        }
        actions.setBackground(background);
        if (actions.getComponents().length > 0) {
            JPanel actionsPanel = JPanelUtils.gridPanel(gridLayout2, progressBar, actions);
            actionsPanel.setBackground(background);
            return actionsPanel;
        } else {
            JPanel actionsPanel = new JPanel(new BorderLayout());
            progressBar.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20));
            actionsPanel.add(progressBar, BorderLayout.CENTER);
            actionsPanel.setBackground(background);
            return actionsPanel;
        }
    }

    @NotNull
    private static JProgressBar getProgressBar(AgentTaskDetail task) {
        JProgressBar progressBar = new JProgressBar();
        progressBar.setBorderPainted(false);
        progressBar.setPreferredSize(new Dimension(-1, 5));
        progressBar.setStringPainted(true);
        progressBar.setOpaque(false);
        if (AgentTaskStatus.RUNNING == task.getStatus()) {
            if (task.getProgressValue() > 0 && task.getProgressValue() < 100) {
                progressBar.setIndeterminate(false);
                progressBar.setValue(task.getProgressValue());
            } else {
                progressBar.setIndeterminate(true);
//                progressBar.setString("执行中");
                progressBar.setString(StringUtils.EMPTY);
            }
        } else if (AgentTaskStatus.SUCCESS == task.getStatus()) {
            progressBar.setIndeterminate(false);
            progressBar.setString("已完成");
        } else if (AgentTaskStatus.CANCEL == task.getStatus()) {
            progressBar.setIndeterminate(false);
            progressBar.setString("已取消");
        } else if (AgentTaskStatus.FAILED == task.getStatus()) {
            progressBar.setIndeterminate(false);
            progressBar.setString("执行失败");
            progressBar.setForeground(JBColor.RED);
        }
        progressBar.setBackground(background);
        return progressBar;
    }

    @NotNull
    private static JPanel getStatusPanel(AgentTaskDetail task) {
        JLabel createdDate = new JLabel("开始 " + LocalDateUtils.convertToPatternString(task.getDateCreated(), LocalDateUtils.FORMAT_DATE_TIME));
        JLabel finishedDate = new JLabel();
        if (AgentTaskStatus.SUCCESS == task.getStatus()) {
            finishedDate.setText((Objects.nonNull(task.getFinishedDate()) ? "已完成 " + LocalDateUtils.convertToPatternString(task.getFinishedDate(), LocalDateUtils.FORMAT_DATE_TIME) : "已完成"));
        } else if (AgentTaskStatus.RUNNING == task.getStatus()) {
            finishedDate.setText("执行中...");
        } else if (AgentTaskStatus.CANCEL == task.getStatus()) {
            finishedDate.setText("已取消");
            finishedDate.setForeground(JBColor.GRAY);
        } else if (AgentTaskStatus.FAILED == task.getStatus()) {
            finishedDate.setText("执行失败");
            finishedDate.setForeground(JBColor.RED);
        }

        JPanel statusPanel = JPanelUtils.gridPanel(gridLayout2, createdDate, finishedDate);
        statusPanel.setBackground(background);
        return statusPanel;
    }

    @NotNull
    private static JPanel getTitle(AgentTaskDetail task, Runnable closeCardAction) {
        JPanel title = new JPanel(new BorderLayout());
        title.setBackground(background);

        JLabel taskName = new JLabel(task.getTaskName());
        taskName.setFont(new Font("微软雅黑", Font.BOLD, 12));
        taskName.setIcon(Icons.scaleToWidth(Icons.AGENT_TASK, 16));
        taskName.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (StringUtils.isNotEmpty(task.getTaskUrl())) {
                    taskName.setText(String.format("<html><u style=\"color: rgb(88,157,246);\">%s</u></html>", task.getTaskName()));
                }
                super.mouseEntered(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                taskName.setText(task.getTaskName());
                super.mouseExited(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (StringUtils.isNotEmpty(task.getTaskUrl())) {
                    BrowserUtil.browse(task.getTaskUrl());
                }
                super.mouseClicked(e);
            }
        });
        title.add(taskName, BorderLayout.WEST);

        if (Objects.nonNull(closeCardAction)) {
            JButton close = new JButton();
            close.setPreferredSize(new Dimension(20, 20));
            close.setIcon(Icons.scaleToWidth(Icons.ROLLBACK2, 13));
            close.setBorderPainted(false);
            close.setContentAreaFilled(false);
            close.addActionListener(event -> {
                new SwingWorker<>() {
                    @Override
                    protected Object doInBackground() {
                        return AgentTaskUtils.closeAgentTask(task.getId());
                    }

                    @Override
                    protected void done() {
                        // reload
                        closeCardAction.run();
                        super.done();
                    }
                }.execute();
            });
            title.add(close, BorderLayout.EAST);
        }

        // desc
        JTextArea taskDesc = new JTextArea(task.getTaskDesc());
        taskDesc.setBackground(background);
        taskDesc.setLineWrap(true);
        taskDesc.setWrapStyleWord(true);
        taskDesc.setEditable(false);
        taskDesc.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        taskDesc.setForeground(JBColor.GRAY);
        taskDesc.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        JPanel titlePanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        titlePanel.add(title);
        titlePanel.add(taskDesc);
        titlePanel.setBackground(background);
        return titlePanel;
    }
}
