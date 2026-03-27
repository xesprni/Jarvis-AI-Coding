package com.qihoo.finance.lowcode.aiquestion.ui.factory.stream;

import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.JBColor;
import com.qihoo.finance.lowcode.aiquestion.ui.AskAiMainPanel;
import com.qihoo.finance.lowcode.aiquestion.util.ChatUtil;
import com.qihoo.finance.lowcode.common.constants.QuestionType;
import com.qihoo.finance.lowcode.common.util.Icons;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Getter
@RequiredArgsConstructor
public class StreamMessageBodyFactory implements StreamRender {

    @NonNull
    private String content;
    private final Color backgroundColor;
    private final StreamRender parentRender;
    private final QuestionType questionType;
    private final static String EMPTY_RESPONSE = "暂无回复，换个问题试试吧";
    private final static String codeIdentifier = "```";
    private JPanel bodyPanel;
    private JPanel suggestedPanel;
    private StreamTextPaneFactory textPaneFactory;
    private StreamCodePanelFactory codePanelFactory;
    private int pos = 0;
    private boolean needTrimStartNewLine = false;
    private AtomicInteger atomicPos;
    private final AtomicBoolean canRender = new AtomicBoolean(true);
    private final AtomicInteger codeLength = new AtomicInteger(0);

    public JPanel create() {
        bodyPanel = new JPanel();
        bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.Y_AXIS));
        bodyPanel.setBackground(backgroundColor);
        return bodyPanel;
    }

    public void flushRender(String content) {
        this.content = content;
        if (pos >= content.length()) {
            return;
        }
        render();
    }

    @Override
    public void stopRender() {
        canRender.set(false);
    }

    @Override
    public void resumeRender() {
        canRender.set(true);
        render();
    }

    @Override
    public void repaint() {
        //刷新父组件
        parentRender.repaint();
    }

    @Override
    public void render() {
        try {
            doRender();
        } catch (Exception e) {
            log.error("renderer error: {}", e.getMessage(), e);
        }
    }

    private void doRender() {
        if (!canRender.get()) {
            return;
        }
        // 判断代码块的渲染进度
        if (atomicPos != null) {
            if (atomicPos.get() > pos) {
                // 上一个代码块已经渲染完了
                pos = atomicPos.get();
                textPaneFactory = null;
                codePanelFactory = null;
                atomicPos = null;
                needTrimStartNewLine = true;
            } else {
                // 继续渲染上一个代码块
                if (content.length() > codeLength.get()) {
                    stopRender();
                    codeLength.set(content.length());
                    codePanelFactory.flushRender(content);
                }
                return;
            }
        }
        if (StringUtils.isBlank(content)) {
            textPaneFactory = new StreamTextPaneFactory(backgroundColor);
            JTextPane textPane = textPaneFactory.createTextPane();
            bodyPanel.add(textPane);
            bodyPanel.revalidate();
            bodyPanel.repaint();
            textPaneFactory.appendText(EMPTY_RESPONSE, this);
            return;
        }
        int codeStartIndex = content.indexOf(codeIdentifier, pos);
        // render plain text
        if (codeStartIndex == -1) {
            String textToAppend = content.substring(pos);
            pos = content.length();
            if (textToAppend.isEmpty()) {
                return;
            }
            if (needTrimStartNewLine) {
                textToAppend = trimStartNewLine(textToAppend);
                needTrimStartNewLine = false;
            }
            if (textPaneFactory == null) {
                textPaneFactory = new StreamTextPaneFactory(backgroundColor);
                JTextPane textPane = textPaneFactory.createTextPane();
                bodyPanel.add(textPane);
                bodyPanel.revalidate();
                bodyPanel.repaint();
            }
            textPaneFactory.appendText(textToAppend, this);
            return;
        }
        // render plaint text which before code
        if (codeStartIndex > pos) {
            String textToAppend = content.substring(pos, codeStartIndex);
            if (needTrimStartNewLine) {
                textToAppend = trimStartNewLine(textToAppend);
                needTrimStartNewLine = false;
            }
            textToAppend = trimEndNewLine(textToAppend);
            if (!textToAppend.isBlank()) {
                if (textPaneFactory == null) {
                    textPaneFactory = new StreamTextPaneFactory(backgroundColor);
                    JTextPane textPane = textPaneFactory.createTextPane();
                    bodyPanel.add(textPane);
                    bodyPanel.revalidate();
                    bodyPanel.repaint();
                }
                textPaneFactory.appendText(textToAppend, this);
            }
            pos = codeStartIndex;
            return;
        }
        // find code type
        int codePos = pos;
        String codeType = "code";
        int newLineIndex = content.indexOf('\n', codePos);
        // 找不到换行符，等待下次内容一起加载
        if (newLineIndex == -1) {
            return;
        } else {
            codePos += codeIdentifier.length();
            if (newLineIndex > 0) {
                codeType = content.substring(codePos, newLineIndex);
            }
        }
        // crate code panel
        stopRender();
        codeLength.set(content.length());
        if (atomicPos == null) {
            atomicPos = new AtomicInteger(pos);
        }
        if (codePanelFactory == null) {
            codePanelFactory = new StreamCodePanelFactory(codeType, content, atomicPos, this, questionType);
            JPanel codePanel = codePanelFactory.create();
            bodyPanel.add(codePanel);
            bodyPanel.revalidate();
            bodyPanel.repaint();
            codePanelFactory.render();
        } else {
            codePanelFactory.flushRender(content);
        }
    }

    private String trimEndNewLine(String str) {
        while (str.endsWith("\n")) {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }

    private String trimStartNewLine(String str) {
        while (str.startsWith("\n")) {
            str = str.substring(1);
        }
        return str;
    }

    public void done() {
        if (Objects.nonNull(codePanelFactory)) {
            StreamCodePaneFactory codePaneFactory = codePanelFactory.getCodePaneFactory();
            if (Objects.nonNull(codePaneFactory) && codePaneFactory.isAgent()
                    && Objects.nonNull(codePaneFactory.getExpandButton())) {
                codePaneFactory.getExpandButton().setIcon(Icons.scaleToWidth(Icons.AI_EXPAND, 16));
                codePaneFactory.getTextField().setVisible(false);
            }
        }
    }

    public void addSuggestQuestion(String messageId, Runnable done) {
        if (Objects.isNull(suggestedPanel)) return;
        if (suggestedPanel.isEnabled()) return;
        suggestedPanel.setEnabled(true);

        new SwingWorker<List<String>, List<String>>() {
            @Override
            protected List<String> doInBackground() {
                return ChatUtil.getSuggested(messageId, questionType);
            }

            @SneakyThrows
            @Override
            protected void done() {
                List<String> questions = get();
                if (CollectionUtils.isEmpty(questions)) {
                    if (Objects.nonNull(done)) {
                        done.run();
                    }
                    return;
                }

                JLabel q = new JLabel();
                q.setText("猜您想问");
                q.setIcon(Icons.scaleToWidth(Icons.STARTS, 15));
                suggestedPanel.add(q);
                for (String question : questions) {
                    suggestedPanel.add(autoSendQuestion(question));
                }

                if (Objects.nonNull(done)) {
                    done.run();
                }
            }
        }.execute();
    }

    public JPanel createSuggested() {
        suggestedPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        suggestedPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, -5, 0));
        suggestedPanel.setBackground(backgroundColor);
        suggestedPanel.setVisible(false);
        suggestedPanel.setEnabled(false);

        return suggestedPanel;
    }

    public static JComponent autoSendQuestion(String questionStr) {
        JLabel question = new JLabel();
        question.setForeground(JBColor.BLUE);
        question.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        question.setText(questionStr);
        question.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        question.setPreferredSize(new Dimension(-1, 15));
        question.addMouseListener(AskAiMainPanel.fastAskQuestion(questionStr, questionStr, question));

        return question;
    }
}
