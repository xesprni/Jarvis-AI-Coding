package com.qihoo.finance.lowcode.apitrack.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTabbedPane;
import com.qihoo.finance.lowcode.common.action.UserInfoAction;
import com.qihoo.finance.lowcode.common.ui.base.BasePanel;
import com.qihoo.finance.lowcode.common.util.JTreeToolbarUtils;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;

/**
 * ApiTreePanel
 *
 * @author fengjinfu-jk
 * date 2023/8/31
 * @version 1.0.0
 * @apiNote ApiTreePanel
 */
public class ApiMainPanel extends BasePanel {

    public final static String API_MASTER = "AI文档-Master分支";
    public final static String API_BRANCH = "AI文档-其它分支";
    public final static String API_MANUAL = "YAPI文档";
    private final ApiTreePanel apiTreePanelFactory;
    private final AiApiTreePanel masterTreePanel;
    private final AiApiTreePanel branchTreePanel;
    @Getter
    private JTabbedPane tabbedPane;
    private Object[] treePanels = new Object[3];


    public ApiMainPanel(@NotNull Project project) {
        super(project);
        masterTreePanel = new AiApiTreePanel(project, AiApiTreePanel.BRANCH_MASTER);
        branchTreePanel = new AiApiTreePanel(project, AiApiTreePanel.BRANCH_OTHER);
        apiTreePanelFactory = project.getService(ApiTreePanel.class);
        treePanels[0] = masterTreePanel;
        treePanels[1] = branchTreePanel;
        treePanels[2] = apiTreePanelFactory;
    }

    @Override
    public JComponent createPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.removeAll();
        tabbedPane = new JBTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.BOTTOM);
        tabbedPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        panel.add(tabbedPane, BorderLayout.CENTER);

        JComponent masterPanel = masterTreePanel.getComponent();
        JComponent branchPanel = branchTreePanel.getComponent();
        JComponent manualPanel = apiTreePanelFactory.createPanel();
        manualPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        tabbedPane.addTab(API_MASTER, AllIcons.Nodes.Interface, masterPanel);
        tabbedPane.addTab(API_BRANCH, AllIcons.Nodes.Interface, branchPanel);
        tabbedPane.addTab(API_MANUAL, AllIcons.Nodes.Interface, manualPanel);
        tabbedPane.addChangeListener(this::tabbedPaneChanged);

        JPanel toolbar = createToolBar();
        panel.add(toolbar, BorderLayout.NORTH);

        panel.repaint();
        SwingUtilities.invokeLater(() -> tabbedPane.setSelectedIndex(2));
        return panel;
    }

    public void show() {
        reload(false);
    }

    public void reload(boolean forceReload) {
        int i = tabbedPane.getSelectedIndex();
        if (i < 0) {
            tabbedPane.setSelectedIndex(0);
            return;
        }
        Object treePanel = treePanels[i];
        if (treePanel instanceof AiApiTreePanel) {
            ((AiApiTreePanel) treePanel).show(false, forceReload);
        } else if (treePanel instanceof ApiTreePanel) {
            ((ApiTreePanel) treePanel).loadApiTree(false);
        }
    }

    private void tabbedPaneChanged(ChangeEvent e) {
        int i = tabbedPane.getSelectedIndex();
        if (i < 0) {
            return;
        }
        Object treePanel = treePanels[i];
        if (treePanel instanceof AiApiTreePanel) {
            ((AiApiTreePanel) treePanel).show();
        }
    }

    private JTree getSelectedTree() {
        int i = tabbedPane.getSelectedIndex();
        if (i < 0) {
            return null;
        }
        Object treePanel = treePanels[i];
        if (treePanel instanceof AiApiTreePanel aiApiTreePanel) {
            return aiApiTreePanel.getTree();
        } else if (treePanel instanceof ApiTreePanel apiTreePanel) {
            return apiTreePanel.getTree();
        }
        return null;
    }


    /**
     * 顶部搜索工具条
     * @return
     */
    private JPanel createToolBar() {
        JPanel searchTree = JTreeToolbarUtils.createJTreeSearch(this::getSelectedTree).getSearchPanel();
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnPanel.setOpaque(false);
        // 全部展开
//        JButton expandAll = JTreeToolbarUtils.createExpandAll(this::getSelectedTree, ApiGroupNode.class, ApiNode.class);
//        expandAll.setToolTipText("展开所有接口");
//        btnPanel.add(expandAll);

        // 全部收缩
        JButton collapseAll = JTreeToolbarUtils.createCollapseAll(this::getSelectedTree);
        collapseAll.setToolTipText("收缩所有接口");
        btnPanel.add(collapseAll);
        // 刷新
        btnPanel.add(project.getService(UserInfoAction.class).refreshButton());

        JPanel toolbar = JTreeToolbarUtils.createToolbarPanel();
        toolbar.add(searchTree, BorderLayout.CENTER);
        toolbar.add(btnPanel, BorderLayout.EAST);
        return toolbar;
    }

}
