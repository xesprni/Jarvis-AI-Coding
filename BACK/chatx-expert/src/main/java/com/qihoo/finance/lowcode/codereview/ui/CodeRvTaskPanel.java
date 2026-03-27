package com.qihoo.finance.lowcode.codereview.ui;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.table.JBTable;
import com.qihoo.finance.lowcode.codereview.dialog.CodeRvTaskDialog;
import com.qihoo.finance.lowcode.codereview.entity.CodeRvRepoNode;
import com.qihoo.finance.lowcode.codereview.entity.CodeRvSprintNode;
import com.qihoo.finance.lowcode.codereview.entity.CodeRvTaskNode;
import com.qihoo.finance.lowcode.codereview.entity.dto.CodeRvComment;
import com.qihoo.finance.lowcode.codereview.entity.dto.CodeRvDiscussion;
import com.qihoo.finance.lowcode.codereview.ui.table.CodeRvTaskTableWrap;
import com.qihoo.finance.lowcode.codereview.util.CodeRvUtils;
import com.qihoo.finance.lowcode.codereview.util.LineMarkerUtils;
import com.qihoo.finance.lowcode.common.constants.OperateType;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.ui.base.BasePanel;
import com.qihoo.finance.lowcode.common.ui.base.RoundedPanel;
import com.qihoo.finance.lowcode.common.ui.base.VFlowLayout;
import com.qihoo.finance.lowcode.common.util.*;
import com.qihoo.finance.lowcode.common.utils.LocalDateUtils;
import com.qihoo.finance.lowcode.common.ui.CustomHeightTabbedPaneUI;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Objects;

import static com.qihoo.finance.lowcode.common.util.LowCodeAppUtils.getErrMsg;

/**
 * CodeRvDetailPanel
 *
 * @author fengjinfu-jk
 * date 2023/11/2
 * @version 1.0.0
 * @apiNote CodeRvDetailPanel
 */
@Slf4j
public class CodeRvTaskPanel extends BasePanel {
    private final Project project;

    public CodeRvTaskPanel(@NotNull Project project) {
        super(project);
        this.project = project;
    }

    @Override
    public JComponent createPanel() {
        JPanel taskPanel = new JPanel(new BorderLayout());
        JProgressBar progressBar = JTreeToolbarUtils.createIndeterminateProgressBar();

        // 详情内容
        new SwingWorker<JBTabbedPane, JBTabbedPane>() {
            @Override
            protected JBTabbedPane doInBackground() {
                progressBar.setString("查询评审任务详情中...");
                taskPanel.add(progressBar, BorderLayout.SOUTH);

                return null;
            }

            @SneakyThrows
            @Override
            protected void done() {
                taskPanel.add(createTitle(), BorderLayout.NORTH);
                taskPanel.add(createTaskTabPanel(), BorderLayout.CENTER);
                progressBar.setVisible(false);

                super.done();
            }
        }.execute();

        return taskPanel;
    }

    private JBTabbedPane createTaskTabPanel() {
        JBTabbedPane tab = new JBTabbedPane();
        tab.setUI(new CustomHeightTabbedPaneUI());
        int maxWidth = Toolkit.getDefaultToolkit().getScreenSize().width - 100;

        // 评论
        JPanel commentPanel = getCommentPanel(maxWidth);
        tab.addTab(" 评论  ", Icons.scaleToWidth(Icons.COMMENT, 20), new JBScrollPane(commentPanel));

        // 详情
        JBScrollPane dialogScrollPane = getTaskDetailPanel(maxWidth);
        tab.addTab(" 代码评审详情  ", Icons.scaleToWidth(Icons.TASK, 20), dialogScrollPane);

        return tab;
    }

    @NotNull
    private JPanel getCommentPanel(int maxWidth) {
        JPanel commentPanel = new JPanel(new VFlowLayout());

        DataContext dataContext = DataContext.getInstance(project);
        CodeRvRepoNode repoNode = dataContext.getSelectCodeRvRepo();
        CodeRvTaskNode taskNode = dataContext.getSelectCodeRvTask();
        List<CodeRvDiscussion> discussions = CodeRvUtils.queryCodeReviewTaskDiscussions(repoNode, taskNode);
        for (CodeRvDiscussion discussion : discussions) {
            JPanel discussionPanel = new JPanel(new BorderLayout());
            JPanel centerDiscussion = createDiscussionPanel(discussion);
            JPanel titlePanel = createTitlePanel(discussion, centerDiscussion);

            discussionPanel.add(titlePanel, BorderLayout.NORTH);
            discussionPanel.add(centerDiscussion, BorderLayout.CENTER);
            commentPanel.add(discussionPanel);
        }

        return commentPanel;
    }

    private JPanel createDiscussionPanel(CodeRvDiscussion discussion) {
        JPanel discussionPanel = new JPanel(new VFlowLayout(VFlowLayout.TOP));
        int i = 0;
        for (CodeRvComment comment : discussion.getComments()) {
            Color background = EditorComponentUtils.BACKGROUND;
            JPanel commentPanel = new RoundedPanel(background, 20);
            commentPanel.setLayout(new VFlowLayout(VFlowLayout.TOP));

            JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel user = new JLabel();
            user.setIcon(Icons.scaleToWidth(Icons.LOGIN_USER, 16));
            user.setFont(new Font("微软雅黑", Font.BOLD, 12));
            user.setText(comment.getAuthor());

            JLabel date = new JLabel(LocalDateUtils.convertToPatternString(comment.getCreatedAt(), LocalDateUtils.FORMAT_DATE_TIME));
            date.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
            userPanel.add(user);
            userPanel.add(date);

            JTextPane commentMsg = new JTextPane();
            commentMsg.setText(comment.getBody());
            commentMsg.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));

            commentPanel.add(userPanel);
            commentPanel.add(commentMsg);
            discussionPanel.add(commentPanel);

            userPanel.setBackground(background);
            commentMsg.setBackground(background);
        }

        return discussionPanel;
    }

    private JPanel createTitlePanel(CodeRvDiscussion discussion, JPanel centerDiscussion) {
        JPanel overviewPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel overview = new JLabel(discussion.getOverview());

        overview.setIcon(Icons.scaleToWidth(Icons.COMMENT_LIGHT, 18));
        overview.setFont(new Font("微软雅黑", Font.BOLD, 13));
        overviewPanel.add(overview);
        overviewPanel.add(expendOrCollapse(centerDiscussion));

        if (CollectionUtils.isNotEmpty(discussion.getComments())) {
            JButton location = new JButton();
            location.setText("<html><u>" + discussion.getComments().get(0).getPosition().getNewPath() + "</u></html>");
            location.setBorderPainted(false);
            location.setContentAreaFilled(false);
            location.setIcon(Icons.scaleToWidth(Icons.LOCATION, 18));
            location.setBorder(BorderFactory.createEmptyBorder(0, 50, 0, 0));

            location.addActionListener(e -> locationAtPosition(project, discussion));
            overviewPanel.add(location);
        }

        // status btn
        JPanel solvePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JLabel solveStatus = new JLabel("<html>状态:    " + (discussion.isResolved() ? "已解决</html>" : "<span style=\"color: red;\">未解决</span></html>"));
        solvePanel.add(solveStatus);

        JButton solveBtn = new JButton("<html><u style=\"color: rgb(88,157,246);\">" + (discussion.isResolved() ? "标记为未解决" : "标记为已解决") + "</u></html>");
        solveBtn.setBorderPainted(false);
        solveBtn.setContentAreaFilled(false);
        Icon btnIcon = discussion.isResolved() ? Icons.scaleToWidth(Icons.FAIL, 16) : Icons.scaleToWidth(Icons.SUCCESS, 16);
        solveBtn.setIcon(btnIcon);
        solveBtn.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 30));

        // action
        solveBtn.addActionListener(e -> updateDiscussionSolveStatus(discussion, solveBtn, solveStatus));
        solvePanel.add(solveBtn);

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.add(overviewPanel, BorderLayout.CENTER);
        titlePanel.add(solvePanel, BorderLayout.EAST);

        return titlePanel;
    }

    private Component expendOrCollapse(JPanel centerDiscussion) {
        JButton button = new JButton();
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setIcon(expendOrCollapseIcon(centerDiscussion));
        button.setText(expendOrCollapseTxt(centerDiscussion));

        button.addActionListener(e -> {
            centerDiscussion.setVisible(!centerDiscussion.isVisible());
            button.setIcon(expendOrCollapseIcon(centerDiscussion));
            button.setText(expendOrCollapseTxt(centerDiscussion));
        });

        return button;
    }

    private String expendOrCollapseTxt(JPanel centerDiscussion) {
        return "<html><u style=\"color: rgb(88,157,246);\">" + (centerDiscussion.isVisible() ? "收起所有评论" : "展开所有评论") + "<html><u style=\"color: rgb(88,157,246);\">";
    }

    private Icon expendOrCollapseIcon(JPanel centerDiscussion) {
        Icon collapse = Icons.scaleToWidth(Icons.Actions.Collapseall, 16);
        Icon expand = Icons.scaleToWidth(Icons.Actions.Expandall, 16);

        return centerDiscussion.isVisible() ? collapse : expand;
    }

    @NotNull
    private JBScrollPane getTaskDetailPanel(int maxWidth) {
        // 任务详情
        JPanel taskPanel = new JPanel(new BorderLayout());
        CodeRvTaskTableWrap taskTableWrap = new CodeRvTaskTableWrap();
        JBTable taskTable = taskTableWrap.createStyleTable(maxWidth, null);
        taskPanel.add(new JBScrollPane(taskTable), BorderLayout.CENTER);

        // 评论
        CodeRvTaskDialog dialog = new CodeRvTaskDialog(project, DataContext.getInstance(project).getSelectCodeRvRepo(), OperateType.VIEW);
        JComponent taskDetailPanel = dialog.createCenterPanel();
        taskDetailPanel.setPreferredSize(new Dimension(-1, taskDetailPanel.getPreferredSize().height + 50));
        return new JBScrollPane(taskDetailPanel);
    }

    private JLabel createTitle() {
        DataContext dataContext = DataContext.getInstance(project);

        // 层级
        CodeRvRepoNode repoNode = dataContext.getSelectCodeRvRepo();
        CodeRvSprintNode sprintNode = dataContext.getSelectCodeRvSprint();
        CodeRvTaskNode taskNode = dataContext.getSelectCodeRvTask();

        JLabel title = new JLabel();
        title.setText(String.format("%s > %s > %s", repoNode, sprintNode, taskNode));
        title.setIcon(Icons.scaleToWidth(Icons.TASK, 22));
        title.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        return title;
    }

    //------------------------------------------------------------------------------------------------------------------

    private void updateDiscussionSolveStatus(CodeRvDiscussion discussion, JButton solveBtn, JLabel solveStatus) {
        int option = Messages.showDialog(String.format("\n确认标记该讨论为 [%s] ?", discussion.isResolved() ? "未解决" : "已解决"), "更新解决状态", new String[]{"是", "否"}, Messages.YES, Icons.scaleToWidth(Icons.SUCCESS, 60));
        if (option == Messages.YES) {
            // 状态更新
            Result<?> result = CodeRvUtils.updateDiscussionSolveStatus(discussion);
            if (result.isSuccess()) {
                // reload
                discussion.setResolved(!discussion.isResolved());
                String statusTxt = "<html>状态:    " + (discussion.isResolved() ? "已解决</html>" : "<span style=\"color: red;\">未解决</span></html>");
                solveStatus.setText(statusTxt);

                String btnTxt = "<html><u style=\"color: rgb(88,157,246);\">" + (discussion.isResolved() ? "标记为未解决" : "标记为已解决") + "</u></html>";
                solveBtn.setText(btnTxt);
                Icon btnIcon = discussion.isResolved() ? Icons.scaleToWidth(Icons.FAIL, 16) : Icons.scaleToWidth(Icons.SUCCESS, 16);
                solveBtn.setIcon(btnIcon);

                return;
            }

            Messages.showMessageDialog("\n" + getErrMsg(result), "状态修改失败", Icons.scaleToWidth(Icons.FAIL, 60));
        }
    }

    @SuppressWarnings("all")
    public static void locationAtPosition(Project project, CodeRvDiscussion discussion) {
        if (Objects.isNull(discussion)) return;
        if (CollectionUtils.isEmpty(discussion.getComments())) return;

        // 打开文件并定位到评论行号
        CodeRvComment comment = discussion.getComments().get(0);
        CodeRvComment.Position position = comment.getPosition();
        VirtualFile virtualFile = project.getBaseDir().findFileByRelativePath(position.getNewPath());

        if (Objects.isNull(virtualFile)) {
            NotifyUtils.notify("找不到文件 " + position.getNewPath(), NotificationType.WARNING);
            return;
        }

        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        int line = position.getNewLine() > 0 ? position.getNewLine() : position.getOldLine();
        fileEditorManager.openTextEditor(new OpenFileDescriptor(project, virtualFile, Math.max(line - 1, 0), 0), true);

        // 标记
        Editor editor = fileEditorManager.getSelectedTextEditor();
        LineMarkerUtils.addLineMarkup(editor, line, Icons.scaleToWidth(Icons.COMMENT_LIGHT, 16), comment.getBody(), null);
    }
}
