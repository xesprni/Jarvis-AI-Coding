package com.qihoo.finance.lowcode.common.ui.base;

import com.intellij.util.ui.JBUI;
import com.qihoo.finance.lowcode.common.util.Icons;

import javax.swing.*;
import java.awt.*;

/**
 * MainPanel
 *
 * @author fengjinfu-jk
 * date 2023/8/18
 * @version 1.0.0
 * @apiNote MainPanel
 */
public class DefaultPanel {

    public Component createPanel() {
        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 15;
        gbc.insets = JBUI.insetsTop(15);

        JLabel plugins = new JLabel();
        plugins.setIcon(Icons.scaleToWidth(Icons.API_DESIGN, 200));
        contentPanel.add(plugins, gbc);

        gbc.gridy = 1;
        // Add welcome label at the top
        JLabel placeHolder = new JLabel(" ");
        placeHolder.setFont(new Font("微软雅黑", Font.BOLD, 15));
        placeHolder.setHorizontalAlignment(SwingConstants.CENTER);
        contentPanel.add(placeHolder, gbc);

        gbc.gridy = 2;
        // Add welcome label at the top
        JLabel welcomeLabel = new JLabel("更多功能开发中, 敬请期待 !");
        welcomeLabel.setFont(new Font("微软雅黑", Font.BOLD, 15));
        welcomeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        contentPanel.add(welcomeLabel, gbc);

        return contentPanel;
    }
}
