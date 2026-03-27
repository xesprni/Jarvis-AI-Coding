package com.qihoo.finance.lowcode.common.configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SelectModule
 *
 * @author fengjinfu-jk
 * date 2023/10/17
 * @version 1.0.0
 * @apiNote SelectModule
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectModule {
    private String moduleName;
    private String packageName;
    private String entityPackage;
    private String dtoPackage;
    private String daoPackage;
    private String mapperPackage;
    private String servicePackage;
    private String controllerPackage;
    private String facadeModuleName;
    private String facadePackage;
    private String facadeImplPackage;
    private String checkBoxEnable;

    private boolean useExample;
    // default true
    private boolean useAutogen;
    private boolean useLombok;
    private boolean extendMsfEntity;
    private boolean useMsfResponse;
}
