package com.qihoo.finance.lowcode.gentracker.service.impl;

import com.qihoo.finance.lowcode.gentracker.config.SettingsSettingStorage;
import com.qihoo.finance.lowcode.gentracker.service.SettingsStorageService;
import org.jetbrains.annotations.Nullable;

/**
 * 设置储存服务实现
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
public class SettingsStorageServiceImpl implements SettingsStorageService {

    /**
     * 获取配置
     *
     * @return 配置对象
     */
    @Nullable
    @Override
    public SettingsSettingStorage getState() {
        return SettingsSettingStorage.getCacheResource();
    }

}
