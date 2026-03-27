package com.qihoo.finance.lowcode.common.ui.date.ui;

import com.michaelbaranov.microba.Microba;
import com.michaelbaranov.microba.calendar.HolidayPolicy;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

class CalendarHeader extends JPanel {
    private Locale locale;
    private TimeZone zone;
    private Date date;
    private HolidayPolicy holidayPolicy;
    private Color backgroundColorActive;
    private Color backgroundColorInactive;
    private Color foregroundColorActive;
    private Color foregroundColorInactive;
    private Color foregroundColorWeekendEnabled;
    private Color foregroundColorWeekendDisabled;

    public CalendarHeader(CalendarPane peer, Date date, Locale locale, TimeZone zone, HolidayPolicy holidayPolicy) {
//        this.backgroundColorActive = Microba.getOverridenColor("calendar.header.background.enabled", peer, UIManager.getColor("activeCaption"));
        this.backgroundColorActive = null;
        this.backgroundColorInactive = Microba.getOverridenColor("calendar.header.background.disabled", peer, UIManager.getColor("inactiveCaption"));
        this.foregroundColorActive = Microba.getOverridenColor("calendar.header.foreground.enabled", peer, UIManager.getColor("controlText"));
        this.foregroundColorInactive = Microba.getOverridenColor("calendar.header.foreground.disabled", peer, UIManager.getColor("textInactiveText"));
        this.foregroundColorWeekendEnabled = Microba.getOverridenColor("calendar.header.foreground.weekend.enabled", peer, Color.RED);
        this.foregroundColorWeekendDisabled = Microba.getOverridenColor("calendar.header.foreground.weekend.disabled", peer, this.foregroundColorInactive);
        this.locale = locale;
        this.zone = zone;
        this.date = date;
        this.holidayPolicy = holidayPolicy;
        this.reflectData();
    }

    private void reflectData() {
        Calendar cal = Calendar.getInstance(this.zone, this.locale);
        cal.setTime(this.date == null ? new Date() : this.date);
        SimpleDateFormat fmt = new SimpleDateFormat("E", this.locale);
        fmt.setTimeZone(this.zone);
        int numDaysInWeek = cal.getActualMaximum(7) - cal.getActualMinimum(7) + 1;
        int firstDayOfWeek = cal.getFirstDayOfWeek();
        cal.set(7, firstDayOfWeek);
        this.removeAll();
        this.setLayout(new GridLayout(1, numDaysInWeek, 2, 2));
        this.setBackground(this.isEnabled() ? this.backgroundColorActive : this.backgroundColorInactive);

        for (int i = 0; i < numDaysInWeek; ++i) {
            JLabel label = new JLabel();
            label.setText(fmt.format(cal.getTime()));
            label.setForeground(this.isEnabled() ? this.foregroundColorActive : this.foregroundColorInactive);
            label.setHorizontalAlignment(0);
            label.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
            Font boldFont = label.getFont().deriveFont(1);
            label.setFont(boldFont);
            this.add(label);
            boolean isHolliday = false;
            if (this.holidayPolicy != null) {
                isHolliday = this.holidayPolicy.isWeekend(this, cal);
            }

            if (isHolliday) {
                label.setForeground(this.isEnabled() ? this.foregroundColorWeekendEnabled : this.foregroundColorWeekendDisabled);
            }

            cal.add(7, 1);
        }

        this.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 1));
        this.revalidate();
        this.repaint();
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
        this.reflectData();
    }

    public void setDate(Date date) {
        this.date = date;
        this.reflectData();
    }

    public TimeZone getZone() {
        return this.zone;
    }

    public void setZone(TimeZone zone) {
        this.zone = zone;
        this.reflectData();
    }

    public void setHolidayPolicy(HolidayPolicy holidayPolicy) {
        this.holidayPolicy = holidayPolicy;
        this.reflectData();
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.reflectData();
    }
}

