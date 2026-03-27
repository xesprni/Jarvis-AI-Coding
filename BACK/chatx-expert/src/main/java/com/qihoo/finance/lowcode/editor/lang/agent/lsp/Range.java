package com.qihoo.finance.lowcode.editor.lang.agent.lsp;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class Range {

    @SerializedName("start")
    @NotNull
    Position start;

    @SerializedName("end")
    @NotNull
    Position end;

    @NotNull
    public static Range of(@NotNull Position start, @NotNull Position end) {
        return new Range(start, end);
    }
}
