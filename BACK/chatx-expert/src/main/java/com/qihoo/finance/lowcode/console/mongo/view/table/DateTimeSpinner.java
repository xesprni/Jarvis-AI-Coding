/*
 * Copyright (c) 2018 David Boissier.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
  This is licensed under LGPL.  License can be found here:  http://www.gnu.org/licenses/lgpl-3.0.txt
  This is provided as is.  If you have questions please direct them to charlie.hubbard at gmail dot you know what.
 */
package com.qihoo.finance.lowcode.console.mongo.view.table;

import com.qihoo.finance.lowcode.common.utils.LocalDateUtils;

import javax.swing.*;
import javax.swing.text.DateFormatter;
import javax.swing.text.DefaultFormatterFactory;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class DateTimeSpinner extends JSpinner {
    public static DateTimeSpinner create() {
        SpinnerDateModel dateModel = new SpinnerDateModel();
        return new DateTimeSpinner(dateModel, new SimpleDateFormat(LocalDateUtils.FORMAT_DATE_TIME));
    }

    public DateTimeSpinner(SpinnerDateModel dateModel, DateFormat timeFormat) {
        super(dateModel);
        updateTextFieldFormat(timeFormat);
    }

    private void updateTextFieldFormat(DateFormat timeFormat) {
        JFormattedTextField tf = ((JSpinner.DefaultEditor) this.getEditor()).getTextField();
        DefaultFormatterFactory factory = (DefaultFormatterFactory) tf.getFormatterFactory();
        DateFormatter formatter = (DateFormatter) factory.getDefaultFormatter();
        // Change the date format to only show the hours
        formatter.setFormat(timeFormat);
    }
}
