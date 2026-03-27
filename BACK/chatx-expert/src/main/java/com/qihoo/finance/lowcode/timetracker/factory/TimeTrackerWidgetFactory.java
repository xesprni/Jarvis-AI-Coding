package com.qihoo.finance.lowcode.timetracker.factory;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import com.qihoo.finance.lowcode.timetracker.service.PostTimeTrackerService;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author weiyichao
 * @date 2023-07-21
 **/
public final class TimeTrackerWidgetFactory implements StatusBarWidgetFactory {

    @Override
    public @NonNls @NotNull String getId() {
        return TimeTrackerWidget.ID;
    }

    @Override
    public @Nls @NotNull String getDisplayName() {
        return GlobalDict.TITLE_INFO;
    }

    @Override
    public boolean isAvailable(@NotNull Project project) {
        return project.getService(PostTimeTrackerService.class) != null;
    }

    @Override
    public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
        return project.getService(PostTimeTrackerService.class).widget();
    }

    @Override
    public void disposeWidget(@NotNull StatusBarWidget widget) {

    }

    @Override
    public boolean canBeEnabledOn(@NotNull StatusBar statusBar) {
        return true;
    }
}
