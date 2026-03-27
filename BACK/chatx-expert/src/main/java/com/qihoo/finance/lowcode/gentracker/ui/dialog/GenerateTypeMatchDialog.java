package com.qihoo.finance.lowcode.gentracker.ui.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.JPanelUtils;
import com.qihoo.finance.lowcode.design.entity.MySQLTableNode;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import com.qihoo.finance.lowcode.gentracker.entity.TableInfo;
import com.qihoo.finance.lowcode.gentracker.service.TableInfoSettingsService;
import com.qihoo.finance.lowcode.gentracker.ui.table.TypeMatchTableWrap;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * CreateCodeRvTaskDialog
 *
 * @author fengjinfu-jk
 * date 2023/11/2
 * @version 1.0.0
 * @apiNote CreateCodeRvTaskDialog
 */
@Slf4j
public class GenerateTypeMatchDialog extends DialogWrapper {

    public GenerateTypeMatchDialog(@NotNull Project project, MySQLTableNode table) {
        super(project);
        this.project = project;
        this.table = table;
        this.dbTable = TableInfoSettingsService.getInstance().getTableInfo(table);

        initComponents();

        // setting components size
        settingSizes();

        // setting components status
        initComponentsEvent();
        initComponentsData();

        // 提交按钮事件
        log.info(Constants.Log.USER_ACTION, "用户打开字段匹配Dialog");
    }

    @Override
    protected void doOKAction() {
        // todo 提交并刷新当前表字段类型匹配信息

        // 关闭窗口
        super.doOKAction();
    }

    private void initComponents() {
        // panel
        initNorthComments();
        initCenterComments();

        // dialog
        init();
        setTitle(GlobalDict.TITLE_INFO + "  字段类型匹配");
        setOKButtonText("确定");
        setCancelButtonText("取消");
    }

    private void initCenterComments() {
        centerPanel = new JPanel(new BorderLayout());

        TypeMatchTableWrap typeMatchTableWrap = new TypeMatchTableWrap(project, dbTable);
        typeMatchTable = typeMatchTableWrap.createTable();

        JBScrollPane scrollPane = new JBScrollPane(typeMatchTable);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private void initNorthComments() {
        northPanel = new JPanel(new BorderLayout());
        // title
        JPanel titlePanel = new JPanel(new FlowLayout());
        JLabel title = new JLabel();
        title.setIcon(Icons.scaleToWidth(Icons.TABLE2, 16));
        title.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        title.setText(String.format("%s.%s 字段类型", table.getDatabase(), table.getTableName()));
        titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 50, 20, 50));
        titlePanel.add(title);
        northPanel.add(titlePanel, BorderLayout.CENTER);
    }

    private void settingSizes() {
        JPanelUtils.setSize(this.centerPanel, new Dimension(800, 400));
    }


    private void initComponentsEvent() {
        // todo
    }

    private void initComponentsData() {
        // todo
    }

    //------------------------------------------------------------------------------------------------------------------

    @Override
    public @NotNull JComponent createCenterPanel() {
        return this.centerPanel;
    }

    @Override
    protected @Nullable JComponent createNorthPanel() {
        return this.northPanel;
    }

    private final Project project;
    private final MySQLTableNode table;
    private final TableInfo dbTable;
    private JTable typeMatchTable;
    private JPanel centerPanel;
    private JPanel northPanel;
}
