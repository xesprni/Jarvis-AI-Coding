package com.qihoo.finance.lowcode.kit.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.JBColor;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.NotifyUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
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
@Slf4j
public abstract class KitDialog extends DialogWrapper {
    protected static int height = 550;
    protected static int width = 800;
    protected static long lastShowTime = 0;

    protected static boolean moment() {
        if (System.currentTimeMillis() - lastShowTime < 500) return true;
        lastShowTime = System.currentTimeMillis();
        return false;
    }

    public KitDialog(@Nullable Project project) {
        super(project);
        init();
        setModal(false);
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{};
    }

    @Override
    public void show() {
        if (moment()) return;
        super.show();
        this.getContentPanel().requestFocus();
    }

    protected void notifyException(Runnable runnable, JComponent component, String errMsg) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            NotifyUtils.notifyBalloon(component, errMsg, MessageType.ERROR, true);
        }
    }

    protected JComponent holder() {
        JLabel holder = new JLabel(Icons.scaleToWidth(Icons.HOLDER, 250));
        holder.setHorizontalAlignment(SwingConstants.CENTER);
        holder.setBorder(BorderFactory.createEmptyBorder(150, 0, 30, 0));

        JLabel holderLabel = new JLabel("敬请期待");
        holderLabel.setForeground(JBColor.GRAY);
        holderLabel.setFont(new Font("微软雅黑", Font.PLAIN, 20));
        holderLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel panel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        panel.add(holder);
        panel.add(holderLabel);
        return panel;
    }
}
