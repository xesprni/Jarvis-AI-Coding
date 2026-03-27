package com.qihoo.finance.lowcode.editor;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.util.containers.ContainerUtil;
import com.qihoo.finance.lowcode.editor.request.EditorRequest;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ChatxEditorUtil {

    static final Key<List<EditorRequest>> KEY_REQUESTS = Key.create("chatx.editorRequests");

    public static boolean isFocusedEditor(@NotNull Editor editor) {
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return true;
        }
        return editor.getContentComponent().isFocusOwner();
    }

    public static boolean isSelectedEditor(@NotNull Editor editor) {
        Project project = editor.getProject();
        if (project == null || project.isDisposed()) {
            return false;
        }
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        if (editorManager == null) {
            return false;
        }
        if (editorManager instanceof FileEditorManagerImpl) {
            Editor editor1 = ((FileEditorManagerImpl)editorManager).getSelectedTextEditor(true);
            return (editor1 != null && editor1.equals(editor));
        }
        FileEditor current = editorManager.getSelectedEditor();
        return (current instanceof TextEditor && editor.equals(((TextEditor)current).getEditor()));
    }

    public static long getDocumentModificationStamp(@NotNull Document document) {
        return (document instanceof DocumentEx) ? (
                (DocumentEx)document).getModificationSequence() :
                document.getModificationStamp();
    }

    public static int whitespacePrefixLength(@NotNull String lineContent, int tabWidth) {
        int maxLength = lineContent.length();
        int i = 0;
        int whiteSpaceLen = 0;
        for (; i < maxLength; i++) {
            char c = lineContent.charAt(i);
            if (c == ' ') {
                whiteSpaceLen++;
            } else if (c == '\t') {
                whiteSpaceLen += tabWidth;
            } else {
                break;
            }
        }
        return whiteSpaceLen;
    }

    public static void addEditorRequest(@NotNull Editor editor, @NotNull EditorRequest request) {
        EditorUtil.disposeWithEditor(editor, request.getDisposable());
        if (!KEY_REQUESTS.isIn(editor)) {
            KEY_REQUESTS.set(editor, ContainerUtil.createLockFreeCopyOnWriteList());
        }
        KEY_REQUESTS.getRequired(editor).add(request);
    }

}
