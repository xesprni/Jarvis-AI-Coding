package com.qihoo.finance.lowcode.common.ui;

import com.intellij.ide.ui.laf.darcula.ui.DarculaTabbedPaneUI;

/**
 * CustomTabbedPaneUI
 *
 * @author fengjinfu-jk
 * date 2023/12/6
 * @version 1.0.0
 * @apiNote CustomTabbedPaneUI
 */
public class CustomHeightTabbedPaneUI extends DarculaTabbedPaneUI {

    @Override
    protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
        return Math.min(30, fontHeight + 10);
    }
}
