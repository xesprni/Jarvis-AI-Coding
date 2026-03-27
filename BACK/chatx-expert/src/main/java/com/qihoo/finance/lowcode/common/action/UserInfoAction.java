package com.qihoo.finance.lowcode.common.action;


import com.intellij.ide.BrowserUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;
import com.qifu.ui.settings.rules.RulesComponent;
import com.qihoo.finance.lowcode.aiquestion.ui.QuestionPanel;
import com.qihoo.finance.lowcode.apitrack.ui.ApiMainPanel;
import com.qihoo.finance.lowcode.codereview.ui.CodeRvTreePanel;
import com.qihoo.finance.lowcode.common.configuration.UserInfoPersistentState;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.factory.ChatXToolWindowFactory;
import com.qihoo.finance.lowcode.common.listener.ChatxStartupActivity;
import com.qihoo.finance.lowcode.common.ui.MainSettingForm;
import com.qihoo.finance.lowcode.common.update.PluginUpdater;
import com.qihoo.finance.lowcode.common.util.*;
import com.qihoo.finance.lowcode.design.ui.DatabaseTreePanelFactory;
import com.qihoo.finance.lowcode.design.ui.tree.DatabaseTreePanel;
import com.qihoo.finance.lowcode.gentracker.config.SettingsSettingStorage;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import com.qihoo.finance.lowcode.status.ChatxStatus;
import com.qihoo.finance.lowcode.status.ChatxStatusListener;
import com.qihoo.finance.lowcode.status.ChatxStatusService;
import com.qihoo.finance.lowcode.smartconversation.panels.SmartToolWindowPanel;
import com.qifu.ui.settings.SettingsOverlayPanel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Objects;

import static com.qihoo.finance.lowcode.common.ui.base.BasePanel.getUserInfo;
import static com.qihoo.finance.lowcode.common.ui.base.BasePanel.getUserInfoPersistentState;

/**
 * @author weiyichao
 * @date 2025-09-11
 **/
@Slf4j
public class UserInfoAction extends AnAction {

    private final Project project;
    @Getter
    @Setter
    private String homePageHost;
    @Getter
    @Setter
    private String webHost;

    @Getter
    @Setter
    private String helpDocHost;

    private static final String LABEL_TEMPLATE = "    ";


    public UserInfoAction(@NotNull Project project) {
        super();
        this.project = project;
        updateDisplayText();
        
        // 监听用户状态变化，自动更新显示
        MessageBusConnection connection = project.getMessageBus().connect();
        connection.subscribe(ChatxStatusListener.TOPIC, (ChatxStatusListener) (chatxStatus, paramString) -> {
            // 当用户状态变化时，更新显示文本
            SwingUtilities.invokeLater(() -> {
                updateDisplayText();
                // 强制刷新工具栏
                forceToolbarRefresh();
            });
        });
    }
    
    private void updateDisplayText() {
        String userEmail = getEmail();
        String displayText = StringUtils.isBlank(userEmail) ? "未登录" : userEmail;
        
        // 更新Action的显示文本和图标
        getTemplatePresentation().setText(displayText);
        getTemplatePresentation().setIcon(Icons.scaleToWidth(Icons.SETTING, 20));
        getTemplatePresentation().setDescription("");
    }

    /**
     * 强制刷新工具栏显示
     */
    private void forceToolbarRefresh() {
        try {
            // 获取工具窗口并刷新
            com.intellij.openapi.wm.ToolWindow toolWindow = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
                    .getToolWindow(com.qihoo.finance.lowcode.common.constants.Constants.PLUGIN_TOOL_WINDOW_ID);
            if (toolWindow != null) {
                // 重新绘制工具窗口组件来触发Action的update方法
                toolWindow.getComponent().repaint();
                toolWindow.getComponent().revalidate();
            }
        } catch (Exception e) {
            log.warn("Failed to refresh toolbar", e);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        // 每次UI更新时都刷新显示文本
        String userEmail = getEmail();
        String displayText = StringUtils.isBlank(userEmail) ? "未登录" : userEmail;
        
        e.getPresentation().setText(displayText);
        e.getPresentation().setIcon(Icons.scaleToWidth(Icons.SETTING, 20));
        e.getPresentation().setDescription("");
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // 如果你的 action 里涉及 UI 组件操作（比如 Swing、Presentation 更新），用 EDT
        return ActionUpdateThread.EDT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        String userEmail = getEmail();
        if (StringUtils.isBlank(userEmail)) {
            return;
        }
        DynamicUserActionGroup group = new DynamicUserActionGroup();

        ListPopup popup = JBPopupFactory.getInstance()
                .createActionGroupPopup(
                        null,
                        group,
                        e.getDataContext(),
                        JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                        true);

        // 设置弹出框合适的宽度和高度，确保有足够的padding空间
        popup.setMinimumSize(new Dimension(160, 0));

        // 获取触发事件的组件
        Component sourceComponent = e.getInputEvent() != null ? e.getInputEvent().getComponent() : null;
        if (sourceComponent != null) {
            try {
                // 计算弹出框位置，确保右对齐且不超出IDE边界
                Point locationOnScreen = sourceComponent.getLocationOnScreen();
                Dimension sourceSize = sourceComponent.getSize();
                
                // 获取弹出框的预估大小
                Dimension popupSize = new Dimension(160, 120); // 使用预估大小避免null
                try {
                    Dimension preferredSize = popup.getContent().getPreferredSize();
                    if (preferredSize != null) {
                        popupSize = preferredSize;
                    }
                } catch (Exception ignored) {
                    // 使用默认大小
                }
                
                // 计算右对齐位置（从按钮右侧向左展开）
                int x = locationOnScreen.x + sourceSize.width - popupSize.width;
                int y = locationOnScreen.y + sourceSize.height;
                
                // 确保不超出屏幕左边界
                if (x < 0) {
                    x = locationOnScreen.x;
                }
                
                popup.showInScreenCoordinates(sourceComponent, new Point(x, y));
            } catch (Exception ex) {
                // 如果计算位置出错，使用默认方式
                popup.showUnderneathOf(sourceComponent);
            }
        } else {
            // 备用方案：使用最佳位置
            popup.showInBestPositionFor(e.getDataContext());
        }
    }

    private  AnAction userAction() {
        String user = StringUtils.defaultString(getUserInfo().nickName, getUser());
        String displayText = StringUtils.isBlank(user) ? "用户信息" : user;
        // 添加前后空格和额外间距来增加padding效果
        String paddedText = String.format("%s%s",LABEL_TEMPLATE, displayText);
        return new AnAction(paddedText, "", Icons.scaleToWidth(Icons.LOGIN_USER, 16)) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                BrowserUtil.browse(Constants.Url.USER);
            }
        };
    }
    
    private class DynamicUserActionGroup extends DefaultActionGroup {
        @Override
        public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
            java.util.List<AnAction> actions = new java.util.ArrayList<>();
            
            // 用户信息组
            actions.add(userAction());
            actions.add(com.intellij.openapi.actionSystem.Separator.getInstance());

            // Agent 相关 配置入口
            actions.add(autoApprovalAction());
            actions.add(mcpAction());
            actions.add(rulesAction());
            actions.add(skillsAction());
//            actions.add(agentManageAction());
            actions.add(com.intellij.openapi.actionSystem.Separator.getInstance());

            // 操作组
            actions.add(helpAction());
            //actions.add(refreshAction());
            actions.add(logoutAction());
//            actions.add(autoApprovalAction());
            actions.add(com.intellij.openapi.actionSystem.Separator.getInstance());
            
            // 帮助组
            actions.add(aboutAction());
            actions.add(updateAction());
            
            return actions.toArray(new AnAction[0]);
        }
        
        @Override
        public boolean isDumbAware() {
            return true;
        }
    }

    public AnAction refreshAction() {
        String paddedText = String.format("%s%s",LABEL_TEMPLATE, "刷新");
        return new AnAction(paddedText, "", Icons.scaleToWidth(Icons.REFRESH, 16)) {
            private int countdown = 10;
            private Timer timer;
            private boolean notRefreshing = true;

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (notRefreshing) {
                    refresh();
                    startCountdown(e);
                }
                else  {
                    NotifyUtils.notify("正在刷新中，请稍后", NotificationType.WARNING);
                }

            }
            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(notRefreshing);
                if (notRefreshing && countdown == 0) {
                    e.getPresentation().setText("刷新");
                }
            }

            private void startCountdown(AnActionEvent e) {
                notRefreshing = false;
                countdown = 10;
                e.getPresentation().setEnabled(notRefreshing); // 倒计时期间不可点击
                e.getPresentation().setText("刷新 (" + countdown + "s)");

                if (timer != null) {
                    timer.stop();
                }

                timer = new Timer(1000, ev -> {
                    countdown--;
                    if (countdown > 0) {
                        e.getPresentation().setText("刷新 (" + countdown + "s)");
                    } else {
                        e.getPresentation().setText("刷新");
                        notRefreshing = true;
                        e.getPresentation().setEnabled(notRefreshing); // 倒计时结束恢复点击
                        timer.stop();
                    }
                });
                timer.start();
            }
        };
    }

    private AnAction logoutAction() {
        String paddedText = String.format("%s%s",LABEL_TEMPLATE, "退出登录");
        return new AnAction(paddedText, "", Icons.scaleToWidth(Icons.LOGOUT, 16)) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                int logout = Messages.showDialog("确定退出登录吗?", "退出登录", new String[]{"是", "否"}, Messages.NO, Icons.scaleToWidth(Icons.LOGOUT, 60));
                if (logout == Messages.YES) {
                    logout();
                }
            }
        };
    }

    private AnAction helpAction() {
        String paddedText = String.format("%s%s",LABEL_TEMPLATE, "帮助");
        return new AnAction(paddedText, "", Icons.scaleToWidth(Icons.HELP, 16)) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                BrowserUtil.browse(StringUtils.defaultIfBlank(helpDocHost, Constants.Url.TOOL_HELP));
            }
        };
    }

    private AnAction aboutAction() {
        String paddedText = String.format("%s%s",LABEL_TEMPLATE, "Jarvis 设置");
        return new AnAction(paddedText, "", Icons.scaleToWidth(Icons.SETTING2, 16)) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, MainSettingForm.DISPLAY_NAME);
            }
        };
    }

    private AnAction updateAction() {
        String paddedText = String.format("%s%s",LABEL_TEMPLATE, "版本更新");
        return new AnAction(paddedText, "", Icons.scaleToWidth(Icons.UPDATE, 16)) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                new PluginUpdater.CheckUpdatesTask(project, String.format("您的%s已经是最新版本, 请尽情体验 !", GlobalDict.PLUGIN_NAME)).queue();
            }
        };
    }

    private AnAction autoApprovalAction() {
        String paddedText = String.format("%s%s",LABEL_TEMPLATE, "自动批准");
        return new AnAction(paddedText, "", Icons.scaleToWidth(Icons.AUTO_APPROVAL, 16)) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                // 跳转到 Jarvis AI 参数配置页面
                ShowSettingsUtil.getInstance().showSettingsDialog(project, "自动批准设置");
//                log.debug("Auto approval - navigated to AI settings");
            }
        };
    }

    private AnAction mcpAction() {
        String paddedText = String.format("%s%s", LABEL_TEMPLATE, "MCP");
        return new AnAction(paddedText, "", Icons.scaleToWidth(Icons.MCP, 16)) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                showMcpOverlay(SettingsOverlayPanel.TAB_MCP);
            }
        };
    }

    private AnAction skillsAction() {
        String paddedText = String.format("%s%s", LABEL_TEMPLATE, "Skills");
        return new AnAction(paddedText, "", Icons.scaleToWidth(Icons.SKILLS, 16)) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                showMcpOverlay(SettingsOverlayPanel.TAB_SKILLS);
            }
        };
    }

    private AnAction rulesAction() {
        String paddedText = String.format("%s%s", LABEL_TEMPLATE, "Rules");
        return new AnAction(paddedText, "", Icons.scaleToWidth(Icons.ListFiles, 16)) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                RulesComponent.openRulesConfig(project);
            }
        };
    }

    private AnAction agentManageAction() {
        String paddedText = String.format("%s%s", LABEL_TEMPLATE, "智能体");
        return new AnAction(paddedText, "", IconUtil.getThemeAwareIcon(Icons.AI_AGENT_LIGHT, Icons.AI_AGENT, 16)) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                showMcpOverlay(SettingsOverlayPanel.TAB_AGENT);
            }
        };
    }

    private void showMcpOverlay(String tabId) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(Constants.PLUGIN_TOOL_WINDOW_ID);
        if (toolWindow == null) {
            return;
        }
        Content smartContent = Arrays.stream(toolWindow.getContentManager().getContents()).filter(content -> content.getComponent() instanceof SmartToolWindowPanel).findFirst().orElse(null);
        if (smartContent == null) {
            Messages.showInfoMessage(project, "请先登录后再查看 MCP 设置。", "提示");
            return;
        }
        Runnable showOverlay = () -> {
            toolWindow.getContentManager().setSelectedContent(smartContent, true);
            ((SmartToolWindowPanel) smartContent.getComponent()).showSettingsOverlay(tabId);
        };
        if (toolWindow.isVisible()) {
            showOverlay.run();
        } else {
            toolWindow.activate(showOverlay);
        }
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


    public String getEmail() {
        try {
            String email = StringUtils.defaultString(getUserInfo().email, StringUtils.EMPTY);
            log.debug("Current user email: {}", email);
            return email;
        } catch (Exception e) {
            log.warn("Failed to get user email", e);
            return StringUtils.EMPTY;
        }
    }

    public String getUser() {
        return getEmail().split("@")[0];
    }


    public void refresh() {
        asyncRefresh();
    }

    private void asyncRefresh() {
        Content content = ChatXToolWindowFactory.getSelectedContent();
        if (content == null) {
            return;
        }
        DatabaseTreePanelFactory treeFactory = project.getService(DatabaseTreePanelFactory.class);
        DatabaseTreePanel treePanel = treeFactory.databaseTreePanel();
        CodeRvTreePanel codeRvTreePanel = project.getService(CodeRvTreePanel.class);

        new SwingWorker<>() {
            @Override
            protected Object doInBackground() {
                // 插件缓存清除
                InnerCacheUtils.refresh();
                String selectedTab = content.getDisplayName();
                switch (selectedTab) {
                    case ChatXToolWindowFactory.TAB_DATABASE_NAME -> refreshDbTree();
                    case ChatXToolWindowFactory.TAB_API_NAME -> refreshApiTree();
                    case ChatXToolWindowFactory.TAB_AGENT_NAME -> refreshAskAi(codeRvTreePanel);
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
        Content content = ChatXToolWindowFactory.getSelectedContent();
        if (content == null) {
            return;
        }

        if (ChatXToolWindowFactory.TAB_AGENT_NAME.equals(content.getDisplayName())) {
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

    public void logout() {
        UserInfoPersistentState.UserInfo userInfo = getUserInfo();
        // 重置用户信息
        userInfo.email = "";
        userInfo.token = "";
        userInfo.nickName = "";
        getUserInfoPersistentState().loadState(userInfo);
        ChatxStatusService.notifyApplication(ChatxStatus.NotSignedIn);

        // 跳转登录界面
        ChatXToolWindowFactory.getToolWindow().getContentManager().removeAllContents(true);
        ChatXToolWindowFactory chatXToolWindowFactory = this.project.getService(ChatXToolWindowFactory.class);
        chatXToolWindowFactory.createToolWindowContent(this.project, ChatXToolWindowFactory.getToolWindow());

        NotifyUtils.notify("注销登录成功！", NotificationType.INFORMATION);

        log.info(Constants.Log.USER_ACTION, "用户退出登录");
    }

}
