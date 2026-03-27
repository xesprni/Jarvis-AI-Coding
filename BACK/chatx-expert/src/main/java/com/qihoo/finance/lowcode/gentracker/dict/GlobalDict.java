package com.qihoo.finance.lowcode.gentracker.dict;

/**
 * 全局字典
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
public interface GlobalDict {
    /**
     * 提示信息
     */
    String PLUGIN_NAME = "Jarvis";

    String PLUGIN_FULL_NAME = "Jarvis • AI软件研发";
    /**
     * 提示信息
     */
    String TITLE_INFO = PLUGIN_NAME;
    /**
     * 版本号
     */
    String VERSION = "1.2.8";
    /**
     * 动态替换为当前登录人信息
     * 作者名称
     */
    String AUTHOR = TITLE_INFO;
    /**
     * 公司
     */
    String COMPANY = TITLE_INFO;

    /**
     * 默认分组名称
     */
    String DEFAULT_GROUP_NAME = "Default";
}
