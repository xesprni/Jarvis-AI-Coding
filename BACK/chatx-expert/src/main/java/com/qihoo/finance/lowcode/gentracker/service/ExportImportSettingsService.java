package com.qihoo.finance.lowcode.gentracker.service;

import com.qihoo.finance.lowcode.gentracker.config.SettingsSettingStorage;

/**
 * 导出导入设置服务
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
public interface ExportImportSettingsService {

    /**
     * 导出设置
     *
     * @param settingsStorage 要导出的设置
     */
    void exportConfig(SettingsSettingStorage settingsStorage);

    /**
     * 导入设置
     *
     * @return 设置信息
     */
    SettingsSettingStorage importConfig();

}
