package org.qifu.devops.ide.plugins.jiracommit.ui;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * @author linshiyuan-jk
 */
public class CommitTypeInputKeyListen implements KeyListener {

    private JComboBox commitTypeComboBox;

    private JTextField filterTextFieldForCommitTypeComboBox;

    private Object[] items;

    public CommitTypeInputKeyListen(JComboBox commitTypeComboBox,JTextField filterTextFieldForCommitTypeComboBox,Object[] items){
        this.commitTypeComboBox = commitTypeComboBox;
        this.filterTextFieldForCommitTypeComboBox = filterTextFieldForCommitTypeComboBox;
        this.items = items;
    }

    @Override
    public void keyTyped(KeyEvent e) {
        commitTypeComboBox.showPopup();
        commitTypeComboBox.setPopupVisible(true);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        commitTypeComboBox.showPopup();
        commitTypeComboBox.setPopupVisible(true);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        Object obj = e.getSource();
        if (obj == filterTextFieldForCommitTypeComboBox) {
            String key = filterTextFieldForCommitTypeComboBox.getText();
            commitTypeComboBox.removeAllItems();
            for (Object item : items) {
                if (((String)item).contains(key.toUpperCase())) { //这里是包含key的项目都筛选出来，可以把startsWith改成contains就是筛选以key开头的项目
                    commitTypeComboBox.addItem(item);
                }
            }
            filterTextFieldForCommitTypeComboBox.setText(key);
        }
    }
}
