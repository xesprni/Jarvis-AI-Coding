package com.qihoo.finance.lowcode.aiquestion.ui.factory;

import com.intellij.ui.EditorTextField;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.qihoo.finance.lowcode.aiquestion.util.ColorUtil;
import com.qihoo.finance.lowcode.aiquestion.util.EditorUtil;
import com.qihoo.finance.lowcode.common.ui.base.RoundedPanel;
import com.qihoo.finance.lowcode.common.util.Icons;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CodePanelFactory {

    private final ImgButtonFactory imgButtonFactory = new ImgButtonFactory();
    private final TextPaneFactory textPaneFactory = new TextPaneFactory();
    private final static ExecutorService executorService = Executors.newFixedThreadPool(1);

    public JPanel create(String codeType, String code, Color backgroundColor) {
        JPanel codePanel = new RoundedPanel(backgroundColor, 5);
        codePanel.setLayout(new BorderLayout());
        Border emptyBorder = BorderFactory.createEmptyBorder(2, 2, 2, 2);
        codePanel.setBorder(emptyBorder);

        JPanel headerPanel = createHeaderPanel(codeType, code, ColorUtil.getCodeTitleBackground());
        codePanel.add(headerPanel, BorderLayout.NORTH);

        EditorTextField editor = textPaneFactory.createEditor(code);
        editor.setHorizontalSizeReferent(codePanel);
        codePanel.add(editor, BorderLayout.CENTER);

        codePanel.revalidate();
        codePanel.repaint();
        return codePanel;
    }

    private JPanel createHeaderPanel(String codeType, String code, Color backgroundColor) {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(backgroundColor);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(-5, 5, -5 ,5));
        // add username panel
        JLabel usernameLabel = new JLabel();
        usernameLabel.setText(codeType);
        usernameLabel.setForeground(JBColor.foreground());
        usernameLabel.setFont(usernameLabel.getFont().deriveFont(Font.BOLD));
        headerPanel.add(usernameLabel, BorderLayout.WEST);
        // add button panel
        JPanel btnPanel = new JPanel(new FlowLayout());
        JButton insertCodeButton = createInsertCodeButton(code, backgroundColor);
        btnPanel.add(insertCodeButton);
        btnPanel.setBackground(backgroundColor);
        JButton copyButton = createCopyButton(code, backgroundColor);
        btnPanel.add(copyButton);
        headerPanel.add(btnPanel, BorderLayout.EAST);
        return headerPanel;
    }

    private JButton createCopyButton(final String msg, Color backgroundColor) {
        JButton copyButton = imgButtonFactory.create("复制", Icons.AI_COPY, 13, 13
                , JBUI.insets(5), backgroundColor);
        copyButton.addActionListener(e -> {
            StringSelection selection = new StringSelection(msg);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, null);
            copyButton.setIcon(Icons.DONE);
            copyButton.setToolTipText("复制成功");
            executorService.submit(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {}
                copyButton.setIcon(Icons.AI_COPY);
                copyButton.setToolTipText("复制");
            });
        });
        return copyButton;
    }

    private JButton createInsertCodeButton(final String msg, Color backgroundColor) {
        JButton insertButton = imgButtonFactory.create("插入代码", Icons.INSERT_CODE, 13, 13
                , JBUI.insets(5), backgroundColor);
        insertButton.addActionListener(e -> {
            EditorUtil.insertTextToSelectedEditor(msg);
            insertButton.setIcon(Icons.DONE);
            executorService.submit(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {}
                insertButton.setIcon(Icons.INSERT_CODE);
            });
        });
        return insertButton;
    }
}
