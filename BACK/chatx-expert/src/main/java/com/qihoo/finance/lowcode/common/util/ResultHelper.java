package com.qihoo.finance.lowcode.common.util;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.qihoo.finance.lowcode.common.action.UserInfoAction;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.constants.ServiceErrorCode;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.update.PluginUpdater;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Slf4j
public class ResultHelper {

    public static void handleResult(Result<?> result, String url) {
        handleResult(result, url, "Jarvis加载失败" + LowCodeAppUtils.ADD_NOTIFY, false);
    }

    public static void handleResult(Result<?> result, String url, String notifyIfFail, boolean notifyErr) {
        if (result.isSuccess()) return;

        // token失效登录失效, 重定向到登录页面
        if (checkInvalidateToken(result)) {
            logoutRedirect(url);
        } else if (checkForceUpdate(result)) {
            notifyUpdateVersion(result);
        } else if (result.isFail()) {
            notifyIfNeed(result, url, notifyIfFail, notifyErr);
        }
    }


    public static boolean checkInvalidateToken(Result<?> result) {
        return result.isFail() && Constants.ResponseCode.TOKEN_INVALID.equals(result.getErrorCode());
    }

    public static boolean checkForceUpdate(Result<?> result) {
        return (result.isFail() && Objects.equals(result.getErrorCode(), ServiceErrorCode.PLUGIN_VERSION_TOO_OLD.getCode()));
    }

    public static void logoutRedirect(String url) {
        String tips = String.format("【%s】登录信息已过期, 请重新登录", GlobalDict.PLUGIN_NAME);
        if (NotifyUtils.checkNotifyTimeInterval(tips)) {
            NotifyUtils.notify(tips, NotificationType.ERROR);
        }

        log.warn("登录信息已过期, 请重新登录 {}", url);
        // 重定向至登陆页面
        ApplicationManager.getApplication().invokeLater(() -> {
           UserInfoAction service = ProjectUtils.getCurrProject().getService(UserInfoAction.class);
           service.logout();
        });
    }

    private static void notifyIfNeed(Result<?> result, String url, String notifyMsg, boolean notifyErr) {
        String errorCode = result.getErrorCode();
        ServiceErrorCode serviceError = ServiceErrorCode.getByCode(errorCode);
        if (Objects.nonNull(serviceError)) {
            notifyMsg = result.getErrorMsg();
        }
        // 优先使用后端返回的具体错误信息
        if (StringUtils.isEmpty(notifyMsg) && StringUtils.isNotEmpty(result.getErrorMsg())) {
            notifyMsg = result.getErrorMsg();
        }

        if (StringUtils.isNotEmpty(notifyMsg)) {
            log.error("{}, 错误码: {}, 接口: {}", notifyMsg, result.getErrorCode(), url);
        } else {
            log.error("Jarvis加载失败{}, 错误码: {}, 接口: {}", LowCodeAppUtils.ADD_NOTIFY, result.getErrorCode(), url);
        }
        if (notifyErr) {
            // 修改：优先使用后端返回的具体错误信息，只有在没有具体错误信息时才使用默认提示
            String finalNotifyMsg = StringUtils.isNotEmpty(notifyMsg) ? notifyMsg :
                    (StringUtils.isNotEmpty(result.getErrorMsg()) ? result.getErrorMsg() : "Jarvis加载失败" + LowCodeAppUtils.ADD_NOTIFY);
            // 提醒时间间隔
            if (NotifyUtils.checkNotifyTimeInterval(finalNotifyMsg)) NotifyUtils.notify(finalNotifyMsg, NotificationType.ERROR);
        }
    }

    private static void notifyUpdateVersion(Result<?> result) {
        notifyUpdateVersion(result.getErrorMsg());
    }

    public static void notifyUpdateVersion(String errorMsg) {
        Element root = XmlUtils.loadUrlXml(Constants.Plugins.PLUGIN_XML_URL);
        String onlineVersion = "最新版本";
        if (root != null) {
            onlineVersion = root.element("plugin").attribute("version").getValue();
        }

        if (NotifyUtils.checkNotifyTimeInterval(errorMsg)) {
            NotifyUtils.build("Update", String.format("【%s】%s", GlobalDict.PLUGIN_NAME, errorMsg), NotificationType.WARNING)
                    .setIcon(Icons.scaleToWidth(Icons.ROCKET, 20))
                    .addAction(new NotificationAction(String.format("立即更新到 %s", onlineVersion)) {
                        @Override
                        public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                            // 定位到插件界面
                            PluginUpdater.searchAvailableUpdate();
                        }
                    }).notify(ProjectUtils.getCurrProject());
        }
    }
}
