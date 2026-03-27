package org.qifu.devops.ide.plugins.jiracommit.configuration;

import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.qihoo.finance.lowcode.common.ui.base.VFlowLayout;
import com.qihoo.finance.lowcode.common.util.Icons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;


public class JiraCommitSettingPanel {
    @NotNull
    private JPanel mainPanel;

    @NotNull
    private JBTextField jiraAccountUsername = new JBTextField();
    @NotNull
    private JBTextField displayLimit = new JBTextField();
    @NotNull
    private JBTextField checkHost = new JBTextField();

    private JCheckBox showTitleCheckBox = new JCheckBox("插入标题");
    private JCheckBox showSprintCheckBox = new JCheckBox("插入迭代");

    private ButtonGroup detailPositionCheck = new ButtonGroup();
    private JRadioButton detailBeforeComments = new JRadioButton("任务信息置于注释前");
    private JRadioButton detailAfterComments = new JRadioButton("任务信息置于注释后");


    private ToolbarDecorator toolbar;

    @NotNull
    public JPanel getMainPanel() {
        return this.mainPanel;
    }

    @NotNull
    public String getJiraAccountUsernameText() {
        return this.jiraAccountUsername.getText();
    }

    public void setJiraAccountUsernameText(@NotNull String jiraAccountUsernameText) {
        this.jiraAccountUsername.setText(jiraAccountUsernameText);
    }

    @NotNull
    public String getDisplayLimitText() {
        return this.displayLimit.getText();
    }

    public void setDisplayLimitText(@NotNull String displayLimitText) {
        this.displayLimit.setText(displayLimitText);
    }

    @NotNull
    public String getCheckHostText() {
        return this.checkHost.getText();
    }

    public void setCheckHostText(String checkHostText) {
        this.checkHost.setText(checkHostText);
    }

    public boolean getShowTitleChecked() {
        return this.showTitleCheckBox.isSelected();
    }

    public void setShowTitleChecked(boolean isShowTitle) {
        this.showTitleCheckBox.setSelected(isShowTitle);
    }

    public boolean getShowSprintChecked() {
        return this.showSprintCheckBox.isSelected();
    }

    public void setShowSprintChecked(boolean isShowSprint) {
        this.showSprintCheckBox.setSelected(isShowSprint);
    }

    public int getJiraInfoPositionIndex() {
        if (this.detailBeforeComments.isSelected()) {
            return 1;
        }
        if (this.detailAfterComments.isSelected()) {
            return 2;
        }
        return 0;
    }

    public void setJiraInfoPosition(int jiraInfoPosition) {
        switch (jiraInfoPosition) {
            case 2:
                this.detailBeforeComments.setSelected(false);
                this.detailAfterComments.setSelected(true);
                break;
            default:
                this.detailBeforeComments.setSelected(true);
                this.detailAfterComments.setSelected(false);
        }
    }

    public JComponent getPreferredFocusedComponent() {
        return jiraAccountUsername;
    }

    public JiraCommitSettingPanel() {
        mainPanel = new JPanel(new VFlowLayout(VFlowLayout.TOP));

        // title
        JPanel titlePanel = new JPanel(new FlowLayout());
        JLabel title = new JLabel();
        title.setIcon(Icons.scaleToWidth(Icons.FLASH, 22));
        title.setFont(new Font("微软雅黑", Font.BOLD, 18));
        title.setText("代码提交-Jira配置");
        titlePanel.add(title);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));
        mainPanel.add(titlePanel);

        // form
        detailPositionCheck.add(detailBeforeComments);
        detailPositionCheck.add(detailAfterComments);
        JPanel jiraInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        jiraInfoPanel.add(showTitleCheckBox);
        jiraInfoPanel.add(showSprintCheckBox);
        JPanel jiraInfoPositionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        jiraInfoPositionPanel.add(detailBeforeComments);
        jiraInfoPositionPanel.add(detailAfterComments);

        JPanel formPanel = FormBuilder.createFormBuilder()
//                .addLabeledComponent(new JBLabel("Jira账号"), jiraAccountUsername, 1, false)
//                .addLabeledComponent(new JBLabel("Check Host"), checkHost, 1, false)
                .addLabeledComponent(new JBLabel("活跃任务列表数限制"), displayLimit, 1, false)
                .addLabeledComponent(new JBLabel("Jira信息是否插入注释"), jiraInfoPanel, false)
                .addLabeledComponent(new JBLabel("Jira信息插入注释位置"), jiraInfoPositionPanel, false)
                .getPanel();
        mainPanel.add(formPanel);
    }
}
