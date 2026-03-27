package com.qihoo.finance.lowcode.common.ui.base;

import com.intellij.ui.components.JBCheckBox;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * CheckBoxGroup
 *
 * @author fengjinfu-jk
 * date 2023/11/27
 * @version 1.0.0
 * @apiNote CheckBoxGroup
 */
public class CheckBoxGroup<T> {
    private final List<JBCheckBox> checkBoxList = new ArrayList<>();
    Map<String, T> data = new HashMap<>();

    public JBCheckBox add(String name, T t) {
        JBCheckBox checkBox = new JBCheckBox(name);
        data.put(name, t);
        checkBoxList.add(checkBox);

        checkBox.addActionListener(e -> mutuallyExclusive(checkBox));
        return checkBox;
    }

    public JBCheckBox add(JBCheckBox checkBox, T value) {
        data.put(checkBox.getText(), value);
        checkBoxList.add(checkBox);

        checkBox.addActionListener(e -> mutuallyExclusive(checkBox));
        return checkBox;
    }

    public void add(T t) {
        JBCheckBox checkBox = new JBCheckBox(t.toString());
        data.put(t.toString(), t);
        checkBoxList.add(checkBox);

        checkBox.addActionListener(e -> mutuallyExclusive(checkBox));
    }

    private void mutuallyExclusive(JBCheckBox checkBox) {
        if (checkBox.isSelected()) {
            // 互斥
            for (JBCheckBox otherCheckBox : checkBoxList) {
                if (!checkBox.equals(otherCheckBox)) {
                    otherCheckBox.setSelected(false);
                }
            }
        }
    }

    public JBCheckBox getSelectedCheckBox() {
        return checkBoxList.stream().filter(JBCheckBox::isSelected).findFirst().orElse(null);
    }

    public T getSelected(T defaultVal) {
        JBCheckBox selectedCheckBox = getSelectedCheckBox();
        return Objects.nonNull(selectedCheckBox) ? data.get(selectedCheckBox.getText()) : defaultVal;
    }

    public T getSelected() {
        JBCheckBox selectedCheckBox = getSelectedCheckBox();
        return Objects.nonNull(selectedCheckBox) ? data.get(selectedCheckBox.getText()) : null;
    }

    public void setSelected(String name) {
        for (JBCheckBox checkBox : checkBoxList) {
            checkBox.setSelected(checkBox.getText().equals(name));
        }
    }

    public void removeAll() {
        checkBoxList.clear();
        data.clear();
    }

    public JPanel checkBoxGroupPanel(GridLayout layout, String defaultSelected) {
        JPanel panel = new JPanel(layout);
        for (JBCheckBox checkBox : checkBoxList) {
            if (checkBox.getText().equals(defaultSelected)) {
                checkBox.setSelected(true);
            }

            panel.add(checkBox);
        }

        return panel;
    }

    public JPanel checkBoxGroupPanel() {
        return checkBoxGroupPanel(new GridLayout(-1, 4), null);
    }
}
