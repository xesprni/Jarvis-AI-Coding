package com.qihoo.finance.lowcode.common.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.qihoo.finance.lowcode.common.configuration.UserInfoPersistentState;
import com.qihoo.finance.lowcode.common.ui.base.LoadingPanel;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * MainPanel
 *
 * @author fengjinfu-jk
 * date 2023/8/18
 * @version 1.0.0
 * @apiNote MainPanel
 */
public class MainPanel extends JFrame {
    protected final UserInfoPersistentState userInfoPersistentState;
    protected final UserInfoPersistentState.UserInfo userInfo;

    private final ToolBarPanel toolBarPanel;
    private final JPanel mainPanel;
    private final LoginPanel loginPanel;
    private final TabPanel tabbedPanel;
    private final Component loadingPanel;

    public MainPanel(@NotNull Project project) {
        userInfoPersistentState = ApplicationManager.getApplication().getService(UserInfoPersistentState.class);
        userInfo = userInfoPersistentState.getState();

        this.toolBarPanel = project.getService(ToolBarPanel.class);
        this.loginPanel = project.getService(LoginPanel.class);
        this.tabbedPanel = project.getService(TabPanel.class);

        this.loadingPanel = LoadingPanel.createLoadingPanel();
        this.mainPanel = new JPanel(new BorderLayout());
    }

    public JComponent createPanel() {
        if (isLoggedIn()) {
            loginRepaint();
        } else {
            logoutRepaint();
        }

        return mainPanel;
    }

    public boolean isLoggedIn() {
        return userInfo != null && StringUtils.isNotEmpty(userInfo.email) && StringUtils.isNotEmpty(userInfo.token);
    }

    public void logoutRepaint() {
        mainPanel.removeAll();
        mainPanel.revalidate();
        repaintLoginForm();
        mainPanel.repaint();
    }

    public void loginRepaint() {
        mainPanel.removeAll();
        mainPanel.revalidate();
        repaintMainForm();
        mainPanel.repaint();
    }

    protected void repaintLoginForm() {
        mainPanel.add(loginPanel.createPanel(), BorderLayout.CENTER);
    }

    protected void repaintMainForm() {
        //mainPanel.add(toolBarPanel.createPanel(), BorderLayout.NORTH);
        mainPanel.add(tabbedPanel.createPanel(), BorderLayout.CENTER);
    }

    public void refresh() {
        loginRepaint();
    }

    public void showLoading() {
        mainPanel.removeAll();
        mainPanel.add(loadingPanel, BorderLayout.NORTH);
        mainPanel.repaint();
    }

    public void closeLoading() {
        createPanel();
    }
}
