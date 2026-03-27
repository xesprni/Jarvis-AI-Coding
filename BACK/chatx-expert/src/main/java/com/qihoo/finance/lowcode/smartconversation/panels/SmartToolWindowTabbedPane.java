package com.qihoo.finance.lowcode.smartconversation.panels;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import com.qihoo.finance.lowcode.common.constants.Constants;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class SmartToolWindowTabbedPane extends JBTabbedPane {

    private final Map<String, TabbedPanelProperty> activeTabMapping = new LinkedHashMap<>();
    private final Disposable parentDisposable;
    private final Project project;

    private static final String PLUS_TAB_TITLE = "+";

    public SmartToolWindowTabbedPane(Disposable parentDisposable, Project project) {
        this.parentDisposable = parentDisposable;
        setTabComponentInsets(null);
        setComponentPopupMenu(new TabPopupMenu());
        addChangeListener(e -> refreshTabState());
        this.project = project;
        this.setBackground(Constants.Color.PANEL_BACKGROUND);
    }


    /**
     * 新建一个 Tab
     */
    public void addNewTab(SmartToolWindowTabPanel toolWindowPanel) {
        addNewTab(toolWindowPanel, UUID.randomUUID().toString().replaceAll("-", ""), "");
    }

    public void addNewTab(SmartToolWindowTabPanel toolWindowPanel, String taskId, String title) {
        // "+" tab 始终在最后，所以新 tab 要插到倒数第二个位置
        int plusIndex = indexOfTab(PLUS_TAB_TITLE);
        if (plusIndex == -1) {
            addPlusTab();
            plusIndex = indexOfTab(PLUS_TAB_TITLE);
        }
        if (title.isBlank()){
            title = getNextTitle();
        }
        String displayTitle = shortenTitle(title);
        super.insertTab(displayTitle, null, toolWindowPanel.getContent(), title, plusIndex);

        activeTabMapping.put(taskId, getTabProperty(title, toolWindowPanel, plusIndex));
        super.setSelectedIndex(plusIndex);

        setTabComponentAt(plusIndex, createCloseableTabButtonPanel(taskId, displayTitle));
        setToolTipTextAt(plusIndex, title);
        toolWindowPanel.requestFocusForTextArea();

        Disposer.register(parentDisposable, toolWindowPanel);
    }

    private JPanel createTitleOnlyTabPanel(String title) {
        return JBUI.Panels.simplePanel(4, 0)
                .addToLeft(new JBLabel(shortenTitle(title)))
                .andTransparent();
    }

    /**
     * 始终把 "+" 放到最后
     */
    private void addPlusTab() {
        JPanel plusPanel = new JPanel(); // 空内容
        super.addTab(PLUS_TAB_TITLE, plusPanel);
        setTabComponentAt(getTabCount() > 0 ? getTabCount() - 1 : 0, createPlusButtonTab());
    }

    private JPanel createPlusButtonTab() {
        JLabel label = new JLabel("+", SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 16f));
        //label.setForeground(new Color(80, 80, 80));
        label.setForeground(new JBColor(
                new Color(60, 60, 60),     // Light theme color
                new Color(220, 220, 220)   // Dark theme color
        ));
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(label, BorderLayout.CENTER);
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        panel.setBackground(Constants.Color.PANEL_BACKGROUND);
        panel.setPreferredSize(new Dimension(50, 40)); // 或你需要的大小
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String taskId = UUID.randomUUID().toString().replaceAll("-", "");
                addNewTab(new SmartToolWindowTabPanel(
                        project,
                        taskId
                ), taskId, "");
                movePlusTabToEnd();
            }
        });

        // 让整个 Panel 都能响应点击（包括 label 区域）
        label.setFocusable(false);
        label.setOpaque(false);

        return panel;
    }


    private void movePlusTabToEnd() {
        int plusIndex = indexOfTab(PLUS_TAB_TITLE);
        if (plusIndex != getTabCount() - 1 && plusIndex != -1) {
            Component comp = getComponentAt(plusIndex);
            removeTabAt(plusIndex);
            addTab(PLUS_TAB_TITLE, comp);
            setTabComponentAt(getTabCount() - 1, createPlusButtonTab());
        }
    }

    /**
     * 自动生成下一个 Tab 的标题
     */
    private String getNextTitle() {
        int maxIndex = 0;
        for (TabbedPanelProperty property : activeTabMapping.values()) {
            String title = property.getTitle();
            if (title.matches("Chat \\d+")) {
                String numberPart = title.replaceAll("\\D+", "");
                int tabNum = Integer.parseInt(numberPart);
                if (tabNum > maxIndex) {
                    maxIndex = tabNum;
                }
            }
        }
        return "Chat " + (maxIndex + 1);
    }

    /**
     * 将指定 tab 的标题重置为新的 "Chat X" 格式
     * @param taskId 要重置的 tab 的 taskId
     */
    public void resetTabTitle(String taskId) {
        TabbedPanelProperty property = activeTabMapping.get(taskId);
        if (property == null) {
            return;
        }
        String newTitle = getNextTitle();
        String displayTitle = shortenTitle(newTitle);
        setToolTipTextAt(property.getIndex(), newTitle);
        setTabComponentAt(property.getIndex(), createCloseableTabButtonPanel(taskId, displayTitle));
        property.setTitle(newTitle);
    }

    public Optional<SmartToolWindowTabPanel> tryFindActiveTabPanel() {
        var selectedIndex = getSelectedIndex();
        if (selectedIndex == -1) {
            return Optional.empty();
        }

        for (TabbedPanelProperty property : activeTabMapping.values()) {
            int index = property.getIndex();
            if (index == selectedIndex) {
                return Optional.ofNullable(property.getSmartToolWindowTabPanel());
            }
        }
        return Optional.empty();
    }

    public void clearAll() {
        removeAll();
        activeTabMapping.clear();
    }

    public boolean hasTabExist(String taskId) {
        TabbedPanelProperty property = activeTabMapping.get(taskId);
        return property != null;
    }

    public void renameTab(String taskId, String newName) {
        TabbedPanelProperty property = activeTabMapping.get(taskId);
        if (property == null) {
            return;
        }
        String uniqueName = ensureUniqueName(newName, property.getTitle());

        String displayTitle = shortenTitle(uniqueName);
        setToolTipTextAt(property.getIndex(), uniqueName);

        setTabComponentAt(property.getIndex(), createCloseableTabButtonPanel(taskId, displayTitle));
        property.setTitle(newName);
        activeTabMapping.remove(taskId);
        activeTabMapping.put(taskId, property);
    }

    String ensureUniqueName(String desiredName, String currentTitle) {
        String baseName = desiredName.trim();
        String uniqueName = baseName;
        int counter = 2;

        while (activeTabMapping.containsKey(uniqueName) && !uniqueName.equals(currentTitle)) {
            uniqueName = baseName + " (" + counter + ")";
            counter++;
        }

        return uniqueName;
    }

    private String shortenTitle(String title) {
        int maxLength = 12;
        if (title == null) return "";
        if (title.length() <= maxLength) return title;
        return title.substring(0, maxLength - 3) + "...";
    }

    private void refreshTabState() {
        var selectedIndex = getSelectedIndex();
        if (selectedIndex == -1) {
            return;
        }
    }

    public void trySwitchTab(String taskId) {
        activeTabMapping.forEach((key, value) -> {
            if (taskId.equals(key)) {
                super.setSelectedIndex(value.getIndex());
            }
        });
    }

    public SmartToolWindowTabPanel getTab(String taskId) {
        var tab = activeTabMapping.get(taskId);
        if (tab == null) {
            return null;
        }
        return tab.getSmartToolWindowTabPanel();
    }

    public Optional<String> tryFindTabTitle(UUID conversationId) {
        return activeTabMapping.entrySet().stream()
                .filter(entry -> {
                    var panelConversation = entry.getValue().getTitle();
                    return conversationId.equals(panelConversation);
                })
                .findFirst()
                .map(Map.Entry::getKey);
    }


    private JPanel createCloseableTabButtonPanel(String taskId, String title) {
        var closeIcon = AllIcons.Actions.Close;
        var button = new JButton(closeIcon);
        button.addActionListener(new CloseActionListener(taskId));
        button.setPreferredSize(new Dimension(closeIcon.getIconWidth(), closeIcon.getIconHeight()));
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setContentAreaFilled(false);
        button.setToolTipText("Close Chat");
        button.setRolloverIcon(AllIcons.Actions.CloseHovered);

        return JBUI.Panels.simplePanel(4, 0)
                .addToLeft(new JBLabel(shortenTitle(title)))
                .addToRight(button)
                .andTransparent();
    }


    private TabbedPanelProperty getTabProperty(String title, SmartToolWindowTabPanel tabPanel, int index) {
        TabbedPanelProperty tabbedPanelProperty = new TabbedPanelProperty();
        tabbedPanelProperty.setTitle(title);
        tabbedPanelProperty.setIndex(index);
        tabbedPanelProperty.setSmartToolWindowTabPanel(tabPanel);
        return tabbedPanelProperty;
    }

    class CloseActionListener implements ActionListener {

        private final String taskId;

        public CloseActionListener(String taskId) {
            this.taskId = taskId;
        }

        public void actionPerformed(ActionEvent evt) {
            TabbedPanelProperty tabbedPanelProperty = activeTabMapping.get(taskId);
            if (tabbedPanelProperty == null) {
                return;
            }
            Disposer.dispose(tabbedPanelProperty.getSmartToolWindowTabPanel());
            removeTabAt(tabbedPanelProperty.getIndex());
            activeTabMapping.remove(taskId);
            int index = 0;
            for (Map.Entry<String, TabbedPanelProperty> entry : activeTabMapping.entrySet()) {
                entry.getValue().setIndex(index++);
            }

            if (!activeTabMapping.isEmpty()) {
                int newIndex = Math.max(0, tabbedPanelProperty.getIndex() - 1);
                setSelectedIndex(newIndex);
            } else {
                String taskId = UUID.randomUUID().toString().replaceAll("-", "");
                addNewTab(new SmartToolWindowTabPanel(project,taskId), taskId, "");
            }
        }
    }

    class TabPopupMenu extends JPopupMenu {

        private int selectedPopupTabIndex = -1;

        TabPopupMenu() {
            add(createPopupMenuItem("Close", e -> {
                if (selectedPopupTabIndex > 0) {
                    activeTabMapping.entrySet().removeIf(entry -> entry.getValue().getIndex() == selectedPopupTabIndex);
                    removeTabAt(selectedPopupTabIndex);
                }
            }));
            add(createPopupMenuItem("Close Other Tabs", e -> {
                Optional<Map.Entry<String, TabbedPanelProperty>> tabbedPanelProperty = activeTabMapping.entrySet().stream().filter(entry -> entry.getValue().getIndex() == selectedPopupTabIndex).findFirst();
                if (tabbedPanelProperty.isPresent()) {
                    clearAll();
                    addNewTab(tabbedPanelProperty.get().getValue().getSmartToolWindowTabPanel());
                }
            }));
        }

        @Override
        public void show(Component invoker, int x, int y) {
            selectedPopupTabIndex = SmartToolWindowTabbedPane.this.getUI()
                    .tabForCoordinate(SmartToolWindowTabbedPane.this, x, y);
            if (selectedPopupTabIndex > 0) {
                super.show(invoker, x, y);
            }
        }

        private JBMenuItem createPopupMenuItem(String label, ActionListener listener) {
            var menuItem = new JBMenuItem(label);
            menuItem.addActionListener(listener);
            return menuItem;
        }
    }

    @Getter
    @Setter
    @ToString
    public static class TabbedPanelProperty {
        private String title;
        private int index;
        private SmartToolWindowTabPanel smartToolWindowTabPanel;
    }
}
