package com.qihoo.finance.lowcode.agent.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.LoadingDecorator;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.UIUtil;
import com.qihoo.finance.lowcode.agent.util.AgentTaskUtils;
import com.qihoo.finance.lowcode.common.entity.agent.AgentTaskDetail;
import com.qihoo.finance.lowcode.common.factory.ChatXToolWindowFactory;
import com.qihoo.finance.lowcode.common.ui.CustomHeightTabbedPaneUI;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.JButtonUtils;
import com.qihoo.finance.lowcode.common.util.LowCodeAppUtils;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * MessagePopupPanel
 *
 * @author fengjinfu-jk
 * date 2024/3/27
 * @version 1.0.0
 * @apiNote MessagePopupPanel
 */
public class AgentTaskPopup implements Disposable {
    private static final JLabel noneRecord = new JLabel(Icons.scaleToWidth(Icons.HOLDER, 180));
    private static final JLabel noneRecord2 = new JLabel(Icons.scaleToWidth(Icons.HOLDER, 180));

    static {
        noneRecord.setBorder(BorderFactory.createEmptyBorder(200, 0, 30, 0));
        noneRecord2.setBorder(BorderFactory.createEmptyBorder(200, 0, 65, 0));
    }

    public static void show() {
        if (!LowCodeAppUtils.isLogin()) {
            return;
        }
        ToolWindow toolWindow = ChatXToolWindowFactory.show();
        // show popup
        PopupPanel popupPanel = new AgentTaskPopup().createPanel();
        JComponent popupComponent = popupPanel.getPopupPanel();
        JBPopup popup = JBPopupFactory.getInstance().createComponentPopupBuilder(popupComponent, popupComponent).createPopup();
        popupPanel.getClose().addActionListener(e -> popup.cancel());
        popup.showInCenterOf(toolWindow.getComponent());
    }

    private PopupPanel createPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        Dimension maxSize = ChatXToolWindowFactory.getToolWindow().getComponent().getSize();
        panel.setPreferredSize(new Dimension(Math.max(450, maxSize.width), maxSize.height - 10));

        // title
        JPanel titlePanel = new JPanel(new BorderLayout());
        JLabel title = new JLabel();
        title.setIcon(Icons.scaleToWidth(Icons.AGENT_TASK2, 16));
        title.setFont(new Font("微软雅黑", Font.BOLD, 13));
        title.setText("我的任务列表");
        title.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
        titlePanel.add(title, BorderLayout.WEST);

        // close pop
        JButton close = JButtonUtils.createNonOpaqueButton(
                Icons.scaleToWidth(Icons.ROLLBACK2, 13), new Dimension(20, 20));
        titlePanel.add(close, BorderLayout.EAST);
        panel.add(titlePanel, BorderLayout.NORTH);

        JPanel agentTaskCard = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        JPanel agentCloseTaskCard = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        JBScrollPane agentTaskScroll = new JBScrollPane();
        JBScrollPane agentCloseTaskScroll = new JBScrollPane();

        JBTabbedPane tabbedPane = new JBTabbedPane();
        tabbedPane.setUI(new CustomHeightTabbedPaneUI());
        tabbedPane.add("最近任务", agentTaskScroll);
        tabbedPane.add("历史任务", agentCloseTaskScroll);
        LoadingDecorator loadingDecorator = new LoadingDecorator(tabbedPane, this, 0);

        panel.add(loadingDecorator.getComponent(), BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        // action
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearAll = JButtonUtils.createNonOpaqueButton("全部清空");
        clearAll.setBorder(BorderFactory.createEmptyBorder(0, -10, 0, -10));
        clearAll.setForeground(JBColor.BLUE);
        clearAll.addActionListener(e -> {
            UIUtil.invokeLaterIfNeeded(() -> loadingDecorator.startLoading(false));
            new SwingWorker<>() {
                @SneakyThrows
                @Override
                protected Object doInBackground() {
                    return AgentTaskUtils.clearAllAgentTask();
                }

                @Override
                protected void done() {
                    // clear all
                    reloadPopupView(loadingDecorator, agentTaskScroll, agentTaskCard, agentCloseTaskScroll, agentCloseTaskCard);
                }
            }.execute();
        });
        actionPanel.add(clearAll);
        tabbedPane.addChangeListener(c -> actionPanel.setVisible(tabbedPane.getSelectedIndex() == 0));
        panel.add(actionPanel, BorderLayout.SOUTH);

        // load
        reloadPopupView(loadingDecorator, agentTaskScroll, agentTaskCard, agentCloseTaskScroll, agentCloseTaskCard);
        return new PopupPanel(panel, close);
    }

    private static void reloadPopupView(LoadingDecorator loadingDecorator, JBScrollPane agentTaskScroll, JPanel agentTaskCard, JBScrollPane agentCloseTaskScroll, JPanel agentCloseTaskCard) {
        new SwingWorker<List<AgentTaskDetail>, List<AgentTaskDetail>>() {
            @Override
            protected List<AgentTaskDetail> doInBackground() {
                UIUtil.invokeLaterIfNeeded(() -> loadingDecorator.startLoading(false));
                return AgentTaskUtils.queryUserRecentAgentTask();
            }

            @SneakyThrows
            @Override
            protected void done() {
                List<AgentTaskDetail> agentTasks = get();
                UIUtil.invokeLaterIfNeeded(() -> {
                    agentTaskCard.removeAll();
                    agentCloseTaskCard.removeAll();
                    if (!CollectionUtils.isEmpty(agentTasks)) {
                        for (AgentTaskDetail agentTask : agentTasks) {
                            if (agentTask.isHidden()) {
                                agentCloseTaskCard.add(AgentTaskCard.createAgentTaskCard(agentTask, null));
                            } else {
                                agentTaskCard.add(AgentTaskCard.createAgentTaskCard(agentTask,
                                        () -> reloadPopupView(loadingDecorator, agentTaskScroll, agentTaskCard, agentCloseTaskScroll, agentCloseTaskCard)));
                            }
                        }
                    }
                    agentTaskScroll.setViewportView(agentTaskCard.getComponents().length > 0 ? agentTaskCard : noneRecord);
                    agentCloseTaskScroll.setViewportView(agentCloseTaskCard.getComponents().length > 0 ? agentCloseTaskCard : noneRecord2);
                    loadingDecorator.stopLoading();
                    super.done();
                });
            }
        }.execute();
    }


    @Override
    public void dispose() {

    }

    @Data
    private static class PopupPanel {
        JButton close;
        JComponent popupPanel;

        public PopupPanel(JComponent popupPanel, JButton close) {
            this.close = close;
            this.popupPanel = popupPanel;
        }
    }
}
