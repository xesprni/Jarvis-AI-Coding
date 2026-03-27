package com.qihoo.finance.lowcode.editor;

import com.intellij.openapi.command.CommandEvent;
import com.intellij.openapi.command.CommandListener;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
public class ChatxCommandListener implements CommandListener {

    private final static Logger LOG = Logger.getInstance(ChatxCommandListener.class);
    private static final Key<CommandEditorState> COMMAND_STATE_KEY = Key.create("chatx.commandState");
    private final Project project;
    private final AtomicInteger activeCommands = new AtomicInteger();
    private final AtomicBoolean startedWithEditor = new AtomicBoolean(false);
    private final AtomicReference<UndoTransparentActionState> undoTransparentActionStamp = new AtomicReference<>();

    @Override
    public void commandStarted(@NotNull CommandEvent event) {
        if (activeCommands.getAndIncrement() > 0) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Skipping nested commandStarted. Event: " + event);
            }
            return;
        }
        Editor editor = getSelectedEditor(this.project);
        if (editor != null) {
            startedWithEditor.set(true);
            COMMAND_STATE_KEY.set(editor, createCommandState(editor));
        } else {
            startedWithEditor.set(false);
        }
    }

    @Override
    public void commandFinished(@NotNull CommandEvent event) {
        if (activeCommands.decrementAndGet() > 0) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Skipping nested commandFinished. Event: " + event);
            }
            return;
        }
        if (ChatxEditorActionTracker.getInstance().isExecutingForcedCompletionAction()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Skipping command because editor action listener will enforce completion");
                return;
            }
        }
        if (!startedWithEditor.get()) {
            return;
        }
        Editor editor = getSelectedEditor(project);
        if (editor == null) {
            return;
        }
        ChatxEditorManager editorManager = ChatxEditorManager.getInstance();
        if (!editorManager.isAvailable(editor)) {
            return;
        }
        CommandEditorState commandStartState = COMMAND_STATE_KEY.get(editor);
        if (commandStartState == null) {
            return;
        }
        if (event.getCommandName() != null
                && ("Paste".equals(event.getCommandName()) || event.getCommandName().startsWith("Undo")
                || "Backspace".equals(event.getCommandName()) || "Redo".equals(event.getCommandName())
                || "Cut".equals(event.getCommandName())) || "Delete".equals(event.getCommandName())) {
            editorManager.disposeInlays(editor, InlayDisposeContext.CaretChange);
            return;
        }
        CommandEditorState commandEndState = createCommandState(editor);
        if (isDocumentModification(commandStartState, commandEndState)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("command modified document: " + event.getCommandName());
            }
            if (!"Apply ChatX Suggestion".equals(event.getCommandName())) {
                if ("Choose Lookup Item".equals(event.getCommandName())) {
                    editorManager.editorModified(editor, CompletionRequestType.Forced);
                } else {
                    editorManager.editorModified(editor, CompletionRequestType.Automatic);
                }
            }
        } else if (isCaretPositionChange(commandStartState, commandEndState)) {
            editorManager.disposeInlays(editor, InlayDisposeContext.CaretChange);
        }
    }

    @Override
    public void undoTransparentActionStarted() {
        Editor editor = getSelectedEditor(this.project);
        undoTransparentActionStamp.set(editor != null ? createUndoTransparentState(editor) : null);
    }

    @Override
    public void undoTransparentActionFinished() {
        UndoTransparentActionState undoStartState = undoTransparentActionStamp.get();
        undoTransparentActionStamp.set(null);
        Editor editor = getSelectedEditor(project);
        if (editor == null || undoStartState == null || undoStartState.getEditor() != editor) {
            return;
        }
        UndoTransparentActionState undoEndState = createUndoTransparentState(editor);
        if (undoStartState.getModificationStamp() == undoEndState.getModificationStamp()) {
            return;
        }
        ChatxEditorManager editorManager = ChatxEditorManager.getInstance();
        if (editorManager.isAvailable(editor) && editorManager.hasCompletionInlays(editor)) {
            editorManager.editorModified(editor, CompletionRequestType.Forced);
        }
    }

    private static boolean isDocumentModification(@NotNull CommandEditorState first, @NotNull CommandEditorState second) {
        return first.modificationStamp != second.modificationStamp;
    }

    private static boolean isCaretPositionChange(@NotNull CommandEditorState first, @NotNull CommandEditorState second) {
        return !first.visualPosition.equals(second.visualPosition);
    }

    @Nullable
    private static Editor getSelectedEditor(@NotNull Project project) {
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        try {
            return (editorManager != null) ? editorManager.getSelectedTextEditor() : null;
        } catch (Exception e) {
            return null;
        }
    }

    @NotNull
    private static CommandEditorState createCommandState(@NotNull Editor editor) {
        return new CommandEditorState(getDocumentStamp(editor.getDocument()), editor.getCaretModel().getVisualPosition());
    }

    @NotNull
    private static UndoTransparentActionState createUndoTransparentState(@NotNull Editor editor) {
        return new UndoTransparentActionState(editor, getDocumentStamp(editor.getDocument()));
    }

    private static long getDocumentStamp(@NotNull Document document) {
        if (document instanceof DocumentEx) {
            return ((DocumentEx)document).getModificationSequence();
        }
        return document.getModificationStamp();
    }

    @Getter
    private static final class UndoTransparentActionState {
        private final Editor editor;
        private final long modificationStamp;

        public UndoTransparentActionState(@NotNull Editor editor, long modificationStamp) {
            this.editor = editor;
            this.modificationStamp = modificationStamp;
        }

    }

    @Getter
    @RequiredArgsConstructor
    private static final class CommandEditorState {

        private final long modificationStamp;
        private final VisualPosition visualPosition;
    }
}
