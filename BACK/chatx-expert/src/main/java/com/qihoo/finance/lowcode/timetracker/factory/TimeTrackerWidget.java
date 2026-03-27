package com.qihoo.finance.lowcode.timetracker.factory;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.wm.CustomStatusBarWidget;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.qihoo.finance.lowcode.timetracker.service.InactivityService;
import com.qihoo.finance.lowcode.timetracker.service.PostTimeTrackerService;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author weiyichao
 * @date 2023-07-21
 **/
public final class TimeTrackerWidget extends JButton implements CustomStatusBarWidget {

    public static final String ID = "com.qihoo.finance.lowcode.chatx-expert";

    @NotNull
    private final PostTimeTrackerService service;

    private boolean mouseInside = false;


    public TimeTrackerWidget(@NotNull PostTimeTrackerService service) {
        this.service = service;
        service.toggleRunning(PostTimeTrackerService.EVENT_FROM_READ);
        addActionListener(
                e -> {
                    final AWTEvent event = EventQueue.getCurrentEvent();
                    if (event instanceof MouseEvent) {
                        final MouseEvent mouseEvent = (MouseEvent) event;

                        final int width = getWidth();
                        final Insets insets = getInsets();
                        final int totalBarLength = width - insets.left - insets.right;
                        final int resumeStopWidth = resumeStopButtonWidth(totalBarLength);
                        final int actionSplit = insets.left + resumeStopWidth;

                        if (mouseEvent.getX() <= actionSplit) {
                            service.toggleRunning(PostTimeTrackerService.EVENT_FROM_READ);
                        }
                    }
                });
        setOpaque(true);
        setFocusable(false);

        addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        mouseInside = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        mouseInside = false;
                        repaint();
                    }
                });
    }

    private static int resumeStopButtonWidth(int widgetWidth) {
        return widgetWidth - Math.max(widgetWidth / 5, SETTINGS_ICON.getIconWidth() / 2 * 3);
    }

    @Override
    public Dimension getPreferredSize() {
        int width = 1;
        int height = 1;
        return new Dimension(width, height);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public @NonNls @NotNull String ID() {
        return ID;
    }

    @Override
    public void install(@NotNull StatusBar statusBar) {
        InactivityService.getInstance().assignProjectWindow(service, statusBar.getComponent());
    }

    @Override
    public void dispose() {

    }

    @Override
    public void paintComponent(final Graphics g) {
        // todo
    }

    private static final Icon SETTINGS_ICON = AllIcons.General.Settings;
    private static final Icon START_ICON = AllIcons.Actions.Resume;
    private static final Icon STOP_ICON = AllIcons.Actions.Pause;
    private static final Font WIDGET_FONT = JBUI.Fonts.label(11);

    private static final Color COLOR_OFF = new JBColor(new Color(189, 0, 16), new Color(128, 0, 0));
    private static final Color COLOR_ON = new JBColor(new Color(28, 152, 19), new Color(56, 113, 41));
    private static final Color COLOR_IDLE =
            new JBColor(new Color(200, 164, 23), new Color(163, 112, 17));

    private static final Color COLOR_MENU_OFF =
            new JBColor(new Color(198, 88, 97), new Color(97, 38, 38));
    private static final Color COLOR_MENU_ON =
            new JBColor(new Color(133, 194, 130), new Color(55, 80, 48));

    private static final int[] PREFERRED_SIZE_SECOND_QUERIES = {
            60, 60 * 60, 60 * 60 * 24, 60 * 60 * 24 * 7, 1999999999
    };
}
