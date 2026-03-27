package com.qihoo.finance.lowcode.gentracker.entity;

import com.qihoo.finance.lowcode.gentracker.tool.CloneUtils;

/**
 * 抽象的项
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
public interface AbstractItem<T> {
    /**
     * 默认值
     *
     * @return {@link T}
     */
    T defaultVal();

    /**
     * 克隆对象
     *
     * @return 克隆结果
     */
    @SuppressWarnings("unchecked")
    default T cloneObj() {
        return (T) CloneUtils.cloneByJson(this);
    }
}
