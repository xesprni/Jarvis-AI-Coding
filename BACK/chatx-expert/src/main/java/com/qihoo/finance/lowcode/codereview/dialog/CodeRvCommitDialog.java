package com.qihoo.finance.lowcode.codereview.dialog;

import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.qihoo.finance.lowcode.codereview.entity.dto.CodeRvBranch;
import com.qihoo.finance.lowcode.codereview.entity.dto.CodeRvCommit;
import com.qihoo.finance.lowcode.codereview.ui.table.CodeRvCommitTableWrap;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.JTreeToolbarUtils;
import com.qihoo.finance.lowcode.common.utils.LocalDateUtils;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CreateCodeRvTaskDialog
 *
 * @author fengjinfu-jk
 * date 2023/11/2
 * @version 1.0.0
 * @apiNote CreateCodeRvTaskDialog
 */
@Slf4j
public class CodeRvCommitDialog extends DialogWrapper {

    public CodeRvCommitDialog(@NotNull Project project, CodeRvBranch branch, List<CodeRvCommit> selectCommits, JTextArea commitTextArea) {
        super(project);
        this.project = project;
        this.commitTextArea = commitTextArea;
        this.selectCommits = selectCommits;
        this.branch = branch;

        initComponents();

        // setting components size
        settingSizes();

        // setting components status
        initComponentsEvent();
        initComponentsData();

        // 提交按钮事件
        log.info(Constants.Log.USER_ACTION, "用户打开代码评审Dialog");
    }

    @Override
    protected void doOKAction() {
        List<CodeRvCommit> selectCommits = CodeRvCommitTableWrap.getSelectCommits(commitTable);
        this.selectCommits.clear();
        this.selectCommits.addAll(selectCommits);
        this.commitTextArea.setText(CodeRvTaskDialog.commitsToText(selectCommits));

        // 关闭窗口
        super.doOKAction();
    }

    private void initComponents() {
        // panel
        initCenterComments();
        initNorthComments();

        // dialog
        init();
        setTitle(GlobalDict.TITLE_INFO + "  代码提交记录");
        setOKButtonText("确定");
        setCancelButtonText("取消");
        getRootPane().setDefaultButton(searchBtn);
    }

    private void initCenterComments() {
        centerPanel = new JPanel(new BorderLayout());

        CodeRvCommitTableWrap codeRvCommitTableWrap = new CodeRvCommitTableWrap(project, branch, selectCommits);
        commitTable = codeRvCommitTableWrap.createTable();

        JBScrollPane scrollPane = new JBScrollPane(commitTable);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private void initNorthComments() {
        northPanel = new JPanel(new BorderLayout());
        // title
        JPanel titlePanel = new JPanel(new FlowLayout());
        JLabel title = new JLabel();
        title.setIcon(Icons.scaleToWidth(Icons.GIT_LAB, 36));
        title.setFont(new Font("微软雅黑", Font.BOLD, 18));
        title.setText(StringUtils.defaultString(branch.getBranch()) + " 提交记录");
        titlePanel.add(title);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 50, 0, 50));
        northPanel.add(titlePanel, BorderLayout.NORTH);

        // search
        searchCommit = new JTextField();
        searchBtn = new JButton();
        JPanel search = JTreeToolbarUtils.createToolbarSearch(searchCommit, searchBtn);

        // btn
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        // filter user
        btnPanel.add(new JLabel(Icons.scaleToWidth(Icons.LOGIN_USER, 18)));
        userComboBox = new ComboBox<>();
        btnPanel.add(userComboBox);

        // filter Date
        btnPanel.add(new JLabel(Icons.scaleToWidth(Icons.DATE, 18)));
        dateComboBox = new ComboBox<>();
        btnPanel.add(dateComboBox);

        // search panel
        northPanel.add(search, BorderLayout.CENTER);
        northPanel.add(btnPanel, BorderLayout.EAST);
    }

    private void settingSizes() {
        centerPanel.setPreferredSize(new Dimension(950, 460));
    }


    private void initComponentsEvent() {
        userComboBox.addActionListener(e -> filterTable(commitTable));
        dateComboBox.addActionListener(e -> filterTable(commitTable));
        searchBtn.addActionListener(e -> filterTable(commitTable));
    }

    private void initComponentsData() {
        // User filter
        userComboBox.removeAllItems();
        userComboBox.addItem("ALL");
        List<CodeRvCommit> commits = CodeRvCommitTableWrap.getAllCommits(commitTable);
        Set<String> users = commits.stream().map(CodeRvCommit::getAuthor).collect(Collectors.toSet());
        for (String user : users) {
            userComboBox.addItem(user);
        }

        // Date filter
        dateComboBox.removeAllItems();
        String[] dateFilters = {"ALL", "Last 24 hours", "Last 7 Days"};
        for (String dateFilter : dateFilters) {
            dateComboBox.addItem(dateFilter);
        }
    }

    private void filterTable(JTable table) {
        RowFilter<DefaultTableModel, Object> filters = RowFilter.andFilter(Lists.newArrayList(
                filterCommit(searchCommit.getText()),
                filterCommit(Objects.requireNonNull(userComboBox.getSelectedItem()).toString(), CodeRvCommitTableWrap.COMMIT_AUTHOR),
                filterCommitDate())
        );

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>((DefaultTableModel) table.getModel());
        table.setRowSorter(sorter);
        sorter.setRowFilter(filters);
    }

    private RowFilter<DefaultTableModel, Object> filterCommit(String text, int... indices) {
        text = "ALL".equalsIgnoreCase(text) ? StringUtils.EMPTY : text;
        return RowFilter.regexFilter(text.trim(), indices);
    }

    private RowFilter<DefaultTableModel, Object> filterCommitDate() {
        Object selectedDate = dateComboBox.getSelectedItem();
        String dateComboBoxVal = Objects.nonNull(selectedDate) ? selectedDate.toString() : "ALL";
        Date endDate = new Date();
        Date startDate;
        switch (dateComboBoxVal) {
            case "Last 24 hours":
                startDate = LocalDateUtils.yesterday(endDate);
                break;
            case "Last 7 Days":
                startDate = LocalDateUtils.plusDays(endDate, -7L);
                break;
            default:
                startDate = new Date(0L);
        }

        return new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ?> entry) {
                Date rowDate = LocalDateUtils.formatDateStr(entry.getStringValue(CodeRvCommitTableWrap.COMMIT_DATE), LocalDateUtils.FORMAT_DATE_TIME);
                return rowDate != null && (rowDate.equals(startDate) || rowDate.equals(endDate) || (rowDate.after(startDate) && rowDate.before(endDate)));
            }
        };
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

    private Project project;
    private CodeRvBranch branch;
    private JTextArea commitTextArea;
    private List<CodeRvCommit> selectCommits;
    private JTable commitTable;
    private JButton searchBtn;
    private JTextField searchCommit;
    private JPanel centerPanel;
    private JPanel northPanel;
    private ComboBox<String> userComboBox;
    private ComboBox<String> dateComboBox;
}
