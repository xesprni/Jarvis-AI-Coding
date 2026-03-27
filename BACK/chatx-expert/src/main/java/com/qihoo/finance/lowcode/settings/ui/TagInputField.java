package com.qihoo.finance.lowcode.settings.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class TagInputField extends JPanel {
    private final List<String> tags = new ArrayList<>();
    private final JPanel tagsPanel;
    private final Color tagBackground = new Color(93, 94, 97);

    public TagInputField() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

        tagsPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

                g2.setColor(Color.GRAY);
                g2.setStroke(new BasicStroke(1));

                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            public void doLayout() {
                // 自定义布局：标签自动换行
                int x = 3, y = 3;
                int maxWidth = getWidth() ; // 留出边距
                int rowHeight = 0;

                for (Component comp : getComponents()) {
                    Dimension preferredSize = comp.getPreferredSize();
                    
                    // 如果当前行放不下，换行
                    if (x + preferredSize.width > maxWidth && x > 3) {
                        x = 3;
                        y += rowHeight + 3;
                        rowHeight = 0;
                    }
                    
                    comp.setBounds(x, y, preferredSize.width, preferredSize.height);
                    x += preferredSize.width + 3;
                    rowHeight = Math.max(rowHeight, preferredSize.height);
                }
                
                // 设置面板的首选高度，不限制最大值
                int totalHeight = y + rowHeight + 6;
                setPreferredSize(new Dimension(getWidth(), Math.max(totalHeight, 30)));
            }

            @Override
            public Dimension getPreferredSize() {
                // 计算所需的高度
                if (getComponentCount() == 0) {
                    return new Dimension(520, 30);
                }
                
                int maxWidth = 520; // 固定宽度
                
                int x = 3, y = 3;
                int rowHeight = 0;
                
                for (Component comp : getComponents()) {
                    Dimension preferredSize = comp.getPreferredSize();
                    
                    if (x + preferredSize.width > maxWidth && x > 3) {
                        x = 3;
                        y += rowHeight + 3;
                        rowHeight = 0;
                    }
                    
                    x += preferredSize.width + 3;
                    rowHeight = Math.max(rowHeight, preferredSize.height);
                }
                
                int totalHeight = y + rowHeight + 6;
                // 不限制最大高度，允许无限扩展
                return new Dimension(maxWidth, Math.max(totalHeight, 30));
            }
        };
        tagsPanel.setOpaque(false);
        add(tagsPanel, BorderLayout.CENTER);
    }

    public void addTag(String tag) {
        if (tag.isEmpty() || tags.contains(tag)) return;
        
        tags.add(tag);
        
        JPanel tagPanel = createTagPanel(tag);
        tagsPanel.add(tagPanel);
        
        // 触发布局重新计算和重绘，包括父容器
        tagsPanel.invalidate();
        tagsPanel.revalidate();
        tagsPanel.repaint();
        invalidate();
        revalidate();
        repaint();
        
        // 通知父容器更新布局
        Container parent = getParent();
        while (parent != null) {
            parent.revalidate();
            parent = parent.getParent();
        }
    }

    private JPanel createTagPanel(String tag) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                
                g2.setColor(Color.GRAY);
                g2.setStroke(new BasicStroke(1)); // 边框粗细 2px
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                
                g2.dispose();
                super.paintComponent(g);
            }
        };
        // 设置面板为透明（不绘制默认背景）
        panel.setOpaque(false);
        panel.setBackground(tagBackground);
        panel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

        JLabel label = new JLabel(tag);
        label.setForeground(Color.WHITE);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 12));

        JLabel closeLabel = new JLabel("×");
        closeLabel.setForeground(Color.WHITE);
        closeLabel.setFont(closeLabel.getFont().deriveFont(Font.BOLD, 12));
        // 鼠标悬停时显示手型光标
        closeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        closeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                removeTagByValue(tag);
            }

        });

        panel.add(label);
        panel.add(closeLabel);

        return panel;
    }

    private void removeTagByValue(String tag) {
        int index = tags.indexOf(tag);
        if (index >= 0) {
            removeTag(index);
        }
    }

    private void removeTag(int index) {
        if (index < 0 || index >= tags.size()) return;
        
        tags.remove(index);
        tagsPanel.remove(index);
        
        // 触发布局重新计算和重绘，包括父容器
        tagsPanel.invalidate();
        tagsPanel.revalidate();
        tagsPanel.repaint();
        invalidate();
        revalidate();
        repaint();
        
        // 通知父容器更新布局
        Container parent = getParent();
        while (parent != null) {
            parent.revalidate();
            parent = parent.getParent();
        }
    }

    public void setTags(List<String> newTags) {
        clearTags();
        for (String tag : newTags) {
            addTag(tag);
        }
    }

    public void clearTags() {
        tags.clear();
        tagsPanel.removeAll();
        
        // 触发布局重新计算和重绘，包括父容器
        tagsPanel.invalidate();
        tagsPanel.revalidate();
        tagsPanel.repaint();
        invalidate();
        revalidate();
        repaint();
        
        // 通知父容器更新布局
        Container parent = getParent();
        while (parent != null) {
            parent.revalidate();
            parent = parent.getParent();
        }
    }

    public List<String> getTags() {
        return new ArrayList<>(tags);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        tagsPanel.setEnabled(enabled);
    }

    @Override
    public Dimension getPreferredSize() {
        // 让整个组件根据内容自动调整大小
        return tagsPanel.getPreferredSize();
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(420, 30);
    }

    @Override
    public Dimension getMaximumSize() {
        // 不限制最大高度，允许无限扩展
        return new Dimension(420, Integer.MAX_VALUE);
    }
}
