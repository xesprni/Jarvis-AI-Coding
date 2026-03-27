package com.qihoo.finance.lowcode.editor;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import com.intellij.util.concurrency.annotations.RequiresEdt;
import com.qihoo.finance.lowcode.common.util.NotifyUtils;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;
import org.jetbrains.annotations.NotNull;

public interface ChatxService {

    static ChatxService getInstance() {
        return ApplicationManager.getApplication().getService(ChatxService.class);
    }

    @RequiresEdt
    default String getLanguageFromEditor(@NotNull Editor editor) {
        String lang = "text";
        PsiFile file = PsiDocumentManager.getInstance(editor.getProject()).getCachedPsiFile(editor.getDocument());
        if (file != null) {
            VirtualFile virtualFile = file.getVirtualFile();
            if (virtualFile != null) {
                FileType fileType = virtualFile.getFileType();
                lang = fileType.getName();
            }
        }
        return lang;
    }

    default boolean isChineseEnabled() {
//        ChatxApplicationState settings = ChatxApplicationSettings.settings();
//        PluginId pluginId = null;
//        try {
//            Method findIdMethod = PluginId.class.getMethod("findId", String.class);
//            pluginId = (PluginId)findIdMethod.invoke(null, new Object[] { "com.intellij.zh" });
//        } catch (NoSuchMethodException|java.lang.reflect.InvocationTargetException|IllegalAccessException|ClassCastException ignored) {}
//        if (pluginId == null) {
//            return false;
//        }
//        IdeaPluginDescriptor ideaPluginDescriptor = PluginManagerCore.getPlugin(pluginId);
//        return (ideaPluginDescriptor != null && ideaPluginDescriptor.isEnabled());
        return true;
    }

    @RequiresEdt
    default void showLoginNotification(@NotNull Project project, boolean force) {
        boolean shown = (ChatxApplicationSettings.settings()).signinNotificationShown;
        if (force || !shown) {
            if (!force) {
                (ChatxApplicationSettings.settings()).signinNotificationShown = true;
            }
            NotifyUtils.notify("请先登录", NotificationType.WARNING);
            // TODO 打开登录页
        }
    }


    @RequiresEdt
    default void showPleaseSelectNotification(@NotNull Project project) {
        NotifyUtils.notify("请选中一段内容", NotificationType.INFORMATION);
    }

    @RequiresEdt
    default void showSelectionTooLongNotification(@NotNull Project project) {
        NotifyUtils.notify("选中的内容过长", NotificationType.INFORMATION);
    }

    @RequiresEdt
    default void showSelectionChangedNotification(@NotNull Project project) {
        NotifyUtils.notify("选中的内容已改变", NotificationType.INFORMATION);
    }


    @RequiresEdt
    default void showInfoNotification(@NotNull Project project, String title, String content) {
        NotifyUtils.notify(title, content, NotificationType.INFORMATION);
    }

    @RequiresEdt
    default void showErrorNotification(@NotNull Project project, String title, String content) {
        NotifyUtils.notify(title, content, NotificationType.ERROR);
    }

    @RequiresEdt
    default void showCaretPositions(@NotNull Project project, int caretOffset, int startOffset) {
        NotifyUtils.notify("插入光标位置：" + caretOffset, NotificationType.INFORMATION);
    }

    @RequiresEdt
    default void showPrefixSuffix(@NotNull Project project, String prefix, String suffix) {
        NotifyUtils.notify("prefix：" + prefix + ", suffix: " + suffix, NotificationType.INFORMATION);
    }

    boolean isSignedIn();

    @RequiresEdt
    @RequiresBackgroundThread
    void loginInteractive(@NotNull Project project);

    @RequiresEdt
    @RequiresBackgroundThread
    void logout(@NotNull Project project);

}
