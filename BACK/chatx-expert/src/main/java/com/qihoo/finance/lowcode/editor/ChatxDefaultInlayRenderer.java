package com.qihoo.finance.lowcode.editor;

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.JBColor;
import com.qihoo.finance.lowcode.editor.completions.ChatxCompletionType;
import com.qihoo.finance.lowcode.editor.request.EditorRequest;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class ChatxDefaultInlayRenderer implements ChatxInlayRenderer {

    @NotNull
    private final List<String> lines;
    @NotNull
    private final String content;
    @NotNull
    private final ChatxCompletionType type;
    @NotNull
    private final TextAttributes textAttributes;
    @Nullable
    private Inlay<ChatxInlayRenderer> inlay;

    private int cachedWidth = -1;
    private int cachedHeight = -1;

    ChatxDefaultInlayRenderer(@NotNull Editor editor, @NotNull EditorRequest request, @NotNull ChatxCompletionType type
            , @NotNull List<String> lines) {
        this.lines = replaceLeadingTabs(lines, request);
        this.type = type;
        this.content = StringUtils.join(lines, "\n");
        this.textAttributes = getTextAttributes(editor);
    }

    /**
     * 替换Tab为空格
     * @param lines
     * @param request
     * @return
     */
    public static List<String> replaceLeadingTabs(@NotNull List<String> lines, @NotNull EditorRequest request) {
        return lines.stream().map(line -> {
            int tabCount = countLeadingTabs(line, '\t', 0, line.length(), true);
            if (tabCount > 0) {
                String tabSpaces = StringUtil.repeatSymbol(' ', request.getTabWidth());
                for (int i = 0; i < tabCount; i++)
                    line = line.replaceFirst("\t", tabSpaces);
            }
            return line;
        }).collect(Collectors.toList());
    }

    /**
     * 计算文本中开头的tab数量
     * @param text
     * @param c
     * @param start
     * @param end
     * @param stopAtOtherChar
     * @return
     */
    static int countLeadingTabs(@NotNull CharSequence text, char c, int start, int end, boolean stopAtOtherChar) {
        boolean forward = (start <= end);
        start = forward ? Math.max(0, start) : Math.min(text.length(), start);
        end = forward ? Math.min(text.length(), end) : Math.max(0, end);
        int count = 0;
        int i;
        for (i = forward ? start : (start - 1); forward == ((i < end)); i += forward ? 1 : -1) {
            if (text.charAt(i) == c) {
                count++;
            } else if (text.charAt(i) != ' ') {
                if (text.charAt(i) != '\n')
                    if (stopAtOtherChar)
                        break;
            }
        }
        return count;
    }

    static int countLeadingWhiteSpaces(@NotNull CharSequence text, int start, int end, boolean stopAtOtherChar) {
        boolean forward = (start <= end);
        start = forward ? Math.max(0, start) : Math.min(text.length(), start);
        end = forward ? Math.min(text.length(), end) : Math.max(0, end);
        int count = 0;
        int i;
        for (i = forward ? start : (start - 1); forward == ((i < end)); i += forward ? 1 : -1) {
            if (Character.isWhitespace(text.charAt(i))) {
                count++;
            } else if (stopAtOtherChar) {
                break;
            }
        }
        return count;
    }

    @NotNull
    private static TextAttributes getTextAttributes(@NotNull Editor editor) {
        Color userColor = (ChatxApplicationSettings.settings()).inlayTextColor;
        EditorColorsScheme scheme = editor.getColorsScheme();
        TextAttributes themeAttributes = scheme.getAttributes(DefaultLanguageHighlighterColors.INLAY_TEXT_WITHOUT_BACKGROUND);
        if (userColor == null && themeAttributes != null && themeAttributes.getForegroundColor() != null) {
            return themeAttributes;
        }
        TextAttributes customAttributes = (themeAttributes != null) ? themeAttributes.clone() : new TextAttributes();
        if (userColor != null)
            customAttributes.setForegroundColor(userColor);
        if (customAttributes.getForegroundColor() == null)
            customAttributes.setForegroundColor(JBColor.GRAY);
        return customAttributes;
    }

    public static int countSpaceLength(String documentContent, TextRange replacementRange, int tabWidth) {
        if (replacementRange == null || replacementRange.getLength() == 0) {
            return 0;
        }
        int count = 0;
        for (int i = replacementRange.getStartOffset(); i < replacementRange.getEndOffset(); i++) {
            if (documentContent.charAt(i) == ' ') {
                count++;
            } else if (documentContent.charAt(i) == '\t') {
                count += tabWidth;
            } else {
                break;
            }
        }
        return count;
    }

    @Override
    public @NotNull List<String> getContentLines() {
        return lines;
    }

    @Override
    public int calcHeightInPixels(@NotNull Inlay inlay) {
        if (cachedHeight < 0) {
            cachedHeight = inlay.getEditor().getLineHeight() * lines.size();
        }
        return cachedHeight;
    }

    @Override
    public int calcWidthInPixels(@NotNull Inlay inlay) {
        if (cachedWidth < 0) {
            cachedWidth = InlayRendering.calculateWidth(inlay.getEditor(), this.content, this.lines);
        }
        return cachedWidth;
    }

    @Override
    public void paint(@NotNull Inlay inlay, @NotNull Graphics g, @NotNull Rectangle targetRegion
            , @NotNull TextAttributes textAttributes) {
        Editor editor = inlay.getEditor();
        if (editor.isDisposed()) {
            return;
        }
        InlayRendering.renderCodeBlock(editor, this.content, this.lines, g, targetRegion, this.textAttributes);
    }
}
