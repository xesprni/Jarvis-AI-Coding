package com.qihoo.finance.lowcode.apitrack.dialog;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;
import com.qihoo.finance.lowcode.apitrack.entity.ApiGroupNode;
import com.qihoo.finance.lowcode.apitrack.entity.ApiNode;
import com.qihoo.finance.lowcode.apitrack.entity.ApplicationNode;
import com.qihoo.finance.lowcode.apitrack.util.ApiDesignUtils;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.entity.dto.AppBaseInfo;
import com.qihoo.finance.lowcode.common.entity.dto.interfacegen.InterfaceGenRequest;
import com.qihoo.finance.lowcode.common.entity.dto.interfacegen.InterfaceGenResult;
import com.qihoo.finance.lowcode.common.entity.dto.interfacegen.ResourceGenResult;
import com.qihoo.finance.lowcode.common.ui.base.DocumentUtils;
import com.qihoo.finance.lowcode.common.ui.table.BaseJTableWrap;
import com.qihoo.finance.lowcode.common.util.*;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import com.qihoo.finance.lowcode.gentracker.tool.ModuleUtils;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import com.qihoo.finance.lowcode.gentracker.util.IoUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import javax.swing.text.AbstractDocument;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;

/**
 * GenerateApiDialog
 *
 * @author fengjinfu-jk
 * date 2023/8/31
 * @version 1.0.0
 * @apiNote GenerateApiDialog
 */
@Slf4j
public class ApiGenCodeDialog extends DialogWrapper {
    private static final Dimension textFieldSize = new Dimension(900, 30);
    private final List<Module> moduleList = new LinkedList<>();
    private final Project project;

    public ApiGenCodeDialog(@Nullable Project project) {
        super(project);
        this.project = project;

        initComponents();

        // setting components size
        settingSizes();

        // setting components status
        settingComponentsStatus(ObjectUtils.defaultIfNull(project, ProjectUtils.getCurrProject()));

        // init event
        initEvent();

        // check
        checkMethodNames();

        // refresh
        refreshPath();

        log.info(Constants.Log.USER_ACTION, "用户打开接口设计生成代码");
    }

    private void initEvent() {
        //监听module选择事件
        moduleBox.addActionListener(e -> {
            refreshPath();
        });
    }

    private void refreshPath() {
        Module module = getSelectModule();
        if (Objects.nonNull(module)) {
            AppBaseInfo appBaseInfo = LowCodeAppUtils.queryApplicationCode(ProjectUtils.getCurrProjectName(), module.getName());
            String autoGenPackage = DataContext.getInstance(project).getSelectApiGroupNode().getAutoGenPackage();
            this.packageName.setText(StringUtils.defaultString(autoGenPackage, appBaseInfo.getRootPackage()));
        }
    }

    private void settingSizes() {
        JPanelUtils.setSize(dialogPane, new Dimension(1200, 800));
        errTips.setPreferredSize(new Dimension(900, 100));
    }

    private void settingComponentsStatus(@NotNull Project project) {
        errTips.setEditable(false);
        errTips.setForeground(JBColor.RED);
        errTips.setBorder(null);
        errTips.setOpaque(false);
        errTips.setBackground(JBColor.background());
        errTips.setFont(new Font("微软雅黑", Font.PLAIN, 12));

        // 初始化module，存在资源路径的排前面
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            // 存在源代码文件夹放前面，否则放后面
            if (ModuleUtils.existsSourcePath(module)) {
                this.moduleList.add(0, module);
            } else {
                this.moduleList.add(module);
            }
        }
        //初始化Module选择
        DataContext dataContext = DataContext.getInstance(project);
        String selectedModule = dataContext.getSelectApiGenModule();
        for (Module module : this.moduleList) {
            moduleBox.addItem(module.getName());
            if (org.apache.commons.lang3.StringUtils.isNotEmpty(selectedModule) && selectedModule.equals(module.getName())) {
                moduleBox.setSelectedItem(module.getName());
            }
        }

        // 类名方法名
        ApiGroupNode apiGroupNode = dataContext.getSelectApiGroupNode();
        this.controllerClass.setText(apiGroupNode.getClassName());
        this.controllerClass.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                checkMethodNames();
                super.keyReleased(e);
            }
        });
        this.controllerClass.addActionListener(e -> checkMethodNames());
        this.controllerClass.getDocument().addDocumentListener(methodNameListener());
        ((AbstractDocument) controllerClass.getDocument()).setDocumentFilter(DocumentUtils.createDocumentFilter(Constants.REGEX.ENG_NUM_UNDER_LINE));

        List<ApiNode> apiNodes = new ArrayList<>();
        for (Enumeration<? extends TreeNode> children = apiGroupNode.children(); children.hasMoreElements(); ) {
            TreeNode treeNode = children.nextElement();
            if (treeNode instanceof ApiNode) {
                apiNodes.add((ApiNode) treeNode);
            }
        }

        Object[][] apis = new Object[apiNodes.size()][];
        for (int i = 0; i < apiNodes.size(); i++) {
            ApiNode apiNode = apiNodes.get(i);
            Object[] api = {true, apiNode.toString(), apiNode.getMethodName(), apiNode.getId(), "必填项"};
            apis[i] = api;
        }

        DefaultTableModel methodsModel = new DefaultTableModel(apis, new String[]{"选中", "接口", "生成Controller方法名", "id", ""});
        methodsTable.setModel(methodsModel);

        configColumnProperties(methodsTable);
    }


    public void configColumnProperties(JBTable table) {
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();

        table.getColumnModel().getColumn(0).setCellRenderer(new BaseJTableWrap.CheckBoxRenderer());

        BaseJTableWrap.CheckBoxEditor checkBoxEditor = new BaseJTableWrap.CheckBoxEditor();
        checkBoxEditor.getCheckBox().addActionListener(e -> checkMethodNames());
        table.getColumnModel().getColumn(0).setCellEditor(checkBoxEditor);

        table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(1).setCellEditor(new BaseJTableWrap.TextCellEditor(false, false));

        BaseJTableWrap.TextCellEditor methodNameEditor = new BaseJTableWrap.TextCellEditor(true, false);
        JTextField textField = methodNameEditor.getTextField();
        textField.getDocument().addDocumentListener(methodNameListener());
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(2).setCellEditor(methodNameEditor);

        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(3).setCellEditor(new BaseJTableWrap.TextCellEditor(false, false));

        DefaultTableCellRenderer require = new DefaultTableCellRenderer();
        require.setForeground(JBColor.RED);
        table.getColumnModel().getColumn(4).setCellRenderer(require);
        BaseJTableWrap.TextCellEditor textCellEditor = new BaseJTableWrap.TextCellEditor(false, false);
        textCellEditor.getTextField().setEnabled(false);
        textCellEditor.getTextField().setForeground(JBColor.RED);
        table.getColumnModel().getColumn(4).setCellEditor(textCellEditor);

        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(0).setMinWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(450);

        table.getColumnModel().getColumn(4).setPreferredWidth(50);
        table.getColumnModel().getColumn(4).setMaxWidth(50);
        table.getColumnModel().getColumn(4).setMinWidth(50);

        // 隐藏ID列, 隐藏后需通过model获取值
        methodsTable.removeColumn(methodsTable.getColumnModel().getColumn(3));

        // 设置表头居中对齐
        JTableHeader header = table.getTableHeader();
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        header.setDefaultRenderer(headerRenderer);
        headerRenderer.setHorizontalAlignment(JLabel.CENTER);
    }

    private DocumentListener methodNameListener() {
        return new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                checkMethodNames();
            }
        };
    }

    private void checkMethodNames() {
        errTips.setText(StringUtils.EMPTY);
        String className = controllerClass.getText();
        if (StringUtils.isEmpty(className)) {
            errTips.setText("错误: 请填写Controller类名, 例如: UserInfoController\n");
        }

        // 检测methodName是否存在相同的
        List<String> names = new ArrayList<>();
        Set<String> dupNames = new HashSet<>();
        List<String> empNames = new ArrayList<>();
        TableModel model = methodsTable.getModel();
        TableCellEditor cellEditor = methodsTable.getCellEditor();
        if (Objects.nonNull(cellEditor) && cellEditor instanceof BaseJTableWrap.CheckBoxEditor) {
            cellEditor.stopCellEditing();
        }

        for (int i = 0; i < methodsTable.getRowCount(); i++) {
            Object selected = methodsTable.getValueAt(i, 0);
            if (Objects.nonNull(selected)) {
                if (selected instanceof Boolean && !(Boolean) selected) {
                    model.setValueAt(null, i, 4);
                    continue;
                }
            }

            String methodName = (String) ObjectUtils.defaultIfNull(methodsTable.getValueAt(i, 2), "");
            if (i == methodsTable.getEditingRow()) {
                methodName = (String) ObjectUtils.defaultIfNull(methodsTable.getCellEditor().getCellEditorValue(), "");
            }

            if (StringUtils.isEmpty(methodName)) {
                empNames.add((String) ObjectUtils.defaultIfNull(methodsTable.getValueAt(i, 1), ""));
                model.setValueAt("必填项", i, 4);
            } else if (names.contains(methodName)) {
                dupNames.add(methodName);
            } else {
                model.setValueAt(null, i, 4);
            }

            names.add(methodName);
        }

        if (CollectionUtils.isNotEmpty(dupNames)) {
            errTips.setText(errTips.getText() + String.format("错误: 存在同名方法 %s", String.join(" ,", dupNames)) + "\n");
            this.setOKActionEnabled(false);
        }

        if (CollectionUtils.isNotEmpty(empNames)) {
            for (String empName : empNames) {
                errTips.setText(errTips.getText() + String.format("错误: 接口 %s 未填写生成Controller方法名, 例如: queryPage", empName) + "\n");
            }
            this.setOKActionEnabled(false);
        }

        if (StringUtils.isNotEmpty(className) && CollectionUtils.isEmpty(dupNames) && CollectionUtils.isEmpty(empNames)) {
            errTips.setText(StringUtils.EMPTY);
            this.setOKActionEnabled(true);
        }
    }

    @Override
    protected void doOKAction() {
        log.info(Constants.Log.USER_ACTION, "用户确认接口设计生成代码");

        DataContext dataContext = DataContext.getInstance(project);
        String module = (String) moduleBox.getSelectedItem();
        dataContext.setSelectApiGenModule(module);

        ApplicationNode applicationNode = dataContext.getSelectApplicationNode();
        String appCode = applicationNode.getAppCode();

        ApiGroupNode apiGroupNode = dataContext.getSelectApiGroupNode();
        String token = apiGroupNode.getProjectToken();
        String categoryId = apiGroupNode.getId();

        // 类名
        String className = controllerClass.getText();
        // 方法名
        Map<Long, String> interfaces = getSelectInterfaces();

        InterfaceGenRequest genRequest = new InterfaceGenRequest();
        genRequest.setAppCode(appCode);
        genRequest.setProjectModule(module);
        genRequest.setToken(token);
        genRequest.setProjectId(apiGroupNode.getProjectId());
        genRequest.setCategoryId(Long.valueOf(categoryId));
        genRequest.setClassName(className);
        genRequest.setInterfaces(interfaces);
        setPackage(genRequest);


        new SwingWorker<>() {
            private Result<InterfaceGenResult> result = null;

            @Override
            protected Object doInBackground() {
                progressBar.setVisible(true);
                result = ApiDesignUtils.apiInterfaceGenerate(genRequest);
                return null;
            }

            @Override
            protected void done() {
                if (result.isSuccess()) {
                    InterfaceGenResult genResult = result.getData();
                    try {
                        // 文件写入
                        String path = write(genResult.getResources());
                        // 展开定位
                        String[] split = path.replaceAll("\\\\", ".").replaceAll("/", ".").split("src\\.main\\.java\\.");
                        if (split.length > 1) {
                            String expendPath = split[1];
                            if (expendPath.endsWith(".")) expendPath = expendPath.substring(0, expendPath.length() - 1);
                            ModuleUtils.selectAndOpenPackage(project, getSelectModule(), expendPath);
                        }
                        // 提示成功
                        Messages.showMessageDialog("代码生成完成\n\n" + String.join("\n", genResult.getWarnings()), "接口代码生成", Icons.scaleToWidth(Icons.SUCCESS, 60));

                        superDoOkAction();
                        // reload api tree
                        reloadCategory();
                        return;
                    } catch (IOException e) {
                        Messages.showMessageDialog("代码生成失败\n\n" + e.getMessage(), "接口代码生成", Icons.scaleToWidth(Icons.FAIL, 60));
                    }
                }

                Messages.showMessageDialog("代码生成失败\n\n" + result.getErrorCode() + " : " + result.getErrorMsg(), "接口代码生成", Icons.scaleToWidth(Icons.FAIL, 60));
                progressBar.setVisible(true);
                super.done();
            }
        }.execute();
    }

    private void superDoOkAction() {
        super.doOKAction();
    }

    private void setPackage(InterfaceGenRequest genRequest) {
        Module module = getSelectModule();

        if (Objects.nonNull(module)) {
            String packagePath = this.packageName.getText();
            // 接口设计无兼容问题, 直接返回用户填写的路径即可
            genRequest.setAutoGenPackage(packagePath);
            genRequest.setExtensionPackage(packagePath);

        }

    }

    private void reloadCategory() {
        CacheManager.refreshInnerCache();
        JTree tree = DataContext.getInstance(project).getApiTree();
        ApiGroupNode apiGroupNode = DataContext.getInstance(project).getSelectApiGroupNode();

        JTreeLoadingUtils.loading(true, tree, apiGroupNode, () -> ApiDesignUtils.apiInterfaceList(apiGroupNode));
    }

    private Map<Long, String> getSelectInterfaces() {
        Map<Long, String> interfaces = new HashMap<>();

        for (int i = 0; i < methodsTable.getRowCount(); i++) {
            Object valueAt = methodsTable.getValueAt(i, 0);
            // 选中
            if (Boolean.TRUE.equals(valueAt)) {
                Object id = methodsTable.getModel().getValueAt(i, 3);
                String methodName = (String) ObjectUtils.defaultIfNull(methodsTable.getValueAt(i, 2), "");

                if (StringUtils.isNotEmpty(methodName)) interfaces.put(Long.parseLong(id.toString()), methodName);
            }
        }

        return interfaces;
    }


    /**
     * 通过IDEA自带的Psi文件方式写入
     */
    public String write(List<ResourceGenResult> resources) throws IOException {
        String path = null;
        for (ResourceGenResult resource : resources) {
            String classFullName = resource.getClassFullName();
            String fullPackage = classFullName.substring(0, classFullName.lastIndexOf("."));
            String className = classFullName.substring(classFullName.lastIndexOf(".") + 1);
            // 处理保存路径
            String savePath = handlerPath(fullPackage, true);

            // 提示创建目录
            Path directoryPath = Paths.get(savePath);
            File directory = directoryPath.toFile();
            if (directory.getPath().contains("autogen")) {
                path = directory.getPath().substring(0, directory.getPath().indexOf("autogen"));
            } else {
                path = directory.getPath();
            }

            if (!directory.exists() && !directory.mkdirs()) {
                return directory.getPath();
            }

            File javaFile = new File(directory, className + ".java");
            if (!resource.isForcedOverlay() && javaFile.exists()) {
                // 不强制覆盖
                continue;
            }

            if (!javaFile.exists() && !javaFile.createNewFile()) {
                return directory.getPath();
            }

            IoUtils.writeFile(javaFile, resource.getContent(), true);
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            project.getBaseDir().refresh(false, true);
        });

        return path;
    }

    private String handlerPath(String fullPackage, boolean lowerCase) {
        String basePath = getBasePath();
        String path = basePath + "/" + fullPackage;
        String resPath = path.replaceAll("\\.", "/");

        return lowerCase ? resPath.toLowerCase() : resPath;
    }

    private String getBasePath() {
        Module module = getSelectModule();
        VirtualFile baseVirtualFile = ProjectUtils.getBaseDir(project);
        if (baseVirtualFile == null) {
            Messages.showWarningDialog("无法获取到项目基本路径 !", GlobalDict.TITLE_INFO);
            return "";
        }
        String baseDir = baseVirtualFile.getPath();
        if (module != null) {
            VirtualFile virtualFile = ModuleUtils.getSourcePath(module);
            if (virtualFile != null) {
                baseDir = virtualFile.getPath();
            }
        }

        return baseDir;
    }

    private Module getSelectModule() {
        String name = (String) moduleBox.getSelectedItem();
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        return ModuleManager.getInstance(project).findModuleByName(name);
    }


    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        moduleLabel = new JLabel();
        moduleBox = new ComboBox<>();
        moduleBox.setSwingPopup(false);
        packageLabel = new JLabel();
        packageName = new JTextField();
        controllerLabel = new JLabel();
        controllerClass = new JTextField();
        serviceLabel = new JLabel();
        methodsScroll = new JBScrollPane();
        methodsTable = new JBTable();
        tips = new JLabel();
        errTips = new JTextArea();

        //======== this ========
        JPanel contentPane = new JPanel();
        dialogPane = new JPanel();

        contentPane.setLayout(new GridLayoutManager(16, 2, JBUI.emptyInsets(), -1, -1));
        dialogPane.setBorder(JBUI.Borders.empty(60, 60, 100, 60));
        dialogPane.setLayout(new BorderLayout());

        progressBar = new JProgressBar();
        progressPanel = new JPanel();

        //---- moduleLabel ----
        moduleLabel.setText("所属模块");
        contentPane.add(moduleLabel, new GridConstraints(0, 0, 1, 1,
                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null, null, null));
        contentPane.add(moduleBox, new GridConstraints(0, 1, 1, 1,
                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                textFieldSize, textFieldSize, textFieldSize));

        //---- controllerLabel ----
        packageLabel.setText("包路径");
        contentPane.add(packageLabel, new GridConstraints(1, 0, 1, 1,
                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null, null, null));
        contentPane.add(packageName, new GridConstraints(1, 1, 1, 1,
                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                textFieldSize, textFieldSize, textFieldSize));

        //---- controllerLabel ----
        controllerLabel.setText("Controller类名");
        contentPane.add(controllerLabel, new GridConstraints(2, 0, 1, 1,
                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null, null, null));
        contentPane.add(controllerClass, new GridConstraints(2, 1, 1, 1,
                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                textFieldSize, textFieldSize, textFieldSize));

        //---- serviceLabel ----
        serviceLabel.setText("生成接口");
        contentPane.add(serviceLabel, new GridConstraints(3, 0, 1, 1,
                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null, null, null));

        methodsScroll.setViewportView(methodsTable);
        contentPane.add(methodsScroll, new GridConstraints(3, 1, 1, 1,
                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                new Dimension(890, 350), new Dimension(890, 350), new Dimension(890, 350)));

        //---- Tips ----
        tips.setText("注意: 生成的代码会覆盖Controller原有代码");
        contentPane.add(tips, new GridConstraints(9, 1, 1, 1,
                GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null, null, null));

        //---- errTips ----
        contentPane.add(errTips, new GridConstraints(10, 1, 5, 1,
                GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null, null, null));

        dialogPane.add(contentPane, BorderLayout.CENTER);

        {
            progressBar.setStringPainted(true);
            // 设置采用不确定进度条
            progressBar.setIndeterminate(true);
            progressBar.setVisible(false);
            progressBar.setString("代码生成中......");// 设置提示信息

            progressPanel.setBorder(JBUI.Borders.emptyTop(12));
            progressPanel.setLayout(new BorderLayout());
            progressPanel.add(progressBar, BorderLayout.CENTER);
        }
        dialogPane.add(progressPanel, BorderLayout.PAGE_END);

        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on

        init();
        setTitle(GlobalDict.TITLE_INFO + "-接口生成代码");
        setOKButtonText("代码生成");
        setCancelButtonText("取消");
        this.setModal(false);
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel moduleLabel;
    private ComboBox<String> moduleBox;
    private JLabel packageLabel;
    private JTextField packageName;
    private JLabel controllerLabel;
    private JTextField controllerClass;
    private JLabel serviceLabel;
    private JBScrollPane methodsScroll;
    private JBTable methodsTable;
    private JLabel tips;
    private JTextArea errTips;
    private JPanel dialogPane;
    private JProgressBar progressBar;
    private JPanel progressPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return dialogPane;
    }
}
