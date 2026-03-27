package com.qihoo.finance.lowcode.kit.ui.json;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.util.ui.JBUI;
import com.qihoo.finance.lowcode.common.enums.LightVirtualType;
import com.qihoo.finance.lowcode.common.util.*;
import com.qihoo.finance.lowcode.gentracker.tool.JSON;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import com.qihoo.finance.lowcode.kit.ui.KitDialog;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

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
public class JsonKitDialog extends KitDialog {
    private static final String errMsg = "转换失败, 请检查JSON格式是否正确";

    public JsonKitDialog(@Nullable Project project) {
        super(project);
        setTitle("JSON格式化");
    }

    public static void showDialog() {
        new JsonKitDialog(ProjectUtils.getCurrProject()).show();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel content = new JPanel(new BorderLayout());
        content.setPreferredSize(JBUI.size(width, height));
        // json area
        Editor editor = EditorComponentUtils.createEditorPanel(ProjectUtils.getCurrProject(), LightVirtualType.JSON);
        content.add(editor.getComponent(), BorderLayout.CENTER);
        // btn
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        Dimension size = new Dimension(30, 30);

        JButton format = JButtonUtils.createNonOpaqueButton(Icons.scaleToWidth(Icons.JSON, 16), size);
        format.setToolTipText("格式化JSON");
        format.setBorder(BorderFactory.createEmptyBorder(0, -10, 0, -10));
        format.addActionListener(e -> format(editor, format));
        btnPanel.add(format);

        JButton compress = JButtonUtils.createNonOpaqueButton(Icons.scaleToWidth(Icons.COMPRESS, 16), size);
        compress.setBorder(BorderFactory.createEmptyBorder(0, -10, 0, -10));
        compress.setToolTipText("压缩JSON");
        compress.addActionListener(e -> compress(editor, compress));
        btnPanel.add(compress);

        JButton compressTransfer = JButtonUtils.createNonOpaqueButton(Icons.scaleToWidth(Icons.COMPRESS_TRANSFER, 16), size);
        compressTransfer.setToolTipText("压缩并转义JSON");
        compressTransfer.setBorder(BorderFactory.createEmptyBorder(0, -10, 0, -10));
        compressTransfer.addActionListener(e -> compressTransfer(editor, compressTransfer));
        btnPanel.add(compressTransfer);

        // split
        btnPanel.add(new JLabel("|"));

        JButton toXml = JButtonUtils.createNonOpaqueButton("<XML>");
        toXml.setToolTipText("JSON转XML并复制");
        toXml.setBorder(BorderFactory.createEmptyBorder(0, -10, 0, -10));
        toXml.addActionListener(e -> toXml(editor, toXml));
        btnPanel.add(toXml);

//        JButton toTypeScript = JButtonUtils.createNonOpaqueButton("<TypeScript>");
//        toTypeScript.setToolTipText("JSON转TypeScript并复制");
//        toTypeScript.setBorder(BorderFactory.createEmptyBorder(0, -10, 0, -10));
//        toTypeScript.addActionListener(e -> toTypeScript(editor, toTypeScript));
//        btnPanel.add(toTypeScript);

        btnPanel.setBorder(BorderFactory.createEmptyBorder(-3, 0, -3, 0));
        content.add(btnPanel, BorderLayout.SOUTH);
        content.setBorder(BorderFactory.createEmptyBorder(-5, 0, -5, 0));
        return content;
    }

    private void toTypeScript(Editor editor, JButton toTypeScript) {
        notifyException(() -> {
            String text = editor.getDocument().getText();
            if (StringUtils.isEmpty(text)) return;

            String unescapeJson = StringEscapeUtils.unescapeJson(text);
            String xml = JSON.toXml(unescapeJson);
            JPanelUtils.copyToClipboard(xml);
            NotifyUtils.notifyBalloon(toTypeScript, "TypeScript已复制到剪贴板", MessageType.INFO, true);
        }, toTypeScript, errMsg);
        NotifyUtils.notifyBalloon(toTypeScript, "TypeScript已复制到剪贴板", MessageType.INFO, true);
    }

    private void toXml(Editor editor, JButton toXml) {
        notifyException(() -> {
            String text = editor.getDocument().getText();
            if (StringUtils.isEmpty(text)) return;

            String unescapeJson = StringEscapeUtils.unescapeJson(text);
            String xml = JSON.toXml(unescapeJson);
            JPanelUtils.copyToClipboard(xml);
            NotifyUtils.notifyBalloon(toXml, "XML已复制到剪贴板", MessageType.INFO, true);
        }, toXml, errMsg);
    }

    private void compressTransfer(Editor editor, JButton compressTransfer) {
        notifyException(() -> {
            String text = editor.getDocument().getText();
            if (StringUtils.isEmpty(text)) return;

            String json = JSON.compressJson(text);
            String escapeJson = StringEscapeUtils.escapeJson(json);
            ApplicationManager.getApplication().runWriteAction(() -> editor.getDocument().setText(escapeJson));
        }, compressTransfer, errMsg);
    }

    private void compress(Editor editor, JButton compress) {
        notifyException(() -> {
            String text = editor.getDocument().getText();
            if (StringUtils.isEmpty(text)) return;

            String unescapeJson = StringEscapeUtils.unescapeJson(text);
            String compressJson = JSON.compressJson(unescapeJson);
            ApplicationManager.getApplication().runWriteAction(() -> editor.getDocument().setText(compressJson));
        }, compress, errMsg);
    }

    private void format(Editor editor, JButton format) {
        notifyException(() -> {
            String text = editor.getDocument().getText();
            if (StringUtils.isEmpty(text)) return;

            String unescapeJson = StringEscapeUtils.unescapeJson(text);
            String formatJson = JSON.formatJson(unescapeJson, true);
            ApplicationManager.getApplication().runWriteAction(() -> editor.getDocument().setText(formatJson));
        }, format, errMsg);
    }
}
