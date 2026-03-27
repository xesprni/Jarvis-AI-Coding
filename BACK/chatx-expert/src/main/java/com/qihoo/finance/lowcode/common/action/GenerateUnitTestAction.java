package com.qihoo.finance.lowcode.common.action;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.qihoo.finance.lowcode.aiquestion.util.EditorUtil;
import com.qihoo.finance.lowcode.convertor.util.PsiUtil;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class GenerateUnitTestAction extends BaseQuickAskAction {

    @Override
    String getPrompt() {
        return "帮我生成单元测试";
    }

    @Override
    String getSelectedTextEmptyTips() {
        return "请先选中代码再点击生成单元测试";
    }

    public static String preparePrompt(String prompt) {
        Editor editor = EditorUtil.getSelectedEditor(ProjectUtils.getCurrProject());
        if (editor == null) {
            return prompt;
        }
        String selectedText = editor.getSelectionModel().getSelectedText();
        if (selectedText == null || selectedText.isEmpty()) {
            return prompt;
        }
        PsiFile psiFile = EditorUtil.getPsiFile(editor);
        StringBuilder builder = new StringBuilder();
        PsiClass psiClass = PsiTreeUtil.getChildOfType(psiFile, PsiClass.class);
        if (psiClass != null) {
            builder.append("```java\n");
            int offset = editor.getSelectionModel().getSelectionStart();
            Set<String> importSet = appendRelativeText(psiFile, offset, builder);
            if (psiFile instanceof PsiJavaFile) {
                String packageName = ((PsiJavaFile) psiFile).getPackageName();
                builder.append("package ").append(packageName).append(";\n\n");
            }
            builder.append("import ").append(psiClass.getQualifiedName()).append("\n");
            for (String importClass : importSet) {
                builder.append("import ").append(importClass).append(";\n");
            }
            builder.append("public class ").append(psiClass.getName()).append(" {\n\n");
            if (!Character.isWhitespace(selectedText.charAt(0))) {
                builder.append("    ");
            }
            builder.append(selectedText);
            builder.append("\n}\n```");
            prompt = builder.toString();
        }
        return prompt;
    }

    private static Set<String> appendRelativeText(PsiFile psiFile, int offset, StringBuilder builder) {
        HashSet<String> importSet = new HashSet<>();
        PsiMethod psiMethod = PsiTreeUtil.findElementOfClassAtOffset(psiFile, offset, PsiMethod.class, false);
        if (psiMethod == null) {
            return importSet;
        }
        // 方法涉及的类
        Set<PsiClass> referenceSet = new LinkedHashSet<>();
        // 方法返回类型涉及的类
        Optional.ofNullable(psiMethod.getReturnType()).map(com.intellij.psi.util.PsiUtil::resolveClassInType)
                .ifPresent(referenceSet::add);
        // 方法参数涉及的类
        Arrays.stream(psiMethod.getParameterList().getParameters())
                .map(PsiParameter::getType).map(com.intellij.psi.util.PsiUtil::resolveClassInType)
                .filter(Objects::nonNull).forEach(referenceSet::add);
        // 解析类为字符串
        for (PsiClass referenceClass : referenceSet) {
            String classQualifiedName = referenceClass.getQualifiedName();
            if (classQualifiedName == null || PsiUtil.isBuiltinType(classQualifiedName)) {
                continue;
            }
            importSet.add(classQualifiedName);
            String classSimpleText = PsiUtil.getClassSimpleText(referenceClass);
            if (StringUtils.isNotBlank(classSimpleText)) {
                builder.append(classSimpleText);
            }
        }
        return importSet;
    }
}
