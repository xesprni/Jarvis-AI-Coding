package com.qihoo.finance.lowcode.aiquestion.ui.factory.stream;

import com.intellij.ide.plugins.PluginManagerMain;
import com.intellij.ui.components.JBScrollPane;
import com.qihoo.finance.lowcode.aiquestion.ui.QuestionPanel;
import com.qihoo.finance.lowcode.aiquestion.util.MarkdownParser;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

@Slf4j
@RequiredArgsConstructor
public class StreamTextPaneFactory {

    private final Color backgroundColor;
    private final static int margin = 5;
    private final static int DELAY = 20;
    private String txt = StringUtils.EMPTY;
    private JTextPane textPane;

    public JTextPane createTextPane() {
        QuestionPanel questionPanel = ProjectUtils.getCurrProject().getService(QuestionPanel.class);
        this.textPane = new JTextPane() {
            @Override
            public void scrollRectToVisible(Rectangle aRect) {
                questionPanel.scrollBottom(false);
            }
        };

        textPane.setFont(UIManager.getFont("Label.font"));
        textPane.setContentType("text/html");
        textPane.setBackground(backgroundColor);
        textPane.setBorder(BorderFactory.createEmptyBorder(margin, margin, margin, margin));
        textPane.setEditable(false);
        textPane.addHyperlinkListener(new PluginManagerMain.MyHyperlinkListener());
        return textPane;
    }

    public void appendText(String msg, StreamRender render) {
        render.stopRender();
        Timer timer = new Timer(DELAY, new ActionListener() {
            private int index = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (index < msg.length()) {
                    try {
                        int maxStepSize = 128;
                        int stepSize = Math.min(maxStepSize, msg.length() - index);
                        String text = msg.substring(0, Math.min(index + stepSize, msg.length()));
                        String replaceTxt = MarkdownParser.parseMarkdown(txt + text);
                        textPane.setText(replaceTxt);
                        index += stepSize;
                    } catch (Exception ex) {
                        log.error("Append text got an exception: {}", ex.getMessage());
                    }
                } else {
                    ((Timer) e.getSource()).stop();
                    render.resumeRender();
                    txt += msg;
                }
            }
        });
        timer.start();
    }

}
