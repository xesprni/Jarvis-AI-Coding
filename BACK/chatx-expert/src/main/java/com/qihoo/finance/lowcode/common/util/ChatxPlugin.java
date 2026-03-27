package com.qihoo.finance.lowcode.common.util;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.extensions.PluginId;
import org.jetbrains.annotations.NotNull;

public class ChatxPlugin {

    public static final PluginId CHATX_ID = PluginId.getId("com.qihoo.finance.lowcode.chatx-expert");

    public static boolean isChatxPlugin(@NotNull PluginDescriptor pluginDescriptor) {
        return pluginDescriptor.getPluginId().equals(CHATX_ID);
    }

    public static String getVersion() {
        IdeaPluginDescriptor pluginDescriptor = PluginManagerCore.getPlugin(CHATX_ID);
        if (pluginDescriptor == null) {
            return null;
        }
        return pluginDescriptor.getVersion();
    }

}
