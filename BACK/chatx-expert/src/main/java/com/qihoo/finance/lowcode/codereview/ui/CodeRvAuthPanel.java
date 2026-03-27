package com.qihoo.finance.lowcode.codereview.ui;

import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.qihoo.finance.lowcode.codereview.util.CodeRvUtils;
import com.qihoo.finance.lowcode.common.constants.ServiceErrorCode;
import com.qihoo.finance.lowcode.common.ui.TabPanel;
import com.qihoo.finance.lowcode.common.ui.base.BasePanel;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * CodeRvAuthPanel
 *
 * @author fengjinfu-jk
 * date 2023/11/2
 * @version 1.0.0
 * @apiNote CodeRvTreePanel
 */
@Slf4j
public class CodeRvAuthPanel extends BasePanel {
    private final Project project;

    public CodeRvAuthPanel(@NotNull Project project) {
        super(project);
        this.project = project;
    }

    public void executeVerifyTokenWorker() {
        TabPanel tabPanel = project.getService(TabPanel.class);
        new SwingWorker<Boolean, Boolean>() {
            @Override
            protected Boolean doInBackground() {
                return CodeRvUtils.validateGitlabToken();
            }

            @Override
            protected void done() {
                SwingUtilities.invokeLater(() -> {
                    Boolean verify = false;
                    try {
                        verify = get();
                    } catch (Exception e) {
                        // ignore
                    }

                    log.info("user gitlab token verify {}", verify);
                    if (verify) {
                        tabPanel.reloadCodeReviewTab();
                    } else {
                        tabPanel.reloadCodeReviewAuthTab();
                    }

                    super.done();
                });
            }
        }.execute();
    }

    @Override
    public JComponent createPanel() {

        // 88,157,246
        String btnTxt = String.format("<html>%s%s&nbsp;&nbsp;<u style=\"color: rgb(88,157,246);\">Gitlab Token</u></html>", ServiceErrorCode.GITLAB_NO_PRIVILEGES.getMessage(), ", 请重新配置");
        JButton settingBtn = new JButton();
        settingBtn.setText(btnTxt);
        settingBtn.setIcon(Icons.scaleToWidth(Icons.GIT_LAB, 36));
        settingBtn.setBorderPainted(false);
        settingBtn.setContentAreaFilled(false);
        settingBtn.addActionListener(e -> {
            // 定位到 Gitlab 令牌配置
            ShowSettingsUtil.getInstance().showSettingsDialog(ProjectUtils.getCurrProject(), GitAuthSettingForm.CODE_REVIEW_CONFIG);
        });

        JPanel settingPanel = new JPanel(new BorderLayout());
        settingPanel.add(settingBtn, BorderLayout.CENTER);

        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 15;
        gbc.insets = JBUI.insetsTop(10);
        contentPanel.add(settingPanel, gbc);

        JBScrollPane scrollPane = new JBScrollPane(contentPanel);
        scrollPane.setBorder(null);
        return scrollPane;
    }
}
