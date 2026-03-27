package com.qihoo.finance.lowcode.smartconversation.conversations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qihoo.finance.lowcode.smartconversation.utils.BaseConverter;

public class ConversationConverter extends BaseConverter<Conversation> {

  public ConversationConverter() {
    super(new TypeReference<>() {});
  }
}
