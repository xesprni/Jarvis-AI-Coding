package com.qihoo.finance.lowcode.common.constants;

/**
 * OperateType
 *
 * @author fengjinfu-jk
 * date 2023/11/9
 * @version 1.0.0
 * @apiNote OperateType
 */
public enum OperateType {
    CREATE, EDIT, VIEW, COPY;

    public static boolean isCreate(OperateType type) {
        return CREATE.equals(type);
    }

    public static boolean isEdit(OperateType type) {
        return EDIT.equals(type);
    }

    public static boolean isView(OperateType type) {
        return VIEW.equals(type);
    }

    public static boolean isCopy(OperateType type) {
        return COPY.equals(type);
    }
}
