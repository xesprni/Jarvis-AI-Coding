package com.qihoo.finance.lowcode.editor;

import com.intellij.ide.actions.CutAction;
import com.intellij.ide.actions.UndoAction;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.AnActionResult;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 撤销或者剪贴动作发生的时候，强制刷新补全
 */
public class ChatxEditorActionTracker {

    private final static Logger LOG = Logger.getInstance(ChatxEditorActionTracker.class);

    @NotNull
    public static ChatxEditorActionTracker getInstance() {
        return ApplicationManager.getApplication().getService(ChatxEditorActionTracker.class);
    }

    private final AtomicInteger executingForcedCompletionAction = new AtomicInteger(0);

    public boolean isExecutingForcedCompletionAction() {
        return (this.executingForcedCompletionAction.get() > 0);
    }

    private void enterForcedCompletionAction() {
        this.executingForcedCompletionAction.incrementAndGet();
    }

    private void exitForcedCompletionAction() {
        int newValue = this.executingForcedCompletionAction.decrementAndGet();
        assert newValue >= 0;
    }

    private static boolean isForcedCompletionAction(@NotNull AnAction action) {
        return (action instanceof CutAction || action instanceof UndoAction);
    }

    public static final class ActionListener implements AnActionListener {
        private final AtomicReference<Editor> currentActionEditor = new AtomicReference<>(null);

        @Override
        public void beforeActionPerformed(@NotNull AnAction action, @NotNull AnActionEvent event) {
            Editor editor = CommonDataKeys.EDITOR.getData(event.getDataContext());
            this.currentActionEditor.set((editor != null && ChatxEditorManager.getInstance().isAvailable(editor)) ? editor : null);
            if (ChatxEditorActionTracker.isForcedCompletionAction(action)) {
                ChatxEditorActionTracker.getInstance().enterForcedCompletionAction();
            }
        }

        @Override
        public void afterActionPerformed(@NotNull AnAction action, @NotNull AnActionEvent event
                , @NotNull AnActionResult result) {
//            if (event.getInputEvent() instanceof KeyEvent) {
//                KeyEvent keyEvent = (KeyEvent) event.getInputEvent();
//                if (keyEvent != null && keyEvent.getKeyCode() == KeyEvent.VK_ESCAPE) {
//                    // IDE有的AnAction优先级高于自定义的，导致无法触发自定义的AnAction，这里手动触发
//                    AnAction disposeInlayAction = ActionManager.getInstance().getAction("Chatx.disposeInlays");
//                    if (action != disposeInlayAction) {
//                        disposeInlayAction.actionPerformed(event);
//                    }
//                }
//            }
            if (ChatxEditorActionTracker.isForcedCompletionAction(action)) {
                ChatxEditorActionTracker.getInstance().exitForcedCompletionAction();
                if (!ChatxEditorActionTracker.getInstance().isExecutingForcedCompletionAction()) {
                    Editor editor = this.currentActionEditor.get();
                    if (editor != null && !editor.isDisposed() && ChatxEditorUtil.isSelectedEditor(editor)) {
                        Project project = editor.getProject();
                        if (project != null && !project.isDisposed()) {
                            ChatxEditorManager editorManager = ChatxEditorManager.getInstance();
                            if (editorManager.isAvailable(editor)) {
                                LOG.debug("Forcing editor completion update");
//                                editorManager.editorModified(editor, CompletionRequestType.Forced);
                                // CUT, UNDO等操作不补全
                                editorManager.disposeInlays(editor, InlayDisposeContext.CaretChange);
                            }
                        }
                    }
                }
            }
        }
    }
}
