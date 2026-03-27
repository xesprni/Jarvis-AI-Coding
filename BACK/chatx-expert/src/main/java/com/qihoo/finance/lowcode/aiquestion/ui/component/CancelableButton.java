package com.qihoo.finance.lowcode.aiquestion.ui.component;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * CancelableButton
 *
 * @author fengjinfu-jk
 * date 2024/4/17
 * @version 1.0.0
 * @apiNote CancelableButton
 */
@Getter
public class CancelableButton extends JButton {
    private boolean cancelable;
    private Icon defaultIcon;
    private Icon cancelIcon;
    private String toolTipTxt;
    private String cancelToolTipTxt;
    protected List<ActionListener> listenerList = new ArrayList<>();
    protected List<ActionListener> cancelListenerList = new ArrayList<>();

    public CancelableButton() {
    }

    public CancelableButton(Icon icon) {
        super(icon);
        this.defaultIcon = icon;
    }

    public CancelableButton(String text) {
        super(text);
    }

    public CancelableButton(Action a) {
        super(a);
    }

    public CancelableButton(String text, Icon icon) {
        super(text, icon);
        this.defaultIcon = icon;
    }

    public void setCancelButton(Icon cancelIcon) {
        this.cancelIcon = cancelIcon;
    }

    public boolean isFree() {
        return !cancelable;
    }

    @Override
    public void setIcon(Icon defaultIcon) {
        super.setIcon(defaultIcon);
        this.defaultIcon = defaultIcon;
    }

    public void setToolTipTxt(String text) {
        super.setToolTipText(text);
        this.toolTipTxt = text;
    }

    public void setCancelToolTipTxt(String text) {
        this.cancelToolTipTxt = text;
    }

    public void addCancelListener(ActionListener listener) {
        cancelListenerList.add(listener);
    }

    @Override
    public void addActionListener(ActionListener l) {
        listenerList.add(l);
    }

    @Override
    protected void fireActionPerformed(ActionEvent event) {
        actionPerformed(event);
    }

    private void actionPerformed(ActionEvent event) {
        try {
            setEnabled(false);
            if (!cancelable) {
                startAction();
                for (ActionListener actionListener : listenerList) {
                    actionListener.actionPerformed(event);
                }
            } else {
                stopAction();
                for (ActionListener actionListener : cancelListenerList) {
                    actionListener.actionPerformed(event);
                }
            }
        } finally {
            setEnabled(true);
        }
    }

    public void click() {
        doClick();
    }

    public void done() {
        stopAction();
    }

    public void startAction() {
        cancelable = true;
        if (StringUtils.isNotEmpty(cancelToolTipTxt)) super.setToolTipText(cancelToolTipTxt);
        if (Objects.nonNull(cancelIcon)) super.setIcon(cancelIcon);
    }

    public void stopAction() {
        cancelable = false;
        super.setToolTipText(toolTipTxt);
        if (Objects.nonNull(defaultIcon)) super.setIcon(defaultIcon);
    }
}
