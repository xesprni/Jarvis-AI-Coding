package org.qifu.devops.ide.plugins.jiracommit.configuration;

import com.intellij.openapi.options.Configurable;
import com.qihoo.finance.lowcode.common.configuration.UserInfoPersistentState;
import com.qihoo.finance.lowcode.common.util.UserUtils;
import org.apache.commons.lang3.StringUtils;
import org.qifu.devops.ide.plugins.jiracommit.action.JiraCommitHelperAction;
import org.qifu.devops.ide.plugins.jiracommit.util.JiraCommitUtils;

import javax.swing.*;
import java.util.Map;
import java.util.Objects;

public class JiraCommitSettingForm implements Configurable {
    private JiraCommitSettingPanel jiraCommitSettingPanel;
    private final JiraCommitSettingState.JiraCommitSetting settingState;

    public JiraCommitSettingForm() {
        this.settingState = JiraCommitSettingState.getSetting();
    }

    @Override
    public JComponent createComponent() {
        jiraCommitSettingPanel = new JiraCommitSettingPanel();
        return jiraCommitSettingPanel.getMainPanel();
    }

    public boolean isModified() {
        boolean modified = !jiraCommitSettingPanel.getJiraAccountUsernameText().equals(settingState.account);
        modified |= !jiraCommitSettingPanel.getCheckHostText().equals(settingState.checkHost);
        modified |= !jiraCommitSettingPanel.getDisplayLimitText().equals(settingState.displayLimit);
        modified |= jiraCommitSettingPanel.getShowSprintChecked() != settingState.showSprint;
        modified |= jiraCommitSettingPanel.getShowTitleChecked() != settingState.showTitle;
        modified |= jiraCommitSettingPanel.getJiraInfoPositionIndex() != settingState.detailPositionIndex;
        return modified;
    }

    @Override
    public void apply() {
        settingState.account = jiraCommitSettingPanel.getJiraAccountUsernameText();
        settingState.checkHost = jiraCommitSettingPanel.getCheckHostText();
        settingState.displayLimit = jiraCommitSettingPanel.getDisplayLimitText();
        settingState.showSprint = jiraCommitSettingPanel.getShowSprintChecked();
        settingState.showTitle = jiraCommitSettingPanel.getShowTitleChecked();
        settingState.detailPositionIndex = jiraCommitSettingPanel.getJiraInfoPositionIndex();

        Map<String, String> jiraPluginParam = JiraCommitHelperAction.getJiraPluginParam(settingState);
        JiraCommitUtils.setPluginParam(jiraPluginParam);
    }

    @Override
    public String getDisplayName() {
        return "代码提交-Jira配置";
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return jiraCommitSettingPanel.getPreferredFocusedComponent();
    }

    @Override
    public void reset() {
        jiraCommitSettingPanel.setCheckHostText(settingState.checkHost);
        jiraCommitSettingPanel.setDisplayLimitText(settingState.displayLimit);

        UserInfoPersistentState.UserInfo userInfo = UserUtils.getUserInfo();
        if (Objects.nonNull(userInfo) && StringUtils.isNotEmpty(userInfo.email)) {
            settingState.account = userInfo.getUserNo();
        } else {
            settingState.account = settingState.defaultAccount;
        }
        jiraCommitSettingPanel.setJiraAccountUsernameText(settingState.account);
        jiraCommitSettingPanel.setShowTitleChecked(settingState.showTitle);
        jiraCommitSettingPanel.setShowSprintChecked(settingState.showSprint);
        jiraCommitSettingPanel.setJiraInfoPosition(settingState.detailPositionIndex);

    }

    @Override
    public void disposeUIResources() {
        jiraCommitSettingPanel = null;
    }
}
