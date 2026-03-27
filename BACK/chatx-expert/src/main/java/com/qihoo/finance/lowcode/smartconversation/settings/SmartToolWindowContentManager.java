package com.qihoo.finance.lowcode.smartconversation.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentContainer;
import com.intellij.openapi.wm.RegisterToolWindowTask;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.qifu.ui.smartconversation.settings.prompts.PromptsSettings;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.smartconversation.conversations.Conversation;
import com.qihoo.finance.lowcode.smartconversation.conversations.ConversationsState;
import com.qihoo.finance.lowcode.smartconversation.conversations.Message;
import com.qihoo.finance.lowcode.smartconversation.panels.SmartToolWindowPanel;
import com.qihoo.finance.lowcode.smartconversation.panels.SmartToolWindowTabPanel;
import com.qihoo.finance.lowcode.smartconversation.panels.SmartToolWindowTabbedPane;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Service(Service.Level.PROJECT)
public final class SmartToolWindowContentManager {

  private final Project project;

  public SmartToolWindowContentManager(Project project) {
    this.project = project;
  }

  public void sendMessageInNewTab(Message message) {
    createNewTabPanel().sendMessage(message);
  }

  public void sendMessage(Message message) {
    getToolWindow().show();

    var startInNewWindow = ApplicationManager.getApplication().getService(PromptsSettings.class)
        .getState()
        .getChatActions()
        .getStartInNewWindow();
    if (startInNewWindow || ConversationsState.getCurrentConversation() == null) {
      createNewTabPanel().sendMessage(message);
      return;
    }

    tryFindChatTabbedPane()
        .map(tabbedPane -> tabbedPane.tryFindActiveTabPanel().orElseGet(this::createNewTabPanel))
        .orElseGet(this::createNewTabPanel)
        .sendMessage(message);
  }

  public Optional<SmartToolWindowTabPanel> tryFindActiveChatTabPanel() {
    return tryFindChatTabbedPane().flatMap(SmartToolWindowTabbedPane::tryFindActiveTabPanel);
  }

  public void displayConversation(@NotNull Conversation conversation) {
    displayChatTab();
    tryFindChatTabbedPane()
        .ifPresent(tabbedPane -> tabbedPane.tryFindTabTitle(conversation.getId())
            .ifPresentOrElse(
                title -> tabbedPane.setSelectedIndex(tabbedPane.indexOfTab(title)),
                () -> tabbedPane.addNewTab(new SmartToolWindowTabPanel(project,null))));
  }

  public SmartToolWindowTabPanel createNewTabPanel() {
    displayChatTab();
    return tryFindChatTabbedPane()
        .map(item -> {
          var panel = new SmartToolWindowTabPanel(project, null);
          item.addNewTab(panel);
          return panel;
        })
        .orElseThrow();
  }

  public void displayChatTab() {
    var toolWindow = getToolWindow();
    toolWindow.show();

    var contentManager = toolWindow.getContentManager();
    tryFindFirstChatTabContent().ifPresentOrElse(
        contentManager::setSelectedContent,
        () -> contentManager.setSelectedContent(requireNonNull(contentManager.getContent(0)))
    );
  }

  public Optional<SmartToolWindowTabbedPane> tryFindChatTabbedPane() {
    var chatTabContent = tryFindFirstChatTabContent();
    if (chatTabContent.isPresent()) {
      var chatToolWindowPanel = (SmartToolWindowPanel) chatTabContent.get().getComponent();
      return Optional.of(chatToolWindowPanel.getChatTabbedPane());
    }
    return Optional.empty();
  }

  public Optional<SmartToolWindowPanel> tryFindChatToolWindowPanel() {
    return tryFindFirstChatTabContent()
        .map(ComponentContainer::getComponent)
        .filter(component -> component instanceof SmartToolWindowPanel)
        .map(component -> (SmartToolWindowPanel) component);
  }

  public void resetAll() {
    tryFindChatTabbedPane().ifPresent(tabbedPane -> {
      tabbedPane.clearAll();
      tabbedPane.addNewTab(new SmartToolWindowTabPanel(project,null));
    });
  }

  public @NotNull ToolWindow getToolWindow() {
    var toolWindowManager = ToolWindowManager.getInstance(project);
    var toolWindow = toolWindowManager.getToolWindow("Jarvis");
    // https://intellij-support.jetbrains.com/hc/en-us/community/posts/11533368171026/comments/11538403084562
    return Objects.requireNonNullElseGet(toolWindow, () -> toolWindowManager
        .registerToolWindow(RegisterToolWindowTask.closable(
            "Jarvis",
            () -> "Jarvis",
            Icons.LOGO_ROUND,
            ToolWindowAnchor.RIGHT)));
  }

  private Optional<Content> tryFindFirstChatTabContent() {
    return Arrays.stream(getToolWindow().getContentManager().getContents())
        .filter(content -> "Chat".equals(content.getTabName()))
        .findFirst();
  }

  public void clearAllTags() {
    tryFindActiveChatTabPanel().ifPresent(SmartToolWindowTabPanel::clearAllTags);
  }
}