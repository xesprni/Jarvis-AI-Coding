package com.qihoo.finance.lowcode.common.ui.date.ui;


import com.michaelbaranov.microba.calendar.HolidayPolicy;
import com.michaelbaranov.microba.calendar.VetoPolicy;
import com.michaelbaranov.microba.calendar.ui.basic.CalendarNumberOfWeekPanel;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.util.*;

public class MyCalendarPaneUI extends com.michaelbaranov.microba.calendar.ui.CalendarPaneUI implements PropertyChangeListener, FocusListener {

    protected static final String ESCAPE_KEY = "##MyDatePickerUI.escape##";
    protected static final String ENTER_KEY = "##MyDatePickerUI.enter##";
    protected CalendarPane peer;
    protected ClassicCalendarPanel classicPanel;
    protected ModernCalendarPanel modernPanel;
    protected AuxPanel auxPanel;
    protected CalendarGridPanel gridPanel;
    protected CalendarNumberOfWeekPanel numberOfWeekPanel;
    protected CalendarHeader headerPanel;
    protected Set focusableComponents = new HashSet();
    protected MyCalendarPaneUI.ComponentListener componentListener;

    public MyCalendarPaneUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return new MyCalendarPaneUI();
    }

    public void installUI(JComponent component) {
        this.peer = (CalendarPane) component;
        this.createNestedComponents();
        this.addNestedComponents();
        this.installListeners();
        this.installKeyboardActions();
    }

    public void uninstallUI(JComponent component) {
        this.uninstallKeyboardActions();
        this.uninstallListeners();
        this.removeNestedComponents();
        this.destroyNestedComponents();
        this.peer = null;
    }

    protected void uninstallKeyboardActions() {
        InputMap input = this.peer.getInputMap(1);
        ActionMap action = this.peer.getActionMap();
        input.remove(KeyStroke.getKeyStroke(10, 0));
        input.remove(KeyStroke.getKeyStroke(27, 0));
        action.remove("##MyDatePickerUI.enter##");
        action.remove("##MyDatePickerUI.escape##");
    }

    protected void installKeyboardActions() {
        InputMap input = this.peer.getInputMap(1);
        ActionMap action = this.peer.getActionMap();
        input.put(KeyStroke.getKeyStroke(10, 0), "##MyDatePickerUI.enter##");
        input.put(KeyStroke.getKeyStroke(27, 0), "##MyDatePickerUI.escape##");
        input.put(KeyStroke.getKeyStroke(33, 0), "pgupkey");
        input.put(KeyStroke.getKeyStroke(34, 0), "pgdownkey");
        action.put("##MyDatePickerUI.enter##", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                MyCalendarPaneUI.this.peer.commitEdit();
            }
        });
        action.put("##MyDatePickerUI.escape##", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                MyCalendarPaneUI.this.peer.revertEdit();
            }
        });
        action.put("pgupkey", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                MyCalendarPaneUI.this.classicPanel.addMonth(1);
            }
        });
        action.put("pgdownkey", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                MyCalendarPaneUI.this.classicPanel.addMonth(-1);
            }
        });
    }

    protected void uninstallListeners() {
        this.peer.removePropertyChangeListener(this);
        this.peer.removeFocusListener(this);
    }

    protected void installListeners() {
        this.peer.addPropertyChangeListener(this);
        this.peer.addFocusListener(this);
    }

    protected void createNestedComponents() {
        Date baseDate = this.peer.getDate() == null ? new Date() : this.peer.getDate();
        this.classicPanel = new ClassicCalendarPanel(baseDate, this.peer.getLocale(), this.peer.getZone());
        this.modernPanel = new ModernCalendarPanel(baseDate, this.peer.getLocale(), this.peer.getZone());
        this.headerPanel = new CalendarHeader(this.peer, baseDate, this.peer.getLocale(), this.peer.getZone(), this.peer.getHolidayPolicy());
        this.auxPanel = new AuxPanel(this.peer.getLocale(), this.peer.getZone(), this.peer.getVetoPolicy(), this.peer.isShowTodayButton(), this.peer.isShowNoneButton(), this.peer.getResources());
        this.gridPanel = new CalendarGridPanel(this.peer, this.peer.getDate(), this.peer.getLocale(), this.peer.getZone(), this.peer.getVetoPolicy(), this.peer.getHolidayPolicy());
        this.numberOfWeekPanel = new CalendarNumberOfWeekPanel(this.peer.getDate(), this.peer.getLocale(), this.peer.getZone());
        this.focusableComponents.addAll(this.classicPanel.getFocusableComponents());
        this.focusableComponents.addAll(this.modernPanel.getFocusableComponents());
        this.focusableComponents.addAll(this.auxPanel.getFocusableComponents());
        this.focusableComponents.addAll(this.gridPanel.getFocusableComponents());
        this.focusableComponents.addAll(this.auxPanel.getFocusableComponents());
        this.componentListener = new MyCalendarPaneUI.ComponentListener();

        for (int i = 0; i < this.focusableComponents.size(); ++i) {
            ((JComponent) this.focusableComponents.toArray()[i]).addFocusListener(this.componentListener);
        }

        this.gridPanel.addPropertyChangeListener(this.componentListener);
        this.modernPanel.addPropertyChangeListener(this.componentListener);
        this.classicPanel.addPropertyChangeListener(this.componentListener);
        this.auxPanel.addPropertyChangeListener(this.componentListener);
        this.classicPanel.setEnabled(this.peer.isEnabled());
        this.modernPanel.setEnabled(this.peer.isEnabled());
        this.headerPanel.setEnabled(this.peer.isEnabled());
        this.auxPanel.setEnabled(this.peer.isEnabled());
        this.numberOfWeekPanel.setEnabled(this.peer.isEnabled());
        this.gridPanel.setEnabled(this.peer.isEnabled());
    }

    protected void destroyNestedComponents() {
        this.gridPanel.removePropertyChangeListener(this.componentListener);
        this.modernPanel.removePropertyChangeListener(this.componentListener);
        this.classicPanel.removePropertyChangeListener(this.componentListener);
        this.auxPanel.removePropertyChangeListener(this.componentListener);
        this.componentListener = null;

        for (int i = 0; i < this.focusableComponents.size(); ++i) {
            ((JComponent) this.focusableComponents.toArray()[i]).removeFocusListener(this.componentListener);
        }

        this.focusableComponents.clear();
        this.classicPanel = null;
        this.modernPanel = null;
        this.headerPanel = null;
        this.auxPanel = null;
        this.gridPanel = null;
        this.numberOfWeekPanel = null;
    }

    protected void addNestedComponents() {
        this.peer.removeAll();
        this.peer.setLayout(new GridBagLayout());
//        if ((this.peer.getStyle() & 32) > 0) {
//            this.peer.add(this.classicPanel, new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0, 10, 2, new Insets(0, 0, 0, 0), 0, 0));
//        } else {
            this.peer.add(this.modernPanel, new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0, 10, 2, new Insets(0, 0, 0, 0), 0, 0));
//        }

        this.peer.add(this.headerPanel, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, 10, 2, new Insets(0, 0, 0, 0), 0, 0));
        if (this.peer.isShowNumberOfWeek()) {
            this.peer.add(this.numberOfWeekPanel, new GridBagConstraints(0, 2, 1, 1, 0.0, 1.0, 10, 3, new Insets(0, 0, 0, 0), 0, 0));
        }

        this.peer.add(this.gridPanel, new GridBagConstraints(1, 2, 1, 1, 1.0, 1.0, 10, 1, new Insets(0, 0, 0, 0), 0, 0));
        // todo: 添加时分秒组件

        if (this.peer.isShowTodayButton()) {
            this.peer.add(this.auxPanel, new GridBagConstraints(0, 3, 2, 1, 1.0, 0.0, 10, 2, new Insets(0, 0, 0, 0), 0, 0));
        }

        if (this.peer.isShowNumberOfWeek()) {
        }

        this.peer.revalidate();
        this.peer.repaint();
    }

    protected void removeNestedComponents() {
        this.peer.removeAll();
    }

    protected void widgetDateChanged(Date date) {
        Date baseDate = date == null ? new Date() : date;
        this.headerPanel.setDate(baseDate);
        this.classicPanel.setDate(baseDate);
        this.modernPanel.setDate(baseDate);
        this.gridPanel.setBaseDate(baseDate);
        this.gridPanel.setDate(date);
        this.numberOfWeekPanel.setBaseDate(baseDate);
    }

    protected void widgetLocaleChanged(Locale newValue) {
        this.classicPanel.setLocale(newValue);
        this.modernPanel.setLocale(newValue);
        this.gridPanel.setLocale(newValue);
        this.headerPanel.setLocale(newValue);
        this.auxPanel.setLocale(newValue);
        this.numberOfWeekPanel.setLocale(newValue);
    }

    protected void widgetZoneChanged(TimeZone zone) {
        this.classicPanel.setZone(zone);
        this.modernPanel.setZone(zone);
        this.gridPanel.setZone(zone);
        this.headerPanel.setZone(zone);
        this.auxPanel.setZone(zone);
        this.numberOfWeekPanel.setZone(zone);
    }

    protected void widgetResourceChanged() {
        this.auxPanel.setResources(this.peer.getResources());
    }

    public void commit() throws PropertyVetoException {
        this.peer.setDate(this.gridPanel.getDateToCommit());
    }

    public void revert() {
        this.widgetDateChanged(this.peer.getDate());
    }

    public void focusGained(FocusEvent e) {
        this.gridPanel.requestFocus(true);
    }

    public void focusLost(FocusEvent e) {
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("date")) {
            this.widgetDateChanged((Date) evt.getNewValue());
        } else if (evt.getPropertyName().equals("locale")) {
            this.widgetLocaleChanged((Locale) evt.getNewValue());
        } else if (evt.getPropertyName().equals("zone")) {
            this.widgetZoneChanged((TimeZone) evt.getNewValue());
        } else if (evt.getPropertyName().equals("vetoPlicy")) {
            this.gridPanel.setVetoPolicy((VetoPolicy) evt.getNewValue());
            this.auxPanel.setVetoModel((VetoPolicy) evt.getNewValue());
        } else if (evt.getPropertyName().equals("holidayPolicy")) {
            this.gridPanel.setHolidayPolicy((HolidayPolicy) evt.getNewValue());
            this.headerPanel.setHolidayPolicy((HolidayPolicy) evt.getNewValue());
        } else {
            boolean value;
            if (evt.getPropertyName().equals("enabled")) {
                value = (Boolean) evt.getNewValue();
                this.classicPanel.setEnabled(value);
                this.modernPanel.setEnabled(value);
                this.headerPanel.setEnabled(value);
                this.auxPanel.setEnabled(value);
                this.numberOfWeekPanel.setEnabled(value);
                this.gridPanel.setEnabled(value);
            } else if (evt.getPropertyName().equals("style")) {
                this.addNestedComponents();
            } else {
                if (evt.getPropertyName().equals("showTodayButton")) {
                    value = (Boolean) evt.getNewValue();
                    this.auxPanel.setShowTodayBtn(value);
                } else if (evt.getPropertyName().equals("showNoneButton")) {
                    value = (Boolean) evt.getNewValue();
                    this.auxPanel.setShowNoneButton(value);
                } else if (evt.getPropertyName().equals("showNumberOfWeek")) {
                    this.addNestedComponents();
                } else if (evt.getPropertyName().equals("focusable")) {
                    value = (Boolean) evt.getNewValue();
                    this.classicPanel.setFocusable(value);
                    this.modernPanel.setFocusable(value);
                    this.gridPanel.setFocusable(value);
                    this.auxPanel.setFocusable(value);
                } else if (evt.getPropertyName().equals("resources")) {
                    this.widgetResourceChanged();
                } else if (evt.getPropertyName().equals("enabled")) {
                    value = (Boolean) evt.getNewValue();
                    this.classicPanel.setEnabled(value);
                    this.modernPanel.setEnabled(value);
                    this.gridPanel.setEnabled(value);
                    this.auxPanel.setEnabled(value);
                }
            }
        }

    }

    protected class ComponentListener implements FocusListener, PropertyChangeListener {
        protected ComponentListener() {
        }

        public void focusGained(FocusEvent e) {
        }

        public void focusLost(FocusEvent e) {
            boolean isFocusableComponent = MyCalendarPaneUI.this.focusableComponents.contains(e.getSource());
            boolean isNonEmptyOpposite = e.getOppositeComponent() != null;
            if (isFocusableComponent && isNonEmptyOpposite && !SwingUtilities.isDescendingFrom(e.getOppositeComponent(), MyCalendarPaneUI.this.peer)) {
                MyCalendarPaneUI.this.peer.commitOrRevert();
            }

        }

        public void propertyChange(PropertyChangeEvent evt) {
            Date date;
            if (evt.getSource() == MyCalendarPaneUI.this.gridPanel && evt.getPropertyName().equals("date")) {
                date = (Date) evt.getNewValue();

                try {
                    MyCalendarPaneUI.this.peer.setDate(date);
                } catch (PropertyVetoException var4) {
                }
            }

            if (evt.getSource() == MyCalendarPaneUI.this.gridPanel && evt.getPropertyName().equals("##same date clicked##")) {
                MyCalendarPaneUI.this.peer.fireActionEvent();
            }

            if (evt.getSource() == MyCalendarPaneUI.this.gridPanel && evt.getPropertyName().equals("baseDate")) {
                date = (Date) evt.getNewValue();
                MyCalendarPaneUI.this.modernPanel.setDate(date);
                MyCalendarPaneUI.this.classicPanel.setDate(date);
            }

            if (evt.getSource() == MyCalendarPaneUI.this.modernPanel && evt.getPropertyName().equals("date")) {
                date = (Date) evt.getNewValue();
                MyCalendarPaneUI.this.gridPanel.setBaseDate(date);
                MyCalendarPaneUI.this.classicPanel.setDate(date);
                MyCalendarPaneUI.this.numberOfWeekPanel.setBaseDate(date);
            }

            if (evt.getSource() == MyCalendarPaneUI.this.classicPanel && evt.getPropertyName().equals("date")) {
                date = (Date) evt.getNewValue();
                MyCalendarPaneUI.this.gridPanel.setBaseDate(date);
                MyCalendarPaneUI.this.modernPanel.setDate(date);
                MyCalendarPaneUI.this.numberOfWeekPanel.setBaseDate(date);
            }

            if (evt.getSource() == MyCalendarPaneUI.this.auxPanel && evt.getPropertyName().equals("date")) {
                date = (Date) evt.getNewValue();
                MyCalendarPaneUI.this.gridPanel.setDate(date);
                MyCalendarPaneUI.this.peer.commitEdit();
            }

        }
    }
}

