package com.qihoo.finance.lowcode.common.action;

public class ExplainCodeAction extends BaseQuickAskAction  {

    @Override
    String getPrompt() {
        return "帮我解释代码";
    }

    @Override
    String getSelectedTextEmptyTips() {
        return "请先选中代码再点击解释代码";
    }
}
