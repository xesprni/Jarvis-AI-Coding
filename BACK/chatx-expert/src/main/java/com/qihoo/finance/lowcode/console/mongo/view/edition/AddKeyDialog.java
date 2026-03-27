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
import com.qihoo.finance.lowcode.console.mongo.utils.GuiUtils;
import com.qihoo.finance.lowcode.console.mongo.view.model.JsonDataType;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class AddKeyDialog extends AbstractAddDialog {


    private JTextField nameTextField;
    private ComboBox<JsonDataType> typeCombobox;
    private JPanel valuePanel;
    private JPanel mainPanel;

    private AddKeyDialog(MongoEditionPanel mongoEditionPanel) {
        super(mongoEditionPanel);
        initComponents();

        mainPanel.setPreferredSize(GuiUtils.enlargeWidth(mainPanel.getPreferredSize(), 1.5d));
        valuePanel.setLayout(new BorderLayout());
        nameTextField.setName("keyName");
        typeCombobox.setName("valueType");
    }

    private void initComponents() {
        this.nameTextField = new JTextField();
        this.typeCombobox = new ComboBox<>();
        this.valuePanel = new JPanel();
        this.mainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent("字段名称", nameTextField)
                .addLabeledComponent("字段类型", typeCombobox)
                .addLabeledComponent("字段值", valuePanel)
                .getPanel();
        this.mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        this.mainPanel.setPreferredSize(new Dimension(300, -1));
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return mainPanel;
    }

    public static AddKeyDialog createDialog(MongoEditionPanel parentPanel) {
        AddKeyDialog dialog = new AddKeyDialog(parentPanel);
        dialog.init();
        dialog.setTitle("添加字段");

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
        String keyName = getKey();
        if (StringUtils.isBlank(keyName)) {
            return new ValidationInfo("Key name is not set");
        }

        if (mongoEditionPanel.containsKey(keyName)) {
            return new ValidationInfo(String.format("Key '%s' is already used", keyName));
        }

        try {
            currentEditor.validate();
        } catch (Exception ex) {
            return new ValidationInfo(ex.getMessage());
        }

        return null;
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return nameTextField;
    }

    public String getKey() {
        return nameTextField.getText();
    }

    @Override
    public Object getValue() {
        return currentEditor.getValue();
    }
}
