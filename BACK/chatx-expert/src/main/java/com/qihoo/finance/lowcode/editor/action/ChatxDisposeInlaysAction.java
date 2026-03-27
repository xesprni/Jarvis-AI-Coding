package com.qihoo.finance.lowcode.editor.action;

import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.project.DumbAware;

public class ChatxDisposeInlaysAction extends EditorAction implements DumbAware, ChatxAction {

    public ChatxDisposeInlaysAction() {
        super(new ChatxDisposeInlaysEditorHandler(null));
        setInjectedContext(true);
    }


}
