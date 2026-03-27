package com.qihoo.finance.lowcode.gentracker.tool;

import com.qihoo.finance.lowcode.gentracker.entity.GlobalConfig;
import com.qihoo.finance.lowcode.gentracker.entity.Template;

import java.util.Collection;
import java.util.Collections;

/**
 * 模板工具，主要用于对模板进行预处理
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
public final class TemplateUtils {
    /**
     * 不允许创建实例对象
     */
    private TemplateUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * 向模板中注入全局变量
     *
     * @param template      模板
     * @param globalConfigs 全局变量
     * @return 处理好的模板
     */
    public static String addGlobalConfig(String template, Collection<GlobalConfig> globalConfigs) {
        if (CollectionUtil.isEmpty(globalConfigs)) {
            return template;
        }
        for (GlobalConfig globalConfig : globalConfigs) {
            String name = globalConfig.getName();
            // 正则被替换字符转义处理
            String value = globalConfig.getValue().replace("\\", "\\\\").replace("$", "\\$");

            // 将不带{}的变量加上{}
            template = template.replaceAll("\\$!?" + name + "(\\W)", "\\$!{" + name + "}$1");
            // 统一替换
            template = template.replaceAll("\\$!?\\{" + name + "}", value);
        }
        return template;
    }

    /**
     * 向模板中注入全局变量
     *
     * @param template      模板对象
     * @param globalConfigs 全局变量
     */
    public static void addGlobalConfig(Template template, Collection<GlobalConfig> globalConfigs) {
        if (template == null || StringUtils.isEmpty(template.getCode())) {
            return;
        }
        // 模板后面添加换行符号，防止在模板末尾添加全局变量导致无法匹配问题
        template.setCode(addGlobalConfig(template.getCode() + "\n", globalConfigs));
    }

    /**
     * 向模板中注入全局变量
     *
     * @param templates     多个模板
     * @param globalConfigs 全局变量
     */
    public static void addGlobalConfig(Collection<Template> templates, Collection<GlobalConfig> globalConfigs) {
        if (CollectionUtil.isEmpty(templates)) {
            return;
        }
        templates.forEach(template -> addGlobalConfig(template, globalConfigs));
    }

    /**
     * 向模板中注入全局变量
     * // 添加全局配置, 将不同全局配置模板代码块替换掉模板中预设的占位符, globalConfig包括
     * // init.vm(初始化importList等信息, 为autoImport.vm等提供使用数据),
     * // autoImport.vm(import importList信息),
     * // define.vm(GET/SET等宏定义),
     * // mybatisSupport.vm(JDBC_TYPE)
     *
     * @param templates 多个模板
     */
    public static void addGlobalConfig(Collection<Template> templates) {
        addGlobalConfig(templates, CurrGroupUtils.getCurrGlobalConfigGroup().getElementList());
    }

    /**
     * 向模板中注入全局变量
     *
     * @param template 单个模板
     */
    public static void addGlobalConfig(Template template) {
        if (template != null) {
            addGlobalConfig(Collections.singleton(template));
        }
    }
}
