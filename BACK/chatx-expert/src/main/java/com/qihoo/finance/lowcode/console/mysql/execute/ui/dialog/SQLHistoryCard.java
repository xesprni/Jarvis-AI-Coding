package com.qihoo.finance.lowcode.console.mysql.execute.ui.dialog;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LoadingDecorator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import com.qihoo.finance.lowcode.common.entity.dto.SQLExecuteHistory;
import com.qihoo.finance.lowcode.common.ui.base.RoundedPanel;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.utils.LocalDateUtils;
import com.qihoo.finance.lowcode.console.mysql.SQLEditorManager;
import com.qihoo.finance.lowcode.design.entity.DatabaseNode;
import com.qihoo.finance.lowcode.design.ui.tree.MySQLTreePanel;
import com.qihoo.finance.lowcode.design.util.DatabaseDesignUtils;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.commons.collections4.ListUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

/**
 * ConversationCard
 *
 * @author fengjinfu-jk
 * date 2024/5/14
 * @version 1.0.0
 * @apiNote ConversationCard
 */
public class SQLHistoryCard {
    private static final Color background = JBColor.background().darker();
    private final SQLExecuteHistory history;
    private final List<HistoryPanel> historyPanels;

    public SQLHistoryCard(List<HistoryPanel> historyPanels, SQLExecuteHistory history) {
        this.history = history;
        this.historyPanels = historyPanels;
    }

    public JComponent createCard(LoadingDecorator loadingDecorator, Runnable reloadAction) {
        HistoryPanel historyPanel = this.buildSQLHistoryPanel();
        this.historyPanels.add(historyPanel);
        return buildCard(history, historyPanel, loadingDecorator, reloadAction);
    }

    @NotNull
    private JPanel buildCard(SQLExecuteHistory history, HistoryPanel historyPanel, LoadingDecorator loadingDecorator, Runnable reloadAction) {
        JComponent content = historyPanel.getCard();
        JPanel cardPanel = new RoundedPanel(background, 20);
        cardPanel.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        cardPanel.add(content);
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackgroundInHierarchy(cardPanel, background.darker());
                super.mouseEntered(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBackgroundInHierarchy(cardPanel, background);
                super.mouseExited(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                // open sql console
                Project project = ProjectUtils.getCurrProject();
                List<DatabaseNode> databaseNodes = ListUtils.defaultIfNull(
                        DataContext.getInstance(project).getAllMySQLDatabaseList(),
                        new ArrayList<>()
                );

                DatabaseNode databaseNode = databaseNodes.stream().filter(v -> history.getInstanceName().equals(v.getInstanceName())
                        && history.getDatabaseName().equals(v.getName())).findFirst().orElse(null);
                if (Objects.nonNull(databaseNode)) {
                    MySQLTreePanel sqlTreePanel = project.getService(MySQLTreePanel.class);
                    sqlTreePanel.openHistorySQLConsole(databaseNode, history);
                }

                super.mouseClicked(e);
            }
        };

        cardPanel.addMouseListener(mouseAdapter);
        cardPanel.setBackground(background);
        UIUtil.forEachComponentInHierarchy(cardPanel, c -> {
            if (c instanceof JComponent) {
                c.setBackground(background);
                c.addMouseListener(mouseAdapter);
            }
        });
        setBackgroundInHierarchy(cardPanel, background);

        // delete
        historyPanel.getDelete().removeMouseListener(mouseAdapter);
        historyPanel.getDelete().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int deleteFlag = Messages.showDialog("删除该SQL执行记录 ?", "删除SQL执行记录",
                        new String[]{"是", "否"}, Messages.NO, Icons.scaleToWidth(Icons.WARN, 50));
                if (deleteFlag == Messages.NO) return;

                UIUtil.invokeLaterIfNeeded(() -> loadingDecorator.startLoading(false));
                new SwingWorker<Boolean, Boolean>() {
                    @Override
                    protected Boolean doInBackground() {
                        return DatabaseDesignUtils.deleteSQLExecuteHistory(history);
                    }

                    @SneakyThrows
                    @Override
                    protected void done() {
                        // reload
                        try {
                            reloadAction.run();
                            // 删除 virtualFile file 关联 history
                            VirtualFile file = Arrays.stream(FileEditorManager.getInstance(ProjectUtils.getCurrProject()).getOpenFiles())
                                    .filter(f -> history.getShowSQLConsoleName().equals(f.getName()))
                                    .findFirst().orElse(null);
                            if (Objects.nonNull(file)) file.putUserData(SQLEditorManager.SQL_HISTORY, null);
                        } finally {
                            UIUtil.invokeLaterIfNeeded(loadingDecorator::stopLoading);
                            super.done();
                        }
                    }
                }.execute();
            }
        });

        return cardPanel;
    }

    private static void setBackgroundInHierarchy(JComponent component, Color background) {
        component.setBackground(background);
        UIUtil.forEachComponentInHierarchy(component, c -> {
            if (c instanceof JComponent) {
                c.setBackground(background);
            }
        });
    }

    @NotNull
    private SQLHistoryCard.HistoryPanel buildSQLHistoryPanel() {
        // current or delete
        JPanel north = new JPanel(new BorderLayout());
        north.setToolTipText(history.getSqlContent());
        north.setBackground(background);
        JLabel sqlName = new JLabel(history.getShowSQLConsoleName());
        sqlName.setToolTipText(history.getSqlContent());
        sqlName.setIcon(AllIcons.Providers.Mysql);
        north.add(sqlName, BorderLayout.CENTER);

        // desc
        Date date = history.getDateUpdated();
        JLabel time = new JLabel(LocalDateUtils.convertToPatternString(date, LocalDateUtils.FORMAT_DATE_TIME));
        time.setToolTipText(history.getSqlContent());
        time.setBackground(background);
        time.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        time.setForeground(JBColor.GRAY);
        time.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        JPanel northEast = new JPanel(new FlowLayout(FlowLayout.LEFT));
        northEast.setBorder(BorderFactory.createEmptyBorder(-5, -5, -5, -5));
        northEast.add(time);
        JLabel delete = new JLabel(Icons.scaleToWidth(Icons.ROLLBACK2, 16));
        northEast.add(delete);
        north.add(northEast, BorderLayout.EAST);

        // assistant icon
        JLabel sqlContent = new JLabel(history.getShowSQLContent());
        sqlContent.setToolTipText(history.getSqlContent());
        sqlContent.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        sqlContent.setForeground(JBColor.GRAY);

        JPanel south = new JPanel(new BorderLayout());
        south.setToolTipText(history.getSqlContent());
        south.setBackground(background);
        south.add(sqlContent, BorderLayout.WEST);

        JPanel panel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        panel.setToolTipText(history.getSqlContent());
        panel.add(north);
        panel.add(south);
        panel.setBackground(background);
        panel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        return new HistoryPanel(panel, delete);
    }

    @Data
    public static class HistoryPanel {
        private JComponent card;
        private JLabel delete;

        public HistoryPanel(JComponent card, JLabel delete) {
            this.card = card;
            this.delete = delete;
        }
    }
}
