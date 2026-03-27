package com.qihoo.finance.lowcode.aiquestion.ui.component;

import javax.swing.JTextPane;
import java.awt.Dimension;

public class NoWrapJTextPane extends JTextPane {

    public NoWrapJTextPane() {
        super.setSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        // 当viewPort的宽度大于EditorPane的宽度时，才设置跟随viewPort的宽度
        return getUI().getPreferredSize(this).width
                <= getParent().getSize().width;
    }

    @Override
    public Dimension getPreferredSize() {
        // 禁止掉特性：当viewPort的宽度小于EditorPane的最小宽度时，getPreferredSize返回的是minWidth
        return getUI().getPreferredSize(this);
    }
}
