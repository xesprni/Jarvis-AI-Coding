package com.qihoo.finance.lowcode.aiquestion.ui.component;

import lombok.RequiredArgsConstructor;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.LayoutManager;

public class FixedWidthJPanel extends JPanel {

    private final int width;

    public FixedWidthJPanel(int width) {
        super();
        this.width = width;
    }
    public FixedWidthJPanel(LayoutManager layout, int width) {
        super(layout);
        this.width = width;
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
