package com.qihoo.finance.lowcode.gentracker.service;

import com.intellij.openapi.application.ApplicationManager;
import com.qihoo.finance.lowcode.gentracker.dto.GenerateVelocityOptions;
import com.qihoo.finance.lowcode.gentracker.dto.GenerateRecordDTO;
import com.qihoo.finance.lowcode.gentracker.entity.Callback;
import com.qihoo.finance.lowcode.gentracker.entity.TableInfo;
import com.qihoo.finance.lowcode.gentracker.entity.Template;

import java.util.Collection;
import java.util.Map;

/**
 * 代码生成记录及版本管理
 * 1.用户进行代码生成操作时, 同步生成批次操作记录信息
 * 2.对单次操作产生的每个文件都进行批次详细记录, 并检测当前命名文件是否存在历史生成记录, 存在则版本记录 +1
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
public interface GenerateRecordService extends BaseGenerateService {
    public GenerateRecordDTO recordGenerate(Collection<Template> templates, Collection<TableInfo> tableInfoList, GenerateVelocityOptions generateOptions, Map<String, Object> otherParam);

    public void recordGenerateCode(GenerateRecordDTO generateRecord, TableInfo tableInfo, Template template, Callback callback, String code);

    /**
     * 获取实例对象
     *
     * @return 实例对象
     */
    static GenerateRecordService getInstance() {
        return ApplicationManager.getApplication().getService(GenerateRecordService.class);
    }
}
