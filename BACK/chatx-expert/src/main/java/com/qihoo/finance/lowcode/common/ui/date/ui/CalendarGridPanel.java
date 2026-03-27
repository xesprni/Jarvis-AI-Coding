package com.qihoo.finance.lowcode.common.ui.date.ui;

import com.intellij.ui.JBColor;
import com.michaelbaranov.microba.Microba;
import com.michaelbaranov.microba.calendar.HolidayPolicy;
import com.michaelbaranov.microba.calendar.VetoPolicy;
import com.michaelbaranov.microba.common.PolicyEvent;
import com.michaelbaranov.microba.common.PolicyListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

class CalendarGridPanel extends JPanel implements FocusListener, PolicyListener, PropertyChangeListener, MouseListener, KeyListener {
    public static final String PROPERTY_NAME_DATE = "date";
    public static final String PROPERTY_NAME_BASE_DATE = "baseDate";
    public static final String PROPERTY_NAME_LOCALE = "locale";
    public static final String PROPERTY_NAME_ZONE = "zone";
    public static final String PROPERTY_NAME_VETO_POLICY = "vetoPolicy";
    private static final String PROPERTY_NAME_HOLIDAY_POLICY = "holidayPolicy";
    public static final String PROPERTY_NAME_NOTIFY_SELECTED_DATE_CLICKED = "##same date clicked##";
    private CalendarPane peer;
    private Date date;
    private Date baseDate;
    private Date focusDate;
    private Locale locale;
    private TimeZone zone;
    private VetoPolicy vetoPolicy;
    private CalendarGridPanel.DateLabel[] labels = new CalendarGridPanel.DateLabel[42];
    private Set focusableComponents = new HashSet();
    private boolean explicitDateSetToNullFlag;
    private HolidayPolicy holidayPolicy;
    private Color focusColor;
    private Color restrictedColor;
    private Color gridBgEn;
    private Color gridBgDis;
    private Color gridFgEn;
    private Color gridFgDis;
    private Color selBgEn;
    private Color selBgDis;
    private Color wkFgEn;
    private Color wkFgDis;
    private Color holFgEn;
    private Color holFgDis;

    public CalendarGridPanel(CalendarPane peer, Date date, Locale locale, TimeZone zone, VetoPolicy vetoDateModel, HolidayPolicy holidayPolicy) {
        this.peer = peer;
//        this.focusColor = Microba.getOverridenColor("calendar.grid.focus", peer, UIManager.getColor("TabbedPane.focus"));
        this.focusColor = Microba.getOverridenColor("calendar.grid.focus", peer, JBColor.GRAY);
        this.restrictedColor = Microba.getOverridenColor("calendar.grid.banned", peer, Color.RED);
        this.gridBgEn = Microba.getOverridenColor("calendar.grid.background.enabled", peer, UIManager.getColor("TextField.background"));
        this.gridBgDis = Microba.getOverridenColor("calendar.grid.background.disabled", peer, UIManager.getColor("TextField.background"));
        this.gridFgEn = Microba.getOverridenColor("calendar.grid.foreground.enabled", peer, UIManager.getColor("TextField.foreground"));
        this.gridFgDis = Microba.getOverridenColor("calendar.grid.foreground.disabled", peer, UIManager.getColor("controlText"));
//        this.selBgEn = Microba.getOverridenColor("calendar.grid.selection.background.enabled", peer, UIManager.getColor("ComboBox.selectionBackground"));
        this.selBgDis = Microba.getOverridenColor("calendar.grid.selection.background.disabled", peer, UIManager.getColor("ComboBox.selectionBackground"));
        this.selBgEn = Microba.getOverridenColor("calendar.grid.selection.background.enabled", peer, JBColor.GRAY);
        this.selBgDis = Microba.getOverridenColor("calendar.grid.selection.background.disabled", peer, JBColor.GRAY);
        this.wkFgDis = Microba.getOverridenColor("calendar.grid.weekend.foreground.disabled", peer, this.gridFgDis);
        this.wkFgEn = Microba.getOverridenColor("calendar.grid.weekend.foreground.enabled", peer, Color.RED);
        this.holFgDis = Microba.getOverridenColor("calendar.grid.holiday.foreground.disabled", peer, this.gridFgDis);
        this.holFgEn = Microba.getOverridenColor("calendar.grid.holiday.foreground.enabled", peer, Color.RED);

        this.locale = locale;
        this.zone = zone;
        this.date = date;
        this.baseDate = date == null ? new Date() : date;
        this.explicitDateSetToNullFlag = date == null;
        this.focusDate = this.getFocusDateForDate(date);
        this.vetoPolicy = vetoDateModel;
        this.holidayPolicy = holidayPolicy;
        if (this.vetoPolicy != null) {
            this.vetoPolicy.addVetoPolicyListener(this);
        }

        if (this.holidayPolicy != null) {
            this.holidayPolicy.addVetoPolicyListener(this);
        }

        this.addPropertyChangeListener(this);
        this.setLayout(new GridLayout(6, 7, 2, 2));

        for (int i = 0; i < 42; ++i) {
            CalendarGridPanel.DateLabel l = new CalendarGridPanel.DateLabel(i);
            this.labels[i] = l;
            l.setText(String.valueOf(i));
            l.addMouseListener(this);
            this.add(l);
        }

        this.focusableComponents.add(this);
        this.addKeyListener(this);
        this.setFocusable(true);
        InputMap input = this.getInputMap(0);
        input.put(KeyStroke.getKeyStroke(10, 0), "##microba.commit##");
        input.put(KeyStroke.getKeyStroke(32, 0), "##microba.commit##");
        this.getActionMap().put("##microba.commit##", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Calendar c = CalendarGridPanel.this.getCalendar(CalendarGridPanel.this.focusDate);
                if (CalendarGridPanel.this.vetoPolicy == null || !CalendarGridPanel.this.vetoPolicy.isRestricted(this, c)) {
                    CalendarGridPanel.this.setDate(CalendarGridPanel.this.focusDate);
                }

            }
        });
        this.addFocusListener(this);
        this.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        this.reflectData();
    }

    public void focusGained(FocusEvent e) {
        this.setBorder(BorderFactory.createLineBorder(this.focusColor));
        this.reflectFocusedDate();
    }

    public void focusLost(FocusEvent e) {
        this.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        this.reflectFocusedDate();
    }

    private void setSelectedByIndex(int i) {
        CalendarGridPanel.DateLabel label = this.labels[i];
        if (label.isVisible()) {
            int day = Integer.parseInt(label.getText());
            Calendar c = this.getCalendar(this.baseDate);
            c.set(5, day);
            this.setDate(c.getTime());
        }

    }

    private Calendar getCalendar(Date date) {
        Calendar c = Calendar.getInstance(this.zone, this.locale);
        c.setTime(date);
        return c;
    }

    private int getSelectedIndex() {
        if (this.date == null) {
            return -1;
        } else {
            Calendar bc = this.getCalendar(this.baseDate);
            Calendar sc = this.getCalendar(this.date);
            if (bc.get(0) == sc.get(0) && bc.get(1) == sc.get(1) && bc.get(2) == sc.get(2)) {
                bc.set(5, 1);
                int skipBefore = bc.get(7) - bc.getFirstDayOfWeek();
                if (skipBefore < 0) {
                    skipBefore += 7;
                }

                int selDay = sc.get(5);
                return skipBefore + selDay - 1;
            } else {
                return -1;
            }
        }
    }

    private void setFocusedByIndex(int i) {
        CalendarGridPanel.DateLabel label = this.labels[i];
        if (label.isVisible()) {
            int day = Integer.parseInt(label.getText());
            Calendar c = this.getCalendar(this.baseDate);
            c.set(5, day);
            this.setFocusDate(c.getTime());
        }

    }

    private int getFocusedIndex() {
        Calendar bc = this.getCalendar(this.baseDate);
        Calendar fc = this.getCalendar(this.focusDate);
        bc.set(5, 1);
        int skipBefore = bc.get(7) - bc.getFirstDayOfWeek();
        if (skipBefore < 0) {
            skipBefore += 7;
        }

        int selDay = fc.get(5);
        int maxDay = bc.getActualMaximum(5);
        if (selDay > maxDay) {
            selDay = maxDay;
        }

        return skipBefore + selDay - 1;
    }

    private void reflectData() {
        this.setBackground(this.isEnabled() ? this.gridBgEn : this.gridBgDis);
        this.reflectBaseDate();
        this.reflectSelectedDate();
        this.reflectFocusedDate();
    }

    private void reflectFocusedDate() {
        int focusedIndex = this.getFocusedIndex();
        CalendarGridPanel.DateLabel l = this.labels[focusedIndex];
        l.setFocused(this.isFocusOwner());
    }

    private void reflectSelectedDate() {
        int selIndex = this.getSelectedIndex();
        if (selIndex > -1) {
            CalendarGridPanel.DateLabel l = this.labels[selIndex];
            l.setSelected(true);
        }

    }

    private void reflectBaseDate() {
        Calendar calendar = this.getCalendar(this.baseDate);
        calendar.set(5, 1);
        int skipBefore = calendar.get(7) - calendar.getFirstDayOfWeek();
        if (skipBefore < 0) {
            skipBefore += 7;
        }

        int activeDays = calendar.getActualMaximum(5);
        int day = 1;
        calendar.setTime(this.baseDate);
        calendar.set(5, 1);

        for (int i = 0; i < 42; ++i) {
            CalendarGridPanel.DateLabel l = this.labels[i];
            l.setBackground(this.isEnabled() ? this.selBgEn : this.selBgDis);
            l.setSelected(false);
            l.setFocused(false);
            l.setEnabled(this.isEnabled());
            if (i < skipBefore) {
                l.setText("");
                l.setVisible(false);
            }

            if (i >= skipBefore && i < skipBefore + activeDays) {
                l.setVisible(true);
                l.setText(String.valueOf(day));
                if (this.vetoPolicy != null) {
                    l.setBanned(this.vetoPolicy.isRestricted(this.peer, calendar));
                } else {
                    l.setBanned(false);
                }

                if (this.holidayPolicy != null) {
                    l.setDate(calendar.getTime());
                    l.setHolliday(this.holidayPolicy.isHolliday(this.peer, calendar));
                    l.setWeekend(this.holidayPolicy.isWeekend(this.peer, calendar));
                } else {
                    l.setHolliday(false);
                    l.setWeekend(false);
                }

                ++day;
                calendar.add(5, 1);
            }

            if (i >= skipBefore + activeDays) {
                l.setText("");
                l.setVisible(false);
            }
        }

    }

    public Date getDate() {
        return this.date;
    }

    public void setDate(Date date) {
        Date old = this.date;
        this.date = date;
        this.explicitDateSetToNullFlag = date == null;
        this.focusDate = this.getFocusDateForDate(date);
        if (old != null || date != null) {
            this.firePropertyChange("date", old, date);
        }

        this.reflectData();
    }

    private Date getFocusDateForDate(Date date) {
        if (date == null) {
            Calendar c = this.getCalendar(this.baseDate);
            c.set(5, 1);
            return c.getTime();
        } else {
            return date;
        }
    }

    public Locale getLocale() {
        return this.locale;
    }

    public void setLocale(Locale locale) {
        Locale old = this.locale;
        this.locale = locale;
        this.firePropertyChange("locale", old, locale);
        this.reflectData();
    }

    public VetoPolicy getVetoPolicy() {
        return this.vetoPolicy;
    }

    public void setVetoPolicy(VetoPolicy vetoModel) {
        VetoPolicy old = this.getVetoPolicy();
        this.vetoPolicy = vetoModel;
        this.firePropertyChange("vetoPolicy", old, vetoModel);
        this.reflectData();
    }

    public HolidayPolicy getHolidayPolicy() {
        return this.holidayPolicy;
    }

    public void setHolidayPolicy(HolidayPolicy holidayPolicy) {
        HolidayPolicy old = this.getHolidayPolicy();
        this.holidayPolicy = holidayPolicy;
        this.firePropertyChange("holidayPolicy", old, holidayPolicy);
        this.reflectData();
    }

    public TimeZone getZone() {
        return this.zone;
    }

    public void setZone(TimeZone zone) {
        TimeZone old = this.zone;
        this.zone = zone;
        this.firePropertyChange("zone", old, zone);
        this.reflectData();
    }

    public Collection getFocusableComponents() {
        return this.focusableComponents;
    }

    public void policyChanged(PolicyEvent event) {
        this.reflectData();
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("vetoPolicy")) {
            VetoPolicy oldValue = (VetoPolicy) evt.getOldValue();
            VetoPolicy newValue = (VetoPolicy) evt.getOldValue();
            if (oldValue != null) {
                oldValue.removeVetoPolicyListener(this);
            }

            if (newValue != null) {
                newValue.addVetoPolicyListener(this);
            }

            this.reflectData();
        }

    }

    public Date getBaseDate() {
        return this.baseDate;
    }

    public void setBaseDate(Date baseDate) {
        Date old = this.baseDate;
        this.baseDate = baseDate;
        this.firePropertyChange("baseDate", old, baseDate);
        Calendar bc = this.getCalendar(baseDate);
        Calendar fc = this.getCalendar(this.focusDate);
        int focDate = fc.get(5);
        int maxDay = bc.getActualMaximum(5);
        if (focDate > maxDay) {
            focDate = maxDay;
        }

        bc.set(5, focDate);
        this.focusDate = bc.getTime();
        this.reflectData();
    }

    private Date getFocusDate() {
        return this.focusDate;
    }

    private void setFocusDate(Date focusDate) {
        this.focusDate = focusDate;
        this.explicitDateSetToNullFlag = false;
        this.reflectData();
    }

    public void mouseClicked(MouseEvent e) {
        if (this.isEnabled()) {
            this.requestFocusInWindow();
            CalendarGridPanel.DateLabel l = (CalendarGridPanel.DateLabel) e.getSource();
            if (l.isVisible()) {
                int id = Integer.parseInt(l.getText());
                Calendar c = this.getCalendar(this.baseDate);
                c.set(5, id);
                if (this.vetoPolicy == null || !this.vetoPolicy.isRestricted(this, c)) {
                    boolean selected = l.isSelected();
                    this.setDate(c.getTime());
                    if (selected) {
                        this.firePropertyChange("##same date clicked##", (Object) null, Integer.valueOf(id));
                    }
                }
            }

        }
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void keyTyped(KeyEvent e) {
    }

    public void keyPressed(KeyEvent e) {
        if (this.isEnabled()) {
            int id = this.getFocusedIndex();
            int row = id / 7;
            int col = id % 7;
            if (e.getKeyCode() == 40) {
                ++row;
                if (row < 6) {
                    this.setFocusedByIndex(row * 7 + col);
                }
            }

            if (e.getKeyCode() == 38) {
                --row;
                if (row >= 0) {
                    this.setFocusedByIndex(row * 7 + col);
                }
            }

            if (e.getKeyCode() == 37) {
                --col;
                if (col >= 0) {
                    this.setFocusedByIndex(row * 7 + col);
                }
            }

            if (e.getKeyCode() == 39) {
                ++col;
                if (col < 7) {
                    this.setFocusedByIndex(row * 7 + col);
                }
            }

        }
    }

    public void keyReleased(KeyEvent e) {
    }

    public Date getDateToCommit() {
        Calendar c = this.getCalendar(this.focusDate);
        return !this.explicitDateSetToNullFlag && (this.vetoPolicy == null || !this.vetoPolicy.isRestricted(this, c)) ? this.focusDate : this.date;
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.reflectData();
    }

    class DateLabel extends JLabel {
        private Date date;
        private int id;
        private boolean focused;
        private boolean selected;
        private boolean weekend;
        private boolean banned;
        private boolean holliday;

        public DateLabel(int id) {
            this.id = id;
            this.setHorizontalAlignment(0);
            this.setFocused(false);
            this.setSelected(false);
            this.setWeekend(false);
            this.setBanned(false);
            this.setHolliday(false);
        }

        public void setHolliday(boolean b) {
            this.holliday = b;
            this.update();
            this.repaint();
        }

        public int getId() {
            return this.id;
        }

        public boolean isFocused() {
            return this.focused;
        }

        public void setFocused(boolean focused) {
            this.focused = focused;
            this.update();
            this.repaint();
        }

        public boolean isSelected() {
            return this.selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
            this.update();
            this.repaint();
        }

        private void update() {
            this.updateBg();
            this.updateFg();
            this.udapteBorder();
            this.updateTooltip();
        }

        private void updateTooltip() {
            if (CalendarGridPanel.this.holidayPolicy != null && this.holliday) {
                Calendar c = Calendar.getInstance(CalendarGridPanel.this.zone, CalendarGridPanel.this.locale);
                c.setTime(this.date);
                this.setToolTipText(CalendarGridPanel.this.holidayPolicy.getHollidayName(this, c));
            } else {
                this.setToolTipText((String) null);
            }

        }

        private void udapteBorder() {
            if (this.isFocused() && this.isEnabled()) {
                this.setBorder(BorderFactory.createLineBorder(this.banned ? CalendarGridPanel.this.restrictedColor : CalendarGridPanel.this.focusColor));
            } else {
                this.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
            }

        }

        private void updateFg() {
            if (this.isHolliday()) {
                this.setForeground(this.isEnabled() ? CalendarGridPanel.this.holFgEn : CalendarGridPanel.this.holFgDis);
            } else if (this.isWeekend()) {
                this.setForeground(this.isEnabled() ? CalendarGridPanel.this.wkFgEn : CalendarGridPanel.this.wkFgDis);
            } else {
                this.setForeground(this.isEnabled() ? CalendarGridPanel.this.gridFgEn : CalendarGridPanel.this.gridFgDis);
            }

        }

        private void updateBg() {
            if (this.isSelected()) {
                this.setOpaque(true);
                this.setBackground(this.isEnabled() ? CalendarGridPanel.this.selBgEn : CalendarGridPanel.this.selBgDis);
            } else {
                this.setOpaque(false);
            }

        }

        public boolean isWeekend() {
            return this.weekend;
        }

        public void setWeekend(boolean weekend) {
            this.weekend = weekend;
        }

        public boolean isBanned() {
            return this.banned;
        }

        public void setBanned(boolean banned) {
            this.banned = banned;
            this.update();
            this.repaint();
        }

        public void paint(Graphics g) {
            if (this.isBanned()) {
                g.setColor(CalendarGridPanel.this.restrictedColor);
                g.drawLine(2, 2, this.getWidth() - 4, this.getHeight() - 4);
                g.drawLine(2, this.getHeight() - 4, this.getWidth() - 4, 2);
            }

            super.paint(g);
        }

        public boolean isHolliday() {
            return this.holliday;
        }

        public Date getDate() {
            return this.date;
        }

        public void setDate(Date date) {
            this.date = date;
        }
    }
}

