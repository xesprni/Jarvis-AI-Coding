package com.qihoo.finance.lowcode.editor.action;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.editor.ChatxEditorManager;
import com.qihoo.finance.lowcode.editor.CompletionRequestType;
import com.qihoo.finance.lowcode.editor.util.ChatxStringUtil;
import org.jetbrains.annotations.NotNull;

public class GenerateCodeByCommentIntentionAction extends PsiElementBaseIntentionAction {
    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        PsiFile psiFile = element.getContainingFile();
        if (psiFile.getVirtualFile() == null) {
            return;
        }
        PsiComment psiComment = null;
        if (element instanceof PsiComment) {
            psiComment = (PsiComment) element;
        } else if (element.getPrevSibling() instanceof PsiComment) {
            psiComment = (PsiComment) element.getPrevSibling();
        }
        if (psiComment == null) {
            return;
        }
        // 获取当前行的缩进，新增一行并缩进
        int offset = editor.getCaretModel().getOffset();
        Document document = editor.getDocument();
        int lineNumber = document.getLineNumber(offset);
        int lineStartOffset = document.getLineStartOffset(lineNumber);
        int lineEndOffset = document.getLineEndOffset(lineNumber);
        String line = document.getText(new TextRange(lineStartOffset, lineEndOffset));
        int tabWidth = EditorUtil.getTabSize(editor);
        int indent = ChatxStringUtil.leadingWhitespaceLength(line, tabWidth);
        String insertString = "\n" + " ".repeat(indent);
        WriteCommandAction.runWriteCommandAction(project, () -> {
            editor.getDocument().insertString(lineEndOffset, insertString);
            editor.getCaretModel().moveToOffset(lineEndOffset + insertString.length());
            ChatxEditorManager.getInstance().editorModified(editor, CompletionRequestType.GENERATE_CODE_BY_COMMENT);;
        });
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        if (editor == null || editor.isViewer()) {
            return false;
        }
        // 单行注释
        if (element instanceof PsiComment || element.getPrevSibling() instanceof PsiComment) {
            return true;
        }
        return false;
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return getText();
    }

    @Override
    public @IntentionName @NotNull String getText() {
        return Constants.Editor.GENERATE_CODE_BY_COMMENT;
    }
}
