package com.qihoo.finance.lowcode.status;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Pair;
import com.intellij.util.concurrency.annotations.RequiresEdt;
import com.qihoo.finance.lowcode.common.util.ApplicationUtil;
import com.qihoo.finance.lowcode.editor.ChatxService;
import com.qihoo.finance.lowcode.editor.statusBar.EditorStatusBarWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.GuardedBy;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatxStatusService implements ChatxStatusListener, Disposable {

    private final Object lock = new Object();

    @GuardedBy("lock")
    @NotNull
    private ChatxStatus status = ChatxStatus.Ready;

    @GuardedBy("lock")
    @Nullable
    private String message;

    private static final AtomicBoolean clientRequestsDisabled = new AtomicBoolean();

    public static boolean isClientRequestsDisabled() {
        return clientRequestsDisabled.get();
    }

    @NotNull
    public static Pair<ChatxStatus, String> getCurrentStatus() {
        return ApplicationManager.getApplication().getService(ChatxStatusService.class).getStatus();
    }

    public ChatxStatusService() {
        ApplicationManager.getApplication().getMessageBus()
                .connect(this)
                .subscribe(ChatxStatusListener.TOPIC, this);
    }

    public static void notifyApplication(@NotNull ChatxStatus status) {
        notifyApplication(status, null);
    }

    public static void notifyApplication(@NotNull ChatxStatus status, @Nullable String customMessage) {
        ApplicationManager.getApplication().getMessageBus()
                .syncPublisher(ChatxStatusListener.TOPIC)
                .onChatxStatus(status, customMessage);
    }

    public void onChatxStatus(@NotNull ChatxStatus status, @Nullable String customMessage) {
        boolean notify = false;
        synchronized (this.lock) {
//            ChatxStatus oldStatus = this.status;
//            if (!oldStatus.isDisablingClientRequests()) {
                notify = (this.status != status);
                this.status = status;
                this.message = customMessage;
//            }
        }
        if (status.isDisablingClientRequests()) {
            boolean changed = clientRequestsDisabled.compareAndSet(false, true);
            if (changed && status == ChatxStatus.IncompatibleClient) {
                Project project = ApplicationUtil.findCurrentProject();
                if (project != null) {
                    ApplicationManager.getApplication().invokeLater(() -> showRequestsDisabledNotification(project));
                }
            }
        } else {
            clientRequestsDisabled.compareAndSet(true, false);
        }
        if (notify) {
            updateAllStatusBarIcons();
        }
    }

    public void dispose() {}

    @NotNull
    private Pair<ChatxStatus, String> getStatus() {
        synchronized (this.lock) {
            if (!ChatxService.getInstance().isSignedIn()) {
                status = ChatxStatus.NotSignedIn;
            }
            return Pair.create(this.status, this.message);
        }
    }

    private void updateAllStatusBarIcons() {
        Runnable action = () -> {
            for (Project project : ProjectManager.getInstance().getOpenProjects()) {
                if (!project.isDisposed())
                    EditorStatusBarWidget.update(project, this.message);
            }
        };
        Application application = ApplicationManager.getApplication();
        if (application.isDispatchThread()) {
            action.run();
        } else {
            application.invokeLater(action);
        }
    }

    @RequiresEdt
    private static void showRequestsDisabledNotification(@NotNull Project project) {
        // TODO show request disabled notification
//        Notification notification = CodegeexNotifications.createFullContentNotification(
//                CodegeexBundle.get("requestsDisabledNotification.title"),
//                CodegeexBundle.get("requestsDisabledNotification.text"), NotificationType.ERROR, true);
//        notification.addAction((AnAction) NotificationAction.createSimpleExpiring(CodegeexBundle.get("requestsDisabledNotification.checkUpdates"), () -> (new CodegeexPluginUpdater.CheckUpdatesTask(project, true)).queue()));
//        notification.addAction((AnAction)NotificationAction.createSimpleExpiring(CodegeexBundle.get("requestsDisabledNotification.hide"), EmptyRunnable.getInstance()));
//        notification.notify(project);
    }
}
