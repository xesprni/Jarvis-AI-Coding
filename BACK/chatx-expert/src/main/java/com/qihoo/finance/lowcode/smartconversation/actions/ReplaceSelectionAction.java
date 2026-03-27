package com.qihoo.finance.lowcode.smartconversation.actions;

import com.intellij.icons.AllIcons.Actions;
import com.intellij.openapi.editor.ex.EditorEx;
import com.qifu.ui.utils.EditorUtil;
import com.qihoo.finance.lowcode.smartconversation.utils.OverlayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

import static java.util.Objects.requireNonNull;

public class ReplaceSelectionAction extends AbstractAction {

  private final @NotNull EditorEx toolwindowEditor;
  private final Point locationOnScreen;

  public ReplaceSelectionAction(
      @NotNull EditorEx toolwindowEditor,
      @Nullable Point locationOnScreen) {
    super(
        "Replace Selection",
        Actions.Replace);
    this.toolwindowEditor = toolwindowEditor;
    this.locationOnScreen = locationOnScreen;
  }

  @Override
  public void actionPerformed(ActionEvent event) {
    var project = requireNonNull(toolwindowEditor.getProject());
    if (EditorUtil.isMainEditorTextSelected(project)) {
      var mainEditor = EditorUtil.getSelectedEditor(project);
      if (mainEditor != null) {
        EditorUtil.replaceEditorSelection(mainEditor, toolwindowEditor.getDocument().getText());
      }
    } else {
      OverlayUtil.showSelectedEditorSelectionWarning(project, locationOnScreen);
    }
  }
}
