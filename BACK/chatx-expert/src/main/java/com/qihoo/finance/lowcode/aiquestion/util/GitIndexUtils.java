package com.qihoo.finance.lowcode.aiquestion.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.openapi.project.Project;
import com.qihoo.finance.lowcode.aiquestion.dto.GitIndex;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.constants.ServiceErrorCode;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.enums.GitIndexStatus;
import com.qihoo.finance.lowcode.common.exception.ServiceException;
import com.qihoo.finance.lowcode.common.util.GitUtils;
import com.qihoo.finance.lowcode.common.util.LowCodeAppUtils;
import com.qihoo.finance.lowcode.common.util.RestTemplateUtil;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

/**
 * GitIndexUtil
 *
 * @author fengjinfu-jk
 * date 2024/8/5
 * @version 1.0.0
 * @apiNote GitIndexUtil
 */
@Slf4j
public class GitIndexUtils extends LowCodeAppUtils {
    private final static Map<String, GitIndex> branchIndexStatus = new HashMap<>();
    private static final TypeReference<Result<GitIndex>> GIT_INDEX_STATUS = new TypeReference<>() {
    };
    private static long lastIndexTime = 0;

    public static GitIndex gitIndexStatus(Project project) {
        return branchIndexStatus.getOrDefault(indexKey(project), GitIndex.of(GitIndexStatus.none.name()));
    }

    public static String indexKey(String gitUrl, String branchName, String revision) {
        // branchName 和 revision 不允许同时为 empty
        if (StringUtils.isBlank(gitUrl) || (StringUtils.isBlank(branchName) && StringUtils.isBlank(revision))) {
            return StringUtils.EMPTY;
        }

        return gitUrl + "_" + branchName + "_" + revision;
    }

    public static String indexKey(Project project) {
        String revision = GitUtils.getRevision(project);
        String gitUrl = GitUtils.getSSHUrl(project);
        String branchName = GitUtils.getBranchName(project);

        return indexKey(gitUrl, branchName, revision);
    }

    public static void buildGitIndex(Project project) {
        String key = indexKey(project);
        if (System.currentTimeMillis() - lastIndexTime < 2 * 60 * 1000L) {
            throw new ServiceException(ServiceErrorCode.TOO_FREQUENT_REQUEST);
        }
        lastIndexTime = System.currentTimeMillis();

        // 异步更新git索引
        branchIndexStatus.put(key, GitIndex.of(GitIndexStatus.init.name()));
        new SwingWorker<GitIndex, GitIndex>() {
            @Override
            protected GitIndex doInBackground() {
                // 触发索引构建
                return buildIndex();
            }

            @SneakyThrows
            @Override
            protected void done() {
                // 发起定时任务, 检查代码仓库索引分支构建状态
                branchIndexStatus.put(key, get());
                super.done();
            }
        }.execute();
    }

    public static void flushGitIndexStatus(Project project) {
        String key = indexKey(project);
        GitIndex gitIndex = fetchIndexStatus();
        branchIndexStatus.put(key, gitIndex);
    }

    public static GitIndex buildIndex() {
        String url = Constants.Url.POST_BUILD_INDEX;

        Map<String, Object> params = new HashMap<>();
        Project project = ProjectUtils.getCurrProject();
        params.put("repoUrl", GitUtils.getSSHUrl(project));
        params.put("revision", GitUtils.getBranchOrRevision(project));

        log.info("POST url {}, params {}", url, params);
        Result<GitIndex> result = catchException(url, () ->
                RestTemplateUtil.post(url, params, new HashMap<>(), GIT_INDEX_STATUS), "索引知识库" + ADD_NOTIFY, false);
        if (result == null || result.getData() == null) {
            return GitIndex.of(GitIndexStatus.none.name());
        }
        return result.isSuccess() ? GitIndex.of(GitIndexStatus.init.name()) : result.getData();
    }

    public static GitIndex fetchIndexStatus() {
        String url = Constants.Url.POST_INDEX_STATUS;

        Map<String, Object> params = new HashMap<>();
        Project project = ProjectUtils.getCurrProject();
        params.put("repoUrl", GitUtils.getSSHUrl(project));
        params.put("revision", GitUtils.getBranchOrRevision(project));

        Result<GitIndex> result = catchException(url, () ->
                RestTemplateUtil.post(url, params, new HashMap<>(), GIT_INDEX_STATUS), "查询知识库索引状态" + ADD_NOTIFY, false);
        if (result == null || result.getData() == null) {
            return GitIndex.of(GitIndexStatus.none.name());
        }
        return result.isSuccess() ? result.getData() : GitIndex.of(GitIndexStatus.none.name());
    }
}
