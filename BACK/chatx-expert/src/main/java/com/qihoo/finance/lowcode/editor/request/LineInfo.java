package com.qihoo.finance.lowcode.editor.request;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.qihoo.finance.lowcode.editor.ChatxEditorUtil;
import com.qihoo.finance.lowcode.editor.util.ChatxStringUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@RequiredArgsConstructor
public class LineInfo {

    private final int lineCount;

    private final int lineNumber;

    private final int lineStartOffset;

    private final int columnOffset;

    @NotNull
    private final String line;

    private final String nextLine;
    private final int nextLineIndent;

    @NotNull
    public static LineInfo create(@NotNull Document document, int offset, int tabWidth) {
        int line = document.getLineNumber(offset);
        TextRange lineRange = TextRange.create(document.getLineStartOffset(line), document.getLineEndOffset(line));
        String nextLine = getNextNotEmptyLine(document, offset);
        int nextLineIndent = -1;
        if (nextLine != null) {
            nextLineIndent = ChatxEditorUtil.whitespacePrefixLength(nextLine, tabWidth);
        }
        return new LineInfo(document.getLineCount(), line, lineRange.getStartOffset()
                , offset - lineRange.getStartOffset(), document.getText(lineRange),
                nextLine, nextLineIndent);
    }

    @NotNull
    public String getLinePrefix() {
        return this.line.substring(0, this.columnOffset);
    }

    @NotNull
    public String getLineSuffix() {
        return this.line.substring(this.columnOffset);
    }

    public boolean isBlankLine() {
        return this.line.isBlank();
    }

    /**
     * 获取光标所在行-光标位置之前的空白字符
     * @return
     */
    @NotNull
    public String getWhitespaceBeforeCursor() {
        return ChatxStringUtil.trailingWhitespace(getLinePrefix());
    }

    public int getLineEndOffset() {
        return getLineStartOffset() + this.line.length();
    }

    private static int calculateNextLineIndent(@NotNull Document document, int offset, int tabWidth) {
        int maxLines = document.getLineCount();
        for (int line = document.getLineNumber(offset) + 1; line < maxLines; line++) {
            int start = document.getLineStartOffset(line);
            int end = document.getLineEndOffset(line);
            if (start != end) {
                String lineContent = document.getText(TextRange.create(start, end));
                if (!lineContent.isBlank()) {
                    return ChatxEditorUtil.whitespacePrefixLength(lineContent, tabWidth);
                }
            }
        }
        return -1;
    }

    private static String getNextNotEmptyLine(@NotNull Document document, int offset) {
        int maxLines = document.getLineCount();
        for (int line = document.getLineNumber(offset) + 1; line < maxLines; line++) {
            int start = document.getLineStartOffset(line);
            int end = document.getLineEndOffset(line);
            if (start != end) {
                String lineContent = document.getText(TextRange.create(start, end));
                if (!lineContent.isBlank()) {
                    return lineContent;
                }
            }
        }
        return null;
    }
}
