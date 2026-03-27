package com.qihoo.finance.lowcode.gentracker.entity;

import com.qihoo.finance.lowcode.gentracker.factory.AbstractItemFactory;
import com.qihoo.finance.lowcode.gentracker.tool.CloneUtils;
import com.qihoo.finance.lowcode.gentracker.tool.ReflectionUtils;

import java.util.List;

/**
 * 抽象分组类
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
public interface AbstractGroup<T, E extends AbstractItem<E>> {
    /**
     * 获取分组名称
     *
     * @return 分组名称
     */
    String getName();

    /**
     * 设置分组名称
     *
     * @param name 分组名称
     */
    void setName(String name);

    /**
     * 获取元素集合
     *
     * @return 元素集合
     */
    List<E> getElementList();

    /**
     * 设置元素集合
     *
     * @param elementList 元素集合
     */
    void setElementList(List<E> elementList);

    /**
     * 默认子元素
     *
     * @return {@link E}
     */
    @SuppressWarnings("unchecked")
    default E defaultChild() {
        Class<E> cls = (Class<E>) ReflectionUtils.getGenericClass(this, 1);
        return AbstractItemFactory.createDefaultVal(cls);
    }

    /**
     * 克隆对象
     *
     * @return {@link T}
     */
    @SuppressWarnings("unchecked")
    default T cloneObj() {
        return (T) CloneUtils.cloneByJson(this);
    }
}
