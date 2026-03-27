//package com.qihoo.finance.lowcode.editor.codeInsight;
//
//import com.intellij.codeInsight.codeVision.CodeVisionAnchorKind;
//import com.intellij.codeInsight.codeVision.CodeVisionEntry;
//import com.intellij.codeInsight.codeVision.CodeVisionPlaceholderCollector;
//import com.intellij.codeInsight.codeVision.CodeVisionProvider;
//import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering;
//import com.intellij.codeInsight.codeVision.CodeVisionState;
//import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry;
//import com.intellij.codeInsight.codeVision.ui.model.CodeVisionPredefinedActionEntry;
//import com.intellij.codeInsight.hints.InlayHintsUtils;
//import com.intellij.openapi.actionSystem.ActionManager;
//import com.intellij.openapi.actionSystem.AnAction;
//import com.intellij.openapi.actionSystem.DefaultActionGroup;
//import com.intellij.openapi.actionSystem.Separator;
//import com.intellij.openapi.application.ApplicationManager;
//import com.intellij.openapi.editor.Document;
//import com.intellij.openapi.editor.Editor;
//import com.intellij.openapi.editor.ex.DocumentEx;
//import com.intellij.openapi.editor.ex.util.EditorUtil;
//import com.intellij.openapi.project.Project;
//import com.intellij.openapi.ui.popup.JBPopupFactory;
//import com.intellij.openapi.ui.popup.ListPopup;
//import com.intellij.openapi.util.Computable;
//import com.intellij.openapi.util.Key;
//import com.intellij.openapi.util.KeyWithDefaultValue;
//import com.intellij.openapi.util.TextRange;
//import com.intellij.psi.PsiDocumentManager;
//import com.intellij.psi.PsiElement;
//import com.intellij.psi.PsiFile;
//import com.intellij.psi.PsiMethod;
//import com.intellij.psi.PsiModifier;
//import com.intellij.psi.SyntaxTraverser;
//import com.intellij.ui.awt.RelativePoint;
//import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;
//import kotlin.Deprecated;
//import kotlin.Pair;
//import kotlin.Unit;
//import kotlin.jvm.functions.Function2;
//import lombok.RequiredArgsConstructor;
//import org.jetbrains.annotations.Nls;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.awt.event.MouseEvent;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Objects;
//
//public class ChatxCodeVisionProvider implements CodeVisionProvider<Unit> {
//
//    public static final String GROUP_ID = "com.qihoo.finance.lowcode";
//    public static final String ID = "chatx";
//    private static final Key<Long> MODIFICATION_STAMP_KEY = Key.create("chatx.modificationStamp");
//    private static final Key<Integer> MODIFICATION_STAMP_COUNT_KEY = KeyWithDefaultValue.create("chatx.modificationStampCount", 0);
//    private static final int MAX_MODIFICATION_STAMP_COUNT = 4;
//
//    @NotNull
//    @Override
//    public CodeVisionAnchorKind getDefaultAnchor() {
//        return CodeVisionAnchorKind.Top;
//    }
//
//    @NotNull
//    @Override
//    public String getGroupId() {
//        return GROUP_ID;
//    }
//
//    @NotNull
//    @Override
//    public String getId() {
//        return ID;
//    }
//
//    @Nls
//    @NotNull
//    @Override
//    public String getName() {
//        return ChatxApplicationSettings.settings().pluginName;
//    }
//
//    @NotNull
//    @Override
//    public List<CodeVisionRelativeOrdering> getRelativeOrderings() {
//        return List.of(CodeVisionRelativeOrdering.CodeVisionRelativeOrderingFirst.INSTANCE);
//    }
//
//    @NotNull
//    @Override
//    @Deprecated(message = "use getPlaceholderCollector")
//    // 已被废弃，不用实现
//    public List<TextRange> collectPlaceholders(@NotNull Editor editor) {
//        return List.of();
//    }
//
//    @Nullable
//    @Override
//    // 不需要实现，computeCodeVision做了相同的事情
//    public CodeVisionPlaceholderCollector getPlaceholderCollector(@NotNull Editor editor, @Nullable PsiFile psiFile) {
//        return null;
//    }
//
//    @NotNull
//    @Override
//    @Deprecated(message = "Use computeCodeVision instead")
//    // 已被废弃，不用实现
//    public List<Pair<TextRange, CodeVisionEntry>> computeForEditor(@NotNull Editor editor, Unit uiData) {
//        return List.of();
//    }
//
//    @NotNull
//    @Override
//    public CodeVisionState computeCodeVision(@NotNull Editor editor, Unit uiData) {
//        List<PsiMethod> psiMethods = getPsiMethods(editor);
//        List<Pair<TextRange, CodeVisionEntry>> lenses = new ArrayList<>();
//        for (PsiMethod psiMethod : psiMethods) {
//            TextRange range = ApplicationManager.getApplication().runReadAction((Computable<TextRange>) () -> {
//                return InlayHintsUtils.INSTANCE.getTextRangeWithoutLeadingCommentsAndWhitespaces(psiMethod);
//            }) ;
//            ChatxClickHandler handler = new ChatxClickHandler(psiMethod);
//            CodeVisionEntry entry = new ClickableTextCodeVisionEntry(getName(), getId()
//                    , handler, null, getName(), getName(), List.of());
//            lenses.add(new Pair<>(range, entry));
//        }
//        return new CodeVisionState.Ready(lenses);
//    }
//
//    private List<PsiMethod> getPsiMethods(Editor editor) {
//        return ApplicationManager.getApplication().runReadAction((Computable<List<PsiMethod>>) () -> {
//            List<PsiMethod> psiMethods = new ArrayList<>();
//            PsiFile psiFile = PsiDocumentManager.getInstance(Objects.requireNonNull(editor.getProject()))
//                    .getPsiFile(editor.getDocument());
//            if (psiFile == null) {
//                return psiMethods;
//            }
//            SyntaxTraverser<PsiElement> traverser = SyntaxTraverser.psiTraverser(psiFile);
//            for (PsiElement element : traverser) {
//                if (!(element instanceof PsiMethod)) {
//                    continue;
//                }
////                if (!InlayHintsUtils.isFirstInLine(element)) {
////                    continue;
////                }
//                PsiMethod psiMethod = (PsiMethod)element;
//                if (!psiMethod.isWritable() && isAbstractOrInterface(psiMethod)) {
//                    continue;
//                }
//                psiMethods.add(psiMethod);
//            }
//            return psiMethods;
//        });
//    }
//
//    @Override
//    public void handleClick(@NotNull Editor editor, @NotNull TextRange textRange, @NotNull CodeVisionEntry entry) {
//        if (entry instanceof CodeVisionPredefinedActionEntry) {
//            ((CodeVisionPredefinedActionEntry)entry).onClick(editor);
//        }
//    }
//
//    @Override
//    public void handleExtraAction(@NotNull Editor editor, @NotNull TextRange textRange, @NotNull String s) {
//
//    }
//
//    @Override
//    public Unit precomputeOnUiThread(@NotNull Editor editor) {
//        return null;
//    }
//
//    @Override
//    public boolean shouldRecomputeForEditor(@NotNull Editor editor, @Nullable Unit uiData) {
//        return ApplicationManager.getApplication().runReadAction((Computable<Boolean>) () -> {
//            if (editor.isDisposed()) {
//                return false;
//            }
//            Project project = editor.getProject();
//            if (project == null) {
//                return false;
//            }
//            Document document = editor.getDocument();
//            PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
//            if (psiFile == null) {
//                return false;
//            }
//            String languageId = psiFile.getLanguage().getID();
//            if (!"JAVA".equalsIgnoreCase(languageId)) {
//                return false;
//            }
//
//            Long prevStamp = MODIFICATION_STAMP_KEY.get(editor);
//            long nowStamp = getDocumentStamp(editor.getDocument());
//            if (prevStamp == null || prevStamp != nowStamp) {
//                Integer count = MODIFICATION_STAMP_COUNT_KEY.get(editor);
//                if (count + 1 < MAX_MODIFICATION_STAMP_COUNT) {
//                    MODIFICATION_STAMP_COUNT_KEY.set(editor, count + 1);
//                    return true;
//                } else {
//                    MODIFICATION_STAMP_COUNT_KEY.set(editor, 0);
//                    MODIFICATION_STAMP_KEY.set(editor, nowStamp);
//                    return true;
//                }
//            }
//            return false;
//        });
//    }
//
//    private static long getDocumentStamp(@NotNull Document document) {
//        if (document instanceof DocumentEx) {
//            return ((DocumentEx)document).getModificationSequence();
//        }
//        return document.getModificationStamp();
//    }
//
//    @RequiredArgsConstructor
//    static class ChatxClickHandler implements Function2<MouseEvent, Editor, Unit> {
//
//        private final PsiMethod psiMethod;
//
//        public Unit invoke(MouseEvent event, Editor editor) {
////            TextRange range = InlayHintsUtils.INSTANCE.getTextRangeWithoutLeadingCommentsAndWhitespaces(psiMethod);
//            TextRange range = psiMethod.getTextRange();
//            int startOffset = range.getStartOffset();
//            int endOffset = range.getEndOffset();
//            editor.getSelectionModel().setSelection(startOffset, endOffset);
//            AnAction unitTestAction = ActionManager.getInstance().getAction("ChatX.GenerateUnitTest");
//            AnAction explainCodeAction = ActionManager.getInstance().getAction("ChatX.ExplainCode");
//            AnAction optimizeCodeAction = ActionManager.getInstance().getAction("Chatx.optimizeCode");
//            AnAction commentCodeAction = ActionManager.getInstance().getAction("Chatx.commentCode");
//            AnAction genConvertMethodAction = ActionManager.getInstance().getAction("ChatX.GenConvertMethod");
//            List<AnAction> actions = new ArrayList<>();
//            if (!psiMethod.isWritable()) {
//                if (!isAbstractOrInterface(psiMethod)) {
//                    actions.add(explainCodeAction);
//                }
//            } else if (isAbstractOrInterface(psiMethod)) {
//                actions.add(commentCodeAction);
//                actions.add(unitTestAction);
//            } else {
//                actions.add(commentCodeAction);
//                actions.add(optimizeCodeAction);
//                actions.add(unitTestAction);
//                actions.add(explainCodeAction);
//                if (psiMethod.getReturnType() != null && psiMethod.getName().startsWith("convert")
//                        && !psiMethod.getParameterList().isEmpty()) {
//                    actions.add(Separator.create());
//                    actions.add(genConvertMethodAction);
//                }
//            }
//            if (!actions.isEmpty()) {
//                DefaultActionGroup actionGroup = new DefaultActionGroup(actions);
//                ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(null, actionGroup
//                        , EditorUtil.getEditorDataContext(editor), JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, true);
//                popup.show(new RelativePoint(event));
//            }
//            return null;
//        }
//
//
//    }
//
//    private static boolean isAbstractOrInterface(PsiMethod psiMethod) {
//        return psiMethod.hasModifierProperty(PsiModifier.ABSTRACT);
//    }
//}
