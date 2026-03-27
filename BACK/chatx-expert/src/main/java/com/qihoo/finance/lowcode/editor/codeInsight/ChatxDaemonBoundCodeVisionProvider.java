package com.qihoo.finance.lowcode.editor.codeInsight;

import com.intellij.codeInsight.codeVision.CodeVisionAnchorKind;
import com.intellij.codeInsight.codeVision.CodeVisionEntry;
import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering;
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry;
import com.intellij.codeInsight.codeVision.ui.model.CodeVisionPredefinedActionEntry;
import com.intellij.codeInsight.hints.InlayHintsUtils;
import com.intellij.codeInsight.hints.codeVision.DaemonBoundCodeVisionProvider;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.SyntaxTraverser;
import com.intellij.ui.awt.RelativePoint;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class ChatxDaemonBoundCodeVisionProvider implements DaemonBoundCodeVisionProvider {

    public static final String GROUP_ID = "com.qihoo.finance.lowcode";
    public static final String ID = "chatx";
    public static final Icon icon = Icons.LOGO_ROUND13;
    public static final Key<PsiMethod> METHOD_KEY = Key.create("chatx.daemon.method");

    @NotNull
    @Override
    public CodeVisionAnchorKind getDefaultAnchor() {
        return CodeVisionAnchorKind.Top;
    }

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @NotNull
    @Override
    public String getGroupId() {
        return GROUP_ID;
    }

    @Nls
    @NotNull
    @Override
    public String getName() {
        return ChatxApplicationSettings.settings().pluginName;
    }

    @NotNull
    @Override
    public List<CodeVisionRelativeOrdering> getRelativeOrderings() {
        return List.of(CodeVisionRelativeOrdering.CodeVisionRelativeOrderingFirst.INSTANCE);
    }

    @NotNull
    @Override
    public List<Pair<TextRange, CodeVisionEntry>> computeForEditor(@NotNull Editor editor, @NotNull PsiFile file) {
        List<Pair<TextRange, CodeVisionEntry>> lenses = new ArrayList<>();
        String languageId = file.getLanguage().getID();
        if (!"JAVA".equalsIgnoreCase(languageId)) {
            return lenses;
        }
        SyntaxTraverser<PsiElement> traverser = SyntaxTraverser.psiTraverser(file);
        for (PsiElement element : traverser) {
            if (!(element instanceof PsiMethod)) {
                continue;
            }
            if (!InlayHintsUtils.isFirstInLine(element)) {
                continue;
            }
            String hint = getName();
            TextRange range = InlayHintsUtils.INSTANCE.getTextRangeWithoutLeadingCommentsAndWhitespaces(element);
            lenses.add(new Pair(range, new ClickableTextCodeVisionEntry(hint, getId()
                    , new ChatxClickHandler((PsiMethod) element), icon, hint, "", List.of())));
        }
        return lenses;
    }

    @NotNull
    @Deprecated
    public List<Pair<TextRange, CodeVisionEntry>> computeForEditor(@NotNull Editor editor) {
        return List.of();
    }

    @Override
    public void handleClick(@NotNull Editor editor, @NotNull TextRange textRange, @NotNull CodeVisionEntry entry) {
        if (entry instanceof CodeVisionPredefinedActionEntry) {
            ((CodeVisionPredefinedActionEntry) entry).onClick(editor);
        }
    }

    @RequiredArgsConstructor
    static class ChatxClickHandler implements Function2<MouseEvent, Editor, Unit> {

        private final PsiMethod psiMethod;

        public Unit invoke(MouseEvent event, Editor editor) {
            editor.putUserData(METHOD_KEY, psiMethod);
            TextRange range = InlayHintsUtils.INSTANCE.getTextRangeWithoutLeadingCommentsAndWhitespaces(psiMethod);
            int startOffset = range.getStartOffset();
            int endOffset = range.getEndOffset();
            editor.getSelectionModel().setSelection(startOffset, endOffset);
//            AnAction askAIAction = ActionManager.getInstance().getAction("ChatX.methodAskAI");
            AnAction addToJarvisChatAction = ActionManager.getInstance().getAction("Chatx.editor.addToJarvisChat");
            Icon addIcon = addToJarvisChatAction.getTemplatePresentation().getIcon();
            addToJarvisChatAction.getTemplatePresentation().setIcon(null);
            AnAction unitTestAction = ActionManager.getInstance().getAction("ChatX.GenerateUnitTest");
            AnAction explainCodeAction = ActionManager.getInstance().getAction("ChatX.ExplainCode");
            AnAction optimizeCodeAction = ActionManager.getInstance().getAction("Chatx.optimizeCode");
            AnAction commentCodeAction = ActionManager.getInstance().getAction("Chatx.commentCode");
            AnAction commentNamingAction = ActionManager.getInstance().getAction("Chatx.optimizeNaming");
            DefaultActionGroup actionGroup = new DefaultActionGroup(List.of(Separator.create(), addToJarvisChatAction,
                    unitTestAction, explainCodeAction, optimizeCodeAction, commentCodeAction, commentNamingAction));
            ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(null, actionGroup
                    , EditorUtil.getEditorDataContext(editor), JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, true);
            popup.show(new RelativePoint(event));
            addToJarvisChatAction.getTemplatePresentation().setIcon(addIcon);
            return null;
        }
    }
}
