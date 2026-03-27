package com.qihoo.finance.lowcode.codereview.ui;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.NlsContexts;
import com.qihoo.finance.lowcode.codereview.util.CodeRvUtils;
import com.qihoo.finance.lowcode.common.configuration.UserInfoPersistentState;
import com.qihoo.finance.lowcode.common.ui.base.VFlowLayout;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.JPanelUtils;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * GitAuthorizationPanel
 *
 * @author fengjinfu-jk
 * date 2023/11/6
 * @version 1.0.0
 * @apiNote GitAuthorizationPanel
 */
public class GitAuthSettingForm implements Configurable {
    private JPanel mainPanel;
    private JTextField gitLabTokenField;
    public static final String CODE_REVIEW_CONFIG = "代码评审-GitLab配置";
    protected final UserInfoPersistentState userInfoPersistentState;
    protected final UserInfoPersistentState.UserInfo userInfo;

    public GitAuthSettingForm() {
        userInfoPersistentState = ApplicationManager.getApplication().getService(UserInfoPersistentState.class);
        userInfo = userInfoPersistentState.getState();
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return CODE_REVIEW_CONFIG;
    }

    @Override
    public @Nullable JComponent createComponent() {
        initComponents();
        initComponentData();

        return mainPanel;
    }

    private void initComponents() {
        mainPanel = new JPanel(new VFlowLayout(VFlowLayout.TOP));
        // add title
        mainPanel.add(initTitlePanel());

        // create gitlab token
        // https://gitlab.daikuan.qihoo.net/-/profile/personal_access_tokens
        JButton createToken = new JButton("<html><u style=\"color: rgb(88,157,246);\">Gitlab Token (个人访问令牌)</u></html>");
        createToken.setBorderPainted(false);
        createToken.setContentAreaFilled(false);
        createToken.addActionListener(e -> BrowserUtil.browse("https://gitlab.daikuan.qihoo.net/-/profile/personal_access_tokens"));

        // test
        gitLabTokenField = new JTextField();
        JButton testToken = new JButton("测试令牌");
        testToken.addActionListener(e -> verifyToken());

        // panel
        JPanel gitlabTokenPanel = new JPanel(new BorderLayout());
        gitlabTokenPanel.add(createToken, BorderLayout.WEST);
        gitlabTokenPanel.add(gitLabTokenField, BorderLayout.CENTER);
        gitlabTokenPanel.add(testToken, BorderLayout.EAST);

        gitlabTokenPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 50));
        mainPanel.add(gitlabTokenPanel);

        // tips
        JTextArea tips = JPanelUtils.tips();
        tips.setBorder(BorderFactory.createEmptyBorder(50, 50, 0, 50));
        tips.setText(CodeRvUtils.getTokenTips());
        mainPanel.add(tips);
    }

    private void verifyToken() {
        String gitlabToken = this.gitLabTokenField.getText();
        if (CodeRvUtils.verifyToken(gitlabToken)) {
            Messages.showMessageDialog("GitLab个人令牌校验通过 !\n\n请点击右下角Apply完成设置", "校验通过", Icons.scaleToWidth(Icons.SUCCESS, 60));
        } else {
            Messages.showMessageDialog("GitLab个人令牌校验不通过 !\n\n请确保Token正确并在有效期内", "校验不通过", Icons.scaleToWidth(Icons.FAIL, 60));
        }
    }

    public JPanel initTitlePanel() {
        JPanel titlePanel = new JPanel(new FlowLayout());
        JLabel title = new JLabel();
        title.setIcon(Icons.scaleToWidth(Icons.GIT_LAB, 28));
        title.setFont(new Font("微软雅黑", Font.BOLD, 18));
        title.setText("代码评审配置");
        titlePanel.add(title);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));
        return titlePanel;
    }

    private void initComponentData() {
        // 尝试从本地用户持久化信息中加载数据
        this.gitLabTokenField.setText(userInfo.gitlabToken);
    }

    @Override
    public boolean isModified() {
        return !this.gitLabTokenField.getText().equals(userInfo.gitlabToken);
    }

    @Override
    public void apply() throws ConfigurationException {
        // 持久化到本地用户信息中
        userInfo.gitlabToken = this.gitLabTokenField.getText();
        userInfoPersistentState.loadState(userInfo);

        Project project = ProjectUtils.getCurrProject();
        CodeRvAuthPanel authPanel = project.getService(CodeRvAuthPanel.class);
        if (Objects.nonNull(authPanel)) {
            authPanel.executeVerifyTokenWorker();
        }
    }
}
