package com.qihoo.finance.lowcode.aiquestion.ui.factory;

import javax.swing.*;
import java.awt.*;

public class MessageBodyFactory {

    private final TextPaneFactory textPaneFactory = new TextPaneFactory();
    private final CodePanelFactory codePanelFactory = new CodePanelFactory();
    private final static String EMPTY_RESPONSE = "暂无回复，换个问题试试吧";
    private final static String codeIdentifier = "```";

    public JPanel create(String msg, Color backgroundColor) {
        JPanel bodyPanel = new JPanel();
        bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.Y_AXIS));
        bodyPanel.setBackground(backgroundColor);
        if (msg == null || msg.isEmpty()) {
            bodyPanel.add(textPaneFactory.createTextPane(EMPTY_RESPONSE, backgroundColor));
            return bodyPanel;
        }
        int pos = 0;
        while (pos < msg.length()) {
            int start = msg.indexOf(codeIdentifier, pos);
            if (start == -1) {
                String textToAppend = msg.substring(pos);
                if (textToAppend.isBlank()) {
                    break;
                }
                textToAppend = trimNewLine(textToAppend);
                bodyPanel.add(textPaneFactory.createTextPane(textToAppend, backgroundColor));
                break;
            }
            int end = msg.indexOf(codeIdentifier, start + codeIdentifier.length());
            if (end == -1) {
                String textToAppend = msg.substring(pos);
                if (textToAppend.isBlank()) {
                    break;
                }
                bodyPanel.add(textPaneFactory.createTextPane(textToAppend, backgroundColor));
                break;
            }
            // append text before code
            if (start > pos) {
                String textToAppend = msg.substring(pos, start);
                textToAppend = trimNewLine(textToAppend);
                if (!textToAppend.isBlank()) {
                    bodyPanel.add(textPaneFactory.createTextPane(textToAppend, backgroundColor));
                }
            }
            // find code
            String code = msg.substring(start + codeIdentifier.length(), end);
            String first10Char = code.substring(0, Math.min(10, code.length()));
            String codeType = "code";
            int newLineIndex = first10Char.indexOf('\n');
            if (newLineIndex > -1) {
                if (newLineIndex > 0) {
                    codeType = first10Char.substring(0, newLineIndex).trim();
                }
                code = code.substring(newLineIndex + 1);
            }
            if (code.endsWith("\n")) {
                code = code.substring(0, code.length() - 1);
            }
            JPanel codePanel = codePanelFactory.create(codeType, code, backgroundColor);
            bodyPanel.add(codePanel);
            // go to find next text
            pos = end + codeIdentifier.length();
        }
        return bodyPanel;
    }

    private String trimNewLine(String str) {
        while (str.startsWith("\n")) {
            str = str.substring(1);
        }
        while (str.endsWith("\n")) {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }

}
