package com.qihoo.finance.lowcode.convertor.util;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiImportList;
import com.intellij.psi.PsiImportStatement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.qihoo.finance.lowcode.common.utils.VelocityUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 生成Convert code
 */
public class ConvertCodeGenUtil {

    public static void genConvertClass(Project project, PsiDirectory baseDirectory, PsiClass sourceClass
            , PsiClass targetClass, String utilClassStr) {
        utilClassStr = removeJavaSuffix(utilClassStr);
        PsiJavaFile utilPsiJavaFile = PsiUtil.getProjectJavaFile(project, utilClassStr);
        if (utilPsiJavaFile == null) {
            // 创建Convert工具类
            int index = utilClassStr.lastIndexOf(".");
            String packageNameTmp = "";
            String classNameTmp = "";
            if (index != -1) {
                packageNameTmp = utilClassStr.substring(0, index);
                classNameTmp = utilClassStr.substring(index + 1);
            } else {
                classNameTmp = utilClassStr;
            }
            Module moduleForFile = ModuleUtilCore.findModuleForFile(baseDirectory.getVirtualFile(), project);
            String module = Optional.ofNullable(moduleForFile).map(Module::getName).orElse("lowcode");
            String classDesc = String.format("%s 转换工具类", sourceClass.getName());
            String content = buildClassContent(module, packageNameTmp, classNameTmp, classDesc);
            final String packageName = packageNameTmp;
            final String className = classNameTmp;
            WriteCommandAction.runWriteCommandAction(project, () -> {
                PsiJavaFile psiJavaFile = PsiUtil.createPsiJavaFile(baseDirectory, packageName, className, content);
                // 增加Convert方法
                appendConvertMethod(project, psiJavaFile, sourceClass, targetClass);
            });
        } else {
            // 增加Convert方法
            appendConvertMethod(project, utilPsiJavaFile, sourceClass, targetClass);
        }
    }

    private static void appendConvertMethod(Project project, PsiJavaFile psiJavaFile, PsiClass sourceClass
            , PsiClass targetClass) {
        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
        PsiElementFactory psiElementFactory = javaPsiFacade.getElementFactory();
        // 导入import
        PsiImportList psiImportList = psiJavaFile.getImportList();
        Set<String> importSet = Optional.ofNullable(psiImportList).map(PsiImportList::getImportStatements)
                .stream().flatMap(Arrays::stream)
                .map(PsiImportStatement::getQualifiedName).collect(Collectors.toSet());
        if (!importSet.contains(sourceClass.getQualifiedName())) {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                psiImportList.add(psiElementFactory.createImportStatement(sourceClass));
            });
        }
        if (!importSet.contains(targetClass.getQualifiedName())) {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                psiImportList.add(psiElementFactory.createImportStatement(targetClass));
            });
        }
        // append psiMethod
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("sourceClass", sourceClass.getQualifiedName());
        paramMap.put("targetClass", targetClass.getQualifiedName());
        paramMap.put("setterList", buildSetterList(sourceClass, targetClass));
        paramMap.put("tool", ConvertorVelocityUtil.getInstance());
        String templateContent = TemplateUtil.getTemplateContent(TemplateUtil.TEMPLATE_CONVERT_METHOD);
        String content = VelocityUtils.generate(templateContent, paramMap);
        PsiMethod psiMethod = psiElementFactory.createMethodFromText(content, psiJavaFile);
        WriteCommandAction.runWriteCommandAction(project, () -> {
            PsiUtil.addMethod(psiJavaFile, psiMethod);
        });
        PsiUtil.navigateInEditor(project, psiJavaFile, psiMethod);
    }

    private static List<String> buildSetterList(PsiClass sourceClass, PsiClass targetClass) {
        String sourceVariable = ConvertorVelocityUtil.getInstance().getVariableName(sourceClass.getName());
        String targetVariable = ConvertorVelocityUtil.getInstance().getVariableName(targetClass.getName());
        List<String> setterList = new ArrayList<>();
        List<PsiMethod> setMethodList = Arrays.stream(targetClass.getAllMethods())
                .filter(x -> x.getName().startsWith("set"))
                .collect(Collectors.toList());
        Set<String> getMethodSet = Arrays.stream(sourceClass.getAllMethods())
                .filter(x -> x.getName().startsWith("get"))
                .filter(x -> x.getParameterList().isEmpty())
                .map(PsiMethod::getName)
                .collect(Collectors.toSet());
        for (PsiMethod setMethod: setMethodList) {
            String getMethodName = "get" + setMethod.getName().substring("set".length());
            if (getMethodSet.contains(getMethodName)) {
                setterList.add(targetVariable + "." + setMethod.getName() + "(" + sourceVariable + "." + getMethodName
                        + "());");
            }
        }
        return setterList;
    }

    private static String buildClassContent(String module, String packageName, String className, String classDesc) {
        String batchNo = UUID.randomUUID().toString().replaceAll("-", "");
        Map<String, Object> paramMap = TemplateUtil.buildBaseParamMap(batchNo, module);
        paramMap.put("packageName", packageName);
        paramMap.put("className", className);
        paramMap.put("classDesc", classDesc);
        String templateContent = TemplateUtil.getTemplateContent(TemplateUtil.TEMPLATE_CONVERT_CLASS);
        return VelocityUtils.generate(templateContent, paramMap);
    }


    private static String removeJavaSuffix(String fileName) {
        if (fileName.endsWith(".java")) {
            return fileName.substring(0, fileName.length() - 5);
        }
        return fileName;
    }

}
