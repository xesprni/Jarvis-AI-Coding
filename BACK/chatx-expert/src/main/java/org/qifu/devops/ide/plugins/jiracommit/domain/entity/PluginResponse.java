package org.qifu.devops.ide.plugins.jiracommit.domain.entity;


import com.alibaba.fastjson.JSONObject;

public class PluginResponse {

    private JSONObject initialData;

    private JSONObject customPluginConfig;


    public JSONObject getInitialData() {
        return initialData;
    }

    public void setInitialData(JSONObject initialData) {
        this.initialData = initialData;
    }

    public JSONObject getCustomPluginConfig() {
        return customPluginConfig;
    }

    public void setCustomPluginConfig(JSONObject customPluginConfig) {
        this.customPluginConfig = customPluginConfig;
    }
}
