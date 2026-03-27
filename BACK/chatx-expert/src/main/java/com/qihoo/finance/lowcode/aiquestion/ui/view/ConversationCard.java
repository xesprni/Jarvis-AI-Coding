package com.qihoo.finance.lowcode.aiquestion.ui.view;

import com.alibaba.fastjson.JSON;
import com.intellij.openapi.ui.LoadingDecorator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import com.qihoo.finance.lowcode.aiquestion.ui.AskAiDecorator;
import com.qihoo.finance.lowcode.aiquestion.ui.QuestionPanel;
import com.qihoo.finance.lowcode.aiquestion.ui.worker.ChatSwingWorker;
import com.qihoo.finance.lowcode.aiquestion.util.ChatUtil;
import com.qihoo.finance.lowcode.common.constants.QuestionType;
import com.qihoo.finance.lowcode.common.entity.FileUpload;
import com.qihoo.finance.lowcode.common.entity.dto.askai.AssistantInfo;
import com.qihoo.finance.lowcode.common.entity.dto.askai.ChatConversationReponse;
import com.qihoo.finance.lowcode.common.entity.dto.askai.ChatMessageResponse;
import com.qihoo.finance.lowcode.common.ui.base.RoundedLabel;
import com.qihoo.finance.lowcode.common.ui.base.RoundedPanel;
import com.qihoo.finance.lowcode.common.util.EditorComponentUtils;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.UserUtils;
import com.qihoo.finance.lowcode.common.utils.LocalDateUtils;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import com.qihoo.finance.lowcode.gentracker.tool.StringUtils;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.commons.collections4.MapUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ConversationCard
 *
 * @author fengjinfu-jk
 * date 2024/5/14
 * @version 1.0.0
 * @apiNote ConversationCard
 */
public class ConversationCard {
    private static final Color background = JBColor.background().darker();
    private final LoadingDecorator loadingDecorator;
    private final ChatConversationReponse conversation;
    private final Runnable closeCardAction;
    private final List<ConversationPanel> conversationPanels;

    public ConversationCard(List<ConversationPanel> conversationPanels, LoadingDecorator loadingDecorator, ChatConversationReponse conversation, Runnable closeCardAction) {
        this.loadingDecorator = loadingDecorator;
        this.conversation = conversation;
        this.closeCardAction = closeCardAction;
        this.conversationPanels = conversationPanels;
    }

    public JComponent createCard(String conversationId) {
        ConversationPanel conversationPanel = this.getConversationInfo(conversationId);
        this.conversationPanels.add(conversationPanel);
        return buildCard(conversation, conversationPanel);
    }

    @NotNull
    private JPanel buildCard(ChatConversationReponse conversation, ConversationPanel conversationPanel) {
        JComponent content = conversationPanel.getCard();
        JPanel cardPanel = new RoundedPanel(background, 20);
        cardPanel.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        cardPanel.add(content);
        cardPanel.addMouseListener(new MouseAdapter() {
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
                QuestionPanel questionPanel = ProjectUtils.getCurrProject().getService(QuestionPanel.class);
                if (Objects.isNull(questionPanel)) return;

                AskAiDecorator askAiDecorator = questionPanel.getAskAiDecorator();
                UIUtil.invokeLaterIfNeeded(() -> {
                    askAiDecorator.showLoading();
                    conversationPanels.forEach(conversationContent -> {
                        setBackgroundInHierarchy(conversationContent.getCard(), EditorComponentUtils.BACKGROUND);
                        conversationContent.getShowCurrent().setVisible(false);
                        conversationContent.getDeleteAction().setVisible(true);
                    });

                    setBackgroundInHierarchy(conversationPanel.getCard(), EditorComponentUtils.BACKGROUND.darker());
                    conversationPanel.getShowCurrent().setVisible(true);
                    conversationPanel.getDeleteAction().setVisible(false);
                });

                new SwingWorker<List<ChatMessageResponse>, List<ChatMessageResponse>>() {
                    @Override
                    protected List<ChatMessageResponse> doInBackground() {
                        return ChatUtil.getConversationMessages(conversation.getAssistantCode(), conversation.getId());
                    }

                    @SneakyThrows
                    @Override
                    protected void done() {
                        try {
                            List<ChatMessageResponse> messages = get();
                            // flush
                            UIUtil.invokeLaterIfNeeded(() -> {
                                JPanel viewPanel = questionPanel.getViewPanel();
                                viewPanel.removeAll();
                                viewPanel.revalidate();
                            });
                            // renderer
                            final String username = Optional.ofNullable(UserUtils.getUserInfo().getUserNo()).orElse("You");
                            for (ChatMessageResponse message : messages) {
                                // question
                                questionPanel.renderMsg(username, message.getQuery());
                                // answer
                                message.setAnswer(org.apache.commons.lang3.StringUtils.defaultIfBlank(message.getAnswer(), "暂无回复，换个问题试试吧"));
                                ChatSwingWorker chatSwingWorker = new ChatSwingWorker(ProjectUtils.getCurrProject()
                                        , questionPanel, conversation.getAssistantCode(), QuestionType.ASK, message.getQuery(), null);
                                chatSwingWorker.renderAnswer(message.getAnswer());
                                chatSwingWorker.done();
                            }

                            // set assistant
                            String assistantCode = conversation.getAssistantCode();
                            questionPanel.getInputPanelFactory().flushAssistant(assistantCode, conversation.getId());

                            // 文件信息
                            resetConversationFiles(conversation);
                        } finally {
                            UIUtil.invokeLaterIfNeeded(askAiDecorator::hidden);
                            super.done();
                        }
                    }

                    private static void resetConversationFiles(ChatConversationReponse conversation) {
                        QuestionPanel questionPanel = ProjectUtils.getCurrProject().getService(QuestionPanel.class);
                        questionPanel.getInputPanelFactory().getUploadFiles().clear();
                        Map<String, Object> inputs = conversation.getInputs();
                        if (MapUtils.isNotEmpty(inputs) && inputs.containsKey("file_details")) {
                            String files = (String) inputs.get("file_details");
                            if (org.apache.commons.lang3.StringUtils.isNotBlank(files)) {
                                List<FileUpload> fileUploads = JSON.parseArray(files, FileUpload.class);
                                for (FileUpload file : fileUploads) {
                                    questionPanel.getInputPanelFactory().getUploadFiles().add(file);
                                }
                            }
                        }
                        questionPanel.getInputPanelFactory().repaintNorthContent();
                    }
                }.execute();
                super.mouseClicked(e);
            }
        });

        setBackgroundInHierarchy(cardPanel, background);
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
    private ConversationPanel getConversationInfo(String conversationId) {
        // current or delete
        RoundedLabel showCurrent = new RoundedLabel("当前会话", RoundedLabel.GREEN, RoundedLabel.WHITE, 6);
        showCurrent.setVisible(conversation.getId().equals(conversationId));
        JComponent deleteAction = createDeleteAction();
        deleteAction.setVisible(!conversation.getId().equals(conversationId));

        JPanel north = new JPanel(new BorderLayout());
        north.setBackground(background);
        JLabel conversationTitle = new JLabel(StringUtils.formatMaxLength(conversation.getName(), 25));
        north.add(conversationTitle, BorderLayout.CENTER);
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        toolbar.add(showCurrent);
        toolbar.add(deleteAction);
        north.add(toolbar, BorderLayout.EAST);

        // desc
        Date date = new Date(conversation.getCreatedAt() * 1000);
        JLabel time = new JLabel(LocalDateUtils.convertToPatternString(date, LocalDateUtils.FORMAT_DATE_TIME));
        time.setBackground(background);
        time.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        time.setForeground(JBColor.GRAY);
        time.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        // assistant icon
        Map<String, AssistantInfo> assistantMap = ChatxApplicationSettings.settings().assistants.stream().collect(Collectors.toMap(AssistantInfo::getCode, Function.identity()));
        AssistantInfo assistant = assistantMap.getOrDefault(conversation.getAssistantCode(), new AssistantInfo());
        JLabel assistantLabel = new JLabel(assistant.getName());
        assistantLabel.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        assistantLabel.setForeground(JBColor.GRAY);
//        Icons.asyncSetUrlIcon(assistantLabel, assistant.getIconUrl16(), Icons.ASSISTANT, 16);

        JPanel south = new JPanel(new BorderLayout());
        south.setBackground(background);
        south.add(assistantLabel, BorderLayout.WEST);
        south.add(time, BorderLayout.EAST);

        JPanel panel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        panel.add(north);
        panel.add(south);
        panel.setBackground(background);
        panel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        return new ConversationPanel(panel, showCurrent, deleteAction);
    }

    private JComponent createDeleteAction() {
        JLabel delete = new JLabel(Icons.scaleToWidth(Icons.ROLLBACK2, 13));
        delete.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int deleteFlag = Messages.showDialog("确认删除会话 ?", "删除会话",
                        new String[]{"是", "否"}, Messages.NO, Icons.scaleToWidth(Icons.WARN, 50));
                if (deleteFlag == Messages.NO) return;

                UIUtil.invokeLaterIfNeeded(() -> loadingDecorator.startLoading(false));
                new SwingWorker<Boolean, Boolean>() {
                    @Override
                    protected Boolean doInBackground() {
                        return ChatUtil.deleteConversation(conversation.getAssistantCode(), conversation.getId());
                    }

                    @SneakyThrows
                    @Override
                    protected void done() {
                        // reload
                        try {
                            closeCardAction.run();
                        } finally {
                            UIUtil.invokeLaterIfNeeded(loadingDecorator::stopLoading);
                            super.done();
                        }
                    }
                }.execute();
            }
        });
        return delete;
    }

    @Data
    public static class ConversationPanel {
        private JComponent showCurrent;
        private JComponent deleteAction;
        private JComponent card;

        public ConversationPanel(JComponent card, JComponent showCurrent, JComponent deleteAction) {
            this.showCurrent = showCurrent;
            this.deleteAction = deleteAction;
            this.card = card;
        }
    }
}
