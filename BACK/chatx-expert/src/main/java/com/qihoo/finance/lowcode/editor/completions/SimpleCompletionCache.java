package com.qihoo.finance.lowcode.editor.completions;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.io.DigestUtil;
import com.qihoo.finance.lowcode.editor.util.ChatxStringUtil;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class SimpleCompletionCache implements CompletionCache {
    
    protected static final Logger LOG = Logger.getInstance(SimpleCompletionCache.class); 

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<CacheKey, List<ChatxCompletion>> cache;
    @Nullable
    private String lastPrefix;
    @Nullable
    private String lastPromptHash;
    private boolean lastIsMultiline;

    public SimpleCompletionCache(final int cacheSize) {
        this.cache = new LinkedHashMap<CacheKey, List<ChatxCompletion>>(cacheSize, 0.6F) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<CacheKey, List<ChatxCompletion>> eldest) {
                return (size() > cacheSize);
            }
        };
    }

    @Override
    public boolean isLatestPrefix(@NotNull String prefix) {
        Lock readLock = this.lock.readLock();
        readLock.lock();
        try {
            return (this.lastPrefix != null && this.lastPrefix.equals(prefix));
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public @Nullable List<ChatxCompletion> get(@NotNull String prompt, boolean isMultiline) {
        LOG.trace("Retrieving cached api items for prompt");
        Lock readLock = this.lock.readLock();
        readLock.lock();
        try {
            return (List)this.cache.get(new CacheKey(promptHash(prompt), isMultiline));
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public @Nullable List<ChatxCompletion> getLatest(@NotNull String prefix, int tabWidth) {
        Lock readLock = this.lock.readLock();
        readLock.lock();
        try {
            return getLatestLocked(prefix, tabWidth);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void add(@NotNull String prefix, @NotNull String prompt, boolean isMultiline, @NotNull ChatxCompletion item) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Caching new APIChoice for prompt: " + item);
        }
        Lock writeLock = this.lock.writeLock();
        writeLock.lock();
        try {
            this.lastPrefix = prefix;
            this.lastPromptHash = promptHash(prompt);
            this.lastIsMultiline = isMultiline;
            CacheKey key = new CacheKey(this.lastPromptHash, this.lastIsMultiline);
            List<ChatxCompletion> apiChoices = this.cache.computeIfAbsent(key
                    ,s -> ContainerUtil.createLockFreeCopyOnWriteList());
            apiChoices.add(item.asCached());
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void updateLatest(@NotNull String prefix, @NotNull String prompt, boolean isMultiline) {
        Lock writeLock = this.lock.writeLock();
        writeLock.lock();
        try {
            this.lastPrefix = prefix;
            this.lastPromptHash = promptHash(prompt);
            this.lastIsMultiline = isMultiline;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void clear() {
        Lock writeLock = this.lock.writeLock();
        writeLock.lock();
        try {
            this.lastPromptHash = null;
            this.lastPrefix = null;
            this.lastIsMultiline = false;
            this.cache.clear();
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 获取上一次已经缓存的补全结果，并将本次用户新敲的部分与补全结果去匹配，如果相同，则返回上次补全结果后除去新键入的部分作为补全结果，否则返回null
     * @param prefix
     * @return
     */
    @Nullable
    private List<ChatxCompletion> getLatestLocked(@NotNull String prefix, int tabWidth) {
        if (this.lastPrefix == null || this.lastPromptHash == null || !prefix.startsWith(this.lastPrefix))
            return null;
        CacheKey cacheKey = new CacheKey(this.lastPromptHash, this.lastIsMultiline);
        List<ChatxCompletion> result = this.cache.get(cacheKey);
        if (result == null) {
            return null;
        }
        // 提取出本次prompt相对于上一次prompt中新增的部分，新增部分的\t转换成对应缩进的空格。
        String remainingPrefix = ChatxStringUtil.replaceLeadingTabs(prefix.substring(this.lastPrefix.length()), tabWidth);
        if (remainingPrefix.isEmpty()) {
            return Collections.unmodifiableList(result);
        }
        List<ChatxCompletion> adjustedChoices = result.stream()
                .map(choice -> choice.withoutPrefix(remainingPrefix))
                .filter(Objects::nonNull)
//                .filter(c -> CollectionUtils.isNotEmpty(c.getCompletion())
//                        && (c.getCompletion().size() > 1 || !c.getCompletion().get(0).isEmpty()))
                .collect(Collectors.toList());
        return adjustedChoices.isEmpty() ? null : adjustedChoices;
    }

    /**
     * 从补全中去除本次prompt相对上一次缓存结果多的部分
     * @param apiChoice
     * @param prefix
     * @return
     */
    @Nullable
    private ChatxCompletion withoutPrefix(@NotNull ChatxCompletion apiChoice, @NotNull String prefix) {
        if (prefix.isEmpty()) {
            return apiChoice;
        }
        boolean ignoreFirstWhiteSpace = ChatxStringUtil.leadingWhitespace(prefix).isEmpty();
        List<String> completion = apiChoice.getCompletion();
        String remainingPrefix = prefix;
        int i = 0, completionSize = completion.size();
        if (i < completionSize) {
            String prefixLastLine, line = completion.get(i);
            int prefixLineEnd = remainingPrefix.lastIndexOf('\n');
            String prefixLine = remainingPrefix.substring(0, (prefixLineEnd == -1) ? remainingPrefix.length() : prefixLineEnd);
            if (prefixLineEnd == -1) {
                prefixLastLine = remainingPrefix.substring(0);
            } else if (prefixLineEnd == remainingPrefix.length()) {
                prefixLastLine = "";
            } else {
                prefixLastLine = remainingPrefix.substring(prefixLineEnd + 1);
            }
            if (ignoreFirstWhiteSpace && i == 0) {
                String trimmedLine = ChatxStringUtil.stripLeading(line);
                boolean ok = (prefixLineEnd == -1) ? trimmedLine.startsWith(prefixLine) : trimmedLine.startsWith(prefixLastLine);
                if (!ok) {
                    return null;
                }
            } else {
                boolean ok = (prefixLineEnd == -1) ? line.startsWith(prefixLine) : line.startsWith(prefixLastLine);
                if (!ok) {
                    return null;
                }
            }
            if (prefixLineEnd == -1) {
                ArrayList<String> arrayList = new ArrayList<>(completionSize - i);
                int j = (ignoreFirstWhiteSpace && i == 0) ? ChatxStringUtil.leadingWhitespace(line).length() : 0;
                arrayList.add(line.substring(j + prefixLine.length()));
                if (i + 1 < completionSize) {
                    arrayList.addAll(completion.subList(i + 1, completionSize));
                }
                return apiChoice.withCompletion(arrayList).asCached();
            }
            if (!remainingPrefix.isBlank()) {
                return null;
            }
            ArrayList<String> newCompletions = new ArrayList<>(completionSize - i);
            int droppedWhitespace = (ignoreFirstWhiteSpace && i == 0) ? ChatxStringUtil.leadingWhitespace(line).length() : 0;
            newCompletions.add(line.substring(droppedWhitespace + prefixLastLine.length()));
            if (i + 1 < completionSize) {
                newCompletions.addAll(completion.subList(i + 1, completionSize));
            }
            return apiChoice.withCompletion(newCompletions).asCached();
        }
        return null;
    }

    @Data
    private static final class CacheKey {

        private final String promptHash;

        private final boolean isMultiline;

        public CacheKey(String promptHash, boolean isMultiline) {
            this.promptHash = promptHash;
            this.isMultiline = isMultiline;
        }

    }

    private static String promptHash(@NotNull String prompt) {
        return DigestUtil.sha256Hex(prompt.getBytes(StandardCharsets.UTF_8));
    }
}
