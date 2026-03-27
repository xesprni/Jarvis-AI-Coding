package org.qifu.devops.ide.plugins.jiracommit.provider;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.notification.NotificationsManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;

public class PluginNotifier {

    /**
     *
     * @param project
     * @param title
     * @param message
     * @param action
     * @param notifyType NotificationType.INFORMATION|NotificationType.WARNING
     */
    public void showNotifier(Project project, String title, String message, AnAction action,NotificationType notifyType){

        NotificationGroupManager.getInstance().getNotificationGroup("Qifu Devops Message Notification Group")
                .createNotification(message, notifyType)
                .setTitle(title)
                .addAction(action)
                .notify(project);
    }

}
