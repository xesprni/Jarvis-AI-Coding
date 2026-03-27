package com.qihoo.finance.lowcode.editor.lang.agent.commands;

import com.google.gson.annotations.SerializedName;
import com.qihoo.finance.lowcode.common.entity.dto.askai.CompletionMode;
import com.qihoo.finance.lowcode.common.entity.enums.CompletionStatus;
import com.qihoo.finance.lowcode.editor.lang.agent.lsp.Position;
import com.qihoo.finance.lowcode.editor.lang.agent.lsp.Range;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Data
public class GetCompletionsResult {

    @SerializedName("completions")
    @NotNull
    List<Completion> completions;

    @Data
    public static final class Completion {
        @SerializedName("uuid")
        @NotNull
        private final String uuid;

        @SerializedName("text")
        @NotNull
        private final String text;

        @SerializedName("range")
        @NotNull
        private final Range range;

        @SerializedName("displayText")
        @NotNull
        private final String displayText;

        @SerializedName("position")
        @NotNull
        private final Position position;

        private final CompletionMode completionMode;

        @SerializedName("model")
        @NotNull
        private final String model;

        private CompletionStatus completionStatus;

    }
}
