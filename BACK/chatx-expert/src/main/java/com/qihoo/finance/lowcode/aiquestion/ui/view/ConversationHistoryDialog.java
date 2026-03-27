package com.qihoo.finance.lowcode.aiquestion.ui.view;

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
import com.qihoo.finance.lowcode.aiquestion.ui.QuestionPanel;
import com.qihoo.finance.lowcode.aiquestion.util.ChatUtil;
import com.qihoo.finance.lowcode.common.entity.dto.askai.ChatConversationReponse;
import com.qihoo.finance.lowcode.common.factory.ChatXToolWindowFactory;
import com.qihoo.finance.lowcode.common.util.Icons;
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
public class ConversationHistoryDialog extends DialogWrapper implements Disposable {
    private static final JLabel noneRecord = new JLabel(Icons.scaleToWidth(Icons.HOLDER, 180));
    private final List<ConversationCard.ConversationPanel> panels = new ArrayList<>();
    private static ConversationHistoryDialog dialog;
    private static final int MAX_WIDTH = 450;

    static {
        noneRecord.setBorder(BorderFactory.createEmptyBorder(200, 0, 30, 0));
    }

    public static void showDialog() {
        if (Objects.nonNull(dialog) && dialog.isShowing()) {
            dialog.close(CANCEL_EXIT_CODE);
        } else {
            dialog = new ConversationHistoryDialog(ProjectUtils.getCurrProject());
            dialog.show();
        }
    }

    @Override
    public @Nullable Point getInitialLocation() {
        JComponent component = ChatXToolWindowFactory.getToolWindow().getComponent();
        Dimension maxSize = component.getSize();
        Point location = component.getLocationOnScreen();
        setSize(MAX_WIDTH, maxSize.height + 6);

        int x = location.x - MAX_WIDTH + 6;
        int y = location.y;
        return new Point(x, y);
    }

    private final JPanel dialogPanel;
    private final ExtendableTextField search;

    public ConversationHistoryDialog(@Nullable Project project) {
        super(project);
        search = new ExtendableTextField();
        dialogPanel = createPanel();
        init();
        setModal(false);
        setTitle("历史会话");
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

        JPanel conversationPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        JBScrollPane conversationScroll = new JBScrollPane();
        LoadingDecorator loadingDecorator = new LoadingDecorator(conversationScroll, this, 0);
        panel.add(loadingDecorator.getComponent(), BorderLayout.CENTER);

        // search
        search.addActionListener(e -> reloadPopupView(loadingDecorator, conversationScroll, conversationPanel));
        Icon searchIcon = Icons.scaleToWidth(Icons.SEARCH, 16);
        ExtendableTextComponent.Extension searchExtension = ExtendableTextComponent.Extension.create(
                searchIcon, searchIcon, "搜索", () -> reloadPopupView(loadingDecorator, conversationScroll, conversationPanel)
        );
        search.addExtension(searchExtension);

        // 新建会话
        JButton newConversation = new JButton("新建会话");
        newConversation.setIcon(Icons.scaleToWidth(Icons.CONVERSATION_NEW, 15));
        newConversation.addActionListener(e -> {
            QuestionPanel questionPanel = ProjectUtils.getCurrProject().getService(QuestionPanel.class);
            if (Objects.isNull(questionPanel)) return;
            questionPanel.repaintPanel();
        });

        // action panel
        JPanel actionPanel = new JPanel(new BorderLayout());
        actionPanel.add(search, BorderLayout.CENTER);
        actionPanel.add(newConversation, BorderLayout.EAST);
        actionPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(actionPanel, BorderLayout.SOUTH);

        // load
        reloadPopupView(loadingDecorator, conversationScroll, conversationPanel);
        return panel;
    }

    private void reloadPopupView(LoadingDecorator loadingDecorator, JBScrollPane conversationScroll, JPanel conversationPanel) {
        new SwingWorker<List<ChatConversationReponse>, List<ChatConversationReponse>>() {
            @Override
            protected List<ChatConversationReponse> doInBackground() {
                UIUtil.invokeLaterIfNeeded(() -> loadingDecorator.startLoading(false));
                return ChatUtil.getConversations(search.getText());
            }

            @SneakyThrows
            @Override
            protected void done() {
                List<ChatConversationReponse> conversations = get();
                UIUtil.invokeLaterIfNeeded(() -> {
                    conversationPanel.removeAll();
                    if (!CollectionUtils.isEmpty(conversations)) {
                        QuestionPanel questionPanel = ProjectUtils.getCurrProject().getService(QuestionPanel.class);
                        String conversationId = questionPanel.getInputPanelFactory().getConversationId();
                        JLabel day3 = new JLabel("最近三天");
                        day3.setForeground(JBColor.GRAY);
                        conversationPanel.add(day3);
                        boolean showBeforeDay3 = false;
                        boolean showBeforeDay7 = false;
                        for (ChatConversationReponse conversation : conversations) {
                            if (!showBeforeDay3) {
                                showBeforeDay3 = showBeforeDay(conversation, conversationPanel, "三天之前", 3);
                            }
                            if (!showBeforeDay7) {
                                showBeforeDay7 = showBeforeDay(conversation, conversationPanel, "一周之前", 7);
                            }
                            conversationPanel.add(new ConversationCard(
                                    panels, loadingDecorator, conversation,
                                    () -> reloadPopupView(loadingDecorator, conversationScroll, conversationPanel))
                                    .createCard(conversationId));
                        }
                    }
                    conversationScroll.setViewportView(conversationPanel.getComponents().length > 0 ? conversationPanel : noneRecord);
                    loadingDecorator.stopLoading();
                    super.done();
                });
            }

            private static boolean showBeforeDay(ChatConversationReponse conversation, JPanel conversationPanel, String title, int beforeDay) {
                if ((System.currentTimeMillis() - conversation.getCreatedAt() * 1000) / 1000 / 60 / 60 / 24 > beforeDay) {
                    JLabel beforeDay3 = new JLabel(title);
                    beforeDay3.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
                    beforeDay3.setForeground(JBColor.GRAY);
                    conversationPanel.add(beforeDay3);
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
