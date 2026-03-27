package com.qihoo.finance.lowcode.aiquestion.ui.factory;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.RoundedLineBorder;
import com.qihoo.finance.lowcode.aiquestion.ui.worker.ChatSwingWorker;
import com.qihoo.finance.lowcode.common.factory.ChatXToolWindowFactory;
import com.qihoo.finance.lowcode.common.ui.base.RoundedLabel;
import com.qihoo.finance.lowcode.common.util.Icons;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 停止生成工厂类
 */
@RequiredArgsConstructor
public class StopGenDialogFactory {

    private final Project project;
    private final Component relativeComponent;

    private ChatSwingWorker chatSwingWorker;

    private JDialog dialog;

    public JDialog create() {
        // 创建按钮
        JLabel stopGenBtn = new JLabel();
        stopGenBtn.setText("终止生成");
        stopGenBtn.setIcon(Icons.TERMINATE);
        stopGenBtn.setOpaque(false);
        stopGenBtn.setBackground(ChatXToolWindowFactory.BACKGROUND);
        stopGenBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                stopGenBtnClick(null);
            }
        });

        // 创建panel容纳按钮
        Border paddingBorder = BorderFactory.createEmptyBorder(5, 0, 5, 5);
        Border lineBorder = BorderFactory.createLineBorder(JBColor.border().brighter(), 1);
        stopGenBtn.setBorder(BorderFactory.createCompoundBorder(lineBorder, paddingBorder));

        // 创建dialog
        JFrame frame = WindowManager.getInstance().getFrame(project);
        dialog = new JDialog(frame, "终止生成", false);
        // 不展示标题栏
        dialog.setUndecorated(true);
        dialog.setContentPane(stopGenBtn);
        dialog.setPreferredSize(new Dimension(100, 40));

        dialog.pack();
        return dialog;
    }

    public void show(ChatSwingWorker chatSwingWorker) {
        Point location = relativeComponent.getLocationOnScreen();
        int x = location.x + (relativeComponent.getWidth() - dialog.getWidth()) / 2;
        int y = location.y - dialog.getHeight() - 5;
        dialog.setLocation(x, y);
        dialog.setVisible(true);
        this.chatSwingWorker = chatSwingWorker;
    }

    public void stopGenBtnClick(ActionEvent e) {
        if (this.chatSwingWorker != null) {
            this.chatSwingWorker.cancel(true);
            dialog.dispose(); // 关闭弹框
        }
    }

    public void close() {
        dialog.dispose();
    }
}
