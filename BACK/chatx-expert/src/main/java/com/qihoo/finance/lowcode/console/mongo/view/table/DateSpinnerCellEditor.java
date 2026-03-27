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

package com.qihoo.finance.lowcode.console.mongo.view.table;

import com.qihoo.finance.lowcode.console.mongo.view.nodedescriptor.MongoKeyValueDescriptor;
import lombok.Getter;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.tree.TreeCellEditor;
import java.awt.*;

@Getter
public class DateSpinnerCellEditor extends AbstractCellEditor implements TableCellEditor, TreeCellEditor {
    private final DateTimeSpinner spinner;

    public DateSpinnerCellEditor() {
        this.spinner = DateTimeSpinner.create();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        if (value instanceof MongoKeyValueDescriptor.MongoKeyDateValueDescriptor) {
            spinner.setValue(((MongoKeyValueDescriptor.MongoKeyDateValueDescriptor) value).getValue());
        }
        return spinner;
    }

    @Override
    public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row) {
        if (value instanceof MongoKeyValueDescriptor.MongoKeyDateValueDescriptor) {
            spinner.setValue(((MongoKeyValueDescriptor.MongoKeyDateValueDescriptor) value).getValue());
        }
        return spinner;
    }

    @Override
    public Object getCellEditorValue() {
        return spinner.getValue();
    }
}
