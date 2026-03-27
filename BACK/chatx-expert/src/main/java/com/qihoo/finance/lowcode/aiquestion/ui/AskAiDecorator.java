package com.qihoo.finance.lowcode.aiquestion.ui;

import com.intellij.CommonBundle;
import com.intellij.ide.RemoteDesktopService;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.Strings;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLayeredPane;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.Alarm;
import com.intellij.util.ui.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * AskAiDecorator
 *
 * @author fengjinfu-jk
 * date 2024/4/8
 * @version 1.0.0
 * @apiNote AskAiDecorator
 */
public class AskAiDecorator {
    public static final Color OVERLAY_BACKGROUND = JBColor.namedColor("BigSpinner.background", JBColor.PanelBackground);

    private Color myOverlayBackground = null;

    JLayeredPane myPane;

    AskAiDecorator.ButtonLayer myLoadingLayer;
    Animator myFadeOutAnimator;

    int myDelay;
    Alarm myStartAlarm;
    boolean myStartRequest;


    public AskAiDecorator(JComponent content, @NotNull Disposable parent, int startDelayMs) {
        this(content, parent, startDelayMs, false);
    }

    public AskAiDecorator(JComponent content, @NotNull Disposable parent, int startDelayMs, boolean useMinimumSize) {
        this(content, parent, startDelayMs, useMinimumSize, new AsyncProcessIcon.Big("Loading"));
    }

    public AskAiDecorator(JComponent content, @NotNull Disposable parent, int startDelayMs, boolean useMinimumSize, @NotNull AsyncProcessIcon icon) {
        myPane = new AskAiDecorator.MyLayeredPane(useMinimumSize ? content : null);
        myLoadingLayer = new AskAiDecorator.ButtonLayer(icon);
        myDelay = startDelayMs;
        myStartAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, parent);

        setLoadingText(CommonBundle.getLoadingTreeNodeText());


        myFadeOutAnimator = new Animator("Loading", 10, RemoteDesktopService.isRemoteSession() ? 2500 : 500, false) {
            @Override
            public void paintNow(final int frame, final int totalFrames, final int cycle) {
                myLoadingLayer.setAlpha(1f - ((float) frame) / ((float) totalFrames));
            }

            @Override
            protected void paintCycleEnd() {
                myLoadingLayer.setAlpha(0); // paint with zero alpha before hiding completely
                hideLoadingLayer();
                myLoadingLayer.setAlpha(-1);
                myPane.repaint();
            }
        };
        if (myFadeOutAnimator instanceof Disposable) {
            Disposer.register(parent, myFadeOutAnimator);
        }

        myPane.add(content, JLayeredPane.DEFAULT_LAYER, 0);
        if (myLoadingLayer.myProgress instanceof Disposable) {
            Disposer.register(parent, myLoadingLayer.myProgress);
        }
    }

    public void showLoading() {
        myLoadingLayer.myTextComponent.setLoading(true);
        show(false);
    }

    public void showMainPage() {
        myLoadingLayer.myTextComponent.setLoading(false);
        show(false);
    }

    public Color getOverlayBackground() {
        return myOverlayBackground;
    }

    public void setOverlayBackground(@Nullable Color background) {
        myOverlayBackground = background;
    }

    /**
     * Removes a loading layer to restore a blit-accelerated scrolling.
     */
    private void hideLoadingLayer() {
        myPane.remove(myLoadingLayer);
        myLoadingLayer.setVisible(false);
    }

    private void addLoadingLayerOnDemand() {
        if (myPane != myLoadingLayer.getParent()) {
            myPane.add(myLoadingLayer, JLayeredPane.DRAG_LAYER, 1);
        }
    }

    protected AskAiMainPanel customizeLoadingLayer(JPanel parent, JLabel text, AsyncProcessIcon icon) {
        text.setFont(StartupUiUtil.getLabelFont());
        text.setForeground(UIUtil.getContextHelpForeground());
        icon.setBorder(Strings.notNullize(text.getText()).endsWith("...")
                ? JBUI.Borders.emptyRight(8)
                : JBUI.Borders.empty());

        NonOpaquePanel loading = new NonOpaquePanel(new VerticalLayout(6));
        loading.add(icon);
        loading.add(text);

        parent.setLayout(new BorderLayout());
        AskAiMainPanel mainPanel = new AskAiMainPanel(loading);
        parent.add(mainPanel, BorderLayout.CENTER);
        return mainPanel;
    }

    public JComponent getComponent() {
        return myPane;
    }

    private void show(final boolean takeSnapshot) {
        if (isLoading() || myStartRequest || myStartAlarm.isDisposed()) return;

        myStartRequest = true;
        if (myDelay > 0) {
            myStartAlarm.addRequest(() -> UIUtil.invokeLaterIfNeeded(() -> {
                if (!myStartRequest) return;
                _startLoading(takeSnapshot);
            }), myDelay);
        } else {
            _startLoading(takeSnapshot);
        }
    }

    protected void _startLoading(final boolean takeSnapshot) {
        addLoadingLayerOnDemand();
        myLoadingLayer.setVisible(true, takeSnapshot);
    }

    public void hidden() {
        myStartRequest = false;
        myStartAlarm.cancelAllRequests();

        if (!isLoading()) return;

        myLoadingLayer.setVisible(false, false);
        myPane.repaint();
    }


    public String getLoadingText() {
        return myLoadingLayer.myText.getText();
    }

    public void setLoadingText(@Nls String loadingText) {
        myLoadingLayer.myText.setVisible(!Strings.isEmptyOrSpaces(loadingText));
        myLoadingLayer.myText.setText(loadingText);
    }

    public boolean isLoading() {
        return myLoadingLayer.isLoading();
    }

    private final class ButtonLayer extends JPanel {
        private final JLabel myText = new JLabel("", SwingConstants.CENTER);

        private BufferedImage mySnapshot;
        private Color mySnapshotBg;

        private final AsyncProcessIcon myProgress;

        private boolean myVisible;

        private float myCurrentAlpha;
        private final AskAiMainPanel myTextComponent;

        private ButtonLayer(@NotNull AsyncProcessIcon processIcon) {
            setOpaque(false);
            setVisible(false);
            myProgress = processIcon;
            myProgress.setOpaque(false);
            myTextComponent = customizeLoadingLayer(this, myText, myProgress);
            myProgress.suspend();
        }

        public void setVisible(final boolean visible, boolean takeSnapshot) {
            if (myVisible == visible) return;

            if (myVisible && myCurrentAlpha != -1) return;

            myVisible = visible;
            myFadeOutAnimator.reset();
            if (myVisible) {
                setVisible(true);
                myCurrentAlpha = -1;

                if (takeSnapshot && getWidth() > 0 && getHeight() > 0) {
                    mySnapshot = ImageUtil.createImage(getGraphics(), getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
                    final Graphics2D g = mySnapshot.createGraphics();
                    myPane.paint(g);
                    final Component opaque = UIUtil.findNearestOpaque(this);
                    mySnapshotBg = opaque != null ? opaque.getBackground() : UIUtil.getPanelBackground();
                    g.dispose();
                }
                myProgress.resume();

                myFadeOutAnimator.suspend();
            } else {
                disposeSnapshot();
                myProgress.suspend();

                myFadeOutAnimator.resume();
            }
        }

        public boolean isLoading() {
            return myVisible;
        }

        private void disposeSnapshot() {
            if (mySnapshot != null) {
                mySnapshot.flush();
                mySnapshot = null;
            }
        }

        @Override
        protected void paintComponent(final Graphics g) {
            if (mySnapshot != null) {
                if (mySnapshot.getWidth() == getWidth() && mySnapshot.getHeight() == getHeight()) {
                    g.drawImage(mySnapshot, 0, 0, getWidth(), getHeight(), null);
                    g.setColor(new Color(200, 200, 200, 240));
                    g.fillRect(0, 0, getWidth(), getHeight());
                    return;
                } else {
                    disposeSnapshot();
                }
            }

            Color background = mySnapshotBg != null ? mySnapshotBg : getOverlayBackground();
            if (background != null) {
                g.setColor(background);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }

        public void setAlpha(final float alpha) {
            myCurrentAlpha = alpha;
            paintImmediately(myTextComponent.getBounds());
        }

        @Override
        protected void paintChildren(final Graphics g) {
            if (myCurrentAlpha != -1) {
                ((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, myCurrentAlpha));
            }
            super.paintChildren(g);
        }
    }

    public interface CursorAware {
    }

    private static final class MyLayeredPane extends JBLayeredPane implements com.intellij.openapi.ui.LoadingDecorator.CursorAware {
        private final JComponent myContent;

        private MyLayeredPane(JComponent content) {
            myContent = content;
        }

        @Override
        public Dimension getMinimumSize() {
            return myContent != null && !isMinimumSizeSet()
                    ? myContent.getMinimumSize()
                    : super.getMinimumSize();
        }

        @Override
        public Dimension getPreferredSize() {
            return myContent != null && !isPreferredSizeSet()
                    ? myContent.getPreferredSize()
                    : super.getPreferredSize();
        }

        @Override
        public void doLayout() {
            super.doLayout();
            for (int i = 0; i < getComponentCount(); i++) {
                final Component each = getComponent(i);
                if (each instanceof Icon) {
                    each.setBounds(0, 0, each.getWidth(), each.getHeight());
                } else {
                    each.setBounds(0, 0, getWidth(), getHeight());
                }
            }
        }
    }
}
