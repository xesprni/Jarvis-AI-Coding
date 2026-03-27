package com.qihoo.finance.lowcode.status;

import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface ChatxStatusListener {

    public static final Topic<ChatxStatusListener> TOPIC = Topic.create("chatx.status", ChatxStatusListener.class);

    void onChatxStatus(@NotNull ChatxStatus chatxStatus, @Nullable String paramString);
}
