package com.qihoo.finance.lowcode.apitrack.listener;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.qihoo.finance.lowcode.apitrack.action.AiApiDesignAction;
import com.qihoo.finance.lowcode.apitrack.action.ApiDesignAction;
import com.qihoo.finance.lowcode.apitrack.dialog.AiApiDesignDialog;
import com.qihoo.finance.lowcode.apitrack.dialog.ApiDesignDialog;
import com.qihoo.finance.lowcode.apitrack.entity.ApiNode;
import com.qihoo.finance.lowcode.apitrack.entity.ai.AiApiNode;
import com.qihoo.finance.lowcode.apitrack.entity.ai.AiProjectNode;
import com.qihoo.finance.lowcode.apitrack.ui.AiApiTreePanel;
import com.qihoo.finance.lowcode.common.constants.OperateType;
import com.qihoo.finance.lowcode.common.util.Icons;
import lombok.RequiredArgsConstructor;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

@RequiredArgsConstructor
public class AiApiTreeMouseListener extends MouseAdapter {

    private final Project project;
    private final JTree tree;
    private final AiApiTreePanel treePanel;


    @Override
    public void mouseClicked(MouseEvent e) {
        TreePath path = tree.getPathForLocation(e.getX(), e.getY());
        if (path == null) {
            return;
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (SwingUtilities.isRightMouseButton(e)) {
            // 鼠标右键弹出菜单
            showPopupMenu(node, e);
        } else if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
            // 鼠标左键双击，展示接口详情页面
            if (node instanceof AiApiNode apiNode) {
                // 展开详情
                new AiApiDesignDialog(project, OperateType.VIEW, apiNode).show();
            }
        }
    }

    /**
     * 展示右键菜单
     * @param node
     */
    private void showPopupMenu(DefaultMutableTreeNode node, MouseEvent e) {
        JBPopupMenu popupMenu = new JBPopupMenu();
        popupMenu.setBorder(BorderFactory.createLineBorder(JBColor.GRAY));
        popupMenu.getInsets().set(50, 50, 50, 50);
        popupMenu.add(Box.createVerticalStrut(8));
        boolean needShow = false;

        if (node instanceof AiProjectNode) {
            needShow = true;
            JBMenuItem collect = treePanel.createCollectMenuItem(AiApiTreePanel.COLLECT_TYPE, node);
            collect.setIcon(Icons.scaleToWidth(Icons.COLLECT, 14));
            popupMenu.add(collect);

            popupMenu.add(Box.createVerticalStrut(2));

        } else if (node instanceof AiApiNode apiNode) {
            needShow = true;
            JBMenuItem apiDetail = createMenuItem(
                new AiApiDesignAction("接口详情", project, OperateType.VIEW, apiNode)
            );
            apiDetail.setIcon(Icons.scaleToWidth(Icons.API, 16));
            popupMenu.add(Box.createVerticalStrut(2));
            popupMenu.add(apiDetail);
        }

        popupMenu.addSeparator();
        popupMenu.add(Box.createVerticalStrut(8));
        if (needShow) {
            popupMenu.show(tree, e.getX(), e.getY());
        }
    }

    private JBMenuItem createMenuItem(Action action) {
        JBMenuItem menuItem = new JBMenuItem(action);
        // 设置自定义边距
        menuItem.setMargin(JBUI.insets(0, 10));
        return menuItem;
    }

}
