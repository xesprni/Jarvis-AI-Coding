package com.qihoo.finance.lowcode.console.completion;

import com.intellij.util.TextFieldCompletionProvider;
import com.qihoo.finance.lowcode.common.constants.Constants;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;

/**
 * CompletionProvider
 *
 * @author fengjinfu-jk
 * date 2024/1/25
 * @version 1.0.0
 * @apiNote CompletionProvider
 */
public abstract class CompletionProvider extends TextFieldCompletionProvider {
    @Override
    public @Nullable String getPrefix(@NotNull String text, int offset) {
        String prefix = text.substring(0, offset);

        if (prefix.contains(" ")) {
            prefix = StringUtils.substringAfterLast(prefix, " ");
        }
        if (prefix.contains("\n")) {
            prefix = StringUtils.substringAfterLast(prefix, "\n");
        }

        Matcher matcher = Constants.REGEX.KEY_WORD.matcher(prefix);
        while (matcher.find()) {
            prefix = matcher.group();
        }

        return prefix;
    }
}
