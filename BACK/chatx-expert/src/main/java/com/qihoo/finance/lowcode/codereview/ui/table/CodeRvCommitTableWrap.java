package com.qihoo.finance.lowcode.codereview.ui.table;

import com.intellij.openapi.project.Project;
import com.qihoo.finance.lowcode.codereview.entity.CodeRvRepoNode;
import com.qihoo.finance.lowcode.codereview.entity.dto.CodeRvBranch;
import com.qihoo.finance.lowcode.codereview.entity.dto.CodeRvCommit;
import com.qihoo.finance.lowcode.codereview.util.CodeRvUtils;
import com.qihoo.finance.lowcode.common.ui.table.BaseJTableWrap;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.common.utils.LocalDateUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CodeRvCommentTable
 *
 * @author fengjinfu-jk
 * date 2023/11/2
 * @version 1.0.0
 * @apiNote CodeRvCommentTable
 */
public class CodeRvCommitTableWrap implements BaseJTableWrap {
    private final List<CodeRvCommit> selectCommits;
    private final CodeRvBranch branch;
    private final Project project;

    private final static String DATA_FORMAT = LocalDateUtils.FORMAT_DATE_TIME;

    public static final int COMMIT_SELECT = 0;
    public static final int COMMIT_MSG = 1;
    public static final int COMMIT_ID = 2;
    public static final int COMMIT_AUTHOR = 3;
    public static final int COMMIT_DATE = 4;

    public CodeRvCommitTableWrap(Project project, CodeRvBranch branch, List<CodeRvCommit> selectCommits) {
        this.project = project;
        this.selectCommits = selectCommits;
        this.branch = branch;
    }

    @Override
    public String[] getTableHeaders() {
        return new String[]{"", "备注", "commitId", "提交人", "时间"};
    }

    @Override
    public Object[][] getDefaultTableData() {
        // git commits
        CodeRvRepoNode repoNode = DataContext.getInstance(project).getSelectCodeRvRepo();
        List<CodeRvCommit> codeRvCommits = CodeRvUtils.queryRepoBranchCommits(repoNode, branch.getBranch());
        // select commits
        List<String> commitIds = selectCommits.stream().map(CodeRvCommit::getCommit).collect(Collectors.toList());

        List<Object[]> commitData = new ArrayList<>();
        for (CodeRvCommit codeRvCommit : codeRvCommits) {
            // "是否选中", "备注", "提交人", "时间", "id"
            commitData.add(new Object[]{commitIds.contains(codeRvCommit.getCommit()), codeRvCommit.getMessage(), codeRvCommit.getCommit(), codeRvCommit.getAuthor(), LocalDateUtils.convertToPatternString(codeRvCommit.getCreatedAt(), DATA_FORMAT)});
        }

        return commitData.toArray(Object[][]::new);
    }

    @Override
    public void addRow(ActionEvent e, JTable table) {

    }

    @Override
    public void configColumnProperties(JTable table, DefaultTableCellRenderer centerRenderer) {
        // "", "备注", "提交人", "时间", "id"

        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(JLabel.LEFT);

        table.getColumnModel().getColumn(COMMIT_SELECT).setCellRenderer(new CheckBoxRenderer());
        table.getColumnModel().getColumn(COMMIT_SELECT).setCellEditor(new CheckBoxEditor());
        table.getColumnModel().getColumn(COMMIT_SELECT).setPreferredWidth(50);
        table.getColumnModel().getColumn(COMMIT_SELECT).setMaxWidth(50);
        table.getColumnModel().getColumn(COMMIT_SELECT).setMinWidth(50);

        table.getColumnModel().getColumn(COMMIT_MSG).setCellRenderer(leftRenderer);
        table.getColumnModel().getColumn(COMMIT_MSG).setCellEditor(new TextCellEditor(false, SwingConstants.LEFT));

        // 隐藏ID列, 隐藏后需通过model获取值
        table.getColumnModel().getColumn(COMMIT_ID).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(COMMIT_ID).setCellEditor(new TextCellEditor());
        table.getColumnModel().getColumn(COMMIT_ID).setPreferredWidth(150);
        table.getColumnModel().getColumn(COMMIT_ID).setMaxWidth(150);
        table.getColumnModel().getColumn(COMMIT_ID).setMinWidth(150);

        table.getColumnModel().getColumn(COMMIT_AUTHOR).setCellRenderer(leftRenderer);
        table.getColumnModel().getColumn(COMMIT_AUTHOR).setCellEditor(new TextCellEditor(false, SwingConstants.LEFT));
        table.getColumnModel().getColumn(COMMIT_AUTHOR).setPreferredWidth(100);
        table.getColumnModel().getColumn(COMMIT_AUTHOR).setMaxWidth(100);
        table.getColumnModel().getColumn(COMMIT_AUTHOR).setMinWidth(100);

        table.getColumnModel().getColumn(COMMIT_DATE).setCellRenderer(leftRenderer);
        table.getColumnModel().getColumn(COMMIT_DATE).setCellEditor(new TextCellEditor(false, SwingConstants.LEFT));
        table.getColumnModel().getColumn(COMMIT_DATE).setPreferredWidth(140);
        table.getColumnModel().getColumn(COMMIT_DATE).setMaxWidth(140);
        table.getColumnModel().getColumn(COMMIT_DATE).setMinWidth(140);
    }

    //------------------------------------------------------------------------------------------------------------------
    public static List<CodeRvCommit> getSelectCommits(JTable table) {
        return getCommits(table, true);
    }

    public static List<CodeRvCommit> getAllCommits(JTable table) {
        return getCommits(table, false);
    }

    private static List<CodeRvCommit> getCommits(JTable table, boolean selected) {
        // "", "备注", "提交人", "时间", "id"

        List<CodeRvCommit> selectCommits = new ArrayList<>();
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        for (int row = 0; row < model.getRowCount(); row++) {
            String commitId = model.getValueAt(row, COMMIT_ID).toString();
            String author = model.getValueAt(row, COMMIT_AUTHOR).toString();
            String msg = model.getValueAt(row, COMMIT_MSG).toString();
            Date createAt = LocalDateUtils.formatDateStr(model.getValueAt(row, COMMIT_DATE).toString(), DATA_FORMAT);

            if (selected && (Boolean) model.getValueAt(row, COMMIT_SELECT)) {
                // 只获取选中
                selectCommits.add(CodeRvCommit.builder().commit(commitId).author(author).message(msg).createdAt(createAt).build());
            } else if (!selected) {
                // 获取所有
                selectCommits.add(CodeRvCommit.builder().commit(commitId).author(author).message(msg).createdAt(createAt).build());
            }
        }

        return selectCommits;
    }
}
