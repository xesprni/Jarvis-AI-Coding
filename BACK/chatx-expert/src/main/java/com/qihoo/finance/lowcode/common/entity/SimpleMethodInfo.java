package com.qihoo.finance.lowcode.common.entity;

import lombok.Data;

/**
 * PsiInfo
 *
 * @author fengjinfu-jk
 * date 2024/8/12
 * @version 1.0.0
 * @apiNote PsiInfo
 */
@Data
public class SimpleMethodInfo {
    private String className;
    private String methodName;

    public static SimpleMethodInfo of(String className, String methodName) {
        SimpleMethodInfo info = new SimpleMethodInfo();
        info.setClassName(className);
        info.setMethodName(methodName);
        return info;
    }
}
