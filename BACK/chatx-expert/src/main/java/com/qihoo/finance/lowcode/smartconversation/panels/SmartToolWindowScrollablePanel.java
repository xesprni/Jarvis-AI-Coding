package com.qihoo.finance.lowcode.smartconversation.panels;


import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel;
import com.intellij.openapi.roots.ui.componentsList.layout.VerticalStackLayout;
import com.qifu.ui.smartconversation.panels.ResponseMessagePanel;
import com.qihoo.finance.lowcode.common.constants.Constants;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author weiyichao
 * @date 2025-09-18
 **/
public class SmartToolWindowScrollablePanel extends ScrollablePanel {
    private final Map<UUID, JPanel> visibleMessagePanels = new HashMap<>();
    private final List<UUID> messageOrder = new ArrayList<>();

    public SmartToolWindowScrollablePanel() {
        super(new VerticalStackLayout());
        setBackground(Constants.Color.PANEL_BACKGROUND);
    }

    public void displayLandingView(JComponent landingView) {
        clearAll();
        add(landingView);
    }

    public ResponseMessagePanel getResponseMessagePanel(UUID messageId) {
        return (ResponseMessagePanel) Arrays.stream(visibleMessagePanels.get(messageId).getComponents())
                .filter(ResponseMessagePanel.class::isInstance)
                .findFirst().orElseThrow();
    }

    public JPanel addMessage(UUID messageId) {
        var messageWrapper = new JPanel();
        messageWrapper.setLayout(new BoxLayout(messageWrapper, BoxLayout.PAGE_AXIS));
        add(messageWrapper);
        visibleMessagePanels.put(messageId, messageWrapper);
        messageOrder.add(messageId);
        return messageWrapper;
    }

    public void removeMessage(UUID messageId) {
        var panel = visibleMessagePanels.get(messageId);
        if (panel == null) {
            return;
        }
        remove(panel);
        update();
        visibleMessagePanels.remove(messageId);
        messageOrder.remove(messageId);
    }

    public void removeMessagesFrom(UUID messageId) {
        int startIndex = messageOrder.indexOf(messageId);
        if (startIndex < 0) {
            return;
        }
        var toRemove = new ArrayList<>(messageOrder.subList(startIndex, messageOrder.size()));
        toRemove.forEach(this::removeMessage);
    }

    public boolean isEmptyMessages() {
        return messageOrder.isEmpty();
    }

    public void clearAll() {
        visibleMessagePanels.clear();
        messageOrder.clear();
        removeAll();
        update();
    }

    public void scrollToBottom() {
        scrollRectToVisible(new Rectangle(0, getHeight(), 1, 1));
    }

    public void update() {
        repaint();
        revalidate();
    }
}
