package com.qihoo.finance.lowcode.gentracker.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.entity.dto.generate.*;
import com.qihoo.finance.lowcode.common.util.InnerCacheUtils;
import com.qihoo.finance.lowcode.common.util.LowCodeAppUtils;
import com.qihoo.finance.lowcode.common.util.RestTemplateUtil;
import com.qihoo.finance.lowcode.design.entity.DatabaseNode;
import com.qihoo.finance.lowcode.design.entity.MySQLTableNode;
import com.qihoo.finance.lowcode.gentracker.entity.CusPanel;
import com.qihoo.finance.lowcode.gentracker.tool.JSON;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * GenerateTrackUtils
 *
 * @author fengjinfu-jk
 * date 2023/11/22
 * @version 1.0.0
 * @apiNote GenerateTrackUtils
 */
public class GenerateTrackUtils extends LowCodeAppUtils {
    private static final TypeReference<Result<List<GenerateOptions>>> GENERATE_OPTIONS_LIST = new TypeReference<>() {
    };
    private static final TypeReference<Result<GenerateOptions>> GENERATE_OPTIONS = new TypeReference<>() {
    };
    private static final TypeReference<Result<GenerateExampleResult>> GENERATE_EXAMPLE = new TypeReference<>() {
    };
    private static final TypeReference<Result<GenerateJsonEntityResult>> GENERATE_JSON_ENTITY = new TypeReference<>() {
    };
    private static final TypeReference<Result<List<String>>> GENERATE_EXT_FILE_SUFFIX = new TypeReference<>() {
    };

    static final TypeReference<Result<Map<String, List<CusPanel>>>> GROUP_SETTING_PANEL = new TypeReference<>() {
    };


    public static Map<String, List<CusPanel>> getDbGenerateOtherSettingPanels() {
        String url = Constants.Url.GET_DB_GENERATE_OTHER_SETTING;
        String cacheKey = "@getDbGenerateOtherSettingPanels";
        Result<Map<String, List<CusPanel>>> cache = InnerCacheUtils.getCache(cacheKey, GROUP_SETTING_PANEL);
        if (Objects.nonNull(cache)) {
            return resultData(cache);
        }

        Result<Map<String, List<CusPanel>>> result = catchException(url, () -> RestTemplateUtil.get(url, new HashMap<>(), new HashMap<>(), GROUP_SETTING_PANEL), "获取插件配置信息失败" + ADD_NOTIFY, false);
        if (result.isSuccess() && Objects.nonNull(result.getData()))
            InnerCacheUtils.setCache(cacheKey, JSON.toJson(result));
        return resultData(result, new HashMap<>());
    }

    public static List<GenerateOptions> queryGenerateOptions(String databaseName, String tableName) {
        String url = Constants.Url.GET_QUERY_GENERATE_OPTIONS;
        Map<String, Object> param = new HashMap<>();
        param.put("databaseName", databaseName);
        param.put("tableName", tableName);

        Result<List<GenerateOptions>> result = catchException(url, () -> RestTemplateUtil.get(url, param, APPLICATION_JSON_HEADERS, GENERATE_OPTIONS_LIST), "查询生成代码参数" + ADD_NOTIFY, false);
        return resultData(result);
    }

    public static Map<String, GenerateOptions> queryModuleGenerateOptions(String databaseName, String tableName) {
        List<GenerateOptions> generateOptions = queryGenerateOptions(databaseName, tableName);
        return ListUtils.defaultIfNull(generateOptions, new ArrayList<>()).stream()
                .filter(opt -> ProjectUtils.getCurrProjectName().equalsIgnoreCase(opt.getProject()))
                .collect(Collectors.toMap(GenerateOptions::getModuleName, Function.identity(), (k1, k2) -> k1));
    }

    public static GenerateOptions saveGenerateOptions(GenerateOptions options) {
        String url = Constants.Url.POST_SAVE_GENERATE_OPTIONS;
        Result<GenerateOptions> result = catchException(url, () -> RestTemplateUtil.post(url, options, APPLICATION_JSON_HEADERS, GENERATE_OPTIONS), "保存生成代码参数" + ADD_NOTIFY, false);
        return resultData(result);
    }

    public static GenerateExampleResult generateExample(MySQLTableNode tableNode, String exampleName, String dtoPackage) {
        return generateExample(GenerateExampleDTO.builder().instanceName(((DatabaseNode) tableNode.getParent()).getInstanceName()).databaseName(tableNode.getDatabase()).tableName(tableNode.getTableName()).exampleName(exampleName).examplePackage(dtoPackage).build());
    }

    public static GenerateExampleResult generateExample(GenerateExampleDTO dto) {
        String url = Constants.Url.POST_GENERATE_EXAMPLE;
        Result<GenerateExampleResult> result = catchException(url, () -> RestTemplateUtil.post(url, dto, APPLICATION_JSON_HEADERS, GENERATE_EXAMPLE), "生成Example" + ADD_NOTIFY, false);
        return resultData(result, new GenerateExampleResult());
    }

    public static GenerateJsonEntityResult generateJsonEntity(GenerateJsonEntityDTO dto) {
        String url = Constants.Url.POST_GENERATE_JSON_ENTITY;
        Result<GenerateJsonEntityResult> result = catchException(url, () -> RestTemplateUtil.post(url, dto, APPLICATION_JSON_HEADERS, GENERATE_JSON_ENTITY), "生成Example" + ADD_NOTIFY, false);
        return resultData(result, new GenerateJsonEntityResult());
    }

    public static String convertExtName(boolean useAutogen, String name) {
        if (!useAutogen) return name;

        List<String> suffixList = queryExtSuffixList();
        for (String suffix : suffixList) {
            if (name.endsWith(suffix)) {
                return StringUtils.substringBefore(name, suffix) + "Ext" + suffix;
            }
        }

        return "Ext" + name;
    }

    private static List<String> queryExtSuffixList() {
        String url = Constants.Url.GET_GENERATE_EXT_FILE_SUFFIX;
        String cacheKey = "@queryExtSuffixList";
        Result<List<String>> cache = InnerCacheUtils.getCache(cacheKey, GENERATE_EXT_FILE_SUFFIX);
        if (Objects.nonNull(cache)) {
            return resultData(cache);
        }

        Result<List<String>> result = catchException(url, () -> RestTemplateUtil.get(url, new HashMap<>(), APPLICATION_JSON_HEADERS, GENERATE_EXT_FILE_SUFFIX), "Ext文件尾缀" + ADD_NOTIFY, false);
        if (result.isSuccess() && Objects.nonNull(result.getData()))
            InnerCacheUtils.setCache(cacheKey, JSON.toJson(result), 30 * 60);
        return resultData(result, Lists.newArrayList("DAO", "Dao", "Service", "ServiceImpl", "Facade", "FacadeImpl", "Mapper", "MAPPER"));
    }
}
