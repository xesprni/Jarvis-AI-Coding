package com.qihoo.finance.lowcode.gentracker.ui.setting;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.qihoo.finance.lowcode.gentracker.config.SettingsSettingStorage;
import com.qihoo.finance.lowcode.gentracker.service.SettingsStorageService;
import org.jetbrains.annotations.Nullable;

/**
 * 基础设置
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
public interface BaseSettings extends Configurable {
    /**
     * 帮助提示信息
     *
     * @return 提示信息
     */
    @Nullable
    @Override
    default String getHelpTopic() {
        return getDisplayName();
    }

    /**
     * 重置设置
     */
    @Override
    default void reset() {
        loadSettingsStore();
    }

    /**
     * 获取设置信息
     *
     * @return 获取设置信息
     */
    default SettingsSettingStorage getSettingsStorage() {
        return SettingsStorageService.getSettingsStorage();
    }

    /**
     * 加载配置信息
     */
    default void loadSettingsStore() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            SettingsSettingStorage settingsStorage = getSettingsStorage();
            ApplicationManager.getApplication().invokeLater(() -> {
                loadSettingsStore(settingsStorage);
            });
        });
    }

    /**
     * 加载配置信息
     *
     * @param settingsStorage 配置信息
     */
    void loadSettingsStore(SettingsSettingStorage settingsStorage);

}
