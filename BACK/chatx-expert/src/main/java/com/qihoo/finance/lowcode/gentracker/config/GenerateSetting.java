package com.qihoo.finance.lowcode.gentracker.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.qihoo.finance.lowcode.gentracker.entity.ColumnConfigGroup;
import com.qihoo.finance.lowcode.gentracker.entity.GlobalConfigGroup;
import com.qihoo.finance.lowcode.gentracker.entity.TemplateGroup;
import com.qihoo.finance.lowcode.gentracker.entity.TypeMapperGroup;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 配置对象
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
@Data
public class GenerateSetting implements Serializable {
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
}
