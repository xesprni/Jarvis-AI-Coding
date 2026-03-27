package org.qifu.devops.ide.plugins.jiracommit.configuration;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SearchSettingComponent implements SearchableConfigurable {


    JPanel settingJPanel;

    @Override
    public @NotNull @NonNls String getId() {
        return null;
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "Search Setting";
    }

    @Override
    public @Nullable @NonNls String getHelpTopic() {
        return SearchableConfigurable.super.getHelpTopic();
    }

    @Override
    public @Nullable JComponent createComponent() {
        if (settingJPanel!=null){
            settingJPanel.repaint();
            return settingJPanel;
        }
        this.settingJPanel = new JiraCommitSettingPanel().getMainPanel();
        return settingJPanel;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {

    }

    @Override
    public void reset() {
        SearchableConfigurable.super.reset();
    }
}
