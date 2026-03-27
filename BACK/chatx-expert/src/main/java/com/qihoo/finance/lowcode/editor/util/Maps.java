package com.qihoo.finance.lowcode.editor.util;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Maps {

    @SafeVarargs
    public static <K, V> Map<K, V> merge(@NotNull Map<K, ? extends V>... maps) {
        if (maps.length == 0)
            return Collections.emptyMap();
        if (maps.length == 1)
            return Map.copyOf(maps[0]);
        Map<K, V> all = null;
        for (Map<K, ? extends V> map : maps) {
            if (!map.isEmpty()) {
                if (all == null) {
                    all = new HashMap<>();
                }
                all.putAll(map);
            }
        }
        return (all == null) ? Collections.<K, V>emptyMap() : Collections.<K, V>unmodifiableMap(all);
    }
}
