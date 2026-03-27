package org.qifu.devops.ide.plugins.jiracommit.domain.entity;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * ActiveJiraIssue
 *
 * @author fengjinfu-jk
 * date 2023/11/6
 * @version 1.0.0
 * @apiNote ActiveJitaTask
 */
@Data
public class ActiveJiraIssue {
    private String issue;
    private String issueTitle;
    /**
     * 任务类型
     */
    private String issueType;
    /**
     * status
     */
    private String status;
    /**
     * isAvailable
     */
    private boolean available;
    /**
     * 代码评审人
     */
    private String reviewer;

    /**
     * 迭代
     */
    private String sprint;
    private String gitRepoUrl;
    private String tips;
    private boolean matchGitRepo = true;
    private String unAvailableTips;
    public String getSimpleGitRepo() {
        if (StringUtils.isBlank(gitRepoUrl)) return StringUtils.EMPTY;
        return StringUtils.substringAfter(StringUtils.substringAfter(gitRepoUrl, "net"), "/");
    }

    @Override
    public String toString() {
        return String.format("%s %s %s", StringUtils.defaultString(getIssue()), StringUtils.defaultString(getIssueTitle()), StringUtils.defaultString(getSprint()));
    }
}
