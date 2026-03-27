package com.qihoo.finance.lowcode.convertor.util;

import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ConvertorVelocityUtil {

    private final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public String getDateTimeStr() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }

    private final static ConvertorVelocityUtil _INSTANCE = new ConvertorVelocityUtil();

    public static ConvertorVelocityUtil getInstance() {
        return _INSTANCE;
    }
    public String getSimpleName(String className) {
        if (StringUtils.isBlank(className)) {
            return className;
        }
        int index = className.lastIndexOf(".");
        if (index != -1) {
            return className.substring(index + 1);
        }
        return className;
    }

    public String getVariableName(String className) {
        String simpleName = getSimpleName(className);
        if (simpleName != null) {
            simpleName = StringUtils.uncapitalize(simpleName);
        }
        return simpleName;
    }

    public String getParamDefineStr(PsiMethod psiMethod) {
        return Arrays.stream(psiMethod.getParameterList().getParameters())
                .map(x -> x.getType().getPresentableText() + " " + x.getName())
                .collect(Collectors.joining(","));
    }

    public String getParamCallStr(PsiMethod psiMethod) {
        return Arrays.stream(psiMethod.getParameterList().getParameters())
                .map(PsiParameter::getName)
                .collect(Collectors.joining(","));
    }

    public String getReturnStr(PsiMethod psiMethod) {
        if (psiMethod.getReturnType() != null && !psiMethod.getReturnType().getPresentableText().equals("void")) {
            return "return";
        }
        return "";
    }
}
