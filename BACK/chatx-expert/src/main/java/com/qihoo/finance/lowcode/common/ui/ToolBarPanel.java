package com.qihoo.finance.lowcode.common.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.toolbarLayout.ToolbarLayoutStrategy;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import com.qihoo.finance.lowcode.agent.ui.AgentNotifications;
import com.qihoo.finance.lowcode.agent.ui.AgentTaskPopup;
import com.qihoo.finance.lowcode.aiquestion.ui.QuestionPanel;
import com.qihoo.finance.lowcode.apitrack.ui.ApiMainPanel;
import com.qihoo.finance.lowcode.codereview.ui.CodeRvTreePanel;
import com.qihoo.finance.lowcode.common.configuration.UserInfoPersistentState;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.factory.ChatXToolWindowFactory;
import com.qihoo.finance.lowcode.common.listener.ChatxStartupActivity;
import com.qihoo.finance.lowcode.common.ui.base.BasePanel;
import com.qihoo.finance.lowcode.common.update.PluginUpdater;
import com.qihoo.finance.lowcode.common.util.*;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.design.ui.DatabaseTreePanelFactory;
import com.qihoo.finance.lowcode.design.ui.tree.DatabaseTreePanel;
import com.qihoo.finance.lowcode.gentracker.config.SettingsSettingStorage;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import com.qihoo.finance.lowcode.status.ChatxStatus;
import com.qihoo.finance.lowcode.status.ChatxStatusService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

/**
 * @author weiyichao
 * @date 2023-07-27
 **/
@Slf4j
public class ToolBarPanel extends BasePanel {
    private final Project project;
    @Getter
    @Setter
    private String helpDocHost;
    @Getter
    @Setter
    private String homePageHost;
    @Getter
    @Setter
    private String webHost;

    public ToolBarPanel(@NotNull Project project) {
        super(project);
        this.project = project;
    }

    public static ToolBarPanel getInstance(Project project) {
        return project.getService(ToolBarPanel.class);
    }

    public static ToolBarPanel getInstance() {
        return getInstance(ProjectUtils.getCurrProject());
    }

    @Override
    public Component createPanel() {
        JPanel toolBar = new JPanel(new BorderLayout());
        toolBar.setLayout(new BorderLayout());
        // toolBar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        toolBar.setPreferredSize(new Dimension(1300, 40));
        toolBar.setBackground(ChatXToolWindowFactory.BACKGROUND);

        JPanel userPanel = new JPanel();
        userPanel.setBorder(BorderFactory.createEmptyBorder(-5, 0, -5, 0));
        userPanel.setOpaque(false);
        Font boldFont = new Font("微软雅黑", Font.BOLD, 12);
        userPanel.setFont(boldFont);
        userPanel.setLayout(new BoxLayout(userPanel, BoxLayout.X_AXIS));

        // 用户信息
        JLabel userEmailLabel = new JLabel(getUser());
        userEmailLabel.setIcon(Icons.scaleToWidth(Icons.LOGIN_USER, 20));
        userEmailLabel.setText(String.format("<html><font><b>&nbsp;&nbsp;%s</b></font></html>", getUser()));
        userEmailLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        userPanel.add(userEmailLabel);

        toolBar.add(userPanel, BorderLayout.WEST);

        // 按键栏
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, -10));

        // menu
        JComponent toolBarMenu = createToolBar(toolBar);
        buttonPanel.add(toolBarMenu);
        buttonPanel.setBackground(ChatXToolWindowFactory.BACKGROUND);
        toolBar.add(buttonPanel, BorderLayout.EAST);
        // registerAgentNotify 暂不开启 2024/08/05
        // AgentNotifications.registerAgentNotify(toolBarMenu, Balloon.Position.below);

        return toolBar;
    }

    private JComponent createToolBar(JPanel toolBar) {
        DefaultActionGroup actionGroup = new DefaultActionGroup("MenuActionGroup", false);

        AnAction more = createMoreAction(toolBar);
        AnAction agent = createAgentNotifyAction(toolBar);
        actionGroup.addAction(agent);
        actionGroup.addAction(more);

        ActionToolbar actionToolBar = ActionManager.getInstance().createActionToolbar("MenuActionToolbar", actionGroup, true);
        actionToolBar.setTargetComponent(actionToolBar.getComponent());
        // actionToolBar.setLayoutPolicy(ActionToolbar.AUTO_LAYOUT_POLICY);
        actionToolBar.setLayoutStrategy(ToolbarLayoutStrategy.AUTOLAYOUT_STRATEGY);

        JComponent actionToolBarComponent = actionToolBar.getComponent();
        actionToolBarComponent.setBorder(null);
        actionToolBarComponent.setOpaque(false);
        return actionToolBarComponent;
    }

    private AnAction createMoreAction(JPanel toolBar) {
        JBPopupMenu moreMenu = moreMenu();
        return new AnAction("更多设置", "更多设置", Icons.scaleToWidth(Icons.MORE, 16)) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                moreMenu.show(toolBar, toolBar.getWidth() - moreMenu.getWidth(), toolBar.getHeight());
            }
        };
    }

    private AnAction createAgentNotifyAction(JPanel toolBar) {
        // AllIcons.Toolwindows.Notifications
        // AllIcons.Toolwindows.NotificationsNew
        Icon _notifyNew = Icons.scaleToWidth(AllIcons.Toolwindows.NotificationsNewImportant, 14);
        Icon _notify = Icons.scaleToWidth(AllIcons.Toolwindows.Notifications, 14);
        return new AnAction("任务列表", "任务列表", _notify) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                AgentNotifications.notifyNew = false;
                AgentTaskPopup.show();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                super.update(e);
                e.getPresentation().setIcon(AgentNotifications.notifyNew ? _notifyNew : _notify);
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }
        };
    }

    public JBPopupMenu simpleMoreMenu() {
        JBPopupMenu popupMenu = new JBPopupMenu();
        popupMenu.setBorder(BorderFactory.createLineBorder(JBColor.GRAY));
        popupMenu.getInsets().set(30, 10, 10, 10);

        popupMenu.add(Box.createVerticalStrut(6));
        popupMenu.add(lcItem());

        popupMenu.add(Box.createVerticalStrut(2));
        popupMenu.addSeparator();
        popupMenu.add(Box.createVerticalStrut(2));
        popupMenu.add(helpDocItem());
        popupMenu.add(Box.createVerticalStrut(6));

        popupMenu.add(aboutItem());
        popupMenu.add(Box.createVerticalStrut(6));

        popupMenu.add(updateVersionItem());
        popupMenu.add(Box.createVerticalStrut(2));
        popupMenu.addSeparator();
        return popupMenu;
    }

    private JBPopupMenu moreMenu() {
        JBPopupMenu popupMenu = new JBPopupMenu();
        popupMenu.setBorder(BorderFactory.createLineBorder(JBColor.GRAY));
        popupMenu.getInsets().set(30, 10, 10, 10);

        popupMenu.add(Box.createVerticalStrut(2));
        popupMenu.add(userItem());

        popupMenu.add(Box.createVerticalStrut(6));
        popupMenu.add(webPage());

        popupMenu.add(Box.createVerticalStrut(6));
        popupMenu.add(lcItem());

        popupMenu.add(Box.createVerticalStrut(2));
        popupMenu.addSeparator();

        popupMenu.add(Box.createVerticalStrut(2));
        popupMenu.add(refreshItem());

        popupMenu.add(Box.createVerticalStrut(6));
        popupMenu.add(logoutItem());

        popupMenu.add(Box.createVerticalStrut(2));
        popupMenu.addSeparator();
        popupMenu.add(Box.createVerticalStrut(2));
        popupMenu.add(helpDocItem());
        popupMenu.add(Box.createVerticalStrut(6));

        popupMenu.add(aboutItem());
        popupMenu.add(Box.createVerticalStrut(6));

        popupMenu.add(updateVersionItem());
        popupMenu.add(Box.createVerticalStrut(2));
        popupMenu.addSeparator();

        return popupMenu;
    }

    @NotNull
    private JBMenuItem helpDocItem() {
        JBMenuItem help = new JBMenuItem(new AbstractAction("帮助文档") {
            @Override
            public void actionPerformed(ActionEvent e) {
                BrowserUtil.browse(StringUtils.defaultIfBlank(helpDocHost, Constants.Url.TOOL_HELP));
            }
        });
        help.setIcon(Icons.scaleToWidth(Icons.HELP, 13));
        return help;
    }

    @NotNull
    private JBMenuItem lcItem() {
        return new JBMenuItem(new AbstractAction("低代码平台", Icons.scaleToWidth(Icons.AGENT_TASK2, 13)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                BrowserUtil.browse(StringUtils.defaultIfBlank(homePageHost, Constants.Url.TOOL_WEB));
            }
        });
    }

    @NotNull
    private JBMenuItem webPage() {
        return new JBMenuItem(new AbstractAction(GlobalDict.PLUGIN_FULL_NAME + "  网页版", Icons.scaleToWidth(Icons.LOGO_ROUND, 13)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                BrowserUtil.browse(StringUtils.defaultIfBlank(webHost, Constants.Url.TOOL_WEB));
            }
        });
    }

    @NotNull
    private JBMenuItem userItem() {
        JBMenuItem user = new JBMenuItem(new AbstractAction(getEmail()) {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 用户信息
                BrowserUtil.browse(Constants.Url.USER);
            }
        });
        user.setIcon(Icons.scaleToWidth(Icons.LOGIN_USER, 16));
        return user;
    }

    @NotNull
    private JBMenuItem updateVersionItem() {
        return new JBMenuItem(new AbstractAction("版本更新", Icons.scaleToWidth(Icons.UPDATE, 13)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 开启版本更新检测
                new PluginUpdater.CheckUpdatesTask(project, String.format("您的%s已经是最新版本, 请尽情体验 !", GlobalDict.PLUGIN_NAME)).queue();
            }
        });
    }

    @NotNull
    private JBMenuItem aboutItem() {
        return new JBMenuItem(new AbstractAction("关于", Icons.scaleToWidth(Icons.ABOUT, 13)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 定位到插件界面
                ShowSettingsUtil.getInstance().showSettingsDialog(project, MainSettingForm.DISPLAY_NAME);
            }
        });
    }

    @NotNull
    private JBMenuItem logoutItem() {
        return new JBMenuItem(new AbstractAction("退出登录", Icons.scaleToWidth(Icons.LOGOUT, 13)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                int logout = Messages.showDialog("确定退出登录吗?", "退出登录", new String[]{"是", "否"}, Messages.NO, Icons.scaleToWidth(Icons.LOGOUT, 60));
                if (logout == Messages.YES) {
                    logout();
                }
            }
        });
    }

    @NotNull
    private JBMenuItem refreshItem() {
        JBMenuItem refreshItem = new JBMenuItem("刷新");
        AbstractAction refreshAction = new AbstractAction("刷新", Icons.scaleToWidth(Icons.REFRESH, 15)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
                JButtonUtils.countdown(refreshItem, 10, false);
            }
        };
        refreshItem.setAction(refreshAction);
        return refreshItem;
    }

    public JButton refreshButton() {
        JButton refresh = JTreeToolbarUtils.createToolbarButton(Icons.scaleToWidth(Icons.REFRESH, 16), "刷新");
        refresh.setPreferredSize(new Dimension(30, 18));
        refresh.addActionListener(e -> {
            refresh();
            JButtonUtils.countdown(refresh, 10);
        });

        return refresh;
    }

    public void refresh() {
        asyncRefresh();
    }

    private void asyncRefresh() {
        TabPanel service = project.getService(TabPanel.class);
        DatabaseTreePanelFactory treeFactory = project.getService(DatabaseTreePanelFactory.class);
        DatabaseTreePanel treePanel = treeFactory.databaseTreePanel();
        CodeRvTreePanel codeRvTreePanel = project.getService(CodeRvTreePanel.class);

        new SwingWorker<>() {
            @Override
            protected Object doInBackground() {
                // 插件缓存清除
                InnerCacheUtils.refresh();
                JTabbedPane tabbedPane = service.getTabbedPane();
                int selectedIndex = tabbedPane.getSelectedIndex();
                switch (selectedIndex) {
                    case TabPanel.DB_INDEX -> refreshDbTree();
                    case TabPanel.API_INDEX -> refreshApiTree();
                    case TabPanel.CRV_INDEX -> refreshCodeReview(codeRvTreePanel);
                    case TabPanel.ASK_AI_INDEX -> refreshAskAi(codeRvTreePanel);
                    default -> {
                        refreshDbTree();
                        refreshApiTree();
                        refreshCodeReview(codeRvTreePanel);
                    }
                }

                // 加载配置
                SettingsSettingStorage.refresh();
                ChatxStartupActivity.initConfig(project);
                return null;
            }

            private void refreshAskAi(CodeRvTreePanel codeRvTreePanel) {
                UIUtil.invokeLaterIfNeeded(() -> {
                    QuestionPanel questionPanel = project.getService(QuestionPanel.class);
                    questionPanel.getAskAiDecorator().showLoading();
                });
            }

            private void refreshDbTree() {
                // 重画DB树
                DataContext dataContext = DataContext.getInstance(project);
                JTree dbTree = dataContext.getDbTree();
                if (Objects.nonNull(dbTree)) {
                    treePanel.loadDepAndDbTree(dbTree, false);
                    treePanel.updateDatabaseContext();
                }

                log.info("refreshDbTree: {}", System.identityHashCode(dbTree));
            }

            private void refreshApiTree() {
                ApiMainPanel apiMainPanel = project.getService(ApiMainPanel.class);
                apiMainPanel.reload(true);
            }

            private void refreshCodeReview(CodeRvTreePanel codeRvTreePanel) {
                // 重画代码评审树
                DataContext dataContext = DataContext.getInstance(project);
                JTree codeRvTree = dataContext.getCodeRvTree();
                if (Objects.nonNull(codeRvTree)) {
                    codeRvTreePanel.loadCodeReviewTree(codeRvTree, false);
                }

                log.info("refreshCodeReview: {}", System.identityHashCode(codeRvTree));
            }

            @Override
            protected void done() {
                // 刷新成功推送
//                NotifyUtils.notify("刷新成功", NotificationType.INFORMATION);
                syncRefresh();
                super.done();
            }
        }.execute();
    }

    private void syncRefresh() {
        TabPanel service = project.getService(TabPanel.class);
        JTabbedPane tabbedPane = service.getTabbedPane();
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex == TabPanel.ASK_AI_INDEX) {
            // 加载配置
            UIUtil.invokeLaterIfNeeded(this::refreshAi);
        }
    }

    private void refreshAi() {
        // 重画AI问答
        QuestionPanel questionPanel = project.getService(QuestionPanel.class);
        questionPanel.repaintPanel();
        log.info("refreshAi");
    }

    public String getEmail() {
        return StringUtils.defaultString(getUserInfo().email, StringUtils.EMPTY);
    }

    public String getUser() {
//        return getEmail().split("@")[0].split("-")[0];
        return getEmail().split("@")[0];
    }

    public void logout() {
        UserInfoPersistentState.UserInfo userInfo = getUserInfo();
        // 重置用户信息
        userInfo.email = "";
        userInfo.token = "";
        getUserInfoPersistentState().loadState(userInfo);
        ChatxStatusService.notifyApplication(ChatxStatus.NotSignedIn);

        // 跳转登录界面
        MainPanel mainPanel = project.getService(MainPanel.class);
        if (mainPanel != null) {
            mainPanel.logoutRepaint();
        }

        log.info(Constants.Log.USER_ACTION, "用户退出登录");
    }
}
