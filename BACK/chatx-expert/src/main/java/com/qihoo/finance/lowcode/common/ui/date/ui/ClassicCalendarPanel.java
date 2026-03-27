package com.qihoo.finance.lowcode.common.ui.date.ui;

import com.qihoo.finance.lowcode.common.util.Icons;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

class ClassicCalendarPanel extends JPanel implements PropertyChangeListener {
    public static final String PROPERTY_NAME_DATE = "date";
    public static final String PROPERTY_NAME_LOCALE = "locale";
    public static final String PROPERTY_NAME_ZONE = "zone";
    private Locale locale;
    private TimeZone zone;
    private Calendar calendar;
    private JButton prevButton;
    private JButton nextButton;
    private JLabel selectedDateLabel;
    private DateFormat format;
    private Set focusableComponents = new HashSet();
    private JButton fastPrevButton;
    private JButton fastNextButton;

    private void setButtonStyle(JButton button) {
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setPreferredSize(new Dimension(22, 22));
    }

    public ClassicCalendarPanel(Date aDate, Locale aLocale, TimeZone zone) {
        this.locale = aLocale;
        this.zone = zone;
        this.prevButton = new JButton();
        this.nextButton = new JButton();
        this.fastPrevButton = new JButton();
        this.fastNextButton = new JButton();

//        this.nextButton.setIcon(new ImageIcon(Resource.class.getResource("forward-16.png")));
//        this.prevButton.setIcon(new ImageIcon(Resource.class.getResource("back-16.png")));
//        this.fastNextButton.setIcon(new ImageIcon(Resource.class.getResource("forward-fast-16.png")));
//        this.fastPrevButton.setIcon(new ImageIcon(Resource.class.getResource("back-fast-16.png")));
        // overwrite
        this.nextButton.setIcon(Icons.scaleToWidth(Icons.PAGE_NEXT, 16));
        setButtonStyle(this.nextButton);
        this.prevButton.setIcon(Icons.scaleToWidth(Icons.PAGE_PREV, 16));
        setButtonStyle(this.prevButton);
        this.fastNextButton.setIcon(Icons.scaleToWidth(Icons.PAGE_FAST_NEXT, 16));
        setButtonStyle(this.fastNextButton);
        this.fastPrevButton.setIcon(Icons.scaleToWidth(Icons.PAGE_FAST_PREV, 16));
        setButtonStyle(this.fastPrevButton);

        this.prevButton.setMargin(new Insets(0, 0, 0, 0));
        this.nextButton.setMargin(new Insets(0, 0, 0, 0));
        this.fastPrevButton.setMargin(new Insets(0, 0, 0, 0));
        this.fastNextButton.setMargin(new Insets(0, 0, 0, 0));
        Dimension psz = this.nextButton.getPreferredSize();
        Dimension npsz = new Dimension(psz.height, psz.height);
        this.nextButton.setPreferredSize(npsz);
        this.prevButton.setPreferredSize(npsz);
        this.selectedDateLabel = new JLabel();
        this.selectedDateLabel.setHorizontalAlignment(0);
//        this.selectedDateLabel.setFont(this.selectedDateLabel.getFont().deriveFont(1));
        this.selectedDateLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        this.setLayout(new GridBagLayout());
        this.add(this.fastPrevButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 3, 0), 0, 0));
        this.add(this.prevButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 3, 0), 0, 0));
        this.add(this.selectedDateLabel, new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0, 10, 2, new Insets(0, 3, 3, 3), 0, 0));
        this.add(this.nextButton, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 3, 0), 0, 0));
        this.add(this.fastNextButton, new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 3, 0), 0, 0));
        this.nextButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Date old = ClassicCalendarPanel.this.calendar.getTime();
                ClassicCalendarPanel.this.calendar.add(2, 1);
                ClassicCalendarPanel.this.firePropertyChange("date", old, ClassicCalendarPanel.this.getDate());
                ClassicCalendarPanel.this.reflectData();
            }
        });
        this.prevButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Date old = ClassicCalendarPanel.this.calendar.getTime();
                ClassicCalendarPanel.this.calendar.add(2, -1);
                ClassicCalendarPanel.this.firePropertyChange("date", old, ClassicCalendarPanel.this.getDate());
                ClassicCalendarPanel.this.reflectData();
            }
        });
        this.fastNextButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Date old = ClassicCalendarPanel.this.calendar.getTime();
                ClassicCalendarPanel.this.calendar.add(1, 1);
                ClassicCalendarPanel.this.firePropertyChange("date", old, ClassicCalendarPanel.this.getDate());
                ClassicCalendarPanel.this.reflectData();
            }
        });
        this.fastPrevButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Date old = ClassicCalendarPanel.this.calendar.getTime();
                ClassicCalendarPanel.this.calendar.add(1, -1);
                ClassicCalendarPanel.this.firePropertyChange("date", old, ClassicCalendarPanel.this.getDate());
                ClassicCalendarPanel.this.reflectData();
            }
        });
        this.addPropertyChangeListener(this);
        this.focusableComponents.add(this.prevButton);
        this.focusableComponents.add(this.nextButton);
        this.focusableComponents.add(this.fastNextButton);
        this.focusableComponents.add(this.fastPrevButton);
        this.createLocaleAndZoneSensitive();
        this.calendar.setTime(aDate);
        this.reflectData();
    }

    private void createLocaleAndZoneSensitive() {
        if (this.calendar != null) {
            Date old = this.calendar.getTime();
            this.calendar = Calendar.getInstance(this.zone, this.locale);
            this.calendar.setTime(old);
        } else {
            this.calendar = Calendar.getInstance(this.zone, this.locale);
        }

        this.format = new SimpleDateFormat("MMMMM yyyy", this.locale);
        this.format.setTimeZone(this.zone);
        this.setPreferredLabelSize();
    }

    private void setPreferredLabelSize() {
        Calendar c = Calendar.getInstance(this.zone, this.locale);
        c.setTime(this.getDate());
        JLabel l = new JLabel();
        l.setFont(this.selectedDateLabel.getFont());
        int maxWidth = Integer.MIN_VALUE;

        for (int i = 0; i <= c.getActualMaximum(2); ++i) {
            c.set(2, i);
            String text = this.format.format(c.getTime());
            l.setText(text);
            int w = l.getPreferredSize().width;
            if (w > maxWidth) {
                maxWidth = w;
            }
        }

        Dimension d = l.getPreferredSize();
        d.width = maxWidth + 10;
        this.selectedDateLabel.setMinimumSize(d);
        this.selectedDateLabel.setPreferredSize(d);
        this.revalidate();
    }

    private void reflectData() {
        this.selectedDateLabel.setText(this.format.format(this.calendar.getTime()));
    }

    public Date getDate() {
        return this.calendar.getTime();
    }

    public void setDate(Date date) {
        Date old = this.getDate();
        this.calendar.setTime(date);
        this.firePropertyChange("date", old, date);
        this.reflectData();
    }

    public Locale getLocale() {
        return this.locale;
    }

    public void setLocale(Locale locale) {
        Locale old = this.locale;
        this.locale = locale;
        this.createLocaleAndZoneSensitive();
        this.firePropertyChange("locale", old, locale);
        this.reflectData();
    }

    public TimeZone getZone() {
        return this.zone;
    }

    public void setZone(TimeZone zone) {
        TimeZone old = this.zone;
        this.zone = zone;
        this.createLocaleAndZoneSensitive();
        this.firePropertyChange("zone", old, this.locale);
        this.reflectData();
    }

    public void propertyChange(PropertyChangeEvent evt) {
        Boolean value;
        if (evt.getPropertyName().equals("focusable")) {
            value = (Boolean) evt.getNewValue();
            this.prevButton.setFocusable(value);
            this.nextButton.setFocusable(value);
            this.fastNextButton.setFocusable(value);
            this.fastPrevButton.setFocusable(value);
        }

        if (evt.getPropertyName().equals("enabled")) {
            value = (Boolean) evt.getNewValue();
            this.prevButton.setEnabled(value);
            this.nextButton.setEnabled(value);
            this.fastNextButton.setEnabled(value);
            this.fastPrevButton.setEnabled(value);
        }

    }

    public Collection getFocusableComponents() {
        return this.focusableComponents;
    }

    public void addMonth(int m) {
        int modM = m > 0 ? m : -m;
        int sign = m > 0 ? 1 : -1;
        Date old = this.calendar.getTime();

        for (int i = 0; i < modM; ++i) {
            this.calendar.add(2, sign);
        }

        this.firePropertyChange("date", old, this.getDate());
        this.reflectData();
    }
}

