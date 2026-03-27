package org.qifu.devops.ide.plugins.jiracommit.ui;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vcs.CommitMessageI;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.ui.CommitMessage;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBScrollPane;
import com.qihoo.finance.lowcode.common.configuration.UserContextPersistent;
import com.qihoo.finance.lowcode.common.entity.dto.jira.TransitionStatusInfo;
import com.qihoo.finance.lowcode.common.ui.base.CheckBoxGroup;
import com.qihoo.finance.lowcode.common.ui.base.RoundedLabel;
import com.qihoo.finance.lowcode.common.ui.base.VFlowLayout;
import com.qihoo.finance.lowcode.common.util.*;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.qifu.devops.ide.plugins.jiracommit.domain.entity.ActiveJiraIssue;
import org.qifu.devops.ide.plugins.jiracommit.domain.entity.ActiveJiraIssueDetail;
import org.qifu.devops.ide.plugins.jiracommit.domain.entity.JiraIssueBaseDTO;
import org.qifu.devops.ide.plugins.jiracommit.domain.enums.CommitType;
import org.qifu.devops.ide.plugins.jiracommit.service.DevopsApiService;
import org.qifu.devops.ide.plugins.jiracommit.util.JiraCommitUtils;
import com.qifu.services.CommitMessageGenerator;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;
import com.intellij.notification.NotificationType;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

public class CommitHelperFromDialog extends DialogWrapper {
    private static final Logger LOG = Logger.getInstance(CommitHelperFromDialog.class);
    
    private final Map<String, String> pluginMap;
    private final List<String> pipelineConfigNames;
    private final Project project;

    private static final String PIPELINE_TIPS = "读取流水线配置信息中...";
    private static final String ACTIVE_TASK_TIPS = "刷新关联活跃任务中...";
    private static final String COMMIT_TIPS = "刷新温馨提示信息中...";
    private final AnActionEvent event;

    private static UserContextPersistent getUserContextPersistent() {
        return UserContextPersistent.getInstance();
    }

    public CommitHelperFromDialog(@NotNull AnActionEvent anActionEvent, Map<String, String> pluginParam) {
        super(true);
        this.project = ObjectUtils.defaultIfNull(anActionEvent.getProject(), ProjectUtils.getCurrProject());
        this.pluginMap = pluginParam;
        this.pipelineConfigNames = new ArrayList<>();
        this.event = anActionEvent;

        // init
        initComponents();
        // initEvent
        initComponentsEvent();

        // dialog
        init();
        setModal(true);
        setTitle("代码提交 & 流水线构建");
        // btn
        this.setOKButtonText("保存");
        this.setCancelButtonText("取消");
    }

    private void initComponentsEvent() {
        loadTips();
        loadActiveTask();
        loadPipeline(false);
    }

    private void initComponentsData(List<ActiveJiraIssueDetail> rs) {
        ActiveJiraIssue commitActiveJiraIssue = getUserContextPersistent().getState().getCommitActiveJiraIssue();
        if (Objects.nonNull(commitActiveJiraIssue)) {
            activeIssueListComboBox.setSelectedItem(commitActiveJiraIssue);
        }

        // 每次都调用 AI 生成 commit message
        generateCommitMessageWithAI();
    }

    private void fillInCommitHistoryMessage(List<ActiveJiraIssueDetail> rs) {
        CommitMessageI messageI = getCommitMessage(this.event);
        if (messageI instanceof CommitMessage) {
            CommitMessage message = (CommitMessage) messageI;
            String comment = message.getComment().trim();
            if (StringUtils.isEmpty(comment)) return;

            for (ActiveJiraIssueDetail issue : rs) {
                if (comment.contains(issue.getIssue())) {
                    comment = StringUtils.substringAfter(comment, issue.getIssue()).trim();
                    if (comment.startsWith("):")) {
                        comment = StringUtils.substringAfter(comment, "):").trim();
                    }
                    if (comment.startsWith(")")) {
                        comment = StringUtils.substringAfter(comment, ")").trim();
                    }
                    if (comment.startsWith("）")) {
                        comment = StringUtils.substringAfter(comment, "）").trim();
                    }
                    if (comment.startsWith(":")) {
                        comment = StringUtils.substringAfter(comment, ":").trim();
                    }

                    if (comment.contains(issue.getIssueTitle())) {
                        comment = comment.replace(issue.getIssueTitle(), "");
                    }
                    if (comment.contains(issue.getSprint())) {
                        comment = comment.replace(issue.getSprint(), "").replace("()", "");
                    }
                }
            }

            comment = StringUtils.substringBefore(comment, "\nbuild");
            comment = StringUtils.substringBefore(comment, "\ntransition");
            comment = subFix(comment.trim());
            String updateComment = comment;
            WriteCommandAction.runWriteCommandAction(project, () -> {
                this.commitMsg.getDocument().setText(updateComment);
            });
        }
    }

    private String subFix(String comment) {
        if (comment.endsWith("\n")) {
            comment = comment.substring(comment.length() - 1);
            return subFix(comment);
        }

        return comment;
    }

    private void initComponents() {
        centerPanel = new JPanel(new VFlowLayout(VFlowLayout.TOP));
        refreshProgressBar = JTreeToolbarUtils.createIndeterminateProgressBar();
        refreshProgressBar.setVisible(false);

        // 请选择关联的活跃任务
        activeIssueListComboBox = new ComboBox<>();
        activeIssueListComboBox.setSwingPopup(false);
        activeIssueListComboBox.setRenderer(activeIssueRenderer());
        activeIssueListComboBox.addActionListener(e -> selectActiveIssue());

        JPanel activeTask = JPanelUtils.settingPanel("开发任务", activeIssueListComboBox, null, new Dimension(-1, 33), true);
        JButton refreshActiveTask = JTreeToolbarUtils.createToolbarButton(Icons.scaleToWidth(Icons.REFRESH, 16), "刷新关联的活跃任务");
        refreshActiveTask.setPreferredSize(new Dimension(30, 18));
        refreshActiveTask.addActionListener(e -> {
            loadActiveTask();
            JButtonUtils.countdown(refreshActiveTask, 10);
        });
        activeTask.add(refreshActiveTask, BorderLayout.EAST);
        centerPanel.add(activeTask);

        activeTaskTips = new JLabel();
        activeTaskTips.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        activeTaskTips.setVisible(false);
        activeTaskTips.setForeground(JBColor.RED);
        centerPanel.add(activeTaskTips);

        // 任务状态更新
        issueTransitionPanel = new JPanel(new GridLayout(-1, 4));
        issueTransitionPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        issueTransitionShowPanel = JPanelUtils.settingPanel("任务状态更新至:", issueTransitionPanel, null, new Dimension(-1, 33), false);
        issueTransitionShowPanel.setBorder(BorderFactory.createEmptyBorder(-5, 5, 0, 0));
        issueTransitionShowPanel.setVisible(false);
        centerPanel.add(issueTransitionShowPanel);

        //代码提交类型信息
        commitTypeComboBox = new ComboBox<>(CommitType.values());
        commitTypeComboBox.setSwingPopup(false);
        commitTypeComboBox.setRenderer(commitTypeRenderer());
        // 请选择代码提交类型
        JPanel commitType = JPanelUtils.settingPanel("提交类型", commitTypeComboBox, null, new Dimension(-1, 33), true);
        commitType.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        centerPanel.add(commitType);

        // 请输入当前代码注释
        commitMsg = EditorComponentUtils.createEditorPanel();
        JPanel commentPanel = JPanelUtils.settingPanel("Commit Message", commitMsg.getComponent(), null, new Dimension(-1, 80), true);
        commentPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        centerPanel.add(commentPanel);

        // 自动构建流水线&刷新
        pipelinePanel = new JPanel(new BorderLayout());
        JButton refreshPipeline = JTreeToolbarUtils.createToolbarButton(Icons.scaleToWidth(Icons.REFRESH, 16), "刷新流水线配置信息");
        refreshPipeline.setPreferredSize(new Dimension(30, 18));
        refreshPipeline.addActionListener(e -> {
            // 插件缓存清除
            CacheManager.refreshTemplate();
            loadPipeline(true);
            JButtonUtils.countdown(refreshPipeline, 10);
        });

        // 自动构建流水线&刷新 布局
        JPanel autoBuild = JPanelUtils.flowPanel(FlowLayout.LEFT, new JLabel("自动构建流水线"), refreshPipeline);
        JPanel autoBuildPanel = new JPanel(new BorderLayout());
        autoBuildPanel.add(autoBuild, BorderLayout.NORTH);
        JBScrollPane autoBuildScroll = new JBScrollPane();
        autoBuildScroll.setViewportView(pipelinePanel);
        autoBuildScroll.setPreferredSize(new Dimension(-1, 80));
        autoBuildPanel.add(autoBuildScroll, BorderLayout.CENTER);
        autoBuildPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        centerPanel.add(autoBuildPanel);

        // 温馨提示
        JPanel tipsPanel = new JPanel(new VFlowLayout(VFlowLayout.TOP));
        tipsArea = JPanelUtils.tips(JBColor.RED);
        tipsPanel.add(tipsArea);
        tipsPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        centerPanel.add(tipsPanel);
        centerPanel.add(refreshProgressBar, BorderLayout.PAGE_END);
        JPanelUtils.setSize(this.centerPanel, new Dimension(700, 540));
    }

    private ListCellRenderer<? super CommitType> commitTypeRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component renderer = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                // 自定义选项的外观和布局
                if (value instanceof CommitType) {
                    JPanel panel = new JPanel(new GridBagLayout());
                    if (renderer instanceof JLabel) panel.setBorder(((JLabel) renderer).getBorder());
                    panel.setPreferredSize(renderer.getPreferredSize());

                    CommitType commitType = (CommitType) value;
                    JLabel commitTypeLabel = new JLabel(commitType.getType());
                    commitTypeLabel.setFont(new Font("微软雅黑", Font.BOLD, 12));
//                    RoundedLabel commitTypeLabel = new RoundedLabel(commitType.getType(), JBColor.background().darker(), RoundedLabel.WHITE, 10);

                    JLabel remark = new JLabel(commitType.getRemark());
                    remark.setHorizontalAlignment(SwingConstants.RIGHT);
                    remark.setForeground(JBColor.GRAY);

                    GridBagConstraints constraints = new GridBagConstraints();
                    constraints.fill = GridBagConstraints.BOTH;
                    constraints.weightx = 1.0;
                    constraints.gridx = 0;
                    constraints.gridwidth = 1;
                    panel.add(commitTypeLabel, constraints);

                    constraints.gridx = 1;
                    constraints.gridwidth = 4;
                    panel.add(remark, constraints);

                    if (isSelected) panel.setBackground(RoundedLabel.SELECTED);
                    return panel;
                }

                return renderer;
            }
        };
    }

    private ListCellRenderer<? super Object> activeIssueRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component renderer = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                // 自定义选项的外观和布局
                if (value instanceof ActiveJiraIssue) {
                    JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                    if (renderer instanceof JLabel) panel.setBorder(((JLabel) renderer).getBorder());

                    ActiveJiraIssue issue = (ActiveJiraIssue) value;
                    if (StringUtils.isNotEmpty(issue.getIssue())) {
                        JLabel issueLabel = new JLabel(StringUtils.rightPad(issue.getIssue(), 10, " "));
                        if (!issue.isAvailable()) issueLabel.setForeground(JBColor.GRAY);
                        panel.add(issueLabel);
                    }
                    if (StringUtils.isNotEmpty(issue.getIssueTitle())) {
                        JLabel titleLabel = new JLabel(issue.getIssueTitle());
                        if (!issue.isAvailable()) titleLabel.setForeground(JBColor.GRAY);
                        panel.add(titleLabel);
                    }
                    if (StringUtils.isNotEmpty(issue.getSprint())) {
                        Color background = issue.isAvailable() ? RoundedLabel.GREEN : JBColor.background().darker();
                        Color foreground = issue.isAvailable() ? RoundedLabel.WHITE : JBColor.GRAY;
                        panel.add(new RoundedLabel(issue.getSprint(), background, foreground, 10));
                    }
                    if (StringUtils.isNotEmpty(issue.getStatus())) {
                        Color background = issue.isAvailable() ? RoundedLabel.BLUE : JBColor.background().darker();
                        Color foreground = issue.isAvailable() ? RoundedLabel.WHITE : JBColor.GRAY;
                        panel.add(new RoundedLabel(issue.getStatus(), background, foreground, 10));
                    }

                    if (!issue.isAvailable()) {
                        panel.setForeground(JBColor.GRAY);
                        panel.setEnabled(false);
                        // 选中置灰
                        if (isSelected) panel.setBackground(JBColor.background().brighter());
                    } else {
                        if (isSelected) panel.setBackground(RoundedLabel.SELECTED);
                    }

                    if (StringUtils.isNotBlank(issue.getTips())) panel.setToolTipText(issue.getTips());
                    return panel;
                }

                return renderer;
            }
        };
    }

    private void selectActiveIssue() {
        // 任务规范校验
        ActiveJiraIssueDetail activeIssue = JPanelUtils.getDialogComboBoxVal(this.activeIssueListComboBox, new ActiveJiraIssueDetail());
        // 隐藏状态更新
        issueTransitionShowPanel.setVisible(false);
        if (!activeIssue.isAvailable()) {
            // 提示不符合规范
            String errTips = StringUtils.defaultIfBlank(activeIssue.getUnAvailableTips(), String.format("%s 不符合提交规范, 请选择其他任务", activeIssue.getIssue()));
            this.activeTaskTips.setText(errTips);
            this.activeTaskTips.setVisible(true);
            // 禁用提交
            this.setOKActionEnabled(false);
        } else {
            this.activeTaskTips.setVisible(false);
            this.setOKActionEnabled(true);
            // 更新显式
            checkBoxGroup.removeAll();
            issueTransitionPanel.removeAll();
            // 联动任务状态更新: 任务状态更新, 多个互斥checkBox
            List<TransitionStatusInfo> changeableStatus = JiraCommitUtils.queryTransitionStatus(activeIssue);
            issueTransitionShowPanel.setVisible(CollectionUtils.isNotEmpty(changeableStatus));
            for (TransitionStatusInfo status : changeableStatus) {
                if (status.isMustTransition()) {
                    JBCheckBox checkBox = new JBCheckBox(status.getName());
                    checkBox.setToolTipText(status.getTips());
                    checkBox.setSelected(status.isMustTransition());
                    checkBox.setEnabled(false);
                    checkBoxGroup.add(checkBox, status);
                    issueTransitionPanel.add(checkBox);
                } else {
                    JBCheckBox checkBox = checkBoxGroup.add(status.getName(), status);
                    checkBox.setForeground(JBColor.BLUE);
                    checkBox.setToolTipText(status.getTips());
                    issueTransitionPanel.add(checkBox);
                }
            }
            issueTransitionPanel.repaint();
        }
    }

    @Override
    protected void doOKAction() {
        boolean titleCheck = Boolean.parseBoolean(pluginMap.get("showTitle"));
        boolean sprintCheck = Boolean.parseBoolean(pluginMap.get("showSprint"));
        int jiraInfoPosition = Integer.parseInt(pluginMap.get("jiraInfoPosition"));
        String commitTypeValue = Objects.requireNonNull(commitTypeComboBox.getSelectedItem()).toString();
        String commentsStr = commitMsg.getDocument().getText().trim();

        // 任务信息
        ActiveJiraIssueDetail issueDetail = (ActiveJiraIssueDetail) activeIssueListComboBox.getSelectedItem();
        String formatCommentsContent = formatComments(issueDetail, titleCheck, sprintCheck, commitTypeValue, commentsStr, jiraInfoPosition);
        // 自动构建信息
        if (CollectionUtils.isNotEmpty(pipelineConfigNames)) {
            formatCommentsContent += "\n\nbuild " + String.join(" ", pipelineConfigNames);
        }

        // 任务状态更新
        TransitionStatusInfo transition = checkBoxGroup.getSelected();
        if (Objects.nonNull(transition)) {
            formatCommentsContent += String.format("\ntransition %s %s", issueDetail.getIssue(), transition.getTransitionId());
        }

        // 回显
        getCommitMessage(event).setCommitMessage(formatCommentsContent);

        // 记录选中ActiveJiraIssue
        loadActiveJiraIssueState();
        super.doOKAction();
    }

    private void loadActiveJiraIssueState() {
        getUserContextPersistent().getState().commitActiveJiraIssue = JPanelUtils.getDialogComboBoxVal(activeIssueListComboBox, null);
    }

    private String getIssueKey(String issueSelectedValue) {
        if (StringUtils.isEmpty(issueSelectedValue)) return issueSelectedValue;

        String[] issueInfoArr = issueSelectedValue.split(" ");
        return issueInfoArr[0];
    }

    private String formatComments(ActiveJiraIssueDetail issueDetail, boolean titleCheck, boolean sprintCheck, String commitType, String comments, int jiraInfoPosition) {
        //处理解析issue关键信息
        // Jira详情信息
        StringBuilder jiraInfoBuilder = new StringBuilder();
        if (titleCheck) {
            jiraInfoBuilder.append(issueDetail.getIssueTitle());
        }
        if (sprintCheck) {
            jiraInfoBuilder.append("(").append(issueDetail.getSprint()).append(")");
        }

        //注释首位信息
        String firstCommentBlock = issueDetail.getIssue();
        if (!StringUtils.isEmpty(commitType)) {
            firstCommentBlock = commitType + "(" + issueDetail.getIssue() + "):";
        }

        //实际注释信息
        StringBuilder commentsBuilder = new StringBuilder();
        commentsBuilder.append(firstCommentBlock).append(" ");
        switch (jiraInfoPosition) {
            case 1:
                commentsBuilder.append(jiraInfoBuilder).append(" ").append(comments);
                break;
            case 2:
                commentsBuilder.append(comments).append("\r\n").append(jiraInfoBuilder);
                break;
            default:
                commentsBuilder.append(comments).append("\r\n").append(jiraInfoBuilder);
        }

        return commentsBuilder.toString();
    }

    private void loadTips() {
        JTreeToolbarUtils.progressWorker(refreshProgressBar, () -> {
            refreshProgressBar.setString(COMMIT_TIPS);
            return JiraCommitUtils.getTips();
        }, rs -> tipsArea.setText(rs));
    }

    private void loadActiveTask() {
        JTreeToolbarUtils.progressWorker(refreshProgressBar, () -> {
            refreshProgressBar.setString(ACTIVE_TASK_TIPS);

//                JiraCommitUtils.refreshActiveTask(pluginMap);
//            String gitUrl = GitUtils.getCurrentProjectGitUrl();
            Set<String> gitUrls = GitUtils.getGitUrls();
            String gitUrlStr = String.join(",", gitUrls);
            return JiraCommitUtils.queryUserActiveIssues(true, gitUrlStr);
        }, rs -> {
            activeIssueListComboBox.removeAllItems();
            // 更新下拉列表数据
            for (ActiveJiraIssueDetail activeIssueItem : rs) {
                activeIssueListComboBox.addItem(activeIssueItem);
            }

            initComponentsData(rs);
            activeIssueListComboBox.repaint();
        });
    }

    private void loadPipeline(boolean forceRefresh) {
        CommitHelperFromDialog commitHelperFromDialog = this;
        JTreeToolbarUtils.progressWorker(refreshProgressBar, () -> {
            refreshProgressBar.setString(PIPELINE_TIPS);
            pipelineConfigNames.clear();

            return JiraCommitUtils.getPipelineConfigNames(project, forceRefresh);
        }, rs -> {
            if (rs.getKey()) {
                List<String> pipelineConfigs = rs.getValue();
                // 清空后重新添加
                pipelinePanel.removeAll();
                if (CollectionUtils.isNotEmpty(pipelineConfigs)) {
                    pipelinePanel.setLayout(new GridLayout(-1, 4));
                    for (String value : pipelineConfigs) {
                        pipelinePanel.add(pipelineConfigNameCheckBox(value));
                    }

//                    JPanelUtils.setSize(centerPanel, new Dimension(700, 540 + (pipelineConfigs.size() / 4) * 20));
                } else {
                    pipelinePanel.setLayout(new BorderLayout());
                    pipelinePanel.add(new JLabel("    -暂无流水线配置信息, 请配置流水线信息并确保拥有权限后点击刷新"), BorderLayout.CENTER);
                }
            } else {
                pipelinePanel.removeAll();
                pipelinePanel.setLayout(new BorderLayout());
                JTextArea errTips = JPanelUtils.tips(JBColor.RED);
                errTips.setText(String.join("\n", rs.getValue()));
                pipelinePanel.add(errTips, BorderLayout.CENTER);
            }

            commitHelperFromDialog.repaint();
        });
    }

    private JBCheckBox pipelineConfigNameCheckBox(String value) {
        JBCheckBox checkBox = new JBCheckBox(value);
        checkBox.addActionListener(e -> {
            if (checkBox.isSelected()) {
                pipelineConfigNames.add(value);
            } else {
                pipelineConfigNames.remove(value);
            }
        });

        return checkBox;
    }

    private CommitMessageI getCommitMessage(AnActionEvent actionEvent) {
        return VcsDataKeys.COMMIT_MESSAGE_CONTROL.getData(actionEvent.getDataContext());
    }


    public Object[] getActiveIssueItems() {
        java.util.List<JiraIssueBaseDTO> activeIssueList = DevopsApiService.getActiveIssueListByParam(pluginMap);
        java.util.List<String> revertToOptionList = convertObjToString(activeIssueList);

        return revertToOptionList.toArray();
    }

    /**
     * 展示时需要将对象的各项关键值进行人工拼接
     *
     * @param objList
     * @return
     */
    private java.util.List<String> convertObjToString(java.util.List<JiraIssueBaseDTO> objList) {
        java.util.List<String> resultList = new ArrayList<>();
        if (objList == null || objList.isEmpty()) {
            resultList.add("当前无可用关联任务,请确认账号是否配置正确");
            return resultList;
        }
        for (JiraIssueBaseDTO issueBaseDTO : objList) {
            String optionValue = StringUtils.defaultString(issueBaseDTO.getKey()) + " " + StringUtils.defaultString(issueBaseDTO.getSummary()) + " " + StringUtils.defaultString(issueBaseDTO.getSprintName());
            resultList.add(optionValue);
        }
        return resultList;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return this.centerPanel;
    }

    /**
     * AI 生成提交信息
     */
    private void generateCommitMessageWithAI() {
        // 获取选中的提交类型和任务信息
        CommitType selectedCommitType = (CommitType) commitTypeComboBox.getSelectedItem();
        String commitTypeValue = selectedCommitType != null ? selectedCommitType.getType() : null;
        
        ActiveJiraIssueDetail issueDetail = (ActiveJiraIssueDetail) activeIssueListComboBox.getSelectedItem();
        String issueKey = issueDetail != null ? issueDetail.getIssue() : null;
        
        // 在 EDT 线程显示进度条
        ApplicationManager.getApplication().invokeLater(() -> {
            refreshProgressBar.setVisible(true);
            refreshProgressBar.setString("AI 正在生成提交信息...");
            centerPanel.revalidate();
            centerPanel.repaint();
        }, ModalityState.stateForComponent(centerPanel));
        
        // 在后台线程执行 AI 生成
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                // 调用 Kotlin 协程方法 (使用 runBlocking)
                String generatedMessage = (String) BuildersKt.runBlocking(
                    EmptyCoroutineContext.INSTANCE,
                    (coroutineScope, continuation) -> 
                        CommitMessageGenerator.INSTANCE.generateCommitMessage(
                            project,
                            commitTypeValue,
                            issueKey,
                            continuation
                        )
                );
                
                // 在 EDT 线程更新 UI
                ApplicationManager.getApplication().invokeLater(() -> {
                    refreshProgressBar.setVisible(false);
                    centerPanel.revalidate();
                    centerPanel.repaint();
                    
                    if (generatedMessage != null && !generatedMessage.isEmpty()) {
                        // 填充到输入框
                        WriteCommandAction.runWriteCommandAction(project, () -> {
                            commitMsg.getDocument().setText(generatedMessage);
                        });
                    } else {
                        LOG.warn("AI 生成提交信息失败: 返回空消息");
                    }
                }, ModalityState.stateForComponent(centerPanel));
                
            } catch (Exception ex) {
                // 在 EDT 线程处理错误
                ApplicationManager.getApplication().invokeLater(() -> {
                    refreshProgressBar.setVisible(false);
                    centerPanel.revalidate();
                    centerPanel.repaint();
                    LOG.warn("AI 生成提交信息异常", ex);
                }, ModalityState.stateForComponent(centerPanel));
            }
        });
    }

    private JPanel centerPanel;
    private JPanel pipelinePanel;
    private JProgressBar refreshProgressBar;
    //活跃任务列表数据
    private ComboBox<ActiveJiraIssueDetail> activeIssueListComboBox;
    private JLabel activeTaskTips;
    //代码提交类型列表数据
    private ComboBox<CommitType> commitTypeComboBox;
    //基于代码提交类型列表过滤输入框
    private Editor commitMsg;
    private JPanel issueTransitionPanel;
    private JPanel issueTransitionShowPanel;
    private JTextArea tipsArea;
    private final CheckBoxGroup<TransitionStatusInfo> checkBoxGroup = new CheckBoxGroup<>();
}
