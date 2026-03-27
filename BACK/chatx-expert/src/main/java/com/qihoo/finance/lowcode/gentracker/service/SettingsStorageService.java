package com.qihoo.finance.lowcode.gentracker.service;

import com.intellij.openapi.application.ApplicationManager;
import com.qihoo.finance.lowcode.gentracker.config.SettingsSettingStorage;

/**
 * 配置信息
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
public interface SettingsStorageService {
    SettingsSettingStorage getState();

    /**
     * 获取实例
     *
     * @return {@link SettingsStorageService}
     */
    static SettingsStorageService getInstance() {
        return ApplicationManager.getApplication().getService(SettingsStorageService.class);
    }

    /**
     * 获取设置存储
     *
     * @return {@link SettingsSettingStorage}
     */
    static SettingsSettingStorage getSettingsStorage() {
        return getInstance().getState();
    }
}
