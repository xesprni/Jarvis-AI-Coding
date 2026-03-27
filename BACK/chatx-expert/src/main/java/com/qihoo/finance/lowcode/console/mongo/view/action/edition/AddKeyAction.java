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

package com.qihoo.finance.lowcode.console.mongo.view.action.edition;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.qihoo.finance.lowcode.console.mongo.view.edition.AddKeyDialog;
import com.qihoo.finance.lowcode.console.mongo.view.edition.MongoEditionPanel;

import java.awt.event.KeyEvent;
import java.util.Objects;

public class AddKeyAction extends AnAction implements DumbAware {

    private final MongoEditionPanel mongoEditionPanel;

    public AddKeyAction(MongoEditionPanel mongoEditionPanel) {
        super("添加字段", "添加字段", AllIcons.General.Add);
        registerCustomShortcutSet(KeyEvent.VK_INSERT, KeyEvent.ALT_MASK, mongoEditionPanel);
        this.mongoEditionPanel = mongoEditionPanel;
    }

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        AddKeyDialog dialog = AddKeyDialog.createDialog(mongoEditionPanel);
        dialog.show();

        if (!dialog.isOK()) {
            return;
        }

        if (Objects.nonNull(dialog.getValue())) {
            mongoEditionPanel.addKey(dialog.getKey(), dialog.getValue());
        }
    }
}
