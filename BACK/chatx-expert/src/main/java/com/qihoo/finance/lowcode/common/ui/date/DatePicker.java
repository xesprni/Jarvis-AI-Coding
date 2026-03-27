package com.qihoo.finance.lowcode.common.ui.date;

import com.qihoo.finance.lowcode.common.ui.date.ui.MyCalendarPaneUI;
import com.qihoo.finance.lowcode.common.ui.date.ui.MyDatePickerUI;

import javax.swing.*;
import java.text.SimpleDateFormat;

/**
 * DatePicker
 *
 * @author fengjinfu-jk
 * date 2024/1/6
 * @version 1.0.0
 * @apiNote DatePicker
 */
public class DatePicker extends com.michaelbaranov.microba.calendar.DatePicker {
    static {
        UIManager.put("MyCalendarPaneUI", MyCalendarPaneUI.class.getName());
        UIManager.put("MyDatePickerUI", MyDatePickerUI.class.getName());
        UIManager.getDefaults().put("MyCalendarPaneUI", MyCalendarPaneUI.class.getName());
        UIManager.getDefaults().put("MyDatePickerUI", MyDatePickerUI.class.getName());
    }

    public DatePicker() {
        MyDatePickerUI ui = new MyDatePickerUI();
        ui.installUI(this);
        UIManager.put(this, ui);
    }

    public DatePicker(String dateFormat) {
        this();
        this.setDateFormat(new SimpleDateFormat(dateFormat));
    }

    @Override
    public String getUIClassID() {
        return "MyDatePickerUI";
    }
}
