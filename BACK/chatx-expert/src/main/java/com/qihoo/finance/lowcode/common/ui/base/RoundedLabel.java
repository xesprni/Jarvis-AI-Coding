package com.qihoo.finance.lowcode.common.ui.base;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * RoundedPanel
 *
 * @author fengjinfu-jk
 * date 2023/11/17
 * @version 1.0.0
 * @apiNote RoundedPanel
 */
public class RoundedLabel extends JLabel {
    public static Color SELECTED = new Color(75, 110, 175);
    public static Color GREEN = new Color(118, 152, 117);
    public static Color BLUE = new Color(86, 127, 164);
    public static Color PURPLE = new Color(164, 117, 164);
    public static Color YELLOW = new Color(164, 152, 86);
    public static Color BLACK = new Color(0, 0, 0);
    public static Color WHITE = new Color(255, 255, 255);
    public static final List<Color> COLORS = Lists.newArrayList(BLUE, YELLOW, PURPLE, GREEN);
    private final Color backgroundColor;
    private final int cornerRadius;

    public static Color randomColor(int random) {
        random = random % COLORS.size();
        return COLORS.get(random);
    }

    public RoundedLabel(String text, Color backgroundColor, int cornerRadius) {
        super(SPACE + text + SPACE);
        this.backgroundColor = backgroundColor;
        this.cornerRadius = cornerRadius;
        setOpaque(false);
    }

    public RoundedLabel(String text, Color backgroundColor, Color foreground, int cornerRadius) {
        this(text, backgroundColor, cornerRadius);
        setForeground(foreground);
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(backgroundColor);
        g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius));
        g2.dispose();
        super.paintComponent(g);
    }

    private static final String SPACE = " ";

    @Override
    public void setText(String text) {
        if (StringUtils.isBlank(text)) super.setText(text);

        super.setText(SPACE + text + SPACE);
    }

    public String getRealText(){
        String text = super.getText();
        return StringUtils.isBlank(text) ? text : text.trim();
    }
}
