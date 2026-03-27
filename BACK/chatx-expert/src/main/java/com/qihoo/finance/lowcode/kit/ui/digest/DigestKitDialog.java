package com.qihoo.finance.lowcode.kit.ui.digest;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import com.qihoo.finance.lowcode.common.ui.CustomHeightTabbedPaneUI;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import com.qihoo.finance.lowcode.kit.ui.KitDialog;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

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
public class DigestKitDialog extends KitDialog {
    private final Project project;

    public DigestKitDialog(@Nullable Project project) {
        super(project);
        this.project = project;
        setTitle("编码/解码");
    }

    public static void showDialog() {
        new DigestKitDialog(ProjectUtils.getCurrProject()).show();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JTabbedPane digestTab = new JBTabbedPane();

        JComponent encodeKit = new EncodeKitTab(project).createCenterPanel();
        encodeKit.setBorder(BorderFactory.createEmptyBorder(-5, -7, -7, -7));
        digestTab.add("编码/解码", encodeKit);

        JComponent digestKit = new DigestKitTab(project).createCenterPanel();
        digestKit.setBorder(BorderFactory.createEmptyBorder(-5, -7, -7, -7));
        digestTab.add("摘要算法(MD5/SHA)", digestKit);

        JComponent encryptKit = new EncryptKitTab(project).createCenterPanel();
        encryptKit.setBorder(BorderFactory.createEmptyBorder(-5, -7, -7, -7));
        digestTab.add("加密/解密(AES/DES/RSA)", encryptKit);

        digestTab.setUI(new CustomHeightTabbedPaneUI());
        digestTab.setPreferredSize(JBUI.size(width + 20, height + 50));
        return digestTab;
    }
}
