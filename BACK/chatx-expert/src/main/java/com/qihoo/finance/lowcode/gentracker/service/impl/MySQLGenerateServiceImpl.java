package com.qihoo.finance.lowcode.gentracker.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.entity.dto.AppBaseInfo;
import com.qihoo.finance.lowcode.common.entity.dto.generate.GenerateExampleResult;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.common.util.NotifyUtils;
import com.qihoo.finance.lowcode.common.util.RestTemplateUtil;
import com.qihoo.finance.lowcode.common.utils.TimeUtils;
import com.qihoo.finance.lowcode.common.utils.VelocityUtils;
import com.qihoo.finance.lowcode.design.entity.DatabaseColumnNode;
import com.qihoo.finance.lowcode.design.entity.DatabaseNode;
import com.qihoo.finance.lowcode.design.entity.MySQLTableNode;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import com.qihoo.finance.lowcode.gentracker.dto.GenerateRecordDTO;
import com.qihoo.finance.lowcode.gentracker.dto.GenerateVelocityOptions;
import com.qihoo.finance.lowcode.gentracker.entity.*;
import com.qihoo.finance.lowcode.gentracker.service.GenerateRecordService;
import com.qihoo.finance.lowcode.gentracker.service.MySQLGenerateService;
import com.qihoo.finance.lowcode.gentracker.service.TableInfoSettingsService;
import com.qihoo.finance.lowcode.gentracker.tool.*;
import com.qihoo.finance.lowcode.gentracker.ui.dialog.MySQLGenerateDialog;
import com.qihoo.finance.lowcode.gentracker.util.GenerateTrackUtils;
import com.qihoo.finance.lowcode.gentracker.util.IoUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.qihoo.finance.lowcode.common.constants.Constants.Encrypt.ENCRYPT;
import static com.qihoo.finance.lowcode.common.constants.Constants.Encrypt.MD5X;
import static com.qihoo.finance.lowcode.gentracker.dto.GenerateVelocityOptions.UNNECESSARY_GENERATE;

/**
 * 代码生成实现
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
@Slf4j
public class MySQLGenerateServiceImpl implements MySQLGenerateService {
    /**
     * 项目对象
     */
    private final Project project;
    /**
     * 模型管理
     */
    private final ModuleManager moduleManager;
    /**
     * 表信息服务
     */
    private final TableInfoSettingsService tableInfoService;
    /**
     * 缓存数据工具
     */
    private final DataContext dataContext;
    /**
     * 代码生成操作记录
     */
    private final GenerateRecordService generateRecordService;
    /**
     * 导入包时过滤的包前缀
     */
    private static final String FILTER_PACKAGE_NAME = "java.lang";

    public MySQLGenerateServiceImpl(Project project) {
        this.project = project;
        this.moduleManager = ModuleManager.getInstance(project);
        this.tableInfoService = TableInfoSettingsService.getInstance();
        this.dataContext = DataContext.getInstance(project);
        this.generateRecordService = GenerateRecordService.getInstance();
    }

    /**
     * 生成
     *
     * @param appBaseInfo
     * @param templates       模板
     * @param generateOptions 生成选项
     */
    @Override
    public List<SaveFile> generate(AppBaseInfo appBaseInfo, Collection<Template> templates, GenerateVelocityOptions generateOptions) {
        // 获取选中表信息
        List<TableInfo> tableInfoList = settingTableInfoList(generateOptions);
        if (CollectionUtil.isEmpty(tableInfoList)) {
            return new ArrayList<>();
        }

        // 生成代码
        return generate(appBaseInfo, templates, tableInfoList, generateOptions, null);
    }

    private List<TableInfo> settingTableInfoList(GenerateVelocityOptions generateOptions) {
        // 外部数据源更新后, 读取缓存可能会有数据仍然为旧数据的情况, 需要实时读取
        TableInfo selectedTableInfo = tableInfoService.getMemoryTableInfo(dataContext.getSelectDbTable(false));
        MySQLGenerateDialog.setEntityColumns(selectedTableInfo);

        // 批量时
        List<TableInfo> tableInfoList = dataContext.getDbTableList(false).stream().map(tableInfoService::getMemoryTableInfo).collect(Collectors.toList());
        for (TableInfo tableInfo : tableInfoList) {
            MySQLGenerateDialog.setEntityColumns(tableInfo);
        }

        // 校验选中表的保存路径是否正确
        if (StringUtils.isEmpty(selectedTableInfo.getSavePath())) {
            if (selectedTableInfo.getObj() != null) {
                log.warn(selectedTableInfo.getObj().getTableName() + "表配置信息不正确，请尝试重新配置");
            } else {
                log.warn("Path配置信息不正确，请尝试重新配置");
            }

            return new ArrayList<>();
        }

        // 将未配置的表进行配置覆盖
        tableInfoList.forEach(tableInfo -> {
            if (StringUtils.isEmpty(tableInfo.getSavePath())) {
                tableInfo.setSaveModelName(selectedTableInfo.getSaveModelName());
                tableInfo.setSavePackageName(selectedTableInfo.getSavePackageName());
                tableInfo.setSavePath(selectedTableInfo.getSavePath());
                tableInfo.setPreName(selectedTableInfo.getPreName());
                tableInfoService.saveTableInfo(tableInfo);
            }
        });
        // 如果使用统一配置，直接全部覆盖
        if (Boolean.TRUE.equals(generateOptions.getUnifiedConfig())) {
            tableInfoList.forEach(tableInfo -> {
                tableInfo.setSaveModelName(selectedTableInfo.getSaveModelName());
                tableInfo.setSavePackageName(selectedTableInfo.getSavePackageName());
                tableInfo.setSavePath(selectedTableInfo.getSavePath());
                tableInfo.setPreName(selectedTableInfo.getPreName());
            });
        }

        return tableInfoList;
    }

    /**
     * 生成代码，并自动保存到对应位置
     *
     * @param templates       模板
     * @param tableInfoList   表信息对象
     * @param generateOptions 生成配置
     * @param otherParam      其他参数
     */
    public List<SaveFile> generate(AppBaseInfo appBaseInfo, Collection<Template> templates, Collection<TableInfo> tableInfoList, GenerateVelocityOptions generateOptions, Map<String, Object> otherParam) {
        List<SaveFile> saveFiles = new ArrayList<>();
        // 处理模板，注入全局变量（克隆一份，防止篡改）
        templates = CloneUtils.cloneByJson(templates, new com.fasterxml.jackson.core.type.TypeReference<ArrayList<Template>>() {
        });

        // 添加全局配置, 将不同全局配置模板代码块替换掉模板中预设的占位符
        TemplateUtils.addGlobalConfig(templates);
        // 生成代码并记录, 调用APP用户代码生成操作记录(用户信息，项目信息，代码信息等)
        GenerateRecordDTO generateRecord = generateRecordService.recordGenerate(templates, tableInfoList, generateOptions, otherParam);

        // applicationName & applicationPath
        String applicationName = appBaseInfo.getAppPackage();

        // 气泡提醒, 在NotificationGroup中添加消息通知内容，以及消息类型。这里为MessageType.INFO
        NotifyUtils.notify("生成代码中, 请稍等...", NotificationType.INFORMATION);
        for (TableInfo tableInfo : tableInfoList) {
            // 构建参数
            Map<String, Object> param = getDefaultParam();
            // 其他参数
            if (otherParam != null) {
                param.putAll(otherParam);
            }
            // 所有表信息对象
            // 应用包名, 如 lingxi.server
            param.put("applicationName", applicationName);
            // 应用包名转路径, 如 lingxi/server
            param.put("applicationPath", applicationName.replaceAll("\\.", "/"));
            // 标准包前缀, 如 autogen
            param.put("packagePrefix", appBaseInfo.getAutogenPackageSuffix());
            // 拓展包前缀, 如 extension
            param.put("extensionPrefix", appBaseInfo.getExtensionPackageSuffix());
            // baseEntity
            if (tableInfo.getFullColumn().size() == tableInfo.getEntityColumn().size()) {
                param.put("baseEntity", "");
            } else {
                param.put("baseEntity", " extends BaseEntity");
            }

            // 加密字段
            param.put("encryptFields", analyzeEncrypt(tableInfo));

            // 所有表信息对象
            param.put("tableInfoList", tableInfoList);
            // 表信息对象
            param.put("tableInfo", tableInfo);
            // 设置模型路径与导包列表
            setModulePathAndImportList(param, tableInfo);
            // 设置额外代码生成服务
            param.put("generateService", new ExtraCodeGenerateUtils(this, tableInfo, generateOptions));

            // check options
            param.put("options", generateOptions);
            // 设置文件Copyright信息
            // remove autogen/extension if not in tech.qifu.lowcode
            removePackageIfNoUseRootPackage(appBaseInfo, tableInfo, param);
            // generate example file
            generateExampleFile(tableInfo, generateRecord, generateOptions);

            List<String> relativePaths = new ArrayList<>();
            for (Template template : templates) {
                if (!selected(template, generateOptions)) continue;

                String tableNameMeta = ExtraCodeGenerateUtils.copyrightMeta("table", tableInfo.getName());
                String templateMeta = ExtraCodeGenerateUtils.copyrightMeta("template", template.getName() + " v" + template.getVersion());
                String copyright = ExtraCodeGenerateUtils.genCopyright(GlobalDict.COMPANY, ProjectUtils.getCurrProjectName(), getUserEmail(), generateRecord.getBatchNo(), tableNameMeta, templateMeta);

                param.put("Copyright", copyright);

                Callback callback = new Callback();
                callback.setWriteFile(true);
                callback.setReformat(generateOptions.getReFormat());
                // 默认名称
                callback.setFileName(tableInfo.getName() + "Default.java");
                // 默认路径
                callback.setSavePath(tableInfo.getSavePath());
                // 设置回调对象
                param.put("callback", callback);
                // 开始生成
                String code = VelocityUtils.generate(template.getCode(), param);
                if (StringUtils.isEmpty(code)) continue;
                if (code.contains(UNNECESSARY_GENERATE)) continue;

                // 设置一个默认保存路径与默认文件名
                String path = callback.getSavePath();
                path = path.replace("\\", "/");
                // 针对相对路径进行处理
                if (path.startsWith(".")) {
                    path = project.getBasePath() + path.substring(1);
                }
                callback.setSavePath(path);
                relativePaths.add(path + "/" + callback.getFileName());
                SaveFile saveFile = new SaveFile(project, code, callback, generateOptions);
                saveFile.setTemplate(template);
                saveFiles.add(saveFile);

                // 调用APP用户代码生成操作记录(用户信息，项目信息，代码信息)
                generateRecordService.recordGenerateCode(generateRecord, tableInfo, template, callback, code);
            }

            // 记录
            catchSaveTableGenCodeLog(tableInfo, relativePaths, generateRecord);
        }

        return saveFiles;
    }

    private String formatMapperContent(TableInfo tableInfo, String mapperElementContent) {
        // 简单处理
        // BaseResultMap 需替换为具体的 TableMap
        if (StringUtils.isEmpty(mapperElementContent)) return mapperElementContent;
        return mapperElementContent.replaceAll("BaseResultMap", tableInfo.getName() + "Map");
    }

    @SneakyThrows
    private void generateExampleFile(TableInfo tableInfo, GenerateRecordDTO generateRecord, GenerateVelocityOptions generateOptions) {
        // generate example content
        String simpleEntityName = generateOptions.getSimpleEntityName();
        GenerateExampleResult exampleResult = GenerateTrackUtils.generateExample(dataContext.getSelectDbTable(), simpleEntityName, generateOptions.getDtoPackage());
        // setting example options
        generateOptions.setExampleName(simpleEntityName + "Example");
        generateOptions.setExampleContent(exampleResult.getExampleContent());
        String mapperContent = formatMapperContent(tableInfo, exampleResult.getMapperElementContent());

        // setting example mapper options
        generateOptions.setMapperElementContent(mapperContent);
        generateOptions.setMapperMethodsElementContent(formatMapperContent(tableInfo, exampleResult.getMapperMethodsElementContent()));
        generateOptions.setExampleWhereClause(formatMapperContent(tableInfo, exampleResult.getMapperElement().getExampleWhereClause()));
        generateOptions.setUpdateByExampleWhereClause(formatMapperContent(tableInfo, exampleResult.getMapperElement().getUpdateByExampleWhereClause()));
        generateOptions.setBaseColumnList(formatMapperContent(tableInfo, exampleResult.getMapperElement().getBaseColumnList()));

        if (!generateOptions.isUseExample()) return;
        // generate example file
        String tableNameMeta = ExtraCodeGenerateUtils.copyrightMeta("table", tableInfo.getName());
        String templateMeta = ExtraCodeGenerateUtils.copyrightMeta("template", "Example");
        String copyright = ExtraCodeGenerateUtils.genCopyright(GlobalDict.COMPANY, ProjectUtils.getCurrProjectName(), getUserEmail(), generateRecord.getBatchNo(), tableNameMeta, templateMeta);
        String exampleName = generateOptions.getExampleName();
        String examplePackage = generateOptions.getDtoPackage();
        String exampleContent = copyright + "\n\n" + generateOptions.getExampleContent();

        String savePath = generateOptions.getModuleBasePath() + "/" + examplePackage.replaceAll("\\.", "/");
        Path directoryPath = Paths.get(savePath);
        File directory = directoryPath.toFile();
        if (!directory.exists() && !directory.mkdirs()) return;

        File javaFile = new File(directory, exampleName + ".java");
        if (!javaFile.exists() && !javaFile.createNewFile()) return;

        IoUtils.writeFile(javaFile, exampleContent, true);
    }

    private void catchSaveTableGenCodeLog(TableInfo tableInfo, List<String> relativePaths, GenerateRecordDTO generateRecord) {
        try {
            saveTableGenCodeLog(tableInfo, relativePaths, generateRecord);
        } catch (Exception e) {
            log.error("saveTableGenCodeLog error:\n" + ExceptionUtils.getStackTrace(e));
        }
    }

    private boolean selected(Template template, GenerateVelocityOptions generateOptions) {
        // 判定模板是否属于选中， options中可以确定选中的生成文件类型
        String templateName = template.getName();
        if (Constants.TemplateTag.SERVICE.contains(templateName)) {
            return generateOptions.isGenService();
        }
        if (Constants.TemplateTag.CONTROLLER.contains(templateName)) {
            return generateOptions.isGenController();
        }
        if (Constants.TemplateTag.FACADE.contains(templateName)) {
            return generateOptions.isGenFacade();
        }

        return true;
    }

    private void saveTableGenCodeLog(TableInfo tableInfo, List<String> relativePaths, GenerateRecordDTO generateRecord) {
        String javaFilePath = "src/main/java";
        String resourceFilePath = "src/main/resources";
        relativePaths = relativePaths.stream().map(path -> {
            String basePath = project.getBasePath();
            if (path.startsWith(basePath)) {
                path = path.substring(basePath.length());
            }
            int index = path.indexOf(javaFilePath);
            if (index != -1) {
                path = path.substring(index + javaFilePath.length());
            }
            index = path.indexOf(resourceFilePath);
            if (index != -1) {
                path = path.substring(index + resourceFilePath.length());
            }
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            return path;
        }).collect(Collectors.toList());
        // send request
        Map<Object, Object> saveLogParam = new HashMap<>();
        String url = Constants.Url.POST_SAVE_TABLE_GEN_CODE_LOG;
        MySQLTableNode tableNode = tableInfo.getObj();
        DatabaseNode dbNode = (DatabaseNode) tableInfo.getObj().getParent();
        saveLogParam.put("dataSourceType", dbNode.getDataSourceType());
        saveLogParam.put("instanceName", dbNode.getNodeAttr().get("instanceName"));
        saveLogParam.put("databaseName", tableNode.getDatabase());
        saveLogParam.put("tableName", tableNode.getTableName());
        saveLogParam.put("batchNo", generateRecord.getBatchNo());
        saveLogParam.put("projectModule", generateRecord.getProject());
        saveLogParam.put("fileRelativePath", org.apache.commons.lang3.StringUtils.join(relativePaths, ","));
        RestTemplateUtil.post(url, saveLogParam, new HashMap<>(), new TypeReference<Result<?>>() {
        });
    }

    private void removePackageIfNoUseRootPackage(AppBaseInfo appBaseInfo, TableInfo tableInfo, Map<String, Object> param) {
        String appPackage = appBaseInfo.getRootPackage();
        // 用户修改了生成包路径, 移除 autogen/extension等信息
        if (!appPackage.equals(tableInfo.getSavePackageName())) {
            param.remove("applicationName");
            param.remove("applicationPath");
            param.remove("packagePrefix");
            param.remove("extensionPrefix");

            param.put("autogen", appBaseInfo.getAutogenPackageSuffix());
        }

    }

    private List<ColumnInfo> analyzeEncrypt(TableInfo tableInfo) {
        List<ColumnInfo> fullColumn = tableInfo.getFullColumn();
        Map<String, ColumnInfo> encryptColumnMap = new HashMap<>();
        Map<String, ColumnInfo> md5xColumnMap = new HashMap<>();
        for (ColumnInfo columnInfo : fullColumn) {
            String columnName = columnInfo.getName();
            if (columnName.endsWith(ENCRYPT)) {
                encryptColumnMap.put(columnName.substring(0, columnName.lastIndexOf(ENCRYPT)), columnInfo);
            }
            if (columnInfo.getName().endsWith(MD5X)) {
                md5xColumnMap.put(columnName.substring(0, columnName.lastIndexOf(MD5X)), columnInfo);
            }
        }

        List<ColumnInfo> encryptColumns = new ArrayList<>();
        for (String encryptColumnName : encryptColumnMap.keySet()) {
            if (md5xColumnMap.containsKey(encryptColumnName)) {
                // 成对出现则为加密字段
                ColumnInfo encryptColumn = new ColumnInfo();
                encryptColumns.add(encryptColumn);

                encryptColumn.setName(encryptColumnName);
                encryptColumn.setType("String");
                String comment = org.apache.commons.lang3.StringUtils.defaultString(encryptColumnMap.get(encryptColumnName).getComment(), md5xColumnMap.get(encryptColumnName).getComment());
                encryptColumn.setComment(comment + "(明文字段)");
            }
        }

        return encryptColumns;
    }

    private String baseEntity(TableInfo tableInfo) {
        // analyze should extend BaseEntity
        Map<String, String> baseEntity = Constants.DB_COLUMN.MIN_BASE_ENTITY;
        List<ColumnInfo> fullColumn = tableInfo.getFullColumn();
        int matchCount = 0;
        for (ColumnInfo columnInfo : fullColumn) {
            DatabaseColumnNode dbColumn = columnInfo.getObj();
            if (baseEntity.containsKey(dbColumn.getFieldName())) {
                String fieldTypes = baseEntity.get(dbColumn.getFieldName());
                for (String fieldType : fieldTypes.split("\\|")) {
                    if (dbColumn.getFieldType().contains(fieldType)) {
                        matchCount++;
                        break;
                    }
                }
            }
        }

        if (matchCount == baseEntity.size()) {
            return " extends BaseEntity";
        }

        return "";
    }

    /**
     * 生成代码
     *
     * @param template  模板
     * @param tableInfo 表信息对象
     * @return 生成好的代码
     */
    @Override
    public String debugGenerate(Template template, TableInfo tableInfo, String batchNo) {
        // 获取默认参数
        Map<String, Object> param = getDefaultParam();
        // 表信息对象，进行克隆，防止篡改
        param.put("tableInfo", tableInfo);
        // 设置模型路径与导包列表
        setModulePathAndImportList(param, tableInfo);
        // 处理模板，注入全局变量
        TemplateUtils.addGlobalConfig(template);
        // 设置文件Copyright信息
        String copyright = ExtraCodeGenerateUtils.genCopyright(GlobalDict.COMPANY, ProjectUtils.getCurrProjectName(), getUserEmail(), batchNo);
        param.put("Copyright", copyright);

        param.put("applicationName", "application");
        // 应用包名转路径, 如 lingxi/server
        param.put("applicationPath", "application".replaceAll("\\.", "/"));
        // 标准包前缀, 如 autogen
        param.put("packagePrefix", "autogen");
        // 拓展包前缀, 如 extension
        param.put("extensionPrefix", "extension");
        // baseEntity
        String baseEntity = baseEntity(tableInfo);
        param.put("baseEntity", baseEntity);
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(baseEntity)) {
            List<ColumnInfo> entityColumns = tableInfo.getFullColumn().stream().filter(c -> !Constants.DB_COLUMN.BASE_ENTITY.containsKey(c.getObj().getFieldName())).collect(Collectors.toList());
            tableInfo.setEntityColumn(entityColumns);
        } else {
            tableInfo.setEntityColumn(tableInfo.getFullColumn());
        }

        return VelocityUtils.generate(template.getCode(), param);
    }

    /**
     * 设置模型路径与导包列表
     *
     * @param param     参数
     * @param tableInfo 表信息对象
     */
    private void setModulePathAndImportList(Map<String, Object> param, TableInfo tableInfo) {
        Module module = null;
        if (!StringUtils.isEmpty(tableInfo.getSaveModelName())) {
            module = this.moduleManager.findModuleByName(tableInfo.getSaveModelName());
        }
        if (module != null) {
            // 设置modulePath
            param.put("modulePath", ModuleUtils.getModuleDir(module).getPath());
        }
        // 设置要导入的包
        param.put("importList", getImportList(tableInfo));
    }

    /**
     * 获取默认参数
     *
     * @return 参数
     */
    private Map<String, Object> getDefaultParam() {
        // 系统设置
        Map<String, Object> param = new HashMap<>(20);
        // 作者
        param.put("author", getUserEmail());
        //工具类
        param.put("tool", GlobalTool.getInstance());
        param.put("time", TimeUtils.getInstance());
        // 项目路径
        param.put("projectPath", project.getBasePath());
        return param;
    }

    /**
     * 获取导入列表
     *
     * @param tableInfo 表信息对象
     * @return 导入列表
     */
    private Set<String> getImportList(TableInfo tableInfo) {
        // 创建一个自带排序的集合
        Set<String> result = new TreeSet<>();
        tableInfo.getFullColumn().forEach(columnInfo -> {
            if (!columnInfo.getType().startsWith(FILTER_PACKAGE_NAME)) {
                String type = NameUtils.getInstance().getClsFullNameRemoveGeneric(columnInfo.getType());
                result.add(type);
            }
        });
        return result;
    }
}
