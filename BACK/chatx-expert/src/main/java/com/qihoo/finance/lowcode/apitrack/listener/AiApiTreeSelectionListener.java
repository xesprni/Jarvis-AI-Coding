package com.qihoo.finance.lowcode.apitrack.listener;

import com.intellij.openapi.project.Project;
import com.qihoo.finance.lowcode.apitrack.entity.ai.AiProjectNode;
import com.qihoo.finance.lowcode.apitrack.util.ApiDesignUtils;
import com.qihoo.finance.lowcode.common.listener.TreeSelectionListenerChain;
import com.qihoo.finance.lowcode.common.util.JTreeLoadingUtils;

import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * 节点被鼠标选中时，加载菜单
 */
public class AiApiTreeSelectionListener extends TreeSelectionListenerChain {

    private final JTree tree;

    public AiApiTreeSelectionListener(Project project, JTree tree) {
        super(project);
        this.tree = tree;
    }

    @Override
    public void handlerValueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = currentNode(e);
        if (node instanceof AiProjectNode) {
            // 项目层，查询项目下的菜单（文件夹）
            JTreeLoadingUtils.loading(
                true,
                tree,
                node,
                () -> ApiDesignUtils.yapiMenus(((AiProjectNode)node).getId())
            );
        }
    }
}
