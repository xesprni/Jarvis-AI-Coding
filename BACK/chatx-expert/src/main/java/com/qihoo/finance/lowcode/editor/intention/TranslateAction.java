package com.qihoo.finance.lowcode.editor.intention;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.BackgroundTaskQueue;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiNamedElement;
import com.intellij.refactoring.actions.BaseRefactoringAction;
import com.intellij.util.ui.UIUtil;
import com.qihoo.finance.lowcode.aiquestion.util.ChatUtil;
import com.qihoo.finance.lowcode.common.entity.dto.askai.ChatCompletionOpenAIRequest;
import com.qihoo.finance.lowcode.common.entity.dto.askai.ChatCompletionOpenAIResponse;
import com.qihoo.finance.lowcode.gentracker.tool.GlobalTool;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * TranslateAction
 *
 * @author fengjinfu-jk
 * date 2024/7/23
 * @version 1.0.0
 * @apiNote TranslateAction
 */
public class TranslateAction extends AnAction {
//    private static final Map<JComponent, TranslateAction> translateActions = new HashMap<>();

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || project.isDisposed()) return;
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (Objects.isNull(editor)) return;

        PsiElement[] psiElementArray = BaseRefactoringAction.getPsiElementArray(e.getDataContext());
        showLookup(editor, psiElementArray.length > 0 ? psiElementArray[0] : null);
    }

    public static void showLookup(Editor editor, PsiElement psiElement) {
        // 在方法开始时获取 ModalityState，避免在后台线程中调用 ModalityState.current()
        ModalityState modalityState = ModalityState.defaultModalityState();
        
        String input = editor.getSelectionModel().getSelectedText();
        // 尝试选中psiElement
        if (StringUtils.isBlank(input) && Objects.nonNull(psiElement)) {
            if (psiElement instanceof PsiNamedElement namedElement) {
                input = namedElement.getName();
            } else if (psiElement instanceof PsiIdentifier identifier) {
                input = identifier.getText();
            }
            setSelection(editor);
        }

        // 尝试选中文本
        if (StringUtils.isBlank(input)) {
            setSelection(editor);
            input = editor.getSelectionModel().getSelectedText();
        }

        if (StringUtils.isBlank(input)) return;

        // do translate
        String inputContent = input;
        Project project = ProjectUtils.getCurrProject();
        BackgroundTaskQueue backgroundTaskQueue = new BackgroundTaskQueue(project, "Jarvis正在翻译中...");
        backgroundTaskQueue.run(new Task.Backgroundable(project, "Jarvis正在翻译中...", true) {
            @Override
            public void run(@org.jetbrains.annotations.NotNull ProgressIndicator indicator) {
                ChatCompletionOpenAIRequest request = buildChatCompletionRequest(inputContent);
                ChatCompletionOpenAIResponse response = ChatUtil.chatCompletion(request);
                String content = response.getContent();
                if (StringUtils.isBlank(content)) {
                    return;
                }
                String chinese = "";
                String underScore = null;
                String camelCase = null;
                String[] translateContents = content.split(",");
                for (String translateContent : translateContents) {
                    if (GlobalTool.matchChinese(translateContent)) {
                        chinese = translateContent;
                    } else if (translateContent.contains("_")) {
                        underScore = translateContent;
                    } else {
                        camelCase = translateContent;
                    }
                }
                underScore = StringUtils.defaultIfBlank(underScore, camelCase);
                if (StringUtils.isBlank(underScore) || StringUtils.isBlank(camelCase)) {
                    return;
                }

                // show lookup
                LookupElement[] items = {
                        LookupElementBuilder.create(StringUtils.uncapitalize(camelCase)),
                        LookupElementBuilder.create(StringUtils.capitalize(camelCase)),
                        LookupElementBuilder.create(StringUtils.lowerCase(underScore)),
                        LookupElementBuilder.create(StringUtils.upperCase(underScore)),
                        LookupElementBuilder.create(chinese),
                };
                UIUtil.invokeLaterIfNeeded(() -> {
                    LookupManager lookupManager = LookupManager.getInstance(ProjectUtils.getCurrProject());
                    lookupManager.showLookup(editor, items);
                });
            }
        }, modalityState, null);
    }

    private static void setSelection(Editor editor) {
        if (StringUtils.isBlank(editor.getSelectionModel().getSelectedText())) {
            Caret currentCaret = editor.getCaretModel().getCurrentCaret();
            int start = getValidStartCharIndex(editor, currentCaret.getSelectionEnd());
            int end = getValidEndCharIndex(editor, currentCaret.getSelectionStart());

            if (end - start > 0) {
                editor.getSelectionModel().setSelection(start, end);
            }
        }
    }

    private static int getValidStartCharIndex(Editor editor, int charIndex) {
        if (charIndex == 0) return charIndex;

        String text = editor.getDocument().getText(new TextRange(charIndex - 1, charIndex));
        char startChar = text.charAt(text.length() - 1);
        if (Character.isLetterOrDigit(startChar) || startChar == '_' || GlobalTool.matchChinese(startChar)) {
            return getValidStartCharIndex(editor, charIndex - 1);
        }

        return charIndex;
    }

    private static int getValidEndCharIndex(Editor editor, int charIndex) {
        if (charIndex == editor.getDocument().getTextLength()) return charIndex;

        String text = editor.getDocument().getText(new TextRange(charIndex, charIndex + 1));
        char endChar = text.charAt(0);
        if (Character.isLetterOrDigit(endChar) || endChar == '_' || GlobalTool.matchChinese(endChar)) {
            return getValidEndCharIndex(editor, charIndex + 1);
        }

        return charIndex;
    }

    @NotNull
    private static ChatCompletionOpenAIRequest buildChatCompletionRequest(String selectedText) {
        ChatCompletionOpenAIRequest request = new ChatCompletionOpenAIRequest();
        request.setStream(false);
        request.setModel(ChatxApplicationSettings.settings().translateModel);
        request.setPrompt(ChatxApplicationSettings.settings().translatePrompt);
        request.setContent(selectedText);

        return request;
    }

//    public static void registerShortcut(JComponent component) {
//        if (!translateActions.containsKey(component)) {
//            TranslateAction translateAction = new TranslateAction();
//            if (SystemInfo.isMac) {
//                translateAction.registerCustomShortcutSet(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK + KeyEvent.META_DOWN_MASK, component);
//            } else {
//                translateAction.registerCustomShortcutSet(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK + KeyEvent.SHIFT_DOWN_MASK, component);
//            }
//            translateActions.put(component, translateAction);
//        }
//    }
//
//    public static void unregisterShortcut(JComponent component) {
//        if (translateActions.containsKey(component)) {
//            translateActions.get(component).unregisterCustomShortcutSet(component);
//            translateActions.remove(component);
//        }
//    }
}
