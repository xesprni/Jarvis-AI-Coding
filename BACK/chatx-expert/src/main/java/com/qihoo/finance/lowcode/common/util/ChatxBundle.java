package com.qihoo.finance.lowcode.common.util;

import com.qihoo.finance.lowcode.editor.ChatxService;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;

import java.util.Locale;
import java.util.ResourceBundle;

public class ChatxBundle {

    private static  ChatxBundle INSTANCE;

    private ResourceBundle resourceBundle;

    private ChatxBundle() {
        reloadBundle();
    }

    private void reloadBundle() {
        String langSetting = "Default";
        try {
            if (ChatxApplicationSettings.settings() != null) {
                langSetting = (ChatxApplicationSettings.settings()).askCodegeexLanguageSettingEnum;
            }
        } catch (Throwable ignored) {
            // 避免 Application 尚未初始化时抛异常
        }

        Locale currentLocale = new Locale("en", "US");
        try {
            ChatxService service = ChatxService.getInstance();
            if ("zh-CN".equals(langSetting) || ("Default".equals(langSetting) && service.isChineseEnabled())) {
                currentLocale = new Locale("zh", "CN");
            }
        } catch (Throwable ignored) {
            // 同理：IDE 启动早期不加载 Service
        }

        resourceBundle = ResourceBundle.getBundle("messages.chatx", currentLocale);
    }

    public static ChatxBundle getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ChatxBundle();
        }
        return INSTANCE;
    }

    public static String get(String key) {
        return getInstance().getString(key);
    }

    public static String get(String key, Object... params) {
        String message = getInstance().getString(key);
        return String.format(message, params);
    }

    private String getString(String key) {
        try {
            return resourceBundle.getString(key);
        } catch (Exception e) {
            return "Key not found: " + key;
        }
    }

    public static void setLocale(String langSetting) {
        getInstance().reloadBundle();
    }
}
