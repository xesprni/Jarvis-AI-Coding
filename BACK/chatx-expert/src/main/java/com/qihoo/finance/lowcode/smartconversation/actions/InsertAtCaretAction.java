package com.qihoo.finance.lowcode.smartconversation.actions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.smartconversation.utils.OverlayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Optional;

import static com.intellij.openapi.application.ActionsKt.runUndoTransparentWriteAction;

public class InsertAtCaretAction extends AbstractAction {

  private final @NotNull Editor toolwindowEditor;
  private final @Nullable Point locationOnScreen;

  public InsertAtCaretAction(
      @NotNull EditorEx toolwindowEditor,
      @Nullable Point locationOnScreen) {
    super(
        "Insert at Caret",
        Icons.SendToTheLeft);
    this.toolwindowEditor = toolwindowEditor;
    this.locationOnScreen = locationOnScreen;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Editor mainEditor = getSelectedTextEditor();
    if (mainEditor == null) {
      OverlayUtil.showWarningBalloon("Active editor not found", locationOnScreen);
      return;
    }

    insertTextAtCaret(mainEditor);
  }

  @Nullable
  private Editor getSelectedTextEditor() {
    return Optional.ofNullable(toolwindowEditor.getProject())
        .map(FileEditorManager::getInstance)
        .map(FileEditorManager::getSelectedTextEditor)
        .orElse(null);
  }

  private void insertTextAtCaret(Editor mainEditor) {
    runUndoTransparentWriteAction(() -> {
      mainEditor.getDocument().insertString(
          mainEditor.getCaretModel().getOffset(),
          toolwindowEditor.getDocument().getText());
      return null;
    });
  }
}
