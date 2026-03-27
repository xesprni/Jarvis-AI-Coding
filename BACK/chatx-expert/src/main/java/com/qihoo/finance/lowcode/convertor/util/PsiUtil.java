package com.qihoo.finance.lowcode.convertor.util;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameterList;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.infos.ClassCandidateInfo;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class PsiUtil {


    public static PsiJavaFile getProjectJavaFile(Project project, String classQualifiedName) {
        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
        PsiClass psiClass = psiFacade.findClass(classQualifiedName, GlobalSearchScope.projectScope(project));
        if (psiClass != null) {
            return (PsiJavaFile) psiClass.getContainingFile();
        }
        return null;
    }

    public static void addMethod(PsiJavaFile psiJavaFile, PsiMethod psiMethod) {
        PsiMethod existsPsiMethod = getPsiMethod(psiJavaFile, psiMethod);
        if (existsPsiMethod != null) {
            navigateInEditor(psiJavaFile.getProject(), psiJavaFile, existsPsiMethod);
            throw new RuntimeException("方法已存在：" + getMethodSignature(psiMethod));
        }
        PsiClass psiClass = getPublicClass(psiJavaFile);
        psiClass.add(psiMethod);
    }

    public static void navigateInEditor(Project project, PsiJavaFile psiJavaFile, PsiMethod psiMethod) {
        PsiMethod existsPsiMethod = getPsiMethod(psiJavaFile, psiMethod);
        if (existsPsiMethod == null) {
            throw new RuntimeException("方法不存在：" + getMethodSignature(psiMethod));
        }
        OpenFileDescriptor descriptor = new OpenFileDescriptor(project, psiJavaFile.getVirtualFile()
                , existsPsiMethod.getTextOffset());
        descriptor.navigate(true);
        locatePsiJavaFileInProjectView(project, psiJavaFile);
    }

    public static void navigateInEditor(Project project, VirtualFile virtualFile) {
        OpenFileDescriptor descriptor = new OpenFileDescriptor(project, virtualFile);
        descriptor.navigate(true);
        locateFileInProjectView(project, virtualFile);
    }

    public static PsiMethod getPsiMethod(PsiJavaFile psiJavaFile, PsiMethod psiMethod) {
        PsiClass psiClass = getPublicClass(psiJavaFile);
        return Arrays.stream(psiClass.getMethods())
                .filter(m -> getMethodSignature(m).equals(getMethodSignature(psiMethod)))
                .findFirst().orElse(null);
    }

    public static String getMethodSignature(PsiMethod psiMethod) {
        return psiMethod.getName() + "(" +
                Arrays.stream(psiMethod.getParameterList().getParameters())
                        .map(x -> x.getType().getPresentableText())
                        .collect(Collectors.joining(",")) + ")";
    }

    public static Module getModule(PsiFile psiFile) {
        return ModuleUtil.findModuleForFile(psiFile);
    }

    public static PsiClass getPublicClass(PsiJavaFile psiJavaFile) {
        PsiClass psiClass = Arrays.stream(psiJavaFile.getClasses())
                .filter(x -> x.getModifierList() != null && x.getModifierList().hasModifierProperty(PsiModifier.PUBLIC))
                .findFirst().orElse(null);
        if (psiClass == null) {
            throw new RuntimeException("该Java File中没有public类定义");
        }
        return psiClass;
    }

    public static PsiClass getInterface(PsiJavaFile psiJavaFile) {
        return Arrays.stream(psiJavaFile.getClasses()).filter(PsiClass::isInterface).findFirst().orElse(null);
    }

    public static PsiJavaFile createPsiJavaFile(PsiDirectory baseDirectory, String packageName, String className, String content) {
        String fileName = className + ".java";
        PsiDirectory psiDirectory = getOrCreatePackage(baseDirectory, packageName);
        PsiFileFactory psiFileFactory = PsiFileFactory.getInstance(psiDirectory.getProject());
        PsiJavaFile psiJavaFile = (PsiJavaFile) psiFileFactory.createFileFromText(fileName, JavaFileType.INSTANCE, content);
        psiDirectory.add(psiJavaFile);
        return (PsiJavaFile) psiDirectory.findFile(fileName);
    }

    public static PsiDirectory getOrCreatePackage(PsiDirectory baseDirectory, String packageName) {
        String[] names = packageName.split("\\.");
        for (String name : names) {
            PsiDirectory subdirectory = baseDirectory.findSubdirectory(name);
            if (subdirectory == null) {
                baseDirectory = baseDirectory.createSubdirectory(name);
            } else {
                baseDirectory = subdirectory;
            }
        }
        return baseDirectory;
    }

    public static VirtualFile getSourceRootVirtualFile(PsiFile psiFile) {
        Project project = psiFile.getProject();
        ProjectRootManager projectRootManager = ProjectRootManager.getInstance(project);
        ProjectFileIndex projectFileIndex = projectRootManager.getFileIndex();
        return projectFileIndex.getSourceRootForFile(psiFile.getVirtualFile());
    }

    public static VirtualFile[] getSourceRoots(Project project) {
        ProjectRootManager projectRootManager = ProjectRootManager.getInstance(project);
        return projectRootManager.getContentSourceRoots();
    }
    public static PsiDirectory getSourceRootDirectory(PsiFile psiFile) {
        Project project = psiFile.getProject();
        VirtualFile sourceRoot = getSourceRootVirtualFile(psiFile);
        PsiManager psiManager = PsiManager.getInstance(project);
        PsiDirectory psiDirectory = null;
        if (sourceRoot != null) {
            psiDirectory = psiManager.findDirectory(sourceRoot);
        }
        if (psiDirectory == null) {
            throw new RuntimeException("找不到源码根目录");
        }
        return psiDirectory;
    }

    public static PsiDirectory getPsiDirectory(Project project, String path) {
        VirtualFileManager virtualFileManager = VirtualFileManager.getInstance();
        VirtualFile virtualFile = virtualFileManager.findFileByNioPath(Path.of(path));
        if (virtualFile == null) {
            return null;
        }
        return PsiManager.getInstance(project).findDirectory(virtualFile);
    }

    public static void locatePsiJavaFileInProjectView(Project project, PsiFile psiFile) {
        ProjectView.getInstance(project).getCurrentProjectViewPane().select(psiFile.getVirtualFile(), psiFile.getVirtualFile(), true);
    }

    public static void locateFileInProjectView(Project project, VirtualFile virtualFile) {
        ProjectView.getInstance(project).getCurrentProjectViewPane().select(virtualFile, virtualFile, true);
    }

    public static PsiClass getPsiClass(Project project, String qualifiedName) {
        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
        return psiFacade.findClass(qualifiedName, GlobalSearchScope.allScope(project));
    }

    public static PsiClass getPsiClass(PsiType psiType) {
        if (psiType instanceof PsiClassType) {
            return ((PsiClassType)psiType).resolve();
        }
        return null;
    }

    public static PsiClass getPsiClass(PsiReferenceExpression psiExpression) {
        PsiType psiType = psiExpression.getType();
        if (psiType != null) {
            return com.intellij.psi.util.PsiUtil.resolveClassInType(psiType);
        }
        return Optional.of(psiExpression).map(e -> e.advancedResolve(true))
                .filter(r -> r instanceof ClassCandidateInfo).map(r -> ((ClassCandidateInfo) r).getElement())
                .orElse(null);
    }

    public static List<PsiClass> getPsiClassWithGenericType(PsiType psiType) {
        if (psiType instanceof PsiClassType) {
            List<PsiClass> psiClasses = new ArrayList<>();
            psiClasses.add(((PsiClassType)psiType).resolve());
            List<PsiType> genericTypes = getGenericType(psiType);
            if (!genericTypes.isEmpty()) {
                for (PsiType genericType : genericTypes) {
                    psiClasses.addAll(getPsiClassWithGenericType(genericType));
                }
            }
            return psiClasses;
        }
        return Collections.emptyList();
    }

    public static List<PsiType> getGenericType(PsiType psiType) {
        if (psiType instanceof PsiClassReferenceType) {
            PsiClassReferenceType psiClassReferenceType = (PsiClassReferenceType)psiType;
            return Arrays.stream(psiClassReferenceType.getParameters())
                    .filter(Objects::nonNull).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
    public static List<PsiClass> getGenericClass(PsiType psiType) {
        return getGenericType(psiType).stream().map(PsiUtil::getPsiClass).collect(Collectors.toList());
    }

    public static PsiClass getPsiClassInProject(Project project, String qualifiedName) {
        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
        return psiFacade.findClass(qualifiedName, GlobalSearchScope.projectScope(project));
    }

    public static PsiElement buildCallStatement(PsiMethod psiMethod) {
        String type = Objects.requireNonNull(psiMethod.getReturnType()).getPresentableText();
        String paramStr = Arrays.stream(psiMethod.getParameterList().getParameters()).map(PsiParameter::getName)
                .collect(Collectors.joining(", "));
        String statementStr = String.format("%s %s = %s(%s);", type, StringUtils.uncapitalize(type), psiMethod.getName()
                , paramStr);
        return PsiElementFactory.getInstance(psiMethod.getProject())
                .createStatementFromText(statementStr, psiMethod);
    }

    public static boolean isBuiltinType(String typeName) {
        return typeName.startsWith("java.") ||
                typeName.equals("boolean") ||
                typeName.equals("byte") ||
                typeName.equals("short") ||
                typeName.equals("int") ||
                typeName.equals("long") ||
                typeName.equals("float") ||
                typeName.equals("double") ||
                typeName.equals("char");
    }

    public static boolean isBuiltinType(PsiType psiType) {
        String typeName = psiType.getCanonicalText();
        return isBuiltinType(typeName);
    }

    /**
     * 获取PsiClass的简单文本：只包含字段和方法签名
     */
    public static String getClassSimpleText(PsiClass psiClass) {
        if (psiClass.getQualifiedName() == null || isBuiltinType(psiClass.getQualifiedName())) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        // append class defined text
        String keyword = psiClass.isEnum() ? "enum" : psiClass.isInterface() ? "interface" : "class";
        builder.append(keyword).append(" ");
        builder.append(psiClass.getName());
        Optional.ofNullable(psiClass.getTypeParameterList()).map(PsiTypeParameterList::getText)
                .filter(StringUtils::isNotBlank).ifPresent(builder::append);
        builder.append(" ");
        Optional.ofNullable(psiClass.getExtendsList()).map(PsiReferenceList::getText).filter(StringUtils::isNotBlank)
                .ifPresent(x -> {builder.append(x).append(" ");});
        Optional.ofNullable(psiClass.getImplementsList()).map(PsiReferenceList::getText).filter(StringUtils::isNotBlank)
                .ifPresent(x -> {builder.append(x).append(" ");});
        builder.append("{\n");
        // append fields
        PsiField[] psiFields = psiClass.getAllFields();
        String fieldStr = Arrays.stream(psiFields)
                .filter(f -> Optional.ofNullable(f.getContainingClass()).map(PsiClass::getQualifiedName)
                        .map(name -> !isBuiltinType(name)).orElse(Boolean.FALSE))
                .map(PsiUtil::getPsiFieldText).filter(StringUtils::isNotBlank)
                .distinct().map(f -> "\t" + f)
                .collect(Collectors.joining("\n"));
        if (StringUtils.isNotBlank(fieldStr)) {
            builder.append(fieldStr).append("\n");
        }
        // append methods
        Set<String> fieldNames = Arrays.stream(psiFields).map(PsiField::getName).collect(Collectors.toSet());
        PsiMethod[] psiMethods = psiClass.getAllMethods();
        String methodStr = Arrays.stream(psiMethods)
                .filter(m -> !m.isConstructor())
                .filter(m -> Optional.ofNullable(m.getContainingClass()).map(PsiClass::getQualifiedName)
                        .map(name -> !isBuiltinType(name)).orElse(Boolean.FALSE))
                .filter(m -> {
                    String methodName = m.getName();
                    if ("equals".equals(methodName) || "hashCode".equals(methodName) || "toString".equals(methodName)) {
                        return false;
                    }
                    if (m.getName().startsWith("get") || m.getName().startsWith("set")) {
                        return !fieldNames.contains(StringUtils.uncapitalize(methodName.substring(3)));
                    }
                    return true;
                }).map(PsiUtil::simplifyMethod).filter(StringUtils::isNotBlank)
                .map(s -> "\t" + s).collect(Collectors.joining("\n"));
        if (StringUtils.isNotBlank(methodStr)) {
            builder.append(methodStr).append("\n");
        }
        builder.append("}").append("\n");
        return builder.toString();
    }

    public static String getPsiFieldText(@NotNull PsiField psiField) {
        if (psiField instanceof PsiEnumConstant) {
            return psiField.getText() + ",";
        }
        if (psiField.getTypeElement() == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        if (psiField.hasModifierProperty(PsiModifier.PUBLIC)) {
            builder.append("public ");
        } else if (psiField.hasModifierProperty(PsiModifier.PROTECTED)) {
            builder.append("protected ");
        } else if (psiField.hasModifierProperty(PsiModifier.PRIVATE)) {
            builder.append("private ");
        }
        if (psiField.hasModifierProperty(PsiModifier.FINAL)) {
            builder.append("final ");
        }
        if (psiField.hasModifierProperty(PsiModifier.STATIC)) {
            builder.append("static ");
        }
        builder.append(psiField.getTypeElement().getText()).append(" ");
        builder.append(psiField.getName()).append(";");
        return builder.toString();
    }

    /**
     * 获取类定义字符串，例如：public class Test {
     */
    public static String getClassDefineStr(PsiClass psiClass) {
        PsiModifierList modifierList = psiClass.getModifierList();
        PsiElement lBrace = psiClass.getLBrace();
        if (modifierList == null || lBrace == null) {
            return null;
        }
        return psiClass.getText().substring(modifierList.getTextRangeInParent().getStartOffset()
                , lBrace.getTextRangeInParent().getEndOffset());
    }

    public static String simplifyMethod(PsiMethod psiMethod) {
        StringBuilder builder = new StringBuilder();
        String text = psiMethod.getText();
        if (text == null) {
            return null;
        }
        int startIndex = psiMethod.getModifierList().getStartOffsetInParent();
        text = text.substring(Math.max(startIndex, 0));
        if (psiMethod.hasModifierProperty(PsiModifier.ABSTRACT) || psiMethod.hasModifierProperty(PsiModifier.NATIVE)) {
            return text + ";";
        } else {
            int index = text.indexOf("{");
            if (index > 0) {
                builder.append(text, 0, index);
                builder.append("{\n\t\t\n\t}");
            } else {
                builder.append(text);
            }
            return builder.toString();
        }
    }

    @SafeVarargs
    public static @Nullable <T extends PsiElement> T getPrevSiblingOfType(@Nullable PsiElement element
            , @NotNull Class<? extends T> @NotNull ... classes) {
        PsiElement run = element;
        while (run != null) {
            if (PsiTreeUtil.instanceOf(run, classes)) {
                return (T)run;
            }
            if (run instanceof PsiFile) break;
            run = run.getPrevSibling();
        }
        return null;
    }
}
