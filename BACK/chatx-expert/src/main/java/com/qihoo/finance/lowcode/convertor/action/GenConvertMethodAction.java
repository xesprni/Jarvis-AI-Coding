package com.qihoo.finance.lowcode.convertor.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.qihoo.finance.lowcode.aiquestion.util.ChatUtil;
import com.qihoo.finance.lowcode.common.constants.CompletionType;
import com.qihoo.finance.lowcode.common.entity.enums.CompletionStatus;
import com.qihoo.finance.lowcode.common.util.AssertUtil;
import com.qihoo.finance.lowcode.convertor.util.ConvertorVelocityUtil;
import com.qihoo.finance.lowcode.convertor.util.PsiUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GenConvertMethodAction extends AnAction {

    private final static String INDENT = "    ";
    private final static String LINE_SEPARATOR = "\n";

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        try {
            PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
            AssertUtil.notNull(psiFile, "请先打开一个文件");
            Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
            PsiMethod psiMethod = findPsiMethod(editor, psiFile);
            PsiElement psiElement = findPsiElement(editor, psiFile);
            AssertUtil.notNull(psiMethod, "请把光标放在方法上，并执行该操作");
            List<PsiStatement> psiStatements = prepareConvertStatements(psiMethod);
            // 添加代码快
            WriteCommandAction.runWriteCommandAction(e.getProject(), () -> {
                PsiCodeBlock body = psiMethod.getBody();
                if (body != null) {
                    int lineEndOffset = editor.getDocument().getLineEndOffset(editor.getDocument()
                            .getLineNumber(editor.getCaretModel().getOffset()));
                    PsiElement nextLineElement = psiElement;
                    while (nextLineElement != null && nextLineElement.getTextRange().getStartOffset() <= lineEndOffset) {
                        nextLineElement = nextLineElement.getNextSibling();
                    }
                    if (psiElement instanceof PsiWhiteSpace) {
                        psiElement.delete();
                    }else if (psiElement == null) {
                        PsiElement firstBodyElement = psiMethod.getBody().getFirstBodyElement();
                        if (firstBodyElement instanceof PsiWhiteSpace) {
                            firstBodyElement.delete();
                        }
                    }
                    PsiElement lastInsertedElement = psiElement;
                    for (PsiStatement psiStatement : psiStatements) {
                        if (nextLineElement != null) {
                            lastInsertedElement = psiFile.addBefore(psiStatement, nextLineElement);
                        } else {
                            lastInsertedElement = body.add(psiStatement);
                        }
                    }
                    int offset = body.getStatements()[0].getTextRange().getStartOffset();
                    String prompt = editor.getDocument().getText().substring(0, offset);
                    ChatUtil.saveCodeCompletionLog(editor, prompt, CompletionType.CONVERT_METHOD_BLOCK
                            , CompletionStatus.ACCEPT, body.getText());
                    editor.getCaretModel().moveToOffset(lastInsertedElement.getTextRange().getEndOffset());
                }
            });
        } catch (Exception ex) {
            Messages.showMessageDialog(ex.getMessage(), "提示", Messages.getInformationIcon());
        }
    }

    private PsiElement findPsiElement(Editor editor, PsiFile psiFile) {
        PsiElement psiElement = null;
        SelectionModel selectionModel = editor.getSelectionModel();
        if (selectionModel.getSelectionEnd() <= selectionModel.getSelectionStart()) {
            psiElement = psiFile.findElementAt(editor.getCaretModel().getOffset());
        }
        return psiElement;
    }

    private PsiMethod findPsiMethod(Editor editor, PsiFile psiFile) {
        PsiMethod psiMethod = null;
        SelectionModel selectionModel = editor.getSelectionModel();
        if (selectionModel.getSelectionEnd() > selectionModel.getSelectionStart()) {
            psiMethod = PsiTreeUtil.findElementOfClassAtRange(psiFile, selectionModel.getSelectionStart(), selectionModel.getSelectionEnd(), PsiMethod.class);
        } else {
            PsiElement psiElement = psiFile.findElementAt(editor.getCaretModel().getOffset());
            psiMethod = getPsiMethod(psiElement);
        }
        return psiMethod;
    }

    private List<PsiStatement> prepareConvertStatements(PsiMethod psiMethod) {
        // data validation
        PsiType returnType = psiMethod.getReturnType();
        AssertUtil.notNull(returnType, "请检查方法是否返回值");
        PsiClass retClass = PsiUtil.getPsiClass(returnType);
        AssertUtil.notNull(retClass, "请检查方法返回值类型是否正确");
        PsiParameterList parameterList = psiMethod.getParameterList();
        if (parameterList.isEmpty()) {
            throw new RuntimeException("方法参数列表不能为空");
        }
        List<PsiStatement> statements = new ArrayList<>();
        PsiElementFactory psiElementFactory = PsiElementFactory.getInstance(psiMethod.getProject());
        // 参数个数为1时，判读是否为空
        PsiParameter[] parameters = parameterList.getParameters();
        if (parameters.length == 1) {
            String ifStr = "if (" + parameters[0].getName() + " == null) {" + LINE_SEPARATOR +
                    INDENT+ "return null;" + LINE_SEPARATOR +
                    "}" + LINE_SEPARATOR;
            statements.add(psiElementFactory.createStatementFromText(ifStr, psiMethod));
        }
        String retClassSimpleName = ConvertorVelocityUtil.getInstance().getSimpleName(retClass.getName());
        String retVal = ConvertorVelocityUtil.getInstance().getVariableName(retClassSimpleName);
        String newObjStr = retClassSimpleName + " " + retVal + " = new " + retClassSimpleName + "();";
        statements.add(psiElementFactory.createStatementFromText(newObjStr, psiMethod));
        // 获取返回对象的set方法
        List<PsiMethod> settterMethodList = Arrays.stream(retClass.getAllMethods())
                .filter(x -> x.getName().startsWith("set"))
                .collect(Collectors.toList());
        // 获取参数列表的get方法
        Map<String, Map<String, PsiMethod>> getterMethodMap = new HashMap<>();
        for (PsiParameter parameter : parameters) {
            Map<String, PsiMethod> getterMethods =  Optional.of(parameter).map(PsiParameter::getType)
                    .filter(x -> !PsiUtil.isBuiltinType(x))
                    .filter(x -> x instanceof PsiClassType).map(x -> (PsiClassType) x)
                    .map(PsiClassType::resolve).map(PsiClass::getMethods)
                    .stream().flatMap(Arrays::stream)
                    .filter(x -> x.getName().startsWith("get"))
                    .collect(Collectors.toMap(this::getFieldNameFromGetter, Function.identity()));
            getterMethodMap.put(parameter.getName(), getterMethods);
        }
        // 生成set语句
        for (PsiMethod setterMethod : settterMethodList) {
            String fieldName = getFieldNameFromSetter(setterMethod);
            PsiType fieldType = setterMethod.getParameterList().getParameters()[0].getType();
            String getterStatementStr = null;
            for (PsiParameter parameter : parameters) {
                Map<String, PsiMethod> paramGetterMap = getterMethodMap.get(parameter.getName());
                // TODO 判断类型是否匹配
                if (PsiUtil.isBuiltinType(parameter.getType())) {
                    // 基本类型
                    if (parameter.getName().equals(fieldName)) {
                        getterStatementStr = parameter.getName();
                        break;
                    }
                }
                PsiMethod getterMethod = paramGetterMap.get(fieldName);
                if (getterMethod != null) {
                    getterStatementStr = parameter.getName() + "." + getterMethod.getName() + "()";
                    break;
                }
            }
            if (getterStatementStr == null) {
                String statementText = retVal + "." + setterMethod.getName() + "();";
                statements.add(psiElementFactory.createStatementFromText(statementText, psiMethod));
            } else {
                String statementText = retVal + "." + setterMethod.getName() + "(" + getterStatementStr + ");";
                statements.add(psiElementFactory.createStatementFromText(statementText, psiMethod));
            }
        }
        String retStr = "return " + retVal + ";";
        statements.add(psiElementFactory.createStatementFromText(retStr, psiMethod));
        return statements;
    }

    private String getFieldNameFromSetter(PsiMethod setMethod) {
        return StringUtils.uncapitalize(setMethod.getName().substring("set".length()));
    }

    private String getFieldNameFromGetter(PsiMethod setMethod) {
        return StringUtils.uncapitalize(setMethod.getName().substring("get".length()));
    }

    private PsiMethod getPsiMethod(PsiElement psiElement) {
        while(psiElement != null) {
            if (psiElement instanceof PsiMethod) {
                return (PsiMethod)psiElement;
            }
            psiElement = psiElement.getParent();
        }
        return null;
    }

}
