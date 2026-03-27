package com.qihoo.finance.lowcode.common.ui.base;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Disposer;
import com.qihoo.finance.lowcode.common.configuration.UserContextPersistent;
import com.qihoo.finance.lowcode.common.configuration.UserInfoPersistentState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * BasePanel
 *
 * @author fengjinfu-jk
 * date 2023/8/18
 * @version 1.0.0
 * @apiNote MainPanel
 */
public abstract class BasePanel extends DialogWrapper implements Disposable {
    private final Component loadingPanel = LoadingPanel.createLoadingPanel();

    protected final Project project;

    public static UserInfoPersistentState.UserInfo getUserInfo() {
        return UserInfoPersistentState.getUserInfo();
    }

    public static UserInfoPersistentState getUserInfoPersistentState() {
        return UserInfoPersistentState.getInstance();
    }

    public BasePanel(@NotNull Project project) {
        super(project);
        this.project = project;
        Disposer.register(project, this);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return null;
    }

    /**
     * 实例化面板
     */
    public abstract Component createPanel();

    protected Component addLoading(Component component) {
        JPanel panel = new JPanel();
        panel.setLayout(new VFlowLayout());
        panel.add(component, VFlowLayout.TOP);
        panel.add(loadingPanel);

        return panel;
    }

    protected void showLoading(Component component) {
        loadingPanel.setVisible(true);
        if (Objects.nonNull(component)) {
            component.setVisible(false);
        }
    }

    protected void closeLoading(Component component) {
        loadingPanel.setVisible(false);
        if (Objects.nonNull(component)) {
            component.setVisible(true);
        }
    }


    @Override
    public void dispose() {
        Disposer.dispose(this);
    }
}
