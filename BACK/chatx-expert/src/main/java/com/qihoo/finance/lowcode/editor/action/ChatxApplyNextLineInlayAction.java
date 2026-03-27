package com.qihoo.finance.lowcode.editor.action;

import com.qihoo.finance.lowcode.editor.ChatxApplyInlayStrategy;

public class ChatxApplyNextLineInlayAction extends AbstractChatxApplyInlayAction {

    public static final String ID = "Chatx.applyInlaysNextLine";

    protected ChatxApplyNextLineInlayAction() {
        super(ChatxApplyInlayStrategy.NEXT_LINE);
    }
}
