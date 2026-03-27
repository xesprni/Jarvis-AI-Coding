package org.qifu.devops.ide.plugins.jiracommit.listener;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.changes.ChangeListAdapter;
import com.intellij.openapi.vcs.changes.ChangeListManagerEx;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.qihoo.finance.lowcode.common.configuration.UserInfoPersistentState;
import com.qihoo.finance.lowcode.common.entity.dto.develop.DevelopTimeCount;
import com.qihoo.finance.lowcode.common.util.GitUtils;
import com.qihoo.finance.lowcode.common.util.SwingWorkerUtils;
import com.qihoo.finance.lowcode.gentracker.tool.PluginUtils;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import com.qihoo.finance.lowcode.timetracker.service.PostTimeTrackerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.qifu.devops.ide.plugins.jiracommit.domain.entity.ActiveJiraIssueDetail;
import org.qifu.devops.ide.plugins.jiracommit.util.JiraCommitUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * VcsChangeListListener
 *
 * @author fengjinfu-jk
 * date 2023/12/18
 * @version 1.0.0
 * @apiNote VcsChangeListListener
 */
@Slf4j
public class VcsChangeListListener extends ChangeListAdapter implements UserCommitListener, Disposable {

    protected final UserInfoPersistentState.UserInfo userInfo;
    private static final Map<String, String> idCommitMessageMap = new HashMap<>();

    public VcsChangeListListener() {
        // userInfo
        this.userInfo = ApplicationManager.getApplication().getService(UserInfoPersistentState.class).getState();
    }

    @Override
    public void changeListDataChanged(@NotNull ChangeList list) {
        // v 1.3.3 版本后, 不再触发 git commit 数据上报
//        log.info("VcsChangeListListener changeListDataChanged user commit msg: {}, changeName: {}, changeSize: {}", list.getComment(), list.getName(), list.getChanges().size());
//        if (list instanceof LocalChangeList) {
//            String commitMessage = list.getComment();
//            String id = ((LocalChangeList) list).getId();
//            if (StringUtils.isEmpty(commitMessage) && idCommitMessageMap.containsKey(id)) {
//                commitMessage = idCommitMessageMap.remove(id);
//            }
//
//            Project currProject = ProjectUtils.getCurrProject();
//            if (null == currProject) {
//                log.info("VcsChangeListListener changeListDataChanged currProject is null !");
//                return;
//            }
//
//            this.onCommit(currProject, StringUtils.defaultIfEmpty(list.getComment(), commitMessage));
//        }
    }

    @Override
    public void changeListsChanged() {
        Project currProject = ProjectUtils.getCurrProject();
        if (null == currProject) return;

        LocalChangeList defaultChangeList = ChangeListManagerEx.getInstance(currProject).getDefaultChangeList();
        if (StringUtils.isNotEmpty(defaultChangeList.getComment())) {
            if (idCommitMessageMap.size() > 100) idCommitMessageMap.clear();
            idCommitMessageMap.put(defaultChangeList.getId(), defaultChangeList.getComment());
        }

        super.changeListsChanged();
    }

    @Override
    public void onCommit(@NotNull Project project, @NotNull String commitMsg) {
        log.info("onCommit start: {}", commitMsg);
        // 耗时
        PostTimeTrackerService timeTrackerService = project.getService(PostTimeTrackerService.class);
        long readTimeSeconds = timeTrackerService.getReadTimeSecondsAfterLastCommit();
        long writeTimeSeconds = timeTrackerService.getWriteTimeSecondsAfterLastCommit();
        if (0 == readTimeSeconds && 0 == writeTimeSeconds) {
            log.warn("onCommit init readTimeSeconds&writeTimeSeconds is 0, 未统计到初始工时数据, 不予上报");
            return;
        }
        // 用户信息
        String userNo = userInfo.getUserNo();
        if (StringUtils.isEmpty(userNo)) {
            log.warn("onCommit execute userNo is empty, 用户未登录");
            return;
        }
        if (StringUtils.isEmpty(commitMsg)) {
            log.warn("onCommit commitMsg is empty, commitMsg为空");
            return;
        }

        SwingWorkerUtils.execute(() -> {
            // issue
            String issueKey = extractIssueKeyFromCommitMsg(commitMsg);
            // 仓库、分支、开发人、需求id、耗时、上报时间
            DevelopTimeCount timeCount = new DevelopTimeCount();
            // developer
            timeCount.setUserNo(userNo);
            // project
            timeCount.setProjectName(project.getName());
            // Git url
            String gitUrl = GitUtils.getGitUrl(project);
            timeCount.setGitUrl(gitUrl);
            // Git branch
            String gitBranch = GitUtils.getBranchName(project);
            timeCount.setGitBranch(gitBranch);
            timeCount.setIssueKey(issueKey);
            timeCount.setReadTimeSeconds(readTimeSeconds);
            timeCount.setWriteTimeSeconds(writeTimeSeconds);
            // commit message
            timeCount.setCommitMessage(commitMsg);
            // ide version
            timeCount.setIdeVersion(ApplicationInfo.getInstance().getFullVersion());
            // plugin version
            timeCount.setPluginVersion(PluginUtils.getPluginVersion());

            log.info("onCommit execute, JiraCommitUtils.postDevelopTimeCount");
            DevelopTimeCount developTimeCount = JiraCommitUtils.postDevelopTimeCount(timeCount);
            if (Objects.isNull(developTimeCount)) {
                developTimeCount = timeCount;
                log.warn("onCommit execute, JiraCommitUtils.postDevelopTimeCount developTimeCount is null");
            }
            developTimeCount.setIssueKey(issueKey);
            return developTimeCount;
        }, rs -> {
            String issueKey = rs.getIssueKey();
            if (StringUtils.isNotEmpty(issueKey)) {
                timeTrackerService.resetTimeSecondsAfterCommit();
                log.info("onCommit end, timeTrackerService.resetTimeSecondsAfterCommit()");
            } else {
                log.warn("onCommit end issueKey is empty");
            }
        });
    }

    private String extractIssueKeyFromCommitMsg(String commitMsg) {
        // 跟实际用户任务做比较
        List<ActiveJiraIssueDetail> activeIssues = JiraCommitUtils.queryUserActiveIssues(true);
        for (ActiveJiraIssueDetail activeIssue : activeIssues) {
            if (commitMsg.contains(activeIssue.getIssue())) {
                return activeIssue.getIssue();
            }
        }

        return StringUtils.EMPTY;
    }

    @Override
    public void dispose() {

    }
}
