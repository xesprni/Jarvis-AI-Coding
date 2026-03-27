package com.qihoo.finance.lowcode.smartconversation.conversations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConversationsContainer {

  private Map<String, List<Conversation>> conversationsMapping = new HashMap<>();

  public Map<String, List<Conversation>> getConversationsMapping() {
    return conversationsMapping;
  }

  public void setConversationsMapping(Map<String, List<Conversation>> conversationsMapping) {
    this.conversationsMapping = conversationsMapping;
  }
}
