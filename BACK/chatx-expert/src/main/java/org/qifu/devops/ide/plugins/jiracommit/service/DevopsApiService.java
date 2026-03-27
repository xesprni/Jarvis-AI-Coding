package org.qifu.devops.ide.plugins.jiracommit.service;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.type.TypeReference;
import com.qihoo.finance.lowcode.common.util.InnerCacheUtils;
import com.qihoo.finance.lowcode.common.util.RestTemplateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.qifu.devops.ide.plugins.jiracommit.domain.entity.JiraActiveResponse;
import org.qifu.devops.ide.plugins.jiracommit.domain.entity.JiraIssueBaseDTO;
import org.qifu.devops.ide.plugins.jiracommit.util.HttpUtil;

import java.util.*;

@Slf4j
public class DevopsApiService {

    private static final TypeReference<List<JiraIssueBaseDTO>> JIRA_ISSUE = new TypeReference<>() {
    };
    private final static String DEVOPS_API_GET_ACTIVE_ISSUE = "/openapi/jira/getActiveIssueList";
    private final static String DEVOPS_API_REFRESH_ACTIVE_ISSUE = "/openapi/qoa/updateQoaCustom";

    public static List<JiraIssueBaseDTO> getActiveIssueListByParam(Map<String, String> pluginMap) {
        HttpUtil httpUtil = new HttpUtil();

        String remoteUrl = pluginMap.get("checkHost") + DEVOPS_API_GET_ACTIVE_ISSUE;
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("Authorization", "a7e3aff44f1af4f7682e6118cc14109a");
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("limit", pluginMap.get("displayLimit"));
        queryParams.put("acc", pluginMap.get("jiraAccount"));


        String cacheKey = "@JiraIssueBaseDTO_List_" + JSON.toJSON(queryParams);
        List<JiraIssueBaseDTO> cache = InnerCacheUtils.getCache(cacheKey, JIRA_ISSUE);
        if (Objects.nonNull(cache)) {
            // ignore 暂不启用缓存
//            return cache;
        }

        String sendParamsAndHeadersResult = httpUtil.commonPostMapWithoutAuthByHeaders(queryParams, remoteUrl, headerMap);
        List<JiraIssueBaseDTO> data = new ArrayList<>();
        if (StringUtils.isNotEmpty(sendParamsAndHeadersResult)) {
            JiraActiveResponse jiraActiveResponse = JSON.parseObject(sendParamsAndHeadersResult, JiraActiveResponse.class);
            data = jiraActiveResponse.getData();
        }

        // emptyList 也缓存, 短时间
        if (Objects.nonNull(data)) InnerCacheUtils.setCache(cacheKey, JSON.toJSONString(data), 300);
        return data;
    }

    public static void refreshActiveIssue(Map<String, String> pluginMap) {
        String remoteUrl = pluginMap.get("checkHost") + DEVOPS_API_REFRESH_ACTIVE_ISSUE;
        // headers
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("Authorization", "7d7dba9a53150bb59abcac4cd4a12547");
        // params
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("author", pluginMap.get("jiraAccount"));

        String result = RestTemplateUtil.post(remoteUrl, queryParams, headerMap);
        log.info("Jira-commit DevopsApiService.refreshActiveIssue, url: {}, headers: {}, params: {}, result: {}", remoteUrl, headerMap, queryParams, result);
    }

}
