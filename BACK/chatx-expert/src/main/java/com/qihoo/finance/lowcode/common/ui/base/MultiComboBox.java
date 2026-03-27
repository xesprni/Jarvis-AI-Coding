package com.qihoo.finance.lowcode.common.ui.base;

import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBScrollPane;
import com.qihoo.finance.lowcode.common.util.Icons;
import lombok.Data;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * MultiComboBox
 *
 * @author fengjinfu-jk
 * date 2023/9/6
 * @version 1.0.0
 * @apiNote MultiSelectDropdownExample
 */
public class MultiComboBox extends JPanel {
    private final List<String> values;
    private final List<JBCheckBox> checkBoxList;
    private PopupPanel popupPanel;
    @Getter
    private JTextField editor;
    private static final String SEPARATOR = ",";

    public MultiComboBox(List<String> values) {
        this.values = values;
        this.checkBoxList = new ArrayList<>();
        // initComponent
        initComponent();
        // init data
        initComponentData();
    }

    public MultiComboBox() {
        this(new ArrayList<>());
    }

    public String getText() {
        return editor.getText();
    }

    public List<String> getSelectedItems() {
        return Arrays.stream(editor.getText().split(",")).toList();
    }

    public void setSelectedItems(List<String> selectedItems) {
        editor.setText(selectedItems.stream().filter(values::contains).collect(Collectors.joining(",")));
    }

    public void setItems(List<String> selectedItems) {
        this.values.clear();
        this.values.addAll(selectedItems);
        loadCheckBoxList();
    }

    public void addItems(List<String> selectedItems) {
        this.values.addAll(selectedItems);
        loadCheckBoxList();
    }

    public void addItem(String item) {
        this.values.add(item);
        loadCheckBoxList();
    }

    //------------------------------------------------------------------------------------------------------------------

    private void initComponent() {
        this.setLayout(new BorderLayout());
        editor = new JTextField();
        editor.setEditable(false);
        editor.setEnabled(false);
        Border lineBorder = editor.getBorder();
        //去掉文本框的边框线
        editor.setBorder(null);
        //设为透明
        editor.setOpaque(false);
        editor.setBackground(JBColor.background());
        editor.addMouseListener(mouseAction());
        popupPanel = createPopupPanel();

        JLabel comboBoxIcon = new JLabel();
        comboBoxIcon.setIcon(Icons.scaleToWidth(Icons.COMBO, 10));
        comboBoxIcon.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
        comboBoxIcon.addMouseListener(mouseAction());

        add(editor, BorderLayout.CENTER);
        add(comboBoxIcon, BorderLayout.EAST);

        this.setBorder(lineBorder);
        this.loadCheckBoxList();
    }

    private PopupPanel createPopupPanel() {
        JPanel popup = new JPanel(new BorderLayout());
        JPanel center = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        JBScrollPane centerScroll = new JBScrollPane();
        centerScroll.setViewportView(center);

        JPanel south = new JPanel(new BorderLayout());
        JButton selectAll = new JButton("全选/反选");
        selectAll.addActionListener(e -> selectAll(checkBoxList));
        JPanel eastPanel = new JPanel();
        eastPanel.add(selectAll);
        south.add(eastPanel, BorderLayout.WEST);

        JPanel westPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton commit = new JButton("确定");
        commit.addActionListener(e -> commitAction(editor, checkBoxList));
        westPanel.add(commit);

        JButton cancel = new JButton("取消");
        westPanel.add(cancel);
        south.add(westPanel, BorderLayout.EAST);

        popup.add(centerScroll, BorderLayout.CENTER);
        popup.add(south, BorderLayout.SOUTH);

        PopupPanel panel = new PopupPanel();
        panel.setPopupComponent(popup);
        panel.setPopupScroll(centerScroll);
        panel.setCenter(center);
        panel.setCommit(commit);
        panel.setCancel(cancel);
        panel.setCheckBoxList(checkBoxList);
        return panel;
    }

    private void loadCheckBoxList() {
        if (Objects.nonNull(popupPanel) && Objects.nonNull(popupPanel.getCenter())) {
            JPanel center = popupPanel.getCenter();
            center.removeAll();
            center.revalidate();
            for (String value : values) {
                JBCheckBox checkBox = new JBCheckBox(value);
                center.add(checkBox);
                checkBoxList.add(checkBox);
            }

            center.repaint();
        }
    }

    private void selectAll(List<JBCheckBox> checkBoxList) {
        boolean hadSelected = checkBoxList.stream().anyMatch(JBCheckBox::isSelected);
        for (JBCheckBox checkBox : checkBoxList) {
            checkBox.setSelected(!hadSelected);
        }
    }

    private void commitAction(JTextField editor, List<JBCheckBox> checkBoxList) {
        String selectText = checkBoxList.stream().filter(JBCheckBox::isSelected)
                .map(JBCheckBox::getText).collect(Collectors.joining(SEPARATOR));
        editor.setText(selectText);
    }

    private MouseAdapter mouseAction() {
        return new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    showPopup();
                }
            }
        };
    }

    private void showPopup() {
        if (Objects.isNull(popupPanel)) return;
        if (System.currentTimeMillis() - popupPanel.getLastCloseTime() < 200) return;

        JComponent popupComponent = popupPanel.getPopupComponent();
        if (popupComponent.isShowing()) return;

        JBScrollPane popupScroll = popupPanel.getPopupScroll();
        Dimension preferredSize = editor.getSize();
        popupScroll.setPreferredSize(new Dimension(preferredSize.width + 13, 300));

        JBPopup newPopup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(popupComponent, null)
                .setBorderColor(null)
                .setShowBorder(true)
                .setShowShadow(false)
                .createPopup();
        newPopup.addListener(new JBPopupListener() {
            @Override
            public void onClosed(@NotNull LightweightWindowEvent event) {
                JBPopupListener.super.onClosed(event);
                popupPanel.setLastCloseTime(System.currentTimeMillis());
            }
        });

        popupPanel.getCommit().addActionListener(e -> newPopup.cancel());
        popupPanel.getCancel().addActionListener(e -> newPopup.cancel());

        initComponentData();
        newPopup.showUnderneathOf(editor);
    }

    private void initComponentData() {
        List<String> selectedValues = Arrays.stream(editor.getText().split(SEPARATOR)).toList();
        for (JBCheckBox checkBox : checkBoxList) {
            checkBox.setSelected(selectedValues.contains(checkBox.getText()));
        }
    }

    @Data
    static class PopupPanel {
        private JComponent popupComponent;
        private JBScrollPane popupScroll;
        private List<JBCheckBox> checkBoxList;
        private JButton cancel;
        private JButton commit;
        private JPanel center;
        private long lastCloseTime;
    }

}
