package com.qihoo.finance.lowcode.smartconversation.conversations;

import com.qihoo.finance.lowcode.smartconversation.service.ReferencedFile;

import java.util.*;

public class ChatSession {

  private final UUID id;
  private final Set<ReferencedFile> referencedFiles;
  private String name;

  public ChatSession() {
    this.id = UUID.randomUUID();
    this.referencedFiles = new HashSet<>();
  }

  public UUID getId() {
    return id;
  }

  public List<ReferencedFile> getReferencedFiles() {
    return new ArrayList<>(referencedFiles);
  }

  public void addReferencedFiles(List<ReferencedFile> files) {
    if (files == null) {
      throw new IllegalArgumentException("Referenced files cannot be null");
    }
    referencedFiles.addAll(files);
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public String getDisplayName() {
    return name != null && !name.trim().isEmpty() ? name : null;
  }
}
