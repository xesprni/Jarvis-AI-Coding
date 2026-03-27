package com.qihoo.finance.lowcode.common.ui.date.ui;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

class ModernCalendarPanel extends JPanel implements PropertyChangeListener {
    public static final String PROPERTY_NAME_DATE = "date";
    public static final String PROPERTY_NAME_LOCALE = "locale";
    public static final String PROPERTY_NAME_ZONE = "zone";
    private Date date;
    private Locale locale;
    private TimeZone zone;
    private DateTimeSpinnerModel dateTimeSpinnerModel;
    private NoGroupingSpinner yearSpinner;
    private MonthComboBoxModel monthComboBoxModel;
    private MonthComboBoxRenderer monthComboBoxRenderer;
    private JComboBox monthCombo;
    private Set focusableComponents = new HashSet();

    public ModernCalendarPanel(Date aDate, Locale aLocale, TimeZone zone) {
        this.date = aDate;
        this.locale = aLocale;
        this.zone = zone;
        this.monthComboBoxModel = new MonthComboBoxModel(aDate, aLocale, zone);
        this.monthComboBoxRenderer = new MonthComboBoxRenderer(aLocale, zone);
        this.monthCombo = new JComboBox(this.monthComboBoxModel);
        this.monthCombo.setRenderer(this.monthComboBoxRenderer);
        this.dateTimeSpinnerModel = new DateTimeSpinnerModel(aDate, aLocale, zone, DateTimeSpinnerModel.YEAR);
        this.yearSpinner = new NoGroupingSpinner(this.dateTimeSpinnerModel);
        this.setLayout(new GridBagLayout());
        this.add(this.monthCombo, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, 13, 0, new Insets(0, 0, 3, 0), 0, 0));
        this.add(this.yearSpinner, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, 13, 3, new Insets(0, 3, 3, 0), 0, 0));
        this.focusableComponents.add(this.yearSpinner);
        this.focusableComponents.add(this.monthCombo);
        this.monthComboBoxModel.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("date")) {
                    Date newDate = (Date) evt.getNewValue();
                    ModernCalendarPanel.this.setDate(newDate);
                }

            }
        });
        this.dateTimeSpinnerModel.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("date")) {
                    Date newDate = (Date) evt.getNewValue();
                    ModernCalendarPanel.this.setDate(newDate);
                }

            }
        });
        this.addPropertyChangeListener(this);
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.monthCombo.setEnabled(enabled);
        this.yearSpinner.setEnabled(enabled);
    }

    public void setFocusable(boolean focusable) {
        super.setFocusable(focusable);
        this.monthCombo.setFocusable(focusable);
        this.yearSpinner.setFocusable(focusable);
    }

    public Date getDate() {
        return this.date;
    }

    public void setDate(Date date) {
        Date oldDate = this.date;
        this.date = date;
        this.firePropertyChange("date", oldDate, date);
        this.monthComboBoxModel.setDate(date);
        this.dateTimeSpinnerModel.setDate(date);
    }

    public Locale getLocale() {
        return this.locale;
    }

    public void setLocale(Locale locale) {
        Locale old = this.locale;
        this.locale = locale;
        this.monthComboBoxRenderer.setLocale(locale);
        this.monthComboBoxModel.setLocale(locale);
        this.dateTimeSpinnerModel.setLocale(locale);
        this.firePropertyChange("locale", old, locale);
    }

    public Collection getFocusableComponents() {
        return this.focusableComponents;
    }

    public TimeZone getZone() {
        return this.zone;
    }

    public void setZone(TimeZone zone) {
        TimeZone old = this.zone;
        this.zone = zone;
        this.monthComboBoxRenderer.setZone(zone);
        this.monthComboBoxModel.setZone(zone);
        this.dateTimeSpinnerModel.setZone(zone);
        this.firePropertyChange("zone", old, zone);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        boolean value;
        if (evt.getPropertyName().equals("focusable")) {
            value = (Boolean) evt.getNewValue();
            this.yearSpinner.setFocusable(value);
            Component[] children = this.yearSpinner.getEditor().getComponents();

            for (int i = 0; i < children.length; ++i) {
                children[i].setFocusable(value);
            }

            this.monthCombo.setFocusable(value);
        }

        if (evt.getPropertyName().equals("enabled")) {
            value = (Boolean) evt.getNewValue();
            this.yearSpinner.setEnabled(value);
            this.monthCombo.setEnabled(value);
        }

    }
}

