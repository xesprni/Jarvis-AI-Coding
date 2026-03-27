package com.qihoo.finance.lowcode.smartconversation.conversations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qihoo.finance.lowcode.smartconversation.utils.BaseConverter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ConversationListConverter extends BaseConverter<List<Conversation>> {

  public ConversationListConverter() {
    super(new TypeReference<>() {});
  }
  
  @Override
  public List<Conversation> fromString(@NotNull String value) {
    List<Conversation> result = super.fromString(value);
    return result != null ? result : new ArrayList<>();
  }
}