package com.qihoo.finance.lowcode.codereview.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.qihoo.finance.lowcode.codereview.entity.CodeRvOrgNode;
import com.qihoo.finance.lowcode.codereview.entity.CodeRvRepoNode;
import com.qihoo.finance.lowcode.codereview.entity.CodeRvSprintNode;
import com.qihoo.finance.lowcode.codereview.entity.CodeRvTaskNode;
import com.qihoo.finance.lowcode.codereview.entity.dto.*;
import com.qihoo.finance.lowcode.codereview.ui.GitAuthSettingForm;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.constants.ServiceErrorCode;
import com.qihoo.finance.lowcode.common.entity.PlaceholderNode;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.util.*;
import com.qihoo.finance.lowcode.gentracker.tool.JSON;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.qifu.devops.ide.plugins.jiracommit.domain.entity.ActiveJiraIssue;
import org.qifu.devops.ide.plugins.jiracommit.domain.entity.ActiveJiraIssueDetail;
import org.qifu.devops.ide.plugins.jiracommit.util.JiraCommitUtils;
import org.springframework.beans.BeanUtils;

import javax.swing.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.qihoo.finance.lowcode.common.constants.Constants.Headers.GITLAB_TOKEN;

/**
 * CodeRvUtils
 *
 * @author fengjinfu-jk
 * date 2023/11/2
 * @version 1.0.0
 * @apiNote CodeRvUtils
 */
public class CodeRvUtils extends LowCodeAppUtils {
    private static final TypeReference<Result<Object>> CRV_OBJ = new TypeReference<>() {
    };
    private static final TypeReference<Result<List<String>>> CRV_LIST_STRING = new TypeReference<>() {
    };
    private static final TypeReference<Result<List<CodeRvBranch>>> CRV_LIST_BRANCH = new TypeReference<>() {
    };
    private static final TypeReference<Result<List<CodeRvOrgNode>>> CRV_PERMISSION_TREE = new TypeReference<>() {
    };
    private static final TypeReference<Result<List<CodeRvTaskNode>>> CRV_TASK_NODE = new TypeReference<>() {
    };
    private static final TypeReference<Result<CodeRvTaskSaveVO>> CRV_TASK_NODE_SAVE = new TypeReference<>() {
    };
    private static final TypeReference<Result<List<CodeRvComment>>> CRV_TASK_NODE_COMMENTS = new TypeReference<>() {
    };
    private static final TypeReference<Result<List<CodeRvDiscussion>>> CRV_TASK_NODE_DISCUSSION = new TypeReference<>() {
    };
    private static final TypeReference<Result<List<CodeRvCommit>>> CRV_BRANCH_COMMITS = new TypeReference<>() {
    };
    private static final TypeReference<Result<String>> STRING = new TypeReference<>() {
    };

    //------------------------------------------------------------------------------------------------------------------

    protected static Map<String, String> gitLabHeaders() {
        Map<String, String> headers = Maps.newHashMap(APPLICATION_JSON_HEADERS);
        headers.put(GITLAB_TOKEN, getUserInfo().gitlabToken);
        return headers;
    }

    public static boolean validateGitlabToken() {
        if (StringUtils.isEmpty(getUserInfo().gitlabToken)) {
            return false;
        }

        // 使用GitlabToken发送请求测试请求是否成功
        return verifyToken(getUserInfo().gitlabToken);
    }

    public static boolean verifyToken(String gitlabToken) {
        String url = Constants.Url.GET_CRV_VERIFY_TOKEN;
        Map<String, String> headers = Maps.newHashMap(APPLICATION_JSON_HEADERS);
        headers.put(GITLAB_TOKEN, gitlabToken);

        Result<?> result = catchException(url, () -> RestTemplateUtil.get(url, new HashMap<>(), headers, CRV_OBJ), "代码评审-校验 gitlab 令牌" + ADD_NOTIFY, false);
        return result.isSuccess();
    }

    // ~ CodeRv Tree
    //------------------------------------------------------------------------------------------------------------------

    /**
     * queryCodeRvDepartments
     * header: user
     *
     * @return List<CodeRvDepartmentNode < List < CodeRvRepoNode>>>
     */
    public static List<CodeRvOrgNode> queryCodeRvDepartments() {
        String url = Constants.Url.GET_CRV_PERMISSION_TREE;
        Map<String, Object> param = new HashMap<>();

        Result<List<CodeRvOrgNode>> result = catchException(url, () -> RestTemplateUtil.get(url, param, gitLabHeaders(), CRV_PERMISSION_TREE), "代码评审-获取授权的权限树" + ADD_NOTIFY, false);
        return buildNodeTree(resultData(result, new ArrayList<>()));
    }

    private static List<CodeRvOrgNode> buildNodeTree(List<CodeRvOrgNode> depNodes) {
        for (CodeRvOrgNode depNode : depNodes) {
            List<CodeRvRepoNode> children = depNode.getChildren();
            if (CollectionUtils.isNotEmpty(children)) {
                children.forEach(depNode::add);
            } else {
                depNode.add(new PlaceholderNode("暂无数据"));
            }
        }

        return depNodes;
    }

    public static List<CodeRvTaskNode> queryCodeRvTasks(CodeRvRepoNode repoNode) {
        String projectId = repoNode.getNodeAttr().getProjectIdStr();
        String url = Constants.Url.GET_CRV_REVIEW_TASKS;
        Map<String, Object> param = new HashMap<>();
        param.put("projectId", projectId);

        Result<List<CodeRvTaskNode>> result = catchException(url, () -> RestTemplateUtil.get(url, param, gitLabHeaders(), CRV_TASK_NODE), "代码评审-获取授权的权限树" + ADD_NOTIFY, false);
        return resultData(result, new ArrayList<>());
    }

    public static List<CodeRvSprintNode> queryCodeRvTaskSprints(CodeRvRepoNode repoNode) {
        List<CodeRvTaskNode> codeRvTaskNodes = queryCodeRvTasks(repoNode);
        Map<String, List<CodeRvTaskNode>> sprintGroup = codeRvTaskNodes.stream().collect(Collectors.groupingBy(CodeRvTaskNode::getSprint));
        return sprintGroup.keySet().stream().map(sprint -> {
            CodeRvSprintNode sprintNode = new CodeRvSprintNode();
            sprintNode.setName(sprint);
            sprintGroup.get(sprint).forEach(sprintNode::add);

            return sprintNode;
        }).collect(Collectors.toList());
    }

    public static Result<CodeRvTaskSaveVO> createCodeReviewTask(CodeRvRepoNode repoNode, CodeRvSaveDTO addDTO) {
        String url = Constants.Url.POST_CRV_REVIEW_TASK_ADD;

        addDTO.setProjectId(Long.parseLong(repoNode.getNodeAttr().getProjectIdStr()));
        return catchException(url, () -> RestTemplateUtil.post(url, addDTO, gitLabHeaders(), CRV_TASK_NODE_SAVE), "代码评审-创建评审任务" + ADD_NOTIFY, false);
    }

    public static Result<CodeRvTaskSaveVO> updateCodeReviewTask(CodeRvRepoNode repoNode, CodeRvSaveDTO updateDTO) {
        String url = Constants.Url.POST_CRV_REVIEW_TASK_UPDATE;

        updateDTO.setProjectId(repoNode.getNodeAttr().getProjectId());
        return catchException(url, () -> RestTemplateUtil.post(url, updateDTO, gitLabHeaders(), CRV_TASK_NODE_SAVE), "代码评审-创建评审任务" + ADD_NOTIFY, false);
    }

    public static Result<CodeRvTaskSaveVO> deleteCodeReviewTask(CodeRvRepoNode repoNode, CodeRvTaskNode taskNode) {
        String url = Constants.Url.POST_CRV_REVIEW_TASK_DEL;
        Map<String, Object> param = new HashMap<>();
        param.put("projectId", repoNode.getNodeAttr().getProjectIdStr());
        param.put("reviewId", String.valueOf(taskNode.getReviewId()));

        return catchException(url, () -> RestTemplateUtil.post(url, param, gitLabHeaders(), CRV_TASK_NODE_SAVE), "代码评审-创建评审任务" + ADD_NOTIFY, false);
    }

    public static Result<CodeRvTaskSaveVO> finishCodeReviewTask(CodeRvRepoNode repoNode, CodeRvTaskNode taskNode) {
        String url = Constants.Url.POST_CRV_REVIEW_TASK_FINISH;
        Map<String, Object> param = new HashMap<>();
        param.put("projectId", repoNode.getNodeAttr().getProjectIdStr());
        param.put("reviewId", String.valueOf(taskNode.getReviewId()));

        return catchException(url, () -> RestTemplateUtil.post(url, param, gitLabHeaders(), CRV_TASK_NODE_SAVE), "代码评审-完成评审任务" + ADD_NOTIFY, false);
    }

    public static Result<CodeRvTaskSaveVO> reopenCodeReviewTask(CodeRvRepoNode repoNode, CodeRvTaskNode taskNode) {
        String url = Constants.Url.POST_CRV_REVIEW_TASK_REOPEN;
        Map<String, Object> param = new HashMap<>();
        param.put("projectId", repoNode.getNodeAttr().getProjectIdStr());
        param.put("reviewId", String.valueOf(taskNode.getReviewId()));

        return catchException(url, () -> RestTemplateUtil.post(url, param, gitLabHeaders(), CRV_TASK_NODE_SAVE), "代码评审-重新打开评审任务" + ADD_NOTIFY, false);
    }

    public static List<CodeRvDiscussion> queryCodeReviewTaskDiscussions(CodeRvRepoNode repoNode, CodeRvTaskNode taskNode) {
        String url = Constants.Url.GET_CRV_REVIEW_TASK_DISCUSSIONS;
        Map<String, Object> param = new HashMap<>();
        param.put("projectId", repoNode.getNodeAttr().getProjectIdStr());
        param.put("reviewId", String.valueOf(taskNode.getReviewId()));

        Result<List<CodeRvDiscussion>> result = catchException(url, () -> RestTemplateUtil.get(url, param, gitLabHeaders(), CRV_TASK_NODE_DISCUSSION), "代码评审-获取评审任务「讨论」列表" + ADD_NOTIFY, false);
        List<CodeRvDiscussion> discussions = resultData(result, new ArrayList<>());
        for (CodeRvDiscussion discussion : discussions) {
            discussion.setRepoNode(repoNode);
            discussion.setTaskNode(taskNode);
        }

        return discussions;
    }

    public static List<CodeRvComment> queryCodeReviewTaskComments(CodeRvRepoNode repoNode, CodeRvTaskNode taskNode) {
        String url = Constants.Url.GET_CRV_REVIEW_TASK_COMMENTS;
        Map<String, Object> param = new HashMap<>();
        param.put("projectId", repoNode.getNodeAttr().getProjectIdStr());
        param.put("reviewId", String.valueOf(taskNode.getReviewId()));

        Result<List<CodeRvComment>> result = catchException(url, () -> RestTemplateUtil.get(url, param, gitLabHeaders(), CRV_TASK_NODE_COMMENTS), "代码评审-获取评审任务评论列表" + ADD_NOTIFY, false);
        return resultData(result, new ArrayList<>());
    }

    public static Result<?> updateDiscussionSolveStatus(CodeRvDiscussion discussion) {
        return markDiscussion(discussion, !discussion.isResolved());
    }

    private static Result<?> markDiscussion(CodeRvDiscussion discussion, boolean resolved) {
        String url = Constants.Url.POST_CRV_DISCUSSION_MARK;
        Map<String, Object> param = new HashMap<>();
        CodeRvRepoNode repoNode = discussion.getRepoNode();
        if (Objects.nonNull(repoNode)) {
            param.put("projectId", repoNode.getNodeAttr().getProjectIdStr());
        }
        CodeRvTaskNode taskNode = discussion.getTaskNode();
        if (Objects.nonNull(taskNode)) {
            param.put("reviewId", String.valueOf(taskNode.getReviewId()));
        }

        param.put("discussionId", discussion.getId());
        param.put("resolved", resolved);

        return catchException(url, () -> RestTemplateUtil.post(url, param, gitLabHeaders(), CRV_OBJ), "代码评审-标记「讨论是否解决」" + ADD_NOTIFY, false);
    }

    public static Result<Object> depTempBranch(CodeRvRepoNode repoNode) {
        String url = Constants.Url.POST_CRV_TEMP_BRANCH_DEL;
        Map<String, Object> param = new HashMap<>();
        param.put("projectId", repoNode.getNodeAttr().getProjectIdStr());

        return catchException(url, () -> RestTemplateUtil.post(url, param, gitLabHeaders(), CRV_OBJ), "代码评审-删除过期评审任务临时分支" + ADD_NOTIFY, false);
    }

    // ~ Dialog
    //------------------------------------------------------------------------------------------------------------------
    public static List<CodeRvBranch> queryRepoBranch(CodeRvRepoNode repoNode) {
        String projectId = repoNode.getNodeAttr().getProjectIdStr();
        String url = Constants.Url.GET_CRV_REMOTE_BRANCHES;
        Map<String, Object> param = new HashMap<>();
        param.put("projectId", projectId);

        Result<List<CodeRvBranch>> result = catchException(url, () -> RestTemplateUtil.get(url, param, gitLabHeaders(), CRV_LIST_BRANCH), "代码评审-获取远程分支列表" + ADD_NOTIFY, false);
        return resultData(result, new ArrayList<>());
    }

    public static List<CodeRvCommit> queryRepoBranchCommits(CodeRvRepoNode repoNode, String branch) {
        String projectId = repoNode.getNodeAttr().getProjectIdStr();
        String url = Constants.Url.GET_CRV_COMMITS;
        Map<String, Object> param = new HashMap<>();
        param.put("projectId", projectId);
        param.put("branch", branch);

        Result<List<CodeRvCommit>> result = catchException(url, () -> RestTemplateUtil.get(url, param, gitLabHeaders(), CRV_BRANCH_COMMITS), "代码评审-获取 commit 列表" + ADD_NOTIFY, false);
        return resultData(result, new ArrayList<>());
    }

    public static List<String> queryUserSprintDemands(String release) {
        // mock
        Map<String, List<String>> releaseDemands = new HashMap<>();
        releaseDemands.put("20231020 迭代版本", Lists.newArrayList("借条ddl权限回收", "金科ddl权限回收", "小范围进行低代码分享", "低代码插件MVP版本"));
        releaseDemands.put("20231027 迭代版本", Lists.newArrayList("功能完善：支持表、字段复制", "功能完善：接口设计 与 数据库设计", "dao、mapper、service等路径，不同团队规范不一致"));
        releaseDemands.put("20231103 迭代版本", Lists.newArrayList("产品插件整合（Convert 工具类生成）", "代码水印「代码模板版本」的概念", "Convert代码块（刘政华）、mediator层", "数据库代码生成整合Controller & Facade"));
        releaseDemands.put("20231120 迭代版本", Lists.newArrayList("插件多模态发布（支持按配置发布全平台版本或IDEA版本插件）", "产品插件 - 接口同步文档 的调研&集成", "产品插件 - codereview的调研&集成", "AI问答埋点、增加数据到低代码grafana看板", "低代码需求识别后，自动生成代码，自动构建，自动部署"));

        return releaseDemands.getOrDefault(release, new ArrayList<>());
    }

    public static List<ActiveJiraIssue> queryUserActiveIssues(boolean forceRefresh) {
        List<ActiveJiraIssue> res = new ArrayList<>();
        List<ActiveJiraIssueDetail> details = JiraCommitUtils.queryUserActiveIssues(forceRefresh);
        for (ActiveJiraIssueDetail detail : details) {
            ActiveJiraIssue issue = new ActiveJiraIssue();
            BeanUtils.copyProperties(detail, issue);
            res.add(issue);
        }

        return res;
    }

    public static List<ActiveJiraIssue> queryUserActiveIssues() {
        return queryUserActiveIssues(false);
    }

    public static ActiveJiraIssueDetail queryUserActiveIssueDetail(ActiveJiraIssue issue) {
        return JiraCommitUtils.queryUserActiveIssueDetail(issue);
    }

    public static Map<String, ActiveJiraIssue> queryUserActiveTaskMap() {
        return queryUserActiveIssues().stream().collect(Collectors.toMap(ActiveJiraIssue::getIssue, Function.identity(), (k1, k2) -> k1));
    }

    public static List<String> queryCodeRvReviewers() {
        String url = Constants.Url.GET_CRV_EMPLOYEES;
        Map<String, Object> param = new HashMap<>();

        Result<List<String>> result = catchException(url, () -> RestTemplateUtil.get(url, param, gitLabHeaders(), CRV_LIST_STRING), "代码评审-获取员工列表" + ADD_NOTIFY, false);
        return resultData(result, new ArrayList<>());
    }

    public static String getTaskTips() {
        String cacheKey = "@CodeRvUtils_getTaskTips" + STRING.getType().getTypeName();
        Result<String> cache = InnerCacheUtils.getCache(cacheKey, STRING);
        if (Objects.nonNull(cache)) {
            return resultData(cache);
        }

        String url = Constants.Url.GET_CRV_TIPS;
        Result<String> result = catchException(url, () -> RestTemplateUtil.get(url, new HashMap<>(), new HashMap<>(), STRING), "查询代码评审提示信息失败" + ADD_NOTIFY, false);
        if (result.isSuccess()) InnerCacheUtils.setCache(cacheKey, JSON.toJson(result));

        return resultData(result, "代码评审是基于开发提交代码产生的代码差异（diff）进行评审\n" +
                "\n" +
                "  创建模式说明:\n" +
                "    1. 基于相同分支commit记录创建: 选择任意2个代码快照(指定分支和 commit 即确定了一个代码快照), 根据2个代码快照生成差异\n" +
                "    2. 基于任意两次commit记录创建: 选择1个分支, 根据选定的 commit 列表生成代码差异");
    }

    public static String getTokenTips() {
        String cacheKey = "@CodeRvUtils_getTokenTips" + STRING.getType().getTypeName();
        Result<String> cache = InnerCacheUtils.getCache(cacheKey, STRING);
        if (Objects.nonNull(cache)) {
            return resultData(cache);
        }

        String url = Constants.Url.GET_CRV_TOKEN_TIPS;
        Result<String> result = catchException(url, () -> RestTemplateUtil.get(url, new HashMap<>(), new HashMap<>(), STRING), "查询代码评审TOKEN提示信息失败" + ADD_NOTIFY, false);
        if (result.isSuccess()) InnerCacheUtils.setCache(cacheKey, JSON.toJson(result));

        return resultData(result, "如何获取Gitlab Token (个人访问令牌)\n" +
                "\n" +
                "  1. 进入Gitlab主页  https://gitlab.daikuan.qihoo.net\n" +
                "  2. 点击右上角头像, 选择 “编辑个人资料”\n" +
                "  3. 左侧导航菜单选择 “访问令牌”\n" +
                "  4. 填写 “令牌名称” (无要求，按需自定义即可）\n" +
                "  5. 选择 “到期时间” (建议点击×, 置为空值即可, 避免Token到期导致需要重新配置)\n" +
                "  6. 选择范围请勾选 “api”\n" +
                "  7. 最后点击“创建令牌”, 在最上方即可看到您的新 个人访问令牌, 复制并配到到插件中, 测试令牌通过后点击 Apply 保存后即可"
        );
    }

    //------------------------------------------------------------------------------------------------------------------

    protected static <R extends Result<T>, T> R catchException(String url, Supplier<R> supplier, String notifyIfFail, boolean notifyErr) {
        R r = LowCodeAppUtils.catchException(url, supplier, notifyIfFail, notifyErr);
        String errorCode = r.getErrorCode();
        // 非测试Token时Token无效
        if (ServiceErrorCode.GITLAB_NO_PRIVILEGES.getCode().equals(errorCode)) {
            Project project = ProjectUtils.getCurrProject();

            // tips & redirect
            String title = "代码评审";
            String tips = ServiceErrorCode.GITLAB_NO_PRIVILEGES.getMessage();
            SwingUtilities.invokeLater(() -> {
                NotifyUtils.build(title, tips, NotificationType.WARNING)
                        .setIcon(Icons.scaleToWidth(Icons.GIT_LAB_LIGHT, 20))
                        .addAction(new NotificationAction("请重新配置 Gitlab Token") {
                            @Override
                            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                                // 定位到 Gitlab 令牌配置
                                ShowSettingsUtil.getInstance().showSettingsDialog(project, GitAuthSettingForm.CODE_REVIEW_CONFIG);
                            }
                        }).notify(project);
            });
        }

        return r;
    }

    protected static <R extends Result<T>, T> R catchException(String url, Supplier<R> supplier, String notifyIfFail) {
        return catchException(url, supplier, notifyIfFail, true);
    }
}
