package com.qihoo.finance.lowcode.gentracker.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.intellij.openapi.application.ApplicationManager;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import com.qihoo.finance.lowcode.gentracker.entity.*;
import lombok.Data;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 设置储存传输对象
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
@Data
public class SettingsSettingStorage {
    private static SettingsSettingStorage storageCache;
    /**
     * 类型映射组
     */
    @JsonProperty("typeMapper")
    private Map<String, TypeMapperGroup> typeMapperGroupMap;
    /**
     * 模板组
     */
    @JsonProperty("template")
    private Map<String, TemplateGroup> templateGroupMap;
    /**
     * 配置表组
     */
    @JsonProperty("columnConfig")
    private Map<String, ColumnConfigGroup> columnConfigGroupMap;
    /**
     * 全局配置组
     */
    @JsonProperty("globalConfig")
    private Map<String, GlobalConfigGroup> globalConfigGroupMap;

    /**
     * 当前类型映射组名
     */
    private String currTypeMapperGroupName;
    /**
     * 当前模板组名
     */
    private String currTemplateGroupName;
    /**
     * 当前配置表组名
     */
    private String currColumnConfigGroupName;
    /**
     * 当前全局配置组名
     */
    private String currGlobalConfigGroupName;

    public static Collection<? extends Template> getInnerTemplates(String groupName) {
        TemplateGroup templateGroup = getCacheResource().getTemplateGroupMap().get(groupName);
        templateGroup = ObjectUtils.defaultIfNull(templateGroup, getCacheResource().getTemplateGroupMap().get(GlobalDict.DEFAULT_GROUP_NAME));

        return templateGroup.getElementList().stream().filter(Template::isInnerTemplate).collect(Collectors.toList());
    }

    public static SettingsSettingStorage getCacheResource() {
        return Optional.ofNullable(storageCache).orElseGet(() -> {
            storageCache = loadResource();
            return storageCache;
        });
    }

    private static SettingsSettingStorage loadResource() {
        SettingsSettingStorage generateSetting = new SettingsSettingStorage();

        @SuppressWarnings("unchecked")
        SettingsLoader<GenerateSetting> loader = ApplicationManager.getApplication().getService(SettingsLoader.class);
        GenerateSetting setting = loader.loadSetting();

        // columnConfigGroup
        generateSetting.setColumnConfigGroupMap(setting.getColumnConfigGroupMap());
        // globalConfigGroup
        generateSetting.setGlobalConfigGroupMap(setting.getGlobalConfigGroupMap());
        // templateGroup
        generateSetting.setTemplateGroupMap(setting.getTemplateGroupMap());
        // typeMapperGroupMap
        generateSetting.setTypeMapperGroupMap(setting.getTypeMapperGroupMap());

        // default group
        generateSetting.setCurrTemplateGroupName(GlobalDict.DEFAULT_GROUP_NAME);
        generateSetting.setCurrGlobalConfigGroupName(GlobalDict.DEFAULT_GROUP_NAME);
        generateSetting.setCurrColumnConfigGroupName(GlobalDict.DEFAULT_GROUP_NAME);
        generateSetting.setCurrTypeMapperGroupName(GlobalDict.DEFAULT_GROUP_NAME);

        return generateSetting;
    }

    public static void refresh() {
        getCacheResource().reset();
    }

    /**
     * 重置为默认值
     */
    public void reset() {
        SettingsSettingStorage defaultVal = loadResource();

        this.setColumnConfigGroupMap(defaultVal.getColumnConfigGroupMap());
        this.setCurrColumnConfigGroupName(GlobalDict.DEFAULT_GROUP_NAME);

        this.setGlobalConfigGroupMap(defaultVal.getGlobalConfigGroupMap());
        this.setCurrGlobalConfigGroupName(GlobalDict.DEFAULT_GROUP_NAME);

        this.setTypeMapperGroupMap(defaultVal.getTypeMapperGroupMap());
        this.setCurrTypeMapperGroupName(GlobalDict.DEFAULT_GROUP_NAME);

        this.setTemplateGroupMap(defaultVal.getTemplateGroupMap());
        this.setCurrTemplateGroupName(GlobalDict.DEFAULT_GROUP_NAME);
    }

    /**
     * 重置为默认值
     */
    public void resetTemplateConfig() {
        SettingsSettingStorage defaultVal = loadResource();

        this.setTemplateGroupMap(defaultVal.getTemplateGroupMap());
        this.setCurrTemplateGroupName(GlobalDict.DEFAULT_GROUP_NAME);
    }

    /**
     * 重置为默认值
     */
    public void resetTypeMapperConfig() {
        SettingsSettingStorage defaultVal = loadResource();

        this.setTypeMapperGroupMap(defaultVal.getTypeMapperGroupMap());
        this.setCurrTypeMapperGroupName(GlobalDict.DEFAULT_GROUP_NAME);
    }
}
