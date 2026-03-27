package com.qihoo.finance.lowcode.common.action;


import com.intellij.ide.ui.LafManagerListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.util.ui.JBUI;
import com.qifu.utils.Conversation;
import com.qifu.utils.ConversationStore;
import com.qihoo.finance.lowcode.common.factory.ChatXToolWindowFactory;
import com.qihoo.finance.lowcode.common.util.IconUtil;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.NotifyUtils;
import com.qihoo.finance.lowcode.smartconversation.panels.SmartToolWindowPanel;
import com.qihoo.finance.lowcode.smartconversation.panels.SmartToolWindowTabPanel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import static com.qihoo.finance.lowcode.common.ui.base.BasePanel.getUserInfo;

/**
 * @author weiyichao
 * @date 2025-11-04
 **/
@Slf4j
public class ChatHistoryAction extends AnAction {

    private final Project project;
    private JBPopup popup;

    public ChatHistoryAction(@NotNull Project project) {
        this.project = project;
        updateDisplayText();
    }

    private void updateDisplayText() {
        getTemplatePresentation().setText("会话历史");
        Icon icon = IconUtil.getThemeAwareIcon(Icons.AI_HISTORY_LIGHT, Icons.AI_HISTORY, 20);
        getTemplatePresentation().setIcon(icon);
        getTemplatePresentation().setDescription("查看最近的会话记录");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (popup != null && popup.isVisible()) {
            popup.cancel();
            return;
        }

        var conversationList = ConversationStore.getConversations(project);

        if (conversationList.isEmpty()) {
            JOptionPane.showMessageDialog(null, "暂无会话记录");
        }

        // 创建内容面板
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(new JBColor(new Color(245, 245, 245), new Color(55, 55, 55)));

        // 按时间分组显示会话
        addGroupedConversations(contentPanel, conversationList);

        // 滚动区域
        JBScrollPane scrollPane = new JBScrollPane(contentPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setPreferredSize(new Dimension(320, 240));

        popup = JBPopupFactory.getInstance().createComponentPopupBuilder(scrollPane, contentPanel).setTitle("会话历史").setResizable(true).setMovable(false).setRequestFocus(true).setFocusable(true).setCancelOnClickOutside(true).createPopup();

        // 获取触发事件的组件
        Component sourceComponent = e.getInputEvent() != null ? e.getInputEvent().getComponent() : null;
        if (sourceComponent != null) {
            try {
                // 计算弹出框位置，确保右对齐且不超出IDE边界
                Point locationOnScreen = sourceComponent.getLocationOnScreen();
                Dimension sourceSize = sourceComponent.getSize();

                // 获取弹出框的预估大小
                Dimension popupSize = new Dimension(160, 120); // 使用预估大小避免null
                try {
                    Dimension preferredSize = popup.getContent().getPreferredSize();
                    if (preferredSize != null) {
                        popupSize = preferredSize;
                    }
                } catch (Exception ignored) {
                    // 使用默认大小
                }

                // 计算右对齐位置（从按钮右侧向左展开）
                int x = locationOnScreen.x + sourceSize.width - popupSize.width;
                int y = locationOnScreen.y + sourceSize.height;

                // 确保不超出屏幕左边界
                if (x < 0) {
                    x = locationOnScreen.x;
                }

                popup.showInScreenCoordinates(sourceComponent, new Point(x, y));
            } catch (Exception ex) {
                // 如果计算位置出错，使用默认方式
                popup.showUnderneathOf(sourceComponent);
            }
        } else {
            // 备用方案：使用最佳位置
            popup.showInBestPositionFor(e.getDataContext());
        }
    }

    /**
     * 按时间分组添加会话
     */
    private void addGroupedConversations(JPanel contentPanel, java.util.List<Conversation> conversationList) {
        long currentTime = System.currentTimeMillis();
        long sevenDaysAgo = currentTime - 7 * 24 * 60 * 60 * 1000L;
        long oneMonthAgo = currentTime - 30 * 24 * 60 * 60 * 1000L;

        java.util.List<Conversation> today = new java.util.ArrayList<>();
        java.util.List<Conversation> sevenDays = new java.util.ArrayList<>();
        java.util.List<Conversation> oneMonth = new java.util.ArrayList<>();
        java.util.List<Conversation> older = new java.util.ArrayList<>();

        for (Conversation conversation : conversationList) {
            long timestamp = conversation.getCreatedTime();
            if (String.valueOf(timestamp).length() == 10) {
                timestamp *= 1000;
            }

            if (isSameDay(timestamp, currentTime)) {
                today.add(conversation);
            } else if (timestamp >= sevenDaysAgo) {
                sevenDays.add(conversation);
            } else if (timestamp >= oneMonthAgo) {
                oneMonth.add(conversation);
            } else {
                older.add(conversation);
            }
        }

        if (!today.isEmpty()) {
            addGroupSection(contentPanel, "今天", today);
        }
        if (!sevenDays.isEmpty()) {
            addGroupSection(contentPanel, "近7天", sevenDays);
        }
        if (!oneMonth.isEmpty()) {
            addGroupSection(contentPanel, "近一个月", oneMonth);
        }
        if (!older.isEmpty()) {
            addGroupSection(contentPanel, "更早", older);
        }
    }

    private boolean isSameDay(long timestamp1, long timestamp2) {
        LocalDateTime date1 = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp1), ZoneId.systemDefault());
        LocalDateTime date2 = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp2), ZoneId.systemDefault());
        return date1.toLocalDate().equals(date2.toLocalDate());
    }

    private void addGroupSection(JPanel contentPanel, String title, java.util.List<Conversation> conversations) {
        // ==== 分组头部容器 ====
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new JBColor(new Color(245, 245, 245), new Color(55, 55, 55))); // 背景条颜色（略深）
        headerPanel.setBorder(JBUI.Borders.empty(4, 10, 4, 10));
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26)); // 限制高度
        headerPanel.setPreferredSize(new Dimension(0, 26));

        // ==== 分组标题 ====
        JLabel groupLabel = new JLabel(title);
        groupLabel.setFont(groupLabel.getFont().deriveFont(Font.BOLD, 11f));
        groupLabel.setForeground(new JBColor(new Color(100, 100, 100), new Color(200, 200, 200)));
        groupLabel.setHorizontalAlignment(SwingConstants.LEFT);
        groupLabel.setOpaque(false);

        headerPanel.add(groupLabel, BorderLayout.WEST);

        // ==== 可选的底部分割线 ====
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setForeground(new Color(70, 70, 70));
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        separator.setPreferredSize(new Dimension(0, 1));
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ==== 添加到主容器 ====
        contentPanel.add(headerPanel);
        contentPanel.add(separator);

        // ==== 添加会话项 ====
        for (Conversation conversation : conversations) {
            contentPanel.add(createConversationCard(conversation));
        }

        // ==== 分组间距 ====
        contentPanel.add(Box.createVerticalStrut(4));
    }

    /**
     * 构建单条会话展示卡片
     */
    private JPanel createConversationCard(Conversation conversation) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(JBUI.Borders.empty(2, 8)); // 减少内边距
        card.setBackground(new JBColor(new Color(245, 245, 245), new Color(45, 45, 45)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28)); // 固定高度
        card.setPreferredSize(new Dimension(0, 28));
        card.setMinimumSize(new Dimension(0, 28));

        // ==== 标题 ====
        String fullTitle = "🧠 " + conversation.getTitle();
        JLabel titleLabel = new JLabel(fullTitle);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 11f)); // 稍微减小字体
        titleLabel.setForeground(new JBColor(new Color(50, 50, 50), new Color(230, 230, 230)));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLabel.setBorder(null);

        // 设置文本截断，超长时显示省略号
        FontMetrics fm = titleLabel.getFontMetrics(titleLabel.getFont());
        int maxWidth = 200; // 预留时间显示空间
        if (fm.stringWidth(fullTitle) > maxWidth) {
            String truncatedTitle = truncateText(fullTitle, fm, maxWidth);
            titleLabel.setText(truncatedTitle);
        }

        // ==== 时间（右对齐） ====
        JLabel timeLabel = new JLabel(formatTimestamp(conversation.getCreatedTime()));
        timeLabel.setFont(timeLabel.getFont().deriveFont(9f)); // 减小时间字体
        timeLabel.setForeground(new Color(150, 150, 150));
        timeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        timeLabel.setBorder(null);

        // ==== 删除按钮 ====
        JButton deleteButton = new JButton("✕");
        deleteButton.setFont(deleteButton.getFont().deriveFont(Font.BOLD, 10f));
        deleteButton.setForeground(new Color(150, 150, 150));
        deleteButton.setBackground(null);
        deleteButton.setBorder(JBUI.Borders.empty(0, 6));
        deleteButton.setOpaque(false);
        deleteButton.setContentAreaFilled(false);
        deleteButton.setBorderPainted(false);
        deleteButton.setFocusPainted(false);
        deleteButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        deleteButton.setPreferredSize(new Dimension(20, 20));
        
        // 鼠标悬停效果
        deleteButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                deleteButton.setForeground(new Color(255, 80, 80));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                deleteButton.setForeground(new Color(150, 150, 150));
            }
        });

        // 删除按钮点击事件
        deleteButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                null,
                "确定要删除这条会话吗？",
                "确认删除",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            
            if (confirm == JOptionPane.YES_OPTION) {
                ConversationStore.deleteConversation(project, conversation.getId());
                NotifyUtils.notify(project, "", "会话已删除", NotificationType.INFORMATION, null);
                
                // 关闭并重新打开弹窗以刷新列表
                if (popup != null) {
                    popup.cancel();
                }
            }
        });

        // ==== 右侧面板（时间 + 删除按钮） ====
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        rightPanel.setOpaque(false);
        rightPanel.add(timeLabel);
        rightPanel.add(deleteButton);

        // ==== 顶部面板（标题 + 右侧面板） ====
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(null);
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(rightPanel, BorderLayout.EAST);

        card.add(headerPanel, BorderLayout.CENTER);

        // ==== 鼠标交互 ====
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(new JBColor(new Color(230, 230, 230), new Color(60, 60, 60)));
                card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(new JBColor(new Color(245, 245, 245), new Color(45, 45, 45)));
                card.setCursor(Cursor.getDefaultCursor());
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (popup != null) {
                    popup.cancel();
                }
                openHistory(conversation.getId(), conversation.getTitle());
            }
        });

        return card;
    }


    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        // 每次UI更新时都刷新显示文本
        String email = Objects.toString(getUserInfo().email, StringUtils.EMPTY);
        e.getPresentation().setVisible(StringUtils.isNotBlank(email));
    }

    private void openHistory(String taskId, String title) {
        ChatXToolWindowFactory.showFirstTab();
        Content content = ChatXToolWindowFactory.getToolWindow().getContentManager().getSelectedContent();
        if (content == null) {
            NotifyUtils.notify("打开历史会话失败", NotificationType.WARNING);
            return;
        }
        if (content.getComponent() instanceof SmartToolWindowPanel smartToolWindowPanel) {
            SmartToolWindowTabPanel smartToolWindowTabPanel = new SmartToolWindowTabPanel(project, taskId);
            if (!smartToolWindowPanel.getChatTabbedPane().hasTabExist(taskId)) {
                smartToolWindowPanel.getChatTabbedPane().addNewTab(smartToolWindowTabPanel, taskId, title);
            }
            smartToolWindowPanel.getChatTabbedPane().trySwitchTab(taskId);
        }
    }

    /**
     * 截断文本，超长时显示省略号
     */
    private String truncateText(String text, FontMetrics fm, int maxWidth) {
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int ellipsisWidth = fm.stringWidth(ellipsis);
        int availableWidth = maxWidth - ellipsisWidth;

        if (availableWidth <= 0) {
            return ellipsis;
        }

        // 二分查找最合适的截断长度
        int left = 0, right = text.length();
        String result = text;

        while (left < right) {
            int mid = (left + right + 1) / 2;
            String candidate = text.substring(0, mid);

            if (fm.stringWidth(candidate) <= availableWidth) {
                result = candidate + ellipsis;
                left = mid;
            } else {
                right = mid - 1;
            }
        }

        return result;
    }

    public static String formatTimestamp(long timestamp) {
        // 判断是否为秒级时间戳（10位）还是毫秒级（13位）
        if (String.valueOf(timestamp).length() == 10) {
            timestamp *= 1000;
        }
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
