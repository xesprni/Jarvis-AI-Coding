package com.qihoo.finance.lowcode.kit.ui.digest;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.util.ui.JBUI;
import com.qihoo.finance.lowcode.common.enums.LightVirtualType;
import com.qihoo.finance.lowcode.common.util.EditorComponentUtils;
import com.qihoo.finance.lowcode.common.util.JButtonUtils;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import com.qihoo.finance.lowcode.kit.ui.KitDialog;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.HexFormat;

/**
 * ToolsDialog
 *
 * @author fengjinfu-jk
 * date 2024/4/11
 * @version 1.0.0
 * @apiNote ToolsDialog
 */
@SuppressWarnings("all")
@Slf4j
public class EncodeKitTab extends KitDialog {

    public EncodeKitTab(@Nullable Project project) {
        super(project);
    }

    @Override
    public @Nullable JComponent createCenterPanel() {
        JTabbedPane tab = new JTabbedPane();
        tab.setPreferredSize(JBUI.size(width, height));
        tab.add("URL编码/解码", urlEncode());
        tab.add("Base64编码/解码", base64Encode());
        tab.add("Unicode与中文编码/解码", unicodeEncode());
        tab.add("字符编码/解码", charEncode());
//        tab.add("字节数组编码/解码", byteArrayEncode());
        return tab;
    }

    private String byteArrayStr(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return "[" + sb.toString() + "]";
    }

    private Component unicodeEncode() {
        JPanel urlEncode = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        // encode content
        Editor encodeEditor = EditorComponentUtils.createEditorPanel(ProjectUtils.getCurrProject(), LightVirtualType.JSON);
        encodeEditor.getSettings().setLineNumbersShown(false);
        JComponent encodeComponent = encodeEditor.getComponent();
        urlEncode.add(encodeComponent);

        // action
        JPanel action = new JPanel(new FlowLayout(FlowLayout.LEFT));
        action.setBorder(BorderFactory.createEmptyBorder(-5, 0, -5, 0));
        urlEncode.add(action);

        JCheckBox unescape = new JCheckBox("Unescape All  ");
        action.add(unescape);

        // encode action
        JButton encode = JButtonUtils.createNonOpaqueButton("Unicode转中文 ↓");
        encode.setBorderPainted(true);
        action.add(encode);

        // decode action
        JButton decode = JButtonUtils.createNonOpaqueButton("中文转Unicode ↑");
        decode.setBorderPainted(true);
        action.add(decode);

        // decode content
        Editor decodeEditor = EditorComponentUtils.createEditorPanel(ProjectUtils.getCurrProject(), LightVirtualType.JSON);
        decodeEditor.getSettings().setLineNumbersShown(false);
        JComponent decodeComponent = decodeEditor.getComponent();
        urlEncode.add(decodeComponent);

        int componentHeight = (height - action.getPreferredSize().height) / 2 - 20;
        decodeComponent.setPreferredSize(JBUI.size(width, componentHeight));
        encodeComponent.setPreferredSize(JBUI.size(width, componentHeight));

        encode.addActionListener(e -> {
            notifyException(() -> {
                // Unicode转中文
                String text = encodeEditor.getDocument().getText();
                String unescapeJava = StringEscapeUtils.unescapeJava(text);
                EditorComponentUtils.write(decodeEditor, unescapeJava, false);
            }, encode, "Unicode转中文, 请检查参数是否正确");
        });

        decode.addActionListener(e -> {
            notifyException(() -> {
                // 中文转Unicode
                String text = decodeEditor.getDocument().getText();
                String escapeJava = StringEscapeUtils.escapeJava(text);
                EditorComponentUtils.write(encodeEditor, escapeJava, false);
            }, decode, "中文转Unicode, 请检查参数是否正确");
        });

        return urlEncode;
    }

    private JPanel urlEncode() {
        JPanel urlEncode = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        // encode content
        Editor encodeEditor = EditorComponentUtils.createEditorPanel(ProjectUtils.getCurrProject(), LightVirtualType.JSON);
        encodeEditor.getSettings().setLineNumbersShown(false);
        JComponent encodeComponent = encodeEditor.getComponent();
        urlEncode.add(encodeComponent);

        // action
        JPanel action = new JPanel(new FlowLayout(FlowLayout.LEFT));
        action.setBorder(BorderFactory.createEmptyBorder(-5, 0, -5, 0));
        urlEncode.add(action);
        action.add(new JLabel("字符编码："));
        ComboBox<String> character = new ComboBox<>();
        character.addItem("UTF-8");
        character.addItem("GBK");
        character.addItem("UTF-16");
        character.addItem("ISO-8859-1");
        action.add(character);

        // encode action
        JButton encode = JButtonUtils.createNonOpaqueButton("Url编码 ↓");
        encode.setBorderPainted(true);
        action.add(encode);

        // decode action
        JButton decode = JButtonUtils.createNonOpaqueButton("Url解码 ↑");
        decode.setBorderPainted(true);
        action.add(decode);

        // decode content
        Editor decodeEditor = EditorComponentUtils.createEditorPanel(ProjectUtils.getCurrProject(), LightVirtualType.JSON);
        decodeEditor.getSettings().setLineNumbersShown(false);
        JComponent decodeComponent = decodeEditor.getComponent();
        urlEncode.add(decodeComponent);

        int componentHeight = (height - action.getPreferredSize().height) / 2 - 20;
        decodeComponent.setPreferredSize(JBUI.size(width, componentHeight));
        encodeComponent.setPreferredSize(JBUI.size(width, componentHeight));

        encode.addActionListener(e -> {
            notifyException(() -> {
                try {
                    String encodeStr = URLEncoder.encode(encodeEditor.getDocument().getText(), (String) character.getSelectedItem());
                    EditorComponentUtils.write(decodeEditor, encodeStr, false);
                } catch (UnsupportedEncodingException ex) {
                    throw new RuntimeException(ex);
                }
            }, encode, "URL编码失败, 请检查URL是否正确");
        });

        decode.addActionListener(e -> {
            notifyException(() -> {
                try {
                    String encodeStr = URLDecoder.decode(decodeEditor.getDocument().getText(), (String) character.getSelectedItem());
                    EditorComponentUtils.write(encodeEditor, encodeStr, false);
                } catch (UnsupportedEncodingException ex) {
                    throw new RuntimeException(ex);
                }
            }, decode, "URL解码失败, 请检查URL是否正确");
        });

        return urlEncode;
    }

    private final Base64.Decoder base64Decoder = Base64.getDecoder();
    private final Base64.Encoder baseEncoder = Base64.getEncoder();

    private JPanel base64Encode() {
        JPanel base64Encode = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        // encode content
        Editor encodeEditor = EditorComponentUtils.createEditorPanel(ProjectUtils.getCurrProject(), LightVirtualType.JSON);
        encodeEditor.getSettings().setLineNumbersShown(false);
        JComponent encodeComponent = encodeEditor.getComponent();
        base64Encode.add(encodeComponent);

        // action
        JPanel action = new JPanel(new FlowLayout(FlowLayout.LEFT));
        action.setBorder(BorderFactory.createEmptyBorder(-5, 0, -5, 0));
        base64Encode.add(action);
        action.add(new JLabel("字符编码："));
        ComboBox<String> character = new ComboBox<>();
        character.addItem("UTF-8");
        character.addItem("GBK");
        character.addItem("UTF-16");
        character.addItem("ISO-8859-1");
        action.add(character);

        // encode action
        JButton encode = JButtonUtils.createNonOpaqueButton("Base64编码 ↓");
        encode.setBorderPainted(true);
        action.add(encode);

        // decode action
        JButton decode = JButtonUtils.createNonOpaqueButton("Base64解码 ↑");
        decode.setBorderPainted(true);
        action.add(decode);

        // decode content
        Editor decodeEditor = EditorComponentUtils.createEditorPanel(ProjectUtils.getCurrProject(), LightVirtualType.JSON);
        decodeEditor.getSettings().setLineNumbersShown(false);
        JComponent decodeComponent = decodeEditor.getComponent();
        base64Encode.add(decodeComponent);

        int componentHeight = (height - action.getPreferredSize().height) / 2 - 20;
        decodeComponent.setPreferredSize(JBUI.size(width, componentHeight));
        encodeComponent.setPreferredSize(JBUI.size(width, componentHeight));

        encode.addActionListener(e -> {
            notifyException(() -> {
                try {
                    String text = encodeEditor.getDocument().getText();
                    String encodeStr = baseEncoder.encodeToString(text.getBytes((String) character.getSelectedItem()));
                    EditorComponentUtils.write(decodeEditor, encodeStr, false);
                } catch (UnsupportedEncodingException ex) {
                    throw new RuntimeException(ex);
                }
            }, encode, "Base64编码失败, 请检查参数是否正确");
        });

        decode.addActionListener(e -> {
            notifyException(() -> {
                try {
                    String text = decodeEditor.getDocument().getText();
                    String decodeStr = new String(base64Decoder.decode(text), (String) character.getSelectedItem());
                    EditorComponentUtils.write(encodeEditor, decodeStr, false);
                } catch (UnsupportedEncodingException ex) {
                    throw new RuntimeException(ex);
                }
            }, decode, "Base64解码失败, 请检查参数是否正确");
        });

        return base64Encode;
    }

    private JPanel charEncode() {
        JPanel urlEncode = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        // encode content
        Editor encodeEditor = EditorComponentUtils.createEditorPanel(ProjectUtils.getCurrProject(), LightVirtualType.JSON);
        encodeEditor.getSettings().setLineNumbersShown(false);
        JComponent encodeComponent = encodeEditor.getComponent();
        urlEncode.add(encodeComponent);

        // action
        JPanel action = new JPanel(new FlowLayout(FlowLayout.LEFT));
        action.setBorder(BorderFactory.createEmptyBorder(-5, 0, -5, 0));
        urlEncode.add(action);
        action.add(new JLabel("字符编码："));
        ComboBox<String> character = new ComboBox<>();
        character.addItem("UTF-8");
        character.addItem("GBK");
        character.addItem("UTF-16");
        character.addItem("ISO-8859-1");
        action.add(character);

        JCheckBox checkBox16Prefix = new JBCheckBox("十六进制带 \\x 前缀  ");
        action.add(checkBox16Prefix);
        JCheckBox checkBox16Upcase = new JBCheckBox("十六进制大写  ");
        action.add(checkBox16Upcase);

        // encode action
        JButton encode = JButtonUtils.createNonOpaqueButton("编码 ↓");
        encode.setBorderPainted(true);
        action.add(encode);

        // decode action
        JButton decode = JButtonUtils.createNonOpaqueButton("解码 ↑");
        decode.setBorderPainted(true);
        action.add(decode);

        // decode content
        Editor decodeEditor = EditorComponentUtils.createEditorPanel(ProjectUtils.getCurrProject(), LightVirtualType.JSON);
        decodeEditor.getSettings().setLineNumbersShown(false);
        JComponent decodeComponent = decodeEditor.getComponent();
        urlEncode.add(decodeComponent);

        int componentHeight = (height - action.getPreferredSize().height) / 2 - 20;
        decodeComponent.setPreferredSize(JBUI.size(width, componentHeight));
        encodeComponent.setPreferredSize(JBUI.size(width, componentHeight));
        encode.addActionListener(e -> {
            notifyException(() -> {
                try {
                    String text = encodeEditor.getDocument().getText();
                    String charsetName = (String) character.getSelectedItem();

                    HexFormat hexFormat = checkBox16Upcase.isSelected() ? HexFormat.of().withUpperCase() : HexFormat.of();
                    String encodeTxt = checkBox16Prefix.isSelected() ? toHexDigits(hexFormat, text, charsetName) : hexFormat.formatHex(text.getBytes(charsetName));
                    EditorComponentUtils.write(decodeEditor, encodeTxt, false);
                } catch (UnsupportedEncodingException ex) {
                    throw new RuntimeException(ex);
                }
            }, encode, "字符编码失败, 请检查参数是否正确");
        });

        decode.addActionListener(e -> {
            notifyException(() -> {
                try {
                    String text = decodeEditor.getDocument().getText();
                    text = checkBox16Prefix.isSelected() ? text.replaceAll("\\\\x", StringUtils.EMPTY) : text;
                    String charsetName = (String) character.getSelectedItem();

                    HexFormat hexFormat = checkBox16Upcase.isSelected() ? HexFormat.of().withUpperCase() : HexFormat.of();
                    String decodeTxt = new String(hexFormat.parseHex(text), charsetName);
                    EditorComponentUtils.write(encodeEditor, decodeTxt, false);
                } catch (UnsupportedEncodingException ex) {
                    throw new RuntimeException(ex);
                }
            }, decode, "字符解码失败, 请检查参数是否正确");
        });

        return urlEncode;
    }

    private Component byteArrayEncode() {
        JPanel urlEncode = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        // encode content
        Editor encodeEditor = EditorComponentUtils.createEditorPanel(ProjectUtils.getCurrProject(), LightVirtualType.JSON);
        JComponent encodeComponent = encodeEditor.getComponent();
        urlEncode.add(encodeComponent);

        // action
        JPanel action = new JPanel(new FlowLayout(FlowLayout.LEFT));
        action.setBorder(BorderFactory.createEmptyBorder(-5, 0, -5, 0));
        urlEncode.add(action);
        action.add(new JLabel("字符编码："));
        ComboBox<String> character = new ComboBox<>();
        character.addItem("UTF-8");
        character.addItem("GBK");
        character.addItem("UTF-16");
        character.addItem("ISO-8859-1");
        action.add(character);

        // test cherry-pick
        ButtonGroup group = new ButtonGroup();
        JBRadioButton radio10 = new JBRadioButton("十进制  ");
        radio10.setSelected(true);
        JBRadioButton radio16 = new JBRadioButton("十六进制  ");
        group.add(radio10);
        group.add(radio16);
        action.add(radio10);
        action.add(radio16);

        // encode action
        JButton encode = JButtonUtils.createNonOpaqueButton("编码 ↓");
        encode.setBorderPainted(true);
        action.add(encode);

        // decode action
        JButton decode = JButtonUtils.createNonOpaqueButton("解码 ↑");
        decode.setBorderPainted(true);
        action.add(decode);

        // decode content
        Editor decodeEditor = EditorComponentUtils.createEditorPanel(ProjectUtils.getCurrProject(), LightVirtualType.JSON);
        JComponent decodeComponent = decodeEditor.getComponent();
        urlEncode.add(decodeComponent);

        int componentHeight = (height - action.getPreferredSize().height) / 2 - 20;
        decodeComponent.setPreferredSize(JBUI.size(width, componentHeight));
        encodeComponent.setPreferredSize(JBUI.size(width, componentHeight));
        encode.addActionListener(e -> {
            notifyException(() -> {
                try {
                    String text = encodeEditor.getDocument().getText();
                    String charsetName = (String) character.getSelectedItem();
                    String encodeTxt = radio10.isSelected() ? text.getBytes(charsetName).toString() : "";
                    EditorComponentUtils.write(decodeEditor, encodeTxt, false);
                } catch (UnsupportedEncodingException ex) {
                    throw new RuntimeException(ex);
                }
            }, encode, "字节数组编码失败, 请检查参数是否正确");
        });

        decode.addActionListener(e -> {
            notifyException(() -> {
                try {
                    String text = decodeEditor.getDocument().getText();
                    String charsetName = (String) character.getSelectedItem();
                    String decodeTxt = radio10.isSelected() ? byteArrayStr(text.getBytes(charsetName)) : "";
                    EditorComponentUtils.write(encodeEditor, decodeTxt, false);
                } catch (UnsupportedEncodingException ex) {
                    throw new RuntimeException(ex);
                }
            }, decode, "字节数组解码失败, 请检查参数是否正确");
        });

        return urlEncode;
    }

    @SneakyThrows
    private String toHexDigits(HexFormat hexFormat, String text, String charsetName) {
        StringBuilder sb = new StringBuilder();
        byte[] bytes = text.getBytes(charsetName);
        char[] charArray = text.toCharArray();
        int j = 0;

        int chineseCount = getChineseCount(charArray);
        int chineseByteLength = chineseCount > 0 ? (bytes.length - charArray.length) / chineseCount : 0;
        for (int i = 0; i < bytes.length; i++) {
            char c = charArray[j++];
            if (isChinese(c)) {
                sb.append("\\x").append(hexFormat.toHexDigits(bytes[i]));
                for (int i1 = 0; i1 < chineseByteLength; i1++) {
                    sb.append(hexFormat.toHexDigits(bytes[++i]));
                }
            } else {
                sb.append("\\x").append(hexFormat.toHexDigits(bytes[i]));
            }
        }

        return sb.toString();
    }

    private static int getChineseCount(char[] charArray) {
        int chineseCount = 0;
        for (char c : charArray) {
            if (isChinese(c)) chineseCount++;
        }
        return chineseCount;
    }

    private static final boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {
            return true;
        }
        return false;
    }
}
