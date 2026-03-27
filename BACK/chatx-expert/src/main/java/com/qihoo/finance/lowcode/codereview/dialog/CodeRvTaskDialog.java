package com.qihoo.finance.lowcode.codereview.dialog;

import com.google.common.collect.Lists;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBScrollPane;
import com.qihoo.finance.lowcode.codereview.entity.CodeRvRepoNode;
import com.qihoo.finance.lowcode.codereview.entity.CodeRvTaskNode;
import com.qihoo.finance.lowcode.codereview.entity.dto.CodeRvBranch;
import com.qihoo.finance.lowcode.codereview.entity.dto.CodeRvCommit;
import com.qihoo.finance.lowcode.codereview.entity.dto.CodeRvSaveDTO;
import com.qihoo.finance.lowcode.codereview.entity.dto.CodeRvTaskSaveVO;
import com.qihoo.finance.lowcode.codereview.util.CodeRvUtils;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.constants.OperateType;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.util.JButtonUtils;
import com.qihoo.finance.lowcode.common.ui.base.VFlowLayout;
import com.qihoo.finance.lowcode.common.util.*;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.qifu.devops.ide.plugins.jiracommit.domain.entity.ActiveJiraIssue;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
public class CodeRvTaskDialog extends DialogWrapper {
    private final Project project;
    private final OperateType operateType;
    private static final Dimension valueSize = new Dimension(600, 30);
    private static final Dimension labelSize = new Dimension(100, 30);

    public CodeRvTaskDialog(@NotNull Project project, CodeRvRepoNode repoNode, OperateType operateType) {
        super(project);
        this.project = project;
        this.operateType = operateType;
        this.repoNode = repoNode;

        initComponents();

        // setting components size
        settingSizes();

        // init event and data
        initComponentsEvent();
        initComponentsData();

        // init status
        initComponentStatus();

        // 提交按钮事件
        updateEnableOkAction();
        log.info(Constants.Log.USER_ACTION, "用户打开代码评审Dialog");
    }

    private void initComponents() {

        centerPanel = new JPanel(new BorderLayout());
        JPanel mainPanel = new JPanel(new VFlowLayout(VFlowLayout.TOP));
        centerPanel.add(mainPanel, BorderLayout.CENTER);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(0, 100, 0, 100));

        // 任务标题
        taskTitleField = new JTextField();
        mainPanel.add(JPanelUtils.settingPanel("标题", taskTitleField, labelSize, valueSize));

        // 关联活跃任务
        activeIssue = JPanelUtils.createSearchComboBox(new ActiveJiraIssue());
        activeIssue.setPreferredSize(new Dimension(200, -1));
        // 刷新
        JButton refreshActiveTask = JTreeToolbarUtils.createToolbarButton(Icons.scaleToWidth(Icons.REFRESH, 16), "刷新关联的活跃任务");
        refreshActiveTask.setPreferredSize(new Dimension(30, 18));
        refreshActiveTask.addActionListener(e -> {
            refreshActiveTask();
            JButtonUtils.countdown(refreshActiveTask, 10);
        });
        // 组合panel
        JPanel activeTask = JPanelUtils.settingPanel("关联开发任务", activeIssue, labelSize, valueSize);
        activeTask.add(refreshActiveTask, BorderLayout.EAST);
        mainPanel.add(activeTask);

        // 关联迭代&需求
        release = JPanelUtils.createPrototypeComboBox();
        demand = JPanelUtils.createPrototypeComboBox();
        release.setPreferredSize(new Dimension(200, -1));
        // 暂时忽略
//        mainPanel.add(JPanelUtils.combinePanel("关联迭代需求", release, demand, labelSize, valueSize));

        // 代码评审人
        reviewers = JPanelUtils.createPrototypeComboBox();
        JPanel reviewersPanel = JPanelUtils.settingPanel("代码评审人", reviewers, labelSize, valueSize);
        mainPanel.add(reviewersPanel);

        // 创建模式
        sameBranchBox = new JBCheckBox("基于Commit列表");
        diffBranchBox = new JBCheckBox("基于起始Commit");
        sameBranchBox.setSelected(true);
        JPanel createModePanel = JPanelUtils.settingPanel("差异(Diff)模式", JPanelUtils.singleCheckBox(sameBranchBox, diffBranchBox), labelSize, valueSize);
        createModePanel.setBorder(BorderFactory.createEmptyBorder(30, 0, 0, 0));
        mainPanel.add(createModePanel);

        // 起始分支节点
        sourceBranch = JPanelUtils.createSearchComboBox(new CodeRvBranch());
        sourceBranch.setPreferredSize(new Dimension(200, -1));

        sourceBranchCommit = JPanelUtils.createSearchComboBox(new CodeRvCommit());
        JPanel sourceBranchCommitPanel = new JPanel(new BorderLayout());
        sourceBranchCommitPanel.add(sourceBranchCommit, BorderLayout.CENTER);
        sourceBranchCommitPanel.add(new JLabel("    Commit  "), BorderLayout.WEST);

        startBranchPanel = JPanelUtils.combinePanel("源分支", sourceBranch, sourceBranchCommitPanel, labelSize, valueSize);
        mainPanel.add(startBranchPanel);

        // 结束分支节点
        targetBranch = JPanelUtils.createSearchComboBox(new CodeRvBranch());
        targetBranch.setPreferredSize(new Dimension(200, -1));

        targetBranchCommit = JPanelUtils.createSearchComboBox(new CodeRvCommit());
        JPanel targetBranchCommitPanel = new JPanel(new BorderLayout());
        targetBranchCommitPanel.add(targetBranchCommit, BorderLayout.CENTER);
        targetBranchCommitPanel.add(new JLabel("    Commit  "), BorderLayout.WEST);

        endBranchPanel = JPanelUtils.combinePanel("目标分支", targetBranch, targetBranchCommitPanel, labelSize, valueSize);
        mainPanel.add(endBranchPanel);

        // 分支
        sameBranch = JPanelUtils.createSearchComboBox(new CodeRvBranch());
        selectCommitBtn = new JButton("选择提交记录");
        selectCommitBtn.setPreferredSize(new Dimension(100, -1));
        sameBranchPanel = JPanelUtils.combinePanel("分支", null, sameBranch, selectCommitBtn, labelSize, valueSize);
        mainPanel.add(sameBranchPanel);

        // commit选中信息展示
        sameBranchCommitTextArea = JPanelUtils.tips(taskTitleField.getForeground());
        sameBranchCommitScroll = new JBScrollPane();
        sameBranchCommitScroll.setViewportView(sameBranchCommitTextArea);
        mainPanel.add(JPanelUtils.settingPanel("", sameBranchCommitScroll, labelSize, new Dimension(valueSize.width, 100)));

        init();
        setTitle(GlobalDict.TITLE_INFO + (isEdit() ? "-更新代码评审任务" : "-新建代码评审任务"));
        setOKButtonText("保存");
        setCancelButtonText("取消");
    }

    private void refreshActiveTask() {
        new SwingWorker<List<ActiveJiraIssue>, List<ActiveJiraIssue>>() {
            @Override
            protected List<ActiveJiraIssue> doInBackground() {
                progressBar.setString("刷新用户活跃任务中...");
                progressBar.setVisible(true);

                return CodeRvUtils.queryUserActiveIssues(true);
            }

            @Override
            @SneakyThrows
            protected void done() {
                activeIssue.removeAllItems();
                // 更新下拉列表数据
                List<ActiveJiraIssue> activeIssueItems = get();
                for (ActiveJiraIssue activeIssueItem : activeIssueItems) {
                    activeIssue.addItem(activeIssueItem);
                }

                activeIssue.repaint();
                progressBar.setVisible(false);
                super.done();
            }
        }.execute();
    }

    private void settingSizes() {
        centerPanel.setPreferredSize(new Dimension(1000, 300));
    }


    private void initComponentsEvent() {
        if (isView()) return;

        DocumentAdapter documentAdapter = new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                updateEnableOkAction();
            }
        };

        this.taskTitleField.getDocument().addDocumentListener(documentAdapter);
        this.sameBranchCommitTextArea.getDocument().addDocumentListener(documentAdapter);

        this.release.addActionListener(e -> {
            progressBar.setString("查询迭代信息中...");
            JTreeToolbarUtils.progressWorker(progressBar, () -> {
                String releaseName = (String) this.release.getSelectedItem();
                List<String> releaseDemands = CodeRvUtils.queryUserSprintDemands(releaseName);

                this.demand.removeAllItems();
                for (String releaseDemand : releaseDemands) {
                    this.demand.addItem(releaseDemand);
                }
                // 提交按钮事件
                updateEnableOkAction();
            });
        });


        this.sourceBranch.addActionListener(e -> {
            progressBar.setString("查询分支提交信息中...");
            JTreeToolbarUtils.progressWorker(progressBar, () -> {
                CodeRvBranch branch = JPanelUtils.getDialogComboBoxVal(this.sourceBranch, new CodeRvBranch());
                List<CodeRvCommit> commits = CodeRvUtils.queryRepoBranchCommits(repoNode, branch.getBranch());

                this.sourceBranchCommit.removeAllItems();
                for (CodeRvCommit commit : commits) {
                    this.sourceBranchCommit.addItem(commit);
                }
                // 提交按钮事件
                updateEnableOkAction();
                return null;
            }, re -> updateSourceBranchCommitIfEdit());
        });

        this.targetBranch.addActionListener(e -> {
            progressBar.setString("查询分支提交信息中...");
            JTreeToolbarUtils.progressWorker(progressBar, () -> {
                CodeRvBranch branch = JPanelUtils.getDialogComboBoxVal(this.targetBranch, new CodeRvBranch());
                List<CodeRvCommit> commits = CodeRvUtils.queryRepoBranchCommits(repoNode, branch.getBranch());

                this.targetBranchCommit.removeAllItems();
                for (CodeRvCommit commit : commits) {
                    this.targetBranchCommit.addItem(commit);
                }
                // 提交按钮事件
                updateEnableOkAction();
                return null;
            }, rs -> updateTargetBranchCommitIfEdit());
        });

        this.activeIssue.addActionListener(e -> {
            ActiveJiraIssue issue = JPanelUtils.getDialogComboBoxVal(this.activeIssue, new ActiveJiraIssue());

            // 同时如果已经设定了审核人, 将审核人选中
            String reviewer = issue.getReviewer();
            if (StringUtils.isNotEmpty(reviewer)) {
                this.reviewers.setSelectedItem(reviewer);
            }
        });

        this.diffBranchBox.addChangeListener(e -> {
            this.startBranchPanel.setVisible(this.diffBranchBox.isSelected());
            this.endBranchPanel.setVisible(this.diffBranchBox.isSelected());
            // 提交按钮事件
            updateEnableOkAction();
        });

        this.sameBranchBox.addChangeListener(e -> {
            this.sameBranchPanel.setVisible(this.sameBranchBox.isSelected());
            this.sameBranchCommitScroll.setVisible(this.sameBranchBox.isSelected());
            // 提交按钮事件
            updateEnableOkAction();
        });

        this.reviewers.addActionListener(e -> updateEnableOkAction());
        this.sameBranch.addActionListener(e -> {
            // 清空重置
            this.selectCommits.clear();
            this.sameBranchCommitTextArea.setText(StringUtils.EMPTY);
            // 提交按钮事件
            updateEnableOkAction();
        });

        // 提交信息
        this.selectCommitBtn.addActionListener(e -> {
            progressBar.setString("查询分支提交信息中...");

            JTreeToolbarUtils.progressWorker(progressBar, null, rs -> {
                CodeRvBranch branch = JPanelUtils.getDialogComboBoxVal(this.sameBranch, new CodeRvBranch());
                new CodeRvCommitDialog(project, branch, selectCommits, sameBranchCommitTextArea).show();
            });
        });
    }

    private void updateTargetBranchCommitIfEdit() {
        if (isEdit()) {
            CodeRvTaskNode taskNode = DataContext.getInstance(project).getSelectCodeRvTask();
            CodeRvTaskNode.DiffDetail diffDetail = taskNode.getDiffDetail();
            if (Objects.nonNull(diffDetail.getTargetCommit())) {
                this.targetBranchCommit.setSelectedItem(diffDetail.getTargetCommit());
            }
        }
    }

    private void updateSourceBranchCommitIfEdit() {
        if (isEdit()) {
            CodeRvTaskNode taskNode = DataContext.getInstance(project).getSelectCodeRvTask();
            CodeRvTaskNode.DiffDetail diffDetail = taskNode.getDiffDetail();
            if (Objects.nonNull(diffDetail.getSourceCommit())) {
                this.sourceBranchCommit.setSelectedItem(diffDetail.getSourceCommit());
            }
        }
    }

    private void initComponentsData() {
        if (isView()) {
            initComponentsViewData();
            initEditData();
            return;
        }

        JTreeToolbarUtils.progressWorker(progressBar, () -> {
            // 迭代
//            List<String> userRelease = CodeRvUtils.queryUserRelease();
//            for (String release : userRelease) {
//                this.release.addItem(release);
//            }

            // 分支信息
            progressBar.setString("查询用户迭代分支信息中...");
            List<CodeRvBranch> branchList = CodeRvUtils.queryRepoBranch(repoNode);
            for (CodeRvBranch branch : branchList) {
                this.sourceBranch.addItem(branch);
                this.targetBranch.addItem(branch);
                this.sameBranch.addItem(branch);
            }

            // 审核人
            progressBar.setString("查询审核人信息中...");
            List<String> reviewerList = CodeRvUtils.queryCodeRvReviewers();
            this.reviewers.removeAllItems();
            this.reviewers.addItem(StringUtils.EMPTY);
            for (String reviewer : reviewerList) {
                this.reviewers.addItem(reviewer);
            }

            // 活跃任务
            progressBar.setString("查询用户活跃任务信息中...");
            List<ActiveJiraIssue> activeIssueList = CodeRvUtils.queryUserActiveIssues();
            for (ActiveJiraIssue issue : activeIssueList) {
                this.activeIssue.addItem(issue);
            }

            return null;
        }, rs -> initEditData());
    }

    private void initComponentsViewData() {
        CodeRvTaskNode taskNode = DataContext.getInstance(project).getSelectCodeRvTask();

        // 审核人
        for (String reviewer : taskNode.getReviewers()) {
            this.reviewers.addItem(reviewer);
        }

        // 分支及模式
        CodeRvTaskNode.DiffDetail diffDetail = taskNode.getDiffDetail();
        CodeRvBranch viewSameBranch = CodeRvBranch.builder().branch(diffDetail.getBranch()).build();
        List<CodeRvCommit> viewSameCommits = diffDetail.getCommits();
        this.sameBranch.addItem(viewSameBranch);
        this.sameBranchCommitTextArea.setText(commitsToText(viewSameCommits));

        CodeRvBranch viewSourceBranch = CodeRvBranch.builder().branch(diffDetail.getSourceBranch()).build();
        CodeRvCommit viewSourceCommit = diffDetail.getSourceCommit();
        this.sourceBranch.addItem(viewSourceBranch);
        this.sourceBranchCommit.addItem(viewSourceCommit);

        CodeRvBranch viewTargetBranch = CodeRvBranch.builder().branch(diffDetail.getTargetBranch()).build();
        CodeRvCommit viewTargetCommit = diffDetail.getTargetCommit();
        this.targetBranch.addItem(viewTargetBranch);
        this.targetBranchCommit.addItem(viewTargetCommit);

        // 活跃任务
        List<ActiveJiraIssue> activeIssueList = CodeRvUtils.queryUserActiveIssues();
        for (ActiveJiraIssue issue : activeIssueList) {
            this.activeIssue.addItem(issue);
        }
    }

    private void initEditData() {
        if (isEdit() || isView()) {
            CodeRvTaskNode taskNode = DataContext.getInstance(project).getSelectCodeRvTask();

            // title
            this.taskTitleField.setText(taskNode.getTitle());

            // issue
            String issue = taskNode.getIssue();
            Map<String, ActiveJiraIssue> jiraTaskMap = CodeRvUtils.queryUserActiveTaskMap();
            if (StringUtils.isNotEmpty(issue) && jiraTaskMap.containsKey(issue)) {
                this.activeIssue.setSelectedItem(jiraTaskMap.get(issue));
            }

            for (String reviewer : taskNode.getReviewers()) {
                this.reviewers.setSelectedItem(reviewer);
            }

            CodeRvTaskNode.DiffDetail diffDetail = taskNode.getDiffDetail();
            // diffMode = 1, 基于2个commit 差异
            this.diffBranchBox.setSelected(!taskNode.isBaseSameBranch());
            if (StringUtils.isNotEmpty(diffDetail.getSourceBranch())) {
                this.sourceBranch.setSelectedItem(CodeRvBranch.builder().branch(diffDetail.getSourceBranch()).build());
            }
            if (Objects.nonNull(diffDetail.getSourceCommit())) {
                this.sourceBranchCommit.setSelectedItem(diffDetail.getSourceCommit());
            }
            if (StringUtils.isNotEmpty(diffDetail.getTargetBranch())) {
                this.targetBranch.setSelectedItem(CodeRvBranch.builder().branch(diffDetail.getTargetBranch()).build());
            }
            if (Objects.nonNull(diffDetail.getTargetCommit())) {
                this.targetBranchCommit.setSelectedItem(diffDetail.getTargetCommit());
            }

            // diffMode = 2, 基于commit 列表
            this.sameBranchBox.setSelected(taskNode.isBaseSameBranch());
            if (StringUtils.isNotEmpty(diffDetail.getBranch())) {
                this.sameBranch.setSelectedItem(CodeRvBranch.builder().branch(diffDetail.getBranch()).build());
            }

            if (CollectionUtils.isNotEmpty(diffDetail.getCommits())) {
                this.sameBranchCommitTextArea.setText(commitsToText(diffDetail.getCommits()));
                this.selectCommits.clear();
                this.selectCommits.addAll(diffDetail.getCommits());
            }

            // 编辑时不允许修改模式
            this.diffBranchBox.setEnabled(false);
            this.sameBranchBox.setEnabled(false);
        }
    }

    private void initComponentStatus() {
        startBranchPanel.setVisible(diffBranchBox.isSelected());
        endBranchPanel.setVisible(diffBranchBox.isSelected());
        sameBranchPanel.setVisible(sameBranchBox.isSelected());
        sameBranchCommitScroll.setVisible(sameBranchBox.isSelected());
    }

    private void updateEnableOkAction() {
        if (StringUtils.isEmpty(this.taskTitleField.getText())) {
            this.setOKActionEnabled(false);
            return;
        }
        Object reviewersItem = this.reviewers.getSelectedItem();
        if (Objects.nonNull(reviewersItem) && StringUtils.isEmpty(reviewersItem.toString())) {
            this.setOKActionEnabled(false);
            return;
        }
        if (!this.diffBranchBox.isSelected() && !this.sameBranchBox.isSelected()) {
            this.setOKActionEnabled(false);
            return;
        }
        if (this.sameBranchBox.isSelected() && StringUtils.isEmpty(sameBranchCommitTextArea.getText())) {
            this.setOKActionEnabled(false);
            return;
        }

        this.setOKActionEnabled(true);
    }

    //------------------------------------------------------------------------------------------------------------------

    private JPanel centerPanel;
    private JTextField taskTitleField;
    private ComboBox<String> release;
    private ComboBox<String> demand;
    private ComboBox<ActiveJiraIssue> activeIssue;
    private JBCheckBox diffBranchBox;
    private JBCheckBox sameBranchBox;
    private ComboBox<CodeRvBranch> sourceBranch;
    private ComboBox<CodeRvCommit> sourceBranchCommit;
    private ComboBox<CodeRvBranch> targetBranch;
    private ComboBox<CodeRvBranch> sameBranch;
    private JButton selectCommitBtn;
    private ComboBox<CodeRvCommit> targetBranchCommit;
    private JPanel startBranchPanel;
    private JPanel endBranchPanel;
    private JPanel sameBranchPanel;
    private JTextArea sameBranchCommitTextArea;
    private final List<CodeRvCommit> selectCommits = new ArrayList<>();
    private JBScrollPane sameBranchCommitScroll;
    private ComboBox<String> reviewers;
    private JProgressBar progressBar;
    private final CodeRvRepoNode repoNode;

    @Override
    protected void doOKAction() {
        new SwingWorker<Result<CodeRvTaskSaveVO>, Result<CodeRvTaskSaveVO>>() {
            @Override
            protected Result<CodeRvTaskSaveVO> doInBackground() {
                progressBar.setString(isEdit() ? "更新代码评审任务中..." : "创建代码评审任务中...");
                progressBar.setVisible(true);

                return saveTask();
            }

            @Override
            @SneakyThrows
            protected void done() {
                Result<CodeRvTaskSaveVO> result = get();
                if (JPanelUtils.checkSuccessAndShowErrMsg(result, "保存失败", StringUtils.EMPTY)) {
                    // 弹窗提示, 并提供打开web按钮选择
                    int select = Messages.showDialog(project, "\n代码评审任务保存成功 !\n\n", "保存成功", new String[]{"查看任务详情", "确定"}, 1, Icons.scaleToWidth(Icons.SUCCESS, 60));
                    if (select == 0 && Objects.nonNull(result.getData()) && StringUtils.isNotEmpty(result.getData().getWebUrl())) {
                        BrowserUtil.browse(result.getData().getWebUrl());
                    }

                    superDoOKAction();
                }

                // reloadTree
                JTree codeRvTree = DataContext.getInstance(project).getCodeRvTree();
                JTreeLoadingUtils.loading(true, codeRvTree, repoNode, () -> CodeRvUtils.queryCodeRvTaskSprints(repoNode));
                codeRvTree.expandPath(new TreePath(repoNode));

                progressBar.setVisible(false);
                super.done();
            }
        }.execute();
    }

    private void superDoOKAction() {
        super.doOKAction();
    }

    private Result<CodeRvTaskSaveVO> saveTask() {
        CodeRvTaskNode taskNode = DataContext.getInstance(project).getSelectCodeRvTask();
        ActiveJiraIssue activeJiraIssue = JPanelUtils.getDialogComboBoxVal(this.activeIssue, new ActiveJiraIssue());
        String reviewers = JPanelUtils.getDialogComboBoxVal(this.reviewers, null);

        CodeRvSaveDTO saveDTO = CodeRvSaveDTO.builder()
                // edit
                .title(this.taskTitleField.getText()).reviewId(isEdit() ? taskNode.getReviewId() : 0L).mrSourceBranch(isEdit() ? taskNode.getMrSourceBranch() : null).mrTargetBranch(isEdit() ? taskNode.getMrTargetBranch() : null)
                // issue
                .issue(activeJiraIssue.getIssue()).sprint(activeJiraIssue.getSprint()).reviewers(StringUtils.isNotEmpty(reviewers) ? Lists.newArrayList(reviewers) : new ArrayList<>())
                // diff
                .diffMode(this.getDialogDiffMode())
                .diffDetail(
                        CodeRvSaveDTO.DiffDetail.builder()
                                .sourceBranch(JPanelUtils.getDialogComboBoxVal(this.sourceBranch, new CodeRvBranch()).getBranch())
                                .sourceCommit(JPanelUtils.getDialogComboBoxVal(this.sourceBranchCommit, new CodeRvCommit()))
                                .targetBranch(JPanelUtils.getDialogComboBoxVal(this.targetBranch, new CodeRvBranch()).getBranch())
                                .targetCommit(JPanelUtils.getDialogComboBoxVal(this.targetBranchCommit, new CodeRvCommit()))
                                .branch(JPanelUtils.getDialogComboBoxVal(this.sameBranch, new CodeRvBranch()).getBranch())
                                .commits(this.selectCommits).build()).build();

        return isEdit() ? CodeRvUtils.updateCodeReviewTask(repoNode, saveDTO) : CodeRvUtils.createCodeReviewTask(repoNode, saveDTO);
    }

    private boolean isEdit() {
        return OperateType.EDIT.equals(this.operateType);
    }

    private boolean isView() {
        return OperateType.VIEW.equals(this.operateType);
    }

    public static String commitsToText(List<CodeRvCommit> commits) {
        if (CollectionUtils.isEmpty(commits)) return StringUtils.EMPTY;
        return commits.stream().map(CodeRvCommit::getMessage).map(msg -> msg.replaceAll("\n", "")).collect(Collectors.joining("\n"));
    }

    private int getDialogDiffMode() {
        // 1: 基于2个分支commit 差异  2: 基于相同分支commit 列表
        return this.sameBranchBox.isSelected() ? 2 : 1;
    }

    @Override
    public @NotNull JComponent createCenterPanel() {
        return this.centerPanel;
    }

    @Override
    protected @Nullable JComponent createNorthPanel() {
        // title
        JPanel titlePanel = new JPanel(new FlowLayout());
        JLabel title = new JLabel();
        title.setIcon(Icons.scaleToWidth(Icons.GIT_LAB, 36));
        title.setFont(new Font("微软雅黑", Font.BOLD, 18));
        title.setText(repoNode.getName() + "  代码评审");
        titlePanel.add(title);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));
        return titlePanel;
    }

    @Override
    protected JComponent createSouthPanel() {
        JPanel southPanel = new JPanel(new VFlowLayout(VFlowLayout.TOP));
        // 温馨提示
        JTextArea tipsArea = JPanelUtils.tips();
        tipsArea.setText(CodeRvUtils.getTaskTips());
        tipsArea.setBorder(BorderFactory.createEmptyBorder(20, 100, 50, 100));
        southPanel.add(tipsArea);

        // 进度条
        progressBar = JTreeToolbarUtils.createIndeterminateProgressBar();
        progressBar.setVisible(false);
        southPanel.add(progressBar);

        // action
        southPanel.add(super.createSouthPanel());
        return southPanel;
    }
}
