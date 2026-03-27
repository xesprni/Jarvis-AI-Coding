package com.qihoo.finance.lowcode.common.util;

import com.intellij.icons.AllIcons;
import com.intellij.notification.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import groovyjarjarantlr4.v4.runtime.misc.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * GenTrackNotifier
 *
 * @author fengjinfu-jk
 * date 2023/8/21
 * @version 1.0.0
 * @apiNote GenTrackNotifier
 */
public class NotifyUtils {
    private static final Map<String, Long> NOTIFY_TIME_INTERVAL = new HashMap<>();
    private static final int MSG_INTERVAL_SECONDS = 5;

    public static Notification build(String title, String content, NotificationType notificationType) {
        return NotificationGroupManager.getInstance()
                // plugin.xml里配置的id
                .getNotificationGroup("TrackNotify").createNotification(title, content, notificationType);
    }

    public static void notify(@Nullable Project project, String title, String content, NotificationType notificationType, NotificationAction action) {
        // plugin.xml里配置的id.getNotificationGroup("TrackNotify");
        NotificationGroup notificationGroup = NotificationGroupManager.getInstance().getNotificationGroup("TrackNotify");
        if (Objects.isNull(notificationGroup)) return;

        Notification trackNotify = notificationGroup.createNotification(content, notificationType);
        if (StringUtils.isNotEmpty(title)) {
            trackNotify.setTitle(title);
        }

        // click action
        if (Objects.nonNull(action)) {
            trackNotify.addAction(action).notify(project);
        } else {
            trackNotify.notify(project);
        }
    }

    public static void notify(String content, NotificationType notificationType) {
        notify(ProjectUtils.getCurrProject(), null, content, notificationType, null);
    }

    public static void notify(String content, NotificationType notificationType, NotificationAction action) {
        notify(ProjectUtils.getCurrProject(), null, content, notificationType, action);
    }

    public static void notify(String title, String content, NotificationType notificationType) {
        notify(ProjectUtils.getCurrProject(), title, content, notificationType, null);
    }

    public static void notify(String title, String content, NotificationType notificationType, NotificationAction action) {
        notify(ProjectUtils.getCurrProject(), title, content, notificationType, action);
    }

    /**
     * 是否通过瞬时多次通知校验
     * 如果在{@code MSG_INTERVAL_SECONDS}时间内, 检测倒相同信息的通知, 则返回false, 此时外部则不应该重复发送本次通知
     *
     * @param notifyMsg 通知消息
     * @return 校验通过则返回true, 否则返回false
     */
    public static boolean checkNotifyTimeInterval(String notifyMsg) {
        if (!NOTIFY_TIME_INTERVAL.containsKey(notifyMsg)) {
            NOTIFY_TIME_INTERVAL.put(notifyMsg, System.currentTimeMillis());
            return true;
        }

        long lastTime = NOTIFY_TIME_INTERVAL.get(notifyMsg);
        long nowTime = System.currentTimeMillis();

        // 5 内不做重复提示
        if (((nowTime - lastTime) / 1000) < MSG_INTERVAL_SECONDS) {
            return false;
        }

        NOTIFY_TIME_INTERVAL.put(notifyMsg, nowTime);
        return true;
    }

    public static void notifyBalloon(JComponent component, String message, MessageType messageType, boolean hideOnClickOutside) {
        buildBalloon(message, messageType, hideOnClickOutside).show(getNorthOf(component), Balloon.Position.above);
    }

    public static void notifyBalloon(JComponent component, String message, Icon icon, Color textColor, Color fillColor) {
        buildBalloon(message, icon, textColor, fillColor).show(getNorthOf(component), Balloon.Position.above);
    }

    public static Balloon buildBalloon(String message, Icon icon, Color textColor, Color fillColor) {
        return JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(message, icon, textColor, fillColor, null)
                .setFadeoutTime(30000)
                .setHideOnClickOutside(true)
                .setHideOnLinkClick(true)
                .setHideOnCloseClick(true)
                .createBalloon();
    }
    public static Balloon buildBalloon(String message, MessageType messageType, boolean hideOnClickOutside) {
        Icon icon = null;
        if (messageType.equals(MessageType.ERROR)) {
            icon = AllIcons.General.Error;
        } else if (messageType.equals(MessageType.INFO)) {
            icon = AllIcons.General.Information;
        } else if (messageType.equals(MessageType.WARNING)) {
            icon = AllIcons.General.Warning;
        }
        return JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(message, icon, JBColor.foreground(), EditorComponentUtils.BACKGROUND, null)
                .setFadeoutTime(30000)
                .setHideOnClickOutside(hideOnClickOutside)
                .setHideOnLinkClick(true)
                .setHideOnCloseClick(true)
                .createBalloon();
    }

    public static Balloon buildBalloon(String message, MessageType messageType, ActionListener clickHandler, boolean hideOnClickOutside) {
        return JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(message, messageType, null)
                .setFadeoutTime(30000)
                .setHideOnClickOutside(hideOnClickOutside)
                .setHideOnLinkClick(true)
                .setHideOnCloseClick(true)
                .setClickHandler(clickHandler, true)
                .createBalloon();
    }

    @NotNull
    public static RelativePoint getNorthOf(@NotNull JComponent component) {
        final Rectangle visibleRect = component.getVisibleRect();
        final Point point = new Point(visibleRect.x + visibleRect.width / 2, visibleRect.y);
        return new RelativePoint(component, point);
    }

    @NotNull
    public static RelativePoint getSouthOf(@NotNull JComponent component) {
        return RelativePoint.getSouthOf(component);
    }

    @NotNull
    public static RelativePoint getWestOf(@NotNull JComponent component) {
        return RelativePoint.getNorthWestOf(component);
    }

    @NotNull
    public static RelativePoint getEastOf(@NotNull JComponent component) {
        return RelativePoint.getNorthEastOf(component);
    }
}
