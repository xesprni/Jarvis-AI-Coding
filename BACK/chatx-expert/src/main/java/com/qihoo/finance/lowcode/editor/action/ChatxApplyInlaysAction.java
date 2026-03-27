package com.qihoo.finance.lowcode.editor.action;

import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.qihoo.finance.lowcode.editor.ChatxApplyInlayStrategy;
import com.qihoo.finance.lowcode.editor.ChatxEditorManager;
import com.qihoo.finance.lowcode.editor.util.ChatxStringUtil;
import com.qihoo.finance.lowcode.editor.util.EditorUtilCopy;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.KeyEvent;

public class ChatxApplyInlaysAction extends EditorAction implements DumbAware, ChatxAction {
    protected ChatxApplyInlaysAction() {
        super(new ApplyInlaysHandler());
        setInjectedContext(true);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        if (isIgnoredKeyboardEvent(e)) {
            e.getPresentation().setEnabled(false);
            return;
        }
        super.update(e);
    }

    private boolean isIgnoredKeyboardEvent(@NotNull AnActionEvent e) {
        if (!(e.getInputEvent() instanceof KeyEvent)) {
            return false;
        }
        if (((KeyEvent)e.getInputEvent()).getKeyChar() != '\t') {
            return false;
        }
        Project project = e.getProject();
        if (project == null) {
            return false;
        }
        Editor editor = getEditor(e.getDataContext());
        if (editor == null) {
            return false;
        }
        Document document = editor.getDocument();
        int blockIndent = CodeStyle.getIndentOptions(project, document).INDENT_SIZE;
        int caretOffset = editor.getCaretModel().getOffset();
        int line = document.getLineNumber(caretOffset);
        if (isNonEmptyLinePrefix(document, line, caretOffset)) {
            return false;
        }
        int caretOffsetAfterTab = EditorUtilCopy.indentLine(project, editor, line, blockIndent, caretOffset);
        if (caretOffsetAfterTab < caretOffset)
            return false;
        TextRange tabRange = TextRange.create(caretOffset, caretOffsetAfterTab);
        ChatxEditorManager editorManager = ChatxEditorManager.getInstance();
        if (editorManager.countCompletionInlays(editor, tabRange, true, false, false
                , false) > 0) {
            return false;
        }
        int endOfLineInlays = editorManager.countCompletionInlays(editor, tabRange, false, true
                , false, false);
        if (endOfLineInlays > 0) {
            return false;
        }
        int blockInlays = editorManager.countCompletionInlays(editor, tabRange, false, false
                , true, false);
        if (blockInlays > 0) {
            TextRange caretToEndOfLineRange = TextRange.create(caretOffset, document.getLineEndOffset(line));
            return editorManager.countCompletionInlays(editor, caretToEndOfLineRange, true, true
                    , false, true) > 0;
        }
        return true;
    }

    private static boolean isNonEmptyLinePrefix(Document document, int lineNumber, int caretOffset) {
        int lineStartOffset = document.getLineStartOffset(lineNumber);
        if (lineStartOffset == caretOffset) {
            return false;
        }
        String linePrefix = document.getText(TextRange.create(lineStartOffset, caretOffset));
        return !ChatxStringUtil.isSpacesOrTabs(linePrefix, false);
    }

    static boolean isSupported(@NotNull Editor editor) {
        Project project = editor.getProject();
        boolean ideCompletionsSupported = ChatxApplicationSettings.settings().isShowIdeCompletions();
        return project != null && editor.getCaretModel().getCaretCount() == 1
                && (ideCompletionsSupported || LookupManager.getActiveLookup(editor) == null)
                && ChatxEditorManager.getInstance().hasCompletionInlays(editor)
                && TemplateManager.getInstance(project).getActiveTemplate(editor) == null;
    }

    private static class ApplyInlaysHandler extends EditorActionHandler {

        @Override
        protected boolean isEnabledForCaret(@NotNull Editor editor, @NotNull Caret caret, DataContext dataContext) {
            return ChatxApplyInlaysAction.isSupported(editor);
        }

        @Override
        public boolean executeInCommand(@NotNull Editor editor, DataContext dataContext) {
            return false;
        }

        @Override
        protected void doExecute(@NotNull Editor editor, @Nullable Caret caret, DataContext dataContext) {
            ChatxEditorManager.getInstance().applyCompletion(editor, ChatxApplyInlayStrategy.WHOLE);
        }
    }
}
