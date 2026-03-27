package com.qihoo.finance.lowcode.gentracker.entity;

import lombok.Data;

import java.util.List;

/**
 * 类型映射分组
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
@Data
public class TypeMapperGroup implements AbstractGroup<TypeMapperGroup, TypeMapper> {
    /**
     * 分组名称
     */
    private String name;
    /**
     * 元素对象
     */
    private List<TypeMapper> elementList;
}
