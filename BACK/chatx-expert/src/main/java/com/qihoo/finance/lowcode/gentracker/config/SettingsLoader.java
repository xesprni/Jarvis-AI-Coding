package com.qihoo.finance.lowcode.gentracker.config;

import org.apache.commons.lang3.ObjectUtils;

/**
 * 设置储存传输对象
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
public interface SettingsLoader<T> {
    T loadRemoteSettings();

    T loadLocalSettings();

    default T loadSetting() {
        return ObjectUtils.defaultIfNull(loadRemoteSettings(), loadLocalSettings());
    }
}
