package com.qihoo.finance.lowcode.editor;

import com.intellij.util.messages.Topic;
import com.qihoo.finance.lowcode.editor.request.EditorRequest;
import org.jetbrains.annotations.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
public interface CompletionsRejectedMessage {
    public static final Topic<CompletionsRejectedMessage> TOPIC = Topic.create("chatx.completionsRejected"
            , CompletionsRejectedMessage.class);

    void automaticCompletionsRejected(@Nullable EditorRequest editorRequest);
}
