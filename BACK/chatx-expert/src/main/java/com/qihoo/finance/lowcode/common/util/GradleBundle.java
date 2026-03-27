package com.qihoo.finance.lowcode.common.util;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

/**
 * GradleBundle
 *
 * @author fengjinfu-jk
 * date 2023/11/10
 * @version 1.0.0
 * @apiNote GradleBundle
 */
public class GradleBundle extends DynamicBundle {
    @NonNls
    private static final String BUNDLE = "config.project";
    private static final GradleBundle INSTANCE = new GradleBundle();

    private GradleBundle() {
        super(BUNDLE);
    }

    @NotNull
    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, @NotNull Object... params) {
        return INSTANCE.getMessage(key, params);
    }
}
