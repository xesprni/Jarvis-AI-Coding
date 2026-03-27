package com.qihoo.finance.lowcode.editor;

public enum CompletionRequestType {

    Automatic,
    Forced,
    Manual,
    /** 注释生成代码 */
    GENERATE_CODE_BY_COMMENT;

    public boolean isAutomaticOrForced() {
        return (this == Automatic || this == Forced);
    }

    /**
     * 强制刷新或手动触发，此种类型不会判断请求是否重复
     * @return
     */
    public boolean isForcedOrManual() {
        return (this == Forced || this == Manual || this == GENERATE_CODE_BY_COMMENT);
    }

    /**
     * 是否是非强制刷新的请求类型，此种类型会走缓存
     * @return
     */
    public boolean isUnforced() {
        return (this == Automatic);
    }
}
