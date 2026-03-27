package com.qihoo.finance.lowcode.editor;

import com.intellij.util.messages.Topic;
import groovy.transform.Immutable;

@Immutable
public interface EditorRequestsCancelledMessage {
    Topic<EditorRequestsCancelledMessage> TOPIC = Topic.create("chatx.requestsCancelled"
            , EditorRequestsCancelledMessage.class);

    void requestsCancelled(int count);
}
