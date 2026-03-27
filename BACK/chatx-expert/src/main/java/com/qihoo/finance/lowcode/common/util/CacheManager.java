package com.qihoo.finance.lowcode.common.util;

import com.qihoo.finance.lowcode.gentracker.config.SettingsSettingStorage;

/**
 * GenerateRefresher
 *
 * @author fengjinfu-jk
 * date 2023/8/9
 * @version 1.0.0
 * @apiNote GenerateRefresher
 */
public class CacheManager {
    public static void refreshALL() {
        // 接口缓存
        InnerCacheUtils.refresh();

        // 资源缓存
        SettingsSettingStorage.refresh();
    }

    public static void refreshInnerCache() {
        InnerCacheUtils.refresh();
    }

    public static void refreshTemplate() {
        InnerCacheUtils.refresh();

//        SettingsSettingStorage.getCacheResource().resetTemplateConfig();
        // fixme: 暂时刷新所有资源缓存
        SettingsSettingStorage.refresh();
    }

    public static void refreshTypeMapper() {
        InnerCacheUtils.refresh();
        SettingsSettingStorage.getCacheResource().resetTypeMapperConfig();
    }
}
