package com.qihoo.finance.lowcode.aiquestion.ui.factory.stream;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.qihoo.finance.lowcode.aiquestion.ui.AskAiMainPanel;
import com.qihoo.finance.lowcode.aiquestion.ui.QuestionPanel;
import com.qihoo.finance.lowcode.aiquestion.ui.factory.ImgButtonFactory;
import com.qihoo.finance.lowcode.aiquestion.util.ChatUtil;
import com.qihoo.finance.lowcode.aiquestion.util.ColorUtil;
import com.qihoo.finance.lowcode.common.constants.QuestionType;
import com.qihoo.finance.lowcode.common.entity.dto.askai.AssistantInfo;
import com.qihoo.finance.lowcode.common.entity.dto.askai.ChatFeedbacksRequest;
import com.qihoo.finance.lowcode.common.ui.base.RoundedPanel;
import com.qihoo.finance.lowcode.common.util.EditorComponentUtils;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.JButtonUtils;
import com.qihoo.finance.lowcode.common.util.NotifyUtils;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

@Getter
@RequiredArgsConstructor
public class StreamMessageRowFactory implements StreamRender {

    private final Project project;
    private final JBScrollPane viewScrollPane;
    private final String username;
    private final String content;
    private final String question;
    private final Long logId;
    private final QuestionType questionType;
    private final AtomicReference<String> contentRefer = new AtomicReference<String>();
    private final ImgButtonFactory imgButtonFactory = new ImgButtonFactory();
    private final static ExecutorService executorService = Executors.newFixedThreadPool(1);

    private StreamMessageBodyFactory messageBodyFactory;
    private JPanel msgRowPanel;
    private JButton reAnswer;
//    private JLabel progress;
//    private FeedbackDialog feedbackDialog;
    private final QuestionPanel questionPanel = ProjectUtils.getCurrProject().getService(QuestionPanel.class);

    public JPanel create(String assistant, String messageId) {
        contentRefer.set(content);
        // create message row panel
        Color backgroundColor = getBackgroundColor(username);
        msgRowPanel = new RoundedPanel(backgroundColor, 20);
        msgRowPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        msgRowPanel.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP));

        msgRowPanel.setBackground(backgroundColor);
        // add header to row panel
        JPanel headerPanel = createHeaderPanel(assistant, backgroundColor);
        msgRowPanel.add(headerPanel);
        // add body to row panel
        messageBodyFactory = new StreamMessageBodyFactory(content, backgroundColor, this, questionType);
        JPanel bodyPanel = messageBodyFactory.create();
        msgRowPanel.add(bodyPanel);

        JPanel suggestedRow = createSuggestedRow();
        if (suggestedRow.getComponentCount() > 0) {
            msgRowPanel.add(suggestedRow);
        }

        AssistantInfo assistantInfo = ChatxApplicationSettings.settings().getAssistantInfo(assistant);
        if(assistantInfo.getAssistantType() != AssistantInfo.AssistantType.WORKFLOW && StringUtils.isNotEmpty(messageId)) {
            JPanel feedbackRow = createFeedbackRow(assistant, messageId, question, backgroundColor);
            msgRowPanel.add(feedbackRow);
        }

        return msgRowPanel;
    }

    private JPanel createSuggestedRow() {
        return messageBodyFactory.createSuggested();
    }

    @Override
    public void render() {
        messageBodyFactory.render();
        repaint();
    }

    @Override
    public void flushRender(String content) {
        this.contentRefer.set(content);
        messageBodyFactory.flushRender(content);
        repaint();
    }

    @Override
    public void repaint() {
        msgRowPanel.revalidate();
        msgRowPanel.repaint();
    }

    private JPanel createHeaderPanel(String assistant, Color backgroundColor) {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(backgroundColor);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(-5, 2, -5, 10));
        // add username panel
        JLabel usernameLabel = new JLabel();
        AssistantInfo assistantInfo = ChatxApplicationSettings.settings().getAssistantInfo(assistant);
        usernameLabel.setText(assistantInfo.getName());
//        Icons.asyncSetUrlIcon(usernameLabel, assistantInfo.getIconUrl16(), Icons.ASSISTANT, 18);
//        usernameLabel.setForeground(JBColor.foreground());
        usernameLabel.setFont(usernameLabel.getFont().deriveFont(Font.BOLD));
        headerPanel.add(usernameLabel, BorderLayout.WEST);
        // add button panel
        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.setBackground(backgroundColor);
        JButton copyButton = createCopyButton(backgroundColor);
        btnPanel.add(copyButton);
        headerPanel.add(btnPanel, BorderLayout.EAST);

        JSeparator mySeparator = new JSeparator(SwingConstants.HORIZONTAL);
        headerPanel.add(mySeparator, BorderLayout.SOUTH);
        return headerPanel;
    }

    private JButton createCopyButton(Color backgroundColor) {
        JButton copyButton = imgButtonFactory.create("复制", Icons.AI_COPY, 13, 13
                , JBUI.insets(5), backgroundColor);
        copyButton.addActionListener(e -> {
            StringSelection selection = new StringSelection(this.contentRefer.get());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, null);
            copyButton.setIcon(Icons.DONE);
            copyButton.setToolTipText("复制成功");
            executorService.submit(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                }
                copyButton.setIcon(Icons.AI_COPY);
                copyButton.setToolTipText("复制");
            });
        });
        return copyButton;
    }

    private JButton createThumbsUpButton(String assistant, String messageId, Color backgroundColor) {
        JButton button = imgButtonFactory.create("赞", Icons.THUMBS_UP,
                13, 13, JBUI.insets(5), backgroundColor);
        button.addActionListener(e -> {
//            if (feedbackDialog != null) {
//                feedbackDialog.close(DialogWrapper.CANCEL_EXIT_CODE);
//            }
//            feedbackDialog = new FeedbackDialog(project, FeedbackDialog.FEEDBACK_TYPE_THUMBS_UP, logId
//                    , button.getLocationOnScreen());
//            feedbackDialog.show();
            // 点赞
            NotifyUtils.notifyBalloon(button, "\uD83C\uDF57点赞已收到，感谢您的鼓励和支持！",
                    null, button.getForeground(), EditorComponentUtils.BACKGROUND);
            executorService.submit(() -> ChatUtil.messageFeedbacks(assistant, messageId, ChatFeedbacksRequest.Rating.LIKE));
        });
        return button;
    }

    private JButton createThumbsDownButton(String assistant, String messageId, Color backgroundColor) {
        JButton button = imgButtonFactory.create("踩", Icons.THUMBS_DOWN,
                13, 13, JBUI.insets(5), backgroundColor);
        button.addActionListener(e -> {
//            if (feedbackDialog != null) {
//                feedbackDialog.close(DialogWrapper.CANCEL_EXIT_CODE);
//            }
//            feedbackDialog = new FeedbackDialog(project, FeedbackDialog.FEEDBACK_TYPE_THUMBS_DOWN, logId
//                    , button.getLocationOnScreen());
//            feedbackDialog.show();
            // 点踩
            NotifyUtils.notifyBalloon(button, "\uD83C\uDF39感谢你的反馈，我们会努力改进。",
                    null, button.getForeground(), EditorComponentUtils.BACKGROUND);
            executorService.submit(() -> ChatUtil.messageFeedbacks(assistant, messageId, ChatFeedbacksRequest.Rating.DISLIKE));
        });
        return button;
    }

    private Color getBackgroundColor(String username) {
        return ColorUtil.getReplyContentBackground(username);
    }

    public JPanel createFeedbackRow(String assistant, String messageId, String question, Color background) {
        JPanel feedbackRow = new JPanel(new BorderLayout());
        feedbackRow.setBorder(BorderFactory.createEmptyBorder(-5, 0, -5, 0));
        feedbackRow.setOpaque(false);
        feedbackRow.setBackground(background);

        // feedback
        JPanel eastPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        eastPanel.setBackground(background);
        eastPanel.add(createReAnswer(assistant, messageId, question));
        eastPanel.add(createThumbsUpButton(assistant, messageId, background));
        eastPanel.add(createThumbsDownButton(assistant, messageId, background));
        feedbackRow.add(eastPanel, BorderLayout.EAST);

        // progress
//        progress = new JLabel(Icons.LOADING_ANIMATED);
//        feedbackRow.add(progress, BorderLayout.WEST);
        return feedbackRow;
    }

    private Component createReAnswer(String assistant, String messageId, String question) {
        reAnswer = JButtonUtils.createNonOpaqueButton(Icons.scaleToWidth(Icons.REFRESH, 16), new Dimension(20, 20));
        reAnswer.setVisible(false);
        reAnswer.setToolTipText("重新生成");
        MouseAdapter reAnswerAction = AskAiMainPanel.fastAskQuestion("重新生成", question, new JLabel());
        reAnswer.addMouseListener(reAnswerAction);
        return reAnswer;
    }

    public void done() {
        // 关闭进度条(如果有的话
//        if (Objects.nonNull(progress)) {
//            progress.setVisible(false);
//        }
        if (Objects.nonNull(reAnswer)) reAnswer.setVisible(true);

        if (Objects.nonNull(messageBodyFactory)) {
            JPanel suggested = messageBodyFactory.getSuggestedPanel();
            if (Objects.nonNull(suggested)) {
                UIUtil.invokeLaterIfNeeded(() -> {
                    suggested.setVisible(true);
                });
            }
            messageBodyFactory.done();
        }
    }
}
