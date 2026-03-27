package com.qihoo.finance.lowcode.gentracker.tool;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;

import java.util.Objects;

/**
 * PluginUtils
 *
 * @author fengjinfu-jk
 * date 2024/2/7
 * @version 1.0.0
 * @apiNote PluginUtils
 */
public class PluginUtils {
    /**
     * 必须与 plugin.xml 中 <id>com.qihoo.finance.lowcode.chatx-expert<id/> 一致
     */
    public static String PLUGIN_ID = "com.qihoo.finance.lowcode.chatx-expert";

    public static IdeaPluginDescriptor getPlugin() {
        return PluginManager.getInstance().findEnabledPlugin(PluginId.getId(PLUGIN_ID));
    }

    public static String getPluginVersion() {
        IdeaPluginDescriptor plugin = getPlugin();
        return plugin != null ? plugin.getVersion() : null;
    }
}
