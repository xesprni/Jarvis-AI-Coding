package com.qihoo.finance.lowcode.common.util;

import com.intellij.openapi.application.ApplicationManager;
import com.qihoo.finance.lowcode.common.configuration.UserInfoPersistentState;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import com.qihoo.finance.lowcode.gentracker.tool.StringUtils;

import java.util.Objects;

/**
 * @author weiyichao
 * @date 2023-07-10
 */
public class UserUtils {
    public static String getUserEmail() {
        UserInfoPersistentState userInfoPersistentState = ApplicationManager.getApplication().getService(UserInfoPersistentState.class);
        UserInfoPersistentState.UserInfo user = userInfoPersistentState.getState();
        return Objects.nonNull(user) && Objects.nonNull(user.email) ? user.email : GlobalDict.AUTHOR;
    }
    public static String getUserNo() {
        UserInfoPersistentState userInfoPersistentState = ApplicationManager.getApplication().getService(UserInfoPersistentState.class);
        UserInfoPersistentState.UserInfo user = userInfoPersistentState.getState();
        return Objects.nonNull(user) && Objects.nonNull(user.getUserNo()) ? user.getUserNo() : GlobalDict.AUTHOR;
    }

    public static UserInfoPersistentState.UserInfo getUserInfo() {
        UserInfoPersistentState userInfoPersistentState = ApplicationManager.getApplication().getService(UserInfoPersistentState.class);
        return userInfoPersistentState.getState();
    }

    public static String getMac() {
        UserInfoPersistentState.UserInfo user = UserInfoPersistentState.getUserInfo();
        return Objects.nonNull(user) && !StringUtils.isEmpty(user.mac) ? user.mac : null;
    }

    public static String getToken() {
        UserInfoPersistentState.UserInfo user = UserInfoPersistentState.getUserInfo();
        return Objects.nonNull(user) && !StringUtils.isEmpty(user.token) ? user.token : null;
    }
}
