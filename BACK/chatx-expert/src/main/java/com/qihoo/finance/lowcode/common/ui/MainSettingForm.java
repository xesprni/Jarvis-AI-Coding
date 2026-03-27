package com.qihoo.finance.lowcode.common.ui;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.JBColor;
import com.qihoo.finance.lowcode.aiquestion.ui.AskAiMainPanel;
import com.qihoo.finance.lowcode.codereview.ui.GitAuthSettingForm;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.ui.base.VFlowLayout;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.gentracker.config.SettingsSettingStorage;
import com.qihoo.finance.lowcode.gentracker.service.BaseGenerateService;
import com.qihoo.finance.lowcode.gentracker.tool.PluginUtils;
import com.qihoo.finance.lowcode.gentracker.tool.StringUtils;
import com.qihoo.finance.lowcode.gentracker.ui.setting.BaseSettings;
import com.qihoo.finance.lowcode.gentracker.ui.setting.GenOptionSettingForm;
import com.qihoo.finance.lowcode.settings.ui.AutoApprovalSettingForm;
import com.qihoo.finance.lowcode.settings.ui.CodeCompletionSettingForm;
import com.qihoo.finance.lowcode.settings.ui.DataManagementSettingForm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.qifu.devops.ide.plugins.jiracommit.configuration.JiraCommitSettingForm;

import javax.swing.*;
import java.awt.*;

/**
 * IDE插件设置界面
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
public class MainSettingForm implements Configurable, Configurable.Composite, BaseSettings, BaseGenerateService {

    private JLabel versionText;
    private JPanel mainPanel;
    private Configurable[] childConfigurableArray;
    public static final String DISPLAY_NAME = "Jarvis • AI软件研发";

    /**
     * 属性自动绑定Form在IDE2019之后的版本不可用, 需要手动实例化
     */
    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        mainPanel = new JPanel(new BorderLayout());
        JPanel dialogPanel = new JPanel(new VFlowLayout(VFlowLayout.TOP));
        versionText = new JLabel();
        versionText.setForeground(JBColor.GRAY);
        mainPanel.add(dialogPanel, BorderLayout.CENTER);
//        JProgressBar progressBar = JTreeToolbarUtils.createIndeterminateProgressBar();
//        mainPanel.add(progressBar, BorderLayout.SOUTH);

        {
            // logo
            JLabel logo = new JLabel();
            logo.setIcon(Icons.scaleToWidth(Icons.JARVIS, 200));
            logo.setFont(new Font("微软雅黑", Font.BOLD, 32));
            // logo.setText(GlobalDict.PLUGIN_NAME);
            logo.setHorizontalAlignment(SwingConstants.CENTER);
            logo.setHorizontalTextPosition(JLabel.SOUTH_EAST);
            logo.setBorder(BorderFactory.createEmptyBorder(80, 0, 20, 0));
            dialogPanel.add(logo);

            // version
            versionText.setHorizontalAlignment(SwingConstants.CENTER);
            dialogPanel.add(versionText);

            // JLabel bottom = new JLabel(Icons.scaleToWidth(Icons.DRAGON, 680));
            // bottom.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
            // dialogPanel.add(bottom);
            JPanel shortcutWrap = new JPanel(new BorderLayout());
            shortcutWrap.add(AskAiMainPanel.createShortcut(), BorderLayout.CENTER);
            shortcutWrap.setBorder(BorderFactory.createEmptyBorder(10, 120, 0, 120));
            dialogPanel.add(shortcutWrap);
            dialogPanel.add(AskAiMainPanel.getHelpDoc());

            // agreement statement
//            JTextPane agreement = new JTextPane();
//            agreement.setContentType("text/html");
//            agreement.setEditable(false);
//            agreement.setBorder(BorderFactory.createEmptyBorder(100, 60, 0, 50));
//            agreement.setPreferredSize(new Dimension(350, 300));
//            agreement.setOpaque(false);
//            agreement.setBackground(JBColor.background());
//            dialogPanel.add(agreement);
//
//            progressBar.setString("获取重要声明信息中...");
//            JTreeToolbarUtils.progressWorker(progressBar, LowCodeAppUtils::queryAgreementStatement, agreement::setText);
        }
    }

    public MainSettingForm() {
        initComponents();
        loadSetting();
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return getDisplayName();
    }

    @Override
    public Configurable @NotNull [] getConfigurables() {
        this.childConfigurableArray = new Configurable[]{
                new CodeCompletionSettingForm(),
                new GenOptionSettingForm(),
//                new TemplateSettingForm(),
//                new TypeMapperSettingForm(),
//                new ColumnConfigSettingForm(),
//                new GlobalConfigSettingForm(),
//                new GitAuthSettingForm(),
                new JiraCommitSettingForm(),
                new AutoApprovalSettingForm(),
                new DataManagementSettingForm()
        };
        this.loadChildSettingsStore();
        return this.childConfigurableArray;
    }

    private void loadChildSettingsStore() {
        // 初始装置配置信息
        for (Configurable configurable : this.childConfigurableArray) {
            if (configurable instanceof BaseSettings) {
                ((BaseSettings) configurable).loadSettingsStore();
            }
        }
    }

    @Override
    public @Nullable JComponent createComponent() {
        // 加载储存数据
        this.loadSettingsStore();
        return mainPanel;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {

    }

    /**
     * 加载配置信息
     *
     * @param settingsStorage 配置信息
     */
    @Override
    public void loadSettingsStore(SettingsSettingStorage settingsStorage) {
    }

    private void loadSetting() {
        String localVersion = PluginUtils.getPluginVersion();
        if (!StringUtils.isEmpty(localVersion)) {
            this.versionText.setText(Constants.PLUGIN_SLOGAN + "  Version " + localVersion);
        }
    }
}
