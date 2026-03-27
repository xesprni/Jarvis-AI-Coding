package com.qihoo.finance.lowcode.smartconversation.conversations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qihoo.finance.lowcode.smartconversation.utils.BaseConverter;

public class ConversationsConverter extends BaseConverter<ConversationsContainer> {

  public ConversationsConverter() {
    super(new TypeReference<>() {});
  }
}
