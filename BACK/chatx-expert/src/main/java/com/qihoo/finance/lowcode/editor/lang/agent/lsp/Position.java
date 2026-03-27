package com.qihoo.finance.lowcode.editor.lang.agent.lsp;

import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.util.text.StringUtil;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import com.qihoo.finance.lowcode.editor.request.LineInfo;

@Data
public class Position {

    @SerializedName("line")
    int line;

    @SerializedName("character")
    int character;

    @NotNull
    public static Position of(int line, int character) {
        return new Position(line, character);
    }

    public Position(@NotNull LineInfo lineInfo) {
        this(lineInfo.getLineNumber(), lineInfo.getColumnOffset());
    }

    public Position(int line, int character) {
        this.line = line;
        this.character = character;
    }

    public int toOffset(@NotNull String text) {
        return StringUtil.lineColToOffset(text, this.line, this.character);
    }
}
