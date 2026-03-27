package org.qifu.devops.ide.plugins.jiracommit.domain.entity;

import java.util.List;

public class JiraActiveResponse extends DevopsApiBaseResponse{

    private List<JiraIssueBaseDTO> data;

    public List<JiraIssueBaseDTO> getData() {
        return data;
    }

    public void setData(List<JiraIssueBaseDTO> data) {
        this.data = data;
    }
}
