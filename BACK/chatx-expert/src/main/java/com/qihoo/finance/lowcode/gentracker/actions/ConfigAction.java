package com.qihoo.finance.lowcode.gentracker.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.qihoo.finance.lowcode.gentracker.ui.dialog.ConfigTableDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 表配置菜单
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
public class ConfigAction extends AnAction {
    /**
     * 构造方法
     *
     * @param text 菜单名称
     */
    ConfigAction(@Nullable String text) {
        super(text);
    }

    /**
     * 处理方法
     *
     * @param event 事件对象
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }
        new ConfigTableDialog(project).show();
    }
}
