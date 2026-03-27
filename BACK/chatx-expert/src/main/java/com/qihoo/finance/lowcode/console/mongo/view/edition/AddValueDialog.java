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

package com.qihoo.finance.lowcode.console.mongo.view.edition;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.util.ui.FormBuilder;
import com.qihoo.finance.lowcode.console.mongo.view.model.JsonDataType;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class AddValueDialog extends AbstractAddDialog {


    private ComboBox<JsonDataType> typeCombobox;
    private JPanel valuePanel;
    private JPanel mainPanel;

    private AddValueDialog(MongoEditionPanel mongoEditionPanel) {
        super(mongoEditionPanel);
        initComponents();

        valuePanel.setLayout(new BorderLayout());
        typeCombobox.setName("valueType");
        typeCombobox.requestFocus();
    }

    private void initComponents() {
        this.typeCombobox = new ComboBox<>();
        this.valuePanel = new JPanel();
        this.mainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent("数组元素类型", typeCombobox)
                .addLabeledComponent("数组元素值", valuePanel)
                .getPanel();
        this.mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        this.mainPanel.setPreferredSize(new Dimension(300, -1));
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return mainPanel;
    }

    public static AddValueDialog createDialog(MongoEditionPanel parentPanel) {
        AddValueDialog dialog = new AddValueDialog(parentPanel);
        dialog.init();
        dialog.setTitle("添加值");
        return dialog;
    }

    @Override
    protected void init() {
        super.init();
        initCombo(typeCombobox, valuePanel);
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        try {
            currentEditor.validate();
        } catch (Exception ex) {
            return new ValidationInfo(ex.getMessage());
        }

        return null;
    }

    @Override
    public Object getValue() {
        return currentEditor.getValue();
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return typeCombobox;
    }
}
