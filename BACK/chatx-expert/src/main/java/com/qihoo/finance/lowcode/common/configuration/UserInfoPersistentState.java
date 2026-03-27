package com.qihoo.finance.lowcode.common.configuration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.qihoo.finance.lowcode.common.util.RestTemplateUtil;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * @author weiyichao
 * @date 2023-07-27
 **/
@State(name = "UserInfo", storages = @Storage("userInfo.xml"))
public class UserInfoPersistentState implements PersistentStateComponent<UserInfoPersistentState.UserInfo> {
    private UserInfo userInfo = new UserInfo();

    public static UserInfoPersistentState getInstance() {
        if (ApplicationManager.getApplication() == null) {
            return null;
        }
        return ApplicationManager.getApplication().getService(UserInfoPersistentState.class);
    }

    public static UserInfo getUserInfo() {
        if (getInstance() == null) {
            return null;
        }
        return getInstance().getState();
    }

    @Nullable
    @Override
    public UserInfo getState() {
        return userInfo;
    }

    @Override
    public void loadState(@NotNull UserInfo state) {
        userInfo = state;
    }


    public static class UserInfo {
        public String email;
        public String token;
        public String mac;
        public String nickName;
        @Getter
        @Setter
        public String gitlabToken;
        @Getter
        @Setter
        private int agentSignalVersion;

        public String getUserNo() {
            if (StringUtils.isNotEmpty(email)) {
                return email.split("@")[0];
            }

            return email;
        }

        public String getNickName() {
            return nickName != null ? nickName : getUserNo();
        }

        public String getMac() {
            if (StringUtils.isEmpty(mac)) {
                mac = RestTemplateUtil.getLocalMac();
            }

            return mac;
        }
    }
}
