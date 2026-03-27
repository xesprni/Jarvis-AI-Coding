package com.qihoo.finance.lowcode.aiquestion.ui.component;

import javax.swing.JTextPane;
import java.awt.Dimension;

public class FixedWidthJTextPane extends JTextPane {

    private final int width;

    public FixedWidthJTextPane(int width) {
        super();
        this.width = width;
        super.setSize(new Dimension(width, Short.MAX_VALUE));
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension preferredSize = super.getPreferredSize();
        return new Dimension(width, preferredSize.height);
    }

    @Override
    public Dimension getMaximumSize() {
        Dimension maximumSize = super.getMaximumSize();
        return new Dimension(width, maximumSize.height);
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension minimumSize = super.getMinimumSize();
        return new Dimension(width, minimumSize.height);
    }
}
