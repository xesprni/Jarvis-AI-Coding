package com.qihoo.finance.lowcode.aiquestion.ui.component;

import com.intellij.ide.highlighter.HighlighterFactory;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.HorizontalScrollBarEditorCustomization;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.BorderFactory;

public class SoftWrapHorizontalScrollBarEditor extends EditorTextField  {

    public SoftWrapHorizontalScrollBarEditor(Project project, FileType fileType) {
        super(project, fileType);
    }

    public SoftWrapHorizontalScrollBarEditor(@NotNull String text, Project project, FileType fileType) {
        super(text, project, fileType);
    }

    @Override
    protected @NotNull EditorEx createEditor() {
        EditorEx editor = super.createEditor();
        editor.setBorder(BorderFactory.createLineBorder(JBUI.CurrentTheme.Editor.BORDER_COLOR, 1));
        HorizontalScrollBarEditorCustomization.ENABLED.customize(editor);
        editor.setHighlighter(HighlighterFactory.createHighlighter(getProject(), getFileType()));
        editor.getSettings().setUseSoftWraps(true);
        return editor;
    }
}
