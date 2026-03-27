package com.qihoo.finance.lowcode.common.util;

import com.intellij.ide.IdeEventQueue;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBScrollPane;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.ui.base.VFlowLayout;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * JPanelUtils
 *
 * @author fengjinfu-jk
 * date 2023/10/13
 * @version 1.0.0
 * @apiNote JPanelUtils
 */
public class JPanelUtils {

    public static JProgressBar createProgress() {
        JProgressBar progressBar = new JProgressBar();
        progressBar.setBorderPainted(false);
        progressBar.setPreferredSize(new Dimension(-1, 5));
        // 设置采用不确定进度条
        progressBar.setIndeterminate(true);
        progressBar.setOpaque(false);

        return progressBar;
    }
    public static void setSize(JComponent panel, Dimension dimension) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = screenSize.width - 100;
        int screenHeight = screenSize.height - 100;

        int width = dimension.width;
        int height = dimension.height;
        if (width > screenWidth || height > screenHeight) {
            dimension = new Dimension(Math.min(screenWidth, width), Math.min(screenHeight, height));
        }

        panel.setSize(dimension);
        panel.setPreferredSize(dimension);
        panel.setMinimumSize(dimension);
        panel.setMaximumSize(dimension);
    }

    public static JPanel borderPanel(Component westComponent, Component centerComponent, Component eastComponent) {
        JPanel panel = new JPanel(new BorderLayout());
        if (Objects.nonNull(westComponent)) {
            panel.add(westComponent, BorderLayout.WEST);
        }
        if (Objects.nonNull(centerComponent)) {
            panel.add(centerComponent, BorderLayout.CENTER);
        }
        if (Objects.nonNull(eastComponent)) {
            panel.add(eastComponent, BorderLayout.EAST);
        }

        return panel;
    }

    public static JPanel settingPanel(String label, Component valueComponent, Dimension labelSize, Dimension valueSize, boolean nextLine) {
        JPanel settingPanel = new JPanel(new BorderLayout());

        JLabel titleLabel = new JLabel();
        titleLabel.setText(label);
        if (Objects.nonNull(labelSize)) {
            titleLabel.setPreferredSize(labelSize);
        }

        if (nextLine) {
            settingPanel.add(titleLabel, BorderLayout.NORTH);
        } else {
            settingPanel.add(titleLabel, BorderLayout.WEST);
        }

        settingPanel.add(valueComponent, BorderLayout.CENTER);
        if (Objects.nonNull(valueSize)) {
            valueComponent.setPreferredSize(valueSize);
        }

        return settingPanel;
    }

    public static JPanel settingPanel(String label, Component valueComponent, Dimension labelSize, Dimension valueSize) {
        return settingPanel(label, valueComponent, labelSize, valueSize, false);
    }

    public static JPanel settingPanel(String label, Component valueComponent) {
        return settingPanel(label, valueComponent, null, null, false);
    }

    public static JPanel settingPanel(String label, Component valueComponent, Dimension labelSize) {
        return settingPanel(label, valueComponent, labelSize, null, false);
    }

    public static JPanel combinePanel(String label, Component westComponent, Component centerComponent, Dimension labelSize, Dimension valueSize) {
        return combinePanel(label, westComponent, centerComponent, null, labelSize, valueSize);
    }

    public static JPanel combinePanel(String label, Component westComponent, Component centerComponent, Component eastComponent) {
        return combinePanel(label, westComponent, centerComponent, eastComponent, null, null);
    }

    public static JPanel combinePanel(String label, Component westComponent, Component centerComponent, Component eastComponent, Dimension labelSize, Dimension valueSize) {
        JPanel combinePanel = new JPanel(new BorderLayout());

        if (Objects.nonNull(westComponent)) {
            combinePanel.add(westComponent, BorderLayout.WEST);
        }
        if (Objects.nonNull(eastComponent)) {
            combinePanel.add(eastComponent, BorderLayout.EAST);
        }
        if (Objects.nonNull(centerComponent)) {
            combinePanel.add(centerComponent, BorderLayout.CENTER);
        }
        return JPanelUtils.settingPanel(label, combinePanel, labelSize, valueSize);
    }

    public static JPanel combinePanel(String label, Component westComponent, Component centerComponent) {
        return combinePanel(label, westComponent, centerComponent, null, null);
    }

    public static JPanel flowPanel(int align, Component... components) {
        JPanel flowPanel = new JPanel(new FlowLayout(align));
        for (Component component : components) {
            flowPanel.add(component);
        }

        return flowPanel;
    }

    public static JPanel gridPanel(GridLayout layout, JComponent... components) {
        JPanel panel = new JPanel(layout);
        for (JComponent component : components) {
            panel.add(component);
        }

        return panel;
    }

    public static JPanel vFlowPanel(int align, Component... components) {
        JPanel flowPanel = new JPanel(new VFlowLayout(align));
        for (Component component : components) {
            flowPanel.add(component);
        }

        return flowPanel;
    }

    public static JLabel tips(String tips, int iconWidth) {
        JLabel tipLabel = new JLabel(Icons.scaleToWidth(Icons.ABOUT, iconWidth));
        tipLabel.setToolTipText(tips);

        return tipLabel;
    }

    public static JTextArea tips() {
        return tips(null);
    }

    public static JTextArea tips(Color foreground) {
        JTextArea tipsArea = new JTextArea();
        tipsArea.setEditable(false);
        if (Objects.nonNull(foreground)) {
            tipsArea.setForeground(foreground);
        }
        tipsArea.setBorder(null);
        tipsArea.setOpaque(false);
        tipsArea.setBackground(JBColor.background());
        tipsArea.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        tipsArea.setLineWrap(true);

        return tipsArea;
    }

    public static ComboBox<String> createPrototypeComboBox() {
        ComboBox<String> prototypeComboBox = new ComboBox<>();
        prototypeComboBox.setSwingPopup(false);
        prototypeComboBox.setPrototypeDisplayValue("Prototype");

        return prototypeComboBox;
    }

    public static <T> ComboBox<T> createSearchComboBox() {
        ComboBox<T> prototypeComboBox = new ComboBox<>();
        prototypeComboBox.setSwingPopup(false);

        return prototypeComboBox;
    }

    public static <T> ComboBox<T> createSearchComboBox(T prototypeDisplayValue) {
        ComboBox<T> prototypeComboBox = new ComboBox<>();
        prototypeComboBox.setSwingPopup(false);
        prototypeComboBox.setPrototypeDisplayValue(prototypeDisplayValue);

        return prototypeComboBox;
    }

    public static JPanel singleCheckBox(JBCheckBox... checkboxes) {
        return singleCheckBox(null, checkboxes);
    }

    public static JPanel singleCheckBox(LayoutManager layout, JBCheckBox... checkboxes) {
        layout = ObjectUtils.defaultIfNull(layout, new FlowLayout(FlowLayout.LEFT));
        JPanel checkBoxPanel = new JPanel(layout);

        List<JBCheckBox> checkboxList = new ArrayList<>();

        for (JBCheckBox checkbox : checkboxes) {
            checkBoxPanel.add(checkbox);
            checkboxList.add(checkbox);
        }

        for (JBCheckBox checkbox : checkboxList) {
            checkbox.addActionListener(e -> {
                if (checkbox.isSelected()) {
                    checkboxList.stream().filter(c -> !checkbox.getText().equals(c.getText())).forEach(c -> c.setSelected(false));
                }
            });
        }

        return checkBoxPanel;
    }

    public static boolean checkSuccessAndShowErrMsg(Result<?> result, String errTitle, String errTips) {
        if (Objects.nonNull(result) && result.isSuccess()) {
            return true;
        }

        if (Objects.nonNull(result)) {
            Messages.showMessageDialog(
                    String.format("%s\n\n%s", StringUtils.defaultString(errTips), LowCodeAppUtils.getErrMsg(result)),
                    StringUtils.defaultString(errTitle), Icons.scaleToWidth(Icons.FAIL, 60)
            );
        } else {
            Messages.showMessageDialog(StringUtils.defaultString(errTips), StringUtils.defaultString(errTitle), Icons.scaleToWidth(Icons.FAIL, 60));
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getDialogComboBoxVal(ComboBox<T> comboBox, T defaultVal) {
        Object selectedItem = comboBox.getSelectedItem();
        return Objects.nonNull(selectedItem) ? (T) selectedItem : defaultVal;
    }

    public static void registerGlobalShortcut(int keyCode, int modifiers, Runnable action, @Nullable Disposable parent) {
        IdeEventQueue eventQueue = IdeEventQueue.getInstance();
        eventQueue.addDispatcher(e -> {
            if (!(e instanceof KeyEvent keyEvent)) return false;
            if (keyEvent.getKeyCode() == keyCode
                    && keyEvent.getModifiersEx() == modifiers
                    && keyEvent.getID() == KeyEvent.KEY_PRESSED) {
                action.run();
            }

            return false;
        }, parent);
    }

    public static void copyToClipboard(String str) {
        Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection selection = new StringSelection(str);
        systemClipboard.setContents(selection, null);
    }

    public static void scrollToBottom(boolean force, JBScrollPane scrollPane, int adjust) {
        // 获取视口
        JViewport viewport = scrollPane.getViewport();
        if (viewport != null) {
            // 获取视口中的视图组件
            Component view = viewport.getView();
            if (view != null) {
                // 确保视图已经布局完毕
                view.doLayout();
                // 滚动到最底部
                JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
                if (force || scrollBar.isVisible()) {
                    viewport.setViewPosition(new Point(0, view.getHeight() + adjust));
                }
            }
        }
    }

    public static void scrollToBottom(boolean force, JBScrollPane scrollPane) {
        // 获取视口
        scrollToBottom(force, scrollPane, 0);
    }

    public static void scrollToBottom(JBScrollPane scrollPane) {
        // 获取视口
        scrollToBottom(false, scrollPane, 0);
    }
}
