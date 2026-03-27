package com.qihoo.finance.lowcode.gentracker.factory;

import com.qihoo.finance.lowcode.gentracker.entity.AbstractItem;

import java.lang.reflect.InvocationTargetException;

/**
 * 抽象的项目工厂
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
public class AbstractItemFactory {

    public static <T extends AbstractItem<T>> T createDefaultVal(Class<T> cls) {
        try {
            T instance = cls.getDeclaredConstructor().newInstance();
            return instance.defaultVal();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                 InvocationTargetException e) {
            throw new IllegalArgumentException("构建示例失败", e);
        }
    }

}
