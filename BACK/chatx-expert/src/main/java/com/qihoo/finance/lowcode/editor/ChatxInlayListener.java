package com.qihoo.finance.lowcode.editor;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Inlay;
import com.intellij.util.messages.Topic;
import com.qihoo.finance.lowcode.editor.request.EditorRequest;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ChatxInlayListener {

    public static final Topic<ChatxInlayListener> TOPIC = Topic.create("chatx.inlaysUpdate"
            , ChatxInlayListener.class);

    void inlaysUpdated(@NotNull EditorRequest request, @NotNull InlayDisposeContext context, @NotNull Editor editor
            , @NotNull List<Inlay<ChatxInlayRenderer>> insertedInlays);
}
