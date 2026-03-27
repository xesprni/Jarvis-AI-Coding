package com.qihoo.finance.lowcode.common.util;

import com.intellij.ui.JBColor;

import javax.swing.*;

import static com.qihoo.finance.lowcode.common.util.Icons.scaleToWidth;

public class IconUtil {

    public static Icon getThemeAwareIcon(Icon lightIcon, Icon darkIcon) {
        return JBColor.isBright() ? lightIcon : darkIcon;
    }

    public static Icon getThemeAwareIcon(Icon lightIcon, Icon darkIcon, int width) {
        Icon icon = JBColor.isBright() ? lightIcon : darkIcon;
        return scaleToWidth(icon, width);
    }
}
