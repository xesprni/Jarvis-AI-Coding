package com.qihoo.finance.lowcode.smartconversation.panels;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.ui.components.BorderLayoutPanel;
import com.qifu.agent.mcp.McpClientHub;
import com.qifu.ui.settings.SettingsOverlayPanel;
import com.qifu.utils.Conversation;
import com.qifu.utils.ConversationStore;
import com.qihoo.finance.lowcode.common.constants.Constants;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.UUID;

import static com.qihoo.finance.lowcode.smartconversation.configuration.JarvisKeys.IMAGE_ATTACHMENT_FILE_PATH;

public class SmartToolWindowPanel extends SimpleToolWindowPanel {

    private final SmartToolWindowTabbedPane tabbedPane;

    private final Project project;
    private final Disposable parentDisposable;

    private final SmartToolWindowFooterNotification imageFileAttachmentNotification;

    private static final String CARD_MAIN = "main";
    private static final String CARD_SETTINGS = "settings";

    private CardLayout contentLayout;
    private JPanel contentContainer;
    private SettingsOverlayPanel settingsOverlayPanel;

    public SmartToolWindowPanel(@NotNull Project project, @NotNull Disposable parentDisposable) {
        super(true);
        this.project = project;
        this.parentDisposable = parentDisposable;
        this.imageFileAttachmentNotification = new SmartToolWindowFooterNotification(() -> project.putUserData(IMAGE_ATTACHMENT_FILE_PATH, null));
        String taskId = UUID.randomUUID().toString().replaceAll("-", "");
        String taskTitle = "";
        var tabPanel = new SmartToolWindowTabPanel(project, taskId);
        this.tabbedPane = new SmartToolWindowTabbedPane(parentDisposable, project);

        this.tabbedPane.addNewTab(tabPanel, taskId, taskTitle);
        initToolWindowPanel();
        Disposer.register(parentDisposable, tabPanel);
    }


    private void initToolWindowPanel() {
        ApplicationManager.getApplication().invokeLater(() -> {
            BorderLayoutPanel mainPanel = new BorderLayoutPanel().addToCenter(tabbedPane);
            mainPanel.setBackground(Constants.Color.PANEL_BACKGROUND);
            contentLayout = new CardLayout();
            contentContainer = new JPanel(contentLayout);
            contentContainer.setOpaque(false);
            contentContainer.add(mainPanel, CARD_MAIN);
            settingsOverlayPanel = new SettingsOverlayPanel(project, parentDisposable, () -> {
                hideSettingsOverlay();
                return Unit.INSTANCE;
            });
            contentContainer.add(settingsOverlayPanel, CARD_SETTINGS);
            setContent(contentContainer);
            contentLayout.show(contentContainer, CARD_MAIN);
        });
        McpClientHub.getInstance(project).initialize();
    }


    private Conversation getCurrentTask() {
        var conversationList = ConversationStore.getConversations(project);
        if (conversationList.isEmpty()) {
            return null;
        }
        return conversationList.get(0);
    }


    public SmartToolWindowTabbedPane getChatTabbedPane() {
        return this.tabbedPane;
    }

    public void showSettingsOverlay() {
        showSettingsOverlay(SettingsOverlayPanel.TAB_MCP);
    }

    public void showSettingsOverlay(String tabId) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (settingsOverlayPanel == null || contentLayout == null || contentContainer == null) {
                return;
            }
            settingsOverlayPanel.selectTab(tabId, true);
            contentLayout.show(contentContainer, CARD_SETTINGS);
        });
    }

    public void hideSettingsOverlay() {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (contentLayout != null && contentContainer != null) {
                contentLayout.show(contentContainer, CARD_MAIN);
            }
        });
    }


    public void clearImageNotifications(Project project) {
        imageFileAttachmentNotification.hideNotification();
        project.putUserData(IMAGE_ATTACHMENT_FILE_PATH, null);
    }
}
