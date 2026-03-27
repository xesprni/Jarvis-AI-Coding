package com.qihoo.finance.lowcode.convertor.ui;

import com.intellij.openapi.ui.ComboBox;

import javax.swing.DefaultComboBoxModel;
import java.util.Vector;

public class JComboCheckBox extends ComboBox {

    private int maxWidth = 300;

    public JComboCheckBox() {
        super();
        setRenderer(new ComboCheckBoxRenderer());
        updateUI();
    }

    public JComboCheckBox(String[] items) {
        super();
        setRenderer(new ComboCheckBoxRenderer());
        addItems(items);
        updateUI();
    }

    public JComboCheckBox(Vector<String> items) {
        super();
        setRenderer(new ComboCheckBoxRenderer());
        addItems(items.toArray(new String[0]));
        updateUI();
    }

    public JComboCheckBox(int maxWidth) {
        super();
        this.maxWidth = maxWidth;
        setRenderer(new ComboCheckBoxRenderer());
        updateUI();
    }

    public JComboCheckBox(String[] items, int maxWidth) {
        super();
        this.maxWidth = maxWidth;
        setRenderer(new ComboCheckBoxRenderer());
        addItems(items);
        updateUI();
    }

    public JComboCheckBox(Vector<String> items, int maxWidth) {
        super();
        this.maxWidth = maxWidth;
        setRenderer(new ComboCheckBoxRenderer());
        addItems(items.toArray(new String[0]));
        updateUI();
    }

    public void addItems(String[] items) {
        for (int i = 0; i < items.length; i++) {
            String string = items[i];
            this.addItem(new ComboCheckBoxEntry(String.valueOf(i + 1), string));
        }
    }

    public void addItem(ComboCheckBoxEntry item) {
        super.addItem(item);
    }

    public void addItem(boolean checked, boolean state, String id, String value) {
        super.addItem(new ComboCheckBoxEntry(checked, state, id, value));
    }

    public String[] getCheckedCodes() {
        Vector values = new Vector();
        DefaultComboBoxModel model = (DefaultComboBoxModel) getModel();
        for (int i = 0; i < model.getSize(); i++) {
            ComboCheckBoxEntry item = (ComboCheckBoxEntry) model.getElementAt(i);
            boolean checked = item.getChecked();
            if (checked) {
                values.add(item.getUniqueCode());
            }
        }
        String[] retVal = new String[values.size()];
        values.copyInto(retVal);
        return retVal;
    }

    public String[] getCheckedValues() {
        Vector values = new Vector();
        DefaultComboBoxModel model = (DefaultComboBoxModel) getModel();
        for (int i = 0; i < model.getSize(); i++) {
            ComboCheckBoxEntry item = (ComboCheckBoxEntry) model.getElementAt(i);
            boolean checked = item.getChecked();
            if (checked) {
                values.add(item.getValue());
            }
        }
        String[] retVal = new String[values.size()];
        values.copyInto(retVal);
        return retVal;
    }

    @Override
    public void updateUI() {
        setUI(new ComboCheckBoxUI(maxWidth));
    }

}
