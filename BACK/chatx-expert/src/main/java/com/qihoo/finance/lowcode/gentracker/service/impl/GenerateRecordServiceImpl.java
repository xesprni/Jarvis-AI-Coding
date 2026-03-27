package com.qihoo.finance.lowcode.gentracker.service.impl;

import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.util.RestTemplateUtil;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import com.qihoo.finance.lowcode.gentracker.dto.GenerateFileCodeDTO;
import com.qihoo.finance.lowcode.gentracker.dto.GenerateVelocityOptions;
import com.qihoo.finance.lowcode.gentracker.dto.GenerateRecordDTO;
import com.qihoo.finance.lowcode.gentracker.dto.GenerateTableDTO;
import com.qihoo.finance.lowcode.gentracker.entity.Callback;
import com.qihoo.finance.lowcode.gentracker.entity.TableInfo;
import com.qihoo.finance.lowcode.gentracker.entity.Template;
import com.qihoo.finance.lowcode.gentracker.service.GenerateRecordService;
import com.qihoo.finance.lowcode.gentracker.tool.JSON;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import com.qihoo.finance.lowcode.gentracker.tool.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 代码生成记录及版本管理
 * 1.用户进行代码生成操作时, 同步生成批次操作记录信息
 * 2.对单次操作产生的每个文件都进行批次详细记录, 并检测当前命名文件是否存在历史生成记录, 存在则版本记录 +1
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
@Slf4j
public class GenerateRecordServiceImpl implements GenerateRecordService {

    @Override
    public GenerateRecordDTO recordGenerate(Collection<Template> templates,
                                            Collection<TableInfo> tableInfoList,
                                            GenerateVelocityOptions generateOptions,
                                            Map<String, Object> otherParam) {

        long currentTimeMillis = System.currentTimeMillis();
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(currentTimeMillis));
        String project = ProjectUtils.getCurrProject().getName();
        String operator = getUserEmail();

        List<String> tableNames = tableInfoList.stream().map(TableInfo::getName).collect(Collectors.toList());
        List<String> templateNames = templates.stream().map(Template::getName).collect(Collectors.toList());

        // 生成操作记录
        GenerateRecordDTO generateRecord = GenerateRecordDTO.newInstance(
                date, project, operator, tableNames, templateNames, generateOptions, otherParam);
        // generate batchNo
        String batchNo = UUID.randomUUID().toString().replaceAll("-", "");
        generateRecord.setBatchNo(batchNo);
        // 表操作记录
        List<GenerateTableDTO> selectTables = tableInfoList.stream().map(tableInfo -> {
            String name = tableInfo.getName();
            String saveModelName = tableInfo.getSaveModelName();
            String savePackageName = tableInfo.getSavePackageName();
            String savePath = tableInfo.getSavePath();
            // 所用模板分组编码
            String templateGroupName = StringUtils.isEmpty(tableInfo.getTemplateGroupName()) ? GlobalDict.DEFAULT_GROUP_NAME : tableInfo.getTemplateGroupName();

            return GenerateTableDTO.newInstance(name, saveModelName, savePackageName, savePath, templateGroupName, batchNo, operator, project);
        }).collect(Collectors.toList());
        generateRecord.setSelectTables(selectTables);

//        log.info("user[{}] generates code, current project: {}, tableNames: [{}], templateNames: [{}], options: {}, otherParamJson: {}",
//                operator, project, JSON.toJson(tableNames), JSON.toJson(templateNames), JSON.toJson(generateOptions), JSON.toJson(otherParam));

        // send request
        try {
            String res = RestTemplateUtil.post(Constants.Url.POST_ADD_RECORD, generateRecord, new HashMap<>());
            log.info("recordGenerate url: {}, result: {}", Constants.Url.POST_ADD_RECORD, res);
        } catch (Exception e) {
            log.error("error generate record request, url: {}, errMsg: {}, param: {}", Constants.Url.POST_ADD_RECORD, e.getMessage(), JSON.toJson(generateRecord));
        }
        return generateRecord;
    }

    @Override
    public void recordGenerateCode(GenerateRecordDTO generateRecord, TableInfo tableInfo, Template template, Callback callback, String code) {
        String project = generateRecord.getProject();
        String operator = generateRecord.getOperator();
        String batchNo = generateRecord.getBatchNo();
        String templateName = template.getName();
        String fileName = callback.getFileName();
        String savePath = saveProjectPath(callback.getSavePath());
        String saveModelName = tableInfo.getSaveModelName();
        String tableName = tableInfo.getName();
        String groupName = tableInfo.getTemplateGroupName();

        log.info("user[{}] generates codeFile, current project: {} , tableName: {}, templateName: {}, fileName: {}, savePath: {}",
                operator, project, tableName, templateName, fileName, savePath);
        GenerateFileCodeDTO fileCode = GenerateFileCodeDTO.newInstance(batchNo, tableName, groupName, templateName, fileName, savePath, operator, project, saveModelName, code);

        // send request
        try {
            String res = RestTemplateUtil.post(Constants.Url.POST_ADD_FILE_CODE, fileCode, new HashMap<>());
            log.info("recordGenerateCode url: {}, result: {}", Constants.Url.POST_ADD_FILE_CODE, res);
        } catch (Exception e) {
            log.error("error generate file code request, url: {}, errMsg: {}, param: {}", Constants.Url.POST_ADD_FILE_CODE, e.getMessage(), JSON.toJson(fileCode));
        }
    }

    private String saveProjectPath(String sysPath) {
        String[] paths = sysPath.split(ProjectUtils.getCurrProjectName());
        if (paths.length > 1) {
            return ProjectUtils.getCurrProjectName() + paths[1];
        }

        return sysPath;
    }
}
