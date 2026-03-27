package com.qihoo.finance.lowcode.common.ui.date.ui;

import com.michaelbaranov.microba.calendar.CalendarResources;
import com.michaelbaranov.microba.calendar.DefaultCalendarResources;
import com.michaelbaranov.microba.calendar.HolidayPolicy;
import com.michaelbaranov.microba.calendar.VetoPolicy;
import com.michaelbaranov.microba.common.CommitEvent;
import com.michaelbaranov.microba.common.CommitListener;
import com.michaelbaranov.microba.common.MicrobaComponent;
import com.michaelbaranov.microba.calendar.CalendarColors;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class CalendarPane extends MicrobaComponent implements CalendarColors {
    public static final String PROPERTY_NAME_DATE = "date";
    public static final String PROPERTY_NAME_LOCALE = "locale";
    public static final String PROPERTY_NAME_ZONE = "zone";
    public static final String PROPERTY_NAME_STYLE = "style";
    public static final String PROPERTY_NAME_SHOW_TODAY_BTN = "showTodayButton";
    public static final String PROPERTY_NAME_SHOW_NONE_BTN = "showNoneButton";
    public static final String PROPERTY_NAME_FOCUS_LOST_BEHAVIOR = "focusLostBehavior";
    public static final String PROPERTY_NAME_VETO_POLICY = "vetoPlicy";
    public static final String PROPERTY_NAME_HOLIDAY_POLICY = "holidayPolicy";
    public static final String PROPERTY_NAME_RESOURCES = "resources";
    public static final String PROPERTY_NAME_SHOW_NUMBER_WEEK = "showNumberOfWeek";
    public static final String PROPERTY_NAME_STRIP_TIME = "stripTime";
    public static final int STYLE_MODERN = 16;
    public static final int STYLE_CLASSIC = 32;
    private static final String uiClassID = "microba.CalendarPaneUI";
    private EventListenerList commitListenerList;
    private EventListenerList actionListenerList;
    private Date date;
    private TimeZone zone;
    private Locale locale;
    private VetoPolicy vetoPolicy;
    private HolidayPolicy holidayPolicy;
    private CalendarResources resources;
    private int style;
    private boolean showTodayButton;
    private boolean showNoneButton;
    private int focusLostBehavior;
    private boolean showNumberOfWeek;
    private boolean stripTime;

    public String getUIClassID() {
        return "MyCalendarPaneUI";
    }

    public CalendarPane() {
        this((Date) null, 0, Locale.getDefault(), TimeZone.getDefault());
    }

    public CalendarPane(int style) {
        this((Date) null, style, Locale.getDefault(), TimeZone.getDefault());
    }

    public CalendarPane(Date initialDate) {
        this(initialDate, 0, Locale.getDefault(), TimeZone.getDefault());
    }

    public CalendarPane(Date initialDate, int style) {
        this(initialDate, style, Locale.getDefault(), TimeZone.getDefault());
    }

    public CalendarPane(Date initialDate, int style, Locale locale) {
        this(initialDate, style, locale, TimeZone.getDefault());
    }

    public CalendarPane(Date initialDate, int style, Locale locale, TimeZone zone) {
        this.commitListenerList = new EventListenerList();
        this.actionListenerList = new EventListenerList();
        this.checkStyle(style);
        this.checkLocale(locale);
        this.checkTimeZone(zone);
        this.style = style;
        this.date = initialDate;
        this.locale = locale;
        this.zone = zone;
        this.focusLostBehavior = 1;
        this.showTodayButton = true;
        this.showNoneButton = true;
        this.vetoPolicy = null;
        this.resources = new DefaultCalendarResources();
        this.stripTime = true;
        this.addPropertyChangeListener("date", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                CalendarPane.this.fireActionEvent();
            }
        });

        MyCalendarPaneUI ui = new MyCalendarPaneUI();
        ui.installUI(this);
        UIManager.put(this, ui);

        this.updateUI();
    }

    public Date getDate() {
        return this.stripTime ? stripTime(this.date, this.getZone(), this.getLocale()) : this.date;
    }

    public void setDate(Date date) throws PropertyVetoException {
        if (!this.checkDate(date)) {
            PropertyChangeEvent propertyChangeEvent = new PropertyChangeEvent(this, "date", this.date, date);
            throw new PropertyVetoException("Value vetoed by current vetoPolicy", propertyChangeEvent);
        } else {
            Date old = this.date;
            this.date = date;
            if (old != null || date != null) {
                this.firePropertyChange("date", old, date);
            }

        }
    }

    public Locale getLocale() {
        return this.locale;
    }

    public void setLocale(Locale locale) {
        Locale old = this.getLocale();
        this.locale = locale;
        this.firePropertyChange("locale", old, this.getLocale());
    }

    public TimeZone getZone() {
        return this.zone;
    }

    public void setZone(TimeZone zone) {
        TimeZone old = this.getZone();
        this.zone = zone;
        this.firePropertyChange("zone", old, this.getZone());
    }

    public int getStyle() {
        return this.style;
    }

    public void setStyle(int style) {
        style = this.checkStyle(style);
        int old = this.style;
        this.style = style;
        this.firePropertyChange("style", old, style);
    }

    public boolean isShowTodayButton() {
        return this.showTodayButton;
    }

    public void setShowTodayButton(boolean visible) {
        Boolean old = Boolean.valueOf(this.showTodayButton);
        this.showTodayButton = visible;
        this.firePropertyChange("showTodayButton", old, Boolean.valueOf(visible));
    }

    public boolean isShowNoneButton() {
        return this.showNoneButton;
    }

    public void setShowNoneButton(boolean visible) {
        Boolean old = Boolean.valueOf(this.showNoneButton);
        this.showNoneButton = visible;
        this.firePropertyChange("showNoneButton", old, Boolean.valueOf(visible));
    }

    public int getFocusLostBehavior() {
        return this.focusLostBehavior;
    }

    public void setFocusLostBehavior(int behavior) {
        behavior = this.checkFocusLostbehavior(behavior);
        int old = this.focusLostBehavior;
        this.focusLostBehavior = behavior;
        this.firePropertyChange("focusLostBehavior", old, behavior);
    }

    public CalendarResources getResources() {
        return this.resources;
    }

    public void setResources(CalendarResources resources) {
        CalendarResources old = this.resources;
        this.resources = resources;
        this.firePropertyChange("resources", old, resources);
    }

    public HolidayPolicy getHolidayPolicy() {
        return this.holidayPolicy;
    }

    public void setHolidayPolicy(HolidayPolicy holidayPolicy) {
        HolidayPolicy old = this.holidayPolicy;
        this.holidayPolicy = holidayPolicy;
        this.firePropertyChange("holidayPolicy", old, holidayPolicy);
    }

    public VetoPolicy getVetoPolicy() {
        return this.vetoPolicy;
    }

    public void setVetoPolicy(VetoPolicy vetoModel) {
        VetoPolicy old = this.vetoPolicy;
        this.vetoPolicy = vetoModel;
        this.firePropertyChange("vetoPlicy", old, vetoModel);
    }

    public boolean isShowNumberOfWeek() {
        return this.showNumberOfWeek;
    }

    public boolean isStripTime() {
        return this.stripTime;
    }

    public void setStripTime(boolean stripTime) {
        this.stripTime = stripTime;
    }

    public void setShowNumberOfWeek(boolean visible) {
        boolean old = this.showNumberOfWeek;
        this.showNumberOfWeek = visible;
        this.firePropertyChange("showNumberOfWeek", old, visible);
    }

    public void addActionListener(ActionListener listener) {
        this.actionListenerList.add(ActionListener.class, listener);
    }

    public void removeActionListener(ActionListener listener) {
        this.actionListenerList.remove(ActionListener.class, listener);
    }

    public void addCommitListener(CommitListener listener) {
        this.commitListenerList.add(CommitListener.class, listener);
    }

    public void removeCommitListener(CommitListener listener) {
        this.commitListenerList.remove(CommitListener.class, listener);
    }

    public boolean commitEdit() {
        try {
            ((MyCalendarPaneUI) this.getUI()).commit();
            this.fireCommitEvent(true);
            return true;
        } catch (Exception var2) {
            return false;
        }
    }

    public void revertEdit() {
        ((MyCalendarPaneUI) this.getUI()).revert();
        this.fireCommitEvent(false);
    }

    public void commitOrRevert() {
        switch (this.focusLostBehavior) {
            case 0:
                this.commitEdit();
                break;
            case 1:
                if (!this.commitEdit()) {
                    this.revertEdit();
                }
                break;
            case 2:
                this.revertEdit();
            case 3:
        }

    }

    public void fireCommitEvent(boolean commit) {
        Object[] listeners = this.commitListenerList.getListenerList();

        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == CommitListener.class) {
                ((CommitListener) listeners[i + 1]).commit(new CommitEvent(this, commit));
            }
        }

    }

    public void fireActionEvent() {
        Object[] listeners = this.actionListenerList.getListenerList();

        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ActionListener.class) {
                ((ActionListener) listeners[i + 1]).actionPerformed(new ActionEvent(this, 0, "value"));
            }
        }

    }

    private void checkTimeZone(TimeZone zone) {
        if (zone == null) {
            throw new IllegalArgumentException("'zone' can not be null.");
        }
    }

    private void checkLocale(Locale locale) {
        if (locale == null) {
            throw new IllegalArgumentException("'locale' can not be null.");
        }
    }

    private int checkFocusLostbehavior(int behavior) {
        if (behavior != 0 && behavior != 1 && behavior != 2 && behavior != 3) {
            throw new IllegalArgumentException("focusLostBehavior: unrecognized behavior");
        } else {
            return behavior;
        }
    }

    private boolean checkDate(Date date) {
        if (this.vetoPolicy != null) {
            if (date == null) {
                return !this.vetoPolicy.isRestrictNull(this);
            } else {
                return !this.vetoPolicy.isRestricted(this, this.makeCurrentCalendar(date));
            }
        } else {
            return true;
        }
    }

    private int checkStyle(int style) {
        if (style == 0) {
            style = 32;
        }

        if (style != 32 && style != 16) {
            throw new IllegalArgumentException("style: unrecognized style");
        } else {
            return style;
        }
    }

    private Calendar makeCurrentCalendar(Date date) {
        Calendar c = Calendar.getInstance(this.zone, this.locale);
        c.setTime(date);
        return c;
    }

    public static Date stripTime(Date date, TimeZone zone, Locale locale) {
        if (date == null) {
            return null;
        } else {
            Calendar tmpCalendar = Calendar.getInstance(zone, locale);
            tmpCalendar.setTime(date);
            tmpCalendar.set(11, tmpCalendar.getMinimum(11));
            tmpCalendar.set(12, tmpCalendar.getMinimum(12));
            tmpCalendar.set(13, tmpCalendar.getMinimum(13));
            tmpCalendar.set(14, tmpCalendar.getMinimum(14));
            return tmpCalendar.getTime();
        }
    }
}
