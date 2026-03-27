package com.qihoo.finance.lowcode.common.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.JBColor;
import com.intellij.ui.RoundedLineBorder;
import com.qihoo.finance.lowcode.aiquestion.ui.AskAiMainPanel;
import com.qihoo.finance.lowcode.aiquestion.util.ColorUtil;
import com.qihoo.finance.lowcode.common.configuration.UserInfoPersistentState;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.factory.ChatXToolWindowFactory;
import com.qihoo.finance.lowcode.common.ui.base.BasePanel;
import com.qihoo.finance.lowcode.common.util.*;
import com.qihoo.finance.lowcode.common.utils.PatternUtils;
import com.qihoo.finance.lowcode.gentracker.tool.PluginUtils;
import com.qihoo.finance.lowcode.status.ChatxStatus;
import com.qihoo.finance.lowcode.status.ChatxStatusService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * @author weiyichao
 * @date 2023-07-27
 **/
@Slf4j
public class LoginPanel extends BasePanel {

    private final Project project;

    public LoginPanel(@NotNull Project project) {
        super(project);
        this.project = project;
    }

    @Override
    public JComponent createPanel() {
        JLabel logo = new JLabel();
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        logo.setIcon(Icons.scaleToWidth(Icons.JARVIS, 180));
        logo.setBorder(BorderFactory.createEmptyBorder(200, 0, 10, 0));

        // Add welcome label at the top
        JLabel holderLabel = new JLabel(Constants.PLUGIN_SLOGAN + "  Version " + PluginUtils.getPluginVersion());
        holderLabel.setForeground(JBColor.GRAY);
        holderLabel.setHorizontalAlignment(SwingConstants.CENTER);
        holderLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        // 邮箱输入框（完整邮箱地址手动输入）
        JTextField emailField = new JTextField(20);
        emailField.setText(defaultSysUser());
        emailField.setToolTipText("请输入完整的邮箱地址，如：user@example.com");
        
        // 创建邮箱输入面板
        JPanel emailPanel = JPanelUtils.combinePanel("邮箱", null, emailField, null, new Dimension(50, -1), null);
        emailPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        JTextField captchaField = new JTextField(20);
        JButton sendButton = new JButton("获取验证码");
        sendButton.addActionListener(e -> {
            if (sendEmailCode(emailField.getText())) {
                JButtonUtils.countdown(sendButton, 60);
            }
        });
        JPanel captchaPanel = JPanelUtils.combinePanel("验证码", null, captchaField, sendButton, new Dimension(50, -1), null);
        captchaPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        JPanel inputPanelWrap = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        Border paddingBorder = BorderFactory.createEmptyBorder(5, 30, 10, 30);
        Border lineBorder = new RoundedLineBorder(ColorUtil.getBorderLine(), 20);
        inputPanelWrap.setBorder(BorderFactory.createCompoundBorder(paddingBorder, lineBorder));
        inputPanelWrap.add(emailPanel);
        inputPanelWrap.add(captchaPanel);
        inputPanelWrap.add(AskAiMainPanel.getHelpDoc());

        JButton loginButton = new JButton(" 登 录 ");
        loginButton.setHorizontalAlignment(SwingConstants.CENTER);
        loginButton.addActionListener(e -> {
            login(emailField.getText(), captchaField.getText());
        });

        JPanel mainPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        mainPanel.add(logo);
        mainPanel.add(holderLabel);
        mainPanel.add(inputPanelWrap);
        JPanel login = new JPanel();
        login.add(loginButton);
        mainPanel.add(login);

        return mainPanel;
    }

    private String defaultSysUser() {
        String userName = System.getProperty("user.name");
        if (StringUtils.isBlank(userName)) {
            return StringUtils.EMPTY;
        }
        if (StringUtils.endsWith(userName, "-jk")) {
            // 暂不处理非"-jk"用户, 无法确定是否为用户名
            return userName;
        }

        return StringUtils.EMPTY;
    }

    /**
     * 发送邮箱验证码
     * @param email 完整的邮箱地址
     * @return 是否发送成功
     */
    public boolean sendEmailCode(String email) {
        log.info(Constants.Log.USER_ACTION, "用户发送验证码");

        if (StringUtils.isEmpty(email)) {
            Messages.showMessageDialog("邮箱不能为空", "验证失败", Icons.scaleToWidth(Icons.EMAIL_FAIL, 60));
            return false;
        } else if (!validateEmail(email)) {
            return false;
        }

        // 发送邮件逻辑
        Boolean send = LowCodeAppUtils.sendLoginVerifyCode(email);
        if (send) {
            Messages.showMessageDialog("验证码已发送至邮箱, 请注意查收", "邮件发送成功", Icons.scaleToWidth(Icons.EMAIL_SUCCESS, 60));
            return true;
        } else {
            Messages.showMessageDialog("验证码发送失败", "验证失败", Icons.scaleToWidth(Icons.EMAIL_FAIL, 60));
            return false;
        }
    }

    /**
     * 验证邮箱格式
     * 前端只做基本格式校验，后缀校验由后端接口负责
     * @param email 完整的邮箱地址
     * @return 是否格式正确
     */
    private boolean validateEmail(String email) {
        // 邮箱格式验证 - 只做基本格式校验
        if (!PatternUtils.isValidEmail(email)) {
            Messages.showMessageDialog(String.format("邮箱「%s」的格式不正确", email), "验证失败", Icons.scaleToWidth(Icons.EMAIL_FAIL, 60));
            return false;
        }
        // 移除前端邮箱后缀校验，由后端接口统一处理
//        if (!email.endsWith(Constants.Email.ADDRESS)) {
//            Messages.showMessageDialog(String.format("邮箱地址只能以 %s 结尾", Constants.Email.ADDRESS), "验证失败", Icons.scaleToWidth(Icons.EMAIL_FAIL, 60));
//            return false;
//        }

        return true;
    }

    public void login(String email, String captcha) {
        if (email.isEmpty() || captcha.isEmpty()) {
            Messages.showMessageDialog("邮箱或验证码不能为空", "验证失败", Icons.scaleToWidth(Icons.ERROR, 60));
        } else {
            // 校验登录逻辑
            String token = LowCodeAppUtils.loginWithVerifyCode(email, captcha);
            if (StringUtils.isEmpty(token)) {
                Messages.showMessageDialog("登录失败, 请检查验证码", "验证失败", Icons.scaleToWidth(Icons.ERROR, 60));
                return;
            }
            UserInfoPersistentState.UserInfo userInfo = UserInfoPersistentState.getUserInfo();
            userInfo.email = email;
            userInfo.token = token;
            userInfo.mac = RestTemplateUtil.getLocalMac();
            UserInfoPersistentState.getInstance().loadState(userInfo);
            ChatxStatusService.notifyApplication(ChatxStatus.Ready);
            Messages.showMessageDialog("登录成功", "登录成功", Icons.scaleToWidth(Icons.LOGIN_USER, 60));

            // 清除登录窗体
            ChatXToolWindowFactory.removeContent(ChatXToolWindowFactory.getSelectedContent(), true);
            ChatXToolWindowFactory chatXToolWindowFactory = this.project.getService(ChatXToolWindowFactory.class);
            chatXToolWindowFactory.createToolWindowContent(this.project, ChatXToolWindowFactory.getToolWindow());
        }

        log.info(Constants.Log.USER_ACTION, "用户登录插件");
    }
}
