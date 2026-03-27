package com.qihoo.finance.lowcode.kit.ui.digest;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.JBUI;
import com.qihoo.finance.lowcode.common.enums.LightVirtualType;
import com.qihoo.finance.lowcode.common.util.EditorComponentUtils;
import com.qihoo.finance.lowcode.common.util.JButtonUtils;
import com.qihoo.finance.lowcode.common.util.JPanelUtils;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import com.qihoo.finance.lowcode.kit.ui.KitDialog;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.*;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
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
public class DigestKitTab extends KitDialog {
    public static final String HMAC_MD5 = "HmacMD5";
    public static final String HMAC_SHA1 = "HmacSHA1";
    public static final String HMAC_SHA384 = "HmacSHA384";
    public static final String HMAC_SHA256 = "HmacSHA256";
    public static final String HMAC_SHA512 = "HmacSHA512";
    private static final Dimension size = new Dimension(100, -1);

    public DigestKitTab(@Nullable Project project) {
        super(project);
    }

    @Override
    public @Nullable JComponent createCenterPanel() {
        JTabbedPane tab = new JTabbedPane();
        tab.setPreferredSize(JBUI.size(width, height));
        tab.add("MD5", md5Encode());
        tab.add("SHA", shaEncode());
        tab.add("HMAC-MD5", hamcMd5());
        tab.add("HMAC-SHA", hamcSha());
        return tab;
    }

    private Component hamcSha() {
        JPanel content = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP));

        Editor keyEditor = EditorComponentUtils.createEditorPanel(ProjectUtils.getCurrProject(), LightVirtualType.JSON);
        keyEditor.getSettings().setLineNumbersShown(false);
        JComponent keyComponent = keyEditor.getComponent();
        content.add(JPanelUtils.settingPanel("密钥    ", keyComponent, null, null, true));

        // encode content
        content.add(new JLabel("密文    "));
        Editor encodeEditor = EditorComponentUtils.createEditorPanel(ProjectUtils.getCurrProject(), LightVirtualType.JSON);
        encodeEditor.getSettings().setLineNumbersShown(false);
        JComponent encodeComponent = encodeEditor.getComponent();
        content.add(encodeComponent);

        // action
        JPanel action = new JPanel(new FlowLayout(FlowLayout.LEFT));
        action.setBorder(BorderFactory.createEmptyBorder(-5, 0, -5, 0));
        content.add(action);
        action.add(new JLabel("字符编码："));
        ComboBox<String> character = new ComboBox<>();
        character.addItem("UTF-8");
        character.addItem("GBK");
        character.addItem("UTF-16");
        character.addItem("ISO-8859-1");
        action.add(character);

        // encode action
        JButton md5 = JButtonUtils.createNonOpaqueButton(" HMAC-SHA ");
        md5.setBorderPainted(true);
        action.add(md5);

        JCheckBox upcase = new JCheckBox("UpperCase  ");
        upcase.setSelected(true);
        action.add(upcase);

        // SHA1
        Editor sha1Editor = EditorComponentUtils.createEditorPanel(ProjectUtils.getCurrProject(), LightVirtualType.JSON);
        sha1Editor.getSettings().setLineNumbersShown(false);
        JComponent sha1 = sha1Editor.getComponent();
        content.add(JPanelUtils.settingPanel("HMAC-SHA1  ", sha1, size));
        // SHA256
        Editor sha256Editor = EditorComponentUtils.createEditorPanel(ProjectUtils.getCurrProject(), LightVirtualType.JSON);
        sha256Editor.getSettings().setLineNumbersShown(false);
        JComponent sha256 = sha256Editor.getComponent();
        content.add(JPanelUtils.settingPanel("HMAC-SHA256", sha256, size));
        // SHA384
        Editor sha384Editor = EditorComponentUtils.createEditorPanel(ProjectUtils.getCurrProject(), LightVirtualType.JSON);
        sha384Editor.getSettings().setLineNumbersShown(false);
        JComponent sha384 = sha384Editor.getComponent();
        sha384.setPreferredSize(new Dimension(-1, sha1.getPreferredSize().height + 14));
        content.add(JPanelUtils.settingPanel("HMAC-SHA384", sha384, size));
        // SHA512
        Editor sha512Editor = EditorComponentUtils.createEditorPanel(ProjectUtils.getCurrProject(), LightVirtualType.JSON);
        sha512Editor.getSettings().setLineNumbersShown(false);
        JComponent sha512 = sha512Editor.getComponent();
        sha512.setPreferredSize(new Dimension(-1, sha1.getPreferredSize().height + 14));
        content.add(JPanelUtils.settingPanel("HMAC-SHA512", sha512, size));

        int componentHeight = (height - action.getPreferredSize().height) / 2 - 60;
        encodeComponent.setPreferredSize(JBUI.size(width, componentHeight));

        md5.addActionListener(e -> {
            notifyException(() -> {
                try {
                    String key = keyEditor.getDocument().getText();
                    String text = encodeEditor.getDocument().getText();
                    String charsetName = (String) character.getSelectedItem();
                    byte[] bytes = text.getBytes(charsetName);

                    String sha1Str = encrypt(text, key, HMAC_SHA1, charsetName);
                    String sha256Str = encrypt(text, key, HMAC_SHA256, charsetName);
                    String sha384Str = encrypt(text, key, HMAC_SHA384, charsetName);
                    String sha512Str = encrypt(text, key, HMAC_SHA512, charsetName);

                    boolean uppercase = upcase.isSelected();
                    EditorComponentUtils.write(sha1Editor, uppercase ? sha1Str.toUpperCase() : sha1Str, false);
                    EditorComponentUtils.write(sha256Editor, uppercase ? sha256Str.toUpperCase() : sha256Str, false);
                    EditorComponentUtils.write(sha384Editor, uppercase ? sha384Str.toUpperCase() : sha384Str, false);
                    EditorComponentUtils.write(sha512Editor, uppercase ? sha512Str.toUpperCase() : sha512Str, false);
                } catch (UnsupportedEncodingException ex) {
                    throw new RuntimeException(ex);
                }
            }, md5, "加密失败, 请检查参数是否正确");
        });

        return content;
    }

    private Component hamcMd5() {
        JPanel content = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP));

        Editor keyEditor = EditorComponentUtils.createEditorPanel(ProjectUtils.getCurrProject(), LightVirtualType.JSON);
        keyEditor.getSettings().setLineNumbersShown(false);
        JComponent keyComponent = keyEditor.getComponent();
        content.add(JPanelUtils.settingPanel("密钥    ", keyComponent, null, null, true));

        // encode content
        content.add(new JLabel("密文    "));
        Editor encodeEditor = EditorComponentUtils.createEditorPanel(ProjectUtils.getCurrProject(), LightVirtualType.JSON);
        encodeEditor.getSettings().setLineNumbersShown(false);
        JComponent encodeComponent = encodeEditor.getComponent();
        content.add(encodeComponent);

        // action
        JPanel action = new JPanel(new FlowLayout(FlowLayout.LEFT));
        action.setBorder(BorderFactory.createEmptyBorder(-5, 0, -5, 0));
        content.add(action);
        action.add(new JLabel("字符编码："));
        ComboBox<String> character = new ComboBox<>();
        character.addItem("UTF-8");
        character.addItem("GBK");
        character.addItem("UTF-16");
        character.addItem("ISO-8859-1");
        action.add(character);

        // encode action
        JButton md5 = JButtonUtils.createNonOpaqueButton(" HMAC-MD5 ");
        md5.setBorderPainted(true);
        action.add(md5);

        JCheckBox upcase = new JCheckBox("UpperCase  ");
        upcase.setSelected(true);
        action.add(upcase);

        // SHA1
        Editor hmacMd5Editor = EditorComponentUtils.createEditorPanel(ProjectUtils.getCurrProject(), LightVirtualType.JSON);
        hmacMd5Editor.getSettings().setLineNumbersShown(false);
        JComponent hmacMd5 = hmacMd5Editor.getComponent();
        content.add(hmacMd5);

        int componentHeight = (height - action.getPreferredSize().height) / 2 - 60;
        encodeComponent.setPreferredSize(JBUI.size(width, componentHeight));
        hmacMd5.setPreferredSize(JBUI.size(width, componentHeight));

        md5.addActionListener(e -> {
            notifyException(() -> {
                try {
                    String key = keyEditor.getDocument().getText();
                    String text = encodeEditor.getDocument().getText();
                    String charsetName = (String) character.getSelectedItem();
                    byte[] bytes = text.getBytes(charsetName);

                    String HmacMd5Str = encrypt(text, key, HMAC_MD5, charsetName);
                    boolean uppercase = upcase.isSelected();
                    EditorComponentUtils.write(hmacMd5Editor, uppercase ? HmacMd5Str.toUpperCase() : HmacMd5Str, false);
                } catch (UnsupportedEncodingException ex) {
                    throw new RuntimeException(ex);
                }
            }, md5, "加密失败, 请检查参数是否正确");
        });

        return content;
    }

    public static String encrypt(String input, String key, String algorithm, String charsetName) {
        String cipher = "";
        try {
            byte[] data = key.getBytes(charsetName);
            //根据给定的字节数组构造一个密钥，第二个参数指定一个密钥的算法名称，生成HmacSHA1专属密钥
            SecretKey secretKey = new SecretKeySpec(data, algorithm);

            //生成一个指定Mac算法的Mac对象
            Mac mac = Mac.getInstance(algorithm);
            //用给定密钥初始化Mac对象
            mac.init(secretKey);
            byte[] text = input.getBytes(charsetName);
            byte[] encryptByte = mac.doFinal(text);
            cipher = bytesToHexStr(encryptByte);
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
        return cipher;
    }

    public static String bytesToHexStr(byte[] bytes) {
        StringBuilder hexStr = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            hexStr.append(hex);
        }
        return hexStr.toString();
    }

    private JPanel md5Encode() {
        JPanel md5Content = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        md5Content.add(new JLabel("原文  "));

        // encode content
        Editor encodeEditor = EditorComponentUtils.createEditorPanel(ProjectUtils.getCurrProject(), LightVirtualType.JSON);
        encodeEditor.getSettings().setLineNumbersShown(false);
        JComponent encodeComponent = encodeEditor.getComponent();
        md5Content.add(encodeComponent);

        // action
        JPanel action = new JPanel(new FlowLayout(FlowLayout.LEFT));
        action.setBorder(BorderFactory.createEmptyBorder(-5, 0, -5, 0));
        md5Content.add(action);
        action.add(new JLabel("字符编码："));
        ComboBox<String> character = new ComboBox<>();
        character.addItem("UTF-8");
        character.addItem("GBK");
        character.addItem("UTF-16");
        character.addItem("ISO-8859-1");
        action.add(character);

        // encode action
        JButton md5 = JButtonUtils.createNonOpaqueButton(" MD5 ");
        md5.setBorderPainted(true);
        action.add(md5);

        JCheckBox upcase = new JCheckBox("UpperCase  ");
        upcase.setSelected(true);
        action.add(upcase);

        // result
        // MD5-16
        Dimension size = new Dimension(80, -1);
        Editor md516Editor = EditorComponentUtils.createEditorPanel(ProjectUtils.getCurrProject(), LightVirtualType.JSON);
        md516Editor.getSettings().setLineNumbersShown(false);
        JComponent md516 = md516Editor.getComponent();
        md5Content.add(JPanelUtils.settingPanel("MD5-16位", md516, size));
        // MD5-32
        Editor md532Editor = EditorComponentUtils.createEditorPanel(ProjectUtils.getCurrProject(), LightVirtualType.JSON);
        md532Editor.getSettings().setLineNumbersShown(false);
        JComponent md532 = md532Editor.getComponent();
        md5Content.add(JPanelUtils.settingPanel("MD5-32位", md532, size));

        int componentHeight = (height - action.getPreferredSize().height) / 2 - 20;
        encodeComponent.setPreferredSize(JBUI.size(width, componentHeight));

        md5.addActionListener(e -> {
            notifyException(() -> {
                try {
                    String text = encodeEditor.getDocument().getText();
                    String charsetName = (String) character.getSelectedItem();

                    byte[] md5Bytes = DigestUtils.md5(text.getBytes(charsetName));
                    String md5Txt = Hex.encodeHexString(md5Bytes);
                    md5Txt = upcase.isSelected() ? md5Txt.toUpperCase() : md5Txt.toLowerCase();

                    EditorComponentUtils.write(md532Editor, md5Txt);
                    EditorComponentUtils.write(md516Editor, md5Txt.substring(8, 24));
                } catch (UnsupportedEncodingException ex) {
                    throw new RuntimeException(ex);
                }
            }, md5, "加密失败, 请检查参数是否正确");
        });

        return md5Content;
    }

    private final Base64.Decoder base64Decoder = Base64.getDecoder();
    private final Base64.Encoder baseEncoder = Base64.getEncoder();

    private JPanel shaEncode() {
        JPanel md5Content = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        md5Content.add(new JLabel("原文  "));

        // encode content
        Editor encodeEditor = EditorComponentUtils.createEditorPanel(ProjectUtils.getCurrProject(), LightVirtualType.JSON);
        encodeEditor.getSettings().setLineNumbersShown(false);
        JComponent encodeComponent = encodeEditor.getComponent();
        md5Content.add(encodeComponent);

        // action
        JPanel action = new JPanel(new FlowLayout(FlowLayout.LEFT));
        action.setBorder(BorderFactory.createEmptyBorder(-5, 0, -5, 0));
        md5Content.add(action);
        action.add(new JLabel("字符编码："));
        ComboBox<String> character = new ComboBox<>();
        character.addItem("UTF-8");
        character.addItem("GBK");
        character.addItem("UTF-16");
        character.addItem("ISO-8859-1");
        action.add(character);

        // encode action
        JButton md5 = JButtonUtils.createNonOpaqueButton(" SHA ");
        md5.setBorderPainted(true);
        action.add(md5);

        JCheckBox upcase = new JCheckBox("UpperCase  ");
        upcase.setSelected(true);
        action.add(upcase);

        // SHA1
        Dimension size = new Dimension(80, -1);
        Editor sha1Editor = EditorComponentUtils.createEditorPanel(ProjectUtils.getCurrProject(), LightVirtualType.JSON);
        sha1Editor.getSettings().setLineNumbersShown(false);
        JComponent sha1 = sha1Editor.getComponent();
        md5Content.add(JPanelUtils.settingPanel("SHA1  ", sha1, size));
        // SHA256
        Editor sha256Editor = EditorComponentUtils.createEditorPanel(ProjectUtils.getCurrProject(), LightVirtualType.JSON);
        sha256Editor.getSettings().setLineNumbersShown(false);
        JComponent sha256 = sha256Editor.getComponent();
        md5Content.add(JPanelUtils.settingPanel("SHA256", sha256, size));
        // SHA384
        Editor sha384Editor = EditorComponentUtils.createEditorPanel(ProjectUtils.getCurrProject(), LightVirtualType.JSON);
        sha384Editor.getSettings().setLineNumbersShown(false);
        JComponent sha384 = sha384Editor.getComponent();
        sha384.setPreferredSize(new Dimension(-1, sha1.getPreferredSize().height + 14));
        md5Content.add(JPanelUtils.settingPanel("SHA384", sha384, size));
        // SHA512
        Editor sha512Editor = EditorComponentUtils.createEditorPanel(ProjectUtils.getCurrProject(), LightVirtualType.JSON);
        sha512Editor.getSettings().setLineNumbersShown(false);
        JComponent sha512 = sha512Editor.getComponent();
        sha512.setPreferredSize(new Dimension(-1, sha1.getPreferredSize().height + 14));
        md5Content.add(JPanelUtils.settingPanel("SHA512", sha512, size));

        int componentHeight = (height - action.getPreferredSize().height) / 2 - 20;
        encodeComponent.setPreferredSize(JBUI.size(width, componentHeight));

        md5.addActionListener(e -> {
            notifyException(() -> {
                try {
                    String text = encodeEditor.getDocument().getText();
                    String charsetName = (String) character.getSelectedItem();
                    byte[] bytes = text.getBytes(charsetName);

                    String sha1Str = new DigestUtils(MessageDigestAlgorithms.SHA_1).digestAsHex(bytes);
                    String sha256Str = new DigestUtils(MessageDigestAlgorithms.SHA_256).digestAsHex(bytes);
                    String sha384Str = new DigestUtils(MessageDigestAlgorithms.SHA_384).digestAsHex(bytes);
                    String sha512Str = new DigestUtils(MessageDigestAlgorithms.SHA_512).digestAsHex(bytes);

                    boolean uppercase = upcase.isSelected();
                    EditorComponentUtils.write(sha1Editor, uppercase ? sha1Str.toUpperCase() : sha1Str, false);
                    EditorComponentUtils.write(sha256Editor, uppercase ? sha256Str.toUpperCase() : sha256Str, false);
                    EditorComponentUtils.write(sha384Editor, uppercase ? sha384Str.toUpperCase() : sha384Str, false);
                    EditorComponentUtils.write(sha512Editor, uppercase ? sha512Str.toUpperCase() : sha512Str, false);
                } catch (UnsupportedEncodingException ex) {
                    throw new RuntimeException(ex);
                }
            }, md5, "加密失败, 请检查参数是否正确");
        });

        return md5Content;
    }

    private JPanel charEncode() {
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

    @SneakyThrows
    private String toHexDigits(HexFormat hexFormat, String text, String charsetName) {
        StringBuilder sb = new StringBuilder();
        byte[] bytes = text.getBytes(charsetName);
        char[] charArray = text.toCharArray();
        int j = 0;

        int chineseCount = getChineseCount(charArray);
        int chineseByteLength = (bytes.length - charArray.length) / chineseCount;
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
