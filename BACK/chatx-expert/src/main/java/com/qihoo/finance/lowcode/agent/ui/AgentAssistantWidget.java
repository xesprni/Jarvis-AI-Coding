package com.qihoo.finance.lowcode.agent.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.qihoo.finance.lowcode.common.factory.ChatXToolWindowFactory;
import com.qihoo.finance.lowcode.common.ui.ToolBarPanel;
import com.qihoo.finance.lowcode.common.util.Icons;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * DraggableButtonPanel
 *
 * @author fengjinfu-jk
 * date 2024/3/26
 * @version 1.0.0
 * @apiNote DraggableButtonPanel
 */
@Slf4j
public class AgentAssistantWidget extends NonOpaquePanel {

    private final JButton draggableButton;
    private Point mousePt;
    private boolean drag;
    private static final int assistantSize = 22;
    private static final int initHeight = 115;
    private static final int minWidth = 60;
    private static final int minHeight = initHeight;
    private final Project project;

    @SneakyThrows
    public AgentAssistantWidget(Project project) {
        this.project = project;

        setLayout(null); // 使用绝对布局
        draggableButton = new JButton();
        draggableButton.setIcon(Icons.scaleToWidth(Icons.AGENT_TASK2, assistantSize));
        draggableButton.setBorderPainted(false);
        draggableButton.setContentAreaFilled(false);
        draggableButton.setToolTipText("点击查看我的任务列表");

        // init bounds
        Dimension size = ChatXToolWindowFactory.getToolWindow().getComponent().getSize();
        if (size.width != 0 && size.height != 0) {
            draggableButton.setBounds(Math.max(size.width - minWidth, 5), Math.max(size.height - initHeight, 5), assistantSize, assistantSize);
        }
        // componentResized
        ChatXToolWindowFactory.getToolWindow().getComponent().addComponentListener(
                new ComponentAdapter() {
                    Dimension oldSize;

                    @Override
                    public void componentResized(ComponentEvent e) {
                        // log.info("AgentTaskAssistant toolWindow componentResized");
                        Dimension newSize = e.getComponent().getSize();
                        int width = newSize.width;
                        int height = newSize.height;

                        Point location = draggableButton.getLocation();
                        if (location.x == 0 && location.y == 0) {
                            draggableButton.setBounds(Math.max(width - minWidth, 5), Math.max(height - initHeight, 5),
                                    assistantSize, assistantSize);
                        } else {
                            int newX = location.x;
                            int newY = location.y;
                            if (location.x > width - minWidth) {
                                newX = width - minWidth;
                            }
                            if (location.y > height - minHeight) {
                                newY = height - minHeight;
                            }
                            if (oldSize != null && newX == location.x && newY == location.y) {
                                newX = width - (oldSize.width - location.x);
                                newY = height - (oldSize.height - location.y);
                            }

                            draggableButton.setLocation(Math.max(newX, 5), Math.max(newY, 5));
                        }

                        oldSize = newSize;
                        super.componentResized(e);
                    }
                }
        );

        draggableButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mousePt = e.getPoint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    // show popup
                    JBPopupMenu popup = ToolBarPanel.getInstance().simpleMoreMenu();
                    Dimension popupSize = popup.getPreferredSize();
                    popup.show(draggableButton, assistantSize - popupSize.width, assistantSize / 4 - popupSize.height);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                Object source = e.getSource();
                if (source instanceof JButton button) {
                    button.setContentAreaFilled(true);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                Object source = e.getSource();
                if (source instanceof JButton button) {
                    button.setContentAreaFilled(false);
                }
            }
        });

        draggableButton.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                super.mouseDragged(e);
                Point location = draggableButton.getLocation();
                int x = Math.max(5, location.x + e.getX() - mousePt.x);
                int y = Math.max(5, location.y + e.getY() - mousePt.y);

                Dimension maxSize = ChatXToolWindowFactory.getToolWindow().getComponent().getSize();
                draggableButton.setLocation(Math.min(x, maxSize.width - minWidth), Math.min(y, maxSize.height - minHeight));
                drag = true;
            }
        });

        draggableButton.addActionListener(e -> {
            if (!drag) {
                AgentTaskPopup.show();
            }
            drag = false;
        });
        // 消息通知
        // AgentNotifications.registerAgentNotify(draggableButton, Balloon.Position.above);
        add(draggableButton);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // 绘制按钮
//        draggableButton.setBounds(50, 50, 100, 30);
    }
}
