package com.qihoo.finance.lowcode.common.update;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationInfo;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.entity.dto.plugins.PluginConfig;
import com.qihoo.finance.lowcode.common.util.*;
import com.qihoo.finance.lowcode.gentracker.tool.PluginUtils;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * PluginToolWindowAction
 *
 * @author fengjinfu-jk
 * date 2023/8/31
 * @version 1.0.0
 * @apiNote PluginToolWindowAction
 */
@Slf4j
public class PluginXmlVersionChecker {
    private static long lastCheckTime = 0;
    private static long intervalTime = 1000 * 60;


    /*
        http://artifacts.daikuan.qihoo.net/artifacts/public/plugins/chatx-expert/updatePlugins.xml

        This XML file does not appear to have any style information associated with it. The document tree is shown below.
        <plugins>
        <!--
                 插件的描述信息
            id -      为 plugin.xml 中的插件ID
            url -     能够访问并下载的插件地址，官网描述必须为 https ，亲测使用 http 也能更新，不过尽量使用 https
            version - 插件的版本，与 plugin.xml 中的 <version> 一致，build.gradle 中的 build.gradle 也保持一致
           -->
        <plugin id="com.qihoo.finance.lowcode.chatx-expert" url="http://artifacts.daikuan.qihoo.net/artifacts/public/plugins/chatx-expert/ChatX-Expert-1.0.0-20230830.170916.zip" version="1.0.0-20230830.170916">
        <name>Chatx-Expert</name>
        <vendor email="idea-plugins@360shuke.com" url="http://www.qifu.com">奇富科技</vendor>
        <!--
                       该标签必须与插件中的 plugini.xml 中对应，且是必须填写的
             -->
        <idea-version since-build="213"/>
        </plugin>
        </plugins>

     */
    public static void versionCheck(boolean async) {
        long now = System.currentTimeMillis();
        if (now - lastCheckTime > intervalTime) {
            lastCheckTime = now;

            if (async) {
                new SwingWorker<>() {
                    @Override
                    protected Object doInBackground() {
                        checkVersionAndNotify();
                        return null;
                    }
                }.execute();
            } else {
                checkVersionAndNotify();
            }
        }
    }

    public static AvailableVersion checkAvailableVersion() {
        AvailableVersion availableVersion = new AvailableVersion();
        Element root = XmlUtils.loadUrlXml(Constants.Plugins.PLUGIN_XML_URL);
        if (null == root) return availableVersion;

        // 遍历所有plugin元素,找到满足当前IDE版本且版本号最新的记录
        Element latestPlugin = null;
        String latestVersion = null;
        
        for (Element plugin : root.elements("plugin")) {
            try {
                Element ideaVersionElement = plugin.element("idea-version");
                if (ideaVersionElement != null) {
                    String sinceBuild = ideaVersionElement.attributeValue("since-build");
                    if (StringUtils.isNotEmpty(sinceBuild)) {
                        int requiredBuild = Integer.parseInt(sinceBuild);
                        // 检查当前IDE版本是否满足要求
                        if (!ApplicationUtil.isBaselineVersionEgt(requiredBuild)) {
                            continue; // 不满足版本要求,跳过
                        }
                    }
                }
                
                String version = plugin.attributeValue("version");
                if (StringUtils.isEmpty(version)) continue;
                
                // 比较版本号,取最新的
                if (latestVersion == null || ApplicationUtil.compareSemVer(version, latestVersion) > 0) {
                    latestVersion = version;
                    latestPlugin = plugin;
                }
            } catch (Exception e) {
                log.warn("Failed to parse plugin element: {}", e.getMessage());
            }
        }

        if (latestPlugin != null) {
            availableVersion.setOnlineVersion(latestVersion);
            
            // such as: 1.0.0-20230825.113631
            String localVersion = PluginUtils.getPluginVersion();
            availableVersion.setLocalVersion(localVersion);

            if (StringUtils.isNotEmpty(latestVersion) && StringUtils.isNotEmpty(localVersion)) {
                boolean haveAvailableUpdate = !latestVersion.equals(localVersion);
                log.info("currentBuild: {}, localVersion: {}, xml onlineVersion: {}, haveAvailableUpdate: {}",
                        ApplicationInfo.getInstance().getBuild().getBaselineVersion(), localVersion, latestVersion, haveAvailableUpdate);

                availableVersion.setHaveAvailableUpdate(haveAvailableUpdate);
            }
        }

        return availableVersion;
    }

    public static void checkVersionAndNotify() {
        AvailableVersion availableVersion = checkAvailableVersion();
        if (availableVersion.isHaveAvailableUpdate()) {
            // The latest version ${onlineVersion} of ChaX-Expert is available. Please update to the latest version for better experience.
            // 您的JARVIS不是最新版本, 请及时更新以便得到最佳体验 !
            // JARVIS更新啦, 快来体验最新功能 !
//                String title = "Update is available";
            String title = "Update";
            String tips = "Jarvis更新啦, 快来体验最新功能 !";

            PluginConfig pluginConfig = LowCodeAppUtils.getPluginConfig();
            if (pluginConfig.isForceUpdate()) {
                // 强制更新提醒
                tips = "Jarvis更新啦, 部分功能旧版本可能无法正常使用, 快来升级体验最新功能 !";
            }
            NotifyUtils.build(title, tips, NotificationType.WARNING).setIcon(Icons.scaleToWidth(Icons.ROCKET, 20)).addAction(new NotificationAction(String.format("立即更新到 %s", availableVersion.getOnlineVersion())) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                    // 定位到插件界面
                    PluginUpdater.searchAvailableUpdate();
                }
            }).notify(ProjectUtils.getCurrProject());
        }
    }

    @Data
    public static class AvailableVersion {
        private boolean haveAvailableUpdate;
        private String onlineVersion;
        private String localVersion;
    }
}
