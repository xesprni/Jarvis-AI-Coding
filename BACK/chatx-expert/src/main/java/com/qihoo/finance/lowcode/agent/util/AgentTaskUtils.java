package com.qihoo.finance.lowcode.agent.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qihoo.finance.lowcode.common.configuration.UserInfoPersistentState;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.entity.agent.AgentTaskDetail;
import com.qihoo.finance.lowcode.common.entity.agent.AgentTaskSignal;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.util.LowCodeAppUtils;
import com.qihoo.finance.lowcode.common.util.RestTemplateUtil;
import org.qifu.devops.ide.plugins.jiracommit.configuration.PersistentState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GenerateTrackUtils
 *
 * @author fengjinfu-jk
 * date 2023/11/22
 * @version 1.0.0
 * @apiNote GenerateTrackUtils
 */
public class AgentTaskUtils extends LowCodeAppUtils {
    private static int signalVersion = 0;
    private static final TypeReference<Result<List<AgentTaskDetail>>> AGENT_TASK_LIST = new TypeReference<>() {
    };
    private static final TypeReference<Result<AgentTaskDetail>> AGENT_TASK = new TypeReference<>() {
    };
    private static final TypeReference<Result<AgentTaskSignal>> AGENT_TASK_SIGNAL = new TypeReference<>() {
    };

    public static List<AgentTaskDetail> queryUserRecentAgentTask() {
        String url = Constants.Url.GET_USER_RECENT_AGENT_TASK;
        Map<String, Object> param = new HashMap<>();
        param.put("createdBy", getUserInfo().getUserNo());
        Result<List<AgentTaskDetail>> result = catchException(url,
                () -> RestTemplateUtil.get(url, param, new HashMap<>(), AGENT_TASK_LIST), "获取用户最近AgentTask失败" + ADD_NOTIFY, false);
        return resultData(result, new ArrayList<>());
    }

    public static AgentTaskDetail closeAgentTask(String id) {
        String url = Constants.Url.GET_CLOSE_USER_AGENT_TASK;
        Map<String, Object> param = new HashMap<>();
        param.put("id", id);
        param.put("createdBy", getUserInfo().getUserNo());
        Result<AgentTaskDetail> result = catchException(url,
                () -> RestTemplateUtil.get(url, param, new HashMap<>(), AGENT_TASK), "关闭用户AgentTask失败" + ADD_NOTIFY, false);
        return resultData(result, new AgentTaskDetail());
    }

    public synchronized static AgentTaskSignal checkingAgentTaskSignal() {
        String url = Constants.Url.GET_USER_AGENT_TASK_SIGNAL;
        Map<String, Object> param = new HashMap<>();
        param.put("createdBy", getUserInfo().getUserNo());

        Result<AgentTaskSignal> result = catchException(url,
                () -> RestTemplateUtil.get(url, param, new HashMap<>(), AGENT_TASK_SIGNAL), "获取用户最近AgentTask通知信息失败" + ADD_NOTIFY, false);

        // 比较版本
        AgentTaskSignal signal = result.getData();
        if (result.isSuccess() && signalVersion != signal.getVersion()) {
            if (signalVersion == 0) {
                // 首次启动
                signalVersion = signal.getVersion();
                getUserInfo().setAgentSignalVersion(signal.getVersion());
                UserInfoPersistentState.getInstance().loadState(getUserInfo());
                return new AgentTaskSignal(false);
            }

            signalVersion = signal.getVersion();
            int agentSignalVersion = getUserInfo().getAgentSignalVersion();
            if (agentSignalVersion != signal.getVersion()) {
                getUserInfo().setAgentSignalVersion(signal.getVersion());
                UserInfoPersistentState.getInstance().loadState(getUserInfo());
                signal.setNewVersion(true);
            }
            return signal;
        }

        return new AgentTaskSignal(false);
    }

    public static Object clearAllAgentTask() {
        String url = Constants.Url.GET_CLOSE_ALL_USER_AGENT_TASK;
        Map<String, Object> param = new HashMap<>();
        param.put("createdBy", getUserInfo().getUserNo());
        Result<AgentTaskDetail> result = catchException(url,
                () -> RestTemplateUtil.get(url, param, new HashMap<>(), AGENT_TASK), "关闭用户所有AgentTask失败" + ADD_NOTIFY, false);
        return resultData(result, new AgentTaskDetail());
    }
}
