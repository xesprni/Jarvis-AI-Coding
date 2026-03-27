package com.qihoo.finance.lowcode.gentracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * 生成选项
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateVelocityOptions implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 统一配置
     */
    private Boolean unifiedConfig;
    /**
     * 重新格式化代码
     */
    private Boolean reFormat;
    /**
     * 提示选是
     */
    private Boolean titleSure;
    /**
     * 提示选否
     */
    private Boolean titleRefuse;
    /**
     * 支持软删除
     */
    private Boolean deletedAt;
    /**
     * 软删除字段名称
     */
    private String deletedAtField;

    private Map<String, Set<String>> otherSetting;

    private String entityPackage;
    private String dtoPackage;
    private String daoPackage;
    private String mapperPackage;
    private String controllerPackage;
    private String servicePackage;
    private String facadePackage;
    private String facadeDomainPackage;
    private String facadeImplPackage;

    private String entityName;
    private String simpleEntityName;
    private String dtoName;
    private String daoName;
    private String mapperName;
    private String controllerName;
    private String serviceName;
    private String facadeName;
    private String facadeImplName;
    // extName
    private String extServiceName;
    private String extDaoName;
    private String extMapperName;

    private String moduleJavaPath;
    private String facadeModuleJavaPath;
    private boolean genService;
    private boolean genController;
    private boolean genFacade;

    private boolean useExample;
    private String exampleName;
    private String exampleContent;
    private String mapperElementContent;
    private String mapperMethodsElementContent;
    private String exampleWhereClause;
    private String updateByExampleWhereClause;
    private String baseColumnList;

    private String moduleBasePath;
    private boolean unUseAutogen;
    private boolean useMsfResponse;
    private boolean extendMsfEntity;
    public static String UNNECESSARY_GENERATE = "_UNNECESSARY_GENERATE";
    // useLombok
    private boolean useLombok;
    private boolean useDtoParam;
}
