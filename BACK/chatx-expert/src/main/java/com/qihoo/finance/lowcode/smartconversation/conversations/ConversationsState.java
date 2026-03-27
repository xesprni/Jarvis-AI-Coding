package com.qihoo.finance.lowcode.smartconversation.conversations;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.OptionTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@State(
    name = "com.qihoo.finance.lowcode.smartconversation.conversations.ConversationsState",
    storages = @Storage("JarvisConversations_170.xml"))
public class ConversationsState implements PersistentStateComponent<ConversationsState> {

  @Deprecated
  @OptionTag(converter = ConversationsConverter.class)
  public ConversationsContainer conversationsContainer = new ConversationsContainer();

  @OptionTag(converter = ConversationConverter.class)
  public Conversation currentConversation;

  @OptionTag(converter = ConversationListConverter.class)
  public List<Conversation> conversations = new ArrayList<>();

  public boolean discardAllTokenLimits;

  public static ConversationsState getInstance() {
    return ApplicationManager.getApplication().getService(ConversationsState.class);
  }

  @Nullable
  @Override
  public ConversationsState getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull ConversationsState state) {
    XmlSerializerUtil.copyBean(state, this);

    if (this.conversations == null) {
      this.conversations = new ArrayList<>();
    }

    conversationsContainer.getConversationsMapping().values().stream()
        .flatMap(Collection::stream)
        .sorted(Comparator.comparing(Conversation::getUpdatedOn).reversed())
        .forEachOrdered(it -> conversations.add(it));
  }

  public void discardAllTokenLimits() {
    this.discardAllTokenLimits = true;
  }

  public void setCurrentConversation(@Nullable Conversation conversation) {
    this.currentConversation = conversation;
  }

  public static @Nullable Conversation getCurrentConversation() {
    return getInstance().currentConversation;
  }
}
