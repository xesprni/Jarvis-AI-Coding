package com.qihoo.finance.lowcode.kit.ui.digest;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.kit.ui.KitDialog;
import lombok.extern.slf4j.Slf4j;
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
public class EncryptKitTab extends KitDialog {
    public static final String HMAC_MD5 = "HmacMD5";
    public static final String HMAC_SHA1 = "HmacSHA1";
    public static final String HMAC_SHA384 = "HmacSHA384";
    public static final String HMAC_SHA256 = "HmacSHA256";
    public static final String HMAC_SHA512 = "HmacSHA512";
    private static final Dimension size = new Dimension(100, -1);

    public EncryptKitTab(@Nullable Project project) {
        super(project);
    }

    @Override
    public @Nullable JComponent createCenterPanel() {
        JTabbedPane tab = new JTabbedPane();
        tab.setPreferredSize(JBUI.size(width, height));
        tab.add("   AES   ", holder());
        tab.add("   DES   ", holder());
        tab.add("   RSA   ", holder());
        return tab;
    }
}
