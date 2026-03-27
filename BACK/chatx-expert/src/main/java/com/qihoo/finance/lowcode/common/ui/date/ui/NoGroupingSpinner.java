package com.qihoo.finance.lowcode.common.ui.date.ui;

import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.text.NumberFormat;

class NoGroupingSpinner extends JSpinner {
    public NoGroupingSpinner(SpinnerModel spinnerModel) {
        super(spinnerModel);
    }

    protected JComponent createEditor(SpinnerModel model) {
        return (JComponent) (model instanceof SpinnerNumberModel ? new NoGroupingSpinner.NoGroupingNumberEditor(this, model) : super.createEditor(model));
    }

    public static class NoGroupingNumberEditor extends JSpinner.NumberEditor {
        public NoGroupingNumberEditor(JSpinner spinner, SpinnerModel model) {
            super(spinner);
            JFormattedTextField ftf = (JFormattedTextField) this.getComponent(0);
            NumberFormat fmt = NumberFormat.getIntegerInstance();
            fmt.setGroupingUsed(false);
            ftf.setFormatterFactory(new DefaultFormatterFactory(new NumberFormatter(fmt)));
            this.revalidate();
        }
    }
}

