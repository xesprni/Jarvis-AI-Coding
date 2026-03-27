package com.qihoo.finance.lowcode.status;

import com.intellij.openapi.util.NlsContexts;
import com.intellij.util.ui.PresentableEnum;
import com.qihoo.finance.lowcode.common.util.ChatxBundle;
import com.qihoo.finance.lowcode.common.util.Icons;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public enum ChatxStatus implements PresentableEnum {

    Ready, NotSignedIn, CompletionInProgress, AgentWarning, AgentError, AgentBroken, IncompatibleClient, Unsupported, UnknownError;

    public boolean isIconAlwaysShown() {
        return (this != Ready && this != NotSignedIn && this != CompletionInProgress);
    }

    public boolean isDisablingClientRequests() {
        return this == IncompatibleClient
                || this == AgentBroken
                || this == NotSignedIn;
    }

    @NotNull
    public Icon getIcon() {
        switch (this) {
            case NotSignedIn:
                return Icons.STATUS_BAR_DISABLED;
            case Ready:
            case AgentWarning:
                return Icons.STATUS_BAR;
            case CompletionInProgress:
                return Icons.STATUS_BAR_COMPLETION_IN_PROGRESS;
        }
        return Icons.STATUS_BAR;
    }

    @Override
    public @NlsContexts.Label String getPresentableText() {
        switch (this) {
            case Ready:
                return ChatxBundle.get("chatxStatus.ready");
            case NotSignedIn:
                return ChatxBundle.get("chatxStatus.notSignedIn");
            case CompletionInProgress:
                return ChatxBundle.get("chatxStatus.completionInProgress");
            case AgentWarning:
                return ChatxBundle.get("chatxStatus.agentWarning");
            case AgentError:
                return ChatxBundle.get("chatxStatus.agentError");
            case AgentBroken:
                return ChatxBundle.get("chatxStatus.agentBroken");
            case IncompatibleClient:
                return ChatxBundle.get("chatxStatus.incompatibleClient");
            case Unsupported:
                return ChatxBundle.get("chatxStatus.unsupported");
            case UnknownError:
                return ChatxBundle.get("chatxStatus.unknownError");
        }
        throw new IllegalStateException("Unexpected value: " + this);
    }
}
