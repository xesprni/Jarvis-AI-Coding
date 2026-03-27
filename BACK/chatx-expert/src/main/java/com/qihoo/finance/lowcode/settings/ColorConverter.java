package com.qihoo.finance.lowcode.settings;

import com.intellij.ui.ColorUtil;
import com.intellij.util.xmlb.Converter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;

public class ColorConverter extends Converter<Color> {
    @Override
    public @Nullable Color fromString(@NotNull String value) {
        try {
            return ColorUtil.fromHex(value);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public @Nullable String toString(@NotNull Color value) {
        return ColorUtil.toHtmlColor(value);
    }
}
