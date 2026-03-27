package com.qihoo.finance.lowcode.aiquestion.dialog;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBColor;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.entity.dto.aiquestion.AskAiFeedbackReq;
import com.qihoo.finance.lowcode.common.util.NotifyUtils;
import com.qihoo.finance.lowcode.common.util.RestTemplateUtil;
import com.qihoo.finance.lowcode.convertor.util.SpringUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SpringLayout;
import java.awt.Component;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FeedbackDialog extends DialogWrapper {

    public static final int FEEDBACK_TYPE_THUMBS_UP = 0;
    public static final int FEEDBACK_TYPE_THUMBS_DOWN = 1;

    private JPanel dialogPanel;
    private Long logId;
    private final int feedbackType;
    private final Point btnLocation;

    public FeedbackDialog(@Nullable Project project, int feedbackType, Long logId, Point btnLocation) {
        super(project);
        setTitle("你的反馈帮助我们持续进步");
        setModal(false);
        this.feedbackType = feedbackType;
        this.logId = logId;
        this.btnLocation = btnLocation;
        setOKButtonText("提交");
        super.init();
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[] { getOKAction() };
    }

    @Override
    public void show() {
        int x = btnLocation.x - getPreferredSize().width;
        int y = btnLocation.y - getPreferredSize().height;
        setLocation(x, y);
        super.show();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        dialogPanel = new JPanel(new SpringLayout());
        if (feedbackType == FEEDBACK_TYPE_THUMBS_UP) {
            addCheckBox(Arrays.asList("回答准确并专业", "回答清晰易于理解", "响应速度快"));
        } else {
            addCheckBox(Arrays.asList("存在不安全或违法信息", "存在错误信息", "回复内容没有帮助"));
        }

        JTextArea textArea = new JTextArea();
        textArea.setRows(4);
        textArea.setColumns(10);
        textArea.setLineWrap(true);
        textArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBColor.background().brighter(), 1, true),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        dialogPanel.add(textArea);
        SpringUtilities.makeCompactGrid(dialogPanel, dialogPanel.getComponentCount(), 1, 0, 0, 0, 10);
        return dialogPanel;
    }

    private void addCheckBox(List<String> options) {
        for (String option : options) {
            dialogPanel.add(new JCheckBox(option));
        }
    }

    @Override
    protected void doOKAction() {
        String feedback = getFeedback();
        new Thread(() -> {
            AskAiFeedbackReq askAiFeedbackReq = new AskAiFeedbackReq();
            askAiFeedbackReq.setId(logId);
            askAiFeedbackReq.setFeedbackType(feedbackType);
            askAiFeedbackReq.setFeedback(feedback);
            RestTemplateUtil.post(Constants.Url.CHAT_FEEDBACK, askAiFeedbackReq);
            ApplicationManager.getApplication().invokeLater(super::doOKAction);
            NotifyUtils.notify("提交成功", NotificationType.INFORMATION);
        }).start();
    }

    @NotNull
    private String getFeedback() {
        List<String> feedbackList = new ArrayList<>();
        Component[] components = dialogPanel.getComponents();
        for (Component component : components) {
            if (component instanceof JCheckBox) {
                JCheckBox checkBox = (JCheckBox) component;
                if (checkBox.isSelected()) {
                    feedbackList.add(checkBox.getText());
                }
            } else if (component instanceof JTextArea) {
                String text = ((JTextArea) component).getText();
                if (!text.isBlank()) {
                    feedbackList.add(text);
                }
            }
        }
        return String.join("\n", feedbackList);
    }
}
