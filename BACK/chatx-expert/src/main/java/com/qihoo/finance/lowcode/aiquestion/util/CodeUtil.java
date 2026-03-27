package com.qihoo.finance.lowcode.aiquestion.util;

import com.intellij.ui.JBColor;
import com.qihoo.finance.lowcode.aiquestion.dto.Token;
import com.qihoo.finance.lowcode.aiquestion.dto.TokenType;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class CodeUtil {

    private final static Set<String> keywords = new HashSet<>(Arrays.asList("int", "void", "if", "else", "while", "for"
            , "do", "switch", "case", "default", "package","String", "import", "public", "private", "protected"
            , "static", "final", "class", "interface", "extends", "implements","new", "return", "break", "continue"
            , "throw", "try", "catch", "finally", "throws", "super", "this", "true", "false","null", "boolean", "byte"
            , "char", "short", "long", "float", "double", "true", "false", "Object", "List"));
    private final static Set<Character> separates = new HashSet<>(Arrays.asList('.', '"', '/', '(', ',', '['));
    private final static Set<Character> independentSeparates = new HashSet<>(Arrays.asList('.', '(', ')', ',', '[', ']'
            , '{', '}', '+', '-', '*', '/', '%', '&', '|', '='));
    public static void renderCode(JTextPane codePane, String code) {
        List<Token> tokens = tokenize(code);
        for (Token token : tokens) {
            appendText(codePane, token);
        }
    }

    private static void appendText(JTextPane textPane, Token token) {
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
        StyledDocument doc = textPane.getStyledDocument();
        doc.setParagraphAttributes(textPane.getText().length(), doc.getLength() - textPane.getText().length(), set, false);
        try {
            doc.insertString(doc.getLength(), token.getText(), set);//插入文本
        } catch (BadLocationException e) {
            log.error("Append text got an exception: {}", e.getMessage());
        }
    }

    private static List<Token> tokenize(String code) {
        int pos = 0;
        List<Token> tokens = new ArrayList<>();
        boolean charCollecting = false;
        boolean stringCollecting = false;
        boolean commentCollecting = false;
        while (pos < code.length()) {
            char c = code.charAt(pos);
            int start = pos;
            if (Character.isWhitespace(c)) {
                do {
                    pos++;
                } while (pos < code.length() && Character.isWhitespace(code.charAt(pos)));
                String text = code.substring(start, pos);
                tokens.add(new Token(TokenType.WHITESPACE, text));
            } else {
                if (c == '"') {
                    do {
                        pos++;
                    } while (pos < code.length() && code.charAt(pos) != '"');
                    if (pos < code.length()) {
                        pos++;
                    }
                    String text = code.substring(start, pos);
                    tokens.add(new Token(TokenType.STRING, text));
                } else if (c == '/' && pos + 1 < code.length() && code.charAt(pos + 1) == '/') {
                    pos += 2;
                    while (pos < code.length() && code.charAt(pos) != '\n') {
                        pos++;
                    }
                    if (pos < code.length()) {
                        pos++;
                    }
                    String text = code.substring(start, pos);
                    tokens.add(new Token(TokenType.COMMENT_ONE_LINE, text));
                } else if  (c == '/' && pos + 1 < code.length() && code.charAt(pos + 1) == '*') {
                    pos += 2;
                    while (pos < code.length() && (code.charAt(pos) != '*'
                            || (pos + 1 < code.length() &&code.charAt(pos + 1) != '/'))) {
                        pos++;
                    }
                    if (pos + 2 < code.length()) {
                        pos += 2;
                    } else {
                        pos = code.length();
                    }
                    String text = code.substring(start, pos);
                    tokens.add(new Token(TokenType.COMMENT_MULTI_LINE, text));
                } else {
                    do {
                        pos++;
                    } while (pos < code.length() && !Character.isWhitespace(code.charAt(pos))
                            && !separates.contains(code.charAt(pos)));
                    String text = code.substring(start, pos);
                    if (keywords.contains(text)) {
                        tokens.add(new Token(TokenType.KEYWORD, text));
                    } else if (pos < code.length() && code.charAt(pos) == '.' && code.charAt(pos -1) !=')' ) {
                        tokens.add(new Token(TokenType.VARIABLE, text));
                    } else if (text.charAt(0) == '@') {
                        tokens.add(new Token(TokenType.ANNOTATION, text));
                    } else {
                        tokens.add(new Token(TokenType.OTHER, text));
                    }
                    int independentIndex = pos;
                    while (pos < code.length()) {
                        if (independentSeparates.contains(code.charAt(pos))) {
                            pos++;
                        } else {
                            break;
                        }
                    }
                    if (independentIndex < pos) {
                        tokens.add(new Token(TokenType.OTHER, code.substring(independentIndex, pos)));
                    }
                }
            }
        }
        return tokens;
    }
}
