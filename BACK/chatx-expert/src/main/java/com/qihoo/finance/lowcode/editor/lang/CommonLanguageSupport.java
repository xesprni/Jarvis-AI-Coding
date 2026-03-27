package com.qihoo.finance.lowcode.editor.lang;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public final class CommonLanguageSupport {
    private static final Pattern EOL_PATTERN = Pattern.compile("^\\s*[)}\\]\"'`]*\\s*[:{;,]?\\s*$");

    public static boolean isValidMiddleOfTheLinePosition(@NotNull String lineSuffix) {
        return EOL_PATTERN.matcher(lineSuffix.trim()).matches();
    }
}
