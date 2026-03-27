package com.qihoo.finance.lowcode.gentracker.config;

import com.google.common.collect.Lists;
import com.intellij.ide.fileTemplates.impl.UrlUtil;
import com.qihoo.finance.lowcode.common.util.LowCodeAppUtils;
import com.qihoo.finance.lowcode.gentracker.entity.*;
import com.qihoo.finance.lowcode.gentracker.tool.JSON;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 设置储存传输对象
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
@Slf4j
public class GenerateSettingsLoader implements SettingsLoader<GenerateSetting> {
    private static final List<String> LOCAL_INNER_TEMPLATES = Lists.newArrayList("entityDTO.java.vm", "Page.java.vm", "pageOrder.java.vm", "pageQuery.java.vm", "result.java.vm");

    @Override
    public GenerateSetting loadRemoteSettings() {
        GenerateSetting localSettings;
        try {
            localSettings = loadLocalSettings();
        } catch (Exception e) {
            localSettings = new GenerateSetting();
            localSettings.setColumnConfigGroupMap(new HashMap<>());
            localSettings.setTemplateGroupMap(new HashMap<>());
            localSettings.setGlobalConfigGroupMap(new HashMap<>());
            localSettings.setTypeMapperGroupMap(new HashMap<>());
        }

        GenerateSetting remoteSetting = new GenerateSetting();
        // templateGroup
        Map<String, TemplateGroup> templateGroupMap = LowCodeAppUtils.queryTemplateGroup();
        remoteSetting.setTemplateGroupMap(ObjectUtils.defaultIfNull(templateGroupMap, localSettings.getTemplateGroupMap()));
        log.info("load templateGroupMap from {}", Objects.nonNull(templateGroupMap) ? "remote" : "local");

        // typeMapperGroupMap
        Map<String, TypeMapperGroup> typeMapperGroupMap = LowCodeAppUtils.queryTypeMapperGroupMap();
        remoteSetting.setTypeMapperGroupMap(ObjectUtils.defaultIfNull(typeMapperGroupMap, localSettings.getTypeMapperGroupMap()));
        log.info("load typeMapperGroupMap from {}", Objects.nonNull(typeMapperGroupMap) ? "remote" : "local");

        // columnConfigGroup
        Map<String, ColumnConfigGroup> columnConfigGroupMap = LowCodeAppUtils.queryColumnConfigGroup();
        remoteSetting.setColumnConfigGroupMap(ObjectUtils.defaultIfNull(columnConfigGroupMap, localSettings.getColumnConfigGroupMap()));
        log.info("load columnConfigGroupMap from {}", Objects.nonNull(columnConfigGroupMap) ? "remote" : "local");

        // globalConfigGroup
        Map<String, GlobalConfigGroup> globalConfigGroupMap = LowCodeAppUtils.queryGlobalConfigGroup();
        remoteSetting.setGlobalConfigGroupMap(ObjectUtils.defaultIfNull(globalConfigGroupMap, localSettings.getGlobalConfigGroupMap()));
        log.info("load globalConfigGroupMap from {}", Objects.nonNull(globalConfigGroupMap) ? "remote" : "local");

        return remoteSetting;
    }


    @SneakyThrows
    @Override
    public GenerateSetting loadLocalSettings() {
        GenerateSetting localSetting = new GenerateSetting();

        // columnConfigGroup
        Map<String, ColumnConfigGroup> columnConfigGroupMap = new HashMap<>();
        List<String> columnConfigPaths = getChildrenRelativePaths("/columnConfig");
        for (String url : columnConfigPaths) {
            String content = loadText("/columnConfig/" + url);
            ColumnConfigGroup group = JSON.parse(content, ColumnConfigGroup.class);
            columnConfigGroupMap.put(group.getName(), group);
        }
        localSetting.setColumnConfigGroupMap(columnConfigGroupMap);

        // globalConfigGroup
        Map<String, GlobalConfigGroup> globalConfigGroupMap = new HashMap<>();
        List<String> defaultSettingPaths = getChildrenRelativePaths("/Default");
        GlobalConfigGroup group = new GlobalConfigGroup();
        group.setName("Default");
        group.setElementList(new ArrayList<>());
        globalConfigGroupMap.put(group.getName(), group);
        for (String url : defaultSettingPaths) {
            String content = loadText("/Default/" + url);
            GlobalConfig config = new GlobalConfig();
            config.setName(getSourceLastName(url));
            config.setValue(content);
            group.getElementList().add(config);
        }
        localSetting.setGlobalConfigGroupMap(globalConfigGroupMap);

        // templateGroup
        List<String> templatePaths = getChildrenRelativePaths("/template");
        // 先创建分组, 目录结构固定为 template/${groupName}/${template.vm}
        Map<String, TemplateGroup> templateGroupMap = templatePaths.stream().filter(url -> !url.endsWith(".vm")).map(url -> {
            TemplateGroup templateGroup = new TemplateGroup();
            templateGroup.setName(getSourceLastName(url));
            templateGroup.setElementList(new ArrayList<>());
            return templateGroup;
        }).collect(Collectors.toMap(TemplateGroup::getName, Function.identity()));
        // 为分组中设置加载模板
        for (String url : templatePaths) {
            String groupName = getSourceGroupName(url);
            if (url.endsWith(".vm") && MapUtils.isNotEmpty(templateGroupMap) && templateGroupMap.containsKey(groupName)) {
                String templateName = getSourceLastName(url);
                String content = loadText("/template/" + url);
                Template template = new Template();
                template.setName(templateName);
                template.setCode(content);
                template.setVersion("local");

                template.setInnerTemplate(LOCAL_INNER_TEMPLATES.contains(templateName));
                template.setCanModify(true);
                template.setForcedOverlay(false);

                templateGroupMap.get(groupName).getElementList().add(template);
            }
        }
        localSetting.setTemplateGroupMap(templateGroupMap);

        // typeMapperGroupMap
        Map<String, TypeMapperGroup> typeMapperGroupMap = new HashMap<>();
        List<String> typeMapperPaths = getChildrenRelativePaths("/typeMapper");
        for (String url : typeMapperPaths) {
            String content = loadText("/typeMapper/" + url);
            TypeMapperGroup typeMapperGroup = JSON.parse(content, TypeMapperGroup.class);
            typeMapperGroupMap.put(typeMapperGroup.getName(), typeMapperGroup);
        }
        localSetting.setTypeMapperGroupMap(typeMapperGroupMap);

        return localSetting;
    }

    private static String loadText(String url) {
        try {
            return UrlUtil.loadText(Objects.requireNonNull(GenerateSettingsLoader.class.getResource(url)));
        } catch (IOException e) {
//            throw new RuntimeException(e);
            log.error("loadText error: {}", e.getMessage());
            return StringUtils.EMPTY;
        }
    }

    private static List<String> getChildrenRelativePaths(String url) {
        try {
            return UrlUtil.getChildrenRelativePaths(Objects.requireNonNull(GenerateSettingsLoader.class.getResource(url)));
        } catch (IOException e) {
//            throw new RuntimeException(e);
            log.error("getChildrenRelativePaths error: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private static String getSourceGroupName(String url) {
        String[] paths = url.split("/");
        return paths[0];
    }

    private static String getSourceLastName(String url) {
        String[] paths = url.split("/");
        return paths[paths.length - 1];
    }
}
