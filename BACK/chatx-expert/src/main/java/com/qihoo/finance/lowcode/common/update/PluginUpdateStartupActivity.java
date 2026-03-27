package com.qihoo.finance.lowcode.common.update;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import com.qihoo.finance.lowcode.common.util.ApplicationUtil;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class PluginUpdateStartupActivity implements StartupActivity.Background {
    private boolean initialize = true;

    @RequiresBackgroundThread
    public void runActivity(@NotNull Project project) {
        StartupManager.getInstance(project).runWhenProjectIsInitialized(this::scheduleRepeatedUpdateCheck);
    }

    private void scheduleRepeatedUpdateCheck() {
        Callable<?> task = () -> {
            Project project = ApplicationUtil.findCurrentProject();
            if (project != null) {
                scheduleImmediateUpdateCheck(project);
            }
            return project;
        };
        //加一个随机扰动，防止所有用户都在同一时间检查更新
        int randomDelay = (int) (Math.random() * 1000); // 0-1000秒的随机延迟
        AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(() -> {
            if (initialize) {
                try {
                    task.call();
                } catch (Exception e) {
                    // ignore
                } finally {
                    initialize = false;
                }
            } else {
                long nextJob = nextScheduleDelaySeconds();
                AppExecutorUtil.getAppScheduledExecutorService().schedule(task, nextJob + randomDelay, TimeUnit.SECONDS);
            }
        }, 5L, (60 * 60 * 24) + randomDelay, TimeUnit.SECONDS);
    }

    private void scheduleImmediateUpdateCheck(@NotNull Project project) {
        queueUpdateCheck(project);
    }

    private void queueUpdateCheck(@NotNull Project project) {
        new PluginUpdater.CheckUpdatesTask(project).queue();
    }

    public long nextScheduleDelaySeconds() {
        LocalDateTime tomorrow = LocalDate.now().plusDays(1).atStartOfDay();
        Date from = Date.from(tomorrow.atZone(ZoneId.systemDefault()).toInstant());
        long time = from.getTime() - System.currentTimeMillis();
        return time / 1000;
    }
}
