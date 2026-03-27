package com.qihoo.finance.lowcode.editor.completions;

import com.qihoo.finance.lowcode.editor.ChatxApplyInlayStrategy;

import java.util.Set;

public class CompletionSplitter {

    private final static Set<Character> splitChar = Set.of(',', '!', '?', ';', ':', '-', '&', '/', '=', '|', '('
            , ')', '[', ']', '{', '}', '~');

    public static String split(String completion, ChatxApplyInlayStrategy applyStrategy) {
        switch (applyStrategy) {
            case WHOLE:
                return completion;
            case NEXT_WORD:
                return splitNextWord(completion);
            case NEXT_LINE:
                return splitNextLine(completion);
            default:
                throw new IllegalStateException("Unexpected value: " + applyStrategy);
        }
    }

    private static String splitNextWord(String completion) {
        int i = 0;
        while (i < completion.length() && (Character.isWhitespace(completion.charAt(i)) || completion.charAt(i) == '.')) {
            ++i;
        }
        boolean isInQuotes = false;
        for (; i < completion.length(); i++) {
            char c = completion.charAt(i);
            if (c == '"' && (i == 0 || completion.charAt(i - 1) != '\\')) {
                isInQuotes = !isInQuotes;
            }
            if (isInQuotes) {
                continue;
            }
            if (Character.isWhitespace(c) || c == '.') {
                return completion.substring(0, i);
            }
            if (splitChar.contains(completion.charAt(i))) {
                return completion.substring(0, i + 1);
            }
        }
        return completion;
    }

    private static String splitNextLine(String completion) {
        int newLineIndex = completion.indexOf('\n', 1);
        if (newLineIndex != -1) {
            return completion.substring(0, newLineIndex);
        } else {
            return completion;
        }
    }
}
