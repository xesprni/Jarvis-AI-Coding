package com.qihoo.finance.lowcode.common.util;

import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

/**
 * JButtonUtils
 *
 * @author fengjinfu-jk
 * date 2023/11/13
 * @version 1.0.0
 * @apiNote JButtonUtils
 */
public class JButtonUtils {

    public static void countdown(AbstractButton button, int seconds) {
        countdown(button, seconds, true);
    }

    public static void countdown(AbstractButton button, int seconds, boolean overwriteView) {
        CountDownAction countDownAction = new CountDownAction(button, seconds, overwriteView);
        Timer timer = new Timer(1000, countDownAction);
        countDownAction.setTimer(timer);

        timer.start();
    }

    public static JButton createNonOpaqueButton(Icon icon) {
        JButton button = new JButton();
        button.setIcon(icon);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.addMouseListener(new BackgroundAdapter());
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static JButton createNonOpaqueButton(Icon icon, Dimension preferredSize) {
        JButton button = new JButton();
        button.setPreferredSize(preferredSize);
        button.setIcon(icon);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.addMouseListener(new BackgroundAdapter());
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static JButton createNonOpaqueButton(String text) {
        JButton button = new JButton(text);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.addMouseListener(new BackgroundAdapter());
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    static class BackgroundAdapter extends MouseAdapter {
        @Override
        public void mouseEntered(MouseEvent e) {
            Object source = e.getSource();
            if (source instanceof JButton button) {
                button.setContentAreaFilled(true);
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            Object source = e.getSource();
            if (source instanceof JButton button) {
                button.setContentAreaFilled(false);
                button.setBackground(JBColor.background());
            }
        }
    }

    static class CountDownAction implements ActionListener {
        private Timer timer;
        private final AbstractButton button;
        private final Icon icon;
        private final String text;
        private final boolean overwriteView;
        private int secondsTime;

        public CountDownAction(AbstractButton button, int seconds, boolean overwriteView) {
            this.button = button;
            this.icon = button.getIcon();
            this.text = button.getText();
            this.secondsTime = seconds;
            this.overwriteView = overwriteView;

            button.setEnabled(false);
            if (overwriteView) {
                button.setIcon(null);
                button.setText("...");
            }
        }

        public void setTimer(Timer timer) {
            this.timer = timer;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (secondsTime > 0) {
                String countDown = secondsTime + "s";
                button.setText(overwriteView ? countDown : String.format("%s %s", text, countDown));
                secondsTime--;
            } else {
                button.setEnabled(true);
                button.setIcon(icon);
                button.setText(text);

                if (Objects.nonNull(timer)) {
                    timer.stop();
                }
            }
        }
    }
}
