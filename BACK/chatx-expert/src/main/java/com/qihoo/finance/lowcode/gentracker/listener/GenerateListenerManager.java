package com.qihoo.finance.lowcode.gentracker.listener;

/**
 * 文件生成监听器管理类
 *
 * @author fengjinfu-jk
 * date 2023/8/2
 * @version 1.0.0
 * @apiNote EditListener
 */
public class GenerateListenerManager {
    private static boolean readOnly = false;

    public synchronized static void readOnlyStop() {
        readOnly = false;
    }

    public synchronized static void readOnlyStart() {
        readOnly = true;
    }

    public synchronized static boolean isReadOnly() {
//        return readOnly;
        return false;
    }
}
