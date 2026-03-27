package com.qihoo.finance.lowcode.common.ui.base;

import com.intellij.util.ui.JBUI;
import com.qihoo.finance.lowcode.common.util.Icons;

import javax.swing.*;
import java.awt.*;

/**
 * ClosableTabbedPane
 *
 * @author fengjinfu-jk
 * date 2023/9/13
 * @version 1.0.0
 * @apiNote ClosableTabbedPane
 */
class ClosableTabbedPane extends JTabbedPane {

    /**
     * 可关闭Tab
     */
    public ClosableTabbedPane() {
        super();
    }

    @Override
    public void addTab(String title, Component component) {
        JPanel tabHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabHeader.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        tabHeader.add(titleLabel);

        JButton closeButton = new JButton(Icons.scaleToWidth(Icons.REMOVE, 13));
        closeButton.setMargin(JBUI.emptyInsets());
        closeButton.addActionListener(e -> {
            int tabIndex = indexOfComponent(component);
            if (tabIndex != -1) {
                removeTabAt(tabIndex);
            }
        });
        tabHeader.add(closeButton);

        super.addTab(null, component);
        setTabComponentAt(getTabCount() - 1, tabHeader);
    }
}
