package com.qihoo.finance.lowcode.gentracker.ui.component;

import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.components.JBRadioButton;
import com.qihoo.finance.lowcode.gentracker.tool.CollectionUtil;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 列表单选组件
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
public class ListRadioComponent extends JPanel {
    /**
     * 标题
     */
    private final String title;
    /**
     * 原属列表
     */
    private final Collection<String> items;
    /**
     * 复选框列表
     */
    private List<JBRadioButton> radioList;

    /**
     * 默认构造方法
     */
    public ListRadioComponent(String title, Collection<String> items) {
        // 使用垂直流式布局
        super(new VerticalFlowLayout());
        this.title = title;
        this.items = items;
        this.init();
    }

    /**
     * 初始化操作
     */
    private void init() {
        JTextPane textPane = new JTextPane();
        textPane.setText(title);
        textPane.setEditable(false);
        add(textPane);
        JPanel radioPanel = new JPanel(new GridLayout(1, 4));
        if (CollectionUtil.isEmpty(items)) {
            return;
        }
        this.radioList = new ArrayList<>(items.size());
        ButtonGroup buttonGroup = new ButtonGroup();
        for (String item : items) {
            JBRadioButton radioButton = new JBRadioButton(item);
            this.radioList.add(radioButton);
            radioPanel.add(radioButton);
            buttonGroup.add(radioButton);
        }
        add(radioPanel);
        // 默认选中第一个
        this.radioList.get(0).setSelected(true);
    }

    /**
     * 获取已选中的元素
     *
     * @return 已选中的元素
     */
    public String getSelectedItem() {
        if (CollectionUtil.isEmpty(this.radioList)) {
            return null;
        }
        for (JBRadioButton radioButton : this.radioList) {
            if (radioButton.isSelected()) {
                return radioButton.getText();
            }
        }
        return null;
    }
}
