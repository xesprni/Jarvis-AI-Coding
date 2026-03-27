package com.qihoo.finance.lowcode.common.listener;

import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

/**
 * DefaultMouseListener
 *
 * @author fengjinfu-jk
 * date 2023/9/8
 * @version 1.0.0
 * @apiNote DefaultMouseListener
 */
public class DefaultButtonMouseListener extends MouseAdapter {
    private final JButton button;
    private Icon icon;
    private Icon selectIcon;

    public DefaultButtonMouseListener(JButton button) {
        this.button = button;
    }

    public DefaultButtonMouseListener(JButton button, Icon icon, Icon selectIcon) {
        this.button = button;
        this.icon = icon;
        this.selectIcon = selectIcon;
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // 设置悬停时的背景色
        if (Objects.nonNull(selectIcon)) {
            button.setIcon(selectIcon);
        } else {
            button.setOpaque(false);
            button.setBackground(JBColor.GRAY);
            button.setContentAreaFilled(true);
        }

        super.mouseEntered(e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // 恢复默认背景色
        button.setOpaque(false);
        button.setBackground(null);
        button.setContentAreaFilled(false);
        if (Objects.nonNull(icon)) {
            button.setIcon(icon);
        }

        super.mouseExited(e);
    }
}
