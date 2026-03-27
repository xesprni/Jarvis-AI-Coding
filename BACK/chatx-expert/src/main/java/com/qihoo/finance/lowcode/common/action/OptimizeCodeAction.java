package com.qihoo.finance.lowcode.common.action;

public class OptimizeCodeAction extends BaseQuickAskAction {

    @Override
    String getPrompt() {
        return "帮我优化代码";
    }

    @Override
    String getSelectedTextEmptyTips() {
        return "请先选中代码再点击优化代码";
    }
}
