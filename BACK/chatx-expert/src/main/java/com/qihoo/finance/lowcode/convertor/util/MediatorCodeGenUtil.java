package com.qihoo.finance.lowcode.convertor.util;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiImportList;
import com.intellij.psi.PsiImportStatement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.qihoo.finance.lowcode.common.utils.VelocityUtils;
import com.qihoo.finance.lowcode.convertor.dto.MethodDTO;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * {@link com.intellij.psi.PsiClass}
 */
public class MediatorCodeGenUtil {

    private final static Map<String, String> checkResultMethodMap = new HashMap<>();
    private final static Map<String, String> throwStatementMap = new HashMap<>();
    private final static Map<String, List<String>> importClassMap = new HashMap<>();

    static {
        String msfResp = "com.qihoo.finance.msf.api.domain.Response";
        checkResultMethodMap.put(msfResp, "checkIfSuccess");
        throwStatementMap.put(msfResp, "throw new BusinessException(response.genResponseService())");
        importClassMap.put(msfResp, Arrays.asList("com.qihoo.finance.msf.api.exception.BusinessException"));
    }

    public static void genMediatorClass(Project project, PsiDirectory baseDirectory, PsiClass interfaceClass
            , String mediatorStr, String methodSignatures, boolean extractDataField) {
        PsiClass mediatorClass = PsiUtil.getPsiClassInProject(project, mediatorStr);
        if (mediatorClass == null) {
            // 创建类
            String module = Optional.ofNullable(PsiUtil.getModule(baseDirectory.getContainingFile()))
                    .map(Module::getName).orElse("lowcode");
            int index = mediatorStr.lastIndexOf(".");
            String packageName = mediatorStr.substring(0, index);
            String className = mediatorStr.substring(index + 1);
            String classDesc = String.format("Mediator (中介者模式) for class {@link %s}", interfaceClass.getQualifiedName());
            String classContent = buildClassContent(module, packageName, className, classDesc, interfaceClass.getQualifiedName());
            String fileName = className + ".java";
            WriteCommandAction.runWriteCommandAction(project, () -> {
                PsiJavaFile psiJavaFile = PsiUtil.createPsiJavaFile(baseDirectory, packageName, className, classContent);
                // 增加Convert方法
                appendMediatorMethod(project, psiJavaFile, interfaceClass, methodSignatures, extractDataField);
            });
        } else {
            appendMediatorMethod(project, (PsiJavaFile) mediatorClass.getContainingFile(), interfaceClass
                    , methodSignatures, extractDataField);
        }
    }

    private static void appendMediatorMethod(Project project, PsiJavaFile psiJavaFile, PsiClass interfaceClass
            , String methodSignatures, boolean extractDataField) {
        PsiElementFactory psiElementFactory = PsiElementFactory.getInstance(project);
        Map<String, Object> paramMap = TemplateUtil.buildBaseParamMapForMethod();
        List<PsiMethod> psiMethods = new ArrayList<>();
        List<MethodDTO> methods = new ArrayList<>();
        paramMap.put("methods", methods);

        Set<String> signatureSet = new HashSet<String>(List.of(methodSignatures.split(";")));
        for (PsiMethod psiMethod : interfaceClass.getMethods()) {
            if (!signatureSet.contains(PsiUtil.getMethodSignature(psiMethod))) {
                continue;
            }
            MethodDTO m = new MethodDTO();
            m.setMethodName(psiMethod.getName());
            m.setParamDefine(ConvertorVelocityUtil.getInstance().getParamDefineStr(psiMethod));
            m.setInterfaceVar(ConvertorVelocityUtil.getInstance().getVariableName(interfaceClass.getName()));
            m.setCallParam(ConvertorVelocityUtil.getInstance().getParamCallStr(psiMethod));
            if ("void".equals(psiMethod.getReturnType().getPresentableText())) {
                m.setMethodRet("void");
                m.setCallRetType(StringUtils.EMPTY);
                m.setReturnStr(StringUtils.EMPTY);
            } else {
                PsiClass retClass = PsiUtil.getPsiClass(psiMethod.getReturnType());
                m.setCallRetType(psiMethod.getReturnType().getPresentableText());
                List<PsiType> genericTypes = PsiUtil.getGenericType(psiMethod.getReturnType());
                m.setReturnStr("return");
                m.setCallRetVar(ConvertorVelocityUtil.getInstance().getVariableName(retClass.getName()));
                PsiType genericType = null;
                boolean hasDataField = false;

                if (extractDataField) {
                    if (!genericTypes.isEmpty()) {
                        genericType = genericTypes.get(0);
                    }
                    PsiMethod[] getDatas = retClass.findMethodsByName("getData", false);
                    if (getDatas.length > 0) {
                        hasDataField = true;
                    }
                }
                if (genericType != null && hasDataField) {
                    m.setExtractDataField(true);
                    if (checkResultMethodMap.containsKey(retClass.getQualifiedName())) {
                        m.setCheckResultMethod(checkResultMethodMap.get(retClass.getQualifiedName()));
                        m.setThrowStatement(throwStatementMap.get(retClass.getQualifiedName()));
                    }
                    m.setMethodRet(genericType.getPresentableText());
                } else {
                    m.setMethodRet(psiMethod.getReturnType().getPresentableText());
                }
            }
            psiMethods.add(psiMethod);
            methods.add(m);
        }
        String templateContent = TemplateUtil.getTemplateContent(TemplateUtil.TEMPLATE_MEDIATOR_METHOD);
        String methodContent = VelocityUtils.generate(templateContent, paramMap);
        PsiMethod psiMethod = psiElementFactory.createMethodFromText(methodContent, psiJavaFile);
        WriteCommandAction.runWriteCommandAction(project, () -> {
            addImport(psiJavaFile, psiMethods, interfaceClass, extractDataField);
            PsiUtil.addMethod(psiJavaFile, psiMethod);
        });
        PsiUtil.navigateInEditor(project, psiJavaFile, psiMethod);
    }

    private static void addImport(PsiJavaFile psiJavaFile, List<PsiMethod> psiMethods, PsiClass interfaceClass
            , boolean extractDataField) {
        PsiElementFactory psiElementFactory = PsiElementFactory.getInstance(psiJavaFile.getProject());
        PsiImportList psiImportList = psiJavaFile.getImportList();
        Set<String> importSet = Optional.ofNullable(psiImportList).map(PsiImportList::getImportStatements)
                .stream().flatMap(Arrays::stream)
                .map(PsiImportStatement::getQualifiedName).collect(Collectors.toSet());
        Set<PsiClass> needImportSet = new HashSet<>();
        // 导入delegate对象的包
        if (!importSet.contains(interfaceClass.getQualifiedName())) {
            PsiClass psiClass = PsiUtil.getPublicClass(psiJavaFile);
            String interfaceVar = StringUtils.uncapitalize(interfaceClass.getName());
            PsiField interfaceField = psiClass.findFieldByName(interfaceVar, true);
            if (interfaceField == null) {
                needImportSet.add(interfaceClass);
                if (!importSet.contains("javax.annotation.Resource")) {
                    PsiType psiType = psiElementFactory.createTypeFromText("javax.annotation.Resource", null);
                    PsiClass resourceClass = PsiUtil.getPsiClass(psiType);
                    needImportSet.add(resourceClass);
                }
                PsiField psiField = psiElementFactory.createFieldFromText(String.format("@Resource private %s %s;"
                        , interfaceClass.getName(), interfaceVar), psiClass);
                psiClass.add(psiField);
            }
        }
        // 导入方法需要的包
        for (PsiMethod psiMethod : psiMethods) {
            if (!"void".equals(psiMethod.getReturnType().getPresentableText())) {
                // 导入解析data field语句额外需要的包
                if (extractDataField) {
                    PsiClass retClass = PsiUtil.getPsiClass(psiMethod.getReturnType());
                    if (importClassMap.containsKey(retClass.getQualifiedName())) {
                        List<PsiClass> importList = importClassMap.get(retClass.getQualifiedName()).stream()
                                .map(x -> PsiUtil.getPsiClass(psiJavaFile.getProject(), x)).collect(Collectors.toList());
                        needImportSet.addAll(importList);
                    }
                }
                // 导入返回类型需要的包
                List<PsiClass> retClasses = PsiUtil.getPsiClassWithGenericType(psiMethod.getReturnType());
                needImportSet.addAll(retClasses);
                List<PsiClass> genericTypes = PsiUtil.getGenericClass(psiMethod.getReturnType());
                needImportSet.addAll(genericTypes);
            }
            List<PsiClass> importClasses = Arrays.stream(psiMethod.getParameterList().getParameters()).map(PsiParameter::getType)
                    .filter(psiType -> !psiType.getCanonicalText().startsWith("java.lang."))
                    .map(PsiUtil::getPsiClassWithGenericType).flatMap(List::stream)
                    .collect(Collectors.toList());
            needImportSet.addAll(importClasses);
        }
        // 执行导入操作
        for (PsiClass needImport : needImportSet) {
            if (!importSet.contains(needImport.getQualifiedName())) {
                psiImportList.add(psiElementFactory.createImportStatement(needImport));
            }
        }
    }

    private static String buildClassContent(String module, String packageName, String className, String classDesc
            , String delegateClass) {
        String batchNo = UUID.randomUUID().toString().replaceAll("-", "");
        Map<String, Object> paramMap = TemplateUtil.buildBaseParamMap(batchNo, module);
        paramMap.put("packageName", packageName);
        paramMap.put("className", className);
        paramMap.put("classDesc", classDesc);
        paramMap.put("delegateClass", delegateClass);
        String templateContent = TemplateUtil.getTemplateContent(TemplateUtil.TEMPLATE_MEDIATOR_CLASS);
        return VelocityUtils.generate(templateContent, paramMap);
    }
}
