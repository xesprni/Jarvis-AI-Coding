package com.qihoo.finance.lowcode.aiquestion.ui.factory;

import com.intellij.util.ui.JBInsets;
import com.qihoo.finance.lowcode.common.util.Icons;

import javax.swing.Icon;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ImgButtonFactory {

    public JButton create(String toolTip, Icon icon, int imgWidth, int imgHeight, JBInsets insets
            , Color backgroundColor) {
//        ImageIcon imageIcon = new ImageIcon(Objects.requireNonNull(ImgButtonFactory.class.getResource(imgLocation)));
//        ImageIcon scaledIcon = new ImageIcon(imageIcon.getImage().getScaledInstance(imgWidth, imgHeight, Image.SCALE_SMOOTH));
        JButton button = new JButton(Icons.scaleToWidth(icon, imgWidth));
        button.setToolTipText(toolTip);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setPreferredSize(new Dimension(imgWidth + insets.left + insets.right
                , imgHeight + insets.top + insets.bottom));
        button.setBackground(backgroundColor);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setContentAreaFilled(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setContentAreaFilled(false);
                button.setBackground(backgroundColor);
            }
        });
        return button;
    }
}
