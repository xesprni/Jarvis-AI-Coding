package com.qihoo.finance.lowcode.common.action;

import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.codeInsight.hints.presentation.PresentationRenderer;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.qihoo.finance.lowcode.aiquestion.ui.QuestionPanel;
import com.qihoo.finance.lowcode.aiquestion.ui.component.InputEditorTextField;
import com.qihoo.finance.lowcode.aiquestion.ui.factory.InputPanelFactory;
import com.qihoo.finance.lowcode.aiquestion.ui.search.renderer.SearchPresentationFactory;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.entity.SimpleMethodInfo;
import com.qihoo.finance.lowcode.common.factory.ChatXToolWindowFactory;
import com.qihoo.finance.lowcode.editor.codeInsight.ChatxDaemonBoundCodeVisionProvider;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class MethodAskAIAction extends AnAction {
    public static final Key<SimpleMethodInfo> METHOD_INFO = Key.create("chatx.askai.method.info");

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // 打开工具窗口
        Project project = e.getProject();
        // 获取选中的代码
        Editor sourceEditor = e.getData(CommonDataKeys.EDITOR);
        if (project == null || sourceEditor == null) return;

        PsiMethod psiMethod = sourceEditor.getUserData(ChatxDaemonBoundCodeVisionProvider.METHOD_KEY);
        if (psiMethod == null) return;

        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow(Constants.PLUGIN_TOOL_WINDOW_ID);
        if (toolWindow == null) {
            return;
        }
        toolWindow.show(null);
        // 定位到问答Tab
        QuestionPanel questionPanel = project.getService(QuestionPanel.class);
        ChatXToolWindowFactory.getToolWindow().getContentManager().setSelectedContent(Objects.requireNonNull(toolWindow.getContentManager().getContent(0)));
        InputPanelFactory inputPanel = questionPanel.getInputPanelFactory();
        Editor inputEditor = inputPanel.getInput().getEditor();
        if (Objects.isNull(inputEditor)) return;

        ChatXToolWindowFactory.showFirstTab();
        Document inputDocument = inputEditor.getDocument();
        if (inputDocument.getLineCount() <= 1) {
            WriteCommandAction.writeCommandAction(project).run(() -> {
                inputDocument.insertString(0, " ");
            });
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!inputEditor.getComponent().isFocusOwner()) {
                inputEditor.getContentComponent().requestFocus();
            }
            inputEditor.getCaretModel().moveToOffset(inputEditor.getDocument().getTextLength());
        });

        InputEditorTextField.clearCustomElementRenderer(inputEditor);
        inputEditor.getInlayModel().addInlineElement(-1, createInlayRenderer(sourceEditor, psiMethod));

        // 如果索引就绪, 启用代码仓库问答
        inputPanel.enableGitIfIndexed(e);
        inputEditor.putUserData(METHOD_INFO, SimpleMethodInfo.of(psiClassName(psiMethod), psiMethodName(psiMethod)));
    }

    private String psiClassName(PsiMethod psiMethod) {
        PsiClass psiClass = PsiTreeUtil.getParentOfType(psiMethod, PsiClass.class);
        return Objects.nonNull(psiClass) ? psiClass.getQualifiedName() : StringUtils.EMPTY;
    }

    private String psiMethodName(PsiMethod psiMethod) {
        return psiMethod.getName();
    }

    @SuppressWarnings("all")
    public EditorCustomElementRenderer createInlayRenderer(Editor editor, PsiMethod psiMethod) {
        SearchPresentationFactory factory = new SearchPresentationFactory((EditorImpl) editor);
        InlayPresentation presentation =
                factory.referenceOnHover(factory.roundWithBackground(factory.smallText("#" + psiMethod.getName())), (event, point) -> {
                    Project project = editor.getProject();
                    VirtualFile virtualFile = psiMethod.getContainingFile().getVirtualFile();
                    int line = editor.getDocument().getLineNumber(psiMethod.getTextRange().getStartOffset());
                    // open&select method textRange
                    FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                    fileEditorManager.openTextEditor(new OpenFileDescriptor(project, virtualFile, Math.max(line + 1, 0), 0), true);
                    Editor openEditor = fileEditorManager.getSelectedTextEditor();
                    openEditor.getSelectionModel().setSelection(psiMethod.getTextRange().getStartOffset(), psiMethod.getTextRange().getEndOffset());
                });
        return new PresentationRenderer(presentation);
    }
}
