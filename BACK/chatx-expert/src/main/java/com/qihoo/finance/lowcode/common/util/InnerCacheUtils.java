package com.qihoo.finance.lowcode.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qihoo.finance.lowcode.gentracker.tool.JSON;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 内部本地缓存类
 *
 * @author fengjinfu-jk
 * date 2023/8/7
 * @version 1.0.0
 * @apiNote LowCodeAppService
 */
public class InnerCacheUtils {
    // 5min
    private static final long DEFAULT_CACHE_SECONDS = 60 * 30;
    private static final long CACHE_MAX_SIZE = 1000;
    private static final Map<String, InnerCache> CACHE = new HashMap<>();

    public static void setCache(String key, String value, long seconds) {
        if (CACHE.size() > CACHE_MAX_SIZE) {
            // lazy remove
            List<String> expireKeys = new ArrayList<>();
            for (Map.Entry<String, InnerCache> innerCacheEntry : CACHE.entrySet()) {
                if (isExpire(innerCacheEntry.getValue())) {
                    expireKeys.add(innerCacheEntry.getKey());
                }
            }

            for (String expireKey : expireKeys) {
                CACHE.remove(expireKey);
            }

            if (CollectionUtils.isEmpty(expireKeys)) {
                // maxSize 且暂无失效的空间
                return;
            }
        }

        InnerCache innerCache = new InnerCache();
        innerCache.setKey(key);
        innerCache.setValue(value);
        innerCache.setCacheTime(System.currentTimeMillis());
        innerCache.setSeconds(seconds);

        CACHE.put(key, innerCache);
    }

    public static void setCache(String key, String value) {
        setCache(key, value, DEFAULT_CACHE_SECONDS);
    }

    public static <T> T getCache(String key, TypeReference<T> typeReference) {
        if (!CACHE.containsKey(key)) {
            return null;
        }

        InnerCache innerCache = CACHE.get(key);
        if (isExpire(innerCache)) {
            CACHE.remove(key);
            return null;
        }

        return JSON.parse(innerCache.getValue(), typeReference);
    }

    public static String getCache(String key) {
        if (!CACHE.containsKey(key)) {
            return null;
        }

        InnerCache innerCache = CACHE.get(key);
        if (isExpire(innerCache)) {
            CACHE.remove(key);
            return null;
        }

        return innerCache.getValue();
    }

    public static void refresh() {
        CACHE.clear();
    }

    private static boolean isExpire(InnerCache innerCache) {
        long time = System.currentTimeMillis() - innerCache.getCacheTime();
        return time / 1000 > innerCache.getSeconds();
    }
}
