/*
 * Copyright (c) 2018 David Boissier.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qihoo.finance.lowcode.console.mysql.editor;

import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.textCompletion.TextFieldWithCompletion;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.entity.dto.SQLExecuteResult;
import com.qihoo.finance.lowcode.common.enums.LightVirtualType;
import com.qihoo.finance.lowcode.console.mysql.execute.action.SQLCommentAction;
import com.qihoo.finance.lowcode.console.mysql.execute.action.SQLFormatAction;
import com.qihoo.finance.lowcode.console.mysql.execute.completion.SQLCompletionProvider;
import com.qihoo.finance.lowcode.gentracker.ui.base.EditorSettingsInit;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.beans.PropertyChangeListener;

@Getter
public class SQLDataEditor extends UserDataHolderBase implements FileEditor {
    public static final Key<Result<SQLExecuteResult>> EXECUTE_RESULT = Key.create("SQLExecuteResult");
    private boolean disposed;
    private final Editor editor;
    private final SQLVirtualFile SQLVirtualFile;
    private final SQLCommentAction commentAction;
    private final SQLFormatAction formatAction;

    public SQLDataEditor(Project project, SQLVirtualFile sqlVirtualFile) {
        this.SQLVirtualFile = sqlVirtualFile;

        TextFieldWithCompletion completion = new TextFieldWithCompletion(project,
                new SQLCompletionProvider(), "", false, true, true, true, true);
        EditorFactory editorFactory = EditorFactory.getInstance();
        this.editor = editorFactory.createEditor(completion.getDocument(), project);
        EditorSettingsInit.init(editor);
        EditorEx editorEx = (EditorEx) editor;
        editorEx.setHighlighter(EditorHighlighterFactory.getInstance().createEditorHighlighter(project, new LightVirtualFile(LightVirtualType.SQL.getValue())));

        this.commentAction = new SQLCommentAction(project, editor);
        this.formatAction = new SQLFormatAction(project, editor);
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return isDisposed() ? new JPanel() : this.editor.getComponent();
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return this.editor.getComponent();
    }

    @NotNull
    @Override
    public String getName() {
        return "MySQL Data";
    }

    @Override
    public VirtualFile getFile() {
        return SQLVirtualFile;
    }

    @Override
    public void dispose() {
        if (!disposed) {
            disposed = true;
        }
    }

    private boolean isDisposed() {
        return disposed;
    }

    @NotNull
    @Override
    public FileEditorState getState(@NotNull FileEditorStateLevel level) {
        return FileEditorState.INSTANCE;
    }

//    Unused methods

    @Override
    public void setState(@NotNull FileEditorState state) {

    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void selectNotify() {

    }

    @Override
    public void deselectNotify() {

    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {

    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {

    }

    @Nullable
    @Override
    public BackgroundEditorHighlighter getBackgroundHighlighter() {
        return null;
    }

    @Nullable
    @Override
    public FileEditorLocation getCurrentLocation() {
        return null;
    }

    @Nullable
    @Override
    public StructureViewBuilder getStructureViewBuilder() {
        return null;
    }
}
