package com.qihoo.finance.lowcode.common.ui.date.ui;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

class DateTimeSpinnerModel extends SpinnerNumberModel {
    public static final int YEAR = 1;
    public static final int HOUR = 11;
    public static final int MINUTE = 12;
    public static final int SECONDS = 13;
    public static final String PROPERTY_NAME_LOCALE = "locale";
    public static final String PROPERTY_NAME_DATE = "date";
    public static final String PROPERTY_NAME_ZONE = "zone";
    private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
    private Locale locale;
    private TimeZone zone;
    private Calendar calendar;
    private final int modelType;

    public DateTimeSpinnerModel(Date date, Locale locale, TimeZone zone, int modelType) {
        this.locale = locale;
        this.zone = zone;
        this.createLocaleAndZoneSensitive();
        this.calendar.setTime(date);
        this.modelType = modelType;
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

    public Object getValue() {
        // HH:mm:ss this.calendar.get(11) + " : " + this.calendar.get(12) + " : " + this.calendar.get(13)
        return this.calendar.get(modelType);
    }

    public void setValue(Object value) {
        Number newVal = (Number) value;
        Number oldVal = (Number) this.getValue();
        if (oldVal.longValue() != newVal.longValue()) {
            int diff = newVal.intValue() - oldVal.intValue();
            int sign = diff > 0 ? 1 : -1;
            if (diff < 0) {
                diff = -diff;
            }

            Date oldDate = this.calendar.getTime();

            for (int i = 0; i < diff; ++i) {
                this.calendar.add(modelType, sign);
            }

            this.changeSupport.firePropertyChange("date", oldDate, this.getDate());
            this.fireStateChanged();
        }

    }

    public Object getNextValue() {
        Integer currVal = (Integer) this.getValue();
        int newVal = currVal + 1;
        return newVal <= this.calendar.getActualMaximum(modelType) ? Integer.valueOf(newVal) : currVal;
    }

    public Object getPreviousValue() {
        Integer currVal = (Integer) this.getValue();
        int newVal = currVal - 1;
        return newVal >= this.calendar.getActualMinimum(modelType) ? Integer.valueOf(newVal) : currVal;
    }

    public Date getDate() {
        return this.calendar.getTime();
    }

    public void setDate(Date date) {
        Date old = this.calendar.getTime();
        if (!old.equals(date)) {
            this.calendar.setTime(date);
            this.changeSupport.firePropertyChange("date", old, date);
            this.fireStateChanged();
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
        this.fireStateChanged();
    }

    public TimeZone getZone() {
        return this.zone;
    }

    public void setZone(TimeZone zone) {
        TimeZone old = this.zone;
        this.zone = zone;
        this.createLocaleAndZoneSensitive();
        this.changeSupport.firePropertyChange("locale", old, this.locale);
        this.fireStateChanged();
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.changeSupport.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        this.changeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.changeSupport.removePropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        this.changeSupport.removePropertyChangeListener(propertyName, listener);
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
}

