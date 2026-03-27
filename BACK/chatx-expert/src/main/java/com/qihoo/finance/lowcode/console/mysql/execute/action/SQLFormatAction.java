package com.qihoo.finance.lowcode.console.mysql.execute.action;

import com.github.vertical_blank.sqlformatter.SqlFormatter;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.event.KeyEvent;

/**
 * SQLCommentAction
 *
 * @author fengjinfu-jk
 * date 2024/1/30
 * @version 1.0.0
 * @apiNote SQLCommentAction
 */
public class SQLFormatAction extends AnAction implements Disposable, DumbAware {
    private Project project;
    private Editor editor;

    public SQLFormatAction(Project project, Editor editor) {
        this.project = project;
        this.editor = editor;

        if (SystemInfo.isMac) {
            registerCustomShortcutSet(KeyEvent.VK_L, KeyEvent.ALT_MASK + KeyEvent.META_DOWN_MASK, editor.getContentComponent());
        } else {
            registerCustomShortcutSet(KeyEvent.VK_L, KeyEvent.CTRL_MASK + KeyEvent.ALT_MASK, editor.getContentComponent());
        }
    }

    @Override
    public void dispose() {
        unregisterCustomShortcutSet(editor.getContentComponent());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        String text = editor.getDocument().getText();
        if (StringUtils.isBlank(text)) {
            return;
        }

        String format = SqlFormatter.format(text);
        WriteCommandAction.runWriteCommandAction(project, () -> {
            editor.getDocument().setText(format);
        });
    }
}
