package com.qihoo.finance.lowcode.gentracker.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.qihoo.finance.lowcode.apitrack.entity.JsonFormNode;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.entity.dto.generate.GenerateJsonEntityDTO;
import com.qihoo.finance.lowcode.common.entity.dto.generate.GenerateJsonEntityResult;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.common.util.NotifyUtils;
import com.qihoo.finance.lowcode.common.util.RestTemplateUtil;
import com.qihoo.finance.lowcode.common.utils.TimeUtils;
import com.qihoo.finance.lowcode.common.utils.VelocityUtils;
import com.qihoo.finance.lowcode.design.entity.DatabaseNode;
import com.qihoo.finance.lowcode.design.entity.MongoCollectionNode;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import com.qihoo.finance.lowcode.gentracker.dto.GenerateRecordDTO;
import com.qihoo.finance.lowcode.gentracker.dto.GenerateVelocityOptions;
import com.qihoo.finance.lowcode.gentracker.dto.MongoCollection;
import com.qihoo.finance.lowcode.gentracker.entity.Callback;
import com.qihoo.finance.lowcode.gentracker.entity.SaveFile;
import com.qihoo.finance.lowcode.gentracker.entity.TableInfo;
import com.qihoo.finance.lowcode.gentracker.entity.Template;
import com.qihoo.finance.lowcode.gentracker.service.GenerateRecordService;
import com.qihoo.finance.lowcode.gentracker.service.MongoGenerateService;
import com.qihoo.finance.lowcode.gentracker.tool.*;
import com.qihoo.finance.lowcode.gentracker.util.GenerateTrackUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static com.qihoo.finance.lowcode.gentracker.dto.GenerateVelocityOptions.UNNECESSARY_GENERATE;

/**
 * 代码生成实现
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
@Slf4j
public class MongoGenerateServiceImpl implements MongoGenerateService {
    /**
     * 项目对象
     */
    private final Project project;
    /**
     * 模型管理
     */
    private final ModuleManager moduleManager;
    /**
     * 缓存数据工具
     */
    private final DataContext dataContext;
    /**
     * 代码生成操作记录
     */
    private final GenerateRecordService generateRecordService;

    public MongoGenerateServiceImpl(Project project) {
        this.project = project;
        this.moduleManager = ModuleManager.getInstance(project);
        this.dataContext = DataContext.getInstance(project);
        this.generateRecordService = GenerateRecordService.getInstance();
    }

    /**
     * 生成
     *
     * @param templates       模板
     * @param generateOptions 生成选项
     */
    @Override
    public List<SaveFile> generate(MongoCollection collection, Collection<Template> templates, GenerateVelocityOptions generateOptions) {
        // 气泡提醒, 在NotificationGroup中添加消息通知内容，以及消息类型。这里为MessageType.INFO
        NotifyUtils.notify("生成代码中, 请稍等...", NotificationType.INFORMATION);
        // 生成代码
        return executeGenerate(collection, templates, generateOptions);
    }

    /**
     * 生成代码，并自动保存到对应位置
     */
    public List<SaveFile> executeGenerate(MongoCollection collection, Collection<Template> templates, GenerateVelocityOptions generateOptions) {
        List<SaveFile> saveFiles = new ArrayList<>();
        TableInfo convertTable = convert(collection);
        GenerateRecordDTO generateRecord = generateRecordService.recordGenerate(templates, Lists.newArrayList(convertTable), generateOptions, new HashMap<>());
        // 构建参数
        List<String> relativePaths = new ArrayList<>();
        Map<String, Object> param = getDefaultParam();
        // 表信息对象
        param.put("collection", collection);
        // 设置模型路径与导包列表
        param.put("options", generateOptions);
        // 处理模板，注入全局变量（克隆一份，防止篡改）
        templates = CloneUtils.cloneByJson(templates, new TypeReference<ArrayList<Template>>() {
        });
        // 添加全局配置, 将不同全局配置模板代码块替换掉模板中预设的占位符
        TemplateUtils.addGlobalConfig(templates);
        // pk type
        JsonFormNode raw = JSON.parse(collection.getJsonRaw(), JsonFormNode.class);
        JsonFormNode node = raw.getProperties().get("id");
        String pkType = getShortType(node, "String");
        param.put("mongo_pk_type", pkType);

        // generate jsonEntity
        generateJsonEntity(collection, generateOptions, param);
        for (Template template : templates) {
            if (!selected(template, generateOptions)) continue;
            String tableName = collection.getObj().getTableName();
            String templateMeta = ExtraCodeGenerateUtils.copyrightMeta("template", template.getName() + " v" + template.getVersion());
            String tableNameMeta = ExtraCodeGenerateUtils.copyrightMeta("table", tableName);
            // 设置文件Copyright信息
            param.put("Copyright", ExtraCodeGenerateUtils.genCopyright(
                    GlobalDict.COMPANY,
                    ProjectUtils.getCurrProjectName(),
                    getUserEmail(),
                    generateRecord.getBatchNo(),
                    tableNameMeta,
                    templateMeta
            ));

            Callback callback = new Callback();
            // 默认名称
            callback.setFileName(tableName + "Default.java");
            callback.setReformat(generateOptions.getReFormat());
            // 设置回调对象
            param.put("callback", callback);
            callback.setWriteFile(true);
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
            generateRecordService.recordGenerateCode(generateRecord, convertTable, template, callback, code);
        }

        // 记录
        catchSaveTableGenCodeLog(relativePaths, generateRecord);
        return saveFiles;
    }

    private TableInfo convert(MongoCollection collection) {
        TableInfo tableInfo = new TableInfo();
        tableInfo.setTemplateGroupName(collection.getTemplateGroup());
        tableInfo.setSaveModelName(collection.getModuleName());
        tableInfo.setName(collection.getObj().getTableName());

        return tableInfo;
    }

    private static String getShortType(JsonFormNode node, String defaultType) {
        if (Objects.isNull(node)) return defaultType;

        if (org.apache.commons.lang3.StringUtils.isNotEmpty(node.getType())) {
            String[] split = node.getType().split("\\.");
            return split[split.length - 1];
        }

        return defaultType;
    }

    private void generateJsonEntity(MongoCollection collection, GenerateVelocityOptions generateOptions, Map<String, Object> param) {
        GenerateJsonEntityDTO dto = new GenerateJsonEntityDTO();
        dto.setEntityName(generateOptions.getEntityName());
        dto.setDtoName(generateOptions.getDtoName());
        dto.setFacadeInputName(generateOptions.getSimpleEntityName() + "Input");
        dto.setFacadeOutputName(generateOptions.getSimpleEntityName() + "Output");
        dto.setUseLombok(generateOptions.isUseLombok());
        dto.setJson(collection.getJson());
        dto.setJsonRaw(collection.getJsonRaw());
        dto.setTemplateGroup(collection.getTemplateGroup());

        GenerateJsonEntityResult result = GenerateTrackUtils.generateJsonEntity(dto);
        param.put("mongo_entity_content", result.getEntity().getContent());
        param.put("mongo_entity_import", result.getEntity().getImportContent());

        param.put("mongo_dto_content", result.getDto().getContent());
        param.put("mongo_dto_import", result.getDto().getImportContent());

        param.put("mongo_facade_input_content", result.getFacadeInput().getContent());
        param.put("mongo_facade_input_import", result.getFacadeInput().getImportContent());

        param.put("mongo_facade_output_content", result.getFacadeOutput().getContent());
        param.put("mongo_facade_output_import", result.getFacadeOutput().getImportContent());
    }

    private void catchSaveTableGenCodeLog(List<String> relativePaths, GenerateRecordDTO generateRecord) {
        try {
            saveTableGenCodeLog(relativePaths, generateRecord);
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

    private void saveTableGenCodeLog(List<String> relativePaths, GenerateRecordDTO generateRecord) {
        // 表生成记录 @liuzhenghua-jk
        String javaFilePath = "src/main/java";
        String resourceFilePath = "src/main/resources";
        relativePaths = relativePaths.stream().map(path -> {
            String basePath = org.apache.commons.lang3.StringUtils.defaultString(project.getBasePath(), " ");
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

        DatabaseNode dbNode = dataContext.getSelectDatabase();
        MongoCollectionNode collection = dataContext.getSelectMongoCollection();

        // send request
        Map<Object, Object> saveLogParam = new HashMap<>();
        String url = Constants.Url.POST_SAVE_TABLE_GEN_CODE_LOG;
        saveLogParam.put("dataSourceType", Constants.DataSource.MongoDB);
        saveLogParam.put("instanceName", dbNode.getNodeAttr().get("instanceName"));
        saveLogParam.put("databaseName", dbNode.getName());
        saveLogParam.put("tableName", collection.getTableName());
        saveLogParam.put("batchNo", generateRecord.getBatchNo());
        saveLogParam.put("projectModule", generateRecord.getProject());
        saveLogParam.put("fileRelativePath", org.apache.commons.lang3.StringUtils.join(relativePaths, ","));
        RestTemplateUtil.post(url, saveLogParam, new HashMap<>(), new TypeReference<Result<?>>() {
        });
    }

    public void setReadOnly(String path) {
        File file = new File(path);
        if (file.isFile()) {
            boolean readOnly = file.setReadOnly();
            log.info("file set readOnly: {}, {}\n", readOnly, path);
        }
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
}
