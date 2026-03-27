package com.qihoo.finance.lowcode.convertor.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiJavaFile;
import com.intellij.ui.components.JBRadioButton;
import com.qihoo.finance.lowcode.convertor.util.MediatorCodeGenUtil;
import com.qihoo.finance.lowcode.convertor.util.PsiUtil;
import com.qihoo.finance.lowcode.convertor.util.SpringUtilities;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import java.awt.FlowLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Arrays;

/**
 * 生成Convert类弹窗
 */
public class GenMediatorDiaglog extends DialogWrapper {

    private final Project project;
    private final PsiJavaFile psiJavaFile;
    private JPanel dialogPanel;

    public GenMediatorDiaglog(@NotNull Project project, PsiJavaFile psiJavaFile) {
        super(project);
        this.project = project;
        this.psiJavaFile = psiJavaFile;
        setTitle("生成Mediator");
        setModal(false);
        super.init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        try {
            dialogPanel = new JPanel(new SpringLayout());
            String[] labels = {"接口", "生成的Mediator", "方法", "返回值处理逻辑", "目标源路径"};
            for (String label : labels) {
                JLabel jLabel = new JLabel(label, SwingConstants.TRAILING);
                dialogPanel.add(jLabel);
                if (labels[4].equals(label)) {
                    String[] sourceRoots = Arrays.stream(PsiUtil.getSourceRoots(project)).map(VirtualFile::getPath)
                            .filter(x -> x.endsWith("java")).toArray(String[]::new);
                    ComboBox<String> comboBox = new ComboBox<>(sourceRoots);
                    jLabel.setLabelFor(comboBox);
                    dialogPanel.add(comboBox);
                } else if (labels[2].equals(label)) {
                    ComboBox<String> comboBox = new ComboBox<>();
                    jLabel.setLabelFor(comboBox);
                    dialogPanel.add(comboBox);
                } else if (labels[3].equals(label)) {
                    ComboBox<String> comboBox = new ComboBox<>(new String[]{"提取接口返回值中的data字段", "直接返回接口返回值"});
                    JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                    jLabel.setLabelFor(radioPanel);
                    ButtonGroup buttonGroup = new ButtonGroup();
                    JBRadioButton button1 = new JBRadioButton("返回Response的「data」字段", true);
                    button1.setBorder(BorderFactory.createEmptyBorder(0, 0, 0 ,10));
                    JBRadioButton button2 = new JBRadioButton("返回Response");
                    button2.setBorder(BorderFactory.createEmptyBorder(0, 0, 0 ,10));
                    buttonGroup.add(button1);
                    buttonGroup.add(button2);
                    radioPanel.add(button1);
                    radioPanel.add(button2);
                    dialogPanel.add(radioPanel);
                } else {
                    JTextField jTextField = new JTextField(50);
                    jLabel.setLabelFor(jTextField);
                    dialogPanel.add(jTextField);
                }
            }
            SpringUtilities.makeCompactGrid(dialogPanel, labels.length, 2, 10, 10, 10, 10);
            initSourceClass();
        } catch (Exception e) {
            Messages.showMessageDialog(e.getMessage(), "提示", Messages.getInformationIcon());
        }
        return dialogPanel;
    }

    private void initSourceClass() {
        JTextField interfaceTextField = (JTextField) dialogPanel.getComponent(1);
        interfaceTextField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) { }

            @Override
            public void focusLost(FocusEvent e) {
                refreshMethodOption();
            }
        });
        PsiClass interfaceClass = PsiUtil.getInterface(psiJavaFile);
        if (interfaceClass != null) {
            interfaceTextField.setText(interfaceClass.getQualifiedName());
            // 初始化接口方法列表
            refreshMethodOption();
        }
    }

    @SuppressWarnings("unchecked")
    private void refreshMethodOption() {
        JTextField interfaceTextField = (JTextField) dialogPanel.getComponent(1);
        JTextField mediatorTextField = (JTextField) dialogPanel.getComponent(3);
        ComboBox<String> methodComboBox = (ComboBox<String>) dialogPanel.getComponent(5);
        methodComboBox.removeAllItems();
        if (!interfaceTextField.getText().isBlank()) {
            if (mediatorTextField.getText().isBlank()) {
                mediatorTextField.setText(interfaceTextField.getText() + "Mediator");
            }
            PsiClass psiClass = PsiUtil.getPsiClass(project, interfaceTextField.getText());
            if (psiClass != null) {
                if (!psiClass.isInterface()) {
                    throw new RuntimeException("输入的类不是接口");
                }
                Arrays.stream(psiClass.getMethods()).map(PsiUtil::getMethodSignature).forEach(methodComboBox::addItem);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doOKAction() {
        try {
            String interfaceStr = ((JTextField) dialogPanel.getComponent(1)).getText().trim();
            String mediatorStr = ((JTextField) dialogPanel.getComponent(3)).getText().trim();
            String methodSignatures = (String)((ComboBox<String>) dialogPanel.getComponent(5)).getSelectedItem();
            boolean extractDataField = ((JBRadioButton)((JPanel) dialogPanel.getComponent(7)).getComponent(0)).isSelected();
            String basePath = ((String)((ComboBox<String>) dialogPanel.getComponent(9)).getSelectedItem()).trim();
            PsiDirectory baseDirectory = PsiUtil.getPsiDirectory(project, basePath);
            notNull(baseDirectory, "目标源路径不存在");
            notEmpty(interfaceStr, "请输入接口");
            notEmpty(mediatorStr, "请输入生成的Mediator");
            notEmpty(methodSignatures, "请选择方法");
            PsiClass interfaceClass = PsiUtil.getPsiClass(project, interfaceStr);
            notNull(interfaceClass, "接口不存在");
            MediatorCodeGenUtil.genMediatorClass(project, baseDirectory, interfaceClass, mediatorStr, methodSignatures
                    , extractDataField);
            super.doOKAction();
        } catch (Exception e) {
            Messages.showMessageDialog(e.getMessage(), "提示", Messages.getInformationIcon());
        }
    }

    private void notEmpty(String str, String errorMsg) {
        if (StringUtils.isBlank(str)) {
            throw new RuntimeException(errorMsg);
        }
    }

    private void notNull(Object obj, String errorMsg) {
        if (obj == null) {
            throw new RuntimeException(errorMsg);
        }
    }

}
