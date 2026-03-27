package com.qihoo.finance.lowcode.design.dialog;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.UIUtil;
import com.qihoo.finance.lowcode.common.configuration.UserContextPersistent;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.ui.base.VFlowLayout;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.JPanelUtils;
import com.qihoo.finance.lowcode.common.util.JTreeToolbarUtils;
import com.qihoo.finance.lowcode.common.util.SwingWorkerUtils;
import com.qihoo.finance.lowcode.design.dto.ExportDDLDTO;
import com.qihoo.finance.lowcode.design.entity.DatabaseNode;
import com.qihoo.finance.lowcode.design.util.DatabaseDesignUtils;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import com.qihoo.finance.lowcode.gentracker.tool.ModuleUtils;
import com.qihoo.finance.lowcode.gentracker.util.IoUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * DatabaseExportDDL
 *
 * @author fengjinfu-jk
 * date 2023/12/28
 * @version 1.0.0
 * @apiNote DatabaseExportDDL
 */
@Slf4j
public class DatabaseExportDDL extends DialogWrapper {
    protected static final UserContextPersistent userContextPersistent;
    protected static final UserContextPersistent.UserContext userContext;
    private String exportPathStr;
    private final Project project;
    private final String databaseName;
    private final List<String> moduleList;
    private final List<ExportDDLDTO> ddlList;
    private CheckBoxList<ExportDDLDTO> checkBoxList;

    static {
        userContextPersistent = ApplicationManager.getApplication().getService(UserContextPersistent.class);
        userContext = userContextPersistent.getState();
    }

    public DatabaseExportDDL(@NotNull Project project, DatabaseNode selectDatabase, List<ExportDDLDTO> ddlList) {
        super(project);
        this.project = project;
        this.ddlList = ddlList;
        this.databaseName = selectDatabase.getName();
        this.moduleList = Arrays.stream(ModuleManager.getInstance(project).getModules()).map(Module::getName).filter(name -> !name.contains(".")).collect(Collectors.toList());
        initComponents();
        initData();
        initSize();
        log.info(Constants.Log.USER_ACTION, "用户打开DatabaseExportDDL界面");
    }

    @Override
    protected void doOKAction() {
        this.showExportTableArea.setForeground(null);
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setStringPainted(true);
        progressBar.setIndeterminate(false);
        progressBar.setString(null);
        progressBar.setValue(0);
        progressBar.setVisible(true);

        List<ExportDDLDTO> exportDDLs = new ArrayList<>();
        for (int i = 0; i < checkBoxList.getItemsCount(); i++) {
            if (checkBoxList.isItemSelected(i)) {
                exportDDLs.add(checkBoxList.getItemAt(i));
            }
        }
        dialogComponentStatus(false);
        String filePath = getSelectModulePath() + "/" + this.exportPath.getText();
        SwingWorkerUtils.execute(() -> {
            StringBuilder ddlBuilder = new StringBuilder("-- ").append(databaseName).append("\n\n");
            this.showExportTableArea.append("\n----------------------------------------------------------------------------------------------------");
            this.showExportTableArea.append("\n[DDL] Export task start...");
            this.showExportTableArea.append(String.format("\n[DDL] %s DDL scripts will be exported.\n", exportDDLs.size()));
            double max = 100D;
            double eachStep = max / exportDDLs.size();
            double step = 0D;
            int count = 1;
            for (ExportDDLDTO export : exportDDLs) {
                // ddl
                String ddl = DatabaseDesignUtils.queryTableCreateDDL(export.getDatabase(), export.getTable());
                ddlBuilder.append("-- ").append(export.getTable().getTableName()).append("\n").append(ddl).append("\n\n");
                // updateShowTableArea
                this.showExportTableArea.append(String.format("\n[DDL] Exporting %s> %s.%s", count,
                        export.getDatabase().getName(), export.getTable().toString()));
                this.tableAreaScroll.getVerticalScrollBar().setValue(tableAreaScroll.getVerticalScrollBar().getMaximum());

                step += eachStep;
                if (step > 1) {
                    progressBar.setValue(Math.min(progressBar.getValue() + (int) step, 99));
                    step -= (int) step;
                }

                count++;
            }

            this.showExportTableArea.setForeground(JBColor.GREEN);
            this.showExportTableArea.append(String.format("\n\n\n[DDL] %s DDL scripts was exported.", count - 1));
            this.showExportTableArea.append("\n[DDL] Export file path: ");
            this.showExportTableArea.append(filePath);
            this.showExportTableArea.append("\n\n[DDL] Export success !");
            this.showExportTableArea.append("\n----------------------------------------------------------------------------------------------------");
            return ddlBuilder.toString();
        }, ddlStr -> {
            // 生成文件
            this.tableAreaScroll.getVerticalScrollBar().setValue(tableAreaScroll.getVerticalScrollBar().getMaximum());
            progressBar.setValue(100);
            IoUtils.writeFile(filePath, ddlStr, true);

            // 定位文件
            userContext.exportDDLModule = (String) ObjectUtils.defaultIfNull(moduleComboBox.getSelectedItem(), StringUtils.EMPTY);
            Module module = ModuleManager.getInstance(project).findModuleByName(userContext.exportDDLModule);
            ModuleUtils.selectAndOpenPackage(project, module, filePath, false);
            // 提示完成
//            SwingUtilities.invokeLater(() -> {
//                String msg = "DDL导出完成 !  \n\n导出文件路径: " + filePath;
//                Messages.showMessageDialog(msg, "DDL导出", Icons.scaleToWidth(Icons.SUCCESS, 60));
//            });
//            super.doOKAction();
        });
    }

    private Module getSelectModule() {
        String moduleName = (String) ObjectUtils.defaultIfNull(moduleComboBox.getSelectedItem(), StringUtils.EMPTY);
        return ModuleManager.getInstance(project).findModuleByName(moduleName);
    }

    private String getSelectModulePath() {
        VirtualFile[] contentRoots = ModuleRootManager.getInstance(getSelectModule()).getContentRoots();
        if (contentRoots.length > 0) return contentRoots[0].getPath();

        String moduleFilePath = getSelectModule().getModuleFilePath();
        if (moduleFilePath.endsWith(".iml")) {
            moduleFilePath = moduleFilePath.substring(0, moduleFilePath.lastIndexOf("/"));
        }

        return moduleFilePath;
    }

    private void triggerEvent() {
        if (!moduleList.isEmpty()) {
            String exportDDLModule = userContext.exportDDLModule;
            if (moduleList.contains(exportDDLModule)) {
                this.moduleComboBox.setSelectedItem(exportDDLModule);
            } else {
                this.moduleComboBox.setSelectedItem(moduleList.get(0));
            }
        }
    }

    private void initData() {
        dialogComponentStatus(false);
        this.progressBar.setString("查询表信息中...");
        JTreeToolbarUtils.progressWorker(progressBar, () -> {
            boolean dialogEnable = true;
            // 导出路径
            this.exportPathStr = DatabaseDesignUtils.ddlExportPath();

            // showExportTable
            List<String> exportTableList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(exportTableList))
                exportTableList.add("\n[DDL] please select DDL scripts to exported...");

            String exportTables = String.join("\n", exportTableList);
            if (CollectionUtils.isEmpty(ddlList)) {
                exportTables = String.format("当前数据库 %s 暂无表数据, 无法导出DDL", databaseName);
                this.showExportTableArea.setForeground(JBColor.RED);
                dialogEnable = false;
            }
            this.showExportTableArea.setText(exportTables);

            // module
            moduleList.forEach(this.moduleComboBox::addItem);
            return dialogEnable;
        }, dialogEnable -> {
            dialogComponentStatus(dialogEnable);
            initEvent();
            triggerEvent();
            this.tableAreaScroll.getVerticalScrollBar().setValue(tableAreaScroll.getVerticalScrollBar().getMaximum());
        });
    }

    private void dialogComponentStatus(boolean enable) {
        this.setOKActionEnabled(enable);
        this.moduleComboBox.setEnabled(enable);
        this.exportPath.setEnabled(enable);
        this.exportPath.setEditable(enable);
    }

    private void initEvent() {
        moduleComboBox.addActionListener(e -> {
            Object selectedItem = moduleComboBox.getSelectedItem();
            if (Objects.isNull(selectedItem)) return;

            String selectModulePath = getSelectModulePath();
            String moduleFilePath = project.getName() + selectModulePath.replaceFirst(project.getBasePath(), "");
            this.modulePath.setText(moduleFilePath + "/");
            String defaultPath = String.format("%s/%s%s", exportPathStr, databaseName, ".sql");
            defaultPath = defaultPath.replaceAll("//", "/");
            this.exportPath.setText(defaultPath);
            // resize
            int width = modulePath.getPreferredSize().width;
            int height = modulePath.getPreferredSize().height;
            this.modulePath.setSize(new Dimension(width, height));
            this.exportPath.setSize(new Dimension(Math.max(this.moduleComboBox.getSize().width - width, 100), height));
            this.exportPanel.revalidate();
        });

        checkBoxList.addPropertyChangeListener(e -> {
            boolean hadSelected = false;
            for (int i = 0; i < checkBoxList.getItemsCount(); i++) {
                if (checkBoxList.isItemSelected(i)) {
                    hadSelected = true;
                    break;
                }
            }
            dialogComponentStatus(hadSelected);
        });
    }

    private void initComponents() {
        dialogPane = new JPanel(new BorderLayout());
        JPanel centerPanel = new JPanel(new VFlowLayout(VFlowLayout.TOP));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 50, 0, 50));
        dialogPane.add(centerPanel, BorderLayout.CENTER);

        // title
        JPanel titlePanel = new JPanel(new FlowLayout());
        JLabel title = new JLabel();
        title.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        title.setText(String.format("%s DDL导出", databaseName));
        titlePanel.add(title);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));
        title.setIcon(Icons.scaleToWidth(Icons.EXPORT_DDL, 22));
        centerPanel.add(titlePanel, BorderLayout.NORTH);

        moduleComboBox = new ComboBox<>();
        centerPanel.add(JPanelUtils.settingPanel("模块", moduleComboBox, labelSize, valueSize));

        modulePath = new JTextField();
        modulePath.setEditable(false);
        modulePath.setEnabled(false);
        exportPath = new JTextField();
        exportPanel = JPanelUtils.combinePanel("DDL文件路径", modulePath, exportPath, labelSize, valueSize);
        centerPanel.add(exportPanel);

        showExportTableArea = new JTextArea();
        showExportTableArea.setEditable(false);
        showExportTableArea.setFont(new Font("微软雅黑", Font.PLAIN, 13));

        JComponent exportTableCheckBox = selectExportTableCheckBox();
        UIUtil.forEachComponentInHierarchy(exportTableCheckBox, c -> c.setBackground(showExportTableArea.getBackground()));

        JPanel content = new JPanel(new BorderLayout());
        content.add(exportTableCheckBox, BorderLayout.NORTH);
        content.add(showExportTableArea, BorderLayout.CENTER);
        tableAreaScroll = new JBScrollPane(content);
        centerPanel.add(JPanelUtils.settingPanel("导出表信息", tableAreaScroll, labelSize, new Dimension(valueSize.width, 220), true));

        // 底部栏
        JPanel bottomPanel = new JPanel(new VFlowLayout(VFlowLayout.TOP));
        dialogPane.add(bottomPanel, BorderLayout.SOUTH);
        // 进度条
        progressBar = JTreeToolbarUtils.createIndeterminateProgressBar();
        progressBar.setVisible(false);
        progressBar.setBorder(BorderFactory.createEmptyBorder(0, 50, 0, 50));
        bottomPanel.add(progressBar);

        init();
        setTitle(GlobalDict.TITLE_INFO + "  " + databaseName + "-DDL导出");
        setOKButtonText("导出");
        setCancelButtonText("关闭");
    }

    private JComponent selectExportTableCheckBox() {
        checkBoxList = new CheckBoxList<>();
        int index = 1;
        for (ExportDDLDTO ddl : ddlList) {
            String title = String.format("[DDL] %s> %s.%s", index++, ddl.getDatabase().getName(), ddl.getTable().getTableName());
            checkBoxList.addItem(ddl, title, true);
        }

        return checkBoxList;
    }

    private void initSize() {
        dialogPane.setPreferredSize(new Dimension(800, 460));
    }

    private static final Dimension valueSize = new Dimension(300, 30);
    private static final Dimension labelSize = new Dimension(100, 30);
    private ComboBox<String> moduleComboBox;
    private JTextField modulePath;
    private JTextField exportPath;
    private JPanel exportPanel;
    private JTextArea showExportTableArea;
    private JBScrollPane tableAreaScroll;
    private JPanel dialogPane;
    private JProgressBar progressBar;

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return dialogPane;
    }
}
