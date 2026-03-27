package com.qihoo.finance.lowcode.codereview.handler;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.qihoo.finance.lowcode.codereview.dialog.CodeRvTaskDialog;
import com.qihoo.finance.lowcode.codereview.entity.CodeRvRepoNode;
import com.qihoo.finance.lowcode.codereview.entity.CodeRvTaskNode;
import com.qihoo.finance.lowcode.codereview.entity.dto.CodeRvTaskSaveVO;
import com.qihoo.finance.lowcode.codereview.toolwindow.CodeRvToolWindowManager;
import com.qihoo.finance.lowcode.codereview.ui.CodeRvTreePanel;
import com.qihoo.finance.lowcode.codereview.util.CodeRvUtils;
import com.qihoo.finance.lowcode.common.constants.OperateType;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.JMenuUtils;
import com.qihoo.finance.lowcode.common.util.JTreeLoadingUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

import static com.qihoo.finance.lowcode.common.util.LowCodeAppUtils.getErrMsg;

/**
 * CodeRvMouseAdapter
 *
 * @author fengjinfu-jk
 * date 2023/11/3
 * @version 1.0.0
 * @apiNote CodeRvMouseAdapter
 */
public class CodeRvMouseAdapter extends MouseAdapter {
    private final JTree tree;
    private final Project project;
    private final CodeRvTreePanel treePanel;
    private final CodeRvToolWindowManager codeRvManager = CodeRvToolWindowManager.getInstance();

    public CodeRvMouseAdapter(Project project, JTree tree, CodeRvTreePanel treePanel) {
        this.tree = tree;
        this.project = project;
        this.treePanel = treePanel;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        TreePath path = tree.getPathForLocation(e.getX(), e.getY());
        if (Objects.isNull(path)) return;
        int row = tree.getRowForLocation(e.getX(), e.getY());
        tree.setSelectionRow(row);
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

        if (SwingUtilities.isRightMouseButton(e)) {
            if (selectedNode != null) {
                JBPopupMenu popupMenu = createPopupMenu(selectedNode);
                popupMenu.show(tree, e.getX(), e.getY());
            }
        }

        if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
            if (selectedNode instanceof CodeRvTaskNode) {
                codeRvManager.showCodeRvToolWindow();
            }
        }
    }

    private JBPopupMenu createPopupMenu(DefaultMutableTreeNode node) {
        JBPopupMenu popupMenu = new JBPopupMenu();
        popupMenu.setBorder(BorderFactory.createLineBorder(JBColor.GRAY));
        popupMenu.getInsets().set(50, 50, 50, 50);
        popupMenu.add(Box.createVerticalStrut(8));

        if (node instanceof CodeRvRepoNode) {
            JBMenuItem collect = treePanel.createCollectMenuItem(CodeRvTreePanel.COLLECT, node);
            collect.setIcon(Icons.scaleToWidth(Icons.COLLECT, 14));
            popupMenu.add(collect);

            popupMenu.add(Box.createVerticalStrut(2));
            popupMenu.addSeparator();

            popupMenu.add(Box.createVerticalStrut(2));
            JBMenuItem createCodeReview = createCodeReview();
            createCodeReview.setIcon(Icons.scaleToWidth(Icons.TASK, 16));
            popupMenu.add(createCodeReview);

            popupMenu.add(Box.createVerticalStrut(6));
            JBMenuItem deleteTempBranch = deleteTempBranch();
            deleteTempBranch.setIcon(Icons.scaleToWidth(Icons.BRANCH, 16));
            popupMenu.add(deleteTempBranch);

            popupMenu.add(Box.createVerticalStrut(2));
        }
        if (node instanceof CodeRvTaskNode) {
            CodeRvTaskNode taskNode = (CodeRvTaskNode) node;

            popupMenu.add(Box.createVerticalStrut(2));
            if (taskNode.isOpen()) {
                popupMenu.add(Box.createVerticalStrut(2));
                JBMenuItem startCodeReview = startCodeReview();
                startCodeReview.setIcon(Icons.scaleToWidth(Icons.GIT_LAB, 17));
                popupMenu.add(startCodeReview);

                popupMenu.add(Box.createVerticalStrut(6));
                JBMenuItem closeCodeReview = closeCodeReview();
                closeCodeReview.setIcon(Icons.scaleToWidth(Icons.FINISH_CLOSE, 15));
                popupMenu.add(closeCodeReview);
            } else {
                JBMenuItem reopenCodeReview = reopenCodeReview();
                reopenCodeReview.setIcon(Icons.scaleToWidth(Icons.REOPEN, 16));
                popupMenu.add(reopenCodeReview);
            }

            popupMenu.add(Box.createVerticalStrut(2));
            popupMenu.addSeparator();
            popupMenu.add(Box.createVerticalStrut(2));

            JBMenuItem updateCodeReview = updateCodeReview();
            updateCodeReview.setIcon(Icons.scaleToWidth(Icons.API_EDIT, 17));
            popupMenu.add(updateCodeReview);

            // fixme: 权限原因, 暂不支持删除
//            popupMenu.add(Box.createVerticalStrut(6));
//            JBMenuItem deleteCodeReview = deleteCodeReview();
//            deleteCodeReview.setIcon(Icons.scaleToWidth(Icons.DELETE, 16));
//            popupMenu.add(deleteCodeReview);

            popupMenu.add(Box.createVerticalStrut(2));
            popupMenu.addSeparator();
        }

        return popupMenu;
    }

    private JBMenuItem createCodeReview() {
        JBMenuItem menuItem = new JBMenuItem(new AbstractAction("创建代码评审任务") {
            @Override
            public void actionPerformed(ActionEvent e) {
                CodeRvRepoNode repoNode = DataContext.getInstance(project).getSelectCodeRvRepo();
                new CodeRvTaskDialog(project, repoNode, OperateType.CREATE).show();
            }
        });

        // 设置自定义边距
        menuItem.setMargin(JBUI.insets(0, 10));
        return menuItem;
    }

    private JBMenuItem updateCodeReview() {
        JBMenuItem menuItem = new JBMenuItem(new AbstractAction("更新代码评审信息") {
            @Override
            public void actionPerformed(ActionEvent e) {
                CodeRvRepoNode repoNode = DataContext.getInstance(project).getSelectCodeRvRepo();
                new CodeRvTaskDialog(project, repoNode, OperateType.EDIT).show();
            }
        });

        // 设置自定义边距
        menuItem.setMargin(JBUI.insets(0, 10));
        return menuItem;
    }

    private JBMenuItem reopenCodeReview() {
        JBMenuItem menuItem = new JBMenuItem(new AbstractAction("重新打开代码评审") {
            @Override
            public void actionPerformed(ActionEvent e) {
                int logout = Messages.showDialog("\n重新打开后, 该代码评审任务状态将变更为 [进行中]", "重新打开代码评审", new String[]{"是", "否"}, Messages.NO, Icons.scaleToWidth(Icons.REOPEN, 60));
                if (logout == Messages.YES) {
                    // 执行删除
                    DataContext dataContext = DataContext.getInstance(project);
                    CodeRvRepoNode repoNode = dataContext.getSelectCodeRvRepo();
                    Result<CodeRvTaskSaveVO> result = CodeRvUtils.reopenCodeReviewTask(repoNode, dataContext.getSelectCodeRvTask());
                    if (result.isSuccess()) {
                        CodeRvTaskNode codeRvTask = dataContext.getSelectCodeRvTask();

                        int select = Messages.showDialog(project, "\n评审任务已重新打开 !\n\n", "保存成功", new String[]{"查看任务详情", "确定"}, 1, Icons.scaleToWidth(Icons.SUCCESS, 60));
                        if (select == 0 && Objects.nonNull(codeRvTask) && StringUtils.isNotEmpty(codeRvTask.getWebUrl())) {
                            BrowserUtil.browse(codeRvTask.getWebUrl());
                        }

                        // reloadTree
                        JTreeLoadingUtils.loading(true, tree, repoNode, () -> CodeRvUtils.queryCodeRvTaskSprints(repoNode));
                        return;
                    }

                    Messages.showMessageDialog("\n" + getErrMsg(result), "状态修改失败", Icons.scaleToWidth(Icons.FAIL, 60));
                }
            }
        });

        // 设置自定义边距
        menuItem.setMargin(JBUI.insets(0, 10));
        return menuItem;
    }

    private JBMenuItem deleteCodeReview() {
        return JMenuUtils.createDeleteMenu(project, "删除", () -> {
            // 执行删除
            DataContext dataContext = DataContext.getInstance(project);
            CodeRvRepoNode repoNode = dataContext.getSelectCodeRvRepo();
            Result<CodeRvTaskSaveVO> result = CodeRvUtils.deleteCodeReviewTask(repoNode, dataContext.getSelectCodeRvTask());
            if (result.isSuccess()) {
                // reloadTree
                JTreeLoadingUtils.loading(true, tree, repoNode, () -> CodeRvUtils.queryCodeRvTaskSprints(repoNode));
                return;
            }

            Messages.showMessageDialog("\n" + getErrMsg(result), "删除失败", Icons.scaleToWidth(Icons.FAIL, 60));
        });
    }

    private JBMenuItem closeCodeReview() {
        JBMenuItem menuItem = new JBMenuItem(new AbstractAction("完成代码评审") {
            @Override
            public void actionPerformed(ActionEvent e) {
                int logout = Messages.showDialog("\n确认标记该代码评审任务为 [已完成] ?", "完成代码评审任务", new String[]{"是", "否"}, Messages.YES, Icons.scaleToWidth(Icons.FINISH_CLOSE, 60));
                if (logout == Messages.YES) {
                    // 状态更新
                    DataContext dataContext = DataContext.getInstance(project);
                    CodeRvRepoNode repoNode = dataContext.getSelectCodeRvRepo();
                    Result<CodeRvTaskSaveVO> result = CodeRvUtils.finishCodeReviewTask(repoNode, dataContext.getSelectCodeRvTask());
                    if (result.isSuccess()) {
                        // reloadTree
                        JTreeLoadingUtils.loading(true, tree, repoNode, () -> CodeRvUtils.queryCodeRvTaskSprints(repoNode));
                        return;
                    }

                    Messages.showMessageDialog("\n" + getErrMsg(result), "状态修改失败", Icons.scaleToWidth(Icons.FAIL, 60));
                }
            }
        });

        // 设置自定义边距
        menuItem.setMargin(JBUI.insets(0, 10));
        return menuItem;
    }

    private JBMenuItem startCodeReview() {
        JBMenuItem menuItem = new JBMenuItem(new AbstractAction("进行代码评审") {
            @Override
            public void actionPerformed(ActionEvent e) {
                CodeRvTaskNode taskNode = DataContext.getInstance(project).getSelectCodeRvTask();
                if (StringUtils.isNotEmpty(taskNode.getWebUrl())) BrowserUtil.browse(taskNode.getWebUrl());
            }
        });

        // 设置自定义边距
        menuItem.setMargin(JBUI.insets(0, 10));
        return menuItem;
    }

    private JBMenuItem deleteTempBranch() {
        return JMenuUtils.createDeleteMenu(project, "删除临时分支", "您正在删除临时分支\n\n该操作将会删除当前项目中由代码评审任务创建的临时分支\n请输入 “确认删除” 完成删除 !", () -> {
            // 执行删除
            DataContext dataContext = DataContext.getInstance(project);
            Result<Object> result = CodeRvUtils.depTempBranch(dataContext.getSelectCodeRvRepo());
            if (!result.isSuccess()) {
                Messages.showMessageDialog("\n" + getErrMsg(result), "删除失败", Icons.scaleToWidth(Icons.FAIL, 60));
            }
        });
    }
}
