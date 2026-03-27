package com.qihoo.finance.lowcode.gentracker.service;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.qihoo.finance.lowcode.common.entity.dto.AppBaseInfo;
import com.qihoo.finance.lowcode.gentracker.dto.GenerateVelocityOptions;
import com.qihoo.finance.lowcode.gentracker.entity.SaveFile;
import com.qihoo.finance.lowcode.gentracker.entity.TableInfo;
import com.qihoo.finance.lowcode.gentracker.entity.Template;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * 代码生成服务，Project级别Service
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
public interface MySQLGenerateService extends BaseGenerateService {
    /**
     * 获取实例对象
     *
     * @param project 项目对象
     * @return 实例对象
     */
    static MySQLGenerateService getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, MySQLGenerateService.class);
    }

    /**
     * 生成
     *
     * @param appBaseInfo
     * @param templates       模板
     * @param generateOptions 生成选项
     */
    List<SaveFile> generate(AppBaseInfo appBaseInfo, Collection<Template> templates, GenerateVelocityOptions generateOptions);

    /**
     * 生成代码
     *
     * @param template  模板
     * @param tableInfo 表信息对象
     * @return 生成好的代码
     */
    String debugGenerate(Template template, TableInfo tableInfo, String batchNo);

//    void updateDubboProviderXml(PsiFile providerXml, TableInfo tableInfo, GenerateOptions options);
}
