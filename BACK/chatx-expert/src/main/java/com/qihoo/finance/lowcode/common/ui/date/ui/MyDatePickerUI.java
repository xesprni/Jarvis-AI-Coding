package com.qihoo.finance.lowcode.common.ui.date.ui;

import com.michaelbaranov.microba.calendar.CalendarResources;
import com.michaelbaranov.microba.calendar.HolidayPolicy;
import com.michaelbaranov.microba.calendar.VetoPolicy;
import com.michaelbaranov.microba.common.CommitEvent;
import com.michaelbaranov.microba.common.CommitListener;
import com.qihoo.finance.lowcode.common.ui.date.DatePicker;
import com.qihoo.finance.lowcode.common.util.Icons;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.DateFormatter;
import javax.swing.text.DefaultFormatterFactory;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * BasicDatePickerUI
 *
 * @author fengjinfu-jk
 * date 2024/1/6
 * @version 1.0.0
 * @apiNote BasicDatePickerUI
 */


public class MyDatePickerUI extends com.michaelbaranov.microba.calendar.ui.DatePickerUI implements PropertyChangeListener {
    protected static final String POPUP_KEY = "##MyDatePickerUI.popup##";

    protected DatePicker peer;
    protected CalendarPane calendarPane;
    protected JButton button;
    protected JPopupMenu popup;
    protected JFormattedTextField field;
    protected MyDatePickerUI.ComponentListener componentListener;

    public MyDatePickerUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return new MyDatePickerUI();
    }

    public void installUI(JComponent c) {
        this.peer = (DatePicker) c;
        this.installComponents();
        this.istallListeners();
        this.installKeyboardActions();
    }

    public void uninstallUI(JComponent c) {
        this.uninstallKeyboardActions();
        this.uninstallListeners();
        this.uninstallComponents();
        this.peer = null;
    }

    protected void installKeyboardActions() {
        InputMap input = this.peer.getInputMap(1);
        input.put(KeyStroke.getKeyStroke(67, 8), "##MyDatePickerUI.popup##");
        input.put(KeyStroke.getKeyStroke(34, 0), "##MyDatePickerUI.popup##");
        this.peer.getActionMap().put("##MyDatePickerUI.popup##", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                MyDatePickerUI.this.showPopup(true);
            }
        });
    }

    protected void uninstallKeyboardActions() {
        InputMap input = this.peer.getInputMap(1);
        input.remove(KeyStroke.getKeyStroke(67, 8));
        input.remove(KeyStroke.getKeyStroke(34, 0));
        this.peer.getActionMap().remove("##MyDatePickerUI.popup##");
    }

    protected void istallListeners() {
        this.peer.addPropertyChangeListener(this);
    }

    protected void uninstallListeners() {
        this.peer.removePropertyChangeListener(this);
    }

    protected void uninstallComponents() {
        this.button.removeActionListener(this.componentListener);
        this.field.removePropertyChangeListener(this.componentListener);
        this.calendarPane.removePropertyChangeListener(this.componentListener);
        this.calendarPane.removeCommitListener(this.componentListener);
        this.calendarPane.removeActionListener(this.componentListener);
        this.peer.remove(this.field);
        this.peer.remove(this.button);
        this.popup = null;
        this.calendarPane = null;
        this.button = null;
        this.field = null;
    }

    protected void installComponents() {
        this.field = new JFormattedTextField(this.createFormatterFactory());
        this.field.setValue(this.peer.getDate());
        this.field.setFocusLostBehavior(this.peer.getFocusLostBehavior());
        this.field.setEditable(this.peer.isFieldEditable());
        this.field.setToolTipText(this.peer.getToolTipText());
        this.button = new JButton();
        this.button.setFocusable(false);
        this.button.setMargin(new Insets(0, 0, 0, 0));
        this.button.setToolTipText(this.peer.getToolTipText());
        this.setSimpeLook(false);

        // 面板信息
        this.calendarPane = new CalendarPane(this.peer.getStyle());
        this.calendarPane.setShowTodayButton(this.peer.isShowTodayButton());
        this.calendarPane.setFocusLostBehavior(2);
        this.calendarPane.setFocusCycleRoot(true);
        this.calendarPane.setBorder(BorderFactory.createEmptyBorder(1, 3, 0, 3));
        this.calendarPane.setStripTime(false);
        this.calendarPane.setLocale(this.peer.getLocale());
        this.calendarPane.setZone(this.peer.getZone());
        this.calendarPane.setFocusable(this.peer.isDropdownFocusable());
        this.calendarPane.setColorOverrideMap(this.peer.getColorOverrideMap());

        this.popup = new JPopupMenu();
        this.popup.setLayout(new BorderLayout());
        this.popup.add(this.calendarPane, "Center");
        this.popup.setLightWeightPopupEnabled(true);
        this.peer.setLayout(new BorderLayout());
        switch (this.peer.getPickerStyle()) {
            case 272:
                this.peer.add(this.field, "Center");
                this.peer.add(this.button, "East");
                break;
            case 288:
                this.peer.add(this.button, "East");
        }

        this.peer.revalidate();
        this.peer.repaint();
        this.componentListener = new MyDatePickerUI.ComponentListener();
        this.button.addActionListener(this.componentListener);
        this.field.addPropertyChangeListener(this.componentListener);
        this.calendarPane.addPropertyChangeListener(this.componentListener);
        this.calendarPane.addCommitListener(this.componentListener);
        this.calendarPane.addActionListener(this.componentListener);
        this.peerDateChanged(this.peer.getDate());
    }

    public void setSimpeLook(boolean b) {
        if (b) {
            this.field.setBorder(BorderFactory.createEmptyBorder());
            this.button.setText("...");
            this.button.setIcon((Icon) null);
        } else {
            this.field.setBorder((new JTextField()).getBorder());
            this.button.setText("");
//            this.button.setIcon(new ImageIcon(Resource.class.getResource("picker-16.png")));
            this.button.setIcon(Icons.scaleToWidth(Icons.DATE, 15));
        }

    }

    public void showPopup(boolean visible) {
        if (visible) {
            if (this.peer.isKeepTime()) {
                try {
                    JFormattedTextField.AbstractFormatter formatter = this.field.getFormatter();
                    Date value = (Date) formatter.stringToValue(this.field.getText());
                    this.calendarPane.removePropertyChangeListener(this.componentListener);
                    this.calendarPane.setDate(value);
                    this.calendarPane.addPropertyChangeListener(this.componentListener);
                } catch (ParseException var4) {
                } catch (PropertyVetoException var5) {
                }
            }

            this.popup.show(this.peer, 0, this.peer.getHeight());
            this.calendarPane.requestFocus(false);
        } else {
            this.popup.setVisible(false);
        }

    }

    public void propertyChange(PropertyChangeEvent evt) {
        if ("ToolTipText".equals(evt.getPropertyName())) {
            this.field.setToolTipText((String) evt.getNewValue());
            this.button.setToolTipText((String) evt.getNewValue());
        } else if (evt.getPropertyName().equals("date")) {
            Date newValue = (Date) evt.getNewValue();
            this.peerDateChanged(newValue);
        } else if (evt.getPropertyName().equals("fieldEditable")) {
            this.field.setEditable(this.peer.isFieldEditable());
        } else if (evt.getPropertyName().equals("focusLostBehavior")) {
            this.field.setFocusLostBehavior(this.peer.getFocusLostBehavior());
        } else if (evt.getPropertyName().equals("locale")) {
            this.field.setFormatterFactory(this.createFormatterFactory());
            this.calendarPane.setLocale(this.peer.getLocale());
        } else if (evt.getPropertyName().equals("dateFormat")) {
            this.field.setFormatterFactory(this.createFormatterFactory());
        } else if (evt.getPropertyName().equals("zone")) {
            this.field.setFormatterFactory(this.createFormatterFactory());
            this.calendarPane.setZone((TimeZone) evt.getNewValue());
        } else {
            boolean value;
            if (evt.getPropertyName().equals("showTodayButton")) {
                value = (Boolean) evt.getNewValue();
                this.calendarPane.setShowTodayButton(value);
            } else if (evt.getPropertyName().equals("showNoneButton")) {
                value = (Boolean) evt.getNewValue();
                this.calendarPane.setShowNoneButton(value);
            } else if (evt.getPropertyName().equals("showNumberOfWeek")) {
                value = (Boolean) evt.getNewValue();
                this.calendarPane.setShowNumberOfWeek(value);
            } else if (evt.getPropertyName().equals("style")) {
                int value2 = (Integer) evt.getNewValue();
                this.calendarPane.setStyle(value2);
            } else if (evt.getPropertyName().equals("vetoPlicy")) {
                this.calendarPane.setVetoPolicy((VetoPolicy) evt.getNewValue());
            } else if (evt.getPropertyName().equals("holidayPolicy")) {
                this.calendarPane.setHolidayPolicy((HolidayPolicy) evt.getNewValue());
            } else if (evt.getPropertyName().equals("focusable")) {
                value = (Boolean) evt.getNewValue();
                this.field.setFocusable(value);
            } else if (evt.getPropertyName().equals("resources")) {
                CalendarResources resources = (CalendarResources) evt.getNewValue();
                this.calendarPane.setResources(resources);
            } else if (evt.getPropertyName().equals("enabled")) {
                value = (Boolean) evt.getNewValue();
                this.field.setEnabled(value);
                this.button.setEnabled(value);
            } else if (evt.getPropertyName().equals("pickerStyle")) {
                this.peer.updateUI();
            } else if (evt.getPropertyName().equals("dropdownFocusable")) {
                this.calendarPane.setFocusable(this.peer.isDropdownFocusable());
            }
        }

    }

    private void peerDateChanged(Date newValue) {
        try {
            this.calendarPane.removePropertyChangeListener(this.componentListener);
            this.calendarPane.setDate(newValue);
            this.calendarPane.addPropertyChangeListener(this.componentListener);
        } catch (PropertyVetoException var3) {
        }

        this.field.removePropertyChangeListener(this.componentListener);
        this.field.setValue(newValue);
        this.field.addPropertyChangeListener(this.componentListener);
    }

    private DefaultFormatterFactory createFormatterFactory() {
        return new DefaultFormatterFactory(new DateFormatter(this.peer.getDateFormat()));
    }

    public void commit() throws PropertyVetoException, ParseException {
        this.field.commitEdit();
    }

    public void revert() {
        this.peerDateChanged(this.peer.getDate());
    }

    protected class ComponentListener implements ActionListener, PropertyChangeListener, CommitListener {
        protected ComponentListener() {
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() != MyDatePickerUI.this.calendarPane) {
                MyDatePickerUI.this.showPopup(true);
            } else {
                MyDatePickerUI.this.showPopup(false);
            }

        }

        public void propertyChange(PropertyChangeEvent evt) {
            Date fieldValue;
            if (evt.getSource() == MyDatePickerUI.this.calendarPane && "date".equals(evt.getPropertyName())) {
                MyDatePickerUI.this.showPopup(false);
                fieldValue = null;

                try {
                    JFormattedTextField.AbstractFormatter formatter = MyDatePickerUI.this.field.getFormatter();
                    fieldValue = (Date) formatter.stringToValue(MyDatePickerUI.this.field.getText());
                } catch (ParseException var6) {
                    fieldValue = (Date) MyDatePickerUI.this.field.getValue();
                }

                if (fieldValue != null || evt.getNewValue() != null) {
                    if (MyDatePickerUI.this.peer.isKeepTime() && fieldValue != null && evt.getNewValue() != null) {
                        Calendar fieldCal = Calendar.getInstance(MyDatePickerUI.this.peer.getZone(), MyDatePickerUI.this.peer.getLocale());
                        fieldCal.setTime(fieldValue);
                        Calendar valueCal = Calendar.getInstance(MyDatePickerUI.this.peer.getZone(), MyDatePickerUI.this.peer.getLocale());
                        valueCal.setTime((Date) evt.getNewValue());
                        fieldCal.set(0, valueCal.get(0));
                        fieldCal.set(1, valueCal.get(1));
                        fieldCal.set(2, valueCal.get(2));
                        fieldCal.set(5, valueCal.get(5));
                        MyDatePickerUI.this.field.setValue(fieldCal.getTime());
                    } else {
                        MyDatePickerUI.this.field.setValue((Date) evt.getNewValue());
                    }
                }
            }

            if (evt.getSource() == MyDatePickerUI.this.field && "value".equals(evt.getPropertyName())) {
                fieldValue = (Date) MyDatePickerUI.this.field.getValue();

                try {
                    MyDatePickerUI.this.peer.setDate(fieldValue);
                } catch (PropertyVetoException var5) {
                    MyDatePickerUI.this.field.setValue(MyDatePickerUI.this.peer.getDate());
                }
            }

        }

        public void commit(CommitEvent action) {
            MyDatePickerUI.this.showPopup(false);
            if (MyDatePickerUI.this.field.getValue() != null || MyDatePickerUI.this.calendarPane.getDate() != null) {
                MyDatePickerUI.this.field.setValue(MyDatePickerUI.this.calendarPane.getDate());
            }

        }

        public void revert(CommitEvent action) {
            MyDatePickerUI.this.showPopup(false);
        }
    }
}
