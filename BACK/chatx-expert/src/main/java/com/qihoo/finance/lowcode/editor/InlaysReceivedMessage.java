package com.qihoo.finance.lowcode.editor;

import com.intellij.util.messages.Topic;
import com.qihoo.finance.lowcode.editor.completions.ChatxInlayList;
import com.qihoo.finance.lowcode.editor.request.EditorRequest;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface InlaysReceivedMessage {
    Topic<InlaysReceivedMessage> TOPIC = Topic.create("chatx.inlaysReceived", InlaysReceivedMessage.class);

    void inlaysReceived(@NotNull EditorRequest paramEditorRequest, @NotNull List<ChatxInlayList> paramList);
}
