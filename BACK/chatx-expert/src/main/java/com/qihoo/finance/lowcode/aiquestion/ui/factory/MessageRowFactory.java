package com.qihoo.finance.lowcode.aiquestion.ui.factory;

import com.google.common.collect.Lists;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.qihoo.finance.lowcode.aiquestion.util.ColorUtil;
import com.qihoo.finance.lowcode.common.ui.base.RoundedLabel;
import com.qihoo.finance.lowcode.common.ui.base.RoundedPanel;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageRowFactory {

    private final ImgButtonFactory imgButtonFactory = new ImgButtonFactory();
    private final MessageBodyFactory messageBodyFactory = new MessageBodyFactory();
    private final static ExecutorService executorService = Executors.newFixedThreadPool(1);
    private static final Pattern LABEL_PATTERN = Pattern.compile("\\[#(.*?)]");

    public JPanel create(String username, String msg) {
        Color backgroundColor = getBackgroundColor(username);
        JPanel msgRowPanel = new RoundedPanel(backgroundColor, 20);
        msgRowPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        msgRowPanel.setLayout(new BorderLayout());
        // add header to row panel
        JPanel headerPanel = createHeaderPanel(/*username*/"You", msg, backgroundColor);
        msgRowPanel.add(headerPanel, BorderLayout.NORTH);
        // 标签信息
        Pair<String, JPanel> extractLabel = extractLabels(msg, backgroundColor);
        // message
        JPanel bodyPanel = messageBodyFactory.create(extractLabel.getLeft(), backgroundColor);

        JPanel contentPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, -5, 0));
        contentPanel.setBackground(backgroundColor);
        if (Objects.nonNull(extractLabel.getRight())) {
            contentPanel.add(extractLabel.getRight());
            bodyPanel.setBorder(BorderFactory.createEmptyBorder(-5, 0, 0, 0));
        }
        contentPanel.add(bodyPanel);
        msgRowPanel.add(contentPanel, BorderLayout.CENTER);

        return msgRowPanel;
    }


    @NotNull
    private static Pair<String, JPanel> extractLabels(String msg, Color backgroundColor) {
        // [#] 作为固定的标签提取符号, 如 [#标签1], [#标签2], 原文带标签发送至AI会话, 展示时, 以RoundedLabel展示标签

        List<String> labels = Lists.newArrayList();
        Matcher matcher = LABEL_PATTERN.matcher(msg);
        while (matcher.find()) {
            labels.add(matcher.group(1));
        }
        if (CollectionUtils.isEmpty(labels)) return Pair.of(msg, null);

        // 剪除标签
        for (String label : labels) {
            msg = msg.replace("[#" + label + "]", "");
        }
        // 标签展示
        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        labelPanel.setBorder(BorderFactory.createEmptyBorder(-5, 0, -5, 0));
        labelPanel.setBackground(backgroundColor);
        for (int i = 0; i < labels.size(); i++) {
            String label = labels.get(i);
            RoundedLabel roundedLabel = new RoundedLabel(label, RoundedLabel.randomColor(i), RoundedLabel.WHITE, 6);
            labelPanel.add(roundedLabel);
        }
        UIUtil.forEachComponentInHierarchy(labelPanel, c -> c.setBackground(backgroundColor));
        return Pair.of(msg, labelPanel);
    }

    private JPanel createHeaderPanel(String username, String msg, Color backgroundColor) {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(backgroundColor);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(-5, 2, -5, 10));
        // add username panel
        JLabel usernameLabel = new JLabel();
        usernameLabel.setText(username);
        usernameLabel.setIcon(GlobalDict.PLUGIN_NAME.equals(username) ?
                Icons.scaleToWidth(Icons.LOGO_ROUND, 18) : Icons.scaleToWidth(Icons.LOGIN_USER, 18));
//        usernameLabel.setForeground(JBColor.foreground());
        usernameLabel.setFont(usernameLabel.getFont().deriveFont(Font.BOLD));
        headerPanel.add(usernameLabel, BorderLayout.WEST);
        // add button panel
        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.setBackground(backgroundColor);
        JButton copyButton = createCopyButton(msg, backgroundColor);
        btnPanel.add(copyButton);
        headerPanel.add(btnPanel, BorderLayout.EAST);

        JSeparator mySeparator = new JSeparator(SwingConstants.HORIZONTAL);
        headerPanel.add(mySeparator, BorderLayout.SOUTH);
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

    private Color getBackgroundColor(String username) {
        return ColorUtil.getReplyContentBackground(username);
    }
}
