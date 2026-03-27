package com.qihoo.finance.lowcode.console.mysql.execute.action;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import java.awt.event.KeyEvent;
import java.util.Objects;

/**
 * SQLCommentAction
 *
 * @author fengjinfu-jk
 * date 2024/1/30
 * @version 1.0.0
 * @apiNote SQLCommentAction
 */
public class SQLCommentAction extends AnAction implements Disposable, DumbAware {
    private Project project;
    private Editor editor;

    public SQLCommentAction(Project project, Editor editor) {
        this.project = project;
        this.editor = editor;
        if (SystemInfo.isMac) {
            registerCustomShortcutSet(KeyEvent.VK_SLASH, KeyEvent.META_DOWN_MASK, editor.getContentComponent());
        } else {
            registerCustomShortcutSet(KeyEvent.VK_SLASH, KeyEvent.CTRL_DOWN_MASK, editor.getContentComponent());
        }
    }

    @Override
    public void dispose() {
        unregisterCustomShortcutSet(editor.getContentComponent());
    }

    private static final String COMMENT_OPE = "-- ";

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Document document = editor.getDocument();
        SelectionModel selectionModel = editor.getSelectionModel();
        VisualPosition startPosition = selectionModel.getSelectionStartPosition();
        VisualPosition endPosition = selectionModel.getSelectionEndPosition();

        boolean addCommentOpe = false;
        if (Objects.nonNull(startPosition) && Objects.nonNull(endPosition)) {
            for (int i = startPosition.getLine(); i <= endPosition.getLine(); i++) {
                int lineStartOffset = document.getLineStartOffset(i);
                int lineEndOffset = document.getLineEndOffset(i);
                if (lineEndOffset - lineStartOffset < 3) {
                    addCommentOpe = true;
                    break;
                }

                String text = document.getText(new TextRange(lineStartOffset, lineStartOffset + COMMENT_OPE.length()));
                if (!COMMENT_OPE.equals(text)) {
                    addCommentOpe = true;
                    break;
                }
            }
            if (addCommentOpe) {
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    for (int i = startPosition.getLine(); i <= endPosition.getLine(); i++) {
                        document.insertString(document.getLineStartOffset(i), COMMENT_OPE);
                    }
                });
            } else {
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    for (int i = startPosition.getLine(); i <= endPosition.getLine(); i++) {
                        document.deleteString(document.getLineStartOffset(i), document.getLineStartOffset(i) + COMMENT_OPE.length());
                    }
                });
            }
        }
    }
}
