package com.qihoo.finance.lowcode.common.ui.date.ui;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

class MonthComboBoxRenderer extends DefaultListCellRenderer {
    private TimeZone zone;
    private SimpleDateFormat dateFormat;

    public MonthComboBoxRenderer(Locale locale, TimeZone zone) {
        this.zone = zone;
        this.dateFormat = new SimpleDateFormat("MMMM", locale);
        this.dateFormat.setTimeZone(zone);
    }

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        Date date = (Date) value;
        this.setText(this.dateFormat.format(date));
        return this;
    }

    public void setLocale(Locale locale) {
        this.dateFormat = new SimpleDateFormat("MMMM", locale);
        this.dateFormat.setTimeZone(this.zone);
    }

    public void setZone(TimeZone zone) {
        this.zone = zone;
        this.dateFormat.setTimeZone(zone);
    }
}

