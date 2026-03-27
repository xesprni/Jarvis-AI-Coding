package com.qihoo.finance.lowcode.editor;

import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.BulkAwareDocumentListener;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.event.SelectionEvent;
import com.intellij.openapi.editor.event.SelectionListener;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

public class ChatxEditorListener implements EditorFactoryListener {

    private final ChatxSelectionListener selectionListener = new ChatxSelectionListener();

    @Override
    public void editorCreated(@NotNull EditorFactoryEvent event) {
        Editor editor = event.getEditor();
        Project project = editor.getProject();
        if (project == null || project.isDisposed()) {
            return;
        }
        // editor创建的时候，添加插入光标、文档 监听事件
        Disposable editorDisposable = Disposer.newDisposable("ChatxEditorListener");
        EditorUtil.disposeWithEditor(editor, editorDisposable);
        editor.getCaretModel().addCaretListener(new ChatxCaretListener(editor), editorDisposable);
        editor.getDocument().addDocumentListener(new ChatxDocumentListener(editor), editorDisposable);
        editor.getSelectionModel().addSelectionListener(this.selectionListener, editorDisposable);
    }

    /**
     * 监听插入光标改变事件
     */
    @RequiredArgsConstructor
    private static final class ChatxCaretListener implements CaretListener {

        private final Editor editor;

        @Override
        public void caretPositionChanged(@NotNull CaretEvent event) {
            Project project = editor.getProject();
            if (project == null || project.isDisposed()) {
                return;
            }
            boolean wasTypeOver = TypeOverHandler.getPendingTypeOverAndReset(this.editor);
            if (wasTypeOver) {
                ChatxEditorManager.getInstance().editorModified(editor, CompletionRequestType.Forced);
                return;
            }
            if (CommandProcessor.getInstance().getCurrentCommand() != null) {
                return;
            }
            // 判断当前是否有活动的代码补全
            if (ChatxApplicationSettings.settings().isShowIdeCompletions()
                    && LookupManager.getActiveLookup(editor) != null) {
                if (ChatxEditorManager.getInstance().hasCompletionInlays(editor)) {
                    ChatxEditorManager.getInstance().editorModified(editor, CompletionRequestType.Automatic);
                }
                return;
            }
            ChatxEditorManager.getInstance().disposeInlays(editor, InlayDisposeContext.CaretChange);
        }
    }

    @RequiredArgsConstructor
    private static final class ChatxDocumentListener implements BulkAwareDocumentListener {

        private final Editor editor;

        @Override
        public void documentChangedNonBulk(@NotNull DocumentEvent event) {
            Project project = editor.getProject();
            if (project == null || project.isDisposed()) {
                return;
            }
            if (!ChatxEditorUtil.isSelectedEditor(editor)) {
                return;
            }
            ChatxEditorManager editorManager = ChatxEditorManager.getInstance();
            if (!editorManager.isAvailable(this.editor)) {
                return;
            }
            CommandProcessor commandProcessor = CommandProcessor.getInstance();
            if (commandProcessor.isUndoTransparentActionInProgress()) {
                return;
            }
            if (commandProcessor.getCurrentCommandName() != null)
                return;
            int changeOffset = event.getOffset() + event.getNewLength();
            if (this.editor.getCaretModel().getOffset() != changeOffset)
                return;
            CompletionRequestType requestType = (event.getOldLength() != event.getNewLength()) ?
                    CompletionRequestType.Forced : CompletionRequestType.Automatic;
            editorManager.editorModified(this.editor, changeOffset, requestType);
        }
    }

    private static class ChatxSelectionListener implements SelectionListener {
        public void selectionChanged(@NotNull SelectionEvent e) {
            Editor editor = e.getEditor();
            Project project = editor.getProject();
            if (project == null || project.isDisposed())
                return;
            if (!ChatxEditorUtil.isSelectedEditor(editor))
                return;
            ChatxEditorManager.getInstance().disposeInlays(editor, InlayDisposeContext.SelectionChange);
        }
    }

}
