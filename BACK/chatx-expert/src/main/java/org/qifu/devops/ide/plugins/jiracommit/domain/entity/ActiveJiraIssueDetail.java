package org.qifu.devops.ide.plugins.jiracommit.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ActiveJiraIssueDetail
 *
 * @author fengjinfu-jk
 * date 2023/11/6
 * @version 1.0.0
 * @apiNote ActiveJitaTask
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ActiveJiraIssueDetail extends ActiveJiraIssue {
    private String requestId;
    private String requestTitle;

    @Override
    public String toString() {
        return String.format("%s %s %s 【%s】", getIssue(), getIssueTitle(), getSprint(), getStatus());
    }

}
