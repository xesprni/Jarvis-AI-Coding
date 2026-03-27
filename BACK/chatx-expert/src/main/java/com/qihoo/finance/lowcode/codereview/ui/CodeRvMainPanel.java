package com.qihoo.finance.lowcode.codereview.ui;

import com.intellij.openapi.project.Project;
import com.qihoo.finance.lowcode.codereview.entity.CodeRvRepoNode;
import com.qihoo.finance.lowcode.codereview.entity.CodeRvSprintNode;
import com.qihoo.finance.lowcode.codereview.entity.CodeRvTaskNode;
import com.qihoo.finance.lowcode.common.ui.ToolBarPanel;
import com.qihoo.finance.lowcode.common.ui.base.BasePanel;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.common.util.JTreeToolbarUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * CodeRvTreePanel
 *
 * @author fengjinfu-jk
 * date 2023/11/2
 * @version 1.0.0
 * @apiNote CodeRvTreePanel
 */
public class CodeRvMainPanel extends BasePanel {
    private final CodeRvTreePanel codeRvTreePanel;


    public CodeRvMainPanel(@NotNull Project project) {
        super(project);
        codeRvTreePanel = project.getService(CodeRvTreePanel.class);
    }

    @Override
    public Component createPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        panel.removeAll();
        JComponent treePanel = codeRvTreePanel.createPanel();
        treePanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        panel.add(treePanel, BorderLayout.CENTER);
        JTree codeRvTree = DataContext.getInstance(project).getCodeRvTree();

        // 搜索
        JPanel searchTree = JTreeToolbarUtils.createJTreeSearch(() -> codeRvTree).getSearchPanel();

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnPanel.setOpaque(false);
        // 全部展开
        JButton expandAll = JTreeToolbarUtils.createExpandAll(() -> codeRvTree, CodeRvSprintNode.class, CodeRvTaskNode.class);
        btnPanel.add(expandAll);

        // 全部收缩
        JButton collapseAll = JTreeToolbarUtils.createCollapseAll(() -> codeRvTree);
        btnPanel.add(collapseAll);
        // 刷新
        btnPanel.add(project.getService(ToolBarPanel.class).refreshButton());

        // 工具栏
        JPanel toolbar = JTreeToolbarUtils.createToolbarPanel();
        toolbar.add(searchTree, BorderLayout.CENTER);
        toolbar.add(btnPanel, BorderLayout.EAST);

        panel.add(toolbar, BorderLayout.NORTH);
        return panel;
    }
}
