package org.qifu.devops.ide.plugins.jiracommit.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.entity.dto.develop.DevelopTimeCount;
import com.qihoo.finance.lowcode.common.entity.dto.jira.TransitionStatusInfo;
import com.qihoo.finance.lowcode.common.util.*;
import com.qihoo.finance.lowcode.gentracker.tool.JSON;
import com.qihoo.finance.lowcode.gentracker.tool.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.qifu.devops.ide.plugins.jiracommit.action.JiraCommitHelperAction;
import org.qifu.devops.ide.plugins.jiracommit.configuration.JiraCommitSettingState;
import org.qifu.devops.ide.plugins.jiracommit.domain.entity.ActiveJiraIssue;
import org.qifu.devops.ide.plugins.jiracommit.domain.entity.ActiveJiraIssueDetail;
import org.qifu.devops.ide.plugins.jiracommit.service.DevopsApiService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * JiraCommitUtils
 *
 * @author fengjinfu-jk
 * date 2023/10/30
 * @version 1.0.0
 * @apiNote JiraCommitUtils
 */
@Slf4j
public class JiraCommitUtils extends LowCodeAppUtils {
    private static Map<String, String> pluginParam = new HashMap<>();
    private static final String DEFAULT_TIPS = "温馨提示, Git提交规范:\n\n  1. 提交代码时可以选择自己名下状态是进行中的任务或bug或hotfix,\n     同时关联子系统仓库地址与当前提交仓库一致 (灵犀应用中心维护)\n  2. 需求和用户故事不可用于提交代码";
    private static final TypeReference<Result<String>> STRING = new TypeReference<>() {
    };
    private static final TypeReference<Result<List<String>>> PIPELINE_NAME = new TypeReference<>() {
    };
    private static final TypeReference<Result<List<ActiveJiraIssueDetail>>> USER_ACTIVE_TASK = new TypeReference<>() {
    };
    private static final TypeReference<Result<ActiveJiraIssueDetail>> USER_ACTIVE_TASK_DETAIL = new TypeReference<>() {
    };
    private static final TypeReference<Result<List<TransitionStatusInfo>>> USER_ISSUE_TRANSITION_STATUS = new TypeReference<>() {
    };
    private static final TypeReference<Result<DevelopTimeCount>> DEVELOP_TIME_COUNT = new TypeReference<>() {
    };

    static {
        setPluginParam(JiraCommitHelperAction.getJiraPluginParam(JiraCommitSettingState.getSetting()));
    }

    public static void setPluginParam(Map<String, String> pluginParam) {
        JiraCommitUtils.pluginParam = pluginParam;
    }

    public static String getTips() {
        String cacheKey = "@queryTips" + STRING.getType().getTypeName();
        Result<String> cache = InnerCacheUtils.getCache(cacheKey, STRING);
        if (Objects.nonNull(cache)) {
            return resultData(cache);
        }

        String url = Constants.Url.GET_COMMIT_TIPS;
        Result<String> result = catchException(url, () -> RestTemplateUtil.get(url, new HashMap<>(), new HashMap<>(), STRING), "查询提示信息失败" + ADD_NOTIFY, false);
        if (result.isSuccess()) InnerCacheUtils.setCache(cacheKey, JSON.toJson(result));

        return resultData(result, DEFAULT_TIPS);
    }

    public static void refreshActiveTask(Map<String, String> pluginMap) {
        DevopsApiService.refreshActiveIssue(pluginMap);
    }

    public static String getPipelineAuthorization() {
        String cacheKey = "@getPipelineAuthorization" + STRING.getType().getTypeName();
        Result<String> cache = InnerCacheUtils.getCache(cacheKey, STRING);
        if (Objects.nonNull(cache)) {
            return resultData(cache);
        }

        String url = Constants.Url.GET_PIPELINE_AUTHORIZATION;
        Result<String> result = catchException(url, () -> RestTemplateUtil.get(url, new HashMap<>(), new HashMap<>(), STRING), "查询PipelineAuthorization失败" + ADD_NOTIFY, false);
        if (result.isSuccess()) InnerCacheUtils.setCache(cacheKey, JSON.toJson(result));

        return resultData(result);
    }

    public static Pair<Boolean, List<String>> getPipelineConfigNames(@NotNull Project project, boolean forceRefresh) {
        String projectGitUrl = GitUtils.getGitUrl(project);
        if (!validateUrl(projectGitUrl)) {
            return Pair.of(false, Lists.newArrayList(String.format(
                    "    -无效的gitUrl: %s" +
                            "\n    请点击idea工具栏中 Git->Manage Remotes... 配置正确的gitUrl" +
                            "\n    示例: https://gitlab.daikuan.qihoo.net/360jr-base/lowcode.git", projectGitUrl)));
        }
        String user = UserUtils.getUserInfo().getUserNo();
        Map<String, Object> param = new HashMap<>();
        param.put("operator", user);
        param.put("gitRepoUrl", projectGitUrl);

        String cacheKey = "@getPipelineConfigNames" + JSON.toJson(param);
        Result<List<String>> cache = InnerCacheUtils.getCache(cacheKey, PIPELINE_NAME);
        if (Objects.nonNull(cache) && !forceRefresh) {
            return Pair.of(true, resultData(cache));
        }

        String url = Constants.Url.GET_USER_PIPELINE_AUTHORIZATION;
        Result<List<String>> result = catchException(url, () -> RestTemplateUtil.get(url, param, APPLICATION_JSON_HEADERS, PIPELINE_NAME), "查询流水线配置信息失败" + ADD_NOTIFY);
        if (result.isSuccess()) InnerCacheUtils.setCache(cacheKey, JSON.toJson(result));

        log.info("getPipelineConfigNames for project: {}, operator: {}, gitRepoUrl: {}, result: {}", project.getName(), user, projectGitUrl, JSON.toJson(result));
        return Pair.of(true, resultData(result, new ArrayList<>()));
    }

    private static boolean validateUrl(String projectGitUrl) {
        // ssh://git@gitlab.daikuan.qihoo.net:2222/360jr-base/lowcode.git
        // https://gitlab.daikuan.qihoo.net/360jr-base/lowcode.git
        // 错误示例: https://yanjiaxin-jk:yjx5barnEyyyds%@gitlab.daikuan.qihoo.net/360jr-base-comm/lingxi-qoa
        if (StringUtils.isEmpty(projectGitUrl) || !projectGitUrl.endsWith(".git")) {
            log.error("无效的gitUrl: {}, 请点击 Git->Manage Remotes... 配置正确的gitUrl, 示例: https://gitlab.daikuan.qihoo.net/360jr-base/lowcode.git", projectGitUrl);
            return false;
        }

        return true;
    }

    public static List<ActiveJiraIssueDetail> queryUserActiveIssues(boolean forceRefresh) {
        return queryUserActiveIssues(forceRefresh, org.apache.commons.lang3.StringUtils.EMPTY);
    }

    public static List<ActiveJiraIssueDetail> queryUserActiveIssues(boolean forceRefresh, String gitUrl) {
        return queryUserActiveIssues(forceRefresh, gitUrl, 0);
    }

    public static List<ActiveJiraIssueDetail> queryUserActiveIssues(boolean forceRefresh, String gitUrl, int forceLimit) {
        String url = Constants.Url.GET_USER_ACTIVE_JIRA_ISSUE;
        Map<String, Object> param = new HashMap<>();
        param.put("refresh", forceRefresh);
        param.put("limit", forceLimit > 0 ? forceLimit : pluginParam.get("displayLimit"));
        param.put("gitUrl", URLEncoder.encode(gitUrl, StandardCharsets.UTF_8));

        Result<List<ActiveJiraIssueDetail>> result = catchException(url, () -> RestTemplateUtil.get(url, param, APPLICATION_JSON_HEADERS, USER_ACTIVE_TASK), "代码评审-获取活跃的 jira 任务列表" + ADD_NOTIFY, false);
        return resultData(result, new ArrayList<>());
    }

    public static ActiveJiraIssueDetail queryUserActiveIssueDetail(ActiveJiraIssue activeJiraIssue) {
        String url = Constants.Url.GET_USER_ACTIVE_JIRA_ISSUE_DETAIL;
        Map<String, Object> param = new HashMap<>();
        param.put("issue", activeJiraIssue.getIssue());

        Result<ActiveJiraIssueDetail> result = catchException(url, () -> RestTemplateUtil.get(url, param, APPLICATION_JSON_HEADERS, USER_ACTIVE_TASK_DETAIL), "代码评审-获取活跃的 jira 任务列表" + ADD_NOTIFY, false);
        return resultData(result);
    }

    public static List<TransitionStatusInfo> queryTransitionStatus(ActiveJiraIssueDetail activeIssue) {
        String url = Constants.Url.GET_USER_ISSUE_TRANSITION_STATUS;
        Map<String, Object> param = new HashMap<>();
        param.put("issueType", activeIssue.getIssueType());
        param.put("statusName", activeIssue.getStatus());

        Result<List<TransitionStatusInfo>> result = catchException(url, () -> RestTemplateUtil.get(url, param, APPLICATION_JSON_HEADERS, USER_ISSUE_TRANSITION_STATUS), "代码评审-获取活跃的 jira 任务列表" + ADD_NOTIFY, false);
        return resultData(result);
    }

    public static DevelopTimeCount postDevelopTimeCount(DevelopTimeCount timeCount) {
        String url = Constants.Url.POST_DEVELOP_TIME_COUNT;
        Result<DevelopTimeCount> result = catchException(url, () -> RestTemplateUtil.post(url, timeCount, APPLICATION_JSON_HEADERS, DEVELOP_TIME_COUNT), "需求开发耗时统计上报" + ADD_NOTIFY, false);
        return resultData(result, new DevelopTimeCount());
    }
}
