package com.qihoo.finance.lowcode.convertor.dialog;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.qihoo.finance.lowcode.convertor.util.ConvertCodeGenUtil;
import com.qihoo.finance.lowcode.convertor.util.PsiUtil;
import com.qihoo.finance.lowcode.convertor.util.SpringUtilities;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.HashSet;
import java.util.Set;

/**
 * 生成Convert类弹窗
 */
public class GenConvertClassDiaglog extends DialogWrapper {

    private final Project project;
    private final PsiJavaFile psiJavaFile;
    private final Editor editor;
    private JPanel dialogPanel;
    private Set<String> comboBoxData = new HashSet<>();

    public GenConvertClassDiaglog(@NotNull Project project, PsiJavaFile psiJavaFile, Editor editor) {
        super(project);
        this.project = project;
        this.psiJavaFile = psiJavaFile;
        this.editor = editor;
        setTitle("生成转换类");
        setModal(false);
        super.init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        dialogPanel = new JPanel(new SpringLayout());
        String[] labels = {"源类(from)：", "目标类(to)：", "生成的转换工具类：", "工具类源路径"};
        for (String label : labels) {
            JLabel jLabel = new JLabel(label, SwingConstants.TRAILING);
            dialogPanel.add(jLabel);
            if ("工具类源路径".equals(label)) {
                ComboBox<String> comboBox = new ComboBox<>();
                jLabel.setLabelFor(comboBox);
                dialogPanel.add(comboBox);
            } else {
                JTextField jTextField = new JTextField(50);
                jLabel.setLabelFor(jTextField);
                dialogPanel.add(jTextField);
            }
        }
        SpringUtilities.makeCompactGrid(dialogPanel, labels.length, 2, 10, 10, 10, 10);
        initSourceClass();
        return dialogPanel;
    }

    private void initSourceClass() {
        JTextField sourceClassTextField = (JTextField) dialogPanel.getComponent(1);
        JTextField targetClassTextField = (JTextField) dialogPanel.getComponent(3);
        JTextField convertClassTextField = (JTextField) dialogPanel.getComponent(5);
        sourceClassTextField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) { }

            @Override
            public void focusLost(FocusEvent e) {
                refreshComboBoxOption();
            }
        });
        targetClassTextField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) { }

            @Override
            public void focusLost(FocusEvent e) {
                setDefaultUtilClassName(targetClassTextField, convertClassTextField);
                refreshComboBoxOption();
            }
        });
        convertClassTextField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) { }

            @Override
            public void focusLost(FocusEvent e) {
                refreshComboBoxOption();
            }
        });
        // 寻找当前选中或打开的类
        boolean classFound = false;
        String selectedText = editor.getSelectionModel().getSelectedText();
        if (StringUtils.isNotBlank(selectedText)) {
            PsiClass[] classes = PsiShortNamesCache.getInstance(project).getClassesByName(
                    StringUtils.capitalize(selectedText), GlobalSearchScope.allScope(project));
            if (ArrayUtils.isNotEmpty(classes)) {
                PsiClass psiClasses = classes[0];
                sourceClassTextField.setText(psiClasses.getQualifiedName());
                classFound = true;
            }
        }
        if (!classFound) {
            PsiClass[] classes = psiJavaFile.getClasses();
            if (ArrayUtils.isNotEmpty(classes)) {
                PsiClass psiClasses = classes[0];
                sourceClassTextField.setText(psiClasses.getQualifiedName());
                classFound = true;
            }
        }
        // 初始化ComboBox
        refreshComboBoxOption();
    }

    private void refreshComboBoxOption() {
        ComboBox<String> comboBox = (ComboBox<String>) dialogPanel.getComponent(7);
        comboBox.removeAllItems();
        comboBoxData.clear();

        String sourceClassStr = ((JTextField)dialogPanel.getComponent(1)).getText().trim();
        String targetClassStr = ((JTextField)dialogPanel.getComponent(3)).getText().trim();
        String utilClassStr = ((JTextField)dialogPanel.getComponent(5)).getText().trim();
        // 添加util类所在的源码路径
        PsiJavaFile utilPsiJavaFile = PsiUtil.getProjectJavaFile(project, utilClassStr);
        if (utilPsiJavaFile != null) {
            VirtualFile virtualFile = PsiUtil.getSourceRootVirtualFile(utilPsiJavaFile);
            if (virtualFile != null && !comboBoxData.contains(virtualFile.getPath())) {
                comboBox.addItem(virtualFile.getPath());
                comboBoxData.add(virtualFile.getPath());
                return;
            }
        }
        // 添加editor所在类的源码路径
        VirtualFile virtualFile = PsiUtil.getSourceRootVirtualFile(psiJavaFile);
        if (virtualFile != null && !comboBoxData.contains(virtualFile.getPath())) {
            comboBox.addItem(virtualFile.getPath());
            comboBoxData.add(virtualFile.getPath());
        }
        // 添加source类所在的源码路径
        PsiJavaFile sourcePsiJavaFile = PsiUtil.getProjectJavaFile(project, sourceClassStr);
        if (sourcePsiJavaFile != null) {
            virtualFile = PsiUtil.getSourceRootVirtualFile(sourcePsiJavaFile);
            if (virtualFile != null && !comboBoxData.contains(virtualFile.getPath())) {
                comboBox.addItem(virtualFile.getPath());
                comboBoxData.add(virtualFile.getPath());
            }
        }
        // 添加target类所在的源码路径
        PsiJavaFile targetPsiJavaFile = PsiUtil.getProjectJavaFile(project, targetClassStr);
        if (targetPsiJavaFile != null) {
            virtualFile = PsiUtil.getSourceRootVirtualFile(targetPsiJavaFile);
            if (virtualFile != null &&  !comboBoxData.contains(virtualFile.getPath())) {
                comboBox.addItem(virtualFile.getPath());
                comboBoxData.add(virtualFile.getPath());
            }
        }

    }

    @Override
    protected void doOKAction() {
        try {
            String sourceClassStr = ((JTextField)dialogPanel.getComponent(1)).getText().trim();
            String targetClassStr = ((JTextField)dialogPanel.getComponent(3)).getText().trim();
            String utilClassStr = ((JTextField)dialogPanel.getComponent(5)).getText().trim();
            String basePath = ((String)((ComboBox<String>) dialogPanel.getComponent(7)).getSelectedItem()).trim();
            PsiDirectory baseDirectory = PsiUtil.getPsiDirectory(project, basePath);
            notNull(baseDirectory, "工具类源代码路径不存在");
            notEmpty(sourceClassStr, "请输入源类(from)");
            notEmpty(targetClassStr, "请输入目标类(to)");
            notEmpty(targetClassStr, "请输入生成的转换工具类");
            JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
            PsiClass sourceClass = psiFacade.findClass(sourceClassStr, GlobalSearchScope.allScope(project));
            PsiClass targetClass = psiFacade.findClass(targetClassStr, GlobalSearchScope.allScope(project));
            notNull(sourceClass, "源类(from)不存在");
            notNull(targetClass, "目标类(to)不存在");

            ConvertCodeGenUtil.genConvertClass(project, baseDirectory, sourceClass, targetClass, utilClassStr);
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

    private void setDefaultUtilClassName(JTextField classTextField, JTextField utilClassTextField) {
        String sourceClass = classTextField.getText();
        if (utilClassTextField.getText().isBlank() && !sourceClass.isBlank()) {
            int index = sourceClass.lastIndexOf(".");
            if (index != -1) {
                String packageName = sourceClass.substring(0, index);
                String className = sourceClass.substring(index + 1);
                String convertClass = packageName + ".convert." + className + "Converter";
                utilClassTextField.setText(convertClass);
            }
        }
    }
}
