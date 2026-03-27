package com.qihoo.finance.lowcode.agent.ui;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.concurrency.EdtExecutorService;
import com.qihoo.finance.lowcode.agent.util.AgentTaskUtils;
import com.qihoo.finance.lowcode.common.entity.agent.AgentTaskSignal;
import com.qihoo.finance.lowcode.common.factory.ChatXToolWindowFactory;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.NotifyUtils;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * AgentNotifications
 *
 * @author fengjinfu-jk
 * date 2024/5/7
 * @version 1.0.0
 * @apiNote AgentNotifications
 */
public class AgentNotifications {
    private static volatile boolean running = false;
    public static volatile boolean notifyNew = false;
    private static final Map<Project, ScheduledFuture<?>> projectFuture = new HashMap<>();

    public static void registerAgentNotify(JComponent baseComponent, Balloon.Position position) {
        Project project = ProjectUtils.getCurrProject();
        if (projectFuture.containsKey(project)) {
            projectFuture.get(project).cancel(true);
        }

        ScheduledFuture<?> scheduledFuture = EdtExecutorService.getScheduledExecutorInstance()
                .scheduleWithFixedDelay(() -> {
                    if (!AgentTaskUtils.isLastFocusProject(project)) return;
                    if (!AgentTaskUtils.isLogin()) return;
                    if (running) return;
                    running = true;

                    // 窗口激活时, 才做提示
                    new SwingWorker<AgentTaskSignal, AgentTaskSignal>() {
                        @Override
                        protected AgentTaskSignal doInBackground() {
                            return AgentTaskUtils.checkingAgentTaskSignal();
                        }

                        @SneakyThrows
                        @Override
                        protected void done() {
                            try {
                                AgentTaskSignal signal = get();
                                if (signal.isNewVersion()) {
                                    notifyNew = true;
                                    // 如果插件被收起, 气泡通知
                                    for (AgentTaskSignal.Message message : signal.getMessageList()) {
                                        if (ChatXToolWindowFactory.isHidden()) {
                                            NotifyUtils.build(message.isSuccess() ? "任务执行" : "任务执行失败", message.getMessage(), message.isSuccess() ? NotificationType.INFORMATION : NotificationType.ERROR)
                                                    .setIcon(Icons.scaleToWidth(Icons.AGENT_TASK2, 16))
                                                    .addAction(new NotificationAction("查看详情") {
                                                        @Override
                                                        public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                                                            // 展开任务列表
                                                            AgentTaskPopup.show();
                                                        }
                                                    }).notify(ProjectUtils.getCurrProject());
                                        } else {
                                            Balloon balloon = NotifyUtils.buildBalloon(String.format("<html>%s  <a href=\"-\">查看详情</a></html>", message.getMessage()),
                                                    message.isSuccess() ? MessageType.INFO : MessageType.ERROR,
                                                    e -> AgentTaskPopup.show(), false);
                                            switch (position) {
                                                case below -> {
                                                    // 坐标修正
                                                    final Rectangle visibleRect = baseComponent.getVisibleRect();
                                                    final Point point = new Point(visibleRect.x + visibleRect.width / 4, visibleRect.y + visibleRect.height);
                                                    RelativePoint relativePoint = new RelativePoint(baseComponent, point);

                                                    balloon.show(relativePoint, position);
                                                }
                                                case above ->
                                                        balloon.show(NotifyUtils.getNorthOf(baseComponent), position);
                                                case atLeft ->
                                                        balloon.show(NotifyUtils.getWestOf(baseComponent), position);
                                                case atRight ->
                                                        balloon.show(NotifyUtils.getEastOf(baseComponent), position);
                                            }
                                        }
                                    }
                                }
                            } finally {
                                running = false;
                                super.done();
                            }
                        }
                    }.execute();
                }, 10, 5, TimeUnit.SECONDS);

        projectFuture.put(project, scheduledFuture);
    }
}
