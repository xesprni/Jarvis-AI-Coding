package com.qihoo.finance.lowcode.common.action;

public class CommentCodeAction extends BaseQuickAskAction  {

    @Override
    String getPrompt() {
        return "帮我生成代码注释";
    }

    @Override
    String getSelectedTextEmptyTips() {
        return "请先选中代码再点击生成代码注释";
    }
}
