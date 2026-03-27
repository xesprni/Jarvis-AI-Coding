package com.qihoo.finance.lowcode.common.ui.base;

import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;

/**
 * MyItemPresentation
 *
 * @author fengjinfu-jk
 * date 2024/2/6
 * @version 1.0.0
 * @apiNote MyItemPresentation
 */
public interface PlaceTextNode {
    SimpleTextAttributes GRAY = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.GRAY);

    String getDescription();

    default SimpleTextAttributes getAttributes() {
        return SimpleTextAttributes.merge(GRAY, SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }
}
