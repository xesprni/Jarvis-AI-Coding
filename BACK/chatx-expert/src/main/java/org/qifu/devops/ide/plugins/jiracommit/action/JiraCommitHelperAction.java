package org.qifu.devops.ide.plugins.jiracommit.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.qihoo.finance.lowcode.common.configuration.UserInfoPersistentState;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.UserUtils;
import org.apache.commons.lang3.StringUtils;
import org.qifu.devops.ide.plugins.jiracommit.configuration.JiraCommitSettingState;
import org.qifu.devops.ide.plugins.jiracommit.ui.CommitHelperFromDialog;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import static com.intellij.openapi.ui.DialogWrapper.OK_EXIT_CODE;
import static java.util.logging.Level.INFO;

public class JiraCommitHelperAction extends AnAction {
    private final JiraCommitSettingState.JiraCommitSetting settingState = JiraCommitSettingState.getSetting();

    public JiraCommitHelperAction() {
        super(Icons.scaleToWidth(Icons.FLASH, 16));
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        //看下插件获取方式
        //使用Java
        UserInfoPersistentState.UserInfo userInfo = UserUtils.getUserInfo();
        if (Objects.nonNull(userInfo) && StringUtils.isNotEmpty(userInfo.email)) {
            settingState.account = userInfo.getUserNo();
        } else {
            settingState.account = settingState.defaultAccount;
        }

        //设置对话框
        Map<String, String> pluginParam = getJiraPluginParam(settingState);
        CommitHelperFromDialog commitHelperFromDialog = new CommitHelperFromDialog(e, pluginParam);
        commitHelperFromDialog.setCrossClosesWindow(true);
        boolean getExitCode = commitHelperFromDialog.showAndGet();

        int exitCode = commitHelperFromDialog.getExitCode();
        commitHelperFromDialog.close(OK_EXIT_CODE);
        Logger.getLogger(JiraCommitHelperAction.class.getName()).log(INFO, "dialogInfo:" + getExitCode + ";" + exitCode);
    }

    public static Map<String, String> getJiraPluginParam(JiraCommitSettingState.JiraCommitSetting settingState) {
        Map<String, String> pluginParam = new HashMap<>();

        pluginParam.put("jiraAccount", settingState.account);
        pluginParam.put("displayLimit", settingState.displayLimit);
        pluginParam.put("checkHost", settingState.checkHost);
        pluginParam.put("showTitle", String.valueOf(settingState.showTitle));
        pluginParam.put("showSprint", String.valueOf(settingState.showSprint));
        pluginParam.put("jiraInfoPosition", String.valueOf(settingState.detailPositionIndex));

        return pluginParam;
    }
}
