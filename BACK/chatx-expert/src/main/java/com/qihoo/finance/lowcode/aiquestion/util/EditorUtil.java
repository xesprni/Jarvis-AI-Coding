package com.qihoo.finance.lowcode.aiquestion.util;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.qihoo.finance.lowcode.common.constants.CompletionType;
import com.qihoo.finance.lowcode.common.entity.enums.CompletionStatus;
import com.qihoo.finance.lowcode.common.util.ApplicationUtil;
import com.qihoo.finance.lowcode.common.util.NotifyUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class EditorUtil {

    public static void insertTextToSelectedEditor(String text) {
        Project project = ApplicationUtil.findCurrentProject();
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor != null) {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                editor.getDocument().insertString(editor.getCaretModel().getOffset(), text);
                String prompt = editor.getDocument().getText().substring(0, editor.getCaretModel().getOffset());
                ChatUtil.saveCodeCompletionLog(editor, prompt, CompletionType.ASK_AI, CompletionStatus.ACCEPT, text);
            });

        } else {
            NotifyUtils.notify("无法找到选中的Editor", NotificationType.WARNING);
        }
    }

    public static void replaceTextToSelectedEditor(String text, CompletionType completionType) {
        Project project = ApplicationUtil.findCurrentProject();
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor != null) {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                int start = editor.getSelectionModel().getSelectionStart();
                int end = editor.getSelectionModel().getSelectionEnd();
                String prompt = editor.getDocument().getText().substring(0, start);
                if (end > start) {
                    prompt = editor.getDocument().getText().substring(start, end);
                }
                editor.getDocument().replaceString(start, end, text);
                PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
                CodeStyleManager.getInstance(project).reformatText(psiFile, start, start + text.length());
                ChatUtil.saveCodeCompletionLog(editor, prompt, completionType, CompletionStatus.ACCEPT, text);
            });

        } else {
            NotifyUtils.notify("无法找到选中的Editor", NotificationType.WARNING);
        }
    }

    public static Editor getSelectedEditor(Project project) {
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        if (editorManager == null) {
            return null;
        }
        if (editorManager instanceof FileEditorManagerImpl) {
            return ((FileEditorManagerImpl) editorManager).getSelectedTextEditor(true);
        }
        FileEditor current = editorManager.getSelectedEditor();
        if (current instanceof TextEditor) {
            return ((TextEditor) current).getEditor();
        }
        return null;
    }

    public static PsiFile getPsiFile(@NotNull Editor editor) {
        return PsiDocumentManager.getInstance(Objects.requireNonNull(editor.getProject()))
                .getPsiFile(editor.getDocument());
    }

    public static void showDiff(Editor editor, String content) {
        VirtualFile source = EditorUtil.getPsiFile(Objects.requireNonNull(editor)).getVirtualFile();
        showDiff(editor.getProject(), source, content);
    }

    public static void showDiff(Project project, VirtualFile source, String contentToCompare) {
        // 创建左侧编辑器的DiffContent
        DiffContent leftContent = DiffContentFactory.getInstance().create(project, source);
        // 创建右侧提供的文本的DiffContent
        FileType javaFileType = FileTypeManager.getInstance().getFileTypeByExtension("java");
        DiffContent rightContent = DiffContentFactory.getInstance().create(project, contentToCompare, javaFileType);
        // 打开diff窗口
        SimpleDiffRequest diffRequest = new SimpleDiffRequest("Jarvis Diff视图", leftContent, rightContent
                , source.getName() + " (源文件)", source.getName() + " (Jarvis 代码建议)");
        DiffManager.getInstance().showDiff(project, diffRequest);
    }

    public static String formatContent(Project project, FileType fileType, String content, int start, int end) {
        String fileName = "fakeFile." + fileType.getDefaultExtension();
        PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText(fileName, fileType, content);
        CodeStyleManager.getInstance(project).reformatText(psiFile, start, end);
        return psiFile.getText();
    }
}
