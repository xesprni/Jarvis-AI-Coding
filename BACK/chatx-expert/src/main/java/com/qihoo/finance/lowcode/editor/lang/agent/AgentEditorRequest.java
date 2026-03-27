package com.qihoo.finance.lowcode.editor.lang.agent;

import com.intellij.lang.Language;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiBinaryExpression;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiCompiledFile;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiSwitchStatement;
import com.intellij.psi.util.PsiTreeUtil;
import com.qihoo.finance.lowcode.common.constants.CompletionType;
import com.qihoo.finance.lowcode.common.entity.dto.aiquestion.PromptElementRange;
import com.qihoo.finance.lowcode.common.entity.dto.askai.CompletionMode;
import com.qihoo.finance.lowcode.common.entity.enums.PromptElementKind;
import com.qihoo.finance.lowcode.common.exception.AssertException;
import com.qihoo.finance.lowcode.common.util.AssertUtil;
import com.qihoo.finance.lowcode.convertor.util.PsiUtil;
import com.qihoo.finance.lowcode.editor.ChatxEditorUtil;
import com.qihoo.finance.lowcode.editor.ChatxService;
import com.qihoo.finance.lowcode.editor.completions.CompletionUtil;
import com.qihoo.finance.lowcode.editor.lang.LanguageInfoManager;
import com.qihoo.finance.lowcode.editor.request.EditorRequest;
import com.qihoo.finance.lowcode.editor.request.LineInfo;
import com.qihoo.finance.lowcode.editor.request.RequestId;
import com.qihoo.finance.lowcode.editor.util.ChatxStringUtil;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Data
@Slf4j
public class AgentEditorRequest implements EditorRequest, Disposable {

    protected static final Logger LOG = Logger.getInstance(AgentEditorRequest.class);
    private final static int SIMPLE_CLASS_CACHE_SIZE = 1000;
    private final static Map<String, String> simpleClassCache = new LinkedHashMap<String, String>(SIMPLE_CLASS_CACHE_SIZE, 0.6F) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return (size() > SIMPLE_CLASS_CACHE_SIZE);
        }
    };

    private final Project project;
    private final Editor editor;
    private final CompletionType completionType;
    private final boolean useTabIndents;
    private final int tabWidth;
    private final int requestId;
    private final Language fileLanguage;
    private final String documentContent;
    private CompletionMode completionMode;
    private int offset;
    @NotNull
    private final LineInfo lineInfo;
    private final long requestTimestamp = System.currentTimeMillis();
    private final long documentModificationSequence;
    private volatile boolean isCancelled;
    private String lang;
    private String prompt;
    private String suffix;
    private List<PromptElementRange> promptElementRanges;
    private String completionLocation = "method";


    public AgentEditorRequest(Project project, Editor editor, CompletionType completionType, boolean useTabIndents
            , int tabWidth, int requestId, Language fileLanguage, String documentContent, int offset
            , @NotNull LineInfo lineInfo, long documentModificationSequence) {
        this.project = project;
        this.editor = editor;
        this.completionType = completionType;
        this.useTabIndents = useTabIndents;
        this.tabWidth = tabWidth;
        this.requestId = requestId;
        this.fileLanguage = fileLanguage;
        this.documentContent = documentContent;
        this.offset = offset;
        this.lineInfo = lineInfo;
        this.documentModificationSequence = documentModificationSequence;
        this.completionMode = ChatxApplicationSettings.settings().completionMode;
    }

    @Override
    public boolean equalsRequest(@NotNull EditorRequest o) {
        return this.requestId == o.getRequestId();
    }

    @Override
    public Disposable getDisposable() {
        return this;
    }

    @Override
    public void dispose() {
        log.debug("EditorRequest.dispose");
        this.isCancelled = true;
    }

    @Override
    public void cancel() {
        if (this.isCancelled) {
            return;
        }
        this.isCancelled = true;
        Disposer.dispose(this);
    }

    @Nullable
    public static EditorRequest create(@NotNull Editor editor, int offset, @NotNull CompletionType completionType) {
        Project project = editor.getProject();
        if (project == null) {
            return null;
        }
        Document document = editor.getDocument();

        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);
        if (file == null)
            return null;
        boolean useTabs = editor.getSettings().isUseTabCharacter(project);
        int tabWidth = editor.getSettings().getTabSize(project);
        LineInfo lineInfo = LineInfo.create(document, offset, tabWidth);
        return new AgentEditorRequest(project, editor, completionType, useTabs, tabWidth, RequestId.incrementAndGet(),
                LanguageInfoManager.findLanguageMapping(file), document.getText(), offset, lineInfo,
                ChatxEditorUtil.getDocumentModificationStamp(document));
    }

    /**
     * 初始化prompt
     */
    @Override
    public void initPromptFromContext() {
        if (ActionUtil.isDumbMode(project)) {
            return;
        }
        promptElementRanges = new ArrayList<>();
        lang = ChatxService.getInstance().getLanguageFromEditor(editor);
        try {
            // 非java语言直接使用当前文件光标前面的内容作为prompt
            if (!"JAVA".equalsIgnoreCase(lang)) {
                commonInit();
                return;
            }
            PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
            AssertUtil.notNull(psiFile, "psiFile is null");
            PsiMethod psiMethod = PsiTreeUtil.findElementOfClassAtOffset(psiFile, offset, PsiMethod.class, false);
            if (psiMethod == null && completionMode != CompletionMode.ONE_LINE) {
                completionMode = CompletionMode.ONE_STATEMENT;
            }
            // java语言组装prompt和上下文
            PsiClass psiClass = PsiTreeUtil.findElementOfClassAtOffset(psiFile, offset, PsiClass.class, false);
            while (psiClass instanceof PsiAnonymousClass) {
                psiClass = PsiTreeUtil.getParentOfType(psiClass, PsiClass.class);
            }
            AssertUtil.notNull(psiClass, "psiClass is null");
            String beforeCursorText = getBeforeCursorText(psiClass);
            AssertUtil.notNull(beforeCursorText, "beforeCursorText is null");
            String similarFileText = getSimilarFileText(psiFile, psiClass);
            if (PsiTreeUtil.findElementOfClassAtOffset(psiFile, offset, PsiMethod.class, false) == null) {
                completionLocation = "class";
            }
            // 组装参数
            int pos = 0;
            StringBuilder builder = new StringBuilder();
            if (StringUtils.isNotBlank(similarFileText)) {
                builder.append(similarFileText);
                PromptElementRange similarFileRange = PromptElementRange.of(PromptElementKind.SIMILAR_FILE, pos, builder.length());
                promptElementRanges.add(similarFileRange);
                pos = builder.length();
            }
            builder.append(beforeCursorText);
            PromptElementRange beforeCursorRange = PromptElementRange.of(PromptElementKind.BEFORE_CURSOR, pos, builder.length());
            promptElementRanges.add(beforeCursorRange);
            prompt = builder.toString();
        } catch (AssertException e) {
            LOG.debug("can't init prompt from context", e);
            commonInit();
        }
    }

    private void commonInit() {
        prompt = documentContent.substring(0, offset);
        prompt = CompletionUtil.TrimEndSpaceTab(prompt);
        suffix = documentContent.substring(offset);
        PromptElementRange beforeCursorRange = PromptElementRange.of(PromptElementKind.BEFORE_CURSOR, 0
                , prompt.length());
        promptElementRanges.add(beforeCursorRange);
    }

    /**
     * 获取光标前的文本
     */
    private String getBeforeCursorText(PsiClass psiClass) {
        Boolean validPosition = Optional.ofNullable(psiClass.getLBrace()).map(PsiElement::getTextOffset)
                .map(x -> x < offset).orElse(Boolean.FALSE);
        AssertUtil.assertTrue(validPosition, "光标位置不正确");
        String classDefineStr = PsiUtil.getClassDefineStr(psiClass);
        AssertUtil.notBlank(classDefineStr, "classDefineStr is blank");
        StringBuilder builder = new StringBuilder();
        builder.append(classDefineStr).append("\n");
        PsiElement[] psiElements = psiClass.getChildren();
        TextRange lastMethodTextRange = null;
        PsiMethod lastMethod = null;
        int suffixMethodInsertPos = -1;
        for (int i = 0; i < psiElements.length; i++) {
            PsiElement psiElement = psiElements[i];
            if (psiElement.getTextOffset() <= psiClass.getLBrace().getTextOffset()) {
                continue;
            }
            if (psiElement.getTextRange().getStartOffset() >= offset) {
                break;
            }
            if (psiElement.getTextRange().getEndOffset() > offset) {
                boolean needAppendSuffixMethod = false;
                if (lastMethodTextRange != null) {
                    // 把光标前最后一个方法的方法体还原
                    suffixMethodInsertPos = lastMethodTextRange.getStartOffset();
                    builder.replace(lastMethodTextRange.getStartOffset(), lastMethodTextRange.getEndOffset()
                            , "\n\t" + lastMethod.getText() +"\n");
                    needAppendSuffixMethod = true;
                } else {
                    PsiMethod psiMethod = PsiTreeUtil.getParentOfType(psiElement, PsiMethod.class, false);
                    if (psiMethod != null) {
                        needAppendSuffixMethod = true;
                    }
                }
                if (needAppendSuffixMethod) {
                    // 添加光标后方法签名
                    ArrayList<PsiMethod> suffixMethods = new ArrayList<>();
                    for (int j = i + 1; j < psiElements.length; j++) {
                        if (psiElements[j] instanceof PsiMethod) {
                            suffixMethods.add((PsiMethod) psiElements[j]);
                        }
                    }
                    for (int j = suffixMethods.size() - 1; j >= 1; j--) {
                        suffixMethodInsertPos = appendElementText(builder, suffixMethods.get(j), suffixMethodInsertPos, true);
                    }
                    if (!suffixMethods.isEmpty()) {
                        suffixMethodInsertPos = appendElementText(builder, suffixMethods.get(0), suffixMethodInsertPos, false);
                    }
                }
                if (StringUtils.isNotBlank(psiElement.getText())) {
                    if (builder.isEmpty()
                            || (builder.charAt(builder.length() - 1) != '\t' && builder.charAt(builder.length() - 1) != ' ')) {
                        builder.append("\t");
                    }
                    builder.append(psiElement.getText(), 0, offset - psiElement.getTextRange().getStartOffset());
                    this.suffix = psiElement.getText().substring(offset - psiElement.getTextRange().getStartOffset());
                    if (StringUtils.isBlank(this.suffix)) {
                        this.suffix = "\n" + lineInfo.getNextLine();
                    }
                } else {
                    if (!psiElement.getText().isEmpty()) {
                        builder.append(psiElement.getText(), 0, offset - psiElement.getTextRange().getStartOffset());
                    }
                    this.suffix = "\n" + lineInfo.getNextLine();
                }
                break;
            }
            int pos = builder.length();
            appendElementText(builder, psiElement, -1, true);

            if (psiElement instanceof PsiMethod) {
                lastMethodTextRange = new TextRange(pos, builder.length());
                lastMethod = (PsiMethod) psiElement;
            } else if (lastMethod == null) {
                suffixMethodInsertPos = builder.length();
            }
        }
        String beforeCursor = builder.toString();
        beforeCursor = CompletionUtil.TrimEndSpaceTab(beforeCursor);
        beforeCursor = CompletionUtil.removeNoNeedNewLine(beforeCursor);
        return beforeCursor;
    }

    /**
     * 获取相似文件的文本
     * @return
     */
    private String getSimilarFileText(PsiFile psiFile, PsiClass psiClass) {
        StringBuilder builder = new StringBuilder();
        PsiMethod psiMethod = PsiTreeUtil.findElementOfClassAtOffset(psiFile, offset, PsiMethod.class, false);
        Set<PsiClass> referenceSet = new LinkedHashSet<>();
        if (psiMethod != null && psiMethod.getBody() != null && psiMethod.getBody().getTextOffset() < offset) {
            // 获取方法内补全的引用类
            PsiElement psiElement = psiFile.findElementAt(offset);
            AssertUtil.notNull(psiElement, "psiElement is null on current offset");
            // 方法返回类型涉及的类
            Optional.ofNullable(psiMethod.getReturnType()).map(com.intellij.psi.util.PsiUtil::resolveClassInType)
                    .ifPresent(referenceSet::add);
            // 方法参数涉及的类
            Arrays.stream(psiMethod.getParameterList().getParameters())
                    .map(PsiParameter::getType).map(com.intellij.psi.util.PsiUtil::resolveClassInType)
                    .filter(Objects::nonNull).forEach(referenceSet::add);
            // 光标最近语句引用的类
            PsiElement relativeElement = PsiUtil.getPrevSiblingOfType(psiElement, PsiExpressionStatement.class
                    , PsiDeclarationStatement.class, PsiReferenceExpression.class, PsiBinaryExpression.class
                    , PsiMethodCallExpression.class);
            if (relativeElement != null) {
                Optional.of(relativeElement)
                        .map(e -> {
                            if (e instanceof PsiReferenceExpression) {
                                return (PsiReferenceExpression) e;
                            }
                            if (e instanceof PsiDeclarationStatement) {
                                Optional.ofNullable(PsiTreeUtil.getChildOfType(e, PsiLocalVariable.class))
                                        .map(PsiLocalVariable::getType)
                                        .map(PsiUtil::getPsiClass)
                                        .ifPresent(referenceSet::add);
                                return null;
                            }
                            if (e instanceof PsiBinaryExpression || e instanceof PsiMethodCallExpression) {
                                PsiMethodCallExpression psiMethodCallExpression = null;
                                if (e instanceof PsiBinaryExpression) {
                                    PsiBinaryExpression psiBinaryExpression = (PsiBinaryExpression) e;
                                    PsiExpression lOperand = psiBinaryExpression.getLOperand();
                                    PsiExpression rOperand = psiBinaryExpression.getROperand();
                                    // 二进制表达式左侧为方法调用，右侧不存在时，只把左侧方法调用的返回值类作为上下文
                                    if (rOperand == null && lOperand instanceof PsiMethodCallExpression) {
                                        psiMethodCallExpression = (PsiMethodCallExpression) lOperand;
                                    }
                                } else {
                                    psiMethodCallExpression = (PsiMethodCallExpression) e;
                                }
                                Optional.ofNullable(psiMethodCallExpression)
                                        .map(PsiMethodCallExpression::resolveMethod)
                                        .map(PsiMethod::getReturnType)
                                        .map(com.intellij.psi.util.PsiUtil::resolveClassInType)
                                        .filter(PsiClass::isEnum)
                                        .ifPresent(referenceSet::add);
                            }
                            return PsiTreeUtil.findChildrenOfType(e, PsiReferenceExpression.class).stream()
                                    .reduce((first, second) -> second).orElse(null);
                        })
                        .map(PsiUtil::getPsiClass)
                        .ifPresent(referenceSet::add);
            } else {
                // 方法调用内部获取当前类的信息&方法参数中的枚举类
                PsiMethodCallExpression psiMethodCallExpression = PsiTreeUtil.getParentOfType(psiElement, PsiMethodCallExpression.class);
                Optional.ofNullable(psiMethodCallExpression).map(PsiMethodCallExpression::resolveMethod)
                        .map(m -> {
                            Optional.of(m).map(PsiMethod::getParameterList)
                                    .map(PsiParameterList::getParameters)
                                    .stream().flatMap(Arrays::stream)
                                    .map(PsiParameter::getType)
                                    .map(com.intellij.psi.util.PsiUtil::resolveClassInType)
                                    .filter(Objects::nonNull).filter(PsiClass::isEnum)
                                    .forEach(referenceSet::add);
                            return m.getContainingClass();
                        })
                        .ifPresent(referenceSet::add);
                // 获取switch语句中的枚举类
                PsiSwitchStatement psiSwitchStatement = PsiTreeUtil.getParentOfType(psiElement, PsiSwitchStatement.class);
                Optional.ofNullable(psiSwitchStatement)
                        .map(PsiSwitchStatement::getExpression)
                        .map(PsiExpression::getType)
                        .map(com.intellij.psi.util.PsiUtil::resolveClassInType)
                        .filter(PsiClass::isEnum)
                        .ifPresent(referenceSet::add);
            }
        } else {
            // 获取类内补全的引用类
            PsiClassType[] extendsListTypes = psiClass.getExtendsListTypes();
            Arrays.stream(extendsListTypes).map(PsiClassType::resolve).filter(Objects::nonNull).forEach(referenceSet::add);
            PsiClassType[] implementsListTypes = psiClass.getImplementsListTypes();
            Arrays.stream(implementsListTypes).map(PsiClassType::resolve).filter(Objects::nonNull).forEach(referenceSet::add);
        }
        // 解析类为字符串
        for (PsiClass referenceClass : referenceSet) {
            String classQualifiedName = referenceClass.getQualifiedName();
            if (classQualifiedName == null || PsiUtil.isBuiltinType(classQualifiedName)) {
                continue;
            }
            String classSimpleText;
            if (simpleClassCache.containsKey(classQualifiedName)) {
                classSimpleText = simpleClassCache.get(classQualifiedName);
            } else {
                classSimpleText = PsiUtil.getClassSimpleText(referenceClass);
                if (referenceClass.getContainingFile() instanceof PsiCompiledFile) {
                    simpleClassCache.put(classQualifiedName, classSimpleText);
                }
            }
            if (StringUtils.isNotBlank(classSimpleText)) {
                builder.append(classSimpleText);
            }
        }
        return builder.toString();
    }

    private int appendElementText(StringBuilder builder, PsiElement psiElement, int index, boolean simplifyMethod) {
        if (index < 0) {
            index = builder.length();
        }
        if (psiElement instanceof PsiMethod) {
            if (!ChatxStringUtil.endsWithNewLine(builder.substring(0, index))) {
                builder.insert(index, "\n");
                ++index;
            }
            String methodStr = psiElement.getText();
            if (simplifyMethod) {
                methodStr = PsiUtil.simplifyMethod((PsiMethod) psiElement);
            }
            if (StringUtils.isNotBlank(methodStr)) {
                if (index == 0 || (builder.charAt(index - 1) != '\t' && builder.charAt(index - 1) != ' ')) {
                    builder.insert(index, "\t");
                    index++;
                }
                builder.insert(index, methodStr);
                index += methodStr.length();
                builder.insert(index, "\n");
                index++;
            }
        } else {
            builder.insert(index, psiElement.getText());
            index += psiElement.getText().length();
        }
        return index;
    }

}
