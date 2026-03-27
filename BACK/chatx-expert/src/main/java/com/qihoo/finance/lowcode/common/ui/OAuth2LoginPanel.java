package com.qihoo.finance.lowcode.common.ui;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.JBColor;
import com.qihoo.finance.lowcode.common.configuration.UserInfoPersistentState;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.factory.ChatXToolWindowFactory;
import com.qihoo.finance.lowcode.common.oauth2.OAuthCallbackServer;
import com.qihoo.finance.lowcode.common.oauth2.OAuthClient;
import com.qihoo.finance.lowcode.common.oauth2.dto.OAuth2UserInfoDTO;
import com.qihoo.finance.lowcode.common.ui.base.BasePanel;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.NotifyUtils;
import com.qihoo.finance.lowcode.common.util.RestTemplateUtil;
import com.qihoo.finance.lowcode.status.ChatxStatus;
import com.qihoo.finance.lowcode.status.ChatxStatusService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author weiyichao
 * @date 2023-07-27
 **/
@Slf4j
public class OAuth2LoginPanel extends BasePanel {

    private final Project project;
    private Notification loginProgressNotification;
    private Timer loginTimeoutTimer;
    private final AtomicReference<OAuthCallbackServer> serverRef = new AtomicReference<>();
    private JButton loginButton;

    public OAuth2LoginPanel(@NotNull Project project) {
        super(project);
        this.project = project;
        setTitle("Jarvis AI 登录");
    }

    @Override
    public JComponent createPanel() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        // 标题面板 - logo和标题在同一行
        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 0));
        titlePanel.setPreferredSize(new Dimension(0, 40)); // 设置固定高度
        titlePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel logo = new JLabel();
        logo.setIcon(Icons.scaleToWidth(Icons.LOGO_ROUND13, 32));
        titlePanel.add(logo);

        JLabel title = new JLabel("与 Jarvis AI 协作");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setForeground(JBColor.foreground());
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        titlePanel.add(title);
        centerPanel.add(titlePanel);
        centerPanel.add(Box.createVerticalStrut(20));

        // 副标题
        JLabel subTitle1 = new JLabel("基于奇富大模型结合私有金融场景知识，", SwingConstants.CENTER);
        title.setForeground(JBColor.foreground());
        subTitle1.setFont(new Font("PingFang SC", Font.PLAIN, 16));
        subTitle1.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(subTitle1);
        centerPanel.add(Box.createVerticalStrut(8));

        JLabel subTitle2 = new JLabel("赋能\"10x\"生成力，助力业务增长", SwingConstants.CENTER);
        subTitle2.setForeground(JBColor.foreground());
        subTitle2.setFont(new Font("PingFang SC", Font.PLAIN, 16));
        subTitle2.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(subTitle2);
        centerPanel.add(Box.createVerticalStrut(30));

        // 登录按钮
        loginButton = getJButton();
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(loginButton);
        centerPanel.add(buttonPanel);
        centerPanel.add(Box.createVerticalStrut(20));
        wrapper.add(centerPanel, new GridBagConstraints());
        return wrapper;
    }

    private @NotNull JButton getJButton() {
        JButton button = new JButton("登录") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 根据按钮状态设置背景色
                Boolean isHovered = (Boolean) getClientProperty("isHovered");
                Color backgroundColor;
                if (!isEnabled()) {
                    backgroundColor = new Color(0xCCCCCC); // 禁用状态的灰色
                } else if (Boolean.TRUE.equals(isHovered)) {
                    backgroundColor = new Color(0x45A049); // 悬浮时的深绿色
                } else {
                    backgroundColor = new Color(0x4CAF50); // 默认绿色
                }
                
                g2.setColor(backgroundColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                // 文字颜色
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                int stringWidth = fm.stringWidth(getText());
                int stringAscent = fm.getAscent();
                int x = (getWidth() - stringWidth) / 2;
                int y = (getHeight() + stringAscent) / 2 - 2;
                g2.drawString(getText(), x, y);

                g2.dispose();
            }
        };

        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(200, 44));
        button.setMaximumSize(new Dimension(200, 44));
        
        // 设置鼠标光标为手型
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // 添加鼠标悬浮监听器
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                JButton btn = (JButton) e.getSource();
                if (btn.isEnabled()) {
                    btn.putClientProperty("isHovered", true);
                    e.getComponent().repaint();
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                ((JButton) e.getSource()).putClientProperty("isHovered", false);
                e.getComponent().repaint();
            }
        });
        
        button.addActionListener(e -> login());
        return button;
    }


    public void login() {
        log.info(Constants.Log.USER_ACTION, "用户登录插件");
        
        // 禁用登录按钮，防止重复点击
        setLoginButtonEnabled(false);
        
        // 创建带动画效果的持久登录通知
        createLoginProgressNotification();
        
        OAuthCallbackServer server = new OAuthCallbackServer();
        serverRef.set(server);
        int port = server.getRandomPort();
        
        // 设置10秒超时
        setupLoginTimeout(server);
        
        server.start(port, auth2 -> {
            try{
                // 1. 获取 AccessToken
                String accessToken = OAuthClient.getAccessToken(auth2);

                // 2. 获取用户信息
                OAuth2UserInfoDTO oAuth2UserInfo = OAuthClient.getUserInfo(accessToken,RestTemplateUtil.getLocalMac());

                if (oAuth2UserInfo != null) {
                    UserInfoPersistentState.UserInfo userInfo = UserInfoPersistentState.getUserInfo();
                    userInfo.email = oAuth2UserInfo.getEmail();
                    userInfo.token = accessToken;
                    userInfo.nickName = oAuth2UserInfo.getNickName();
                    userInfo.mac = RestTemplateUtil.getLocalMac();
                    UserInfoPersistentState.getInstance().loadState(userInfo);
                    
                    // 清理定时器和进度通知
                    cleanupLoginProcess();
                    
                    NotifyUtils.notify("登录成功！", NotificationType.INFORMATION);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        ChatxStatusService.notifyApplication(ChatxStatus.Ready);
                        // 清除登录窗体
                        ChatXToolWindowFactory.removeContent(ChatXToolWindowFactory.getSelectedContent(), true);
                        ChatXToolWindowFactory chatXToolWindowFactory = this.project.getService(ChatXToolWindowFactory.class);
                        chatXToolWindowFactory.createToolWindowContent(this.project, ChatXToolWindowFactory.getToolWindow());
                        
                        // 自动唤醒IDEA编辑器
                        bringIdeToFront();
                    });
                }
                else {
                    // 清理定时器和进度通知
                    cleanupLoginProcess();
                    NotifyUtils.notify("登录失败，请重试", NotificationType.ERROR);
                }
            }
            catch (Exception e) {
                log.error("登录过程中发生异常", e);
                // 清理定时器和进度通知
                cleanupLoginProcess();
                NotifyUtils.notify("登录过程中发生错误：" + e.getMessage(), NotificationType.ERROR);
            }
            finally {
                // 3. 关闭服务器
                server.stop();
                serverRef.set(null);
            }
        });
        
        try {
            OAuthClient.startLogin(port);
        } catch (Exception e) {
            log.error("启动登录流程失败", e);
            // 清理定时器和进度通知
            cleanupLoginProcess();
            NotifyUtils.notify("启动登录失败：" + e.getMessage(), NotificationType.ERROR);
            // 如果启动失败，需要停止服务器
            server.stop();
            serverRef.set(null);
        }
    }

    /**
     * 创建带动画效果的持久登录通知
     */
    private void createLoginProgressNotification() {
        // 取消之前的通知（如果存在）
        if (loginProgressNotification != null) {
            loginProgressNotification.expire();
        }
        
        // 创建持久通知
        loginProgressNotification = NotificationGroupManager.getInstance()
                .getNotificationGroup("TrackNotify")
                .createNotification("正在登录...", NotificationType.INFORMATION);
        
        // 设置为不自动过期
        loginProgressNotification.setImportant(true);
        
        // 显示通知
        loginProgressNotification.notify(project);
        
        // 启动动画效果
        startProgressAnimation();
    }
    
    /**
     * 启动进度条动画效果
     */
    private void startProgressAnimation() {
        Timer animationTimer = new Timer(true);
        final AtomicReference<Integer> dots = new AtomicReference<>(0);
        
        animationTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (loginProgressNotification == null || loginProgressNotification.isExpired()) {
                    animationTimer.cancel();
                    return;
                }
                
                ApplicationManager.getApplication().invokeLater(() -> {
                    int dotCount = dots.get();
                    StringBuilder message = new StringBuilder("正在登录");
                    for (int i = 0; i <= dotCount; i++) {
                        message.append(".");
                    }
                    
                    // 更新通知内容
                    loginProgressNotification.setContent(message.toString());
                    
                    // 循环显示1-3个点
                    dots.set((dotCount + 1) % 4);
                });
            }
        }, 0, 500); // 每500毫秒更新一次
    }
    
    /**
     * 停止登录进度通知
     */
    private void stopLoginProgressNotification() {
        if (loginProgressNotification != null) {
            loginProgressNotification.expire();
            loginProgressNotification = null;
        }
    }
    
    /**
     * 设置登录超时机制
     */
    private void setupLoginTimeout(OAuthCallbackServer server) {
        // 取消之前的定时器
        if (loginTimeoutTimer != null) {
            loginTimeoutTimer.cancel();
        }
        
        loginTimeoutTimer = new Timer(true);
        loginTimeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                ApplicationManager.getApplication().invokeLater(() -> {
                    log.warn("登录超时，自动停止服务器");
                    
                    // 停止服务器
                    server.stop();
                    serverRef.set(null);
                    
                    // 清理登录过程中的资源（包括恢复按钮状态）
                    cleanupLoginProcess();
                    
                    // 显示超时通知
                    NotifyUtils.notify("登录超时，请重试", NotificationType.ERROR);
                });
            }
        }, 10000 * 6); // 10秒超时
    }
    
    /**
     * 清理登录过程中的资源
     */
    private void cleanupLoginProcess() {
        // 停止进度通知
        stopLoginProgressNotification();
        
        // 取消超时定时器
        if (loginTimeoutTimer != null) {
            loginTimeoutTimer.cancel();
            loginTimeoutTimer = null;
        }
        
        // 重新启用登录按钮
        setLoginButtonEnabled(true);
    }
    
    /**
     * 设置登录按钮的启用状态
     */
    private void setLoginButtonEnabled(boolean enabled) {
        if (loginButton != null) {
            ApplicationManager.getApplication().invokeLater(() -> {
                loginButton.setEnabled(enabled);
                loginButton.setCursor(enabled ? 
                    Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : 
                    Cursor.getDefaultCursor());
                loginButton.repaint();
            });
        }
    }

    /**
     * 自动唤醒IDEA编辑器，使其获得焦点并置于前台
     */
    private void bringIdeToFront() {
        try {
            // 获取IDEA主窗口
            Window ideFrame = WindowManager.getInstance().getFrame(project);
            if (ideFrame != null) {
                // 将窗口置于前台
                ideFrame.toFront();
                ideFrame.requestFocus();
                
                // 使用IdeFocusManager确保焦点正确转移
                IdeFocusManager focusManager = IdeFocusManager.getInstance(project);
                focusManager.requestFocus(ideFrame, true);
                
                log.info("成功唤醒IDEA编辑器");
            } else {
                log.warn("无法获取IDEA主窗口");
            }
        } catch (Exception e) {
            log.error("唤醒IDEA编辑器失败", e);
        }
    }


    static class CardPanel extends JPanel {
        public CardPanel() {
            // setBackground(new Color(0x2B2B2B));
            setBorder(new EmptyBorder(20, 40, 20, 40));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
        }
    }
}
