package com.qihoo.finance.lowcode.editor.action;

import com.qihoo.finance.lowcode.editor.ChatxApplyInlayStrategy;

public class ChatxApplyNextWordInlaysAction extends AbstractChatxApplyInlayAction {

    public static final String ID = "Chatx.applyInlaysNextWord";

    protected ChatxApplyNextWordInlaysAction() {
        super(ChatxApplyInlayStrategy.NEXT_WORD);
    }
}
