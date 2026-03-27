package com.qihoo.finance.lowcode.gentracker.entity;

import lombok.Data;

import java.util.List;

/**
 * 模板分组类
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
@Data
public class TemplateGroup implements AbstractGroup<TemplateGroup, Template> {
    /**
     * 分组名称
     */
    private String name;
    /**
     * 元素对象
     */
    private List<Template> elementList;
}
