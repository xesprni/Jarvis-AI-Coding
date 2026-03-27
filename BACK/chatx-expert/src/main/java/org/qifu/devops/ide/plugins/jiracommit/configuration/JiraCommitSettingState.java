package org.qifu.devops.ide.plugins.jiracommit.configuration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

@State(name = "JiraCommitSetting", storages = @Storage("jiraCommitSetting.xml"))
public class JiraCommitSettingState implements PersistentStateComponent<JiraCommitSettingState.JiraCommitSetting> {
    private JiraCommitSetting jiraCommitSetting = new JiraCommitSetting();

    @Nullable
    @Override
    public JiraCommitSetting getState() {
        return jiraCommitSetting;
    }

    @Override
    public void loadState(@NotNull JiraCommitSetting state) {
        jiraCommitSetting = state;
    }

    @Data
    public static class JiraCommitSetting {
        public String checkHost = "http://10.185.164.126";
        public String defaultAccount = "请输入Jira账号";
        public String account = "请输入Jira账号";
        public String displayLimit = "30";
        public boolean showTitle = false;
        public boolean showSprint = false;
        public int detailPositionIndex = 2;
    }

    private static JiraCommitSettingState settingState;

    public static JiraCommitSettingState getInstance() {
        try {
            JiraCommitSettingState service = ApplicationManager.getApplication().getService(JiraCommitSettingState.class);
            settingState = service;
            return service;
        } catch (Exception e) {
            return settingState;
        }
    }

    public static JiraCommitSetting getSetting() {
        return getInstance().getState();
    }
}
