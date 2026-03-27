package org.qifu.devops.ide.plugins.jiracommit.configuration;

public class PluginState {
    private String checkHost = "";
    private String jiraAccountUsername = "";
    private String displayLimit = "10";


    public String getCheckHost() {
        return checkHost;
    }

    public void setCheckHost(String checkHost) {
        this.checkHost = checkHost;
    }

    public String getJiraAccountUsername() {
        return jiraAccountUsername;
    }

    public void setJiraAccountUsername(String jiraAccountUsername) {
        this.jiraAccountUsername = jiraAccountUsername;
    }

    public String getDisplayLimit() {
        return displayLimit;
    }

    public void setDisplayLimit(String displayLimit) {
        this.displayLimit = displayLimit;
    }
}
