package com.qihoo.finance.lowcode.common.ui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBInsets;
import com.qihoo.finance.lowcode.aiquestion.ui.QuestionPanel;
import com.qihoo.finance.lowcode.apitrack.ui.ApiMainPanel;
import com.qihoo.finance.lowcode.codereview.ui.CodeRvAuthPanel;
import com.qihoo.finance.lowcode.codereview.ui.CodeRvMainPanel;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.design.ui.DatabaseBasePanel;
import com.qihoo.finance.lowcode.design.ui.DatabaseMainPanel;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * MainPanel
 *
 * @author fengjinfu-jk
 * date 2023/8/18
 * @version 1.0.0
 * @apiNote MainPanel
 */
@Getter
@Slf4j
public class TabPanel extends DatabaseBasePanel {
    private final Project project;
    private JBTabbedPane tabbedPane;
    public static final int ASK_AI_INDEX = 0;
    public static final int DB_INDEX = 1;
    public static final int API_INDEX = 2;
    public static final int CRV_INDEX = 3;

    public TabPanel(@NotNull Project project) {
        super(project);
        this.project = project;
    }

    @Override
    public JComponent createPanel() {
        log.info("call TabPanel.createPanel");
        return createTabPanel();
    }

    public static void setSelectIndex(int index) {
        JBTabbedPane tabbed = getInstance().getTabbedPane();
        if (tabbed.getTabCount() > index) {
            tabbed.setSelectedIndex(index);
        }
    }

    public static TabPanel getInstance() {
        return ProjectUtils.getCurrProject().getService(TabPanel.class);
    }

    private synchronized JBTabbedPane createTabPanel() {
        log.info("call TabPanel.initTabPanel");

        tabbedPane = new JBTabbedPane();
        tabbedPane.setTabComponentInsets(JBInsets.create(0, 0));

        Component questionPanel = project.getService(QuestionPanel.class).createPanel();
        tabbedPane.insertTab("Ask AI", Icons.scaleToWidth(Icons.ASK_AI_LIGHT, 18), questionPanel, "Ask AI", ASK_AI_INDEX);

        DatabaseMainPanel databaseMainPanel = project.getService(DatabaseMainPanel.class);
        JComponent databasePanel = databaseMainPanel.createPanel();
        tabbedPane.insertTab("数据库生成代码", Icons.scaleToWidth(Icons.DB_GEN, 18), databasePanel, "数据库生成代码", DB_INDEX);

        ApiMainPanel apiMainPanel = project.getService(ApiMainPanel.class);
        tabbedPane.insertTab("接口生成代码", Icons.scaleToWidth(Icons.API, 18), apiMainPanel.createPanel(), "接口生成代码", API_INDEX);

        // 代码评审配置页面
//        CodeRvAuthPanel authPanel = project.getService(CodeRvAuthPanel.class);
//        tabbedPane.insertTab("代码评审", Icons.scaleToWidth(Icons.GIT_LAB, 20), authPanel.createPanel(), "代码评审", CRV_INDEX);
//        authPanel.executeVerifyTokenWorker();

        tabbedPane.addChangeListener(r -> {
            int index = tabbedPane.getSelectedIndex();
            if (tabbedPane.getTabCount() > DB_INDEX) {
                Component tabComponentAt = tabbedPane.getTabComponentAt(DB_INDEX);
                if (Objects.isNull(tabComponentAt)) return;
                ((JLabel) tabbedPane.getTabComponentAt(DB_INDEX)).setIcon(index == DB_INDEX ?
                        Icons.scaleToWidth(Icons.DB_GEN_LIGHT, 18) : Icons.scaleToWidth(Icons.DB_GEN, 18));
            }

            if (tabbedPane.getTabCount() > API_INDEX) {
                Component tabComponentAt = tabbedPane.getTabComponentAt(API_INDEX);
                if (Objects.isNull(tabComponentAt)) return;
                ((JLabel) tabbedPane.getTabComponentAt(API_INDEX)).setIcon(index == API_INDEX ?
                        Icons.scaleToWidth(Icons.API_GEN_LIGHT, 18) : Icons.scaleToWidth(Icons.API, 18));
            }

            if (tabbedPane.getTabCount() > CRV_INDEX) {
                Component tabComponentAt = tabbedPane.getTabComponentAt(CRV_INDEX);
                if (Objects.isNull(tabComponentAt)) return;
                ((JLabel) tabbedPane.getTabComponentAt(CRV_INDEX)).setIcon(index == CRV_INDEX ?
                        Icons.scaleToWidth(Icons.GIT_LAB_LIGHT, 20) : Icons.scaleToWidth(Icons.GIT_LAB, 20));
            }

            if (tabbedPane.getTabCount() > ASK_AI_INDEX) {
                Component tabComponentAt = tabbedPane.getTabComponentAt(ASK_AI_INDEX);
                if (Objects.isNull(tabComponentAt)) return;
                ((JLabel) tabbedPane.getTabComponentAt(ASK_AI_INDEX)).setIcon(index == ASK_AI_INDEX ?
                        Icons.scaleToWidth(Icons.ASK_AI_LIGHT, 18) : Icons.scaleToWidth(Icons.ASK_AI, 18));
            }

            if (tabbedPane.getSelectedIndex() == API_INDEX) {
                project.getService(ApiMainPanel.class).show();
            }
        });

        return tabbedPane;
    }

    public void reloadCodeReviewTab() {
        Component component = tabbedPane.getComponentAt(CRV_INDEX);
        if (component instanceof JBScrollPane) {
            CodeRvMainPanel codeReview = project.getService(CodeRvMainPanel.class);
            ((JBScrollPane) component).setViewportView(codeReview.createPanel());
        }
    }

    public void reloadCodeReviewAuthTab() {
        Component component = tabbedPane.getComponentAt(CRV_INDEX);
        if (component instanceof JBScrollPane) {
            CodeRvAuthPanel authPanel = project.getService(CodeRvAuthPanel.class);
            ((JBScrollPane) component).setViewportView(authPanel.createPanel());
        }
    }

}
