package com.qihoo.finance.lowcode.gentracker.ui.component;

import com.intellij.openapi.ui.Splitter;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

/**
 * 左右组件
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
public class LeftRightComponent {
    /**
     * 主面板
     */
    @Getter
    private JPanel mainPanel;
    /**
     * 左边面板
     */
    @Setter
    private JComponent leftPanel;
    /**
     * 右边面板
     */
    @Setter
    private JComponent rightPanel;
    /**
     * 分割比例
     */
    private final float proportion;
    /**
     * 预设值窗口大小
     */
    private final Dimension preferredSize;

    public LeftRightComponent(JPanel leftPanel, JPanel rightPanel) {
        this(leftPanel, rightPanel, 0.2F, JBUI.size(400, 300));
    }

    public LeftRightComponent(JComponent leftPanel, JComponent rightPanel, float proportion, Dimension preferredSize) {
        this.leftPanel = leftPanel;
        this.rightPanel = rightPanel;
        this.proportion = proportion;
        this.preferredSize = preferredSize;
        this.init();
    }

    private void init() {
        this.mainPanel = new JPanel(new BorderLayout());
        Splitter splitter = new Splitter(false, proportion);
        splitter.setFirstComponent(this.leftPanel);
        splitter.setSecondComponent(this.rightPanel);
        this.mainPanel.add(splitter, BorderLayout.CENTER);
        mainPanel.setPreferredSize(this.preferredSize);
    }

    public void repaint() {
        Splitter splitter = new Splitter(false, proportion);
        splitter.setFirstComponent(this.leftPanel);
        splitter.setSecondComponent(this.rightPanel);
        this.mainPanel.removeAll();
        this.mainPanel.add(splitter, BorderLayout.CENTER);
        this.mainPanel.revalidate();
        this.mainPanel.repaint();
    }
}
