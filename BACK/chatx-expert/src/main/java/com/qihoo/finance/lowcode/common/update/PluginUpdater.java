package com.qihoo.finance.lowcode.common.update;

import com.google.common.collect.Lists;
import com.intellij.ide.ui.search.SearchUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.impl.ApplicationImpl;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableGroup;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.options.ex.ConfigurableExtensionPointUtil;
import com.intellij.openapi.options.newEditor.SettingsDialogFactory;
import com.intellij.openapi.progress.BackgroundTaskQueue;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.updateSettings.impl.*;
import com.intellij.util.ArrayUtil;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import com.intellij.util.concurrency.annotations.RequiresEdt;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.entity.dto.plugins.PluginConfig;
import com.qihoo.finance.lowcode.common.util.*;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import groovyjarjarantlr4.v4.runtime.misc.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.intellij.openapi.project.ProjectUtil.currentOrDefaultProject;

/**
 * PluginUpdater
 *
 * @author fengjinfu-jk
 * date 2023/12/5
 * @version 1.0.0
 * @apiNote PluginUpdater
 */
@Slf4j
public class PluginUpdater {
    public static final String title = "Update";
    public static final String tips = "Jarvis更新啦, 快来体验最新功能 !";
    public static final String nameToSelect = "Plugins";
    public static final String checkingTitle = String.format("Checking for plugin %s updates", GlobalDict.PLUGIN_NAME);
    private static final AtomicBoolean hasRunUpdatePlugin = new AtomicBoolean(false);
    private static final AtomicBoolean isInstallingUpdate = new AtomicBoolean(false);

    // 获取Jarvis常驻通知组（通知不会自动消失）
    private static NotificationGroup getJarvisStickyNotificationGroup() {
        return NotificationGroup.findRegisteredGroup("jarvisStickyNotifications");
    }


    public static final class CheckUpdatesTask extends Task.Backgroundable {
        private final String noUpdateVersionTips;

        public CheckUpdatesTask(@NotNull Project project) {
            super(project, title, true);
            this.noUpdateVersionTips = null;
        }


        public CheckUpdatesTask(@NotNull Project project, String noUpdateVersionTips) {
            super(project, title, true);
            this.noUpdateVersionTips = noUpdateVersionTips;
        }

        public void run(@NotNull @org.jetbrains.annotations.NotNull ProgressIndicator indicator) {
            log.info("Checking for plugins update...");
            if (Objects.isNull(this.myProject) || this.myProject.isDisposed()) return;
            
            // 防止并发检查更新
            if (!hasRunUpdatePlugin.compareAndSet(false, true)) {
                log.info("Another update check is already running, skipping...");
                return;
            }
            
            try {
                PluginDownloader pluginDownloader = PluginUpdater.findAvailableUpdate(indicator);
                // checking update for plugin
                if (Objects.nonNull(pluginDownloader)) {
                    PluginConfig pluginConfig = LowCodeAppUtils.getPluginConfig(false);
                    if (pluginConfig.isForceUpdate()) {
                        doInstallUpdate(this.myProject, pluginDownloader, indicator);
                    } else {
                        StartupManager.getInstance(this.myProject).runWhenProjectIsInitialized(() -> PluginUpdater.notifyUpdateAvailable(this.myProject, pluginDownloader));
                    }
                } else if (StringUtils.isNotEmpty(noUpdateVersionTips)) {
                    StartupManager.getInstance(this.myProject).runWhenProjectIsInitialized(() -> PluginUpdater.notifyUpdateUnavailable(this.myProject, noUpdateVersionTips));
                }
            } finally {
                hasRunUpdatePlugin.set(false);
            }
        }
    }

    @SuppressWarnings("all")
    private static void doInstallUpdate(Project project, PluginDownloader pluginDownloader, ProgressIndicator indicator) {
        // 防止并发安装更新
        if (!isInstallingUpdate.compareAndSet(false, true)) {
            log.info("Another installation is in progress, skipping...");
            return;
        }
        
        try {
            log.info("start installing:" + pluginDownloader.getPluginVersion());
            Boolean installResult = UpdateInstaller.installPluginUpdates(Collections.singletonList(pluginDownloader), indicator);
            log.info("finished installing:" + installResult);
            if (project != null) {
                notifyUpdateFinished(project);
            } else {
                Project[] projects = ProjectManager.getInstance().getOpenProjects();

                for(Project p : projects) {
                    if (p.isOpen()) {
                        notifyUpdateFinished(p);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("install update failed:" + pluginDownloader.getPluginVersion(), e);
        } finally {
            isInstallingUpdate.set(false);
        }

    }

    @SuppressWarnings("all")
    private static Collection<PluginDownloader> findUpdatedPluginsForNewVersion(@NotNull ProgressIndicator indicator) {
        try {
            PluginUpdates pluginUpdates = UpdateChecker.getInternalPluginUpdates(null, indicator).getPluginUpdates();
            if (pluginUpdates == null) return Collections.emptyList();
            Collection<PluginDownloader> allEnabled = pluginUpdates.getAllEnabled();
            return allEnabled == null ? Collections.emptyList() : allEnabled;
        } catch (Exception e) {
            log.warn("checkPluginsUpdate failed", e);
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public static void notifyUpdateFinished(@org.jetbrains.annotations.NotNull Project project) {
        String title = "插件更新成功";
        String content = String.format("%s 已成功更新，请重启 IDE 以使新版本生效", GlobalDict.PLUGIN_NAME);

        NotificationGroup notificationGroup = getJarvisStickyNotificationGroup();
        Notification notification = notificationGroup.createNotification(title, content, NotificationType.INFORMATION, null);
        notification.addAction(NotificationAction.createSimpleExpiring("立即重启", PluginUpdater::restartLater));
        notification.addAction(NotificationAction.createSimpleExpiring("打开插件设置", () -> ShowSettingsUtil.getInstance().showSettingsDialog(project, "Plugins")));

        notification.setIcon(Icons.scaleToWidth(Icons.ROCKET, 20));
        notification.notify(project);
    }

    public static void restartLater() {
        ApplicationImpl application = (ApplicationImpl) ApplicationManager.getApplication();
        application.invokeLater(() -> application.restart(6, ArrayUtil.EMPTY_STRING_ARRAY));
    }

    @RequiresEdt
    private static void notifyUpdateAvailable(@NotNull Project project, PluginDownloader pluginDownloader) {
        // 重复通知检测
        if (!NotifyUtils.checkNotifyTimeInterval(tips)) return;

        String onlineVersion = pluginDownloader.getPluginVersion();
        NotifyUtils.build(title, tips, NotificationType.WARNING).setIcon(Icons.scaleToWidth(Icons.ROCKET, 20)).addAction(new NotificationAction(String.format("立即更新到 %s", onlineVersion)) {
            @Override
            public void actionPerformed(@org.jetbrains.annotations.NotNull AnActionEvent e, @org.jetbrains.annotations.NotNull Notification notification) {
                // 定位到插件市场并过滤插件信息
                new PluginUpdateDialog(project, Lists.newArrayList(pluginDownloader), null, false).show();
            }
        }).notify(ProjectUtils.getCurrProject());
    }

    public static void searchAvailableUpdate() {
        // 防止同时开启多次检测任务
        if (!hasRunUpdatePlugin.compareAndSet(false, true)) {
            return;
        }

        Project project = ApplicationUtil.findCurrentProject();
        BackgroundTaskQueue backgroundTaskQueue = new BackgroundTaskQueue(project, checkingTitle);
        backgroundTaskQueue.run(new Task.Backgroundable(project, checkingTitle) {
            @Override
            public void run(@org.jetbrains.annotations.NotNull ProgressIndicator indicator) {
                try {
                    PluginDownloader availableUpdate = findAvailableUpdate(indicator);
                    if (Objects.nonNull(availableUpdate)) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            // 定位到插件市场并过滤插件信息
                            new PluginUpdateDialog(project, Lists.newArrayList(availableUpdate), null, false).show();
                        });
                    }
                } finally {
                    hasRunUpdatePlugin.set(false);
                }
            }

        }, ModalityState.current(), null);
    }

    @RequiresBackgroundThread
    public static void updateAdnShowResult(Project project) {
        UpdateSettings settingsCopy = new UpdateSettings();
        settingsCopy.getState().copyFrom(UpdateSettings.getInstance().getState());
        settingsCopy.getState().setCheckNeeded(true);
        settingsCopy.getState().setPluginsCheckNeeded(true);
        settingsCopy.getState().setThirdPartyPluginsAllowed(true);
        settingsCopy.getState().setShowWhatsNewEditor(false);

        UpdateChecker.updateAndShowResult(project, settingsCopy);
    }

    @RequiresEdt
    public static void searchPlugins(Project project, String filter) {
        // 定位到插件市场并过滤插件信息
        ConfigurableGroup group = ConfigurableExtensionPointUtil.getConfigurableGroup(project, false);
        Configurable configurableToSelect = findPreselectedByDisplayName(nameToSelect, Collections.singletonList(group));

        // 打开插件市场并自动搜索当前插件
        SettingsDialogFactory.getInstance().create(currentOrDefaultProject(project), Collections.singletonList(group), configurableToSelect, filter).show();
    }

    @RequiresEdt
    private static void notifyUpdateUnavailable(@NotNull Project project, String noUpdateVersionTips) {
        // 重复通知检测
        if (!NotifyUtils.checkNotifyTimeInterval(noUpdateVersionTips)) return;

        NotifyUtils.build(title, noUpdateVersionTips, NotificationType.INFORMATION).setIcon(Icons.scaleToWidth(Icons.ROCKET, 20)).addAction(new NotificationAction(String.format("关于%s", GlobalDict.PLUGIN_NAME)) {
            @Override
            public void actionPerformed(@org.jetbrains.annotations.NotNull AnActionEvent e, @org.jetbrains.annotations.NotNull Notification notification) {
                // 定位到插件界面, about
                ShowSettingsUtil.getInstance().showSettingsDialog(project, GlobalDict.PLUGIN_FULL_NAME);
            }
        }).notify(ProjectUtils.getCurrProject());
    }

    private static Configurable findPreselectedByDisplayName(@org.jetbrains.annotations.NotNull String preselectedConfigurableDisplayName, @org.jetbrains.annotations.NotNull List<? extends ConfigurableGroup> groups) {
        for (ConfigurableGroup eachGroup : groups) {
            for (Configurable configurable : SearchUtil.expandGroup(eachGroup)) {
                if (preselectedConfigurableDisplayName.equals(configurable.getDisplayName())) {
                    return configurable;
                }
            }
        }
        return null;
    }

    private static PluginDownloader findAvailableUpdate(@NotNull ProgressIndicator indicator) {
        Collection<PluginDownloader> availableUpdates = findAvailableUpdates(indicator);
        return PluginUpdater.findPluginDownloader(availableUpdates);
    }

    @NotNull
    @SuppressWarnings("all")
    private static Collection<PluginDownloader> findAvailableUpdates(@NotNull ProgressIndicator indicator) {
        if (indicator == null) return new ArrayList<>();

        List<String> hosts = UpdateSettings.getInstance().getState().getPluginHosts();
        PluginConfig pluginConfig = LowCodeAppUtils.getPluginConfig();
        if (StringUtils.isNotEmpty(pluginConfig.getPluginHost()) && !hosts.contains(pluginConfig.getPluginHost())) {
            // 自动追加插件私库配置
            hosts.add(pluginConfig.getPluginHost());
        }

        PluginUpdates pluginUpdates = UpdateChecker.getInternalPluginUpdates(null, indicator).getPluginUpdates();
        Collection<PluginDownloader> availableUpdates = pluginUpdates.getAllEnabled();

        return availableUpdates;

    }

    @Nullable
    private static PluginDownloader findPluginDownloader(@NotNull Collection<PluginDownloader> availableUpdates) {
        List<PluginDownloader> downloaderList = availableUpdates.stream()
                .filter(p -> Constants.PLUGIN_ID.equals(p.getId().getIdString()))
                .toList();
        if (CollectionUtils.isNotEmpty(downloaderList) && downloaderList.size() > 1) {
            return downloaderList.stream().filter(p -> p.getPluginName().contains(GlobalDict.PLUGIN_NAME))
                    .findFirst().orElse(downloaderList.get(0));
        }
        return CollectionUtils.isNotEmpty(downloaderList) ? downloaderList.get(0) : null;
    }

    private static void showPluginUpdateDialog(@org.jetbrains.annotations.Nullable Project project, @org.jetbrains.annotations.NotNull Collection<PluginDownloader> downloaders) {
        if (CollectionUtils.isEmpty(downloaders)) return;
        if (ApplicationUtil.isBaselineVersionEgt(252)) {
            ApplicationManager.getApplication().invokeLater(() -> searchPlugins(project, GlobalDict.PLUGIN_NAME));
            return;
        }
        new PluginUpdateDialog(project, downloaders, null, false).show();
    }
}
