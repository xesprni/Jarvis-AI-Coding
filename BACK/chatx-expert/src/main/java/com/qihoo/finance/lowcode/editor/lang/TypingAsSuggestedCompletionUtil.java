package com.qihoo.finance.lowcode.editor.lang;

import com.qihoo.finance.lowcode.editor.completions.ChatxCompletion;
import com.qihoo.finance.lowcode.editor.completions.ChatxInlayList;
import com.qihoo.finance.lowcode.editor.completions.CompletionCache;
import com.qihoo.finance.lowcode.editor.completions.CompletionUtil;
import com.qihoo.finance.lowcode.editor.request.EditorRequest;
import com.qihoo.finance.lowcode.editor.request.LineInfo;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TypingAsSuggestedCompletionUtil {

    @Nullable
    public static List<ChatxInlayList> handleTypeAheadCaching(@NotNull EditorRequest request
            , @NotNull CompletionCache cache) {
        if ((ChatxApplicationSettings.settings()).internalDisableHttpCache) {
            return null;
        }
        if (!isValidLineSuffix(request.getLineInfo()))
            return null;
        String docPrefix = request.getCurrentDocumentPrefix();
        String prefixTrimmed = CompletionUtil.TrimEndSpaceTab(docPrefix);
         List<ChatxCompletion> items = cache.getLatest(docPrefix, request.getTabWidth());
        if (items == null) {
            return null;
        }
        boolean dropLinePrefix = cache.isLatestPrefix(prefixTrimmed);
        List<ChatxInlayList> inlays = items.stream().map(item ->
                CompletionUtil.createEditorCompletion(request, item, dropLinePrefix))
                .filter(Objects::nonNull).collect(Collectors.toList());
        return inlays.isEmpty() ? null : inlays;
    }

    private static boolean isValidLineSuffix(@NotNull LineInfo line) {
        String lineSuffix = line.getLineSuffix();
        return lineSuffix.isEmpty() || CommonLanguageSupport.isValidMiddleOfTheLinePosition(lineSuffix);
    }
}
