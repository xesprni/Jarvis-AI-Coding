package com.qihoo.finance.lowcode.common.ui.base;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * RoundedPanel
 *
 * @author fengjinfu-jk
 * date 2023/11/17
 * @version 1.0.0
 * @apiNote RoundedPanel
 */
public class RoundedPanel extends JPanel {
    public static Color SELECTED = new Color(75, 110, 175);
    public static Color GREEN = new Color(118, 152, 117);
    public static Color BLUE = new Color(86, 127, 164);
    public static Color BLACK = new Color(0, 0, 0);
    public static Color WHITE = new Color(255, 255, 255);
    private Color backgroundColor;
    private final int cornerRadius;

    public RoundedPanel(Color backgroundColor, int cornerRadius) {
        this.backgroundColor = backgroundColor;
        this.cornerRadius = cornerRadius;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(backgroundColor);
        g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius));

        g2.setStroke(new BasicStroke(2)); // 设置边框宽度
        g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius));

        g2.dispose();
    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        this.backgroundColor = bg;
    }
}
