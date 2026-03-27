package com.qihoo.finance.lowcode.editor.action;

import com.intellij.ide.IdeEventQueue;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.qihoo.finance.lowcode.aiquestion.util.EditorUtil;
import com.qihoo.finance.lowcode.common.util.ApplicationUtil;
import com.qihoo.finance.lowcode.editor.ChatxEditorManager;
import com.qihoo.finance.lowcode.editor.CompletionRequestType;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.AWTEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.Optional;

public class GenerateCodeByCommentAction extends EditorAction implements DumbAware, ChatxAction {

    private final static String ACTION_ID = "Chatx.GenerateCodeByComment";

    protected GenerateCodeByCommentAction() {
        super(defaultHandler);
    }

    public final static class DoubleEnterClickEventDispatcher implements IdeEventQueue.EventDispatcher {

        private static final long TIME_THRESHOLD = 500; // 设置两次Enter的时间间隔阈值
        private long lastEnterTimestamp = 0;

        @Override
        public boolean dispatch(@NotNull AWTEvent e) {
            if (e instanceof KeyEvent) {
                KeyEvent keyEvent = (KeyEvent) e;
                if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER && keyEvent.getID() == KeyEvent.KEY_PRESSED) {
                    long currentTimestamp = System.currentTimeMillis();
                    if (currentTimestamp - lastEnterTimestamp < TIME_THRESHOLD) {
                        // 两次Enter间隔小于阈值，执行事件逻辑
                        EditorAction action = (EditorAction)ActionManager.getInstance().getAction(ACTION_ID);
                        Project project = ApplicationUtil.findCurrentProject();
                        Boolean isEnabled = Optional.ofNullable(project)
                                .map(EditorUtil::getSelectedEditor)
                                .map(editor -> action.getHandler().isEnabled(editor, editor.getCaretModel().getCurrentCaret(), null))
                                .orElse(Boolean.FALSE);
                        if (isEnabled) {
                            Editor editor = EditorUtil.getSelectedEditor(project);
                            action.getHandler().execute(Objects.requireNonNull(editor), editor.getCaretModel().getCurrentCaret(), null);
                            return true;
                        }
                    }
                    lastEnterTimestamp = currentTimestamp;
                }
            }
            return false;
        }
    }

    private final static class GenerateCodeByCommentHandler extends EditorActionHandler {

        @Override
        protected boolean isEnabledForCaret(@NotNull Editor editor, @NotNull Caret caret, DataContext dataContext) {
            // 光标行为空行，上一行为单行注释时，触发注释生成代码
            ChatxEditorManager editorManager = ChatxEditorManager.getInstance();
            if (!editorManager.isAvailable(editor)) {
                return false;
            }
            Document document = editor.getDocument();
            int lineNumber = document.getLineNumber(caret.getOffset());
            if (lineNumber < 1) {
                return false;
            }
            String lastLine = getLine(document, lineNumber);
            if (StringUtils.isBlank(lastLine)) {
                String prevLine = getLine(document, lineNumber - 1);
                return prevLine.stripLeading().startsWith("//");
            }
            return false;
        }

        @Override
        protected void doExecute(@NotNull Editor editor, @Nullable Caret caret, DataContext dataContext) {
            ChatxEditorManager editorManager = ChatxEditorManager.getInstance();
            editorManager.editorModified(editor, CompletionRequestType.GENERATE_CODE_BY_COMMENT);
        }

        private String getLine(Document document, int lineNumber) {
            int lineStartOffset = document.getLineStartOffset(lineNumber);
            int lineEndOffset = document.getLineEndOffset(lineNumber);
            return document.getText().substring(lineStartOffset, lineEndOffset);
        }
    }

    private static final EditorActionHandler defaultHandler = new GenerateCodeByCommentHandler();
}
