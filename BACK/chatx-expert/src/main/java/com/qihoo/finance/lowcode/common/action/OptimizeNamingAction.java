package com.qihoo.finance.lowcode.common.action;

public class OptimizeNamingAction extends BaseQuickAskAction {

    @Override
    String getPrompt() {
        return "帮我优化变量命名";
    }

    @Override
    String getSelectedTextEmptyTips() {
        return "请先选中代码再点击优化变量命名";
    }
}
