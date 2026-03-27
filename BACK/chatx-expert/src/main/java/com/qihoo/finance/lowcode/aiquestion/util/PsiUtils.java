package com.qihoo.finance.lowcode.aiquestion.util;

import com.intellij.execution.filters.ExceptionWorker;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageUtil;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SuppressWarnings("all")
@Slf4j
public class PsiUtils {
    private static final int MAX_ERR_CODE_CONTENT = 2;
    private static final int MAX_ERR_SEARCH_LINE = 5;
    public static final String IDENTIFIER = "IDENTIFIER";
    public static final String DEFAULT_CLASS_NAME = "DemoClass";
    private static final List<String> commentFlags = Arrays.asList("/", "#", "\"\"\"", "'''");
    private static final TokenSet INVALID_COMMON_TOKENSET;
    private static final String[] FILE_TYPE_CLASSES;

    public PsiUtils() {
    }

    public static String parsePsiType(PsiType psiType) {
        return psiType == null ? "" : psiType.getPresentableText();
    }

    public static boolean isPrivateModifier(PsiModifierListOwner psiModifierListOwner) {
        return psiModifierListOwner != null && psiModifierListOwner.getModifierList() != null ? psiModifierListOwner.getModifierList().hasModifierProperty("private".trim()) : false;
    }

    public static boolean isPublicModifier(PsiModifierListOwner psiModifierListOwner) {
        return psiModifierListOwner != null && psiModifierListOwner.getModifierList() != null ? psiModifierListOwner.getModifierList().hasModifierProperty("public".trim()) : false;
    }

    public static boolean isProtectedModifier(PsiModifierListOwner psiModifierListOwner) {
        return psiModifierListOwner != null && psiModifierListOwner.getModifierList() != null ? psiModifierListOwner.getModifierList().hasModifierProperty("protected".trim()) : false;
    }

    public static PsiMethod[] getMethodsFromPsiClass(PsiClass psiClass, boolean isStatic, String prefix, Predicate<PsiModifierListOwner> visibilityPredicator) {
        if (psiClass == null) {
            return new PsiMethod[0];
        } else {
            if (prefix == null) {
                prefix = "";
            }

            String finalPrefix = prefix;
            PsiMethod[] methods = new PsiMethod[0];

            try {
                methods = (PsiMethod[]) ((List) Arrays.stream(psiClass.getAllMethods()).filter((psiMethod) -> {
                    if (!psiMethod.getName().toLowerCase().startsWith(finalPrefix)) {
                        return false;
                    } else {
                        return !isStatic || psiMethod.hasModifier(JvmModifier.STATIC);
                    }
                }).filter(visibilityPredicator).collect(Collectors.toList())).toArray(new PsiMethod[0]);
            } catch (PsiInvalidElementAccessException var7) {
                log.warn("Get all methods encountered PsiInvalidElementAccessException" + var7.getMessage());
            }

            return methods;
        }
    }

    public static PsiElement skipEmptyAndJavaTokenForward(PsiElement psiElement) {
        if (psiElement == null) {
            return null;
        } else {
            for (PsiElement next = psiElement.getNextSibling(); next != null; next = next.getNextSibling()) {
                boolean notIdentifierToken = next instanceof PsiJavaToken && !"IDENTIFIER".equals(((PsiJavaToken) next).getTokenType().toString());
                boolean isEmpty = next.getText().isEmpty();
                boolean isWhitespace = next instanceof PsiWhiteSpace;
                boolean isComment = next instanceof PsiComment;
                if (!isEmpty && !isWhitespace && !isComment && !notIdentifierToken) {
                    return next;
                }
            }

            return null;
        }
    }

    public static PsiClass getContainingClass(PsiElement psiElement) {
        return (PsiClass) PsiTreeUtil.getParentOfType(psiElement, PsiClass.class);
    }

    public static String getPackageName(PsiClass psiClass) {
        PsiFile psiFile = psiClass.getContainingFile();
        return psiFile instanceof PsiJavaFile ? ((PsiJavaFile) psiFile).getPackageName() : "";
    }

    public static Predicate<PsiModifierListOwner> generateVisibilityPredicator(PsiClass psiClass, PsiClass contextClass) {
        Predicate<PsiModifierListOwner> defaultPublicPredicator = new Predicate<PsiModifierListOwner>() {
            public boolean test(PsiModifierListOwner psiModifierListOwner) {
                return PsiUtils.isPublicModifier(psiModifierListOwner);
            }
        };
        if (psiClass != null && contextClass != null) {
            String psiClassName = psiClass.getQualifiedName();
            String psiClassPackage = getPackageName(psiClass);
            String contextClassName = contextClass.getQualifiedName();
            String contextClassPackage = getPackageName(contextClass);
            final boolean hasEmpty = StringUtils.isEmpty(psiClassPackage) || StringUtils.isEmpty(contextClassPackage) || StringUtils.isEmpty(psiClassName) || StringUtils.isEmpty(contextClassName);
            final boolean isPackageMatch = contextClassPackage.equals(psiClassPackage);
            final boolean isClassNameMatch = contextClassPackage.equals(psiClassName);
            Predicate<PsiModifierListOwner> predicator = new Predicate<PsiModifierListOwner>() {
                public boolean test(PsiModifierListOwner psiModifierListOwner) {
                    if (!hasEmpty && isPackageMatch) {
                        if (isPackageMatch && !isClassNameMatch) {
                            return !PsiUtils.isPrivateModifier(psiModifierListOwner);
                        } else {
                            return true;
                        }
                    } else {
                        return PsiUtils.isPublicModifier(psiModifierListOwner);
                    }
                }
            };
            return predicator;
        } else {
            return defaultPublicPredicator;
        }
    }

    public static boolean checkCaretAround(Editor editor) {
        int offset = editor.getCaretModel().getOffset();
        LogicalPosition logicalPosition = editor.getCaretModel().getLogicalPosition();
        int lineStartOffset = editor.getDocument().getLineStartOffset(logicalPosition.line);
        int lineEndOffset = editor.getDocument().getLineEndOffset(logicalPosition.line);
        int caretOffset = offset - lineStartOffset;
        String lineText = editor.getDocument().getText(new TextRange(lineStartOffset, lineEndOffset));
        if (caretOffset > 0 && caretOffset < lineText.length()) {
            char afterChar = lineText.charAt(caretOffset);
            char beforeChar = lineText.charAt(caretOffset - 1);
            if (isValidCodeTokenChar(afterChar) && isValidCodeTokenChar(beforeChar)) {
                log.info("invalid position in word middle");
                return false;
            }

            if (caretOffset > 1) {
                char moreBeforeChar = lineText.charAt(caretOffset - 2);
                if (isValidCodeTokenChar(afterChar) && (beforeChar == '=' || beforeChar == ' ' && moreBeforeChar == '=')) {
                    log.info("invalid position after = xxxx");
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean isValidCodeTokenChar(char ch) {
        return Character.isJavaIdentifierPart(ch) || ch == '_' || ch == '$';
    }

    public static PsiElement getCaratElement(Editor editor) {
        if (editor.getProject() == null) {
            return null;
        } else {
            CaretModel caretModel = editor.getCaretModel();
            int offset = caretModel.getOffset();
            PsiFile psiFile = PsiDocumentManager.getInstance(editor.getProject()).getPsiFile(editor.getDocument());
            PsiElement psiElement = null;
            if (psiFile != null && offset > 0) {
                psiElement = findElementAtOffset(psiFile, offset);
            }

            return psiElement;
        }
    }

    private static String getFullDocComment(PsiComment psiComment) {
        if (psiComment == null) {
            return "";
        } else {
            PsiDocComment psiDocComment = (PsiDocComment) PsiTreeUtil.getParentOfType(psiComment, PsiDocComment.class, true);
            return ((PsiComment) Objects.requireNonNullElse(psiDocComment, psiComment)).getText();
        }
    }

    public static PsiElement findPrevAtOffset(PsiFile psiFile, int caretOffset, Class... toSkip) {
        if (caretOffset < 0) {
            return null;
        } else {
            int lineStartOffset = 0;
            Document document = PsiDocumentManager.getInstance(psiFile.getProject()).getDocument(psiFile);
            if (document != null) {
                int lineNumber = document.getLineNumber(caretOffset);
                lineStartOffset = document.getLineStartOffset(lineNumber);
            }

            PsiElement element;
            do {
                --caretOffset;
                element = psiFile.findElementAt(caretOffset);
            } while (caretOffset >= lineStartOffset && (element == null || instanceOf(element, (Class[]) toSkip)));

            return instanceOf(element, (Class[]) toSkip) ? null : element;
        }
    }

    public static PsiElement findNonWhitespaceAtOffset(PsiFile psiFile, int caretOffset) {
        PsiElement element = findNextAtOffset(psiFile, caretOffset, PsiWhiteSpace.class);
        if (element == null) {
            element = findPrevAtOffset(psiFile, caretOffset - 1, PsiWhiteSpace.class);
        }

        return element;
    }

    public static PsiElement findElementAtOffset(PsiFile psiFile, int caretOffset) {
        PsiElement element = findPrevAtOffset(psiFile, caretOffset);
        if (element == null) {
            element = findNextAtOffset(psiFile, caretOffset);
        }

        return element;
    }

    public static PsiElement findNextAtOffset(PsiFile psiFile, int caretOffset, Class... toSkip) {
        PsiElement element = psiFile.findElementAt(caretOffset);
        if (element == null) {
            return null;
        } else {
            Document document = PsiDocumentManager.getInstance(psiFile.getProject()).getDocument(psiFile);
            int lineEndOffset = 0;
            if (document != null) {
                int lineNumber = document.getLineNumber(caretOffset);
                lineEndOffset = document.getLineEndOffset(lineNumber);
            }

            while (caretOffset < lineEndOffset && instanceOf(element, (Class[]) toSkip)) {
                ++caretOffset;
                element = psiFile.findElementAt(caretOffset);
            }

            return instanceOf(element, (Class[]) toSkip) ? null : element;
        }
    }

    public static boolean instanceOf(Object obj, Class... possibleClasses) {
        if (obj != null && possibleClasses != null) {
            Class[] var2 = possibleClasses;
            int var3 = possibleClasses.length;

            for (int var4 = 0; var4 < var3; ++var4) {
                Class cls = var2[var4];
                if (cls.isInstance(obj)) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    public static String getLanguageByCurrentFile(Editor editor) {
        FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
        VirtualFile virtualFile = fileDocumentManager.getFile(editor.getDocument());
        if (virtualFile == null) {
            return null;
        } else {
            String path = virtualFile.getPath();
            int dotIndex = path.lastIndexOf(".");
            return dotIndex > 0 ? path.substring(dotIndex + 1) : null;
        }
    }

    public static String findErrorLineContent(Project project, Editor editor, int line, int maxLine) {
        try {
            return findErrorLineContentByDefault(project, editor, line, maxLine);
        } catch (Exception e) {
            log.error("findErrorLineContent ", e);
            return StringUtils.EMPTY;
        }
    }

    public static String findErrorLineContentByDefault(Project project, Editor editor, int line, int maxLine) {
        List<String> codeContents = new ArrayList<>();
        String languageStr = null;

        while (line < editor.getDocument().getLineCount()) {
            String lineContent = editor.getDocument().getText(new TextRange(editor.getDocument().getLineStartOffset(line), editor.getDocument().getLineEndOffset(line)));
            ExceptionWorker.ParsedLine myInfo = ExceptionWorker.parseExceptionLine(lineContent);
            if (myInfo != null && myInfo.fileName != null) {
                String fileName = myInfo.fileName;
                int documentLine = myInfo.lineNumber;
                String classFullPath = lineContent.substring(myInfo.classFqnRange.getStartOffset(), myInfo.classFqnRange.getEndOffset());
                List<VirtualFile> vFiles = new ArrayList(FilenameIndex.getVirtualFilesByName(project, fileName, GlobalSearchScope.projectScope(project)));
                if (CollectionUtils.isEmpty(vFiles)) {
                    ++line;
                } else {
                    VirtualFile vFile = findMostRelatedVirtualFile(vFiles, classFullPath);
                    try {
                        Document document = FileDocumentManager.getInstance().getDocument(vFile);
                        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
                        PsiMethod psiMethod = PsiTreeUtil.findElementOfClassAtOffset(psiFile, document.getLineStartOffset(documentLine), PsiMethod.class, false);
                        String content = getPsiMethodContent(document, psiMethod, documentLine, MAX_ERR_SEARCH_LINE);
                        codeContents.add(content);

                        if (codeContents.size() >= MAX_ERR_CODE_CONTENT) break;
                        log.info("Find PsiMethod stacktrace related vfs " + vFile.getName());
                    } catch (Exception e) {
                        String var14;
                        try {
                            String content = new String(vFile.contentsToByteArray(true));
                            Language language = LanguageUtil.getFileLanguage(vFile);
                            languageStr = null;
                            if (language != null) {
                                languageStr = language.getDisplayName().toLowerCase();
                            }

                            codeContents.add(getCodeContent(content, documentLine));
                            if (codeContents.size() >= MAX_ERR_CODE_CONTENT) break;
                        } catch (IOException var18) {
                            log.error("vFile parse exception. ", var18);
                            continue;
                        }
                    } finally {
                        ++line;
                    }

                    if (line < maxLine) {
                        continue;
                    } else {
                        break;
                    }
                }
            } else {
                ++line;
            }
        }

        if (CollectionUtils.isNotEmpty(codeContents)) {
            String start = String.format("```%s\n", StringUtils.defaultString(languageStr));
            String content = String.join("\n\n", codeContents);
            String end = "\n```";

            return start + content + end;
        }

        return StringUtils.EMPTY;
    }

    public static String getPsiMethodSignature(PsiMethod psiMethod) {
        return psiMethod.getText().replace(psiMethod.getBody().getText(), "");
    }

    public static String getPsiMethodContent(Document document, PsiMethod psiMethod, int baseLine, int maxLine) {
        String text = psiMethod.getText();
        String[] split = text.split("\n");
        TextRange textRange = psiMethod.getTextRange();
        if (split.length > maxLine) {
            int startOffset = Math.max(textRange.getStartOffset(), document.getLineStartOffset(baseLine - maxLine));
            int endOffset = Math.min(textRange.getEndOffset(), document.getLineEndOffset(baseLine + maxLine));
            StringBuilder simpleBody = new StringBuilder(document.getText(new TextRange(startOffset, endOffset)));

            if (startOffset > textRange.getStartOffset()) {
                simpleBody.insert(0, getPsiMethodSignature(psiMethod) + " {\n        ...\n");
            }
            if (endOffset < textRange.getEndOffset()) {
                simpleBody.append("\n        ...\n    }");
            }

            return simpleBody.toString();
        }

        return text;
    }

    public static VirtualFile findMostRelatedVirtualFile(List<VirtualFile> virtualFiles, String classFullPath) {
        if (!CollectionUtils.isEmpty(virtualFiles) && classFullPath != null) {
            Iterator var2 = virtualFiles.iterator();

            VirtualFile virtualFile;
            String vFileDotPath;
            do {
                if (!var2.hasNext()) {
                    return (VirtualFile) virtualFiles.get(0);
                }

                virtualFile = (VirtualFile) var2.next();
                String vPath = virtualFile.getPath();
                int extPos = vPath.lastIndexOf(".");
                if (extPos > 0) {
                    vPath = vPath.substring(0, extPos);
                }

                vFileDotPath = vPath.replace("/", ".");
            } while (!vFileDotPath.endsWith(classFullPath));

            return virtualFile;
        } else {
            return null;
        }
    }

    public static @NotNull String getCodeContent(String content, int documentLine) {
        String[] contentLines = content.split("\n");
        return findCompleteCodeBlock(contentLines, documentLine, "{", "}", MAX_ERR_SEARCH_LINE);
    }

    public static String findCompleteCodeBlock(String[] contentLines, int documentLine, String blockStartSymbol, String blockEndSymbol, int maxSearchLine) {
        int i = 0;

        boolean foundStart;
        documentLine = Math.min(contentLines.length, documentLine);
        for (foundStart = false; documentLine - i >= 0 && i < maxSearchLine; ++i) {
            String line = contentLines[documentLine - i];
            if (line.trim().endsWith(blockStartSymbol)) {
                foundStart = true;
                break;
            }
        }

        int j = 0;
        boolean foundEnd = false;
        while (documentLine + j <= contentLines.length - 1 && j < maxSearchLine) {
            String line = contentLines[documentLine + j];
            if (line.trim().endsWith(blockEndSymbol)) {
                foundEnd = true;
                break;
            }

            ++j;
        }

        StringBuilder sb = new StringBuilder();

        for (int k = Math.max(documentLine - i, 0); k <= Math.min(documentLine + j, contentLines.length - 1); ++k) {
            sb.append(contentLines[k]);
            sb.append("\n");
        }

        if (sb.length() > 1) sb.setLength(sb.length() - 1);
        if (!foundStart) sb.insert(0, "    " + blockStartSymbol + "\n");
        if (!foundEnd) sb.append("\n    " + blockEndSymbol);

        return sb.toString();
    }

    static {
        INVALID_COMMON_TOKENSET = TokenSet.create(new IElementType[]{TokenType.BAD_CHARACTER, TokenType.WHITE_SPACE, TokenType.NEW_LINE_INDENT, TokenType.ERROR_ELEMENT});
        FILE_TYPE_CLASSES = new String[]{"com.intellij.ide.highlighter.JavaFileType", "com.jetbrains.python.PythonFileType", "com.goide.GoFileType"};
    }
}

