package com.qihoo.finance.lowcode.codereview.ui.table;

import com.intellij.openapi.project.Project;
import com.qihoo.finance.lowcode.codereview.entity.dto.CodeRvComment;
import com.qihoo.finance.lowcode.codereview.entity.dto.CodeRvDiscussion;
import com.qihoo.finance.lowcode.common.ui.table.BaseJTableWrap;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.common.utils.LocalDateUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * CodeRvCommentTable
 *
 * @author fengjinfu-jk
 * date 2023/11/2
 * @version 1.0.0
 * @apiNote CodeRvCommentTable
 */
public class CodeRvCommentTableWrap implements BaseJTableWrap {
    public static final int COLUMN_POSITION = 1;
    public static final int COLUMN_SOLVE = 2;
    public static final int COLUMN_COMMENT_OBJ = 5;
    private final Project project;
    private final CodeRvDiscussion discussion;

    public CodeRvCommentTableWrap(Project project, CodeRvDiscussion discussion) {
        this.project = project;
        this.discussion = discussion;
    }

    @Override
    public String[] getTableHeaders() {
        return new String[]{"评论人", "评论/意见", "代码位置", "评论时间", "comment对象"};
    }

    @Override
    public Object[][] getDefaultTableData() {
        List<Object[]> tableData = new ArrayList<>();
        List<CodeRvComment> codeRvComments = discussion.getComments();
        for (CodeRvComment codeRvComment : codeRvComments) {
            // "代码位置", "评论/意见", "评论人", "评论时间"
            tableData.add(new Object[]{
                    codeRvComment.getBody(),
                    codeRvComment.getPosition().getNewPath(),
                    codeRvComment.getAuthor(),
                    LocalDateUtils.convertToPatternString(codeRvComment.getCreatedAt(), LocalDateUtils.FORMAT_DATE_TIME),
                    codeRvComment
            });
        }

        return tableData.toArray(Object[][]::new);
    }

    @Override
    public void addRow(ActionEvent e, JTable table) {

    }

    @Override
    public void configColumnProperties(JTable table, DefaultTableCellRenderer centerRenderer) {
        // "评论/意见", "代码位置", "是否解决", "评论人", "评论时间"

        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(JLabel.LEFT);

        table.getColumnModel().getColumn(0).setCellRenderer(leftRenderer);
        table.getColumnModel().getColumn(0).setCellEditor(new TextCellEditor(false, SwingConstants.LEFT));

        table.getColumnModel().getColumn(1).setCellRenderer(leftRenderer);
        table.getColumnModel().getColumn(1).setCellEditor(new TextCellEditor(false, SwingConstants.LEFT));

        table.getColumnModel().getColumn(2).setCellRenderer(leftRenderer);
        table.getColumnModel().getColumn(2).setCellEditor(new TextCellEditor(false, SwingConstants.LEFT));
        table.getColumnModel().getColumn(2).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setMaxWidth(150);
        table.getColumnModel().getColumn(2).setMinWidth(150);

        table.getColumnModel().getColumn(3).setCellRenderer(leftRenderer);
        table.getColumnModel().getColumn(3).setCellEditor(new TextCellEditor(false, SwingConstants.LEFT));
        table.getColumnModel().getColumn(3).setPreferredWidth(150);
        table.getColumnModel().getColumn(3).setMaxWidth(150);
        table.getColumnModel().getColumn(3).setMinWidth(150);

        table.removeColumn(table.getColumnModel().getColumn(4));
    }
}
