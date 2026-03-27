package com.qihoo.finance.lowcode.common.ui.base;

import com.intellij.util.ui.JBUI;
import com.qihoo.finance.lowcode.common.util.Icons;

import javax.swing.*;
import java.awt.*;

/**
 * LoadingPanel
 *
 * @author fengjinfu-jk
 * date 2023/9/7
 * @version 1.0.0
 * @apiNote LoadingPanel
 */
public class LoadingPanel {

    public static Component createLoadingPanel() {
        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 15;
        gbc.insets = JBUI.insetsTop(100);

        JLabel plugins = new JLabel();
        plugins.setIcon(Icons.scaleToWidth(Icons.ROCKET, 150));
        contentPanel.add(plugins, gbc);

        gbc.gridy = 1;
        JLabel loading = new JLabel("Loading...");
        loading.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        loading.setHorizontalAlignment(SwingConstants.CENTER);
        loading.setIcon(Icons.scale(Icons.LOADING, 20, 20, Image.SCALE_DEFAULT));
        contentPanel.add(loading, gbc);

        contentPanel.setOpaque(false);
        return contentPanel;
    }

    public static Component createEmptyLoadingPanel(String text) {
        JPanel contentPanel = new JPanel(new BorderLayout());

        JLabel loading = new JLabel(text);
        loading.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        loading.setHorizontalAlignment(SwingConstants.CENTER);
        loading.setIcon(Icons.scale(Icons.LOADING, 20, 20, Image.SCALE_DEFAULT));
        contentPanel.add(loading, BorderLayout.CENTER);

        contentPanel.setOpaque(false);
        return contentPanel;
    }

    public static Component createEmptyLoadingPanel() {
        return createEmptyLoadingPanel("Loading...");
    }
}
