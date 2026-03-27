package com.qihoo.finance.lowcode.console.mysql.execute.ui.dialog;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.LoadingDecorator;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.UIUtil;
import com.qihoo.finance.lowcode.common.entity.dto.SQLExecuteHistory;
import com.qihoo.finance.lowcode.common.entity.dto.SQLExecuteHistoryType;
import com.qihoo.finance.lowcode.common.factory.ChatXToolWindowFactory;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.design.util.DatabaseDesignUtils;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ConversationHistoryPopup
 *
 * @author fengjinfu-jk
 * date 2024/5/10
 * @version 1.0.0
 * @apiNote ConversationHistoryPopup
 */
public class SQLHistoryDialog extends DialogWrapper implements Disposable {
    private static final JLabel noneRecord = new JLabel(Icons.scaleToWidth(Icons.HOLDER, 180));
    private final List<SQLHistoryCard.HistoryPanel> panels = new ArrayList<>();
    private static SQLHistoryDialog dialog;
    private static final int MAX_WIDTH = 450;

    static {
        noneRecord.setBorder(BorderFactory.createEmptyBorder(200, 0, 30, 0));
    }

    private LoadingDecorator loadingDecorator;
    private JBScrollPane historyScroll;
    private JPanel historyPanel;

    public static void showDialog() {
        if (Objects.nonNull(dialog) && dialog.isShowing()) {
            dialog.close(CANCEL_EXIT_CODE);
        } else {
            dialog = new SQLHistoryDialog(ProjectUtils.getCurrProject());
            dialog.show();
        }
    }

    public static void reloadIfShow() {
        if (Objects.nonNull(dialog) && dialog.isShowing()) {
            dialog.reloadPopupView(dialog.loadingDecorator, dialog.historyScroll, dialog.historyPanel);
        }
    }

    @Override
    public @Nullable Point getInitialLocation() {
        JComponent component = ChatXToolWindowFactory.getToolWindow().getComponent();
        Dimension maxSize = component.getSize();
        Point location = component.getLocationOnScreen();
        setSize(MAX_WIDTH, maxSize.height + 6);
        return new Point(location.x, location.y);
    }

    private final JPanel dialogPanel;
    private final ExtendableTextField search;

    public SQLHistoryDialog(@Nullable Project project) {
        super(project);
        search = new ExtendableTextField();
        dialogPanel = createPanel();
        init();
        setModal(false);
        setTitle("SQL执行记录");
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return dialogPanel;
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[0];
    }

    private JPanel createPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        Dimension maxSize = ChatXToolWindowFactory.getToolWindow().getComponent().getSize();
        panel.setPreferredSize(new Dimension(MAX_WIDTH, maxSize.height - 100));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

        historyPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        historyScroll = new JBScrollPane();
        loadingDecorator = new LoadingDecorator(historyScroll, this, 0);
        panel.add(loadingDecorator.getComponent(), BorderLayout.CENTER);

        // search
        search.addActionListener(e -> reloadPopupView(loadingDecorator, historyScroll, historyPanel));
        Icon searchIcon = Icons.scaleToWidth(Icons.SEARCH, 16);
        ExtendableTextComponent.Extension searchExtension = ExtendableTextComponent.Extension.create(
                searchIcon, searchIcon, "搜索", () -> reloadPopupView(loadingDecorator, historyScroll, historyPanel)
        );
        search.addExtension(searchExtension);

        // action panel
        JPanel actionPanel = new JPanel(new BorderLayout());
        actionPanel.add(search, BorderLayout.CENTER);
        actionPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(actionPanel, BorderLayout.SOUTH);

        // load
        reloadPopupView(loadingDecorator, historyScroll, historyPanel);
        return panel;
    }

    private void reloadPopupView(LoadingDecorator loadingDecorator, JBScrollPane historyScroll, JPanel historyPanel) {
        new SwingWorker<List<SQLExecuteHistory>, List<SQLExecuteHistory>>() {
            @Override
            protected List<SQLExecuteHistory> doInBackground() {
                UIUtil.invokeLaterIfNeeded(() -> loadingDecorator.startLoading(false));
                return DatabaseDesignUtils.getSQLExecuteHistory100(search.getText());
            }

            @SneakyThrows
            @Override
            protected void done() {
                List<SQLExecuteHistory> histories = get();
                UIUtil.invokeLaterIfNeeded(() -> {
                    historyPanel.removeAll();
                    if (!CollectionUtils.isEmpty(histories)) {
                        JLabel mySave = new JLabel("保存的SQL");
                        mySave.setForeground(JBColor.GRAY);
                        historyPanel.add(mySave);
                        boolean lasted100 = false;
                        for (SQLExecuteHistory history : histories) {
                            if (!lasted100) {
                                lasted100 = showLasted100(history, historyPanel);
                            }
                            historyPanel.add(new SQLHistoryCard(panels, history)
                                    .createCard(loadingDecorator, () -> reloadPopupView(loadingDecorator, historyScroll, historyPanel)));
                        }
                    }
                    historyScroll.setViewportView(historyPanel.getComponents().length > 0 ? historyPanel : noneRecord);
                    loadingDecorator.stopLoading();
                    super.done();
                });
            }

            private static boolean showLasted100(SQLExecuteHistory history, JPanel conversationPanel) {
                if (SQLExecuteHistoryType.SYS_SAVE.getCode().equals(history.getHistoryType())) {
                    JLabel group = new JLabel("最近SQL执行记录(100条)");
                    group.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
                    group.setForeground(JBColor.GRAY);
                    conversationPanel.add(group);
                    return true;
                }
                return false;
            }
        }.execute();
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
