package com.qihoo.finance.lowcode.gentracker.ui.base;

import com.intellij.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;

import java.util.EventListener;
import java.util.HashMap;

/**
 * MyOtherSettingMap
 *
 * @author fengjinfu-jk
 * date 2023/12/15
 * @version 1.0.0
 * @apiNote MyOtherSettingMap
 */
@SuppressWarnings("ALL")
public class OtherSettingMap<K, V> extends HashMap<K, V> {
    public static final String generateMethods = "generateMethods";
    public static final String queryAllByLimit = "queryAllByLimit";
    public static final String count = "count";
    private final @NotNull EventDispatcher<OtherSettingListener> myDispatcher = EventDispatcher.create(OtherSettingListener.class);

    public void registerListener(OtherSettingListener<K, V> listener) {
        myDispatcher.addListener(listener);
    }

    @Override
    public V put(K key, V value) {
        myDispatcher.getMulticaster().actionPerformed(this);
        return super.put(key, value);
    }

    @Override
    public V remove(Object key) {
        myDispatcher.getMulticaster().actionPerformed(this);
        return super.remove(key);
    }

    public interface OtherSettingListener<K, V> extends EventListener {
        void actionPerformed(OtherSettingMap<K, V> e);
    }
}
