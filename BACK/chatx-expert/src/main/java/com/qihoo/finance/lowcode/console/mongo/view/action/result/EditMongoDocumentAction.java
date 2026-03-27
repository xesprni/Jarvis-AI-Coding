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

package com.qihoo.finance.lowcode.console.mongo.view.action.result;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.qihoo.finance.lowcode.console.mongo.view.edition.MongoEditionDialog;
import com.qihoo.finance.lowcode.console.mongo.view.ui.MongoResultPanel;

import java.awt.*;
import java.awt.event.KeyEvent;

public class EditMongoDocumentAction extends AnAction implements DumbAware {

    private final MongoResultPanel resultPanel;

    public EditMongoDocumentAction(MongoResultPanel resultPanel) {
        super("编辑", "Edit mongo document", AllIcons.Actions.Edit);
        this.resultPanel = resultPanel;

        registerCustomShortcutSet(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), resultPanel);
    }

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        resultPanel.editSelectedMongoDocument(MongoEditionDialog.Operate.EDIT);
    }

    @Override
    public void update(AnActionEvent event) {
        super.update(event);
        event.getPresentation().setEnabled(resultPanel.isSelectedNodeId());
    }
}
