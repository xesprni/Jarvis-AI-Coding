package com.qihoo.finance.lowcode.gentracker.tool;

import com.qihoo.finance.lowcode.gentracker.config.SettingsSettingStorage;
import com.qihoo.finance.lowcode.gentracker.entity.ColumnConfigGroup;
import com.qihoo.finance.lowcode.gentracker.entity.GlobalConfigGroup;
import com.qihoo.finance.lowcode.gentracker.entity.TemplateGroup;
import com.qihoo.finance.lowcode.gentracker.entity.TypeMapperGroup;
import com.qihoo.finance.lowcode.gentracker.service.SettingsStorageService;

/**
 * 当前分组配置获取工具
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
public final class CurrGroupUtils {
    /**
     * 禁用构造方法
     */
    private CurrGroupUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * 获取当前模板组对象
     *
     * @return 模板组对象
     */
    public static TemplateGroup getCurrTemplateGroup() {
        SettingsSettingStorage settingsStorage = SettingsStorageService.getSettingsStorage();
        String groupName = settingsStorage.getCurrTemplateGroupName();
        return settingsStorage.getTemplateGroupMap().get(groupName);
    }


    /**
     * 获取当前全局配置组对象
     *
     * @return 全局配置组对象
     */
    public static GlobalConfigGroup getCurrGlobalConfigGroup() {
        SettingsSettingStorage settingsStorage = SettingsStorageService.getSettingsStorage();
        String groupName = settingsStorage.getCurrGlobalConfigGroupName();
        return settingsStorage.getGlobalConfigGroupMap().get(groupName);
    }


    /**
     * 获取当前类型映射组对象
     *
     * @return 类型映射组对象
     */
    public static TypeMapperGroup getCurrTypeMapperGroup() {
        SettingsSettingStorage settingsStorage = SettingsStorageService.getSettingsStorage();
        String groupName = settingsStorage.getCurrTypeMapperGroupName();
        return settingsStorage.getTypeMapperGroupMap().get(groupName);
    }

    /**
     * 获取当前列配置组对象
     *
     * @return 列配置组对象
     */
    public static ColumnConfigGroup getCurrColumnConfigGroup() {
        SettingsSettingStorage settingsStorage = SettingsStorageService.getSettingsStorage();
        String groupName = settingsStorage.getCurrColumnConfigGroupName();
        return settingsStorage.getColumnConfigGroupMap().get(groupName);
    }

}
