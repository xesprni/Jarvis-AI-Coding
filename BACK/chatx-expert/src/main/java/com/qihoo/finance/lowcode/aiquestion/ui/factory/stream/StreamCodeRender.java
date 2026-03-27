package com.qihoo.finance.lowcode.aiquestion.ui.factory.stream;

import com.intellij.ui.JBColor;
import com.qihoo.finance.lowcode.aiquestion.dto.Token;
import com.qihoo.finance.lowcode.aiquestion.dto.TokenType;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.swing.JTextPane;
import javax.swing.Timer;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Deprecated using {@link StreamCodePaneFactory}
 */
@Deprecated
@Slf4j
@RequiredArgsConstructor
public class StreamCodeRender implements StreamRender {

    private final static String CODE_IDENTIFIER = "```";
    private final static Set<String> KEYWORDS = new HashSet<>(Arrays.asList("int", "void", "if", "else", "while", "for"
            , "do", "switch", "case", "default", "package","String", "import", "public", "private", "protected"
            , "static", "final", "class", "interface", "extends", "implements","new", "return", "break", "continue"
            , "throw", "try", "catch", "finally", "throws", "super", "this", "true", "false","null", "boolean", "byte"
            , "char", "short", "long", "float", "double", "true", "false", "Object", "List"));
    private final static Set<Character> SEPARATES = new HashSet<>(Arrays.asList('.', '"', '/', '(', ',', '['));
    private final static Set<Character> INDEPENDENT_SEPARATES = new HashSet<>(Arrays.asList('.', '(', ')', ',', '[', ']'
            , '{', '}', '+', '-', '*', '/', '%', '&', '|', '='));
    private final static int DELAY = 10;

    private final JTextPane codePane;
    private final AtomicInteger atomicPos;
    @NonNull
    private String content;
    private final StreamRender parentRender;
    private final AtomicBoolean canRender = new AtomicBoolean(true);
    private int pos = -1;

    @Override
    public void flushRender(String content) {
        this.content = content;
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
        parentRender.repaint();
    }

    @Override
    public void render() {
        if (!canRender.get()) {
            return;
        }
        if (pos == -1) {
            int codePos =  atomicPos.get() + CODE_IDENTIFIER.length();
            int newLineIndex = content.indexOf('\n', codePos);
            // 找不到换行符，等待下次内容一起加载
            if (newLineIndex == -1) {
                return;
            }
            pos = newLineIndex + 1;
        }
        // 获取代码块内容
        int codeEndIndex = getCodeEndIndex(content, pos);
        // 渲染代码块
        while (pos < codeEndIndex) {
            char c = content.charAt(pos);
            int start = pos;
            if (Character.isWhitespace(c)) {
                do {
                    pos++;
                } while (pos < codeEndIndex && Character.isWhitespace(content.charAt(pos)));
                String text = content.substring(start, pos);
                // 去除代码结尾的换行
                if (pos >= codeEndIndex && content.indexOf(CODE_IDENTIFIER, pos) != -1) {
                    while (!text.isEmpty() && text.charAt(text.length() - 1) == '\n') {
                        text = text.substring(0, text.length() - 1);
                    }
                }
                if (!text.isEmpty()) {
                    appendText(codePane, new Token(TokenType.WHITESPACE, text));
                }
                break;
            } else {
                if (c == '"') {
                    do {
                        pos++;
                    } while (pos < codeEndIndex && (content.charAt(pos) != '"' || content.charAt(pos - 1) == '\\'));
                    if (pos < codeEndIndex) {
                        pos++;
                    }
                    String text = content.substring(start, pos);
                    appendText(codePane, new Token(TokenType.STRING, text));
                    break;
                } else if (c == '/' && pos + 1 < content.length() && content.charAt(pos + 1) == '/') {
                    pos += 2;
                    while (pos < codeEndIndex && content.charAt(pos) != '\n') {
                        pos++;
                    }
                    if (pos < codeEndIndex) {
                        pos++;
                    }
                    String text = content.substring(start, pos);
                    appendText(codePane, new Token(TokenType.COMMENT_ONE_LINE, text));
                    break;
                } else if (c == '/' && pos + 1 < content.length() && content.charAt(pos + 1) == '*') {
                    pos += 2;
                    while (pos < codeEndIndex && (content.charAt(pos) != '*'
                            || (pos + 1 < content.length() && content.charAt(pos + 1) != '/'))) {
                        pos++;
                    }
                    if (pos + 2 < codeEndIndex) {
                        pos += 2;
                    } else {
                        pos = codeEndIndex;
                    }
                    String text = content.substring(start, pos);
                    appendText(codePane, new Token(TokenType.COMMENT_MULTI_LINE, text));
                    break;
                } else {
                    while (pos < codeEndIndex && !Character.isWhitespace(content.charAt(pos))
                            && !INDEPENDENT_SEPARATES.contains(content.charAt(pos))) {
                        pos++;
                    }
                    if (pos > start) {
                        String text = content.substring(start, pos);
                        if (KEYWORDS.contains(text)) {
                            appendText(codePane, new Token(TokenType.KEYWORD, text));
                            break;
                        } else if (pos < content.length() && content.charAt(pos) == '.' && content.charAt(pos - 1) != ')') {
                            appendText(codePane, new Token(TokenType.VARIABLE, text));
                            break;
                        } else if (text.charAt(0) == '@') {
                            appendText(codePane, new Token(TokenType.ANNOTATION, text));
                            break;
                        } else {
                            appendText(codePane, new Token(TokenType.OTHER, text));
                            break;
                        }
                    } else {
                        while (pos < codeEndIndex) {
                            if (INDEPENDENT_SEPARATES.contains(content.charAt(pos))) {
                                pos++;
                            } else {
                                break;
                            }
                        }
                        if (pos > start) {
                            appendText(codePane, new Token(TokenType.OTHER, content.substring(start, pos)));
                            break;
                        }
                    }
                }
            }
        }
        // 代码块解析结束，通知StreamMessageBodyFactory继续解析响应
        if (!canRender.get()) {
            return;
        }
        codeEndIndex = content.indexOf(CODE_IDENTIFIER, pos);
        if (codeEndIndex != -1) {
            codeEndIndex += CODE_IDENTIFIER.length();
            if (codeEndIndex < content.length() && content.charAt(codeEndIndex) == '\n') {
                codeEndIndex++;
            }
            atomicPos.set(codeEndIndex);
            parentRender.resumeRender();
        } else if (pos >= content.length()) {
            parentRender.resumeRender();
        }

    }

    private static int getCodeEndIndex(String content, int pos) {
        int codeEndIndex = content.indexOf(CODE_IDENTIFIER, pos);
        if (codeEndIndex == -1) {
            codeEndIndex = content.length();
        }
        return codeEndIndex;
    }

    private void appendText(JTextPane textPane, Token token) {
        SimpleAttributeSet set = new SimpleAttributeSet();
        Color foreground = JBColor.foreground();
        switch (token.getType()) {
            case KEYWORD:
                foreground = new Color(206, 134, 79);
                break;
            case VARIABLE:
                foreground = new Color(199, 125, 187);
                break;
            case ANNOTATION:
                foreground = new Color(218, 63, 55);
                break;
            case STRING:
                foreground = new Color(103, 170, 99);
            case COMMENT_ONE_LINE:
                foreground = new Color(122, 126, 133);
            case COMMENT_MULTI_LINE:
                foreground = new Color(89, 129, 91);
        }

        StyleConstants.setForeground(set, foreground);
        StyleConstants.setAlignment(set, StyleConstants.ALIGN_LEFT);

//        StyledDocument doc = textPane.getStyledDocument();
//        doc.setParagraphAttributes(textPane.getText().length(), doc.getLength() - textPane.getText().length(), set, false);
//        try {
//            doc.insertString(doc.getLength(), token.getText(), set);//插入文本
//        } catch (BadLocationException e) {

//        }

        final String msg = token.getText();
        StreamCodeRender render = this;
        Timer timer = new Timer(DELAY, new ActionListener() {
            private int index = 0;
            @Override
            public void actionPerformed(ActionEvent e) {
                if (index < msg.length()) {
                    StyledDocument doc = textPane.getStyledDocument();
                    doc.setParagraphAttributes(textPane.getText().length(), doc.getLength() - textPane.getText().length(), set, false);
                    try {
                        doc.insertString(doc.getLength(), String.valueOf(msg.charAt(index)), set);
                        render.repaint();
                        index++;
                    } catch (Exception ex) {
                        log.error("Append text got an exception: {}", ex.getMessage());
                    }
                } else {
                    ((Timer) e.getSource()).stop();
                    render.resumeRender();
                }
            }
        });
        render.stopRender();
        timer.start();
    }

}
