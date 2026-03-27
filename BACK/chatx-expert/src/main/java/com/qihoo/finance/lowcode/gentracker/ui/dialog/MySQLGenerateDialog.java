package com.qihoo.finance.lowcode.gentracker.ui.dialog;

import com.google.common.collect.Sets;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.qihoo.finance.lowcode.common.configuration.SelectModule;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.entity.dto.AppBaseInfo;
import com.qihoo.finance.lowcode.common.entity.dto.generate.GenerateOptions;
import com.qihoo.finance.lowcode.common.entity.dto.generate.SimpleFileTypeMatch;
import com.qihoo.finance.lowcode.common.listener.KeyListener;
import com.qihoo.finance.lowcode.common.ui.CustomHeightTabbedPaneUI;
import com.qihoo.finance.lowcode.common.ui.base.MultiComboBox;
import com.qihoo.finance.lowcode.common.ui.base.RoundedLabel;
import com.qihoo.finance.lowcode.common.ui.base.VFlowLayout;
import com.qihoo.finance.lowcode.common.util.*;
import com.qihoo.finance.lowcode.design.constant.FieldTypeMatch;
import com.qihoo.finance.lowcode.design.dto.rdb.RdbFieldTypeConfig;
import com.qihoo.finance.lowcode.design.entity.DatabaseColumnNode;
import com.qihoo.finance.lowcode.design.entity.MySQLTableNode;
import com.qihoo.finance.lowcode.gentracker.config.SettingsSettingStorage;
import com.qihoo.finance.lowcode.gentracker.constants.StrState;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import com.qihoo.finance.lowcode.gentracker.dto.GenerateVelocityOptions;
import com.qihoo.finance.lowcode.gentracker.entity.*;
import com.qihoo.finance.lowcode.gentracker.listener.GenerateListenerManager;
import com.qihoo.finance.lowcode.gentracker.service.MySQLGenerateService;
import com.qihoo.finance.lowcode.gentracker.service.SettingsStorageService;
import com.qihoo.finance.lowcode.gentracker.service.TableInfoSettingsService;
import com.qihoo.finance.lowcode.gentracker.tool.JSON;
import com.qihoo.finance.lowcode.gentracker.tool.ModuleUtils;
import com.qihoo.finance.lowcode.gentracker.tool.NameUtils;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import com.qihoo.finance.lowcode.gentracker.ui.base.OtherSettingMap;
import com.qihoo.finance.lowcode.gentracker.ui.component.TemplateSelectComponent;
import com.qihoo.finance.lowcode.gentracker.ui.table.TypeMatchTableWrap;
import com.qihoo.finance.lowcode.gentracker.util.GenerateTrackUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 代码生成路径选择框
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
@Slf4j
public class MySQLGenerateDialog extends GenerateDialog {

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return new JBScrollPane(this.globalPanel);
    }

    @Override
    protected JComponent createSouthPanel() {
        JComponent southPanel = super.createSouthPanel();
        JPanel vFlowPanel = new JPanel(new VFlowLayout(VFlowLayout.TOP));
        vFlowPanel.add(this.progressBar);
        vFlowPanel.add(southPanel);

        return vFlowPanel;
    }

    public MySQLGenerateDialog(Project project, boolean modal, int step, Runnable doAfterOk) {
        super(project);
        this.doAfterOk = doAfterOk;
        initDialog(project, modal, step);
    }

    private void initDialog(Project project, boolean modal, int step) {
        this.step = step;
        this.tableInfoService = TableInfoSettingsService.getInstance();
        this.mySQLGenerateService = MySQLGenerateService.getInstance(project);
        // 初始化module，存在资源路径的排前面
        this.moduleList = new LinkedList<>();
        this.cusComponentGroupMap = new HashMap<>();
        this.cusPanelGroupMap = createCusPanels();

        // generateOptions
        this.table = DataContext.getInstance(project).getSelectDbTable();
        this.dbTable = TableInfoSettingsService.getInstance().getTableInfo(table);
        this.moduleRemoteOptions = GenerateTrackUtils.queryModuleGenerateOptions(table.getDatabase(), table.getTableName());
        // className
        this.tableClassName = NameUtils.getInstance().getClassName(table.getTableName());

        this.initModuleList(project);

        this.initComponents();
        this.initComponentsData();
        this.refreshData();
        this.initEvent();

        // 设置默认选中包路径
        this.resetSelectPackage();
        // 初始化路径
        this.refreshPath(true);
        // 初始化选中模块
        this.triggerEvent(this.moduleRemoteOptions);

        this.setOKButtonText("生成代码");
        this.setCancelButtonText("取消");
        this.setModal(modal);

        init();
        setTitle(GlobalDict.TITLE_INFO + "-MySQL生成代码");
        initSize();

        log.info(Constants.Log.USER_ACTION, String.format("用户点击打开MySQL生成代码界面 %s.%s", table.getDatabase(), table.getTableName()));
    }

    @Override
    protected @NotNull Action getOKAction() {
        return super.getOKAction();
    }

    private void superDoOKAction() {
        super.doOKAction();
    }

    @Override
    protected void doOKAction() {
        log.info(Constants.Log.USER_ACTION, "用户点击确认生成代码");
        // check warning
        if (!checkWarning()) return;

        // save generateOptions
        saveGenerateOptions();
        // saveSelectModule
        saveSelectModule();

        SwingWorkerUtils.execute(() -> {
            enabledDialogSelector(false);
            showProgress();
            return getOkActionResult();
        }, saveFiles -> {
            // 保存物理文件
            ApplicationManager.getApplication().invokeLater(() -> {
                List<SaveFile> saveFileLis = mySQLGenerateService.executeSaveFile(saveFiles);
                // UI更新
                hiddenProgress();
                enabledDialogSelector(true);
                // 缓存需要刷新
                CacheManager.refreshInnerCache();
                if (CollectionUtils.isNotEmpty(saveFileLis)) {
                    // 文件生成后, 展开项目并定位至生成文件包路径
                    ModuleUtils.selectAndOpenPackage(project, getSelectModule(), packageField.getText());
                }

                // 气泡提醒, 在NotificationGroup中添加消息通知内容，以及消息类型。这里为MessageType.INFO
                String msg = table.getDatabase() + "." + table.getTableName() + "\n\n 生成代码完成";
                NotifyUtils.notify(msg, NotificationType.INFORMATION);
                Messages.showInfoMessage(msg, GlobalDict.TITLE_INFO);
                superDoOKAction();
                doAfterOk();
            });
        });
    }

    private boolean checkWarning() {
        for (ColumnInfo columnInfo : dbTable.getFullColumn()) {
            String type = columnInfo.getType();
            if (FieldTypeMatch.WARNING_TYPE.contains(type)) {
                int warnDialog = Messages.showDialog(project, fieldTypeMathTips, "字段类型配置", new String[]{"忽略", "查看字段类型配置"}, 1, Icons.scaleToWidth(Icons.WARNING, 60));
                // ignore
                if (0 == warnDialog) return true;

                baseSettingPanel.setSelectedIndex(1);
                return false;
            }
        }

        return true;
    }

    private void doAfterOk() {
        if (Objects.nonNull(doAfterOk)) {
            doAfterOk.run();
        }
    }

    private List<SaveFile> getOkActionResult() {
        try {
            return onOK();
        } catch (Exception e) {
            log.error(ExceptionUtils.getStackTrace(e));
        }

        return new ArrayList<>();
    }

    //------------------------------------------------------------------------------------------------------------------

    private void triggerEvent(Map<String, GenerateOptions> moduleOptions) {
        for (Module module : moduleList) {
            if (moduleOptions.containsKey(module.getName())) {
                this.moduleComboBox.setSelectedItem(module);
                break;
            }
        }

        moduleComboBoxAction();
        templateGroupAction();
    }

    private void initModuleList(Project project) {
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            // 存在源代码文件夹放前面，否则放后面
            if (ModuleUtils.existsSourcePath(module)) {
                this.moduleList.add(0, module);
            } else {
                this.moduleList.add(module);
            }
        }
    }

    private void initSize() {
        JPanelUtils.setSize(this.globalPanel, new Dimension(1000, 600));
    }

    private void resetSelectPackage() {
        Module selectModule = getSelectModule();
        if (Objects.nonNull(selectModule)) {
            SelectModule peekModule = userContext.peekModule(selectModule.getName());

            GenerateOptions generateOptions = moduleRemoteOptions.get(selectModule.getName());
            if (Objects.nonNull(generateOptions)) {
                updatePackage(generateOptions);
            } else if (Objects.nonNull(peekModule)) {
                updatePackage(peekModule);
            } else {
                // set default value
                selectModule = this.moduleList.get(0);
                AppBaseInfo appBaseInfo = LowCodeAppUtils.queryApplicationCode(ProjectUtils.getCurrProjectName(), selectModule.getName());
                packageField.setText(appBaseInfo.getRootPackage());

                // useExample
                this.useExample.setSelected(false);
                // useAutogen
                this.useAutogen.setSelected(true);
                // useAutogen
                this.useLombok.setSelected(false);
                // useAutogen
                this.extendMsfEntity.setSelected(false);
            }
        }
    }

    private void updatePackage(SelectModule peekModule) {
        packageField.setText(peekModule.getPackageName());

        if (StringUtils.isNotEmpty(peekModule.getCheckBoxEnable())) {
            // useExample
            this.useExample.setSelected(peekModule.isUseExample());
            // useAutogen
            this.useAutogen.setSelected(peekModule.isUseAutogen());
            // useLombok
            this.useLombok.setSelected(peekModule.isUseLombok());
            // extendMsfEntity
            this.extendMsfEntity.setSelected(peekModule.isExtendMsfEntity());
            // useMsfResponse
            this.useMsfResponse.setSelected(peekModule.isUseMsfResponse());
        }
    }

    private void updatePackage(GenerateOptions options) {
        if (Objects.isNull(options)) return;

        packageField.setText(options.getPackageName());
        if (StringUtils.isNotEmpty(options.getEntityPackage())) {
            entityPackageField.setText(options.getEntityPackage());
        }

        if (StringUtils.isNotEmpty(options.getDtoPackage())) {
            dtoPackageField.setText(options.getDtoPackage());
        }

        if (StringUtils.isNotEmpty(options.getDaoPackage())) {
            daoPackageField.setText(options.getDaoPackage());
        }

        if (StringUtils.isNotEmpty(options.getMapperPackage())) {
            mapperPackageField.setText(options.getMapperPackage());
        }

        if (StringUtils.isNotEmpty(options.getControllerPackage())) {
            controllerPackageField.setText(options.getControllerPackage());
        }

        if (StringUtils.isNotEmpty(options.getServicePackage())) {
            servicePackageField.setText(options.getServicePackage());
        }

        if (StringUtils.isNotEmpty(options.getFacadeModuleName())) {
            facadeModuleComboBox.setSelectedItem(options.getFacadeModuleName());
        }

        if (StringUtils.isNotEmpty(options.getFacadePackage())) {
            facadePackageField.setText(options.getFacadePackage());
        }

        if (StringUtils.isNotEmpty(options.getFacadeImplPackage())) {
            facadeImplPackageField.setText(options.getFacadeImplPackage());
        }

        // 表名前缀
        subClassNamePrefixField.setText(StringUtils.defaultString(options.getSubTableNamePrefix()));

        // 业务主键
        List<String> busPriKeys = ListUtils.defaultIfNull(options.getBusPriKeys(), new ArrayList<>());
        busPriKeyMulBox.setSelectedItems(busPriKeys);

        // fileName
        if (StringUtils.isNotEmpty(options.getEntityName())) {
            this.entityName.setText(options.getEntityName());
        }
        if (StringUtils.isNotEmpty(options.getDtoName())) {
            this.dtoName.setText(options.getDtoName());
        }
        if (StringUtils.isNotEmpty(options.getDaoName())) {
            this.daoName.setText(options.getDaoName());
        }
        if (StringUtils.isNotEmpty(options.getMapperName())) {
            this.mapperName.setText(options.getMapperName());
        }
        if (StringUtils.isNotEmpty(options.getControllerName())) {
            this.controllerName.setText(options.getControllerName());
        }
        if (StringUtils.isNotEmpty(options.getServiceName())) {
            this.serviceName.setText(options.getServiceName());
        }
        if (StringUtils.isNotEmpty(options.getFacadeName())) {
            this.facadeName.setText(options.getFacadeName());
        }
        if (StringUtils.isNotEmpty(options.getFacadeImplName())) {
            this.facadeImplName.setText(options.getFacadeImplName());
        }

        subClassNamePrefix();
        // useExample
        this.useExample.setSelected(options.isUseExample());
        // useAutogen
        this.useAutogen.setSelected(options.isUseAutogen());
        // useAutogen
        this.useLombok.setSelected(options.isUseLombok());
        // extendMsfEntity
        this.extendMsfEntity.setSelected(options.isExtendMsfEntity());
        // useMsfResponse
        this.useMsfResponse.setSelected(options.isUseMsfResponse());
        // useDtoParam
        this.useDtoParam.setSelected(options.isUseDtoParam());
    }

    private void moduleComboBoxAction() {
        // 重置默认package
        resetSelectPackage();
        // 刷新路径
        refreshPath();
        // 刷新自定义配置
        refreshCusPanel();
        // 字段类型配置
        refreshFieldTypeMatch();
    }

    private void refreshFieldTypeMatch() {
        Module selectModule = getSelectModule();
        if (Objects.isNull(selectModule)) return;

        // 获取云端数据字段类型配置
        GenerateOptions generateOptions = moduleRemoteOptions.get(selectModule.getName());
        List<SimpleFileTypeMatch> fileTypeMatches = Objects.nonNull(generateOptions) ? generateOptions.getFileTypeMatches() : null;

        // 刷新类型匹配列表
        boolean warning = false;
        List<String> missingMatchFields = new ArrayList<>();
        List<ColumnInfo> columns = dbTable.getFullColumn();
        Map<String, SimpleFileTypeMatch> fileTypeMatchMap = ListUtils.defaultIfNull(fileTypeMatches, new ArrayList<>()).stream()
                .collect(Collectors.toMap(SimpleFileTypeMatch::getField, Function.identity(), (o1, o2) -> o1));
        // 云端配置
        for (ColumnInfo column : columns) {
            DatabaseColumnNode dbColumn = column.getObj();
            String dbFieldName = dbColumn.getFieldName();
            SimpleFileTypeMatch typeMatch = fileTypeMatchMap.get(dbFieldName);
            // 考虑某个字段类型被修改，但云端记录仍然是旧的，需要忽略此字段的云端类型记录
            if (Objects.nonNull(typeMatch) && dbColumn.getFieldType().equals(typeMatch.getFieldType())) {
                column.setType(typeMatch.getJavaType());
                column.setShortType(typeMatch.getJavaShortType());
                column.setJdbcType(typeMatch.getJdbcType());
            } else {
                defaultTypeMatch(column);
            }

            if (FieldTypeMatch.WARNING_TYPE.contains(column.getType())) {
                warning = true;
                missingMatchFields.add(dbFieldName);
            }
        }

        if (CollectionUtils.isNotEmpty(missingMatchFields)) {
            typeMatchTips.setText(fieldTypeMathTips);
            typeMatchTips.setVisible(true);
        } else {
            typeMatchTips.setVisible(false);
            typeMatchTips.setText(StringUtils.EMPTY);
        }

        typeMatchTableScroll.setViewportView(typeMatchTableWrap.createTable());
        baseSettingPanel.remove(typeMatchPanel);
        baseSettingPanel.addTab("字段类型配置", warning ? Icons.scaleToWidth(Icons.WARNING, 16) : Icons.scaleToWidth(Icons.COLUMN, 16), typeMatchPanel);
    }

    private static void defaultTypeMatch(ColumnInfo column) {
        RdbFieldTypeConfig defaultTypeMatch = FieldTypeMatch.getFieldTypeConfig(column.getObj().getFieldType());
        if (Objects.isNull(defaultTypeMatch)) return;

        String defaultJavaType = defaultTypeMatch.getDefaultJavaType();
        column.setType(defaultTypeMatch.getDefaultJavaType());
        column.setShortType(ColumnInfo.convertShortType(defaultJavaType));
        column.setJdbcType(defaultTypeMatch.getDefaultJdbcType());
    }

    private void initEvent() {
        //监听module选择事件
        moduleComboBox.addActionListener(e -> moduleComboBoxAction());

        //添加包选择事件
        packageChooseButton.addActionListener(e -> {
            //将当前选中的model设置为基础路径
            try {
                Class<?> cls = Class.forName("com.intellij.ide.util.PackageChooserDialog");

                Constructor<?> constructor = cls.getConstructor(String.class, Project.class);
                Object dialog = constructor.newInstance("Package Chooser", project);
                // 显示窗口
                Method showMethod = cls.getMethod("show");
                showMethod.invoke(dialog);
                // 获取选中的包名
                Method getSelectedPackageMethod = cls.getMethod("getSelectedPackage");
                Object psiPackage = getSelectedPackageMethod.invoke(dialog);
                if (psiPackage != null) {
                    Method getQualifiedNameMethod = psiPackage.getClass().getMethod("getQualifiedName");
                    String packageName = (String) getQualifiedNameMethod.invoke(psiPackage);
                    packageField.setText(packageName);
                    // 刷新路径
                    refreshPath();
                }
            } catch (Exception ignored) {
                packageChooseButton.setEnabled(false);
                packageChooseButton.setVisible(false);
            }
        });

        packageField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                // 刷新路径
                refreshPath();

                // packageName 级联
                String packageName = packageField.getText();
                entityPackageField.setText(packageName + ".domain." + (userContext.syncEntityPackage ? StringUtils.lowerCase(userContext.entityAlias) : "entity"));
                dtoPackageField.setText(packageName + ".domain." + (userContext.syncDtoPackage ? StringUtils.lowerCase(userContext.dtoAlias) : "dto"));
                daoPackageField.setText(packageName + "." + (userContext.syncDaoPackage ? StringUtils.lowerCase(userContext.daoAlias) : "dao"));
                controllerPackageField.setText(packageName + "." + (userContext.syncControllerPackage ? StringUtils.lowerCase(userContext.controllerAlias) : "controller"));
                servicePackageField.setText(packageName + "." + (userContext.syncServicePackage ? StringUtils.lowerCase(userContext.serviceAlias) : "service"));
                facadePackageField.setText(packageName + "." + (userContext.syncFacadePackage ? StringUtils.lowerCase(userContext.facadeAlias) : "facade"));
                facadeImplPackageField.setText(packageName + "." + (userContext.syncFacadePackage ? StringUtils.lowerCase(userContext.facadeAlias) : "facade") + ".impl");
                mapperPackageField.setText((userContext.syncMapperPackage ? StringUtils.lowerCase(userContext.mapperAlias) : "mapper"));

                // facadePackageField不受packageField联动影响
            }
        });

        DocumentAdapter documentAdapter = new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                // 刷新路径
                updateDialogSelector();
            }
        };

        entityPackageField.getDocument().addDocumentListener(documentAdapter);
        daoPackageField.getDocument().addDocumentListener(documentAdapter);
        dtoPackageField.getDocument().addDocumentListener(documentAdapter);
        mapperPackageField.getDocument().addDocumentListener(documentAdapter);
        controllerPackageField.getDocument().addDocumentListener(documentAdapter);
        servicePackageField.getDocument().addDocumentListener(documentAdapter);
        facadePackageField.getDocument().addDocumentListener(documentAdapter);
        facadeImplPackageField.getDocument().addDocumentListener(documentAdapter);
        // 限制输入
        subClassNamePrefixField.addKeyListener(KeyListener.inputCheck());
        entityName.addKeyListener(KeyListener.inputCheck());
        dtoName.addKeyListener(KeyListener.inputCheck());
        daoName.addKeyListener(KeyListener.inputCheck());
        mapperName.addKeyListener(KeyListener.inputCheck());
        serviceName.addKeyListener(KeyListener.inputCheck());
        controllerName.addKeyListener(KeyListener.inputCheck());
        facadeName.addKeyListener(KeyListener.inputCheck());
        facadeImplName.addKeyListener(KeyListener.inputCheck());

        //选择路径
        pathChooseButton.addActionListener(e -> {
            //将当前选中的model设置为基础路径
            VirtualFile path = ProjectUtils.getBaseDir(project);
            Module module = getSelectModule();
            if (module != null) {
                path = ModuleUtils.getSourcePath(module);
            }
            VirtualFile virtualFile = FileChooser.chooseFile(FileChooserDescriptorFactory.createSingleFolderDescriptor(), project, path);
            if (virtualFile != null) {
                pathField.setText(virtualFile.getPath());
            }
        });

        // 联动事件
        genService.addActionListener(e -> {
            boolean notDefaultGroup = !templateSelectComponent.isDefaultGroup();
            serviceDtoPackagePanel.setVisible(genService.isSelected() && notDefaultGroup);
            servicePackage.setVisible(genService.isSelected());
            updateDialogSelector();
        });

        // 联动事件
        genController.addActionListener(e -> {
            boolean notDefaultGroup = !templateSelectComponent.isDefaultGroup();
            controllerPackage.setVisible(genController.isSelected());
            if (genController.isSelected()) {
                genService.setSelected(true);
                serviceDtoPackagePanel.setVisible(genService.isSelected() && notDefaultGroup);
                servicePackage.setVisible(true);
            } else {
                serviceDtoPackagePanel.setVisible(genService.isSelected() && notDefaultGroup);
                servicePackage.setVisible(genService.isSelected());
            }

            updateDialogSelector();
        });

        genFacade.addActionListener(e -> {
            facadePackage.setVisible(genFacade.isSelected());
            facadeImplPackage.setVisible(genFacade.isSelected());
            updateDialogSelector();
        });

        subClassNamePrefixField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                subClassNamePrefix();
            }
        });

        baseSettingPanel.addChangeListener(e -> {
            Component selectedComponent = baseSettingPanel.getSelectedComponent();
            bottomSettingTab.setVisible(selectedComponent.equals(baseSetting));
        });

        // ORM框架选中事件
        templateSelectComponent.addActionListener(this::actionPerformed);

        // dtoPackageAction
        otherSettings.registerListener(settingMap -> dtoPackageAction());
        templateSelectComponent.addActionListener(e -> dtoPackageAction());
    }

    private void dtoPackageAction() {
        // dto包路径显隐事件, dto仅在「queryAllByLimit」「count」方法存在时生成
        if (templateSelectComponent.isDefaultGroup()) {
            serviceDtoPackagePanel.setVisible(false);
        } else {
            dtoPackagePanel.setVisible(false);
            serviceDtoPackagePanel.setVisible(genService.isSelected());
            return;
        }

        Set<String> generateMethods = otherSettings.getOrDefault(OtherSettingMap.generateMethods, new HashSet<>());
        dtoPackagePanel.setVisible(generateMethods.contains(OtherSettingMap.queryAllByLimit) || generateMethods.contains(OtherSettingMap.count));
    }

    private void templateGroupAction() {
        String newGroup = templateSelectComponent.getSelectedGroupName();

        // 动态高级选项区
        cusSettingPanel.removeAll();
        cusPanelGroupMap.getOrDefault(newGroup, new ArrayList<>()).forEach(cusSettingPanel::add);

        // useExample useAutogen 事件
        boolean defaultGroup = templateSelectComponent.isDefaultGroup();
        this.useExample.setVisible(defaultGroup);
        this.useAutogen.setVisible(defaultGroup);
        this.mapperPanel.setVisible(defaultGroup);
    }

    private void subClassNamePrefix() {
        subStartStringIgnoreCase(entityName, subClassNamePrefixField.getText());
        subStartStringIgnoreCase(dtoName, subClassNamePrefixField.getText());
        subStartStringIgnoreCase(daoName, subClassNamePrefixField.getText());
        subStartStringIgnoreCase(mapperName, subClassNamePrefixField.getText());
        subStartStringIgnoreCase(serviceName, subClassNamePrefixField.getText());
        subStartStringIgnoreCase(controllerName, subClassNamePrefixField.getText());
        subStartStringIgnoreCase(facadeName, subClassNamePrefixField.getText());
        subStartStringIgnoreCase(facadeImplName, subClassNamePrefixField.getText());
    }

    private void subStartStringIgnoreCase(JTextField textField, String startWith) {
        String text = textField.getText();
        String classSuffix = StringUtils.EMPTY;
        for (String suffix : CLASS_NAME_SUFFIX) {
            if (text.endsWith(suffix)) {
                classSuffix = suffix;
                break;
            }
        }

        startWith = startWith.replaceAll("_", StringUtils.EMPTY);
        if (!tableClassName.toLowerCase().startsWith(startWith.toLowerCase())) {
            startWith = StringUtils.EMPTY;
        }

        // 剪除
        String className = tableClassName.substring(startWith.length()) + classSuffix;
        if (text.endsWith(className)) {
            textField.setText(className);
        }

        // 还原
        if (className.endsWith(text)) {
            textField.setText(className);
        }
    }

    private void refreshCusPanel() {
        Module selectModule = getSelectModule();
        if (Objects.isNull(selectModule)) return;

        GenerateOptions options = this.moduleRemoteOptions.get(selectModule.getName());
        if (Objects.isNull(options)) {
            for (Map.Entry<String, List<Component>> entry : cusComponentGroupMap.entrySet()) {
                String key = entry.getKey();
                for (Component component : entry.getValue()) {
                    if (component instanceof JBCheckBox) {
                        // 默认全选
                        ((JBCheckBox) component).setSelected(true);
                        checkBoxAction(key, ((JBCheckBox) component).getText(), (JBCheckBox) component);
                    }
                }
            }

            return;
        }

        Map<String, Set<String>> remoteOtherSettings = options.getOtherSettings();
        remoteOtherSettings.forEach((k, v) -> {
            if (this.cusComponentGroupMap.containsKey(k)) {
                List<Component> components = this.cusComponentGroupMap.get(k);
                for (Component component : components) {
                    if (component instanceof JBCheckBox) {
                        String text = ((JBCheckBox) component).getText();
                        ((JBCheckBox) component).setSelected(v.contains(text));
                        checkBoxAction(k, ((JBCheckBox) component).getText(), (JBCheckBox) component);
                    }
                }
            }
        });
    }

    private void updateDialogSelector() {
        // 刷新路径
        boolean checkPackage = StringUtils.isNotEmpty(entityPackageField.getText()) && StringUtils.isNotEmpty(daoPackageField.getText()) && StringUtils.isNotEmpty(dtoPackageField.getText()) && StringUtils.isNotEmpty(mapperPackageField.getText());

        if (genController.isSelected()) {
            checkPackage = checkPackage && StringUtils.isNotEmpty(controllerPackageField.getText()) && StringUtils.isNotEmpty(servicePackageField.getText());
        }
        if (genFacade.isSelected()) {
            checkPackage = checkPackage && StringUtils.isNotEmpty(facadePackageField.getText()) && StringUtils.isNotEmpty(facadeImplPackageField.getText());
        }

        enabledDialogSelector(checkPackage);
    }

    private void refreshData() {
        // 获取选中的表信息（鼠标右键的那张表），并提示未知类型
        // 设置默认配置信息
        if (!StringUtils.isEmpty(dbTable.getSaveModelName())) {
            moduleComboBox.setSelectedItem(dbTable.getSaveModelName());
        }
        if (!StringUtils.isEmpty(dbTable.getSavePackageName())) {
            packageField.setText(dbTable.getSavePackageName());
        }
//        if (!StringUtils.isEmpty(tableInfo.getPreName())) {
//            subClassNamePrefixField.setText(tableInfo.getPreName());
//        }
        SettingsSettingStorage settings = SettingsStorageService.getSettingsStorage();
        String groupName = settings.getCurrTemplateGroupName();
        if (!StringUtils.isEmpty(dbTable.getTemplateGroupName())) {
            if (settings.getTemplateGroupMap().containsKey(dbTable.getTemplateGroupName())) {
                groupName = dbTable.getTemplateGroupName();
            }
        }
        templateSelectComponent.setSelectedGroupName(groupName);
        String savePath = dbTable.getSavePath();
        if (!StringUtils.isEmpty(savePath)) {
            // 判断是否需要拼接项目路径
            if (savePath.startsWith(StrState.RELATIVE_PATH)) {
                String projectPath = project.getBasePath();
                savePath = projectPath + savePath.substring(1);
            }
            pathField.setText(savePath);
        }

        // 业务字段选择
        setEntityColumns(dbTable);

        List<ColumnInfo> entityColumn = dbTable.getEntityColumn();
        List<String> fields = entityColumn.stream().map(ColumnInfo::getObj).map(DatabaseColumnNode::getFieldName).toList();
        busPriKeyMulBox.setItems(fields);

        List<ColumnInfo> businessColumn = dbTable.getBusinessColumn();
        if (CollectionUtils.isNotEmpty(businessColumn)) {
            List<String> selectFields = businessColumn.stream().map(ColumnInfo::getObj).filter(Objects::nonNull).map(DatabaseColumnNode::getFieldName).toList();
            busPriKeyMulBox.setSelectedItems(selectFields);
        }
    }

    public static void setEntityColumns(TableInfo tableInfo) {
        boolean extendsBaseEntity = isExtendsBaseEntity(tableInfo);
        if (extendsBaseEntity) {
            List<ColumnInfo> entityColumns = tableInfo.getFullColumn().stream().filter(c -> !Constants.DB_COLUMN.BASE_ENTITY.containsKey(c.getObj().getFieldName())).collect(Collectors.toList());
            tableInfo.setEntityColumn(entityColumns);
        } else {
            tableInfo.setEntityColumn(tableInfo.getFullColumn());
        }
    }

    private static boolean isExtendsBaseEntity(TableInfo tableInfo) {
        int matchCount = 0;
        List<ColumnInfo> fullColumn = tableInfo.getFullColumn();
        Map<String, String> baseEntity = Constants.DB_COLUMN.MIN_BASE_ENTITY;
        for (ColumnInfo columnInfo : fullColumn) {
            DatabaseColumnNode dbColumn = columnInfo.getObj();
            if (baseEntity.containsKey(dbColumn.getFieldName())) {
                String fieldTypes = baseEntity.get(dbColumn.getFieldName());
                for (String fieldType : fieldTypes.split("\\|")) {
                    if (dbColumn.getFieldType().contains(fieldType)) {
                        matchCount++;
                        break;
                    }
                }
            }
        }

        return matchCount == baseEntity.size();
    }

    /**
     * 确认按钮回调事件
     */
    private List<SaveFile> onOK() {
        List<Template> selectTemplateList = templateSelectComponent.getAllSelectedTemplate();
        // 如果选择的模板是空的
        if (selectTemplateList.isEmpty()) {
            hiddenProgress();
            Messages.showWarningDialog("请选择ORM框架 !", GlobalDict.TITLE_INFO);
            throw new RuntimeException("请选择ORM框架");
        }

        // 追加预置模板
        selectTemplateList.addAll(templateSelectComponent.getInnerTemplate(selectTemplateList));

        // 保存物理路径跟package
        String savePath = pathField.getText();
        if (StringUtils.isEmpty(savePath)) {
            hiddenProgress();
            Messages.showWarningDialog("请选择生成文件路径 !", GlobalDict.TITLE_INFO);
            throw new RuntimeException("请选择生成文件路径 !");
        }
        // 针对Linux系统路径做处理
        savePath = savePath.replace("\\", "/");
        // 保存路径使用相对路径
        String basePath = project.getBasePath();
        if (!StringUtils.isEmpty(basePath) && savePath.startsWith(basePath)) {
            if (savePath.length() > basePath.length()) {
                if ("/".equals(savePath.substring(basePath.length(), basePath.length() + 1))) {
                    savePath = savePath.replace(basePath, ".");
                }
            } else {
                savePath = savePath.replace(basePath, ".");
            }
        }
        // 保存配置
        dbTable.setSavePath(savePath);
        dbTable.setSavePackageName(packageField.getText());
        // 2023/11/30 移除表名前缀功能被自定义文件名功能Covert
//        tableInfo.setPreName(subTableNamePrefixField.getText());
        dbTable.setTemplateGroupName(templateSelectComponent.getSelectedGroupName());
        Module module = getSelectModule();
        AppBaseInfo appBaseInfo = null;
        if (module != null) {
            dbTable.setSaveModelName(module.getName());

            // appBaseInfo
            appBaseInfo = LowCodeAppUtils.queryApplicationCode(project.getName(), module.getName());
            if (Objects.isNull(appBaseInfo) || StringUtils.isEmpty(appBaseInfo.getRootPackage())) {
                // 找不到所属应用, 跳出
                hiddenProgress();
                Messages.showMessageDialog(String.format("模块[%s]未配置，请先在低代码平台完成应用模块配置 !", module.getName()), "生成失败", Icons.scaleToWidth(Icons.FILE_ERROR, 60));
                throw new RuntimeException(String.format("模块[%s]未配置，请先在低代码平台完成应用模块配置 !", module.getName()));
            }
        }

        // 指定业务字段
        List<String> busPriKeyList = busPriKeyMulBox.getSelectedItems();
        if (!busPriKeyList.isEmpty()) {
            List<ColumnInfo> businessColumn = dbTable.getFullColumn().stream().filter(f -> busPriKeyList.contains(f.getObj().getFieldName())).collect(Collectors.toList());
            dbTable.setBusinessColumn(businessColumn);
        } else {
            dbTable.setBusinessColumn(new ArrayList<>());
        }

        // 生成代码
        try {
            // 保存配置
            tableInfoService.saveTableInfo(dbTable);
            GenerateListenerManager.readOnlyStop();
            // 代码生成
            GenerateVelocityOptions options = getGenerateOptions();
            return mySQLGenerateService.generate(appBaseInfo, selectTemplateList, options);
        } finally {
            GenerateListenerManager.readOnlyStart();
        }
    }

    private void saveSelectModule() {
        SelectModule selectModule = SelectModule.builder().moduleName(Objects.requireNonNull(getSelectModule()).getName()).packageName(packageField.getText()).entityPackage(entityPackageField.getText()).dtoPackage(dtoPackageField.getText()).daoPackage(daoPackageField.getText()).mapperPackage(mapperPackageField.getText()).useExample(this.useExample.isSelected()).useAutogen(this.useAutogen.isSelected()).useLombok(this.useLombok.isSelected()).extendMsfEntity(this.extendMsfEntity.isSelected()).useMsfResponse(this.useMsfResponse.isSelected()).checkBoxEnable("true").build();

        if (genController.isSelected()) {
            selectModule.setServicePackage(servicePackageField.getText());
            selectModule.setControllerPackage(controllerPackageField.getText());
        }
        if (genFacade.isSelected()) {
            Object selectedItem = ObjectUtils.defaultIfNull(facadeModuleComboBox.getSelectedItem(), StringUtils.EMPTY);
            selectModule.setFacadeModuleName(selectedItem.toString());
            selectModule.setFacadePackage(facadePackageField.getText());
            selectModule.setFacadeImplPackage(facadeImplPackageField.getText());
        }

        userContext.addSelectModule(selectModule);
        userContextPersistent.loadState(userContext);
    }

    private void saveGenerateOptions() {
        // 保存配置
        Object facadeModuleNameItem = facadeModuleComboBox.getSelectedItem();
        GenerateOptions options = GenerateOptions.builder()
                // module
                .project(project.getName()).moduleName(Objects.requireNonNull(getSelectModule()).getName())
                // dbInfo
                .databaseName(table.getDatabase()).tableName(table.getTableName())
                // 基础选项
                .packageName(packageField.getText()).subTableNamePrefix(subClassNamePrefixField.getText())
                // busPriKeys
                .busPriKeys(busPriKeyMulBox.getSelectedItems())
                // templateGroup
                .templateGroup(templateSelectComponent.getSelectedGroupName())
                // 高级选项
                .entityPackage(entityPackageField.getText()).dtoPackage(dtoPackageField.getText()).daoPackage(daoPackageField.getText()).mapperPackage(mapperPackageField.getText())
                // 动态组件参数
                .otherSettings(otherSettings)
                // 其他选项
                // package
                .genController(genController.isSelected()).servicePackage(servicePackageField.getText()).controllerPackage(controllerPackageField.getText()).genFacade(genFacade.isSelected()).facadeModuleName(Objects.nonNull(facadeModuleNameItem) ? facadeModuleNameItem.toString() : StringUtils.EMPTY).facadePackage(facadePackageField.getText()).facadeImplPackage(facadeImplPackageField.getText())
                // fileName
                .entityName(entityName.getText()).dtoName(dtoName.getText()).daoName(daoName.getText()).mapperName(mapperName.getText()).controllerName(controllerName.getText()).serviceName(serviceName.getText()).facadeName(facadeName.getText()).facadeImplName(facadeImplName.getText())
                // use example
                .useExample(useExample.isSelected())
                // use autogen
                .useAutogen(useAutogen.isSelected())
                // use lombok
                .useLombok(useLombok.isSelected())
                // use dtoParam
                .useDtoParam(useDtoParam.isSelected())
                // extendMsfEntity
                .extendMsfEntity(extendMsfEntity.isSelected())
                // useMsfResponse
                .useMsfResponse(useMsfResponse.isSelected())
                // fileTypeMatchList
                .fileTypeMatches(getFileTypeMatchList())
                // build
                .build();

        GenerateTrackUtils.saveGenerateOptions(options);
    }

    private List<SimpleFileTypeMatch> getFileTypeMatchList() {
        List<ColumnInfo> columns = dbTable.getFullColumn();
        return columns.stream().map(c -> {
            SimpleFileTypeMatch typeMatch = new SimpleFileTypeMatch();
            typeMatch.setField(c.getObj().getFieldName());
            typeMatch.setFieldType(c.getObj().getFieldType());
            typeMatch.setJavaType(c.getType());
            typeMatch.setJavaShortType(c.getShortType());
            typeMatch.setJdbcType(c.getJdbcType());

            return typeMatch;
        }).collect(Collectors.toList());
    }

    /**
     * 初始化方法
     */
    private void initComponentsData() {
        //初始化Module选择
        for (Module module : this.moduleList) {
            if (module.getName().contains(".")) continue;

            moduleComboBox.addItem(module.getName());
            facadeModuleComboBox.addItem(module.getName());
        }

        SelectModule peekModule = userContext.peekModules(moduleList.stream().map(Module::getName).collect(Collectors.toList()));
        if (Objects.nonNull(peekModule)) {
            moduleComboBox.setSelectedItem(peekModule.getModuleName());
            updatePackage(peekModule);
        }

        // 逻辑删除字段
        logicDelComboBox.addItem(NONE_LOGIC_DELETE);
        for (DatabaseColumnNode column : table.getTableColumns()) {
            if (column.getFieldType().contains("bigint") && !column.getFieldName().equals("id")) {
                logicDelComboBox.addItem(column.getFieldName());
                if (column.getFieldName().contains("deleted_at") || column.getFieldName().contains("delete_at")) {
                    logicDelComboBox.setSelectedItem(column.getFieldName());
                }
            }
        }

        entityName.setText(tableClassName + StringUtils.capitalize(userContext.entityAlias));
        dtoName.setText(tableClassName + StringUtils.capitalize(userContext.dtoAlias));
        daoName.setText(tableClassName + StringUtils.capitalize(userContext.daoAlias));
        mapperName.setText(tableClassName + StringUtils.capitalize(userContext.mapperAlias));
        serviceName.setText(tableClassName + StringUtils.capitalize(userContext.serviceAlias));
        controllerName.setText(tableClassName + StringUtils.capitalize(userContext.controllerAlias));
        facadeName.setText(tableClassName + StringUtils.capitalize(userContext.facadeAlias));
        facadeImplName.setText(tableClassName + StringUtils.capitalize(userContext.facadeAlias) + "Impl");
    }

    /**
     * 获取生成选项
     *
     * @return {@link GenerateVelocityOptions}
     */
    private GenerateVelocityOptions getGenerateOptions() {
        GenerateVelocityOptions options = GenerateVelocityOptions.builder().reFormat(reFormatCheckBox.isSelected()).titleSure(titleSureCheckBox.isSelected()).titleRefuse(titleRefuseCheckBox.isSelected()).unifiedConfig(unifiedConfigCheckBox.isSelected()).deletedAt(deletedAtCheckBox.isSelected()).build();
        // use example
        options.setUseExample(this.useExample.isSelected() && templateSelectComponent.isDefaultGroup());
        // use lombok
        options.setUseLombok(this.useLombok.isSelected());
        options.setUseDtoParam(this.useDtoParam.isSelected());
        // use autogen
        boolean useAutogen = this.useAutogen.isSelected() && templateSelectComponent.isDefaultGroup();
        options.setUnUseAutogen(!useAutogen);
        // otherSettings
        options.setOtherSetting(otherSettings);
        // logicDel
        String logicDelVal = logicDelComboBox.getItem();
        if (!NONE_LOGIC_DELETE.equals(logicDelVal)) {
            options.setDeletedAt(true);
            options.setDeletedAtField(logicDelVal);
        } else {
            options.setDeletedAt(false);
        }

        // 包路径信息
        String entityPackage = entityPackageField.getText();
        String dtoPackage = dtoPackageField.getText();
        String daoPackage = daoPackageField.getText();
        String mapperPackage = mapperPackageField.getText();
        String controllerPackage = controllerPackageField.getText();
        String servicePackage = servicePackageField.getText();
        String facadePackage = facadePackageField.getText();
        String facadeImplPackage = facadeImplPackageField.getText();
        String facadeModuleName = (String) facadeModuleComboBox.getSelectedItem();
        Module facadeModule = getModule(facadeModuleName);
        // fileName
        options.setExtendMsfEntity(extendMsfEntity.isSelected());
        options.setEntityName(entityName.getText());
        options.setSimpleEntityName(simpleEntityName(entityName.getText()));
        options.setDtoName(dtoName.getText());
        options.setDaoName(daoName.getText());
        options.setMapperName(mapperName.getText());
        options.setControllerName(controllerName.getText());
        options.setServiceName(serviceName.getText());
        options.setFacadeName(facadeName.getText());
        options.setFacadeImplName(facadeImplName.getText());
        // extName
        options.setExtDaoName(GenerateTrackUtils.convertExtName(useAutogen, daoName.getText()));
        options.setExtServiceName(GenerateTrackUtils.convertExtName(useAutogen, serviceName.getText()));
        options.setExtMapperName(GenerateTrackUtils.convertExtName(useAutogen, mapperName.getText()));

        options.setEntityPackage(entityPackage);
        options.setDtoPackage(dtoPackage);
        options.setDaoPackage(daoPackage);
        options.setMapperPackage(mapperPackage);

        options.setUseMsfResponse(useMsfResponse.isSelected());
        options.setControllerPackage(controllerPackage);
        options.setServicePackage(servicePackage);
        options.setFacadePackage(facadePackage);
        // 自适应facadePackage路径生成Domain路径
        options.setFacadeDomainPackage(convertFacadeDomainPackage(facadePackage));
        options.setFacadeImplPackage(facadeImplPackage);

        options.setModuleJavaPath(getBasePath());
        options.setFacadeModuleJavaPath(getBasePath(facadeModule));
        options.setGenService(this.genService.isSelected());
        options.setGenController(this.genController.isSelected());
        options.setGenFacade(this.genFacade.isSelected());

        // path
        options.setModuleBasePath(getBasePath());

        log.info("GenerateCodeDialog getGenerateOptions: {}", JSON.toJson(options));
        return options;
    }

    private String simpleEntityName(String entityName) {
        String suffix = "Entity";
        if (entityName.endsWith(suffix)) {
            return org.apache.commons.lang3.StringUtils.substringBefore(entityName, suffix);
        }
        return entityName;
    }

    private String convertFacadeDomainPackage(String facadePackage) {
        // 自适应facadePackage路径生成Domain路径
        if (StringUtils.isEmpty(facadePackage)) return StringUtils.EMPTY;

        if (facadePackage.endsWith(".facade")) {
            return facadePackage.substring(0, facadePackage.lastIndexOf(".facade")) + ".domain";
        }

        return facadePackage + ".domain";
    }

    /**
     * 获取选中的Module
     *
     * @return 选中的Module
     */
    public Module getSelectModule() {
        String name = (String) moduleComboBox.getSelectedItem();
        if (StringUtils.isEmpty(name)) {
            return null;
        }

        return ModuleManager.getInstance(project).findModuleByName(name);
    }

    /**
     * 获取选中的Module
     *
     * @return 选中的Module
     */
    private Module getModule(String name) {
        return ModuleManager.getInstance(project).findModuleByName(name);
    }

    /**
     * 获取基本路径
     *
     * @return 基本路径
     */
    private String getBasePath() {
        return getBasePath(getSelectModule());
    }

    private String getBasePath(Module module) {
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

    private void refreshPath(boolean init) {
        String packageName = packageField.getText();
        // 获取基本路径
        String path = getBasePath();
        // 兼容Linux路径
        path = path.replace("\\", "/");
        // 如果存在包路径，添加包路径
        if (!StringUtils.isEmpty(packageName)) {
            path += "/" + packageName.replace(".", "/");
        }
        pathField.setText(path);

        autogenField.setText("");
        extensionField.setText("");
        Module module = getSelectModule();
        if (Objects.nonNull(module)) {
            AppBaseInfo appBaseInfo = LowCodeAppUtils.queryApplicationCode(ProjectUtils.getCurrProjectName(), module.getName());
            String rootPackage = appBaseInfo.getRootPackage();
            if (StringUtils.isEmpty(rootPackage)) {
                moduleTipLabel.setVisible(true);
            } else {
                // autogenPath
                autogenField.setText(appBaseInfo.getAutogenPackage());
                // extensionPath
                extensionField.setText(appBaseInfo.getExtensionPackage());
                moduleTipLabel.setVisible(false);
            }

            refreshOkActionStatus();
        }

        enabledDialogSelector(!StringUtils.isEmpty(packageName));
    }

    /**
     * 刷新目录
     */
    private void refreshPath() {
        refreshPath(false);
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        globalPanel = new JPanel(new BorderLayout());
        baseSetting = new JPanel(new VFlowLayout(VFlowLayout.TOP));
        // 支持检索
        moduleComboBox = new ComboBox<>();
        moduleComboBox.setSwingPopup(false);
        // 逻辑删除字段
        logicDelComboBox = new ComboBox<>();
        logicDelComboBox.setRenderer(logicDeletedRenderer());
        logicDelComboBox.setSwingPopup(false);
        // 支持检索
        facadeModuleComboBox = new ComboBox<>();
        facadeModuleComboBox.setSwingPopup(false);

        packageField = new JTextField();
        entityPackageField = new JTextField();
        daoPackageField = new JTextField();
        dtoPackageField = new JTextField();
        JTextField serviceDtoPackageField = new JTextField();
        mapperPackageField = new JTextField();
        servicePackageField = new JTextField();
        controllerPackageField = new JTextField();
        facadePackageField = new JTextField();
        facadeImplPackageField = new JTextField();

        packageChooseButton = new JButton();
        pathField = new JTextField();
        pathChooseButton = new JButton();
        subClassNamePrefixField = new JTextField();

        // 业务字段
        busPriKeyMulBox = new MultiComboBox();

        unifiedConfigCheckBox = new JCheckBox();
        titleSureCheckBox = new JCheckBox();
        reFormatCheckBox = new JCheckBox();
        deletedAtCheckBox = new JCheckBox();
        templateLabel = new JLabel();

        // 初始化模板组
        templateSelectComponent = new TemplateSelectComponent(e -> refreshOkActionStatus(), true, Constants.DataSource.MySQL);
        templatePanel = this.templateSelectComponent.getMainPanel();
        titleRefuseCheckBox = new JCheckBox();

        moduleTipLabel = new JLabel();

        autogenField = new JTextField();
        extensionField = new JTextField();

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);// 设置显示提示信息
        progressBar.setIndeterminate(true);// 设置采用不确定进度条
        progressBar.setString("生成代码中......");// 设置提示信息

        // title
        JPanel titlePanel = new JPanel(new FlowLayout());
        {
            JLabel title = new JLabel();
            title.setFont(new Font("微软雅黑", Font.PLAIN, 16));
            title.setIcon(Icons.scaleToWidth(Icons.TABLE_GEN_CODE, 16));
            String stepTxt = this.step > 0? String.format(" Step.%s", step) : org.apache.commons.lang3.StringUtils.EMPTY;
            String titleTxt = " 生成代码";
            if(Objects.nonNull(table)) titleTxt = " " + table.getDatabase() + "." + table.getTableName() + titleTxt;
            title.setText(stepTxt + titleTxt);

            titlePanel.add(title);
        }

        // baseSetting基础选项
        {
            baseSettingPanel = new JBTabbedPane();
            baseSettingPanel.setUI(new CustomHeightTabbedPaneUI());
            baseSetting.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
            baseSettingPanel.addTab("基础选项", Icons.scaleToWidth(Icons.SETTING, 16), baseSetting);

            // 模块
            // 包路径
            JPanel packagePanel = JPanelUtils.settingPanel("包路径", packageField, new Dimension(60, -1), valueSize);
            packagePanel.setBorder(BorderFactory.createEmptyBorder(0, 60, 0, 0));
            moduleComboBox.setPreferredSize(new Dimension(valueSize.width/2, -1));
            JPanel panel = JPanelUtils.combinePanel("模块", moduleComboBox, packagePanel, null, labelSize, valueSize);
            baseSetting.add(panel);

            moduleTipLabel.setText("模块未配置, 请先在低代码平台完成应用模块配置");
            moduleTipLabel.setForeground(JBColor.RED);
            moduleTipLabel.setVisible(false);
            moduleTipLabel.setIcon(Icons.scaleToWidth(Icons.SYS_ERROR2, 13));
            baseSetting.add(moduleTipLabel);

            // 移除表名前缀
            baseSetting.add(JPanelUtils.settingPanel("移除类名前缀", subClassNamePrefixField, labelSize, valueSize));

            // 使用业务主键
            String busPriKeyTips = "选择业务字段后, 将会根据业务字段生成CRUD方法";
            JPanel busPriKeyPanel = JPanelUtils.settingPanel("使用业务主键", busPriKeyMulBox, labelSize, valueSize);
            busPriKeyMulBox.setToolTipText(busPriKeyTips);
            busPriKeyPanel.add(JPanelUtils.tips(busPriKeyTips, 13), BorderLayout.EAST);
            baseSetting.add(busPriKeyPanel);

            // 逻辑删除字段
            String logicDelTips = "仅支持 bigint 类型字段作为逻辑删除标识字段";
            JPanel logicDelPanel = JPanelUtils.settingPanel("逻辑删除字段", logicDelComboBox, labelSize, valueSize);
            logicDelComboBox.setToolTipText(logicDelTips);
            logicDelPanel.add(JPanelUtils.tips(logicDelTips, 13), BorderLayout.EAST);
            baseSetting.add(logicDelPanel);

            // 部分默认设置
            defaultSetting();

            //---- template ----
            // 使用Lombok
            useLombok = new JBCheckBox("使用Lombok");
            useLombok.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
            // 使用Example
            useExample = new JBCheckBox("使用Example");
            useExample.setToolTipText("需要 lowcode-client version >= 1.0.6");
            useExample.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
            // use autogen
            useAutogen = new JBCheckBox("autogen风格");
            useAutogen.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
            useAutogen.setSelected(true);
            // checkBoxPanel
            JPanel checkBoxPanel = new JPanel(new GridLayout(-1, 3));
            checkBoxPanel.add(useLombok);
            checkBoxPanel.add(useExample);
            checkBoxPanel.add(useAutogen);
            baseSetting.add(JPanelUtils.combinePanel("ORM框架", null, templatePanel, checkBoxPanel, labelSize, valueSize));
        }

        // 字段类型配置
        {
            typeMatchPanel = new JPanel(new BorderLayout());
            typeMatchTips = JPanelUtils.tips(Icons.WARNING_COLOR);
            typeMatchTips.setVisible(false);
            typeMatchTips.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 0));
            typeMatchPanel.add(typeMatchTips, BorderLayout.NORTH);

            typeMatchTableWrap = new TypeMatchTableWrap(project, dbTable);
            typeMatchTableScroll = new JBScrollPane();
            typeMatchPanel.add(typeMatchTableScroll, BorderLayout.CENTER);

            baseSettingPanel.addTab("字段类型配置", Icons.scaleToWidth(Icons.COLUMN, 16), typeMatchPanel);
        }

        // 高级选项
        {
            advancedSettingPanel = new JPanel(new VFlowLayout(VFlowLayout.TOP));
            advancedSettingPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));

            // entity包路径
            entityName = new JTextField();
            JPanel entityNamePanel = JPanelUtils.settingPanel("entityName  ", entityName, nameLabelWidth80, nameWidth);
            entityNamePanel.setBorder(BorderFactory.createEmptyBorder(0, 60, 0, 0));
            JPanel entityPackageFieldPanel = new JPanel(new BorderLayout());
            entityPackageFieldPanel.add(entityPackageField, BorderLayout.CENTER);
            extendMsfEntity = new JBCheckBox("继承msf.BaseEntity");
            entityPackageFieldPanel.add(extendMsfEntity, BorderLayout.EAST);
            advancedSettingPanel.add(JPanelUtils.combinePanel("entity包路径", null, entityPackageFieldPanel, entityNamePanel, labelSize, valueSize));

            // dto包路径
            dtoName = new JTextField();
            JPanel dtoNamePanel = JPanelUtils.settingPanel("dtoName  ", dtoName, nameLabelWidth80, nameWidth);
            dtoNamePanel.setBorder(BorderFactory.createEmptyBorder(0, 60, 0, 0));
            dtoPackagePanel = JPanelUtils.combinePanel("dto包路径", null, dtoPackageField, dtoNamePanel, labelSize, valueSize);
            advancedSettingPanel.add(dtoPackagePanel);

            // dao包路径
            daoName = new JTextField();
            JPanel daoNamePanel = JPanelUtils.settingPanel("daoName  ", daoName, nameLabelWidth80, nameWidth);
            daoNamePanel.setBorder(BorderFactory.createEmptyBorder(0, 60, 0, 0));
            advancedSettingPanel.add(JPanelUtils.combinePanel("dao包路径", null, daoPackageField, daoNamePanel, labelSize, valueSize));
            // mapper包路径
            mapperName = new JTextField();
            JPanel mapperNamePanel = JPanelUtils.settingPanel("mapperName  ", mapperName, nameLabelWidth80, nameWidth);
            mapperNamePanel.setBorder(BorderFactory.createEmptyBorder(0, 60, 0, 0));
            mapperPanel = JPanelUtils.combinePanel("mapper.xml路径", null, mapperPackageField, mapperNamePanel, labelSize, valueSize);
            advancedSettingPanel.add(mapperPanel);

            // 分组高级设置
            String selectedGroupName = templateSelectComponent.getSelectedGroupName();
            cusPanelGroupMap.getOrDefault(selectedGroupName, new ArrayList<>()).forEach(advancedSettingPanel::add);

            // 动态设置
            cusSettingPanel = new JPanel(new VFlowLayout(VFlowLayout.TOP));
            cusSettingPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
            advancedSettingPanel.add(cusSettingPanel);

            // 进度条
            progressBar.setVisible(false);
            advancedSettingPanel.add(progressBar);
        }

        // 其他选项
        {
            otherSettingPanel = new JPanel(new VFlowLayout(VFlowLayout.TOP));
            otherSettingPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

            // 生成 service
            genService = new JBCheckBox("生成 " + userContext.serviceAlias);
            genService.setBorder(BorderFactory.createEmptyBorder(0, 50, 0, 50));
            btnPanel.add(genService);

            // 生成 Controller
            genController = new JBCheckBox("生成 " + userContext.controllerAlias);
            genController.setBorder(BorderFactory.createEmptyBorder(0, 50, 0, 50));
            btnPanel.add(genController);

            //生成 Facade
            genFacade = new JBCheckBox("生成 " + userContext.facadeAlias);
            genFacade.setBorder(BorderFactory.createEmptyBorder(0, 50, 0, 50));
            btnPanel.add(genFacade);
            btnPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
            otherSettingPanel.add(btnPanel);

            // dto2包路径
            // dto包路径
            JTextField serviceDtoName = new JTextField();
            serviceDtoName.setDocument(dtoName.getDocument());
            serviceDtoPackageField.setDocument(dtoPackageField.getDocument());
            JPanel serviceDtoNamePanel = JPanelUtils.settingPanel("dtoName  ", serviceDtoName, nameLabelWidth, nameWidth);
            serviceDtoNamePanel.setBorder(BorderFactory.createEmptyBorder(0, 40, 0, 0));
            serviceDtoPackagePanel = JPanelUtils.combinePanel("dto包路径", null, serviceDtoPackageField, serviceDtoNamePanel, nameLabelWidth, valueSize);
            serviceDtoPackagePanel.setVisible(false);
            otherSettingPanel.add(serviceDtoPackagePanel);

            // service包路径
            serviceName = new JTextField();
            JPanel serviceNamePanel = JPanelUtils.settingPanel("serviceName  ", serviceName, nameLabelWidth, nameWidth);
            serviceNamePanel.setBorder(BorderFactory.createEmptyBorder(0, 40, 0, 0));

            JPanel servicePackagePanel = new JPanel(new BorderLayout());
            servicePackagePanel.add(servicePackageField, BorderLayout.CENTER);
            useDtoParam = new JBCheckBox("dto作为出入参");
            servicePackagePanel.add(useDtoParam, BorderLayout.EAST);
            servicePackage = JPanelUtils.combinePanel("service包路径", null, servicePackagePanel, serviceNamePanel, nameLabelWidth, valueSize);
            servicePackage.setVisible(false);
            otherSettingPanel.add(servicePackage);

            // controller包路径
            controllerName = new JTextField();
            JPanel controllerNamePanel = JPanelUtils.settingPanel("controllerName  ", controllerName, nameLabelWidth, nameWidth);
            controllerNamePanel.setBorder(BorderFactory.createEmptyBorder(0, 40, 0, 0));

            JPanel controllerPackagePanel = new JPanel(new BorderLayout());
            controllerPackagePanel.add(controllerPackageField, BorderLayout.CENTER);
            useMsfResponse = new JBCheckBox("msf.Response");
            controllerPackagePanel.add(useMsfResponse, BorderLayout.EAST);
            controllerPackage = JPanelUtils.combinePanel("controller包路径", null, controllerPackagePanel, controllerNamePanel, nameLabelWidth, valueSize);
            controllerPackage.setVisible(false);
            otherSettingPanel.add(controllerPackage);

            // Facade包路径
            facadeName = new JTextField();
            JPanel facadeNamePanel = JPanelUtils.settingPanel("facadeName  ", facadeName, nameLabelWidth, nameWidth);
            facadeNamePanel.setBorder(BorderFactory.createEmptyBorder(0, 40, 0, 0));
            JPanel facadePanel = new JPanel(new BorderLayout());
            String facadeTips = "指定模块下的facadeService包路径, 需要自行配置dubbo-provider-service.xml或添加Dubbo的@Service完成服务注册";
            facadePackageField.setToolTipText(facadeTips);
            facadePanel.add(facadePackageField, BorderLayout.CENTER);
            facadePanel.add(facadeModuleComboBox, BorderLayout.WEST);
            facadePanel.add(JPanelUtils.tips(facadeTips, 13), BorderLayout.EAST);
            facadePackage = JPanelUtils.combinePanel("facade包路径", null, facadePanel, facadeNamePanel, nameLabelWidth, valueSize);
            facadePackage.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
            facadePackage.setVisible(false);
            otherSettingPanel.add(facadePackage);

            // facadeImpl包路径
            facadeImplName = new JTextField();
            JPanel facadeImplNamePanel = JPanelUtils.settingPanel("facadeImplName  ", facadeImplName, nameLabelWidth, nameWidth);
            facadeImplNamePanel.setBorder(BorderFactory.createEmptyBorder(0, 40, 0, 0));
            facadeImplPackage = JPanelUtils.combinePanel("facadeImpl包路径", null, facadeImplPackageField, JPanelUtils.tips("基础选项模块下的facadeImpl包路径", 13), nameLabelWidth, valueSize);
            facadeImplPackageField.setToolTipText("基础选项模块下的facadeImpl包路径");
            facadeImplPackage.add(facadeImplNamePanel, BorderLayout.EAST);
            facadeImplPackage.setVisible(false);
            otherSettingPanel.add(facadeImplPackage);

            // 进度条
            otherSettingPanel.add(progressBar);
        }

        // 面板配置
        {
            // Tab
            bottomSettingTab = new JBTabbedPane();
            bottomSettingTab.setUI(new CustomHeightTabbedPaneUI());
            bottomSettingTab.addTab("高级选项", Icons.scaleToWidth(Icons.FOLDER_SETTING, 16), advancedSettingPanel);
            bottomSettingTab.addTab("其他选项", Icons.scaleToWidth(Icons.SETTING, 16), otherSettingPanel);
            bottomSettingTab.setPreferredSize(new Dimension(-1, 300));

            // add to globalPanel
            titlePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            globalPanel.add(titlePanel, BorderLayout.NORTH);

            baseSettingPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
            globalPanel.add(baseSettingPanel, BorderLayout.CENTER);

            bottomSettingTab.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
            globalPanel.add(bottomSettingTab, BorderLayout.SOUTH);
        }
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    private ListCellRenderer<? super String> logicDeletedRenderer() {
        Font font = new Font("微软雅黑", Font.BOLD, 12);
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component renderer = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (renderer instanceof JLabel && NONE_LOGIC_DELETE.equals(value)) {
                    JPanel panel = new JPanel(new BorderLayout());
                    JLabel label = new JLabel(value.toString());
                    label.setForeground(JBColor.GRAY);
                    panel.add(label);

                    if (isSelected) panel.setBackground(RoundedLabel.SELECTED);
                    return panel;
                }

                return renderer;
            }
        };
    }

    private void defaultSetting() {
        //---- reFormatCheckBox ----
        // 代码格式化
        deletedAtCheckBox.setText("使用软删除");
        deletedAtCheckBox.setSelected(true);
        deletedAtCheckBox.setVisible(false);
        // 代码格式化
        reFormatCheckBox.setText("代码格式化");
        reFormatCheckBox.setSelected(true);
        reFormatCheckBox.setVisible(false);

        //---- unifiedConfigCheckBox ----
        // 统一配置
        unifiedConfigCheckBox.setText("统一配置");
        unifiedConfigCheckBox.setVisible(false);

        //---- titleSureCheckBox ----
        // 弹框选是
        titleSureCheckBox.setText("同名文件自动覆盖");
        titleSureCheckBox.setSelected(true);
        titleSureCheckBox.setVisible(false);

        //---- titleRefuseCheckBox ----
        // 弹框选否
        titleRefuseCheckBox.setText("弹框选否");
        titleRefuseCheckBox.setVisible(false);
    }

    private Map<String, List<JPanel>> createCusPanels() {
        Map<String, List<JPanel>> groupSettingPanels = new HashMap<>();

        Map<String, List<CusPanel>> dbGenerateOtherSettingPanels = GenerateTrackUtils.getDbGenerateOtherSettingPanels();
        dbGenerateOtherSettingPanels.forEach((group, otherSettingPanels) -> {
            List<JPanel> panels = new ArrayList<>();
            groupSettingPanels.put(group, panels);

            if (CollectionUtils.isNotEmpty(otherSettingPanels)) {
                for (CusPanel cusPanel : otherSettingPanels) {
                    List<Component> components = new ArrayList<>();
                    JPanel panel = new JPanel(new BorderLayout());
                    panels.add(panel);

                    // 标题
                    JLabel label = new JLabel(cusPanel.getLabelTxt());
                    label.setPreferredSize(labelWidthSize);
                    panel.add(label, BorderLayout.WEST);
                    // 内容
                    JPanel valuePanel = new JPanel(new BorderLayout());
                    panel.add(valuePanel, BorderLayout.CENTER);
                    switch (cusPanel.getComponentType()) {
                        case LABEL:
                            for (String value : cusPanel.getValues()) {
                                JLabel valLabel = new JLabel(value);
                                valLabel.setPreferredSize(valueSize);

                                components.add(valLabel);
                                valuePanel.add(valLabel);
                            }
                            break;
                        case TEXT_FIELD:
                            for (String value : cusPanel.getValues()) {
                                JTextField textField = new JTextField(value);
                                textField.setPreferredSize(valueSize);
                                textField.addActionListener(e -> {
                                    otherSettings.put(cusPanel.getLabelKey(), Sets.newHashSet(textField.getText()));
                                });

                                components.add(textField);
                                valuePanel.add(textField);
                                otherSettings.put(cusPanel.getLabelKey(), Sets.newHashSet(textField.getText()));
                            }
                            break;
                        case COMBO_BOX:
                            ComboBox<String> comboBox = new ComboBox<>();
                            comboBox.setPreferredSize(valueSize);
                            comboBox.addActionListener(e -> {
                                otherSettings.put(cusPanel.getLabelKey(), Sets.newHashSet((String) comboBox.getSelectedItem()));
                            });

                            for (String value : cusPanel.getValues()) {
                                comboBox.addItem(value);
                            }

                            components.add(comboBox);
                            valuePanel.add(comboBox);
                            otherSettings.put(cusPanel.getLabelKey(), Sets.newHashSet((String) comboBox.getSelectedItem()));
                            break;
                        case CHECK_BOX:
                            valuePanel.setLayout(new GridLayout(-1, 3));
                            for (String value : cusPanel.getValues()) {
                                JBCheckBox checkBox = getOtherSettingCheckBox(cusPanel, value);
                                if (checkBox.isSelected()) {
                                    Set<String> checkValues = otherSettings.getOrDefault(cusPanel.getLabelKey(), new HashSet<>());
                                    checkValues.add(value);
                                    otherSettings.put(cusPanel.getLabelKey(), checkValues);
                                }

                                components.add(checkBox);
                                valuePanel.add(checkBox);
                            }

                            break;
                    }

                    cusComponentGroupMap.put(cusPanel.getLabelKey(), components);
                }
            }
        });

        return groupSettingPanels;
    }

    @NotNull
    private JBCheckBox getOtherSettingCheckBox(CusPanel cusPanel, String value) {
        JBCheckBox checkBox = new JBCheckBox(value);
        checkBox.addActionListener(e -> {
            checkBoxAction(cusPanel.getLabelKey(), value, checkBox);
        });

        // 默认选中
        checkBox.setSelected(true);
        return checkBox;
    }

    private void checkBoxAction(String key, String value, JBCheckBox checkBox) {
        if (checkBox.isSelected()) {
            if (!otherSettings.containsKey(key)) {
                otherSettings.put(key, new HashSet<>());
            }

            Set<String> checkValues = otherSettings.getOrDefault(key, new HashSet<>());
            checkValues.add(value);
            otherSettings.put(key, checkValues);
        } else {
            Set<String> checkValues = otherSettings.getOrDefault(key, new HashSet<>());
            checkValues.remove(value);
            otherSettings.put(key, checkValues);
        }
    }


    private void showProgress() {
        progressBar.setVisible(true);
    }

    private void hiddenProgress() {
        progressBar.setVisible(false);
    }

    private void refreshOkActionStatus() {
        boolean can = !this.moduleTipLabel.isVisible();

        // template checkBox
        List<Template> templates = this.templateSelectComponent.getAllSelectedTemplate();
        if (CollectionUtils.isNotEmpty(templates)) {
            this.templateLabel.setText("  ORM框架");
            this.templateLabel.setForeground(null);
            this.templateLabel.setIcon(null);
        } else {
            can = false;
            this.templateLabel.setText("ORM框架");
            this.templateLabel.setForeground(JBColor.RED);
            this.templateLabel.setIcon(Icons.scaleToWidth(Icons.SYS_ERROR2, 13));
        }

        this.setOKActionEnabled(can);
    }

    private void enabledDialogSelector(boolean enable) {
        this.setOKActionEnabled(enable);
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * 主面板
     */
    private JPanel globalPanel;
    private JBTabbedPane baseSettingPanel;
    /**
     * 主面板
     */
    private JPanel baseSetting;
    private JPanel advancedSettingPanel;
    private JPanel otherSettingPanel;
    private JBTabbedPane bottomSettingTab;
    /**
     * 模型下拉框
     */
    private ComboBox<String> moduleComboBox;
    private ComboBox<String> logicDelComboBox;
    private JLabel moduleTipLabel;
    private JLabel templateLabel;
    /**
     * 包字段
     */
    private JTextField packageField;
    private JTextField entityPackageField;
    private JTextField daoPackageField;
    private JTextField dtoPackageField;
    private JTextField mapperPackageField;
    private JTextField servicePackageField;
    private JTextField controllerPackageField;
    private JTextField facadePackageField;
    private JTextField facadeImplPackageField;
    private ComboBox<String> facadeModuleComboBox;
    /**
     * 路径字段
     */
    private JTextField pathField;

    private JTextField autogenField;
    private JTextField extensionField;
    private JBCheckBox genService;
    private JBCheckBox genController;
    private JBCheckBox genFacade;
    private JTextField entityName;
    private JTextField dtoName;
    private JTextField daoName;
    private JTextField mapperName;
    private JPanel mapperPanel;
    private JTextField serviceName;
    private JTextField controllerName;
    private JTextField facadeName;
    private JTextField facadeImplName;

    /**
     * 前缀字段
     */
    private JTextField subClassNamePrefixField;

    /**
     * 业务主键复选框
     */
    private MultiComboBox busPriKeyMulBox;
    /**
     * 包选择按钮
     */
    private JButton packageChooseButton;
    /**
     * 路径选择按钮
     */
    private JButton pathChooseButton;
    /**
     * 模板面板
     */
    private JPanel templatePanel;

    /**
     * 统一配置复选框
     */
    private JCheckBox unifiedConfigCheckBox;
    /**
     * 弹框选是复选框
     */
    private JCheckBox titleSureCheckBox;
    /**
     * 格式化代码复选框
     */
    private JCheckBox reFormatCheckBox;
    /**
     * 支持软删除复选框
     */
    private JCheckBox deletedAtCheckBox;
    /**
     * 弹框全否复选框
     */
    private JCheckBox titleRefuseCheckBox;
    private JProgressBar progressBar;

    private JPanel serviceDtoPackagePanel;
    private JPanel servicePackage;
    private JPanel controllerPackage;
    private JPanel facadePackage;
    private JPanel facadeImplPackage;

    //------------------------------------------------------------------------------------------------------------------
    /**
     * 表信息服务
     */
    private TableInfoSettingsService tableInfoService;
    /**
     * 代码生成服务
     */
    private MySQLGenerateService mySQLGenerateService;
    /**
     * 当前项目中的module
     */
    private List<Module> moduleList;
    private JCheckBox useLombok;
    private JCheckBox useExample;
    private JCheckBox useAutogen;
    private JCheckBox extendMsfEntity;
    private JCheckBox useMsfResponse;
    private JCheckBox useDtoParam;
    private int step;
    private String tableClassName;
    private JPanel typeMatchPanel;
    private JBScrollPane typeMatchTableScroll;
    private JTextArea typeMatchTips;
    private JPanel dtoPackagePanel;
    private TypeMatchTableWrap typeMatchTableWrap;
    /**
     * 模板选择组件
     */
    private TemplateSelectComponent templateSelectComponent;

    private final OtherSettingMap<String, Set<String>> otherSettings = new OtherSettingMap<>();
    private static final Dimension valueSize = new Dimension(500, 30);
    private static final Dimension labelSize = new Dimension(100, 30);
    private static final Dimension labelWidthSize = new Dimension(100, -1);
    private static final Dimension nameWidth = new Dimension(250, -1);
    private static final Dimension nameLabelWidth = new Dimension(110, -1);
    private static final Dimension nameLabelWidth80 = new Dimension(90, -1);
    private static final String NONE_LOGIC_DELETE = "<不使用逻辑删除>                仅支持<bigint>类型字段作为逻辑删除标识字段";

    private Map<String, GenerateOptions> moduleRemoteOptions;
    private Map<String, List<JPanel>> cusPanelGroupMap;
    private JPanel cusSettingPanel;
    private Map<String, List<Component>> cusComponentGroupMap;
    private MySQLTableNode table;
    private TableInfo dbTable;
    private Runnable doAfterOk = null;
    private static final String fieldTypeMathTips;

    static {
        fieldTypeMathTips = String.format("部分字段未找到匹配的Java字段类型, 已配置为默认类型 %s, 请检查并按需修改", String.join(",  ", FieldTypeMatch.WARNING_TYPE));
    }

    private void actionPerformed(ActionEvent e) {
        templateGroupAction();
    }
}
