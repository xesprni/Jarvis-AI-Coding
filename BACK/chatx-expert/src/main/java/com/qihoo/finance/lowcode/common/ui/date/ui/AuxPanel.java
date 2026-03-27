package com.qihoo.finance.lowcode.common.ui.date.ui;

import com.michaelbaranov.microba.calendar.CalendarResources;
import com.michaelbaranov.microba.calendar.VetoPolicy;
import com.michaelbaranov.microba.common.PolicyEvent;
import com.michaelbaranov.microba.common.PolicyListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.util.*;

class AuxPanel extends JPanel implements PropertyChangeListener, PolicyListener {
    public static final String PROPERTY_NAME_LOCALE = "locale";
    public static final String PROPERTY_NAME_DATE = "date";
    public static final String PROPERTY_NAME_ZONE = "zone";
    public static final String PROPERTY_NAME_RESOURCES = "resources";
    public static final String PROPERTY_NAME_VETO_MODEL = "vetoModel";
    private Locale locale;
    private TimeZone zone;
    private JButton todayButton;
    private JButton noneButton;
    private DateFormat fullDateFormat;
    private Date currentDate;
    private Set focusableComponents = new HashSet();
    private VetoPolicy vetoModel;
    private boolean showTodayBtn;
    private CalendarResources resources;
    private boolean showNoneButton;

    public AuxPanel(Locale locale, TimeZone zone, VetoPolicy vetoModel, boolean showTodayBtn, boolean showNoneButton, CalendarResources resources) {
        this.locale = locale;
        this.zone = zone;
        this.vetoModel = vetoModel;
        this.showTodayBtn = showTodayBtn;
        this.showNoneButton = showNoneButton;
        this.resources = resources;
        if (vetoModel != null) {
            vetoModel.addVetoPolicyListener(this);
        }

        this.setLayout(new GridBagLayout());
        this.todayButton = new JButton();
        this.todayButton.setBorderPainted(false);
        this.todayButton.setContentAreaFilled(false);
        this.todayButton.setVisible(showTodayBtn);
        this.noneButton = new JButton();
        this.noneButton.setBorderPainted(false);
        this.noneButton.setContentAreaFilled(false);
        this.noneButton.setVisible(showNoneButton);
        this.add(this.todayButton, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
        this.add(this.noneButton, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, 13, 0, new Insets(0, 0, 0, 0), 0, 0));
        this.currentDate = new Date();
        this.validateAgainstVeto();
        this.createLocaleAndZoneSensitive();
        this.reflectData();
        this.todayButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                AuxPanel.this.currentDate = new Date();
                AuxPanel.this.firePropertyChange("date", (Object) null, AuxPanel.this.currentDate);
            }
        });
        this.noneButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                AuxPanel.this.firePropertyChange("date", (Object) null, (Object) null);
            }
        });
        this.focusableComponents.add(this.todayButton);
        this.addPropertyChangeListener(this);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        Boolean value;
        if (evt.getPropertyName().equals("focusable")) {
            value = (Boolean) evt.getNewValue();
            this.todayButton.setFocusable(value);
            this.noneButton.setFocusable(value);
        }

        if (evt.getPropertyName().equals("enabled")) {
            value = (Boolean) evt.getNewValue();
            this.todayButton.setEnabled(value);
            this.noneButton.setEnabled(value);
        }

        if (evt.getPropertyName().equals("vetoModel")) {
            VetoPolicy oldValue = (VetoPolicy) evt.getOldValue();
            VetoPolicy newValue = (VetoPolicy) evt.getOldValue();
            if (oldValue != null) {
                oldValue.removeVetoPolicyListener(this);
            }

            if (newValue != null) {
                newValue.addVetoPolicyListener(this);
            }

            this.validateAgainstVeto();
        }

    }

    private void createLocaleAndZoneSensitive() {
        this.fullDateFormat = DateFormat.getDateInstance(2, this.locale);
        this.fullDateFormat.setTimeZone(this.zone);
    }

    private void reflectData() {
        String today = this.resources.getResource("key.today", this.locale);
        String none = this.resources.getResource("key.none", this.locale);
        this.todayButton.setText(today + ": " + this.fullDateFormat.format(this.currentDate));
//        this.todayButton.setText("设置为今天" + ": " + this.fullDateFormat.format(this.currentDate));
        this.noneButton.setText("清空");
    }

    public Locale getLocale() {
        return this.locale;
    }

    public void setLocale(Locale locale) {
        Locale old = this.locale;
        this.locale = locale;
        this.firePropertyChange("locale", old, locale);
        this.createLocaleAndZoneSensitive();
        this.reflectData();
    }

    public Collection getFocusableComponents() {
        return this.focusableComponents;
    }

    public TimeZone getZone() {
        return this.zone;
    }

    public void setZone(TimeZone zone) {
        this.zone = zone;
        this.createLocaleAndZoneSensitive();
        this.reflectData();
    }

    public Date getDate() {
        return this.currentDate;
    }

    public VetoPolicy getVetoModel() {
        return this.vetoModel;
    }

    public void setVetoModel(VetoPolicy vetoModel) {
        VetoPolicy old = this.vetoModel;
        this.vetoModel = vetoModel;
        this.firePropertyChange("vetoModel", old, vetoModel);
    }

    public void policyChanged(PolicyEvent event) {
        this.validateAgainstVeto();
    }

    private void validateAgainstVeto() {
        Calendar c = Calendar.getInstance(this.zone, this.locale);
        c.setTime(this.currentDate);
        if (this.vetoModel != null) {
            this.todayButton.setEnabled(!this.vetoModel.isRestricted(this, c));
            this.noneButton.setEnabled(!this.vetoModel.isRestrictNull(this));
        } else {
            this.todayButton.setEnabled(this.isEnabled());
            this.noneButton.setEnabled(this.isEnabled());
        }

    }

    public void setShowTodayBtn(boolean value) {
        this.showTodayBtn = value;
        this.todayButton.setVisible(this.showTodayBtn);
    }

    public void setResources(CalendarResources resources) {
        CalendarResources old = this.resources;
        this.resources = resources;
        this.firePropertyChange("resources", old, resources);
        this.reflectData();
    }

    public void setShowNoneButton(boolean value) {
        this.showNoneButton = value;
        this.noneButton.setVisible(this.showNoneButton);
    }
}

