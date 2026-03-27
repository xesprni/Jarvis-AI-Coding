package com.qihoo.finance.lowcode.common.ui.date.ui;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

class MonthComboBoxModel extends AbstractListModel implements ComboBoxModel {
    public static final String PROPERTY_NAME_LOCALE = "locale";
    public static final String PROPERTY_NAME_DATE = "date";
    private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
    private Calendar calendar;
    private Locale locale;
    private TimeZone zone;

    public MonthComboBoxModel(Date date, Locale locale, TimeZone zone) {
        this.locale = locale;
        this.zone = zone;
        this.createLocaleAndZoneSensitive();
        this.calendar.setTime(date);
    }

    private void createLocaleAndZoneSensitive() {
        if (this.calendar != null) {
            Date old = this.calendar.getTime();
            this.calendar = Calendar.getInstance(this.zone, this.locale);
            this.calendar.setTime(old);
        } else {
            this.calendar = Calendar.getInstance(this.zone, this.locale);
        }

    }

    public Locale getLocale() {
        return this.locale;
    }

    public void setLocale(Locale locale) {
        Locale old = this.locale;
        this.locale = locale;
        this.createLocaleAndZoneSensitive();
        this.changeSupport.firePropertyChange("locale", old, locale);
        this.fireContentsChanged(this, 0, this.getSize() - 1);
    }

    public Date getDate() {
        return this.calendar.getTime();
    }

    public void setDate(Date date) {
        Date old = this.getDate();
        this.calendar.setTime(date);
        this.changeSupport.firePropertyChange("date", old, date);
        this.fireContentsChanged(this, 0, this.getSize() - 1);
    }

    public void setSelectedItem(Object anItem) {
        Date aDate = (Date) anItem;
        this.setDate(aDate);
    }

    public Object getSelectedItem() {
        return this.calendar.getTime();
    }

    public int getSize() {
        return this.calendar.getActualMaximum(2) + 1;
    }

    public Object getElementAt(int index) {
        Calendar c = Calendar.getInstance(this.locale);
        c.setTimeZone(this.zone);
        c.setTime(this.calendar.getTime());
        c.set(2, 0);

        for (int i = 0; i < index; ++i) {
            c.add(2, 1);
        }

        return c.getTime();
    }

    public TimeZone getZone() {
        return this.zone;
    }

    public void setZone(TimeZone zone) {
        this.zone = zone;
        this.createLocaleAndZoneSensitive();
        this.fireContentsChanged(this, 0, this.getSize() - 1);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.changeSupport.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        this.changeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        return this.changeSupport.getPropertyChangeListeners();
    }

    public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        return this.changeSupport.getPropertyChangeListeners(propertyName);
    }

    public boolean hasListeners(String propertyName) {
        return this.changeSupport.hasListeners(propertyName);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.changeSupport.removePropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        this.changeSupport.removePropertyChangeListener(propertyName, listener);
    }
}

