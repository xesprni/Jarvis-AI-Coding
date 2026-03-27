package com.qihoo.finance.lowcode.common.ui.base;

import com.intellij.util.ui.JBUI;

import javax.swing.border.Border;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * RoundBorder
 *
 * @author fengjinfu-jk
 * date 2023/11/15
 * @version 1.0.0
 * @apiNote RoundBorder
 */
public class RoundBorder implements Border {
    private final int radius;

    public RoundBorder(int radius) {
        this.radius = radius;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(c.getForeground());
        g2.draw(new RoundRectangle2D.Double(x, y, width - 1, height - 1, radius, radius));
    }

    @Override
    public Insets getBorderInsets(Component c) {
        int margin = radius / 2;
        return JBUI.insets(margin, margin, margin, margin);
    }

    @Override
    public boolean isBorderOpaque() {
        return true;
    }
}
